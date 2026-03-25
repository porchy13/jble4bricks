/**
 * BleBridge.m — macOS CoreBluetooth ↔ JNI bridge implementation.
 *
 * This file is the only native compilation unit for the macOS BLE transport.
 * It is built by CMake into a universal binary (arm64 + x86_64) named
 * libble-macos.dylib and bundled inside the JAR under
 * natives/macos/<arch>/libble-macos.dylib.
 *
 * Threading model
 * ───────────────
 * CoreBluetooth dispatches all delegate callbacks on the main run loop (or a
 * background dispatch queue if configured).  This implementation uses a
 * dedicated serial dispatch queue ("ch.varani.lego.ble") so that BLE callbacks
 * never block the application's main thread.
 *
 * JNI callbacks
 * ─────────────
 * When CoreBluetooth delivers a scan result or a characteristic value, the
 * bridge calls back into Java via the cached JNIEnv / method IDs.  Because
 * Objective-C callbacks arrive on the BLE dispatch queue (not the JVM thread
 * that called nativeStartScan), we must attach the current thread to the JVM
 * before calling JNI functions and detach it afterwards.
 *
 * Memory management
 * ─────────────────
 * ARC (-fobjc-arc) is mandatory.  The BleContext and BleConnectionContext
 * structs are heap-allocated plain C structs whose Objective-C object fields
 * are __bridge-transferred to/from the struct so that ARC can track them.
 * The opaque jlong handle passed to Java is simply the C pointer cast to jlong.
 */

@import Foundation;
@import CoreBluetooth;

#include "BleBridge.h"
#include <stdatomic.h>

/* ═══════════════════════════════════════════════════════════════════════════
   Constants
   ═══════════════════════════════════════════════════════════════════════════ */

/** Dispatch queue label used for the CoreBluetooth central manager. */
static const char *const QUEUE_LABEL = "ch.varani.lego.ble";

/** Maximum time (seconds) to wait for a CBCentralManager state change. */
static const NSTimeInterval MANAGER_READY_TIMEOUT = 10.0;

/** Maximum time (seconds) to wait for a connection to be established. */
static const NSTimeInterval CONNECT_TIMEOUT = 15.0;

/** Maximum time (seconds) to wait for a GATT read to complete. */
static const NSTimeInterval READ_TIMEOUT = 10.0;

/* ═══════════════════════════════════════════════════════════════════════════
   Forward declarations
   ═══════════════════════════════════════════════════════════════════════════ */

@interface BleCentralDelegate : NSObject <CBCentralManagerDelegate, CBPeripheralDelegate>
@end

/* ═══════════════════════════════════════════════════════════════════════════
   BleContext — top-level context owned by JniNativeBridge (one per scanner)
   ═══════════════════════════════════════════════════════════════════════════ */

typedef struct {
    CBCentralManager       *__strong centralManager;
    BleCentralDelegate     *__strong delegate;
    dispatch_queue_t        bleQueue;

    /* Back-reference to the Java scanner object; used for scan callbacks. */
    JavaVM                 *jvm;
    jobject                 scannerRef;          /* GlobalRef */
    jmethodID               onDeviceFoundMethod;

    /* Signals that CBCentralManagerStatePoweredOn has been received. */
    dispatch_semaphore_t    readySemaphore;

    /* Signals that scanning has started / stopped. */
    dispatch_semaphore_t    scanSemaphore;

    /* Atomic flag set to 1 while a scan is active. */
    atomic_int              scanning;
} BleContext;

/* ═══════════════════════════════════════════════════════════════════════════
   BleConnectionContext — per-connection context
   ═══════════════════════════════════════════════════════════════════════════ */

