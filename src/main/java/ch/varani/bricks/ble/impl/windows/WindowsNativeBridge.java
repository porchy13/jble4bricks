package ch.varani.bricks.ble.impl.windows;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Abstraction over the WinRT Bluetooth LE JNI calls made by
 * {@link WindowsBleScanner}.
 *
 * <p>The production implementation ({@link WindowsJniNativeBridge}) delegates
 * every method to the corresponding {@code native} method declared on
 * {@link WindowsJniNativeBridge}.  Tests inject a Mockito mock of this
 * interface so that the native shared library is never loaded during unit
 * testing.
 *
 * <p>All pointer parameters represent opaque C pointers cast to {@code long}.
 * They must never be interpreted as numeric values by Java code.
 *
 * @since 1.0
 */
interface WindowsNativeBridge {

    /**
     * Allocates a {@code BleContext} struct, creates a
     * {@code BluetoothLEAdvertisementWatcher} and a
     * {@code BluetoothCacheMode}-aware GATT session factory, and waits for
     * the Bluetooth adapter to become available.
     *
     * <p>The {@code callbacks} instance is stored as a JNI global reference
     * inside the native {@code BleContext} so that WinRT thread-pool callbacks
     * can invoke
     * {@link WindowsBleNativeCallbacks#onDeviceFound(String, String, int, byte[])} and
     * {@link WindowsBleNativeCallbacks#onNotification(long, String, String, byte[])}
     * without holding a reference to any concrete platform class.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeInit}
     * in {@code BleBridge.cpp}.
     *
     * @param callbacks the callback target for scan results and GATT
     *                  notifications; must not be {@code null}
     * @return an opaque pointer to the allocated {@code BleContext}, cast to
     *         {@code long}
     */
    long init(@NonNull WindowsBleNativeCallbacks callbacks);

    /**
     * Starts a BLE advertisement scan via
     * {@code BluetoothLEAdvertisementWatcher.Start()}.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeStartScan}
     * in {@code BleBridge.cpp}.
     *
     * @param ctxPtr      opaque pointer to the {@code BleContext}
     * @param serviceUuid UUID string to filter on, or {@code null} for no filter
     */
    void startScan(long ctxPtr, @Nullable String serviceUuid);

    /**
     * Stops the active BLE advertisement scan via
     * {@code BluetoothLEAdvertisementWatcher.Stop()}.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeStopScan}
     * in {@code BleBridge.cpp}.
     *
     * @param ctxPtr opaque pointer to the {@code BleContext}
     */
    void stopScan(long ctxPtr);

    /**
     * Returns whether the advertisement watcher is currently scanning.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeIsScanning}
     * in {@code BleBridge.cpp}.
     *
     * @param ctxPtr opaque pointer to the {@code BleContext}
     * @return {@code true} if a scan is active
     */
    boolean isScanning(long ctxPtr);

    /**
     * Connects to the peripheral identified by {@code deviceAddress}, performs
     * GATT service and characteristic discovery, and returns a connection
     * pointer.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeConnect}
     * in {@code BleBridge.cpp}, using
     * {@code BluetoothLEDevice.FromBluetoothAddressAsync()} and
     * {@code GattDeviceService.GetCharacteristicsAsync()}.
     *
     * @param ctxPtr        opaque pointer to the {@code BleContext}
     * @param deviceAddress the peripheral's BLE address string
     * @return an opaque pointer to the allocated {@code BleConnectionContext}
     */
    long connect(long ctxPtr, @NonNull String deviceAddress);

    /**
     * Disconnects from a peripheral and frees the
     * {@code BleConnectionContext}.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeDisconnect}
     * in {@code BleBridge.cpp}, releasing the
     * {@code BluetoothLEDevice} and all associated GATT objects.
     *
     * @param ctxPtr        opaque pointer to the {@code BleContext}
     * @param connectionPtr opaque pointer to the {@code BleConnectionContext}
     */
    void disconnect(long ctxPtr, long connectionPtr);

    /**
     * Writes bytes to a GATT characteristic using
     * {@code GattCharacteristic.WriteValueAsync()} with
     * {@code GattWriteOption.WriteWithoutResponse}.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeWriteWithoutResponse}
     * in {@code BleBridge.cpp}.
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
     * {@code GattCharacteristic.ReadValueAsync()}.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeReadCharacteristic}
     * in {@code BleBridge.cpp}.
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
     * Enables or disables GATT notifications via
     * {@code GattCharacteristic.WriteClientCharacteristicConfigurationDescriptorAsync()}.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeSetNotify}
     * in {@code BleBridge.cpp}.
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
     * Destroys the {@code BleContext} and releases all WinRT BLE resources.
     *
     * <p>Corresponds to
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeDestroy}
     * in {@code BleBridge.cpp}.
     *
     * @param ctxPtr opaque pointer to the {@code BleContext}
     */
    void destroy(long ctxPtr);
}
