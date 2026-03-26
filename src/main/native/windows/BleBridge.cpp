/**
 * BleBridge.cpp — JNI implementation of the Windows WinRT Bluetooth LE bridge.
 *
 * This file implements all 10 JNI entry points declared in BleBridge.h using
 * the Windows Runtime (WinRT) Bluetooth LE API:
 *
 *   - Scanning:
 *       Windows::Devices::Bluetooth::Advertisement::BluetoothLEAdvertisementWatcher
 *       Windows::Devices::Bluetooth::Advertisement::BluetoothLEAdvertisementReceivedEventArgs
 *
 *   - Connection and GATT:
 *       Windows::Devices::Bluetooth::BluetoothLEDevice
 *       Windows::Devices::Bluetooth::GenericAttributeProfile::GattDeviceService
 *       Windows::Devices::Bluetooth::GenericAttributeProfile::GattCharacteristic
 *       Windows::Devices::Bluetooth::GenericAttributeProfile::GattClientCharacteristicConfigurationDescriptorValue
 *
 * The JVM is attached to each WinRT thread-pool thread the first time that
 * thread enters a JNI callback; it is detached when the thread exits using a
 * thread_local RAII guard.
 *
 * Memory management:
 *   - BleContext and BleConnectionContext are heap-allocated structs whose
 *     addresses are stored as opaque jlong values on the Java side.
 *   - The JNI global reference to the WindowsBleNativeCallbacks implementation
 *     (WindowsBleScanner) is stored in BleContext and deleted in nativeDestroy.
 *   - BleConnectionContext holds no Java references; it is allocated in
 *     nativeConnect and freed in nativeDisconnect.
 */

#include "BleBridge.h"

#include <windows.h>
#include <winrt/Windows.Foundation.h>
#include <winrt/Windows.Foundation.Collections.h>
#include <winrt/Windows.Devices.Bluetooth.h>
#include <winrt/Windows.Devices.Bluetooth.Advertisement.h>
#include <winrt/Windows.Devices.Bluetooth.GenericAttributeProfile.h>
#include <winrt/Windows.Storage.Streams.h>

#include <atomic>
#include <map>
#include <mutex>
#include <string>
#include <vector>

using namespace winrt;
using namespace Windows::Devices::Bluetooth;
using namespace Windows::Devices::Bluetooth::Advertisement;
using namespace Windows::Devices::Bluetooth::GenericAttributeProfile;
using namespace Windows::Foundation;
using namespace Windows::Storage::Streams;

/* ─────────────────────────────────────────────────────────────────────────────
   JVM reference — set once in JNI_OnLoad
   ───────────────────────────────────────────────────────────────────────────── */

static JavaVM *g_jvm = nullptr;

/**
 * JNI_OnLoad — called by the JVM when ble-windows.dll is first loaded.
 *
 * Stores the JavaVM pointer for later use by WinRT thread-pool callbacks
 * that need to attach a JNI environment.
 */
JNIEXPORT jint JNICALL JNI_OnLoad(JavaVM *vm, void * /*reserved*/) {
    g_jvm = vm;
    winrt::init_apartment();
    return JNI_VERSION_10;
}

/* ─────────────────────────────────────────────────────────────────────────────
   JVM attach/detach RAII helper for WinRT thread-pool threads
   ───────────────────────────────────────────────────────────────────────────── */

/**
 * JniEnvGuard — attaches the current thread to the JVM and detaches it on
 * destruction.  Used inside WinRT event-handler lambdas that run on
 * thread-pool threads not managed by the JVM.
 */
struct JniEnvGuard {
    JNIEnv *env = nullptr;
    bool attached = false;

    /**
     * Constructs a JniEnvGuard.  If the current thread is already attached,
     * uses the existing environment; otherwise attaches and marks for
     * detachment on destruction.
     */
    explicit JniEnvGuard() {
        jint rc = g_jvm->GetEnv(reinterpret_cast<void **>(&env), JNI_VERSION_10);
        if (rc == JNI_EDETACHED) {
            JavaVMAttachArgs args{JNI_VERSION_10, const_cast<char *>("ble-windows-cb"), nullptr};
            if (g_jvm->AttachCurrentThread(reinterpret_cast<void **>(&env), &args) == JNI_OK) {
                attached = true;
            }
        }
    }