typedef struct {
    CBPeripheral           *__strong peripheral;
    BleContext             *ctx;

    /* Semaphore posted when the peripheral finishes service discovery. */
    dispatch_semaphore_t    discoveryDone;

    /* Semaphore posted when a connect / disconnect completes. */
    dispatch_semaphore_t    connectDone;

    /* Semaphore posted when a characteristic read completes. */
    dispatch_semaphore_t    readDone;

    /* Most-recently read characteristic value (or nil on error). */
    NSData                 *__strong lastReadValue;

    /* YES if the last operation resulted in an error. */
    BOOL                    lastOpError;

    /* The Java connection object — for notification callbacks. */
    jobject                 connectionRef;       /* GlobalRef, may be 0 */
    jmethodID               onNotificationMethod;
} BleConnectionContext;

/* ═══════════════════════════════════════════════════════════════════════════
   Helper — attach / detach JNI thread
   ═══════════════════════════════════════════════════════════════════════════ */

/**
 * Attaches the calling thread to the JVM and returns the JNIEnv pointer.
 * Sets *attached to YES if the thread was not already attached (so the caller
 * can detach it afterwards).
 */
static JNIEnv *attachCurrentThread(JavaVM *jvm, BOOL *attached) {
    JNIEnv *env = NULL;
    if ((*jvm)->GetEnv(jvm, (void **)&env, JNI_VERSION_21) == JNI_EDETACHED) {
        (*jvm)->AttachCurrentThreadAsDaemon(jvm, (void **)&env, NULL);
        *attached = YES;
    } else {
        *attached = NO;
    }
    return env;
}

/**
 * Detaches the calling thread from the JVM if it was explicitly attached.
 */
static void detachIfNeeded(JavaVM *jvm, BOOL attached) {
    if (attached) {
        (*jvm)->DetachCurrentThread(jvm);
    }
}

/* ═══════════════════════════════════════════════════════════════════════════
   Helper — NSString ↔ jstring
   ═══════════════════════════════════════════════════════════════════════════ */

static NSString *nsStringFromJString(JNIEnv *env, jstring jstr) {
    if (jstr == NULL) {
        return nil;
    }
    const char *chars = (*env)->GetStringUTFChars(env, jstr, NULL);
    NSString *result = [NSString stringWithUTF8String:chars];
    (*env)->ReleaseStringUTFChars(env, jstr, chars);
    return result;
}

/* ═══════════════════════════════════════════════════════════════════════════
   Helper — find a CBCharacteristic by service + characteristic UUID
   ═══════════════════════════════════════════════════════════════════════════ */

static CBCharacteristic *findCharacteristic(
        CBPeripheral *peripheral,
        NSString *serviceUuidString,
        NSString *characteristicUuidString) {

    CBUUID *svcUuid  = [CBUUID UUIDWithString:serviceUuidString];
    CBUUID *chrUuid  = [CBUUID UUIDWithString:characteristicUuidString];

    for (CBService *service in peripheral.services) {
        if ([service.UUID isEqual:svcUuid]) {
            for (CBCharacteristic *characteristic in service.characteristics) {
                if ([characteristic.UUID isEqual:chrUuid]) {
                    return characteristic;
                }
            }
        }
    }
    return nil;
}

/* ═══════════════════════════════════════════════════════════════════════════
   BleCentralDelegate — CBCentralManagerDelegate + CBPeripheralDelegate
   ═══════════════════════════════════════════════════════════════════════════ */

@implementation BleCentralDelegate {
    BleContext *_ctx;
}

- (instancetype)initWithContext:(BleContext *)ctx {
    self = [super init];
    if (self) {
        _ctx = ctx;
    }
    return self;
}

/* ───────────────────────────────────────────────────────────────────────────
   CBCentralManagerDelegate — state changes
   ─────────────────────────────────────────────────────────────────────────── */

/**
 * centralManagerDidUpdateState: — called by CoreBluetooth whenever the
 * hardware state changes (CBCentralManager.state).
 *
 * Posts the readySemaphore once the adapter is CBManagerStatePoweredOn so
 * that nativeInit can unblock and return.
 */
- (void)centralManagerDidUpdateState:(CBCentralManager *)central {
    if (central.state == CBManagerStatePoweredOn) {
        dispatch_semaphore_signal(_ctx->readySemaphore);
    }
}

