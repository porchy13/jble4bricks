package ch.varani.bricks.ble.impl.windows;

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
 * Windows implementation of {@link BleConnection} backed by the WinRT
 * Bluetooth LE API ({@code Windows.Devices.Bluetooth}) via JNI.
 *
 * <p>Instances are created by
 * {@link WindowsBleScanner#connectPeripheral(String)} after the native layer
 * has established the BLE connection and completed GATT service and
 * characteristic discovery.
 *
 * <p>Notification streams are implemented with {@link SubmissionPublisher} so
 * that the reactive back-pressure semantics of {@link Publisher} are
 * respected.  Each {@code (serviceUuid, characteristicUuid)} pair gets its
 * own publisher.
 *
 * <p>Thread safety: all public methods are thread-safe.  The native layer
 * calls {@link #onNotification(String, String, byte[])} from a WinRT
 * thread-pool thread (a JNI-attached background thread), so the publisher
 * must be safe for concurrent submission.
 *
 * @since 1.0
 */
final class WindowsBleConnection implements BleConnection {

    private static final Logger LOG = Logger.getLogger(WindowsBleConnection.class.getName());

    /** Key type for the notification publisher map. */
    private record CharacteristicKey(@NonNull String serviceUuid,
                                     @NonNull String characteristicUuid) {}

    /** Opaque pointer to the native {@code BleConnectionContext} struct. */
    private final long connectionPtr;

    /** Opaque pointer to the owning {@code BleContext} (needed for write/read calls). */
    private final long contextPtr;

    /** The scanner that owns this connection (used to delegate native calls). */
    private final WindowsBleScanner scanner;

    /** The device this connection belongs to. */
    private final WindowsBleDevice device;

    /** Whether this connection is still open. */
    private final AtomicBoolean open = new AtomicBoolean(true);

    /**
     * Per-characteristic notification publishers.  Created lazily on the first
     * call to {@link #notifications(String, String)}.
     */
    private final Map<CharacteristicKey, SubmissionPublisher<byte[]>> notificationPublishers =
            new ConcurrentHashMap<>();

    /**
     * Constructs a new {@code WindowsBleConnection}.
     *
     * @param connectionPtr opaque pointer to the native {@code BleConnectionContext}
     * @param contextPtr    opaque pointer to the native {@code BleContext}
     * @param scanner       the owning {@link WindowsBleScanner}
     */
    WindowsBleConnection(
            final long connectionPtr,
            final long contextPtr,
            final @NonNull WindowsBleScanner scanner) {
        this.connectionPtr = connectionPtr;
        this.contextPtr    = contextPtr;
        this.scanner       = scanner;
        this.device        = null;    /* resolved lazily via scanner if needed */
    }

    /**
     * Package-private constructor that also captures the
     * {@link WindowsBleDevice}.  Used by tests.
     *
     * @param connectionPtr opaque pointer to the native {@code BleConnectionContext}
     * @param contextPtr    opaque pointer to the native {@code BleContext}
     * @param scanner       the owning {@link WindowsBleScanner}
     * @param device        the device this connection belongs to
     */
    WindowsBleConnection(
            final long connectionPtr,
            final long contextPtr,
            final @NonNull WindowsBleScanner scanner,
            final @NonNull WindowsBleDevice device) {
        this.connectionPtr = connectionPtr;
        this.contextPtr    = contextPtr;
        this.scanner       = scanner;
        this.device        = device;
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
     * {@link WindowsBleScanner#writeWithoutResponseNative(long, String, String, byte[])}
     * which calls {@code GattCharacteristic.WriteValueAsync()} with
     * {@code GattWriteOption::WriteWithoutResponse}.
     */
    @Override
    public @NonNull CompletableFuture<Void> writeWithoutResponse(
            final @NonNull String serviceUuid,
            final @NonNull String characteristicUuid,
            final byte[] data) {

        checkOpen();
        return CompletableFuture.runAsync(() ->
                scanner.writeWithoutResponseNative(connectionPtr, serviceUuid,
                        characteristicUuid, data));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Returns a {@link SubmissionPublisher}-backed {@link Publisher} for
     * the given characteristic.  The same publisher is returned on repeated
     * calls for the same {@code (serviceUuid, characteristicUuid)} pair.  If
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
            final SubmissionPublisher<byte[]> pub = new SubmissionPublisher<>();
            scanner.setNotifyNative(connectionPtr, serviceUuid, characteristicUuid, true);
            return pub;
        });
    }

    /**
     * {@inheritDoc}
     *
     * <p>Calls
     * {@link WindowsBleScanner#readCharacteristicNative(long, String, String)}
     * which invokes {@code GattCharacteristic.ReadValueAsync()} and blocks
     * until the peripheral delivers the value.
     */
    @Override
    public @NonNull CompletableFuture<byte[]> read(
            final @NonNull String serviceUuid,
            final @NonNull String characteristicUuid) {

        checkOpen();
        final long ptr = connectionPtr;
        return CompletableFuture.supplyAsync(() ->
                scanner.readCharacteristicNative(ptr, serviceUuid, characteristicUuid));
    }

    /**
     * {@inheritDoc}
     *
     * <p>Closes all notification publishers, disables notifications on the
     * native side, and calls
     * {@link WindowsBleScanner#disconnectNative(long)}.
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
            LOG.fine("BLE connection disconnected");
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
       Package-private — called by the native layer via the scanner
       ───────────────────────────────────────────────────────────────────────── */

    /**
     * Called by {@link WindowsBleScanner#onNotification} when a GATT
     * notification arrives for any subscribed characteristic.
     *
     * <p>The corresponding native callback is the
     * {@code GattCharacteristic.ValueChanged} event handler in
     * {@code BleBridge.cpp}.
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
