package ch.varani.bricks.ble.impl.linux;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Production implementation of {@link LinuxNativeBridge} that delegates every
 * operation to the corresponding JNI {@code native} method in
 * {@code libble-linux.so}.
 *
 * <p>This class is loaded (and its static initialiser run) only when the
 * production {@link LinuxBleScanner} public constructor is called, so the
 * native library is never required during unit testing.
 *
 * <p>Thread safety: instances are stateless — all state is held in the
 * opaque C pointer values passed by callers.  Concurrent calls to different
 * pointer values are safe; concurrent calls to the same pointer value are
 * safe provided the underlying BlueZ D-Bus objects are accessed from the
 * dedicated GLib main-loop thread (enforced on the native side).
 *
 * @since 1.0
 */
final class LinuxJniNativeBridge implements LinuxNativeBridge {

    /**
     * Constructs a new {@code LinuxJniNativeBridge}.
     *
     * <p>The native library must already have been loaded (by
     * {@link ch.varani.bricks.ble.util.NativeLibraryLoader}) before any method
     * on this instance is called.
     */
    LinuxJniNativeBridge() {
        // no-arg constructor: library loading is the caller's responsibility
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeInit}
     * in {@code BleBridge.c}, which connects to the D-Bus system bus,
     * acquires the BlueZ {@code org.bluez} well-known name, and starts the
     * GLib main loop on a dedicated thread.
     * The {@code callbacks} reference is stored by the native layer as a JNI
     * global reference so that GLib-thread callbacks can invoke
     * {@link LinuxBleNativeCallbacks#onDeviceFound(String, String, int)} and
     * {@link LinuxBleNativeCallbacks#onNotification(long, String, String, byte[])}
     * directly.
     */
    @Override
    public long init(final @NonNull LinuxBleNativeCallbacks callbacks) {
        return nativeInit(callbacks);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeStartScan}
     * in {@code BleBridge.c}, which calls
     * {@code org.bluez.Adapter1.StartDiscovery} over D-Bus.
     */
    @Override
    public void startScan(final long ctxPtr, final @Nullable String serviceUuid) {
        nativeStartScan(ctxPtr, serviceUuid);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeStopScan}
     * in {@code BleBridge.c}, which calls
     * {@code org.bluez.Adapter1.StopDiscovery} over D-Bus.
     */
    @Override
    public void stopScan(final long ctxPtr) {
        nativeStopScan(ctxPtr);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeIsScanning}
     * in {@code BleBridge.c}.
     */
    @Override
    public boolean isScanning(final long ctxPtr) {
        return nativeIsScanning(ctxPtr);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeConnect}
     * in {@code BleBridge.c}, which calls {@code org.bluez.Device1.Connect}
     * and then enumerates {@code org.bluez.GattService1} and
     * {@code org.bluez.GattCharacteristic1} objects via
     * {@code GetManagedObjects}.
     */
    @Override
    public long connect(final long ctxPtr, final @NonNull String devicePath) {
        return nativeConnect(ctxPtr, devicePath);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeDisconnect}
     * in {@code BleBridge.c}, which calls
     * {@code org.bluez.Device1.Disconnect} over D-Bus.
     */
    @Override
    public void disconnect(final long ctxPtr, final long connectionPtr) {
        nativeDisconnect(ctxPtr, connectionPtr);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeWriteWithoutResponse}
     * in {@code BleBridge.c}, which calls
     * {@code org.bluez.GattCharacteristic1.WriteValue} with the
     * {@code write-without-response} option.
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
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeReadCharacteristic}
     * in {@code BleBridge.c}, which calls
     * {@code org.bluez.GattCharacteristic1.ReadValue} over D-Bus.
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
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeSetNotify}
     * in {@code BleBridge.c}, which calls
     * {@code org.bluez.GattCharacteristic1.StartNotify} or
     * {@code org.bluez.GattCharacteristic1.StopNotify} over D-Bus.
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
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeDestroy}
     * in {@code BleBridge.c}, which stops the GLib main loop and releases all
     * D-Bus resources.
     */
    @Override
    public void destroy(final long ctxPtr) {
        nativeDestroy(ctxPtr);
    }

    /* ─────────────────────────────────────────────────────────────────────────
       JNI native method declarations
       ───────────────────────────────────────────────────────────────────────── */

    /**
     * Allocates a {@code BleContext}, connects to the D-Bus system bus, and
     * starts the GLib main loop thread.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeInit}
     * in {@code BleBridge.c}.
     *
     * @param callbacks the {@link LinuxBleNativeCallbacks} implementation that
     *                  will receive scan results and GATT notifications from the
     *                  native layer; stored by native as a JNI global reference
     *                  for the lifetime of the {@code BleContext}
     * @return opaque pointer to the allocated {@code BleContext}
     */
    private native long nativeInit(@NonNull LinuxBleNativeCallbacks callbacks);

    /**
     * Starts a BLE discovery session via {@code org.bluez.Adapter1.StartDiscovery}.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeStartScan}
     * in {@code BleBridge.c}.
     *
     * @param ctxPtr      opaque pointer to the {@code BleContext}
     * @param serviceUuid service UUID filter, or {@code null}
     */
    private native void nativeStartScan(long ctxPtr, @Nullable String serviceUuid);

    /**
     * Stops the active BLE discovery session via
     * {@code org.bluez.Adapter1.StopDiscovery}.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeStopScan}
     * in {@code BleBridge.c}.
     *
     * @param ctxPtr opaque pointer to the {@code BleContext}
     */
    private native void nativeStopScan(long ctxPtr);

    /**
     * Returns whether a BlueZ discovery session is currently active.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeIsScanning}
     * in {@code BleBridge.c}.
     *
     * @param ctxPtr opaque pointer to the {@code BleContext}
     * @return {@code true} if scanning
     */
    private native boolean nativeIsScanning(long ctxPtr);

    /**
     * Connects to a BlueZ device by its D-Bus object path and returns a
     * connection context pointer.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeConnect}
     * in {@code BleBridge.c}.
     *
     * @param ctxPtr     opaque pointer to the {@code BleContext}
     * @param devicePath BlueZ D-Bus object path of the device
     * @return opaque pointer to the allocated {@code BleConnectionContext}
     */
    private native long nativeConnect(long ctxPtr, String devicePath);

    /**
     * Disconnects a device and frees the connection context.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeDisconnect}
     * in {@code BleBridge.c}.
     *
     * @param ctxPtr        opaque pointer to the {@code BleContext}
     * @param connectionPtr opaque pointer to the {@code BleConnectionContext}
     */
    private native void nativeDisconnect(long ctxPtr, long connectionPtr);

    /**
     * Writes bytes to a GATT characteristic without response via
     * {@code org.bluez.GattCharacteristic1.WriteValue}.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeWriteWithoutResponse}
     * in {@code BleBridge.c}.
     *
     * @param connectionPtr      opaque pointer to the {@code BleConnectionContext}
     * @param serviceUuid        GATT service UUID
     * @param characteristicUuid GATT characteristic UUID
     * @param data               bytes to write
     */
    private native void nativeWriteWithoutResponse(
            long connectionPtr, String serviceUuid, String characteristicUuid, byte[] data);

    /**
     * Reads the value of a GATT characteristic via
     * {@code org.bluez.GattCharacteristic1.ReadValue}.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeReadCharacteristic}
     * in {@code BleBridge.c}.
     *
     * @param connectionPtr      opaque pointer to the {@code BleConnectionContext}
     * @param serviceUuid        GATT service UUID
     * @param characteristicUuid GATT characteristic UUID
     * @return the value bytes, or {@code null} on failure
     */
    private native byte[] nativeReadCharacteristic(
            long connectionPtr, String serviceUuid, String characteristicUuid);

    /**
     * Enables or disables GATT notifications via
     * {@code org.bluez.GattCharacteristic1.StartNotify} /
     * {@code org.bluez.GattCharacteristic1.StopNotify}.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeSetNotify}
     * in {@code BleBridge.c}.
     *
     * @param connectionPtr      opaque pointer to the {@code BleConnectionContext}
     * @param serviceUuid        GATT service UUID
     * @param characteristicUuid GATT characteristic UUID
     * @param enable             {@code true} to enable
     */
    private native void nativeSetNotify(
            long connectionPtr, String serviceUuid, String characteristicUuid, boolean enable);

    /**
     * Destroys the {@code BleContext}, stops the GLib main loop, and releases
     * all D-Bus resources.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_linux_LinuxJniNativeBridge_nativeDestroy}
     * in {@code BleBridge.c}.
     *
     * @param ctxPtr opaque pointer to the {@code BleContext}
     */
    private native void nativeDestroy(long ctxPtr);
}
