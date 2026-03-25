package ch.varani.bricks.ble.impl.macos;

import org.jspecify.annotations.NonNull;

/**
 * Callback seam between the CoreBluetooth JNI layer and the Java scanner/connection
 * objects that own the BLE context.
 *
 * <p>The native layer stores a JNI global reference to the concrete implementation
 * of this interface and invokes its methods from the CoreBluetooth dispatch queue
 * (a JNI-attached background thread).
 *
 * <p>This interface is intentionally scoped to {@code impl/macos/}. Each platform
 * implementation defines its own equivalent callback seam in its own package
 * (e.g. {@code impl/windows/}, {@code impl/linux/}), so no platform type ever
 * leaks into another platform's package or into the public API.
 *
 * <p>Thread safety: implementations must be safe for concurrent invocation from
 * any JNI-attached thread.
 *
 * @since 1.0
 */
interface BleNativeCallbacks {

    /**
     * Called by the native layer each time a BLE advertisement is received during
     * a scan.
     *
     * <p>Corresponds to
     * {@code centralManager:didDiscoverPeripheral:advertisementData:RSSI:} in
     * {@code BleBridge.m}.
     *
     * @param id   the peripheral's CoreBluetooth UUID string
     * @param name the advertised local name (empty string if absent)
     * @param rssi received signal strength in dBm
     */
    void onDeviceFound(@NonNull String id, @NonNull String name, int rssi);

    /**
     * Called by the native layer when a GATT notification arrives for a subscribed
     * characteristic.
     *
     * <p>Corresponds to
     * {@code peripheral:didUpdateValueForCharacteristic:error:} in
     * {@code BleBridge.m} when {@code characteristic.isNotifying} is {@code YES}.
     *
     * @param connectionPtr      opaque pointer to the native {@code BleConnectionContext}
     *                           that received the notification; used to dispatch the
     *                           value to the correct {@link MacOsBleConnection} instance
     * @param serviceUuid        the service UUID string
     * @param characteristicUuid the characteristic UUID string
     * @param value              the notification payload bytes
     */
    void onNotification(long connectionPtr,
                        @NonNull String serviceUuid,
                        @NonNull String characteristicUuid,
                        byte[] value);
}