/* ───────────────────────────────────────────────────────────────────────────
   CBCentralManagerDelegate — scan results
   ─────────────────────────────────────────────────────────────────────────── */

/**
 * centralManager:didDiscoverPeripheral:advertisementData:RSSI: — called by
 * CBCentralManager each time a matching advertisement is received.
 *
 * Invokes the Java callback JniNativeBridge.onDeviceFound(id, name, rssi).
 */
- (void)centralManager:(CBCentralManager *)central
    didDiscoverPeripheral:(CBPeripheral *)peripheral
    advertisementData:(NSDictionary<NSString *, id> *)advertisementData
    RSSI:(NSNumber *)RSSI {

    (void)central;
    (void)advertisementData;

    NSString *deviceId   = peripheral.identifier.UUIDString;
    NSString *deviceName = peripheral.name ?: @"";
    int       rssi       = RSSI.intValue;

    BOOL attached = NO;
    JNIEnv *env = attachCurrentThread(_ctx->jvm, &attached);

    jstring jId   = (*env)->NewStringUTF(env, deviceId.UTF8String);
    jstring jName = (*env)->NewStringUTF(env, deviceName.UTF8String);

    (*env)->CallVoidMethod(env,
            _ctx->scannerRef,
            _ctx->onDeviceFoundMethod,
            jId,
            jName,
            (jint)rssi);

    (*env)->DeleteLocalRef(env, jId);
    (*env)->DeleteLocalRef(env, jName);

    detachIfNeeded(_ctx->jvm, attached);
}

/* ───────────────────────────────────────────────────────────────────────────
   CBCentralManagerDelegate — connect / disconnect
   ─────────────────────────────────────────────────────────────────────────── */

/**
 * centralManager:didConnectPeripheral: — connection established.
 *
 * Starts GATT service discovery immediately so that the Java future returned
 * from nativeConnect completes only once all services and characteristics are
 * available.
 */
- (void)centralManager:(CBCentralManager *)central
    didConnectPeripheral:(CBPeripheral *)peripheral {

    (void)central;

    BleConnectionContext *conn = (__bridge BleConnectionContext *)peripheral.delegate;
    if (conn == nil) {
        return;
    }
    /* Discover all services; nil means "discover everything". */
    [peripheral discoverServices:nil];
}

/**
 * centralManager:didFailToConnectPeripheral:error: — connection failed.
 */
- (void)centralManager:(CBCentralManager *)central
    didFailToConnectPeripheral:(CBPeripheral *)peripheral
    error:(NSError *)error {

    (void)central;
    (void)error;

    BleConnectionContext *conn = (__bridge BleConnectionContext *)peripheral.delegate;
    if (conn == nil) {
        return;
    }
    conn->lastOpError = YES;
    dispatch_semaphore_signal(conn->connectDone);
}

/**
 * centralManager:didDisconnectPeripheral:error: — peripheral disconnected.
 */
- (void)centralManager:(CBCentralManager *)central
    didDisconnectPeripheral:(CBPeripheral *)peripheral
    error:(NSError *)error {

    (void)central;
    (void)error;

    BleConnectionContext *conn = (__bridge BleConnectionContext *)peripheral.delegate;
    if (conn == nil) {
        return;
    }
    dispatch_semaphore_signal(conn->connectDone);
}

/* ───────────────────────────────────────────────────────────────────────────
   CBPeripheralDelegate — service / characteristic discovery
   ─────────────────────────────────────────────────────────────────────────── */

/**
 * peripheral:didDiscoverServices: — all GATT services discovered.
 *
 * Immediately triggers characteristic discovery for every discovered service.
 */
