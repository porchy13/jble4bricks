package ch.varani.bricks.ble.impl.linux;

import org.jspecify.annotations.NonNull;

/**
 * Callback seam between the BlueZ D-Bus JNI layer and the Java scanner/connection
 * objects that own the BLE context.
 *
 * <p>The native layer stores a JNI global reference to the concrete implementation
 * of this interface and invokes its methods from the GLib main-loop thread
 * (a JNI-attached background thread).
 *
 * <p>This interface is intentionally scoped to {@code impl/linux/}. Each platform
 * implementation defines its own equivalent callback seam in its own package
 * (e.g. {@code impl/macos/}, {@code impl/windows/}), so no platform type ever
 * leaks into another platform's package or into the public API.
 *
 * <p>Thread safety: implementations must be safe for concurrent invocation from
 * any JNI-attached thread.
 *
 * @since 1.0
 */
interface LinuxBleNativeCallbacks {

    /**
     * Called by the native layer each time a BLE advertisement is received during
     * a scan.
     *
     * <p>Corresponds to the BlueZ D-Bus signal
     * {@code org.freedesktop.DBus.ObjectManager.InterfacesAdded} filtered for
     * {@code org.bluez.Device1} objects in {@code BleBridge.c}.
     *
     * @param id   the BlueZ D-Bus object path of the device (e.g.
     *             {@code /org/bluez/hci0/dev_AA_BB_CC_DD_EE_FF})
     * @param name the advertised local name (empty string if absent)
     * @param rssi received signal strength in dBm
     */
    void onDeviceFound(@NonNull String id, @NonNull String name, int rssi);

    /**
     * Called by the native layer when a GATT notification arrives for a subscribed
     * characteristic.
     *
     * <p>Corresponds to the BlueZ D-Bus signal
     * {@code org.freedesktop.DBus.Properties.PropertiesChanged} on a
     * {@code org.bluez.GattCharacteristic1} object when the {@code Value}
     * property changes in {@code BleBridge.c}.
     *
     * @param connectionPtr      opaque pointer to the native {@code BleConnectionContext}
     *                           that received the notification; used to dispatch the
     *                           value to the correct {@link LinuxBleConnection} instance
     * @param serviceUuid        the GATT service UUID string
     * @param characteristicUuid the GATT characteristic UUID string
     * @param value              the notification payload bytes
     */
    void onNotification(long connectionPtr,
                        @NonNull String serviceUuid,
                        @NonNull String characteristicUuid,
                        byte[] value);
}
