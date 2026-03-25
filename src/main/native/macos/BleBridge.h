/**
 * BleBridge.h — JNI entry-point declarations for the macOS CoreBluetooth bridge.
 *
 * Every function declared here is implemented in BleBridge.m and is registered
 * as a native method in MacOsBleScanner via javac-generated JNI headers.
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
 * Maps to: MacOsBleScanner.nativeInit()
 *
 * Returns a jlong that is the pointer to the heap-allocated Objective-C
 * BleContext object.  The Java side stores this opaque handle and passes it
 * back on every subsequent call.
 */
JNIEXPORT jlong JNICALL
Java_ch_varani_lego_ble_impl_macos_MacOsBleScanner_nativeInit(JNIEnv *env, jobject self);

/**
 * nativeStartScan — asks CBCentralManager to scan for peripherals.
 *
 * Maps to: MacOsBleScanner.nativeStartScan(long contextPtr, String serviceUuid)
 *
 * @param contextPtr  opaque pointer returned by nativeInit
 * @param serviceUuid 128-bit UUID string to filter on, or null for no filter
 */
JNIEXPORT void JNICALL
Java_ch_varani_lego_ble_impl_macos_MacOsBleScanner_nativeStartScan(
        JNIEnv *env, jobject self, jlong contextPtr, jstring serviceUuid);

/**
 * nativeStopScan — asks CBCentralManager to stop scanning.
 *
 * Maps to: MacOsBleScanner.nativeStopScan(long contextPtr)
 */
JNIEXPORT void JNICALL
Java_ch_varani_lego_ble_impl_macos_MacOsBleScanner_nativeStopScan(
        JNIEnv *env, jobject self, jlong contextPtr);

/**
 * nativeIsScanning — returns whether the central manager is currently scanning.
 *
 * Maps to: MacOsBleScanner.nativeIsScanning(long contextPtr)
 */
JNIEXPORT jboolean JNICALL
Java_ch_varani_lego_ble_impl_macos_MacOsBleScanner_nativeIsScanning(
        JNIEnv *env, jobject self, jlong contextPtr);

/**
 * nativeConnect — initiates a BLE connection to the peripheral identified by
 * the given UUID string.
 *
 * Maps to: MacOsBleScanner.nativeConnect(long contextPtr, String peripheralUuid)
 *
 * Returns a jlong that is the pointer to the heap-allocated Objective-C
 * BleConnection object for the connected peripheral.
 */
JNIEXPORT jlong JNICALL
Java_ch_varani_lego_ble_impl_macos_MacOsBleScanner_nativeConnect(
        JNIEnv *env, jobject self, jlong contextPtr, jstring peripheralUuid);

/**
 * nativeDisconnect — disconnects from the peripheral and releases its resources.
 *
 * Maps to: MacOsBleScanner.nativeDisconnect(long contextPtr, long connectionPtr)
 */
JNIEXPORT void JNICALL
Java_ch_varani_lego_ble_impl_macos_MacOsBleScanner_nativeDisconnect(
        JNIEnv *env, jobject self, jlong contextPtr, jlong connectionPtr);

/**
 * nativeWriteWithoutResponse — writes bytes to a GATT characteristic without
 * requesting a Write Response ATT PDU (CBCharacteristicWriteWithoutResponse).
 *
 * Maps to: MacOsBleScanner.nativeWriteWithoutResponse(
 *              long connectionPtr, String serviceUuid, String characteristicUuid,
 *              byte[] data)
 */
JNIEXPORT void JNICALL
Java_ch_varani_lego_ble_impl_macos_MacOsBleScanner_nativeWriteWithoutResponse(
        JNIEnv *env, jobject self, jlong connectionPtr,
        jstring serviceUuid, jstring characteristicUuid, jbyteArray data);

/**
 * nativeReadCharacteristic — reads the current value of a GATT characteristic.
 *
 * Maps to: MacOsBleScanner.nativeReadCharacteristic(
 *              long connectionPtr, String serviceUuid, String characteristicUuid)
 *
 * Returns a jbyteArray containing the characteristic value, or null on error.
 */
JNIEXPORT jbyteArray JNICALL
Java_ch_varani_lego_ble_impl_macos_MacOsBleScanner_nativeReadCharacteristic(
        JNIEnv *env, jobject self, jlong connectionPtr,
        jstring serviceUuid, jstring characteristicUuid);

/**
 * nativeSetNotify — enables or disables GATT notifications for a characteristic.
 *
 * Maps to: MacOsBleScanner.nativeSetNotify(
 *              long connectionPtr, String serviceUuid, String characteristicUuid,
 *              boolean enable)
 */
JNIEXPORT void JNICALL
Java_ch_varani_lego_ble_impl_macos_MacOsBleScanner_nativeSetNotify(
        JNIEnv *env, jobject self, jlong connectionPtr,
        jstring serviceUuid, jstring characteristicUuid, jboolean enable);

/**
 * nativeDestroy — releases all CoreBluetooth resources held by the BleContext
 * created in nativeInit.
 *
 * Maps to: MacOsBleScanner.nativeDestroy(long contextPtr)
 */
JNIEXPORT void JNICALL
Java_ch_varani_lego_ble_impl_macos_MacOsBleScanner_nativeDestroy(
        JNIEnv *env, jobject self, jlong contextPtr);

#ifdef __cplusplus
}
#endif

#endif /* BLE_BRIDGE_H */