- (void)peripheral:(CBPeripheral *)peripheral
    didDiscoverServices:(NSError *)error {

    BleConnectionContext *conn = (__bridge BleConnectionContext *)peripheral.delegate;
    if (conn == nil) {
        return;
    }

    if (error != nil) {
        conn->lastOpError = YES;
        dispatch_semaphore_signal(conn->discoveryDone);
        return;
    }

    if (peripheral.services.count == 0) {
        dispatch_semaphore_signal(conn->discoveryDone);
        return;
    }

    /* Trigger characteristic discovery for every service. */
    for (CBService *service in peripheral.services) {
        [peripheral discoverCharacteristics:nil forService:service];
    }
}

/**
 * peripheral:didDiscoverCharacteristicsForService:error: — characteristics for
 * one service have been discovered.
 *
 * Posts discoveryDone only once ALL services have their characteristics.
 */
- (void)peripheral:(CBPeripheral *)peripheral
    didDiscoverCharacteristicsForService:(CBService *)service
    error:(NSError *)error {

    (void)service;

    BleConnectionContext *conn = (__bridge BleConnectionContext *)peripheral.delegate;
    if (conn == nil) {
        return;
    }

    if (error != nil) {
        conn->lastOpError = YES;
        dispatch_semaphore_signal(conn->discoveryDone);
        return;
    }

    /* Check if every service now has its characteristics populated. */
    BOOL allDone = YES;
    for (CBService *svc in peripheral.services) {
        if (svc.characteristics == nil) {
            allDone = NO;
            break;
        }
    }
    if (allDone) {
        dispatch_semaphore_signal(conn->discoveryDone);
    }
}

/* ───────────────────────────────────────────────────────────────────────────
   CBPeripheralDelegate — reads
   ─────────────────────────────────────────────────────────────────────────── */

/**
 * peripheral:didUpdateValueForCharacteristic:error: — fired after both a read
 * request and an incoming notification.
 *
 * For read requests the value is stored in lastReadValue and readDone is
 * signalled.  For notifications the value is delivered to the Java connection
 * object via onNotification.
 */
- (void)peripheral:(CBPeripheral *)peripheral
    didUpdateValueForCharacteristic:(CBCharacteristic *)characteristic
    error:(NSError *)error {

    BleConnectionContext *conn = (__bridge BleConnectionContext *)peripheral.delegate;
    if (conn == nil) {
        return;
    }

    if (characteristic.isNotifying) {
        /* Notification — deliver to Java. */
        if (conn->connectionRef == NULL || conn->onNotificationMethod == NULL) {
            return;
        }

        NSData *value = error == nil ? characteristic.value : nil;
        if (value == nil) {
            return;
        }

        BOOL attached = NO;
        JNIEnv *env = attachCurrentThread(conn->ctx->jvm, &attached);

        jbyteArray jBytes = (*env)->NewByteArray(env, (jsize)value.length);
        (*env)->SetByteArrayRegion(env, jBytes, 0, (jsize)value.length,
                (const jbyte *)value.bytes);

        jstring svcUuid  = (*env)->NewStringUTF(env,
                characteristic.service.UUID.UUIDString.UTF8String);
        jstring chrUuid  = (*env)->NewStringUTF(env,
                characteristic.UUID.UUIDString.UTF8String);

        (*env)->CallVoidMethod(env,
                conn->connectionRef,
                conn->onNotificationMethod,
                svcUuid,
                chrUuid,
                jBytes);

        (*env)->DeleteLocalRef(env, jBytes);
        (*env)->DeleteLocalRef(env, svcUuid);
        (*env)->DeleteLocalRef(env, chrUuid);

        detachIfNeeded(conn->ctx->jvm, attached);
    } else {
        /* Read response. */
        if (error == nil) {
            conn->lastReadValue = [characteristic.value copy];
            conn->lastOpError   = NO;
        } else {
            conn->lastReadValue = nil;
            conn->lastOpError   = YES;
        }
        dispatch_semaphore_signal(conn->readDone);
    }
}

/* ───────────────────────────────────────────────────────────────────────────
   CBPeripheralDelegate — writes
   ─────────────────────────────────────────────────────────────────────────── */

