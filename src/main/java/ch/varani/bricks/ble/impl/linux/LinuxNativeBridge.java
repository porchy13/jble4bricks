package ch.varani.bricks.ble.impl.linux;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Abstraction over the BlueZ D-Bus JNI calls made by {@link LinuxBleScanner}.
 *
 * <p>The production implementation ({@link LinuxJniNativeBridge}) delegates every
 * method to the corresponding {@code native} method declared on
 * {@link LinuxJniNativeBridge}.  Tests inject a Mockito mock of this interface so
 * that the native shared library is never loaded during unit testing.
 *
 * <p>All pointer parameters represent opaque C pointers cast to {@code long}.
 * They must never be interpreted as numeric values by Java code.
 *
 * @since 1.0
 */
interface LinuxNativeBridge {

    /**
     * Allocates a {@code BleContext} struct, connects to the D-Bus system bus,
     * acquires the BlueZ {@code org.bluez} well-known name, and starts the GLib
     * main loop on a dedicated thread so that D-Bus signal callbacks can fire.
     *
     * <p>The {@code callbacks} instance is stored as a JNI global reference inside
     * the native {@code BleContext} so that GLib-thread callbacks can invoke
     * {@link LinuxBleNativeCallbacks#onDeviceFound(String, String, int, byte[])} and
     * {@link LinuxBleNativeCallbacks#onNotification(long, String, String, byte[])}
     * without holding a reference to any concrete platform class.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeInit}
     * in {@code BleBridge.c}.
     *
     * @param callbacks the callback target for scan results and GATT notifications;
     *                  must not be {@code null}
     * @return an opaque pointer to the allocated {@code BleContext}, cast to {@code long}
     */
    long init(@NonNull LinuxBleNativeCallbacks callbacks);

    /**
     * Starts a BLE scan via {@code org.bluez.Adapter1.StartDiscovery} on the D-Bus
     * system bus, optionally filtered to a specific service UUID.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeStartScan}
     * in {@code BleBridge.c}.
     *
     * @param ctxPtr      opaque pointer to the {@code BleContext}
     * @param serviceUuid UUID string to filter on, or {@code null} for no filter
     */
    void startScan(long ctxPtr, @Nullable String serviceUuid);

    /**
     * Stops the active BLE scan via {@code org.bluez.Adapter1.StopDiscovery}.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeStopScan}
     * in {@code BleBridge.c}.
     *
     * @param ctxPtr opaque pointer to the {@code BleContext}
     */
    void stopScan(long ctxPtr);

    /**
     * Returns whether a BlueZ discovery session is currently active.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeIsScanning}
     * in {@code BleBridge.c}.
     *
     * @param ctxPtr opaque pointer to the {@code BleContext}
     * @return {@code true} if a scan is active
     */
    boolean isScanning(long ctxPtr);

    /**
     * Connects to the BlueZ device identified by its D-Bus object path, performs
     * GATT service and characteristic discovery, and returns a connection pointer.
     *
     * <p>Calls {@code org.bluez.Device1.Connect} and then enumerates
     * {@code org.bluez.GattService1} and {@code org.bluez.GattCharacteristic1}
     * objects under the device's D-Bus path.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeConnect}
     * in {@code BleBridge.c}.
     *
     * @param ctxPtr     opaque pointer to the {@code BleContext}
     * @param devicePath the BlueZ D-Bus object path of the device
     * @return an opaque pointer to the allocated {@code BleConnectionContext}
     */
    long connect(long ctxPtr, @NonNull String devicePath);

    /**
     * Disconnects from a device via {@code org.bluez.Device1.Disconnect} and
     * frees the {@code BleConnectionContext}.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeDisconnect}
     * in {@code BleBridge.c}.
     *
     * @param ctxPtr        opaque pointer to the {@code BleContext}
     * @param connectionPtr opaque pointer to the {@code BleConnectionContext}
     */
    void disconnect(long ctxPtr, long connectionPtr);

    /**
     * Writes bytes to a GATT characteristic via
     * {@code org.bluez.GattCharacteristic1.WriteValue} with the
     * {@code write-without-response} option.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeWriteWithoutResponse}
     * in {@code BleBridge.c}.
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
            byte[] data);

    /**
     * Reads the current value of a GATT characteristic via
     * {@code org.bluez.GattCharacteristic1.ReadValue}.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeReadCharacteristic}
     * in {@code BleBridge.c}.
     *
     * @param connectionPtr      opaque pointer to the {@code BleConnectionContext}
     * @param serviceUuid        GATT service UUID string
     * @param characteristicUuid GATT characteristic UUID string
     * @return the characteristic value bytes, or {@code null} on failure
     */
    byte[] readCharacteristic(
            long connectionPtr,
            @NonNull String serviceUuid,
            @NonNull String characteristicUuid);

    /**
     * Enables or disables GATT notifications by calling
     * {@code org.bluez.GattCharacteristic1.StartNotify} or
     * {@code org.bluez.GattCharacteristic1.StopNotify}.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeSetNotify}
     * in {@code BleBridge.c}.
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
     * Destroys the {@code BleContext}, stops the GLib main loop, and releases all
     * D-Bus resources.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeDestroy}
     * in {@code BleBridge.c}.
     *
     * @param ctxPtr opaque pointer to the {@code BleContext}
     */
    void destroy(long ctxPtr);
}
