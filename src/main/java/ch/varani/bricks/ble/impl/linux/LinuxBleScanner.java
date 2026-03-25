package ch.varani.bricks.ble.impl.linux;

import java.util.Map;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ConcurrentHashMap;
import java.util.concurrent.Executor;
import java.util.concurrent.Executors;
import java.util.concurrent.atomic.AtomicLong;
import java.util.logging.Logger;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import ch.varani.bricks.ble.api.BleException;
import ch.varani.bricks.ble.api.BleScanner;
import ch.varani.bricks.ble.api.ScanCallback;
import ch.varani.bricks.ble.util.NativeLibraryLoader;

/**
 * Linux implementation of {@link BleScanner} backed by the BlueZ D-Bus API
 * ({@code org.bluez}) via JNI.
 *
 * <p>On construction the class loads {@code libble-linux.so} from the JAR
 * (extracted to a temp file by {@link NativeLibraryLoader}) and calls
 * {@link LinuxNativeBridge#init(LinuxBleNativeCallbacks)} to allocate the
 * underlying {@code BleContext} and start the GLib main loop.
 *
 * <p>All blocking native calls are dispatched on a cached thread-pool so that
 * the {@link CompletableFuture} returned to callers never blocks the JVM
 * thread that constructed the future.
 *
 * <p>This class implements {@link LinuxBleNativeCallbacks} so that the native
 * layer can call back into Java without holding a reference to any
 * Linux-specific concrete type.
 * {@link #onDeviceFound(String, String, int, byte[])} is invoked for every discovered
 * peripheral; {@link #onNotification(long, String, String, byte[])} is invoked
 * for every GATT notification and is routed to the correct
 * {@link LinuxBleConnection} by {@code connectionPtr}.
 * The GLib main-loop thread delivers both callbacks after attaching to the JVM
 * via {@code AttachCurrentThreadAsDaemon}.
 *
 * <p>Thread safety: all public methods are thread-safe.
 *
 * @since 1.0
 */
public final class LinuxBleScanner implements BleScanner, LinuxBleNativeCallbacks {

    private static final Logger LOG = Logger.getLogger(LinuxBleScanner.class.getName());

    /** Base name of the native shared library (without extension or "lib" prefix). */
    static final String LIBRARY_NAME = "ble-linux";

    /**
     * Default executor used to run blocking JNI calls off the caller's thread.
     *
     * <p>Tests may inject a different executor via the package-private
     * {@link #LinuxBleScanner(LinuxNativeBridge, long, Executor)} constructor.
     */
    private static final Executor DEFAULT_EXEC =
            Executors.newCachedThreadPool(r -> {
                final Thread t = new Thread(r, "ble-linux-worker");
                t.setDaemon(true);
                return t;
            });

    /** The native bridge used for all BlueZ D-Bus operations. */
    private final LinuxNativeBridge bridge;

    /** Executor used to dispatch async BLE operations. */
    private final Executor exec;

    /** Opaque pointer to the native {@code BleContext} struct (cast to {@code long}). */
    private final long contextPtr;

    /** Current scan callback; {@code null} when not scanning. */
    @Nullable
    private volatile ScanCallback currentCallback;

    /**
     * Peripherals discovered since the last {@link #startScan} call, keyed by
     * BlueZ D-Bus object path.  Used to hand a {@link LinuxBleDevice} to the
     * Java callback and to look up the device when
     * {@link LinuxBleDevice#connect()} is called.
     */
    private final Map<String, LinuxBleDevice> knownDevices = new ConcurrentHashMap<>();

    /**
     * Open connections keyed by the native {@code BleConnectionContext}
     * pointer.  Used by {@link #onNotification(long, String, String, byte[])}
     * to route GATT notifications from the GLib main-loop thread to the correct
     * {@link LinuxBleConnection} instance.
     */
    private final Map<Long, LinuxBleConnection> openConnections = new ConcurrentHashMap<>();

    /** Counter used to detect stale callbacks after {@link #stopScan()} or {@link #close()}. */
    private final AtomicLong scanGeneration = new AtomicLong(0L);

    /** Generation number that was active when the last startScan was issued. */
    private volatile long activeScanGeneration = 0L;

    /**
     * Constructs a {@code LinuxBleScanner} by loading the native library and
     * initialising the BlueZ D-Bus context.
     *
     * @throws BleException if the native library cannot be loaded or the
     *                      D-Bus connection to BlueZ cannot be established
     */
    public LinuxBleScanner() throws BleException {
        NativeLibraryLoader.load(LIBRARY_NAME);
        this.bridge = new LinuxJniNativeBridge();
        this.contextPtr = bridge.init(this);
        this.exec = DEFAULT_EXEC;
    }

