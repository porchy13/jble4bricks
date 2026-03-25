package ch.varani.bricks.ble.impl.macos;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Production implementation of {@link NativeBridge} that delegates every
 * operation to the corresponding JNI {@code native} method in
 * {@code libble-macos.dylib}.
 *
 * <p>This class is loaded (and its static initialiser run) only when the
 * production {@link MacOsBleScanner} public constructor is called, so the
 * native library is never required during unit testing.
 *
 * <p>Thread safety: instances are stateless — all state is held in the
 * opaque C pointer values passed by callers.  Concurrent calls to different
 * pointer values are safe; concurrent calls to the same pointer value are
 * safe provided the underlying CoreBluetooth objects are accessed from their
 * designated dispatch queue (enforced on the native side).
 *
 * @since 1.0
 */
final class JniNativeBridge implements NativeBridge {

    /**
     * Constructs a new {@code JniNativeBridge}.
     *
     * <p>The native library must already have been loaded (by
     * {@link ch.varani.bricks.ble.util.NativeLibraryLoader}) before any method
     * on this instance is called.
     */
    JniNativeBridge() {
        // no-arg constructor: library loading is the caller's responsibility
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeInit}
     * in {@code BleBridge.m}, which calls
     * {@code [CBCentralManager initWithDelegate:queue:options:]}.
     */
    @Override
    public long init() {
        return nativeInit();
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeStartScan}
     * in {@code BleBridge.m}, which calls
     * {@code [CBCentralManager scanForPeripheralsWithServices:options:]}.
     */
    @Override
    public void startScan(final long ctxPtr, final @Nullable String serviceUuid) {
        nativeStartScan(ctxPtr, serviceUuid);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeStopScan}
     * in {@code BleBridge.m}, which calls {@code [CBCentralManager stopScan]}.
     */
    @Override
    public void stopScan(final long ctxPtr) {
        nativeStopScan(ctxPtr);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeIsScanning}
     * in {@code BleBridge.m}.
     */
    @Override
    public boolean isScanning(final long ctxPtr) {
        return nativeIsScanning(ctxPtr);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeConnect}
     * in {@code BleBridge.m}, which calls
     * {@code [CBCentralManager connectPeripheral:options:]} and
     * {@code [CBPeripheral discoverServices:]}.
     */
    @Override
    public long connect(final long ctxPtr, final @NonNull String peripheralUuid) {
        return nativeConnect(ctxPtr, peripheralUuid);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeDisconnect}
     * in {@code BleBridge.m}, which calls
     * {@code [CBCentralManager cancelPeripheralConnection:]}.
     */
    @Override
    public void disconnect(final long ctxPtr, final long connectionPtr) {
        nativeDisconnect(ctxPtr, connectionPtr);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeWriteWithoutResponse}
     * in {@code BleBridge.m}, which calls
     * {@code [CBPeripheral writeValue:forCharacteristic:type:]} with
     * {@code CBCharacteristicWriteWithoutResponse}.
     */
    @Override
    public void writeWithoutResponse(
            final long connectionPtr,
            final @NonNull String serviceUuid,
            final @NonNull String characteristicUuid,
            final @NonNull byte[] data) {
        nativeWriteWithoutResponse(connectionPtr, serviceUuid, characteristicUuid, data);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeReadCharacteristic}
     * in {@code BleBridge.m}, which calls
     * {@code [CBPeripheral readValueForCharacteristic:]}.
     */
    @Override
    public @Nullable byte[] readCharacteristic(
            final long connectionPtr,
            final @NonNull String serviceUuid,
            final @NonNull String characteristicUuid) {
        return nativeReadCharacteristic(connectionPtr, serviceUuid, characteristicUuid);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@code Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeSetNotify}
     * in {@code BleBridge.m}, which calls
     * {@code [CBPeripheral setNotifyValue:forCharacteristic:]}.
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
     * {@code Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeDestroy}
     * in {@code BleBridge.m}.
     */
    @Override
    public void destroy(final long ctxPtr) {
        nativeDestroy(ctxPtr);
    }

    /* ─────────────────────────────────────────────────────────────────────────
       JNI native method declarations
       ───────────────────────────────────────────────────────────────────────── */

    /**
     * Allocates a {@code BleContext} and initialises {@code CBCentralManager}.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeInit}
     * in {@code BleBridge.m}.
     *
     * @return opaque pointer to the allocated {@code BleContext}
     */
    private native long nativeInit();

    /**
     * Starts a BLE peripheral scan.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeStartScan}
     * in {@code BleBridge.m}.
     *
     * @param ctxPtr      opaque pointer to the {@code BleContext}
     * @param serviceUuid service UUID filter, or {@code null}
     */
    private native void nativeStartScan(long ctxPtr, @Nullable String serviceUuid);

    /**
     * Stops the active BLE scan.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeStopScan}
     * in {@code BleBridge.m}.
     *
     * @param ctxPtr opaque pointer to the {@code BleContext}
     */
    private native void nativeStopScan(long ctxPtr);

    /**
     * Returns whether a scan is active.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeIsScanning}
     * in {@code BleBridge.m}.
     *
     * @param ctxPtr opaque pointer to the {@code BleContext}
     * @return {@code true} if scanning
     */
    private native boolean nativeIsScanning(long ctxPtr);

    /**
     * Connects to a peripheral and returns a connection context pointer.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeConnect}
     * in {@code BleBridge.m}.
     *
     * @param ctxPtr         opaque pointer to the {@code BleContext}
     * @param peripheralUuid CoreBluetooth peripheral UUID string
     * @return opaque pointer to the allocated {@code BleConnectionContext}
     */
    private native long nativeConnect(long ctxPtr, String peripheralUuid);

    /**
     * Disconnects a peripheral and frees the connection context.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeDisconnect}
     * in {@code BleBridge.m}.
     *
     * @param ctxPtr        opaque pointer to the {@code BleContext}
     * @param connectionPtr opaque pointer to the {@code BleConnectionContext}
     */
    private native void nativeDisconnect(long ctxPtr, long connectionPtr);

    /**
     * Writes bytes to a GATT characteristic without response.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeWriteWithoutResponse}
     * in {@code BleBridge.m}.
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
     * {@code Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeReadCharacteristic}
     * in {@code BleBridge.m}.
     *
     * @param connectionPtr      opaque pointer to the {@code BleConnectionContext}
     * @param serviceUuid        GATT service UUID
     * @param characteristicUuid GATT characteristic UUID
     * @return the value bytes, or {@code null} on failure
     */
    @Nullable
    private native byte[] nativeReadCharacteristic(
            long connectionPtr, String serviceUuid, String characteristicUuid);

    /**
     * Enables or disables GATT notifications for a characteristic.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeSetNotify}
     * in {@code BleBridge.m}.
     *
     * @param connectionPtr      opaque pointer to the {@code BleConnectionContext}
     * @param serviceUuid        GATT service UUID
     * @param characteristicUuid GATT characteristic UUID
     * @param enable             {@code true} to enable
     */
    private native void nativeSetNotify(
            long connectionPtr, String serviceUuid, String characteristicUuid, boolean enable);

    /**
     * Destroys the {@code BleContext} and releases CoreBluetooth resources.
     *
     * <p>Native function:
     * {@code Java_ch_varani_bricks_ble_impl_macos_JniNativeBridge_nativeDestroy}
     * in {@code BleBridge.m}.
     *
     * @param ctxPtr opaque pointer to the {@code BleContext}
     */
    private native void nativeDestroy(long ctxPtr);
}
