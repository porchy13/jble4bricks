package ch.varani.bricks.ble.impl.macos;

import java.util.Locale;
import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.SubmissionPublisher;
import java.util.concurrent.atomic.AtomicBoolean;
import java.util.logging.Logger;

import org.jspecify.annotations.NonNull;

import ch.varani.bricks.ble.api.BleConnection;
import ch.varani.bricks.ble.api.BleDevice;
import ch.varani.bricks.ble.api.BleException;

/**
 * macOS implementation of {@link BleConnection} backed by CoreBluetooth via JNI.
 *
 * <p>Instances are created by {@link MacOsBleScanner#connectPeripheral(String)}
 * after the native layer has established the BLE connection and completed GATT
 * service and characteristic discovery.
 *
 * <p>Notification streams are implemented with {@link SubmissionPublisher} so
 * that the reactive back-pressure semantics of {@link Publisher} are respected.
 * Each {@code (serviceUuid, characteristicUuid)} pair gets its own publisher.
 *
 * <p>Thread safety: all public methods are thread-safe.  The native layer
 * calls {@link #onNotification(String, String, byte[])} from the CoreBluetooth
 * dispatch queue (a JNI-attached background thread), so the publisher must be
 * safe for concurrent submission.
 *
 * @since 1.0
 */
final class MacOsBleConnection implements BleConnection {

    private static final Logger LOG = Logger.getLogger(MacOsBleConnection.class.getName());

    /**
     * Key type for the notification publisher map.
     *
     * <p>UUID strings are normalised to upper case on construction so that keys
     * registered from Java constants (lower case, e.g. {@code "00001624-..."})
     * match keys derived from CoreBluetooth's {@code UUIDString} property, which
     * always returns upper case (e.g. {@code "00001624-...".toUpperCase()}).
     * Without this normalisation all notification lookups would silently fail.
     */
    private record CharacteristicKey(@NonNull String serviceUuid,
                                     @NonNull String characteristicUuid) {

        /**
         * Constructs a {@code CharacteristicKey} with both UUID strings normalised
         * to upper case using the {@link Locale#ROOT} locale.
         *
         * @param serviceUuid        GATT service UUID string; must not be {@code null}
         * @param characteristicUuid GATT characteristic UUID string; must not be {@code null}
         */
        CharacteristicKey {
            serviceUuid        = serviceUuid.toUpperCase(Locale.ROOT);
            characteristicUuid = characteristicUuid.toUpperCase(Locale.ROOT);
        }
    }

    /** Opaque pointer to the native {@code BleConnectionContext} struct. */
    private final long connectionPtr;

    /** Opaque pointer to the owning {@code BleContext} (needed for write/read calls). */
    private final long contextPtr;

    /** The scanner that owns this connection (used to delegate native calls). */
    private final MacOsBleScanner scanner;

    /** The device this connection belongs to. */
    private final MacOsBleDevice device;

    /** Whether this connection is still open. */
    private final AtomicBoolean open = new AtomicBoolean(true);