    /**
     * Destructs a JniEnvGuard, detaching the thread from the JVM if it was
     * attached by this guard instance.
     */
    ~JniEnvGuard() {
        if (attached) {
            g_jvm->DetachCurrentThread();
        }
    }

    JniEnvGuard(const JniEnvGuard &) = delete;
    JniEnvGuard &operator=(const JniEnvGuard &) = delete;
};

/* ─────────────────────────────────────────────────────────────────────────────
   Native context structs
   ───────────────────────────────────────────────────────────────────────────── */

/**
 * BleContext — holds all state associated with one call to nativeInit.
 *
 * Fields:
 *   callbacksRef       — JNI global reference to the WindowsBleNativeCallbacks
 *                        implementation (WindowsBleScanner).
 *   onDeviceFoundMethod — cached jmethodID for
 *                        onDeviceFound(String,String,int,byte[]).
 *   onNotificationMethod — cached jmethodID for onNotification(long,String,String,[B).
 *   watcher            — the BluetoothLEAdvertisementWatcher for scanning.
 *   scanning           — atomic flag tracking whether a scan is active.
 *   watcherToken       — registration token for the Received event handler.
 */
struct BleContext {
    jobject callbacksRef           = nullptr;
    jmethodID onDeviceFoundMethod  = nullptr;
    jmethodID onNotificationMethod = nullptr;

    BluetoothLEAdvertisementWatcher watcher{nullptr};
    std::atomic<bool> scanning{false};
    event_token watcherToken{};
};

/**
 * BleConnectionContext — holds all state associated with one connected
 * peripheral.
 *
 * Fields:
 *   device             — the connected BluetoothLEDevice.
 *   characteristics    — map from (serviceUuid + "#" + charUuid) to
 *                        GattCharacteristic for quick lookup.
 *   notifyTokens       — registration tokens for ValueChanged handlers,
 *                        keyed by the same composite string.
 *   ctx                — back-pointer to the owning BleContext (for callbacks).
 *   connPtrSelf        — the jlong address of this struct (passed to Java as
 *                        connectionPtr so Java can route notifications back).
 */
struct BleConnectionContext {
    BluetoothLEDevice device{nullptr};
    std::map<std::string, GattCharacteristic> characteristics;
    std::map<std::string, event_token> notifyTokens;
    BleContext *ctx = nullptr;
    jlong connPtrSelf = 0;
};

/* ─────────────────────────────────────────────────────────────────────────────
   Helper: convert jstring to std::wstring
   ───────────────────────────────────────────────────────────────────────────── */

static std::wstring jstringToWstring(JNIEnv *env, jstring js) {
    if (js == nullptr) return {};
    const jchar *chars = env->GetStringChars(js, nullptr);
    const jsize len    = env->GetStringLength(js);
    std::wstring result(reinterpret_cast<const wchar_t *>(chars),
                        static_cast<size_t>(len));
    env->ReleaseStringChars(js, chars);
    return result;
}

/* ─────────────────────────────────────────────────────────────────────────────
   Helper: composite key for characteristic lookup
   ───────────────────────────────────────────────────────────────────────────── */

static std::string charKey(JNIEnv *env, jstring serviceUuid, jstring characteristicUuid) {
    const char *svc = env->GetStringUTFChars(serviceUuid, nullptr);
    const char *chr = env->GetStringUTFChars(characteristicUuid, nullptr);
    std::string key = std::string(svc) + "#" + chr;
    env->ReleaseStringUTFChars(serviceUuid, svc);
    env->ReleaseStringUTFChars(characteristicUuid, chr);
    return key;
}

/* ─────────────────────────────────────────────────────────────────────────────
   JNI entry points
   ───────────────────────────────────────────────────────────────────────────── */

