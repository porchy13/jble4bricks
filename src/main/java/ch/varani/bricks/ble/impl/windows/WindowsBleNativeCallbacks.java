package ch.varani.bricks.ble.impl.windows;

import org.jspecify.annotations.NonNull;

/**
 * Callback seam between the WinRT Bluetooth LE JNI layer and the Java
 * scanner/connection objects that own the BLE context.
 *
 * <p>The native layer stores a JNI global reference to the concrete
 * implementation of this interface and invokes its methods from the WinRT
 * thread-pool thread on which the BLE event arrives (a JNI-attached
 * background thread).
 *
 * <p>This interface is intentionally scoped to {@code impl/windows/}. Each
 * platform implementation defines its own equivalent callback seam in its own
 * package (e.g. {@code impl/macos/}, {@code impl/linux/}), so no platform
 * type ever leaks into another platform's package or into the public API.
 *
 * <p>Thread safety: implementations must be safe for concurrent invocation
 * from any JNI-attached thread.
 *
 * @since 1.0
 */
interface WindowsBleNativeCallbacks {

    /**
     * Called by the native layer each time a BLE advertisement is received
     * during a scan.
     *
     * <p>Corresponds to the
     * {@code BluetoothLEAdvertisementWatcher.Received} event handler
     * in {@code BleBridge.cpp}.
     *
     * @param id   the peripheral's BLE address as a hex string
     * @param name the advertised local name (empty string if absent)
     * @param rssi received signal strength in dBm
     */
    void onDeviceFound(@NonNull String id, @NonNull String name, int rssi);

    /**
     * Called by the native layer when a GATT notification arrives for a
     * subscribed characteristic.
     *
     * <p>Corresponds to the
     * {@code GattCharacteristic.ValueChanged} event handler
     * in {@code BleBridge.cpp} when notifications are enabled.
     *
     * @param connectionPtr      opaque pointer to the native
     *                           {@code BleConnectionContext} that received
     *                           the notification; used to dispatch the value
     *                           to the correct {@link WindowsBleConnection}
     *                           instance
     * @param serviceUuid        the service UUID string
     * @param characteristicUuid the characteristic UUID string
     * @param value              the notification payload bytes
     */
    void onNotification(long connectionPtr,
                        @NonNull String serviceUuid,
                        @NonNull String characteristicUuid,
                        byte[] value);
}