    /**
     * Package-private constructor used by tests to inject a
     * {@link LinuxNativeBridge} mock, bypassing the real native library load
     * and context initialisation.
     *
     * @param bridge     the native bridge to use; must not be {@code null}
     * @param contextPtr opaque pointer to a pre-allocated {@code BleContext}
     */
    LinuxBleScanner(final @NonNull LinuxNativeBridge bridge, final long contextPtr) {
        this(bridge, contextPtr, DEFAULT_EXEC);
    }

    /**
     * Package-private constructor used by tests to inject a
     * {@link LinuxNativeBridge} mock and a custom {@link Executor},
     * bypassing native loading entirely.
     *
     * @param bridge     the native bridge to use; must not be {@code null}
     * @param contextPtr opaque pointer to a pre-allocated {@code BleContext}
     * @param executor   executor to use for async BLE operations
     */
    LinuxBleScanner(
            final @NonNull LinuxNativeBridge bridge,
            final long contextPtr,
            final @NonNull Executor executor) {
        this.bridge     = bridge;
        this.contextPtr = contextPtr;
        this.exec       = executor;
    }

    /* ─────────────────────────────────────────────────────────────────────────
       BleScanner — public API
       ───────────────────────────────────────────────────────────────────────── */

    /**
     * {@inheritDoc}
     *
     * <p>Equivalent to {@link #startScan(String, ScanCallback)} with a
     * {@code null} service UUID filter (discovers all nearby peripherals).
     */
    @Override
    public @NonNull CompletableFuture<Void> startScan(final @NonNull ScanCallback callback) {
        return startScan(null, callback);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Calls {@link LinuxNativeBridge#startScan(long, String)} on the
     * worker executor.  Any previously registered scan is implicitly replaced.
     */
    @Override
    public @NonNull CompletableFuture<Void> startScan(
            final @Nullable String serviceUuid,
            final @NonNull ScanCallback callback) {

        currentCallback      = callback;
        activeScanGeneration = scanGeneration.incrementAndGet();
        knownDevices.clear();

        final long generation = activeScanGeneration;
        final long ptr        = contextPtr;

        return CompletableFuture.runAsync(() -> {
            if (generation != activeScanGeneration) {
                return;  /* superseded before it even started */
            }
            bridge.startScan(ptr, serviceUuid);
            LOG.fine(() -> "BLE scan started (serviceUuid=" + serviceUuid + ")");
        }, exec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull CompletableFuture<Void> stopScan() {
        currentCallback = null;
        scanGeneration.incrementAndGet();           /* invalidate running callbacks */
        final long ptr = contextPtr;
        return CompletableFuture.runAsync(() -> {
            bridge.stopScan(ptr);
            LOG.fine("BLE scan stopped");
        }, exec);
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public boolean isScanning() {
        return bridge.isScanning(contextPtr);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Stops any active scan, calls
     * {@link LinuxNativeBridge#destroy(long)}, and releases all BlueZ D-Bus
     * resources.
     */
    @Override
    public void close() throws BleException {
        currentCallback = null;
        scanGeneration.incrementAndGet();
        try {
            bridge.destroy(contextPtr);
        } catch (RuntimeException e) {
            throw new BleException("Error releasing native BLE resources: " + e.getMessage(), e);
        }
    }

    /* ─────────────────────────────────────────────────────────────────────────
       Package-visible methods — called by LinuxBleDevice and LinuxBleConnection
       ───────────────────────────────────────────────────────────────────────── */

    /**
     * Establishes a BLE connection to the device identified by its BlueZ
     * D-Bus object path.
     *
     * <p>This method is package-private so that {@link LinuxBleDevice} can
     * delegate to it while keeping the native pointer encapsulated here.
     *
     * @param devicePath the BlueZ D-Bus object path of the device
     * @return a future that completes with the open connection
     */
    @NonNull
    CompletableFuture<LinuxBleConnection> connectDevice(final @NonNull String devicePath) {
        final long ptr = contextPtr;
        return CompletableFuture.supplyAsync(() -> {
            final long connPtr;
            try {
                connPtr = bridge.connect(ptr, devicePath);
            } catch (RuntimeException e) {
                throw new java.util.concurrent.CompletionException(
                        new BleException("Connection to " + devicePath
                                + " failed: " + e.getMessage(), e));
            }
            final LinuxBleConnection conn = new LinuxBleConnection(connPtr, ptr, this);
            openConnections.put(connPtr, conn);
            return conn;
        }, exec);
    }

    /**
     * Disconnects the given native connection context, deregisters it from the
     * open-connection map, and frees its resources.
     *
     * @param connectionPtr pointer to the {@code BleConnectionContext} struct
     */
    void disconnectNative(final long connectionPtr) {
        openConnections.remove(connectionPtr);
        bridge.disconnect(contextPtr, connectionPtr);
    }

    /**
     * Writes bytes to a GATT characteristic without requesting an ATT
     * response.
     *
     * @param connectionPtr      pointer to the native {@code BleConnectionContext}
     * @param serviceUuid        GATT service UUID string
     * @param characteristicUuid GATT characteristic UUID string
     * @param data               bytes to write
     */
    void writeWithoutResponseNative(
            final long connectionPtr,
            final @NonNull String serviceUuid,
            final @NonNull String characteristicUuid,
            final byte[] data) {
        bridge.writeWithoutResponse(connectionPtr, serviceUuid, characteristicUuid, data);
    }

    /**
     * Reads the current value of a GATT characteristic.
     *
     * @param connectionPtr      pointer to the native {@code BleConnectionContext}
     * @param serviceUuid        GATT service UUID string
     * @param characteristicUuid GATT characteristic UUID string
     * @return the characteristic value bytes, or an empty array on failure
     */
    byte[] readCharacteristicNative(
            final long connectionPtr,
            final @NonNull String serviceUuid,
            final @NonNull String characteristicUuid) {
        final byte[] result =
                bridge.readCharacteristic(connectionPtr, serviceUuid, characteristicUuid);
        return result != null ? result : new byte[0];
    }

    /**
     * Enables or disables GATT notifications for a characteristic.
     *
     * @param connectionPtr      pointer to the native {@code BleConnectionContext}
     * @param serviceUuid        GATT service UUID string
     * @param characteristicUuid GATT characteristic UUID string
     * @param enable             {@code true} to enable, {@code false} to disable
     */
    void setNotifyNative(
            final long connectionPtr,
            final @NonNull String serviceUuid,
            final @NonNull String characteristicUuid,
            final boolean enable) {
        bridge.setNotify(connectionPtr, serviceUuid, characteristicUuid, enable);
    }

    /* ─────────────────────────────────────────────────────────────────────────
       LinuxBleNativeCallbacks — called by the native layer
       ───────────────────────────────────────────────────────────────────────── */

    /**
     * Invoked by the native BlueZ layer when a BLE advertisement is received.
     *
     * <p>Corresponds to the {@code org.freedesktop.DBus.ObjectManager.InterfacesAdded}
     * signal handler for {@code org.bluez.Device1} objects in {@code BleBridge.c}.
     *
     * @param id               the BlueZ D-Bus object path of the device (e.g.
     *                         {@code /org/bluez/hci0/dev_AA_BB_CC_DD_EE_FF})
     * @param name             the advertised local name (may be empty)
     * @param rssi             received signal strength in dBm
     * @param manufacturerData raw manufacturer-specific advertisement payload bytes;
     *                         empty array if none was present
     */
    @Override
    public void onDeviceFound(
            final @NonNull String id,
            final @NonNull String name,
            final int rssi,
            final byte[] manufacturerData) {

        final ScanCallback cb = currentCallback;
        if (cb == null) {
            return;
        }
        final LinuxBleDevice device = knownDevices.computeIfAbsent(
                id, path -> new LinuxBleDevice(path, name, rssi, manufacturerData, this));
        cb.onDeviceFound(device);
    }

    /**
     * Invoked by the native BlueZ layer when a GATT characteristic notification
     * arrives on a subscribed characteristic.
     *
     * <p>Routes the notification to the {@link LinuxBleConnection} that owns
     * the native connection context identified by {@code connectionPtr}.  If no
     * matching connection is registered (e.g. it was disconnected concurrently)
     * the notification is silently discarded.
     *
     * <p>Corresponds to the
     * {@code org.freedesktop.DBus.Properties.PropertiesChanged} signal handler
     * for {@code org.bluez.GattCharacteristic1} objects in {@code BleBridge.c}.
     *
     * @param connectionPtr      opaque pointer to the native {@code BleConnectionContext}
     * @param serviceUuid        the service UUID string
     * @param characteristicUuid the characteristic UUID string
     * @param value              the notification payload bytes
     */
    @Override
    public void onNotification(
            final long connectionPtr,
            final @NonNull String serviceUuid,
            final @NonNull String characteristicUuid,
            final byte[] value) {

        final LinuxBleConnection conn = openConnections.get(connectionPtr);
        if (conn != null) {
            conn.onNotification(serviceUuid, characteristicUuid, value);
        }
    }
}
