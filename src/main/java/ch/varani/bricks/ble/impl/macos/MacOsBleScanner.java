package ch.varani.bricks.ble.impl.macos;

import java.util.HexFormat;
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
 * macOS implementation of {@link BleScanner} backed by CoreBluetooth via JNI.
 *
 * <p>On construction the class loads {@code libble-macos.dylib} from the JAR
 * (extracted to a temp file by {@link NativeLibraryLoader}) and calls
 * {@link NativeBridge#init(BleNativeCallbacks)} to allocate the underlying
 * {@code CBCentralManager}.
 *
 * <p>All blocking native calls are dispatched on a cached thread-pool so that
 * the {@link CompletableFuture} returned to callers never blocks the JVM
 * thread that constructed the future.
 *
 * <p>This class implements {@link BleNativeCallbacks} so that the native layer
 * can call back into Java without holding a reference to any macOS-specific
 * concrete type.  {@link #onDeviceFound(String, String, int, byte[])} is invoked for
 * every discovered peripheral; {@link #onNotification(long, String, String, byte[])}
 * is invoked for every GATT notification and is routed to the correct
 * {@link MacOsBleConnection} by {@code connectionPtr}.
 * CoreBluetooth delivers both callbacks on its own internal dispatch queue,
 * not on a JVM thread, so the JNI bridge attaches that thread to the JVM
 * before making the call.
 *
 * <p>Thread safety: all public methods are thread-safe.
 *
 * @since 1.0
 */
public final class MacOsBleScanner implements BleScanner, BleNativeCallbacks {

    private static final Logger LOG = Logger.getLogger(MacOsBleScanner.class.getName());

    /** Base name of the native shared library (without "lib" prefix or extension). */
    static final String LIBRARY_NAME = "ble-macos";

    /**
     * Default executor used to run blocking JNI calls off the caller's thread.
     *
     * <p>Tests may inject a different executor via the package-private
     * {@link #MacOsBleScanner(NativeBridge, long, Executor)} constructor.
     */
    private static final Executor DEFAULT_EXEC =
            Executors.newCachedThreadPool(r -> {
                final Thread t = new Thread(r, "ble-macos-worker");
                t.setDaemon(true);
                return t;
            });

    /** The native bridge used for all CoreBluetooth operations. */
    private final NativeBridge bridge;

    /** Executor used to dispatch async BLE operations. */
    private final Executor exec;

    /** Opaque pointer to the native {@code BleContext} struct (cast to {@code long}). */
    private final long contextPtr;

    /** Current scan callback; {@code null} when not scanning. */
    @Nullable
    private volatile ScanCallback currentCallback;

    /**
     * Peripherals discovered since the last {@link #startScan} call, keyed by
     * peripheral UUID string.  Used to hand a {@link MacOsBleDevice} to the Java
     * callback and to look up the peripheral when {@link MacOsBleDevice#connect()}
     * is called.
     */
    private final Map<String, MacOsBleDevice> knownDevices = new ConcurrentHashMap<>();

    /**
     * Open connections keyed by the native {@code BleConnectionContext} pointer.
     * Used by {@link #onNotification(long, String, String, byte[])} to route
     * GATT notifications from the CoreBluetooth dispatch queue to the correct
     * {@link MacOsBleConnection} instance.
     */
    private final Map<Long, MacOsBleConnection> openConnections = new ConcurrentHashMap<>();

    /** Counter used to detect stale callbacks after {@link #stopScan()} or {@link #close()}. */
    private final AtomicLong scanGeneration = new AtomicLong(0L);

    /** Generation number that was active when the last startScan was issued. */
    private volatile long activeScanGeneration = 0L;

    /**
     * Constructs a {@code MacOsBleScanner} by loading the native library and
     * initialising the CoreBluetooth central manager.
     *
     * @throws BleException if the native library cannot be loaded or the
     *                      Bluetooth adapter does not become ready in time
     */
    public MacOsBleScanner() throws BleException {
        NativeLibraryLoader.load(LIBRARY_NAME);
        this.bridge = new JniNativeBridge();
        this.contextPtr = bridge.init(this);
        this.exec = DEFAULT_EXEC;
    }

    /**
     * Package-private constructor used by tests to inject a {@link NativeBridge}
     * mock, bypassing the real native library load and context initialisation.
     *
     * @param bridge     the native bridge to use; must not be {@code null}
     * @param contextPtr opaque pointer to a pre-allocated {@code BleContext}
     */
    MacOsBleScanner(final @NonNull NativeBridge bridge, final long contextPtr) {
        this(bridge, contextPtr, DEFAULT_EXEC);
    }

    /**
     * Package-private constructor used by tests to inject a {@link NativeBridge}
     * mock and a custom {@link Executor}, bypassing native loading entirely.
     *
     * @param bridge     the native bridge to use; must not be {@code null}
     * @param contextPtr opaque pointer to a pre-allocated {@code BleContext}
     * @param executor   executor to use for async BLE operations
     */
    MacOsBleScanner(
            final @NonNull NativeBridge bridge,
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
     * <p>Calls {@link NativeBridge#startScan(long, String)} on the worker
     * executor.  Any previously registered scan is implicitly replaced.
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
     * <p>Stops any active scan, calls {@link NativeBridge#destroy(long)}, and
     * releases all CoreBluetooth resources.
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
       Package-visible method — called by MacOsBleDevice to initiate a connect
       ───────────────────────────────────────────────────────────────────────── */

    /**
     * Establishes a BLE connection to the peripheral identified by the given
     * UUID string.
     *
     * <p>This method is package-private so that {@link MacOsBleDevice} can
     * delegate to it while keeping the native pointer encapsulated here.
     *
     * @param peripheralUuid the CoreBluetooth peripheral UUID string
     * @return a future that completes with the open connection
     */
    @NonNull
    CompletableFuture<MacOsBleConnection> connectPeripheral(
            final @NonNull String peripheralUuid) {

        LOG.info(() -> "Connecting to peripheral: " + peripheralUuid);
        final long ptr = contextPtr;
        return CompletableFuture.supplyAsync(() -> {
            final long connPtr;
            try {
                connPtr = bridge.connect(ptr, peripheralUuid);
            } catch (RuntimeException e) {
                LOG.warning(() -> "Connection to " + peripheralUuid + " failed: " + e.getMessage());
                throw new java.util.concurrent.CompletionException(
                        new BleException("Connection to " + peripheralUuid
                                + " failed: " + e.getMessage(), e));
            }
            final MacOsBleConnection conn = new MacOsBleConnection(connPtr, ptr, this);
            openConnections.put(connPtr, conn);
            LOG.info(() -> "Connected to peripheral: " + peripheralUuid
                    + " (connPtr=0x" + Long.toHexString(connPtr) + ")");
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
     * Writes bytes to a GATT characteristic without requesting an ATT response.
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
        final byte[] result = bridge.readCharacteristic(connectionPtr, serviceUuid, characteristicUuid);
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
       BleNativeCallbacks — called by the native layer
       ───────────────────────────────────────────────────────────────────────── */

    /**
     * Invoked by the native CoreBluetooth layer when a peripheral advertisement
     * is received.
     *
     * <p>Corresponds to the {@code CBCentralManagerDelegate} callback
     * {@code centralManager:didDiscoverPeripheral:advertisementData:RSSI:}
     * in {@code BleBridge.m}.
     *
     * @param id               the CoreBluetooth peripheral UUID string
     * @param name             the advertised local name (may be empty)
     * @param rssi             received signal strength in dBm
     * @param manufacturerData raw manufacturer-specific payload bytes extracted
     *                         from {@code CBAdvertisementDataManufacturerDataKey};
     *                         empty array if the advertisement did not include
     *                         manufacturer-specific data
     */
    @Override
    public void onDeviceFound(
            final @NonNull String id,
            final @NonNull String name,
            final int rssi,
            final byte[] manufacturerData) {

        final ScanCallback cb = currentCallback;
        if (cb == null) {
            LOG.fine(() -> "onDeviceFound: no active scan, discarding device: " + id);
            return;
        }
        LOG.info(() -> "Device found: id=" + id + " name='" + name + "' rssi=" + rssi
                + " mfrData=[" + HexFormat.of().withUpperCase().formatHex(manufacturerData) + "]");
        final MacOsBleDevice device = knownDevices.computeIfAbsent(
                id, uuid -> new MacOsBleDevice(uuid, name, rssi, manufacturerData, this));
        cb.onDeviceFound(device);
    }

    /**
     * Invoked by the native CoreBluetooth layer when a GATT characteristic
     * notification arrives.
     *
     * <p>Routes the notification to the {@link MacOsBleConnection} that owns the
     * native connection context identified by {@code connectionPtr}.  If no
     * matching connection is registered (e.g. it was disconnected concurrently)
     * the notification is silently discarded.
     *
     * <p>Corresponds to the {@code CBPeripheralDelegate} callback
     * {@code peripheral:didUpdateValueForCharacteristic:error:}
     * in {@code BleBridge.m}.
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

        final MacOsBleConnection conn = openConnections.get(connectionPtr);
        if (conn != null) {
            conn.onNotification(serviceUuid, characteristicUuid, value);
        }
    }
}