/**
 * peripheral:didWriteValueForCharacteristic:error: — write-with-response
 * confirmation (not used for WriteWithoutResponse, but kept for completeness).
 */
- (void)peripheral:(CBPeripheral *)peripheral
    didWriteValueForCharacteristic:(CBCharacteristic *)characteristic
    error:(NSError *)error {

    (void)peripheral;
    (void)characteristic;
    (void)error;
    /* Write-without-response paths do not wait for this callback. */
}

@end

/* ═══════════════════════════════════════════════════════════════════════════
   JNI entry points
   ═══════════════════════════════════════════════════════════════════════════ */

/**
 * Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeInit
 *
 * Creates a CBCentralManager on a dedicated serial dispatch queue and waits
 * (up to MANAGER_READY_TIMEOUT seconds) for the hardware to become powered on.
 *
 * Returns: pointer to a heap-allocated BleContext cast to jlong, or 0 on
 *          failure (in which case a Java exception has been thrown).
 */
JNIEXPORT jlong JNICALL
Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeInit(JNIEnv *env, jobject self) {

    BleContext *ctx = calloc(1, sizeof(BleContext));
    if (ctx == NULL) {
        (*env)->ThrowNew(env,
                (*env)->FindClass(env, "ch/varani/lego/ble/api/BleException"),
                "Failed to allocate BleContext");
        return 0L;
    }

    /* Store a reference to the JVM so that BLE-thread callbacks can attach. */
    (*env)->GetJavaVM(env, &ctx->jvm);

    /* Keep a global reference to the Java scanner object for callbacks. */
    ctx->scannerRef = (*env)->NewGlobalRef(env, self);

    /* Look up the Java callback method: void onDeviceFound(String, String, int) */
    jclass scannerClass = (*env)->GetObjectClass(env, self);
    ctx->onDeviceFoundMethod = (*env)->GetMethodID(env, scannerClass,
            "onDeviceFound", "(Ljava/lang/String;Ljava/lang/String;I)V");

    if (ctx->onDeviceFoundMethod == NULL) {
        free(ctx);
        (*env)->ThrowNew(env,
                (*env)->FindClass(env, "ch/varani/lego/ble/api/BleException"),
                "JniNativeBridge.onDeviceFound method not found — JNI binding error");
        return 0L;
    }

    ctx->readySemaphore = dispatch_semaphore_create(0);
    ctx->scanSemaphore  = dispatch_semaphore_create(0);
    atomic_store(&ctx->scanning, 0);

    ctx->bleQueue = dispatch_queue_create(QUEUE_LABEL, DISPATCH_QUEUE_SERIAL);

    ctx->delegate      = [[BleCentralDelegate alloc] initWithContext:ctx];
    ctx->centralManager = [[CBCentralManager alloc]
            initWithDelegate:ctx->delegate
                       queue:ctx->bleQueue
                     options:@{CBCentralManagerOptionShowPowerAlertKey: @NO}];

    /* Wait for the adapter to become ready. */
    const long result = dispatch_semaphore_wait(
            ctx->readySemaphore,
            dispatch_time(DISPATCH_TIME_NOW,
                    (int64_t)(MANAGER_READY_TIMEOUT * NSEC_PER_SEC)));

    if (result != 0) {
        /* Timed out — Bluetooth is likely off or unavailable. */
        (*env)->DeleteGlobalRef(env, ctx->scannerRef);
        /* ARC will release centralManager and delegate. */
        ctx->centralManager = nil;
        ctx->delegate       = nil;
        free(ctx);
        (*env)->ThrowNew(env,
                (*env)->FindClass(env, "ch/varani/lego/ble/api/BleException"),
                "Bluetooth adapter did not become ready within the timeout. "
                "Ensure Bluetooth is enabled on this Mac.");
        return 0L;
    }

    return (jlong)(uintptr_t)ctx;
}

/**
 * Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeStartScan
 *
 * Calls CBCentralManager.scanForPeripheralsWithServices:options: on the BLE
 * dispatch queue.  If serviceUuid is non-null only peripherals advertising
 * that service UUID are reported.
 */
