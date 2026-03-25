/**
 * BleBridge.h — JNI entry-point declarations for the Windows WinRT
 * Bluetooth LE bridge.
 *
 * Every function declared here is implemented in BleBridge.cpp and is
 * registered as a native method in WindowsJniNativeBridge via javac-generated
 * JNI headers.
 *
 * WinRT API used:
 *   Windows::Devices::Bluetooth::BluetoothLEDevice
 *       — connect to a peripheral, enumerate GATT services/characteristics
 *   Windows::Devices::Bluetooth::Advertisement::BluetoothLEAdvertisementWatcher
 *       — scan for BLE peripherals
 *   Windows::Devices::Bluetooth::GenericAttributeProfile::GattCharacteristic
 *       — read, write-without-response, enable/disable notifications
 */

#ifndef BLE_BRIDGE_H
#define BLE_BRIDGE_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * nativeInit — initialises the WinRT Bluetooth LE adapter context.
 *
 * Maps to: WindowsJniNativeBridge.nativeInit(WindowsBleNativeCallbacks)
 *
 * callbacksObj is the WindowsBleNativeCallbacks implementation
 * (WindowsBleScanner) whose onDeviceFound(String,String,int) and
 * onNotification(long,String,String,[B) methods are called by WinRT
 * thread-pool threads.  A JNI global reference to it is stored in BleContext
 * for the lifetime of the context.
 *
 * Returns a jlong that is the pointer to the heap-allocated BleContext struct.
 * The Java side stores this opaque handle and passes it back on every
 * subsequent call.
 */
JNIEXPORT jlong JNICALL
Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeInit(
        JNIEnv *env, jobject self, jobject callbacksObj);

/**
 * nativeStartScan — starts a BluetoothLEAdvertisementWatcher scan.
 *
 * Maps to: WindowsJniNativeBridge.nativeStartScan(long, String)
 *
 * @param contextPtr  opaque pointer returned by nativeInit
 * @param serviceUuid 128-bit UUID string to filter on, or null for no filter
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeStartScan(
        JNIEnv *env, jobject self, jlong contextPtr, jstring serviceUuid);

/**
 * nativeStopScan — stops the BluetoothLEAdvertisementWatcher.
 *
 * Maps to: WindowsJniNativeBridge.nativeStopScan(long)
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeStopScan(
        JNIEnv *env, jobject self, jlong contextPtr);

/**
 * nativeIsScanning — returns whether the watcher is currently scanning.
 *
 * Maps to: WindowsJniNativeBridge.nativeIsScanning(long)
 */
JNIEXPORT jboolean JNICALL
Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeIsScanning(
        JNIEnv *env, jobject self, jlong contextPtr);

/**
 * nativeConnect — connects to the peripheral at the given BLE address and
 * returns a connection context pointer.
 *
 * Maps to: WindowsJniNativeBridge.nativeConnect(long, String)
 *
 * Uses BluetoothLEDevice::FromBluetoothAddressAsync() and
 * GattDeviceService::GetCharacteristicsAsync().
 *
 * Returns a jlong that is the pointer to the heap-allocated
 * BleConnectionContext struct.
 */
JNIEXPORT jlong JNICALL
Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeConnect(
        JNIEnv *env, jobject self, jlong contextPtr, jstring deviceAddress);

/**
 * nativeDisconnect — closes the BLE connection and releases the
 * BleConnectionContext.
 *
 * Maps to: WindowsJniNativeBridge.nativeDisconnect(long, long)
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeDisconnect(
        JNIEnv *env, jobject self, jlong contextPtr, jlong connectionPtr);

/**
 * nativeWriteWithoutResponse — writes bytes to a GATT characteristic using
 * GattWriteOption::WriteWithoutResponse.
 *
 * Maps to: WindowsJniNativeBridge.nativeWriteWithoutResponse(
 *              long, String, String, byte[])
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeWriteWithoutResponse(
        JNIEnv *env, jobject self, jlong connectionPtr,
        jstring serviceUuid, jstring characteristicUuid, jbyteArray data);

/**
 * nativeReadCharacteristic — reads the current value of a GATT
 * characteristic via GattCharacteristic::ReadValueAsync().
 *
 * Maps to: WindowsJniNativeBridge.nativeReadCharacteristic(
 *              long, String, String)
 *
 * Returns a jbyteArray containing the characteristic value, or null on error.
 */
JNIEXPORT jbyteArray JNICALL
Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeReadCharacteristic(
        JNIEnv *env, jobject self, jlong connectionPtr,
        jstring serviceUuid, jstring characteristicUuid);

/**
 * nativeSetNotify — enables or disables GATT notifications by writing the
 * Client Characteristic Configuration Descriptor (CCCD) via
 * GattCharacteristic::WriteClientCharacteristicConfigurationDescriptorAsync().
 *
 * Maps to: WindowsJniNativeBridge.nativeSetNotify(
 *              long, String, String, boolean)
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeSetNotify(
        JNIEnv *env, jobject self, jlong connectionPtr,
        jstring serviceUuid, jstring characteristicUuid, jboolean enable);

/**
 * nativeDestroy — releases all WinRT BLE resources held by the BleContext
 * created in nativeInit.
 *
 * Maps to: WindowsJniNativeBridge.nativeDestroy(long)
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeDestroy(
        JNIEnv *env, jobject self, jlong contextPtr);

#ifdef __cplusplus
}
#endif

#endif /* BLE_BRIDGE_H */