/**
 * nativeInit — allocates a BleContext, stores a JNI global reference to the
 * WindowsBleNativeCallbacks implementation, caches method IDs, and creates a
 * BluetoothLEAdvertisementWatcher.
 *
 * Corresponds to Windows::Devices::Bluetooth::Advertisement::
 *     BluetoothLEAdvertisementWatcher constructor.
 */
JNIEXPORT jlong JNICALL
Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeInit(
        JNIEnv *env, jobject /*self*/, jobject callbacksObj) {

    auto *ctx = new BleContext();
    ctx->callbacksRef = env->NewGlobalRef(callbacksObj);

    /* Cache the two callback method IDs on the BleNativeCallbacks interface. */
    jclass cls = env->GetObjectClass(callbacksObj);
    ctx->onDeviceFoundMethod = env->GetMethodID(
            cls, "onDeviceFound",
            "(Ljava/lang/String;Ljava/lang/String;I[B)V");
    ctx->onNotificationMethod = env->GetMethodID(
            cls, "onNotification", "(JLjava/lang/String;Ljava/lang/String;[B)V");
    env->DeleteLocalRef(cls);

    /* Create the advertisement watcher. */
    ctx->watcher = BluetoothLEAdvertisementWatcher();
    ctx->watcher.ScanningMode(BluetoothLEScanningMode::Active);

    return reinterpret_cast<jlong>(ctx);
}

/**
 * nativeStartScan — registers the Received handler and starts the
 * BluetoothLEAdvertisementWatcher.
 *
 * Corresponds to Windows::Devices::Bluetooth::Advertisement::
 *     BluetoothLEAdvertisementWatcher::Start().
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeStartScan(
        JNIEnv * /*env*/, jobject /*self*/, jlong contextPtr, jstring /*serviceUuid*/) {

    auto *ctx = reinterpret_cast<BleContext *>(contextPtr);
    ctx->scanning.store(true);

    ctx->watcherToken = ctx->watcher.Received(
        [ctx](BluetoothLEAdvertisementWatcher const &,
              BluetoothLEAdvertisementReceivedEventArgs const &args) {

            JniEnvGuard guard;
            if (guard.env == nullptr) return;

            /* Build device ID from Bluetooth address. */
            const uint64_t addr = args.BluetoothAddress();
            wchar_t addrBuf[18];
            swprintf_s(addrBuf, L"%012llX", static_cast<unsigned long long>(addr));
            jstring jid = guard.env->NewString(
                    reinterpret_cast<const jchar *>(addrBuf),
                    static_cast<jsize>(wcslen(addrBuf)));

            /* Device name from advertisement data. */
            hstring localName = args.Advertisement().LocalName();
            jstring jname = guard.env->NewString(
                    reinterpret_cast<const jchar *>(localName.c_str()),
                    static_cast<jsize>(localName.size()));

            jint rssi = static_cast<jint>(args.RawSignalStrengthInDBm());

            /*
             * Extract manufacturer-specific data from the advertisement payload.
             * Windows::Devices::Bluetooth::Advertisement::
             *     BluetoothLEAdvertisementDataSection — type 0xFF per BT spec.
             * We concatenate all manufacturer-data sections into a single byte
             * array (in practice there is at most one, but the API returns a
             * collection).
             */
            std::vector<uint8_t> mfrBytes;
            auto mfrSections = args.Advertisement().ManufacturerData();
            for (auto const &section : mfrSections) {
                /* Each section: 2-byte little-endian company ID + data */
                uint16_t companyId = section.CompanyId();
                mfrBytes.push_back(static_cast<uint8_t>(companyId & 0xFF));
                mfrBytes.push_back(static_cast<uint8_t>((companyId >> 8) & 0xFF));
                auto reader = DataReader::FromBuffer(section.Data());
                uint32_t dataLen = reader.UnconsumedBufferLength();
                std::vector<uint8_t> sectionData(dataLen);
                reader.ReadBytes(sectionData);
                mfrBytes.insert(mfrBytes.end(), sectionData.begin(), sectionData.end());
            }
            jbyteArray jmfr = guard.env->NewByteArray(
                    static_cast<jsize>(mfrBytes.size()));
            if (!mfrBytes.empty()) {
                guard.env->SetByteArrayRegion(jmfr, 0,
                        static_cast<jsize>(mfrBytes.size()),
                        reinterpret_cast<const jbyte *>(mfrBytes.data()));
            }

            guard.env->CallVoidMethod(
                    ctx->callbacksRef, ctx->onDeviceFoundMethod,
                    jid, jname, rssi, jmfr);

            guard.env->DeleteLocalRef(jid);
            guard.env->DeleteLocalRef(jname);
            guard.env->DeleteLocalRef(jmfr);
        });

    ctx->watcher.Start();
}