JNIEXPORT void JNICALL
Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeStartScan(
        JNIEnv *env, jobject self, jlong contextPtr, jstring serviceUuid) {

    (void)self;

    BleContext *ctx = (BleContext *)(uintptr_t)contextPtr;

    NSString *uuidString = nsStringFromJString(env, serviceUuid);

    NSArray<CBUUID *> *services = nil;
    if (uuidString != nil) {
        services = @[[CBUUID UUIDWithString:uuidString]];
    }

    dispatch_async(ctx->bleQueue, ^{
        [ctx->centralManager
                scanForPeripheralsWithServices:services
                                       options:@{CBCentralManagerScanOptionAllowDuplicatesKey: @YES}];
        atomic_store(&ctx->scanning, 1);
        dispatch_semaphore_signal(ctx->scanSemaphore);
    });

    /* Wait until the scan has actually started. */
    dispatch_semaphore_wait(
            ctx->scanSemaphore,
            dispatch_time(DISPATCH_TIME_NOW, (int64_t)(5.0 * NSEC_PER_SEC)));
}

/**
 * Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeStopScan
 *
 * Calls CBCentralManager.stopScan on the BLE dispatch queue.
 */
JNIEXPORT void JNICALL
Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeStopScan(
        JNIEnv *env, jobject self, jlong contextPtr) {

    (void)env;
    (void)self;

    BleContext *ctx = (BleContext *)(uintptr_t)contextPtr;

    dispatch_async(ctx->bleQueue, ^{
        [ctx->centralManager stopScan];
        atomic_store(&ctx->scanning, 0);
        dispatch_semaphore_signal(ctx->scanSemaphore);
    });

    dispatch_semaphore_wait(
            ctx->scanSemaphore,
            dispatch_time(DISPATCH_TIME_NOW, (int64_t)(5.0 * NSEC_PER_SEC)));
}

/**
 * Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeIsScanning
 *
 * Returns the cached scanning flag (CBCentralManager.isScanning is not KVO-safe
 * from an arbitrary thread).
 */
JNIEXPORT jboolean JNICALL
Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeIsScanning(
        JNIEnv *env, jobject self, jlong contextPtr) {

    (void)env;
    (void)self;

    BleContext *ctx = (BleContext *)(uintptr_t)contextPtr;
    return (jboolean)(atomic_load(&ctx->scanning) != 0);
}

/**
 * Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeConnect
 *
 * Retrieves (or creates) the CBPeripheral for the given UUID, asks the central
 * manager to connect, waits for the connection + service discovery to
 * complete, then returns a pointer to the BleConnectionContext.
 *
 * Returns: pointer to BleConnectionContext cast to jlong, or 0 on failure.
 */
