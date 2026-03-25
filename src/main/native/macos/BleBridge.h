/**
 * BleBridge.h — JNI entry-point declarations for the macOS CoreBluetooth bridge.
 *
 * Every function declared here is implemented in BleBridge.m and is registered
 * as a native method in JniNativeBridge via javac-generated JNI headers.
 *
 * CoreBluetooth API used:
 *   CBCentralManager            — scan start / stop, peripheral connect / disconnect
 *   CBPeripheral                — service discovery, characteristic read / write / notify
 *   CBCentralManagerDelegate    — async callbacks from the BLE stack
 *   CBPeripheralDelegate        — async callbacks from a connected peripheral
 */

#ifndef BLE_BRIDGE_H
#define BLE_BRIDGE_H

#import <Foundation/Foundation.h>
#include <jni.h>

#ifdef __cplusplus
extern "C" {
#endif

/**
 * nativeInit — initialises the CoreBluetooth central manager.
 *
 * Maps to: JniNativeBridge.nativeInit(BleNativeCallbacks callbacks)
 *
 * callbacksObj is the BleNativeCallbacks implementation (MacOsBleScanner) whose
 * onDeviceFound(String,String,int) and onNotification(long,String,String,[B) methods
 * are called by CoreBluetooth dispatch-queue threads.  A JNI global reference to
 * it is stored in BleContext for the lifetime of the context.
 *
 * Returns a jlong that is the pointer to the heap-allocated BleContext struct.
 * The Java side stores this opaque handle and passes it back on every subsequent call.
 */
JNIEXPORT jlong JNICALL
Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeInit(
        JNIEnv *env, jobject self, jobject callbacksObj);

/**
 * nativeStartScan — asks CBCentralManager to scan for peripherals.
 *
 * Maps to: JniNativeBridge.nativeStartScan(long contextPtr, String serviceUuid)
 *
 * @param contextPtr  opaque pointer returned by nativeInit
 * @param serviceUuid 128-bit UUID string to filter on, or null for no filter
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeStartScan(
        JNIEnv *env, jobject self, jlong contextPtr, jstring serviceUuid);

/**
 * nativeStopScan — asks CBCentralManager to stop scanning.
 *
 * Maps to: JniNativeBridge.nativeStopScan(long contextPtr)
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeStopScan(
        JNIEnv *env, jobject self, jlong contextPtr);

/**
 * nativeIsScanning — returns whether the central manager is currently scanning.
 *
 * Maps to: JniNativeBridge.nativeIsScanning(long contextPtr)
 */
JNIEXPORT jboolean JNICALL
Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeIsScanning(
        JNIEnv *env, jobject self, jlong contextPtr);

/**
 * nativeConnect — initiates a BLE connection to the peripheral identified by
 * the given UUID string.
 *
 * Maps to: JniNativeBridge.nativeConnect(long contextPtr, String peripheralUuid)
 *
 * Returns a jlong that is the pointer to the heap-allocated BleConnectionContext
 * struct for the connected peripheral.
 */
JNIEXPORT jlong JNICALL
Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeConnect(
        JNIEnv *env, jobject self, jlong contextPtr, jstring peripheralUuid);

/**
 * nativeDisconnect — disconnects from the peripheral and releases its resources.
 *
 * Maps to: JniNativeBridge.nativeDisconnect(long contextPtr, long connectionPtr)
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeDisconnect(
        JNIEnv *env, jobject self, jlong contextPtr, jlong connectionPtr);

/**
 * nativeWriteWithoutResponse — writes bytes to a GATT characteristic without
 * requesting a Write Response ATT PDU (CBCharacteristicWriteWithoutResponse).
 *
 * Maps to: JniNativeBridge.nativeWriteWithoutResponse(
 *              long connectionPtr, String serviceUuid, String characteristicUuid,
 *              byte[] data)
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeWriteWithoutResponse(
        JNIEnv *env, jobject self, jlong connectionPtr,
        jstring serviceUuid, jstring characteristicUuid, jbyteArray data);

/**
 * nativeReadCharacteristic — reads the current value of a GATT characteristic.
 *
 * Maps to: JniNativeBridge.nativeReadCharacteristic(
 *              long connectionPtr, String serviceUuid, String characteristicUuid)
 *
 * Returns a jbyteArray containing the characteristic value, or null on error.
 */
JNIEXPORT jbyteArray JNICALL
Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeReadCharacteristic(
        JNIEnv *env, jobject self, jlong connectionPtr,
        jstring serviceUuid, jstring characteristicUuid);

/**
 * nativeSetNotify — enables or disables GATT notifications for a characteristic.
 *
 * Maps to: JniNativeBridge.nativeSetNotify(
 *              long connectionPtr, String serviceUuid, String characteristicUuid,
 *              boolean enable)
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeSetNotify(
        JNIEnv *env, jobject self, jlong connectionPtr,
        jstring serviceUuid, jstring characteristicUuid, jboolean enable);

/**
 * nativeDestroy — releases all CoreBluetooth resources held by the BleContext
 * created in nativeInit.
 *
 * Maps to: JniNativeBridge.nativeDestroy(long contextPtr)
 */
JNIEXPORT void JNICALL
Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeDestroy(
        JNIEnv *env, jobject self, jlong contextPtr);

#ifdef __cplusplus
}
#endif

#endif /* BLE_BRIDGE_H */
