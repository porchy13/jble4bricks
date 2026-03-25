package ch.varani.bricks.ble.impl.windows;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Production implementation of {@link WindowsNativeBridge} that delegates
 * every operation to the corresponding JNI {@code native} method in
 * {@code ble-windows.dll}.
 *
 * <p>This class is loaded (and its static initialiser run) only when the
 * production {@link WindowsBleScanner} public constructor is called, so the
 * native library is never required during unit testing.
 *
 * <p>Thread safety: instances are stateless — all state is held in the opaque
 * C pointer values passed by callers.  Concurrent calls to different pointer
 * values are safe; concurrent calls to the same pointer value are safe
 * provided the underlying WinRT objects are accessed from their designated
 * thread-pool thread (enforced on the native side).
 *
 * @since 1.0
 */
final class WindowsJniNativeBridge implements WindowsNativeBridge {

    /**
     * Constructs a new {@code WindowsJniNativeBridge}.
     *
     * <p>The native library must already have been loaded (by
     * {@link ch.varani.bricks.ble.util.NativeLibraryLoader}) before any
     * method on this instance is called.
     */
    WindowsJniNativeBridge() {
        // no-arg constructor: library loading is the caller's responsibility
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeInit}
     * in {@code BleBridge.cpp}, which calls
     * {@code BluetoothLEAdvertisementWatcher()} and
     * {@code IBluetoothAdapter.GetDefaultAsync()}.
     * The {@code callbacks} reference is stored by the native layer as a JNI
     * global reference so that WinRT thread-pool callbacks can invoke
     * {@link WindowsBleNativeCallbacks#onDeviceFound(String, String, int, byte[])} and
     * {@link WindowsBleNativeCallbacks#onNotification(long, String, String, byte[])}
     * directly.
     */
    @Override
    public long init(final @NonNull WindowsBleNativeCallbacks callbacks) {
        return nativeInit(callbacks);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeStartScan}
     * in {@code BleBridge.cpp}, which calls
     * {@code BluetoothLEAdvertisementWatcher.Start()}.
     */
    @Override
    public void startScan(final long ctxPtr, final @Nullable String serviceUuid) {
        nativeStartScan(ctxPtr, serviceUuid);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeStopScan}
     * in {@code BleBridge.cpp}, which calls
     * {@code BluetoothLEAdvertisementWatcher.Stop()}.
     */
    @Override
    public void stopScan(final long ctxPtr) {
        nativeStopScan(ctxPtr);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeIsScanning}
     * in {@code BleBridge.cpp}.
     */
    @Override
    public boolean isScanning(final long ctxPtr) {
        return nativeIsScanning(ctxPtr);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeConnect}
     * in {@code BleBridge.cpp}, which calls
     * {@code BluetoothLEDevice.FromBluetoothAddressAsync()} and
     * {@code GattDeviceService.GetCharacteristicsAsync()}.
     */
    @Override
    public long connect(final long ctxPtr, final @NonNull String deviceAddress) {
        return nativeConnect(ctxPtr, deviceAddress);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeDisconnect}
     * in {@code BleBridge.cpp}, which closes the
     * {@code BluetoothLEDevice} session.
     */
    @Override
    public void disconnect(final long ctxPtr, final long connectionPtr) {
        nativeDisconnect(ctxPtr, connectionPtr);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeWriteWithoutResponse}
     * in {@code BleBridge.cpp}, which calls
     * {@code GattCharacteristic.WriteValueAsync()} with
     * {@code GattWriteOption::WriteWithoutResponse}.
     */
    @Override
    public void writeWithoutResponse(
            final long connectionPtr,
            final @NonNull String serviceUuid,
            final @NonNull String characteristicUuid,
            final byte[] data) {
        nativeWriteWithoutResponse(connectionPtr, serviceUuid, characteristicUuid, data);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeReadCharacteristic}
     * in {@code BleBridge.cpp}, which calls
     * {@code GattCharacteristic.ReadValueAsync()}.
     */
    @Override
    public byte[] readCharacteristic(
            final long connectionPtr,
            final @NonNull String serviceUuid,
            final @NonNull String characteristicUuid) {
        return nativeReadCharacteristic(connectionPtr, serviceUuid, characteristicUuid);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeSetNotify}
     * in {@code BleBridge.cpp}, which calls
     * {@code GattCharacteristic.WriteClientCharacteristicConfigurationDescriptorAsync()}.
     */
    @Override
    public void setNotify(
            final long connectionPtr,
            final @NonNull String serviceUuid,
            final @NonNull String characteristicUuid,
            final boolean enable) {
        nativeSetNotify(connectionPtr, serviceUuid, characteristicUuid, enable);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeDestroy}
     * in {@code BleBridge.cpp}.
     */
    @Override
    public void destroy(final long ctxPtr) {
        nativeDestroy(ctxPtr);
    }

    /* ─────────────────────────────────────────────────────────────────────────
       JNI native method declarations
       ───────────────────────────────────────────────────────────────────────── */

    /**
     * Allocates a {@code BleContext} and initialises the WinRT BLE adapter.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeInit}
     * in {@code BleBridge.cpp}.
     *
     * @param callbacks the {@link WindowsBleNativeCallbacks} implementation
     *                  that will receive scan results and GATT notifications;
     *                  stored by native as a JNI global reference for the
     *                  lifetime of the {@code BleContext}
     * @return opaque pointer to the allocated {@code BleContext}
     */
    private native long nativeInit(@NonNull WindowsBleNativeCallbacks callbacks);

    /**
     * Starts a BLE advertisement scan.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeStartScan}
     * in {@code BleBridge.cpp}.
     *
     * @param ctxPtr      opaque pointer to the {@code BleContext}
     * @param serviceUuid service UUID filter, or {@code null}
     */
    private native void nativeStartScan(long ctxPtr, @Nullable String serviceUuid);

    /**
     * Stops the active BLE advertisement scan.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeStopScan}
     * in {@code BleBridge.cpp}.
     *
     * @param ctxPtr opaque pointer to the {@code BleContext}
     */
    private native void nativeStopScan(long ctxPtr);

    /**
     * Returns whether a scan is active.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeIsScanning}
     * in {@code BleBridge.cpp}.
     *
     * @param ctxPtr opaque pointer to the {@code BleContext}
     * @return {@code true} if scanning
     */
    private native boolean nativeIsScanning(long ctxPtr);

    /**
     * Connects to a peripheral and returns a connection context pointer.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeConnect}
     * in {@code BleBridge.cpp}.
     *
     * @param ctxPtr        opaque pointer to the {@code BleContext}
     * @param deviceAddress BLE device address string
     * @return opaque pointer to the allocated {@code BleConnectionContext}
     */
    private native long nativeConnect(long ctxPtr, String deviceAddress);

    /**
     * Disconnects a peripheral and frees the connection context.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeDisconnect}
     * in {@code BleBridge.cpp}.
     *
     * @param ctxPtr        opaque pointer to the {@code BleContext}
     * @param connectionPtr opaque pointer to the {@code BleConnectionContext}
     */
    private native void nativeDisconnect(long ctxPtr, long connectionPtr);

    /**
     * Writes bytes to a GATT characteristic without response.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeWriteWithoutResponse}
     * in {@code BleBridge.cpp}.
     *
     * @param connectionPtr      opaque pointer to the {@code BleConnectionContext}
     * @param serviceUuid        GATT service UUID
     * @param characteristicUuid GATT characteristic UUID
     * @param data               bytes to write
     */
    private native void nativeWriteWithoutResponse(
            long connectionPtr, String serviceUuid, String characteristicUuid, byte[] data);

    /**
     * Reads the value of a GATT characteristic.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeReadCharacteristic}
     * in {@code BleBridge.cpp}.
     *
     * @param connectionPtr      opaque pointer to the {@code BleConnectionContext}
     * @param serviceUuid        GATT service UUID
     * @param characteristicUuid GATT characteristic UUID
     * @return the value bytes, or {@code null} on failure
     */
    private native byte[] nativeReadCharacteristic(
            long connectionPtr, String serviceUuid, String characteristicUuid);

    /**
     * Enables or disables GATT notifications for a characteristic.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeSetNotify}
     * in {@code BleBridge.cpp}.
     *
     * @param connectionPtr      opaque pointer to the {@code BleConnectionContext}
     * @param serviceUuid        GATT service UUID
     * @param characteristicUuid GATT characteristic UUID
     * @param enable             {@code true} to enable
     */
    private native void nativeSetNotify(
            long connectionPtr, String serviceUuid, String characteristicUuid, boolean enable);

    /**
     * Destroys the {@code BleContext} and releases WinRT resources.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_windows_WindowsJniNativeBridge_nativeDestroy}
     * in {@code BleBridge.cpp}.
     *
     * @param ctxPtr opaque pointer to the {@code BleContext}
     */
    private native void nativeDestroy(long ctxPtr);
}