/**
 * nativeStopScan — stops the BluetoothLEAdvertisementWatcher and removes the
 * Received event handler.
 *
 * Corresponds to Windows::Devices::Bluetooth::Advertisement::
 *     BluetoothLEAdvertisementWatcher::Stop().
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeStopScan(
        JNIEnv * /*env*/, jobject /*self*/, jlong contextPtr) {

    auto *ctx = reinterpret_cast<BleContext *>(contextPtr);
    ctx->watcher.Received(ctx->watcherToken);
    ctx->watcher.Stop();
    ctx->scanning.store(false);
}

/**
 * nativeIsScanning — returns whether the watcher is currently active.
 *
 * Corresponds to reading the Status property of
 *     Windows::Devices::Bluetooth::Advertisement::BluetoothLEAdvertisementWatcher.
 */
JNIEXPORT jboolean JNICALL
Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeIsScanning(
        JNIEnv * /*env*/, jobject /*self*/, jlong contextPtr) {

    auto *ctx = reinterpret_cast<BleContext *>(contextPtr);
    return ctx->scanning.load() ? JNI_TRUE : JNI_FALSE;
}

/**
 * nativeConnect — connects to a peripheral via
 *     Windows::Devices::Bluetooth::BluetoothLEDevice::FromBluetoothAddressAsync(),
 * discovers all GATT services and characteristics, caches them in a new
 * BleConnectionContext, and returns a pointer to it.
 */
JNIEXPORT jlong JNICALL
Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeConnect(
        JNIEnv *env, jobject /*self*/, jlong contextPtr, jstring deviceAddress) {

    auto *ctx = reinterpret_cast<BleContext *>(contextPtr);

    /* Parse address string back to uint64. */
    const char *addrStr = env->GetStringUTFChars(deviceAddress, nullptr);
    uint64_t addr = strtoull(addrStr, nullptr, 16);
    env->ReleaseStringUTFChars(deviceAddress, addrStr);

    auto *conn = new BleConnectionContext();
    conn->ctx = ctx;
    conn->connPtrSelf = reinterpret_cast<jlong>(conn);

    /* Connect. */
    conn->device = BluetoothLEDevice::FromBluetoothAddressAsync(addr).get();
    if (conn->device == nullptr) {
        delete conn;
        return 0L;
    }

    /* Discover services. */
    auto servicesResult = conn->device.GetGattServicesAsync(
            BluetoothCacheMode::Uncached).get();
    if (servicesResult.Status() != GattCommunicationStatus::Success) {
        delete conn;
        return 0L;
    }

    for (auto const &svc : servicesResult.Services()) {
        /* Widen service UUID to string. */
        winrt::hstring svcUuid = winrt::to_hstring(
                winrt::to_string(to_hstring(winrt::guid_string(svc.Uuid()))));

        auto charsResult = svc.GetCharacteristicsAsync(
                BluetoothCacheMode::Uncached).get();
        if (charsResult.Status() != GattCommunicationStatus::Success) {
            continue;
        }
        for (auto const &ch : charsResult.Characteristics()) {
            winrt::hstring chrUuid = winrt::to_hstring(
                    winrt::to_string(to_hstring(winrt::guid_string(ch.Uuid()))));
            std::string key = winrt::to_string(svcUuid) + "#" + winrt::to_string(chrUuid);
            conn->characteristics.emplace(key, ch);
        }
    }

    return conn->connPtrSelf;
}

