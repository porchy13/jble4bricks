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
 * dedicated serial dispatch queue ("ch.varani.bricks.ble") so that BLE callbacks
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
@import os.log;

#include "BleBridge.h"
#include <stdatomic.h>

/* ═══════════════════════════════════════════════════════════════════════════
   Logging — os_log categories
   ═══════════════════════════════════════════════════════════════════════════ */

/** os_log subsystem for all BLE bridge log messages. */
static const char *const LOG_SUBSYSTEM = "ch.varani.bricks.ble";

/** os_log category for scan-related events. */
static os_log_t BLE_LOG_SCAN;

/** os_log category for connection lifecycle events. */
static os_log_t BLE_LOG_CONNECT;

/** os_log category for GATT service/characteristic events. */
static os_log_t BLE_LOG_GATT;

/* ═══════════════════════════════════════════════════════════════════════════
   Constants
   ═══════════════════════════════════════════════════════════════════════════ */

/** Dispatch queue label used for the CoreBluetooth central manager. */
static const char *const QUEUE_LABEL = "ch.varani.bricks.ble";

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

    /*
     * Global reference to the BleNativeCallbacks implementation (MacOsBleScanner).
     * All JNI callbacks — both scan results and GATT notifications — are routed
     * through this single object so that BleConnectionContext carries no Java
     * references.
     */
    JavaVM                 *jvm;
    jobject                 callbacksRef;         /* GlobalRef to BleNativeCallbacks */
    jmethodID               onDeviceFoundMethod;  /* void onDeviceFound(String,String,int,byte[]) */
    jmethodID               onNotificationMethod; /* void onNotification(long,String,String,[B) */

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
    /*
     * Per-peripheral manufacturer-data cache (UUID string → NSData).
     *
     * CoreBluetooth splits advertisement payloads across multiple callbacks and
     * omits fields that have not changed since the previous scan window.  In
     * practice this means that CBAdvertisementDataManufacturerDataKey is often
     * absent for peripherals that the OS already knows (e.g. previously paired
     * or recently seen devices).  We cache the last non-nil value so that every
     * callback fired by didDiscoverPeripheral carries usable manufacturer data.
     *
     * The cache is cleared at the start of each scan so stale data from a
     * previous scan session cannot leak into a new one.
     */
    NSMutableDictionary<NSString *, NSData *> *_mfrDataCache;
}

- (instancetype)initWithContext:(BleContext *)ctx {
    self = [super init];
    if (self) {
        _ctx = ctx;
        _mfrDataCache = [NSMutableDictionary dictionary];
    }
    return self;
}

/** Clears the manufacturer-data cache.  Must be called before each new scan. */
- (void)clearMfrDataCache {
    [_mfrDataCache removeAllObjects];
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
    switch (central.state) {
        case CBManagerStatePoweredOn:
            os_log_info(BLE_LOG_SCAN, "Bluetooth adapter state: PoweredOn");
            dispatch_semaphore_signal(_ctx->readySemaphore);
            break;
        case CBManagerStatePoweredOff:
            os_log_error(BLE_LOG_SCAN, "Bluetooth adapter state: PoweredOff");
            break;
        case CBManagerStateUnauthorized:
            os_log_error(BLE_LOG_SCAN, "Bluetooth adapter state: Unauthorized");
            break;
        case CBManagerStateUnsupported:
            os_log_error(BLE_LOG_SCAN, "Bluetooth adapter state: Unsupported");
            break;
        case CBManagerStateResetting:
            os_log_info(BLE_LOG_SCAN, "Bluetooth adapter state: Resetting");
            break;
        default:
            os_log_info(BLE_LOG_SCAN, "Bluetooth adapter state: Unknown (%ld)",
                    (long)central.state);
            break;
    }
}

/* ───────────────────────────────────────────────────────────────────────────
   CBCentralManagerDelegate — scan results
   ─────────────────────────────────────────────────────────────────────────── */

/**
 * centralManager:didDiscoverPeripheral:advertisementData:RSSI: — called by
 * CBCentralManager each time a matching advertisement is received.
 *
 * Invokes the Java callback BleNativeCallbacks.onDeviceFound(id, name, rssi, manufacturerData).
 * The manufacturer data byte array corresponds to the raw CBAdvertisementDataManufacturerDataKey
 * payload from the advertisement. An empty array is passed when no manufacturer data is present.
 */
