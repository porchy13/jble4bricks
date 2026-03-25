/*
 * BleBridge.h — JNI function declarations for the Linux BlueZ BLE bridge.
 *
 * Each function corresponds to a native method declared on
 * LinuxJniNativeBridge.java and is implemented in BleBridge.c using
 * GLib/GDBus to communicate with BlueZ over the D-Bus system bus.
 *
 * Naming convention follows the JNI mangled-name spec:
 *   Java_<package_underscored>_<ClassName>_<methodName>
 */

#ifndef BLE_BRIDGE_H
#define BLE_BRIDGE_H

#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * Allocates a BleContext, connects to the D-Bus system bus, acquires the
 * BlueZ org.bluez well-known name, and starts the GLib main loop on a
 * dedicated thread so that D-Bus signal callbacks can fire.
 *
 * Corresponds to LinuxJniNativeBridge.nativeInit().
 *
 * @param env       JNI environment pointer
 * @param obj       instance of LinuxJniNativeBridge (unused)
 * @param callbacks jobject implementing LinuxBleNativeCallbacks; stored as a
 *                  JNI global reference inside BleContext for the lifetime of
 *                  the context
 * @return opaque pointer to the allocated BleContext cast to jlong
 */
JNIEXPORT jlong JNICALL
Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeInit(
        JNIEnv *env, jobject obj, jobject callbacks);

/**
 * Starts a BLE discovery session via org.bluez.Adapter1.StartDiscovery,
 * optionally filtered to a specific GATT service UUID.
 *
 * Subscribes to org.freedesktop.DBus.ObjectManager.InterfacesAdded signals
 * filtered for org.bluez.Device1 objects.
 *
 * Corresponds to LinuxJniNativeBridge.nativeStartScan().
 *
 * @param env         JNI environment pointer
 * @param obj         instance of LinuxJniNativeBridge (unused)
 * @param ctxPtr      opaque pointer to BleContext
 * @param serviceUuid jstring UUID filter, or NULL for no filter
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeStartScan(
        JNIEnv *env, jobject obj, jlong ctxPtr, jstring serviceUuid);

/**
 * Stops the active BLE discovery session via
 * org.bluez.Adapter1.StopDiscovery.
 *
 * Corresponds to LinuxJniNativeBridge.nativeStopScan().
 *
 * @param env    JNI environment pointer
 * @param obj    instance of LinuxJniNativeBridge (unused)
 * @param ctxPtr opaque pointer to BleContext
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeStopScan(
        JNIEnv *env, jobject obj, jlong ctxPtr);

/**
 * Returns whether a BlueZ discovery session is currently active.
 *
 * Corresponds to LinuxJniNativeBridge.nativeIsScanning().
 *
 * @param env    JNI environment pointer
 * @param obj    instance of LinuxJniNativeBridge (unused)
 * @param ctxPtr opaque pointer to BleContext
 * @return JNI_TRUE if scanning, JNI_FALSE otherwise
 */
JNIEXPORT jboolean JNICALL
Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeIsScanning(
        JNIEnv *env, jobject obj, jlong ctxPtr);

/**
 * Connects to the BlueZ device at the given D-Bus object path via
 * org.bluez.Device1.Connect, then discovers GATT services and
 * characteristics via GetManagedObjects.
 *
 * Corresponds to LinuxJniNativeBridge.nativeConnect().
 *
 * @param env        JNI environment pointer
 * @param obj        instance of LinuxJniNativeBridge (unused)
 * @param ctxPtr     opaque pointer to BleContext
 * @param devicePath jstring D-Bus object path of the device
 * @return opaque pointer to the allocated BleConnectionContext cast to jlong
 */
JNIEXPORT jlong JNICALL
Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeConnect(
        JNIEnv *env, jobject obj, jlong ctxPtr, jstring devicePath);

/**
 * Disconnects from the device via org.bluez.Device1.Disconnect and frees
 * the BleConnectionContext.
 *
 * Corresponds to LinuxJniNativeBridge.nativeDisconnect().
 *
 * @param env           JNI environment pointer
 * @param obj           instance of LinuxJniNativeBridge (unused)
 * @param ctxPtr        opaque pointer to BleContext
 * @param connectionPtr opaque pointer to BleConnectionContext
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeDisconnect(
        JNIEnv *env, jobject obj, jlong ctxPtr, jlong connectionPtr);

/**
 * Writes bytes to a GATT characteristic via
 * org.bluez.GattCharacteristic1.WriteValue with the write-without-response
 * option.
 *
 * Corresponds to LinuxJniNativeBridge.nativeWriteWithoutResponse().
 *
 * @param env                JNI environment pointer
 * @param obj                instance of LinuxJniNativeBridge (unused)
 * @param connectionPtr      opaque pointer to BleConnectionContext
 * @param serviceUuid        jstring GATT service UUID
 * @param characteristicUuid jstring GATT characteristic UUID
 * @param data               jbyteArray payload to write
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeWriteWithoutResponse(
        JNIEnv *env, jobject obj, jlong connectionPtr,
        jstring serviceUuid, jstring characteristicUuid, jbyteArray data);

/**
 * Reads the current value of a GATT characteristic via
 * org.bluez.GattCharacteristic1.ReadValue.
 *
 * Corresponds to LinuxJniNativeBridge.nativeReadCharacteristic().
 *
 * @param env                JNI environment pointer
 * @param obj                instance of LinuxJniNativeBridge (unused)
 * @param connectionPtr      opaque pointer to BleConnectionContext
 * @param serviceUuid        jstring GATT service UUID
 * @param characteristicUuid jstring GATT characteristic UUID
 * @return jbyteArray containing the characteristic value, or NULL on failure
 */
JNIEXPORT jbyteArray JNICALL
Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeReadCharacteristic(
        JNIEnv *env, jobject obj, jlong connectionPtr,
        jstring serviceUuid, jstring characteristicUuid);

/**
 * Enables or disables GATT notifications by calling
 * org.bluez.GattCharacteristic1.StartNotify or
 * org.bluez.GattCharacteristic1.StopNotify.
 *
 * Corresponds to LinuxJniNativeBridge.nativeSetNotify().
 *
 * @param env                JNI environment pointer
 * @param obj                instance of LinuxJniNativeBridge (unused)
 * @param connectionPtr      opaque pointer to BleConnectionContext
 * @param serviceUuid        jstring GATT service UUID
 * @param characteristicUuid jstring GATT characteristic UUID
 * @param enable             JNI_TRUE to enable, JNI_FALSE to disable
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeSetNotify(
        JNIEnv *env, jobject obj, jlong connectionPtr,
        jstring serviceUuid, jstring characteristicUuid, jboolean enable);

/**
 * Destroys the BleContext: stops the GLib main loop, releases the D-Bus
 * connection, and frees all heap memory.
 *
 * Corresponds to LinuxJniNativeBridge.nativeDestroy().
 *
 * @param env    JNI environment pointer
 * @param obj    instance of LinuxJniNativeBridge (unused)
 * @param ctxPtr opaque pointer to BleContext
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeDestroy(
        JNIEnv *env, jobject obj, jlong ctxPtr);

#ifdef __cplusplus
}
#endif

#endif /* BLE_BRIDGE_H */