/**
 * nativeDisconnect — closes the BluetoothLEDevice session and frees the
 * BleConnectionContext.
 *
 * Corresponds to calling Close() on the
 *     Windows::Devices::Bluetooth::BluetoothLEDevice IClosable interface.
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeDisconnect(
        JNIEnv * /*env*/, jobject /*self*/, jlong /*contextPtr*/, jlong connectionPtr) {

    auto *conn = reinterpret_cast<BleConnectionContext *>(connectionPtr);
    if (conn == nullptr) return;

    /* Remove any remaining ValueChanged handlers. */
    for (auto &[key, token] : conn->notifyTokens) {
        auto it = conn->characteristics.find(key);
        if (it != conn->characteristics.end()) {
            it->second.ValueChanged(token);
        }
    }
    conn->notifyTokens.clear();

    /* Close the device. */
    if (conn->device) {
        conn->device.Close();
    }
    delete conn;
}

/**
 * nativeWriteWithoutResponse — writes bytes to a GATT characteristic using
 *     GattWriteOption::WriteWithoutResponse.
 *
 * Corresponds to
 *     Windows::Devices::Bluetooth::GenericAttributeProfile::
 *         GattCharacteristic::WriteValueAsync(IBuffer, GattWriteOption).
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeWriteWithoutResponse(
        JNIEnv *env, jobject /*self*/, jlong connectionPtr,
        jstring serviceUuid, jstring characteristicUuid, jbyteArray data) {

    auto *conn = reinterpret_cast<BleConnectionContext *>(connectionPtr);
    if (conn == nullptr) return;

    const std::string key = charKey(env, serviceUuid, characteristicUuid);
    auto it = conn->characteristics.find(key);
    if (it == conn->characteristics.end()) return;

    const jsize len = env->GetArrayLength(data);
    jbyte *bytes = env->GetByteArrayElements(data, nullptr);

    auto writer = DataWriter();
    writer.WriteBytes({reinterpret_cast<uint8_t *>(bytes),
                       static_cast<uint32_t>(len)});
    env->ReleaseByteArrayElements(data, bytes, JNI_ABORT);

    it->second.WriteValueAsync(writer.DetachBuffer(),
            GattWriteOption::WriteWithoutResponse).get();
}

/**
 * nativeReadCharacteristic — reads the current value of a GATT characteristic
 * via GattCharacteristic::ReadValueAsync() with
 *     Windows::Devices::Bluetooth::BluetoothCacheMode::Uncached.
 */
JNIEXPORT jbyteArray JNICALL
Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeReadCharacteristic(
        JNIEnv *env, jobject /*self*/, jlong connectionPtr,
        jstring serviceUuid, jstring characteristicUuid) {

    auto *conn = reinterpret_cast<BleConnectionContext *>(connectionPtr);
    if (conn == nullptr) return nullptr;

    const std::string key = charKey(env, serviceUuid, characteristicUuid);
    auto it = conn->characteristics.find(key);
    if (it == conn->characteristics.end()) return nullptr;

    auto readResult = it->second.ReadValueAsync(
            BluetoothCacheMode::Uncached).get();
    if (readResult.Status() != GattCommunicationStatus::Success) return nullptr;

    auto reader = DataReader::FromBuffer(readResult.Value());
    const uint32_t len = reader.UnconsumedBufferLength();
    std::vector<uint8_t> buf(len);
    reader.ReadBytes(buf);

    jbyteArray jbuf = env->NewByteArray(static_cast<jsize>(len));
    env->SetByteArrayRegion(jbuf, 0, static_cast<jsize>(len),
            reinterpret_cast<const jbyte *>(buf.data()));
    return jbuf;
}

