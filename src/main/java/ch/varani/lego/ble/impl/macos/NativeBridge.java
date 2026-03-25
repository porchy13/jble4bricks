package ch.varani.lego.ble.impl.macos;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Abstraction over the CoreBluetooth JNI calls made by {@link MacOsBleScanner}.
 *
 * <p>The production implementation ({@link JniNativeBridge}) delegates every
 * method to the corresponding {@code native} method declared on
 * {@link JniNativeBridge}.  Tests inject a Mockito mock of this interface so
 * that the native shared library is never loaded during unit testing.
 *
 * <p>All pointer parameters represent opaque C pointers cast to {@code long}.
 * They must never be interpreted as numeric values by Java code.
 *
 * @since 1.0
 */
interface NativeBridge {

    /**
     * Allocates a {@code BleContext} struct, creates a {@code CBCentralManager}
     * on a dedicated serial dispatch queue, and waits for the adapter to reach
     * {@code CBManagerStatePoweredOn}.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeInit}
     * in {@code BleBridge.m}.
     *
     * @return an opaque pointer to the allocated {@code BleContext}, cast to {@code long}
     */
    long init();

    /**
     * Starts a BLE scan via {@code CBCentralManager scanForPeripheralsWithServices:options:}.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeStartScan}
     * in {@code BleBridge.m}.
     *
     * @param ctxPtr      opaque pointer to the {@code BleContext}
     * @param serviceUuid UUID string to filter on, or {@code null} for no filter
     */
    void startScan(long ctxPtr, @Nullable String serviceUuid);

    /**
     * Stops the active BLE scan via {@code CBCentralManager stopScan}.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeStopScan}
     * in {@code BleBridge.m}.
     *
     * @param ctxPtr opaque pointer to the {@code BleContext}
     */
    void stopScan(long ctxPtr);

    /**
     * Returns whether {@code CBCentralManager} is currently scanning.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeIsScanning}
     * in {@code BleBridge.m}.
     *
     * @param ctxPtr opaque pointer to the {@code BleContext}
     * @return {@code true} if a scan is active
     */
    boolean isScanning(long ctxPtr);

    /**
     * Connects to the peripheral identified by {@code peripheralUuid}, performs
     * GATT service and characteristic discovery, and returns a connection pointer.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeConnect}
     * in {@code BleBridge.m}, using
     * {@code CBCentralManager connectPeripheral:options:} and
     * {@code CBPeripheral discoverServices:}.
     *
     * @param ctxPtr         opaque pointer to the {@code BleContext}
     * @param peripheralUuid the peripheral's CoreBluetooth UUID string
     * @return an opaque pointer to the allocated {@code BleConnectionContext}
     */
    long connect(long ctxPtr, @NonNull String peripheralUuid);

    /**
     * Disconnects from a peripheral and frees the {@code BleConnectionContext}.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeDisconnect}
     * in {@code BleBridge.m}, using
     * {@code CBCentralManager cancelPeripheralConnection:}.
     *
     * @param ctxPtr        opaque pointer to the {@code BleContext}
     * @param connectionPtr opaque pointer to the {@code BleConnectionContext}
     */
    void disconnect(long ctxPtr, long connectionPtr);

    /**
     * Writes bytes to a GATT characteristic using
     * {@code CBCharacteristicWriteWithoutResponse}.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeWriteWithoutResponse}
     * in {@code BleBridge.m}, using
     * {@code CBPeripheral writeValue:forCharacteristic:type:}.
     *
     * @param connectionPtr      opaque pointer to the {@code BleConnectionContext}
     * @param serviceUuid        GATT service UUID string
     * @param characteristicUuid GATT characteristic UUID string
     * @param data               bytes to write
     */
    void writeWithoutResponse(
            long connectionPtr,
            @NonNull String serviceUuid,
            @NonNull String characteristicUuid,
            @NonNull byte[] data);

    /**
     * Reads the current value of a GATT characteristic via
     * {@code CBPeripheral readValueForCharacteristic:}.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeReadCharacteristic}
     * in {@code BleBridge.m}.
     *
     * @param connectionPtr      opaque pointer to the {@code BleConnectionContext}
     * @param serviceUuid        GATT service UUID string
     * @param characteristicUuid GATT characteristic UUID string
     * @return the characteristic value bytes, or {@code null} on failure
     */
    @Nullable
    byte[] readCharacteristic(
            long connectionPtr,
            @NonNull String serviceUuid,
            @NonNull String characteristicUuid);

    /**
     * Enables or disables GATT notifications via
     * {@code CBPeripheral setNotifyValue:forCharacteristic:}.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeSetNotify}
     * in {@code BleBridge.m}.
     *
     * @param connectionPtr      opaque pointer to the {@code BleConnectionContext}
     * @param serviceUuid        GATT service UUID string
     * @param characteristicUuid GATT characteristic UUID string
     * @param enable             {@code true} to enable, {@code false} to disable
     */
    void setNotify(
            long connectionPtr,
            @NonNull String serviceUuid,
            @NonNull String characteristicUuid,
            boolean enable);

    /**
     * Destroys the {@code BleContext} and releases all CoreBluetooth resources.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_lego_ble_impl_macos_JniNativeBridge_nativeDestroy}
     * in {@code BleBridge.m}.
     *
     * @param ctxPtr opaque pointer to the {@code BleContext}
     */
    void destroy(long ctxPtr);
}