    /**
     * Per-characteristic notification publishers.  Created lazily on the first
     * call to {@link #notifications(String, String)}.
     */
    private final Map<CharacteristicKey, SubmissionPublisher<byte[]>> notificationPublishers =
            new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code MacOsBleConnection}.
     *
     * @param connectionPtr opaque pointer to the native {@code BleConnectionContext}
     * @param contextPtr    opaque pointer to the native {@code BleContext}
     * @param scanner       the owning {@link MacOsBleScanner}
     */
    MacOsBleConnection(
            final long connectionPtr,
            final long contextPtr,
            final @NonNull MacOsBleScanner scanner) {
        this.connectionPtr = connectionPtr;
        this.contextPtr    = contextPtr;
        this.scanner       = scanner;
        this.device        = null;    /* resolved lazily via scanner if needed */
        LOG.info(() -> "BLE connection established: connPtr=0x"
                + Long.toHexString(connectionPtr));
    }

    /**
     * Package-private constructor that also captures the {@link MacOsBleDevice}.
     * Used by tests.
     *
     * @param connectionPtr opaque pointer to the native {@code BleConnectionContext}
     * @param contextPtr    opaque pointer to the native {@code BleContext}
     * @param scanner       the owning {@link MacOsBleScanner}
     * @param device        the device this connection belongs to
     */
    MacOsBleConnection(
            final long connectionPtr,
            final long contextPtr,
            final @NonNull MacOsBleScanner scanner,
            final @NonNull MacOsBleDevice device) {
        this.connectionPtr = connectionPtr;
        this.contextPtr    = contextPtr;
        this.scanner       = scanner;
        this.device        = device;
        LOG.info(() -> "BLE connection established: connPtr=0x"
                + Long.toHexString(connectionPtr));
    }

    /**
     * Returns the native connection pointer (package-private; used by tests).
     *
     * @return opaque pointer to the {@code BleConnectionContext}
     */
    long connectionPtr() {
        return connectionPtr;
    }

    /**
     * Returns the native context pointer (package-private; used by tests).
     *
     * @return opaque pointer to the {@code BleContext}
     */
    long contextPtr() {
        return contextPtr;
    }

    /* ─────────────────────────────────────────────────────────────────────────
       BleConnection — public API
       ───────────────────────────────────────────────────────────────────────── */

    /**
     * {@inheritDoc}
     *
     * @throws IllegalStateException if called after this connection was closed
     */
    @Override
    public @NonNull BleDevice device() {
        if (device != null) {
            return device;
        }
        throw new IllegalStateException(
                "Device reference not available on this connection instance");
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to
     * {@link MacOsBleScanner#writeWithoutResponseNative(long, String, String, byte[])}
     * which calls {@code CBPeripheral writeValue:forCharacteristic:type:} with
     * {@code CBCharacteristicWriteWithoutResponse}.
     */
    @Override
    public @NonNull CompletableFuture<Void> writeWithoutResponse(
            final @NonNull String serviceUuid,
            final @NonNull String characteristicUuid,
            final byte[] data) {

        checkOpen();
        LOG.fine(() -> "Write: chr=" + characteristicUuid + " svc=" + serviceUuid
                + " len=" + data.length + " bytes");
        return CompletableFuture.runAsync(() ->
                scanner.writeWithoutResponseNative(connectionPtr, serviceUuid,
                        characteristicUuid, data));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a {@link SubmissionPublisher}-backed {@link Publisher} for the
     * given characteristic.  The same publisher is returned on repeated calls
     * for the same {@code (serviceUuid, characteristicUuid)} pair.  If
     * notifications have not yet been enabled on the native side they are
     * enabled here.
     */
    @Override
    public @NonNull Publisher<byte[]> notifications(
            final @NonNull String serviceUuid,
            final @NonNull String characteristicUuid) {

        checkOpen();
        final CharacteristicKey key = new CharacteristicKey(serviceUuid, characteristicUuid);
        return notificationPublishers.computeIfAbsent(key, k -> {
            LOG.fine(() -> "Enabling notifications: chr=" + characteristicUuid
                    + " svc=" + serviceUuid);
            final SubmissionPublisher<byte[]> pub = new SubmissionPublisher<>();
            scanner.setNotifyNative(connectionPtr, serviceUuid, characteristicUuid, true);
            return pub;
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Calls {@link MacOsBleScanner#readCharacteristicNative(long, String, String)}
     * which invokes {@code CBPeripheral readValueForCharacteristic:} and blocks
     * until the peripheral delivers the value.
     */
    @Override
    public @NonNull CompletableFuture<byte[]> read(
            final @NonNull String serviceUuid,
            final @NonNull String characteristicUuid) {

        checkOpen();
        LOG.fine(() -> "Read: chr=" + characteristicUuid + " svc=" + serviceUuid);
        final long ptr = connectionPtr;
        return CompletableFuture.supplyAsync(() ->
                scanner.readCharacteristicNative(ptr, serviceUuid, characteristicUuid));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Closes all notification publishers, disables notifications on the
     * native side, and calls
     * {@link MacOsBleScanner#disconnectNative(long)}.
     */
    @Override
    public @NonNull CompletableFuture<Void> disconnect() {
        if (!open.compareAndSet(true, false)) {
            return CompletableFuture.completedFuture(null);
        }
        closeNotificationPublishers();
        final long ptr = connectionPtr;
        return CompletableFuture.runAsync(() -> {
            scanner.disconnectNative(ptr);
            LOG.info("BLE connection disconnected");
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Calls {@link #disconnect()} and waits for it to complete.
     */
    @Override
    public void close() throws BleException {
        try {
            disconnect().get();
        } catch (InterruptedException e) {
            Thread.currentThread().interrupt();
            throw new BleException("Disconnect interrupted", e);
        } catch (java.util.concurrent.ExecutionException e) {
            throw new BleException("Disconnect failed: " + e.getMessage(), e);
        }
    }

    /* ─────────────────────────────────────────────────────────────────────────
       Package-private — called by the JNI bridge on notification
       ───────────────────────────────────────────────────────────────────────── */

    /**
     * Called by the native layer when a GATT notification arrives for any
     * subscribed characteristic.
     *
     * <p>The corresponding native callback is
     * {@code peripheral:didUpdateValueForCharacteristic:error:} in
     * {@code BleBridge.m} when {@code characteristic.isNotifying} is {@code YES}.
     *
     * @param serviceUuid        the service UUID string
     * @param characteristicUuid the characteristic UUID string
     * @param value              the notification payload bytes
     */
    void onNotification(
            final @NonNull String serviceUuid,
            final @NonNull String characteristicUuid,
            final byte[] value) {

        final CharacteristicKey key = new CharacteristicKey(serviceUuid, characteristicUuid);
        final SubmissionPublisher<byte[]> pub = notificationPublishers.get(key);
        if (pub != null) {
            pub.submit(value);
        }
    }

    /* ─────────────────────────────────────────────────────────────────────────
       Private helpers
       ───────────────────────────────────────────────────────────────────────── */

    /**
     * Throws {@link IllegalStateException} if this connection is closed.
     */
    private void checkOpen() {
        if (!open.get()) {
            throw new IllegalStateException("BLE connection is already closed");
        }
    }

    /**
     * Closes every notification publisher and removes it from the map.
     */
    private void closeNotificationPublishers() {
        for (final Map.Entry<CharacteristicKey, SubmissionPublisher<byte[]>> entry
                : notificationPublishers.entrySet()) {
            try {
                scanner.setNotifyNative(connectionPtr,
                        entry.getKey().serviceUuid(),
                        entry.getKey().characteristicUuid(),
                        false);
            } catch (RuntimeException e) {
                LOG.warning("Failed to disable notifications: " + e.getMessage());
            }
            entry.getValue().close();
        }
        notificationPublishers.clear();
    }

    /**
     * A no-op {@link Subscription} used internally to satisfy the
     * {@link Publisher}/{@link Subscriber} contract during testing.
     */
    static final class NoOpSubscription implements Subscription {

        /** The single shared instance. */
        static final NoOpSubscription INSTANCE = new NoOpSubscription();

        /** Private constructor — use {@link #INSTANCE}. */
        private NoOpSubscription() {}

        @Override
        public void request(final long n) {
            /* no-op */
        }

        @Override
        public void cancel() {
            /* no-op */
        }
    }
}