/**
 * nativeSetNotify — enables or disables GATT notifications by writing the
 * Client Characteristic Configuration Descriptor (CCCD) and registering or
 * removing a ValueChanged event handler.
 *
 * Corresponds to
 *     Windows::Devices::Bluetooth::GenericAttributeProfile::
 *         GattCharacteristic::WriteClientCharacteristicConfigurationDescriptorAsync().
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeSetNotify(
        JNIEnv *env, jobject /*self*/, jlong connectionPtr,
        jstring serviceUuid, jstring characteristicUuid, jboolean enable) {

    auto *conn = reinterpret_cast<BleConnectionContext *>(connectionPtr);
    if (conn == nullptr) return;

    const std::string key = charKey(env, serviceUuid, characteristicUuid);
    auto it = conn->characteristics.find(key);
    if (it == conn->characteristics.end()) return;

    if (enable == JNI_TRUE) {
        it->second.WriteClientCharacteristicConfigurationDescriptorAsync(
                GattClientCharacteristicConfigurationDescriptorValue::Notify).get();

        /* Capture raw pointers/values that are safe to use across threads. */
        jlong connPtrCopy  = conn->connPtrSelf;
        BleContext *ctxCopy = conn->ctx;

        /* Build ANSI key strings for the callback. */
        const char *svcUtf = env->GetStringUTFChars(serviceUuid, nullptr);
        const char *chrUtf = env->GetStringUTFChars(characteristicUuid, nullptr);
        std::string svcStr(svcUtf);
        std::string chrStr(chrUtf);
        env->ReleaseStringUTFChars(serviceUuid, svcUtf);
        env->ReleaseStringUTFChars(characteristicUuid, chrUtf);

        event_token token = it->second.ValueChanged(
            [connPtrCopy, ctxCopy, svcStr, chrStr]
            (GattCharacteristic const &, GattValueChangedEventArgs const &args) {

                JniEnvGuard guard;
                if (guard.env == nullptr) return;

                /* Convert IBuffer to jbyteArray. */
                auto reader = DataReader::FromBuffer(args.CharacteristicValue());
                const uint32_t len = reader.UnconsumedBufferLength();
                std::vector<uint8_t> buf(len);
                reader.ReadBytes(buf);

                jbyteArray jbuf = guard.env->NewByteArray(static_cast<jsize>(len));
                guard.env->SetByteArrayRegion(jbuf, 0, static_cast<jsize>(len),
                        reinterpret_cast<const jbyte *>(buf.data()));

                jstring jsvc = guard.env->NewStringUTF(svcStr.c_str());
                jstring jchr = guard.env->NewStringUTF(chrStr.c_str());

                guard.env->CallVoidMethod(
                        ctxCopy->callbacksRef,
                        ctxCopy->onNotificationMethod,
                        connPtrCopy, jsvc, jchr, jbuf);

                guard.env->DeleteLocalRef(jbuf);
                guard.env->DeleteLocalRef(jsvc);
                guard.env->DeleteLocalRef(jchr);
            });

        conn->notifyTokens.emplace(key, token);

    } else {
        auto tokenIt = conn->notifyTokens.find(key);
        if (tokenIt != conn->notifyTokens.end()) {
            it->second.ValueChanged(tokenIt->second);
            conn->notifyTokens.erase(tokenIt);
        }
        it->second.WriteClientCharacteristicConfigurationDescriptorAsync(
                GattClientCharacteristicConfigurationDescriptorValue::None).get();
    }
}

/**
 * nativeDestroy — releases all WinRT resources held by the BleContext:
 * stops the advertisement watcher, deletes the JNI global reference to the
 * callbacks object, and frees the BleContext struct.
 *
 * Corresponds to calling Close() on the
 *     Windows::Devices::Bluetooth::Advertisement::BluetoothLEAdvertisementWatcher
 * IClosable interface.
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeDestroy(
        JNIEnv *env, jobject /*self*/, jlong contextPtr) {

    auto *ctx = reinterpret_cast<BleContext *>(contextPtr);
    if (ctx == nullptr) return;

    if (ctx->scanning.load()) {
        ctx->watcher.Received(ctx->watcherToken);
        ctx->watcher.Stop();
        ctx->scanning.store(false);
    }

    if (ctx->callbacksRef != nullptr) {
        env->DeleteGlobalRef(ctx->callbacksRef);
        ctx->callbacksRef = nullptr;
    }
    delete ctx;
}