JNIEXPORT jlong JNICALL
Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeConnect(
        JNIEnv *env, jobject self, jlong contextPtr, jstring peripheralUuid) {

    (void)self;

    BleContext *ctx = (BleContext *)(uintptr_t)contextPtr;
    NSString *uuidString = nsStringFromJString(env, peripheralUuid);

    NSUUID *nsuuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    if (nsuuid == nil) {
        (*env)->ThrowNew(env,
                (*env)->FindClass(env, "ch/varani/lego/ble/api/BleException"),
                "Invalid peripheral UUID string");
        return 0L;
    }

    /* Retrieve known peripherals for this UUID (already discovered during scan). */
    __block CBPeripheral *peripheral = nil;
    dispatch_sync(ctx->bleQueue, ^{
        NSArray<CBPeripheral *> *known =
                [ctx->centralManager retrievePeripheralsWithIdentifiers:@[nsuuid]];
        peripheral = known.firstObject;
    });

    if (peripheral == nil) {
        (*env)->ThrowNew(env,
                (*env)->FindClass(env, "ch/varani/lego/ble/api/BleException"),
                "Peripheral not found — ensure a scan has discovered it first");
        return 0L;
    }

    BleConnectionContext *conn = calloc(1, sizeof(BleConnectionContext));
    if (conn == NULL) {
        (*env)->ThrowNew(env,
                (*env)->FindClass(env, "ch/varani/lego/ble/api/BleException"),
                "Failed to allocate BleConnectionContext");
        return 0L;
    }

    conn->peripheral    = peripheral;
    conn->ctx           = ctx;
    conn->discoveryDone = dispatch_semaphore_create(0);
    conn->connectDone   = dispatch_semaphore_create(0);
    conn->readDone      = dispatch_semaphore_create(0);
    conn->lastOpError   = NO;

    /* Set the peripheral delegate to this connection context (bridged ptr). */
    dispatch_sync(ctx->bleQueue, ^{
        peripheral.delegate = (__bridge id<CBPeripheralDelegate>)conn;
        [ctx->centralManager connectPeripheral:peripheral options:nil];
    });

    /* Wait for service discovery to complete (connect + discoverServices). */
    const long discoveryResult = dispatch_semaphore_wait(
            conn->discoveryDone,
            dispatch_time(DISPATCH_TIME_NOW,
                    (int64_t)(CONNECT_TIMEOUT * NSEC_PER_SEC)));

    if (discoveryResult != 0 || conn->lastOpError) {
        dispatch_sync(ctx->bleQueue, ^{
            [ctx->centralManager cancelPeripheralConnection:peripheral];
        });
        free(conn);
        (*env)->ThrowNew(env,
                (*env)->FindClass(env, "ch/varani/lego/ble/api/BleException"),
                "Connection or service discovery failed");
        return 0L;
    }

    return (jlong)(uintptr_t)conn;
}

/**
 * Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeDisconnect
 *
 * Calls CBCentralManager.cancelPeripheralConnection and waits for the
 * disconnection callback, then frees the BleConnectionContext.
 */
JNIEXPORT void JNICALL
Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeDisconnect(
        JNIEnv *env, jobject self, jlong contextPtr, jlong connectionPtr) {

    (void)self;

    BleContext           *ctx  = (BleContext *)(uintptr_t)contextPtr;
    BleConnectionContext *conn = (BleConnectionContext *)(uintptr_t)connectionPtr;

    if (conn->connectionRef != NULL) {
        (*env)->DeleteGlobalRef(env, conn->connectionRef);
        conn->connectionRef = NULL;
    }

    dispatch_sync(ctx->bleQueue, ^{
        conn->peripheral.delegate = nil;
        [ctx->centralManager cancelPeripheralConnection:conn->peripheral];
    });

    dispatch_semaphore_wait(
            conn->connectDone,
            dispatch_time(DISPATCH_TIME_NOW,
                    (int64_t)(CONNECT_TIMEOUT * NSEC_PER_SEC)));

    conn->peripheral = nil;
    free(conn);
}

/**
 * Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeWriteWithoutResponse
 *
 * Calls CBPeripheral.writeValue:forCharacteristic:type: with
 * CBCharacteristicWriteWithoutResponse.
 */
JNIEXPORT void JNICALL
Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeWriteWithoutResponse(
        JNIEnv *env, jobject self, jlong connectionPtr,
        jstring serviceUuid, jstring characteristicUuid, jbyteArray data) {

    (void)self;

    BleConnectionContext *conn = (BleConnectionContext *)(uintptr_t)connectionPtr;

    NSString *svcStr = nsStringFromJString(env, serviceUuid);
    NSString *chrStr = nsStringFromJString(env, characteristicUuid);

    jsize    len   = (*env)->GetArrayLength(env, data);
    jbyte   *bytes = (*env)->GetByteArrayElements(env, data, NULL);
    NSData  *nsData = [NSData dataWithBytes:bytes length:(NSUInteger)len];
    (*env)->ReleaseByteArrayElements(env, data, bytes, JNI_ABORT);

    dispatch_async(conn->ctx->bleQueue, ^{
        CBCharacteristic *chr = findCharacteristic(conn->peripheral, svcStr, chrStr);
        if (chr != nil) {
            [conn->peripheral writeValue:nsData
                       forCharacteristic:chr
                                    type:CBCharacteristicWriteWithoutResponse];
        }
    });
}