- (void)centralManager:(CBCentralManager *)central
    didDiscoverPeripheral:(CBPeripheral *)peripheral
    advertisementData:(NSDictionary<NSString *, id> *)advertisementData
    RSSI:(NSNumber *)RSSI {

    (void)central;

    NSString *deviceId   = peripheral.identifier.UUIDString;
    NSString *deviceName = peripheral.name ?: @"";
    int       rssi       = RSSI.intValue;

    /* Extract raw manufacturer-specific advertisement data (may be nil). */
    NSData *mfrData = advertisementData[CBAdvertisementDataManufacturerDataKey];

    /*
     * CoreBluetooth caches advertisement payloads and may omit fields that
     * have not changed since the last scan window.  Update the per-peripheral
     * cache when fresh data arrives; fall back to the cached value otherwise.
     */
    if (mfrData != nil) {
        _mfrDataCache[deviceId] = mfrData;
    } else {
        mfrData = _mfrDataCache[deviceId];  /* nil if never seen */
    }

    /* Log advertised service UUIDs for diagnostic purposes (not yet passed to Java). */
    NSArray<CBUUID *> *serviceUuids = advertisementData[CBAdvertisementDataServiceUUIDsKey];
    NSMutableString *uuidLog = [NSMutableString string];
    for (CBUUID *u in serviceUuids) { [uuidLog appendFormat:@"%@ ", u.UUIDString]; }

    os_log_info(BLE_LOG_SCAN,
            "Peripheral discovered: id=%{public}@ name='%{public}@' rssi=%d mfrData=%zu bytes serviceUuids=[%{public}@]",
            deviceId, deviceName, rssi, mfrData != nil ? mfrData.length : (NSUInteger)0,
            uuidLog.length > 0 ? uuidLog : @"");

    BOOL attached = NO;
    JNIEnv *env = attachCurrentThread(_ctx->jvm, &attached);

    jstring jId   = (*env)->NewStringUTF(env, deviceId.UTF8String);
    jstring jName = (*env)->NewStringUTF(env, deviceName.UTF8String);

    /* Build a jbyteArray from the manufacturer data (empty array when absent). */
    jsize mfrLen = (mfrData != nil) ? (jsize)mfrData.length : 0;
    jbyteArray jMfrData = (*env)->NewByteArray(env, mfrLen);
    if (mfrLen > 0) {
        (*env)->SetByteArrayRegion(env, jMfrData, 0, mfrLen,
                (const jbyte *)mfrData.bytes);
    }

    (*env)->CallVoidMethod(env,
            _ctx->callbacksRef,
            _ctx->onDeviceFoundMethod,
            jId,
            jName,
            (jint)rssi,
            jMfrData);

    (*env)->DeleteLocalRef(env, jId);
    (*env)->DeleteLocalRef(env, jName);
    (*env)->DeleteLocalRef(env, jMfrData);

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

    os_log_info(BLE_LOG_CONNECT, "Peripheral connected: id=%{public}@",
            peripheral.identifier.UUIDString);

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

    os_log_error(BLE_LOG_CONNECT,
            "Peripheral connection failed: id=%{public}@ error=%{public}@",
            peripheral.identifier.UUIDString,
            error != nil ? error.localizedDescription : @"(no error details)");

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

    if (error != nil) {
        os_log_info(BLE_LOG_CONNECT,
                "Peripheral disconnected: id=%{public}@ error=%{public}@",
                peripheral.identifier.UUIDString, error.localizedDescription);
    } else {
        os_log_info(BLE_LOG_CONNECT, "Peripheral disconnected: id=%{public}@",
                peripheral.identifier.UUIDString);
    }

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
        os_log_error(BLE_LOG_GATT,
                "Service discovery failed: id=%{public}@ error=%{public}@",
                peripheral.identifier.UUIDString, error.localizedDescription);
        conn->lastOpError = YES;
        dispatch_semaphore_signal(conn->discoveryDone);
        return;
    }

    os_log_info(BLE_LOG_GATT, "Services discovered: id=%{public}@ count=%lu",
            peripheral.identifier.UUIDString,
            (unsigned long)peripheral.services.count);

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

    BleConnectionContext *conn = (__bridge BleConnectionContext *)peripheral.delegate;
    if (conn == nil) {
        return;
    }

    if (error != nil) {
        os_log_error(BLE_LOG_GATT,
                "Characteristic discovery failed: id=%{public}@ svc=%{public}@ error=%{public}@",
                peripheral.identifier.UUIDString,
                service.UUID.UUIDString, error.localizedDescription);
        conn->lastOpError = YES;
        dispatch_semaphore_signal(conn->discoveryDone);
        return;
    }

    os_log_info(BLE_LOG_GATT,
            "Characteristics discovered: id=%{public}@ svc=%{public}@ count=%lu",
            peripheral.identifier.UUIDString,
            service.UUID.UUIDString,
            (unsigned long)service.characteristics.count);

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
        /* Notification — deliver to Java via BleNativeCallbacks.onNotification. */
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

        /* Pass the connection pointer so the Java side can route to the
         * correct MacOsBleConnection instance in its openConnections map. */
        jlong connPtr = (jlong)(uintptr_t)conn;

        (*env)->CallVoidMethod(env,
                conn->ctx->callbacksRef,
                conn->ctx->onNotificationMethod,
                connPtr,
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
 * Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeInit
 *
 * Creates a CBCentralManager on a dedicated serial dispatch queue and waits
 * (up to MANAGER_READY_TIMEOUT seconds) for the hardware to become powered on.
 *
 * callbacksObj is the BleNativeCallbacks implementation (MacOsBleScanner) whose
 * onDeviceFound(String,String,int) and onNotification(long,String,String,[B) methods
 * are called by the CoreBluetooth dispatch-queue threads.  A JNI global reference
 * to it is stored in BleContext for the lifetime of the context.
 *
 * Returns: pointer to a heap-allocated BleContext cast to jlong, or 0 on
 *          failure (in which case a Java exception has been thrown).
 */
JNIEXPORT jlong JNICALL
Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeInit(
        JNIEnv *env, jobject self, jobject callbacksObj) {

    (void)self;   /* JniNativeBridge instance — not used for callbacks */

    /* Initialise os_log objects on first call. */
    BLE_LOG_SCAN    = os_log_create(LOG_SUBSYSTEM, "scan");
    BLE_LOG_CONNECT = os_log_create(LOG_SUBSYSTEM, "connect");
    BLE_LOG_GATT    = os_log_create(LOG_SUBSYSTEM, "gatt");

    os_log_info(BLE_LOG_SCAN, "nativeInit: allocating BleContext");

    BleContext *ctx = calloc(1, sizeof(BleContext));
    if (ctx == NULL) {
        (*env)->ThrowNew(env,
                (*env)->FindClass(env, "ch/varani/bricks/ble/api/BleException"),
                "Failed to allocate BleContext");
        return 0L;
    }

    /* Store a reference to the JVM so that BLE-thread callbacks can attach. */
    (*env)->GetJavaVM(env, &ctx->jvm);

    /* Keep a global reference to the BleNativeCallbacks implementation. */
    ctx->callbacksRef = (*env)->NewGlobalRef(env, callbacksObj);

    /* Look up void onDeviceFound(String, String, int, byte[]) */
    jclass callbacksClass = (*env)->GetObjectClass(env, callbacksObj);
    ctx->onDeviceFoundMethod = (*env)->GetMethodID(env, callbacksClass,
            "onDeviceFound", "(Ljava/lang/String;Ljava/lang/String;I[B)V");

    if (ctx->onDeviceFoundMethod == NULL) {
        (*env)->DeleteGlobalRef(env, ctx->callbacksRef);
        free(ctx);
        (*env)->ThrowNew(env,
                (*env)->FindClass(env, "ch/varani/bricks/ble/api/BleException"),
                "BleNativeCallbacks.onDeviceFound method not found — JNI binding error");
        return 0L;
    }

    /* Look up void onNotification(long, String, String, byte[]) */
    ctx->onNotificationMethod = (*env)->GetMethodID(env, callbacksClass,
            "onNotification", "(JLjava/lang/String;Ljava/lang/String;[B)V");

    if (ctx->onNotificationMethod == NULL) {
        (*env)->DeleteGlobalRef(env, ctx->callbacksRef);
        free(ctx);
        (*env)->ThrowNew(env,
                (*env)->FindClass(env, "ch/varani/bricks/ble/api/BleException"),
                "BleNativeCallbacks.onNotification method not found — JNI binding error");
        return 0L;
    }

    if (ctx->onDeviceFoundMethod == NULL) {
        free(ctx);
        (*env)->ThrowNew(env,
                (*env)->FindClass(env, "ch/varani/bricks/ble/api/BleException"),
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
        os_log_error(BLE_LOG_SCAN,
                "nativeInit timed out waiting for Bluetooth adapter "
                "(state=%ld). Ensure Bluetooth is enabled.",
                (long)ctx->centralManager.state);
        (*env)->DeleteGlobalRef(env, ctx->callbacksRef);
        /* ARC will release centralManager and delegate. */
        ctx->centralManager = nil;
        ctx->delegate       = nil;
        free(ctx);
        (*env)->ThrowNew(env,
                (*env)->FindClass(env, "ch/varani/bricks/ble/api/BleException"),
                "Bluetooth adapter did not become ready within the timeout. "
                "Ensure Bluetooth is enabled on this Mac.");
        return 0L;
    }

    return (jlong)(uintptr_t)ctx;
}

/**
 * Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeStartScan
 *
 * Calls CBCentralManager.scanForPeripheralsWithServices:options: on the BLE
 * dispatch queue.  If serviceUuid is non-null only peripherals advertising
 * that service UUID are reported.
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeStartScan(
        JNIEnv *env, jobject self, jlong contextPtr, jstring serviceUuid) {

    (void)self;

    BleContext *ctx = (BleContext *)(uintptr_t)contextPtr;

    NSString *uuidString = nsStringFromJString(env, serviceUuid);

    NSArray<CBUUID *> *services = nil;
    if (uuidString != nil) {
        services = @[[CBUUID UUIDWithString:uuidString]];
    }

    os_log_info(BLE_LOG_SCAN, "nativeStartScan: serviceUuid=%{public}@",
            uuidString != nil ? uuidString : @"(all)");

    /* Clear stale manufacturer-data cache from any previous scan. */
    [ctx->delegate clearMfrDataCache];

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
 * Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeStopScan
 *
 * Calls CBCentralManager.stopScan on the BLE dispatch queue.
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeStopScan(
        JNIEnv *env, jobject self, jlong contextPtr) {

    (void)env;
    (void)self;

    BleContext *ctx = (BleContext *)(uintptr_t)contextPtr;

    os_log_info(BLE_LOG_SCAN, "nativeStopScan");

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
 * Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeIsScanning
 *
 * Returns the cached scanning flag (CBCentralManager.isScanning is not KVO-safe
 * from an arbitrary thread).
 */
JNIEXPORT jboolean JNICALL
Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeIsScanning(
        JNIEnv *env, jobject self, jlong contextPtr) {

    (void)env;
    (void)self;

    BleContext *ctx = (BleContext *)(uintptr_t)contextPtr;
    return (jboolean)(atomic_load(&ctx->scanning) != 0);
}

/**
 * Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeConnect
 *
 * Retrieves (or creates) the CBPeripheral for the given UUID, asks the central
 * manager to connect, waits for the connection + service discovery to
 * complete, then returns a pointer to the BleConnectionContext.
 *
 * Returns: pointer to BleConnectionContext cast to jlong, or 0 on failure.
 */
JNIEXPORT jlong JNICALL
Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeConnect(
        JNIEnv *env, jobject self, jlong contextPtr, jstring peripheralUuid) {

    (void)self;

    BleContext *ctx = (BleContext *)(uintptr_t)contextPtr;
    NSString *uuidString = nsStringFromJString(env, peripheralUuid);

    os_log_info(BLE_LOG_CONNECT, "nativeConnect: uuid=%{public}@", uuidString);

    NSUUID *nsuuid = [[NSUUID alloc] initWithUUIDString:uuidString];
    if (nsuuid == nil) {
        (*env)->ThrowNew(env,
                (*env)->FindClass(env, "ch/varani/bricks/ble/api/BleException"),
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
                (*env)->FindClass(env, "ch/varani/bricks/ble/api/BleException"),
                "Peripheral not found — ensure a scan has discovered it first");
        return 0L;
    }

    BleConnectionContext *conn = calloc(1, sizeof(BleConnectionContext));
    if (conn == NULL) {
        (*env)->ThrowNew(env,
                (*env)->FindClass(env, "ch/varani/bricks/ble/api/BleException"),
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
        if (discoveryResult != 0) {
            os_log_error(BLE_LOG_CONNECT,
                    "nativeConnect timed out waiting for service discovery: uuid=%{public}@",
                    uuidString);
        } else {
            os_log_error(BLE_LOG_CONNECT,
                    "nativeConnect failed (connection or discovery error): uuid=%{public}@",
                    uuidString);
        }
        dispatch_sync(ctx->bleQueue, ^{
            [ctx->centralManager cancelPeripheralConnection:peripheral];
        });
        free(conn);
        (*env)->ThrowNew(env,
                (*env)->FindClass(env, "ch/varani/bricks/ble/api/BleException"),
                "Connection or service discovery failed");
        return 0L;
    }

    os_log_info(BLE_LOG_CONNECT,
            "nativeConnect succeeded: uuid=%{public}@ connPtr=0x%lx",
            uuidString, (unsigned long)(uintptr_t)conn);

    return (jlong)(uintptr_t)conn;
}

/**
 * Calls CBCentralManager.cancelPeripheralConnection and waits for the
 * disconnection callback, then frees the BleConnectionContext.
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeDisconnect(
        JNIEnv *env, jobject self, jlong contextPtr, jlong connectionPtr) {

    (void)env;
    (void)self;

    BleContext           *ctx  = (BleContext *)(uintptr_t)contextPtr;
    BleConnectionContext *conn = (BleConnectionContext *)(uintptr_t)connectionPtr;

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
 * Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeWriteWithoutResponse
 *
 * Calls CBPeripheral.writeValue:forCharacteristic:type: with
 * CBCharacteristicWriteWithoutResponse.
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeWriteWithoutResponse(
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
        } else {
            os_log_error(BLE_LOG_GATT,
                    "nativeWriteWithoutResponse: characteristic not found svc=%{public}@ chr=%{public}@",
                    svcStr, chrStr);
        }
    });
}

/**
 * Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeReadCharacteristic
 *
 * Calls CBPeripheral.readValueForCharacteristic: and waits (up to
 * READ_TIMEOUT seconds) for the peripheral:didUpdateValueForCharacteristic:
 * callback to deliver the value.
 *
 * Returns: jbyteArray with the value, or null if the read failed.
 */
JNIEXPORT jbyteArray JNICALL
Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeReadCharacteristic(
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
        if (result != 0) {
            os_log_error(BLE_LOG_GATT,
                    "nativeReadCharacteristic timed out: svc=%{public}@ chr=%{public}@",
                    svcStr, chrStr);
        } else {
            os_log_error(BLE_LOG_GATT,
                    "nativeReadCharacteristic failed: svc=%{public}@ chr=%{public}@",
                    svcStr, chrStr);
        }
        return NULL;
    }

    jbyteArray jBytes = (*env)->NewByteArray(env, (jsize)conn->lastReadValue.length);
    (*env)->SetByteArrayRegion(env, jBytes, 0, (jsize)conn->lastReadValue.length,
            (const jbyte *)conn->lastReadValue.bytes);
    return jBytes;
}

/**
 * Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeSetNotify
 *
 * Calls CBPeripheral.setNotifyValue:forCharacteristic: to enable or disable
 * notifications on the given characteristic.
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeSetNotify(
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
 * Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeDestroy
 *
 * Stops any active scan, releases the CBCentralManager, and frees the
 * BleContext struct.  Must be called exactly once when the JniNativeBridge
 * is closed.
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeDestroy(
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

    (*env)->DeleteGlobalRef(env, ctx->callbacksRef);
    free(ctx);
}