/**
 * Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeReadCharacteristic
 *
 * Calls CBPeripheral.readValueForCharacteristic: and waits (up to
 * READ_TIMEOUT seconds) for the peripheral:didUpdateValueForCharacteristic:
 * callback to deliver the value.
 *
 * Returns: jbyteArray with the value, or null if the read failed.
 */
JNIEXPORT jbyteArray JNICALL
Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeReadCharacteristic(
        JNIEnv *env, jobject self, jlong connectionPtr,
        jstring serviceUuid, jstring characteristicUuid) {

    (void)self;

    BleConnectionContext *conn = (BleConnectionContext *)(uintptr_t)connectionPtr;

    NSString *svcStr = nsStringFromJString(env, serviceUuid);
    NSString *chrStr = nsStringFromJString(env, characteristicUuid);

    conn->lastReadValue = nil;
    conn->lastOpError   = NO;

    dispatch_async(conn->ctx->bleQueue, ^{
        CBCharacteristic *chr = findCharacteristic(conn->peripheral, svcStr, chrStr);
        if (chr != nil) {
            [conn->peripheral readValueForCharacteristic:chr];
        } else {
            conn->lastOpError = YES;
            dispatch_semaphore_signal(conn->readDone);
        }
    });

    const long result = dispatch_semaphore_wait(
            conn->readDone,
            dispatch_time(DISPATCH_TIME_NOW,
                    (int64_t)(READ_TIMEOUT * NSEC_PER_SEC)));

    if (result != 0 || conn->lastOpError || conn->lastReadValue == nil) {
        return NULL;
    }

    jbyteArray jBytes = (*env)->NewByteArray(env, (jsize)conn->lastReadValue.length);
    (*env)->SetByteArrayRegion(env, jBytes, 0, (jsize)conn->lastReadValue.length,
            (const jbyte *)conn->lastReadValue.bytes);
    return jBytes;
}

/**
 * Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeSetNotify
 *
 * Calls CBPeripheral.setNotifyValue:forCharacteristic: to enable or disable
 * notifications on the given characteristic.
 */
JNIEXPORT void JNICALL
Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeSetNotify(
        JNIEnv *env, jobject self, jlong connectionPtr,
        jstring serviceUuid, jstring characteristicUuid, jboolean enable) {

    (void)self;

    BleConnectionContext *conn = (BleConnectionContext *)(uintptr_t)connectionPtr;

    NSString *svcStr = nsStringFromJString(env, serviceUuid);
    NSString *chrStr = nsStringFromJString(env, characteristicUuid);
    BOOL      doEnable = (enable == JNI_TRUE);

    dispatch_async(conn->ctx->bleQueue, ^{
        CBCharacteristic *chr = findCharacteristic(conn->peripheral, svcStr, chrStr);
        if (chr != nil) {
            [conn->peripheral setNotifyValue:doEnable forCharacteristic:chr];
        }
    });
}

/**
 * Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeDestroy
 *
 * Stops any active scan, releases the CBCentralManager, and frees the
 * BleContext struct.  Must be called exactly once when the JniNativeBridge
 * is closed.
 */
JNIEXPORT void JNICALL
Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeDestroy(
        JNIEnv *env, jobject self, jlong contextPtr) {

    (void)self;

    BleContext *ctx = (BleContext *)(uintptr_t)contextPtr;

    dispatch_sync(ctx->bleQueue, ^{
        if (atomic_load(&ctx->scanning) != 0) {
            [ctx->centralManager stopScan];
            atomic_store(&ctx->scanning, 0);
        }
        ctx->centralManager.delegate = nil;
        ctx->centralManager = nil;
        ctx->delegate       = nil;
    });

    (*env)->DeleteGlobalRef(env, ctx->scannerRef);
    free(ctx);
}
