package ch.varani.bricks.ble.api.dsl;

import java.util.ArrayList;
import java.util.HexFormat;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.TimeoutException;
import java.util.function.Predicate;
import java.util.logging.Logger;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

import ch.varani.bricks.ble.api.BleDevice;
import ch.varani.bricks.ble.api.BleException;
import ch.varani.bricks.ble.api.BleScanner;
import ch.varani.bricks.ble.device.buwizz.BuWizz2ProtocolConstants;
import ch.varani.bricks.ble.device.buwizz.BuWizz3ProtocolConstants;
import ch.varani.bricks.ble.device.circuitcubes.CircuitCubesProtocolConstants;
import ch.varani.bricks.ble.device.lego.LegoHubType;
import ch.varani.bricks.ble.device.lego.LegoProtocolConstants;
import ch.varani.bricks.ble.device.sbrick.SBrickProtocolConstants;

/**
 * Fluent builder for BLE scan operations.
 *
 * <p>Obtained via {@link BrickDsl#scan()}. The typical usage is:
 * <pre>{@code
 * dsl.scan()
 *    .forLegoHubs()          // filter by LEGO hub GATT service UUID
 *    .timeoutSeconds(10)     // stop automatically after 10 s
 *    .first()                // take only the first discovered device
 *    .thenConnect();         // connect and return a ConnectionDsl
 * }</pre>
 *
 * <p>Thread safety: instances of this class are not thread-safe and must
 * not be shared across threads.
 */
public final class ScanDsl {

    private static final Logger LOG = Logger.getLogger(ScanDsl.class.getName());

    /** Default scan timeout in seconds (no timeout by default). */
    private static final long NO_TIMEOUT = 0L;

    /** Default maximum number of devices to collect (no limit by default). */
    private static final int NO_LIMIT = 0;

    private final BleScanner scanner;

    @Nullable
    private String serviceUuidFilter;
    private long timeoutSeconds;
    private int maxDevices;

    /**
     * Post-scan predicate applied to every discovered device before it is
     * admitted to the result list.  A {@code null} value means no filtering
     * (every device is accepted).
     */
    @Nullable
    private Predicate<BleDevice> deviceFilter;

    /**
     * Creates a {@code ScanDsl} wrapping the given scanner.
     *
     * @param scanner the BLE scanner to delegate to; must not be {@code null}
     */
    ScanDsl(@NonNull BleScanner scanner) {
        this.scanner = scanner;
        this.serviceUuidFilter = null;
        this.timeoutSeconds = NO_TIMEOUT;
        this.maxDevices = NO_LIMIT;
        this.deviceFilter = null;
    }

    // =========================================================================
    // Filter helpers — named shortcuts for common GATT service UUIDs
    // =========================================================================

    /**
     * Restricts the scan to peripherals advertising the LEGO Hub GATT service.
     *
     * <p>Sets the service UUID filter to
     * {@link LegoProtocolConstants#HUB_SERVICE_UUID}.
     *
     * <p><b>WeDo 2.0 compatibility note:</b> the WeDo 2.0 hub does <em>not</em>
     * include the LWP3 service UUID ({@value LegoProtocolConstants#HUB_SERVICE_UUID})
     * in its BLE advertisement packet.  On macOS this UUID is passed directly to
     * {@code CBCentralManager.scanForPeripherals(withServices:)}, which means
     * CoreBluetooth will silently suppress WeDo 2.0 devices and {@link #collect}
     * will time out without finding any hub.  When scanning for WeDo 2.0 hubs,
     * use {@link #forWeDo2()} alone (without this method):
     * <pre>{@code
     * dsl.scan()
     *    .forWeDo2()        // manufacturer-data filter only — no OS-level UUID filter
     *    .timeoutSeconds(15)
     *    .first();
     * }</pre>
     * For all other LEGO hubs (City Hub, Technic Hub, …) combining
     * {@code forLegoHubs()} with the hub-type filter is both correct and more
     * efficient as the OS-level filter reduces radio traffic.
     *
     * @return this builder for chaining
     */
    public @NonNull ScanDsl forLegoHubs() {
        this.serviceUuidFilter = LegoProtocolConstants.HUB_SERVICE_UUID;
        return this;
    }

    /**
     * Restricts the scan to peripherals advertising the SBrick Remote Control
     * GATT service.
     *
     * <p>Sets the service UUID filter to
     * {@link SBrickProtocolConstants#REMOTE_CONTROL_SERVICE_UUID}.
     *
     * @return this builder for chaining
     */
    public @NonNull ScanDsl forSBricks() {
        this.serviceUuidFilter = SBrickProtocolConstants.REMOTE_CONTROL_SERVICE_UUID;
        return this;
    }

    /**
     * Restricts the scan to peripherals advertising the Circuit Cubes NUS
     * service.
     *
     * <p>Sets the service UUID filter to
     * {@link CircuitCubesProtocolConstants#NUS_SERVICE_UUID}.
     *
     * @return this builder for chaining
     */
    public @NonNull ScanDsl forCircuitCubes() {
        this.serviceUuidFilter = CircuitCubesProtocolConstants.NUS_SERVICE_UUID;
        return this;
    }

    /**
     * Restricts the scan to BuWizz 2.0 peripherals using the Application
     * service UUID.
     *
     * <p>Sets the service UUID filter to
     * {@link BuWizz2ProtocolConstants#APPLICATION_SERVICE_UUID}.
     *
     * @return this builder for chaining
     */
    public @NonNull ScanDsl forBuWizz2() {
        this.serviceUuidFilter = BuWizz2ProtocolConstants.APPLICATION_SERVICE_UUID;
        return this;
    }

    /**
     * Restricts the scan to BuWizz 3.0 peripherals using the Application
     * service UUID.
     *
     * <p>Sets the service UUID filter to
     * {@link BuWizz3ProtocolConstants#APPLICATION_SERVICE_UUID}.
     *
     * @return this builder for chaining
     */
    public @NonNull ScanDsl forBuWizz3() {
        this.serviceUuidFilter = BuWizz3ProtocolConstants.APPLICATION_SERVICE_UUID;
        return this;
    }

    /**
     * Applies a custom GATT service UUID filter.
     *
     * <p>Pass {@code null} to scan without any service UUID filter.
     *
     * @param uuid 128-bit UUID string, or {@code null} for no filter
     * @return this builder for chaining
     */
    public @NonNull ScanDsl forService(@Nullable String uuid) {
        this.serviceUuidFilter = uuid;
        return this;
    }

    /**
     * Restricts the scan to the single device whose BLE identifier equals
     * {@code deviceId}.
     *
     * <p>All other discovered devices are silently discarded by the
     * {@link #collect(int)} callback, so the scan will block until exactly
     * the named device appears (or the timeout expires).
     *
     * <p>This filter is additive with {@link #forLegoHubType} and
     * {@link #forService}.
     *
     * @param deviceId the BLE device identifier to match (e.g. a CoreBluetooth
     *                 peripheral UUID, a BlueZ D-Bus path, or a WinRT address
     *                 string); must not be {@code null}
     * @return this builder for chaining
     */
    public @NonNull ScanDsl withDeviceId(final @NonNull String deviceId) {
        this.deviceFilter = andFilter(deviceFilter, d -> deviceId.equals(d.id()));
        return this;
    }

    /**
     * Restricts the scan to LEGO hubs of the specified hub type.
     *
     * <p>The hub type is identified by reading the System Type and Device
     * Number byte (index {@link LegoProtocolConstants#MANUFACTURER_DATA_IDX_SYSTEM_TYPE})
     * from the manufacturer-specific advertisement payload and comparing it
     * with {@link LegoHubType#systemTypeDeviceByte()}.  Devices with a payload
     * shorter than {@link LegoProtocolConstants#MANUFACTURER_DATA_MIN_LENGTH}
     * bytes are rejected.
     *
     * <p>Combine with {@link #forLegoHubs()} to also apply a GATT service UUID
     * filter at the OS level (more efficient):
     * <pre>{@code
     * dsl.scan()
     *    .forLegoHubs()
     *    .forLegoHubType(LegoHubType.CITY_HUB)
     *    .timeoutSeconds(10)
     *    .first();
     * }</pre>
     *
     * @param type the hub type to match; must not be {@code null}
     * @return this builder for chaining
     */
    public @NonNull ScanDsl forLegoHubType(final @NonNull LegoHubType type) {
        this.deviceFilter = andFilter(deviceFilter, d -> matchesLegoHubType(d, type));
        return this;
    }

    /**
     * Convenience shortcut for {@code forLegoHubType(LegoHubType.WEDO2_HUB)}.
     *
     * <p>Restricts the scan to WeDo 2.0 hubs only.
     *
     * @return this builder for chaining
     */
    public @NonNull ScanDsl forWeDo2() {
        return forLegoHubType(LegoHubType.WEDO2_HUB);
    }

    // =========================================================================
    // Private helpers
    // =========================================================================

    /**
     * Returns a predicate that is the logical AND of {@code existing} and
     * {@code additional}, or {@code additional} alone if {@code existing}
     * is {@code null}.
     *
     * @param existing   the current predicate; may be {@code null}
     * @param additional the new predicate to AND in; must not be {@code null}
     * @return the combined predicate
     */
    private static @NonNull Predicate<BleDevice> andFilter(
            final @Nullable Predicate<BleDevice> existing,
            final @NonNull Predicate<BleDevice> additional) {
        return existing == null ? additional : existing.and(additional);
    }

    /**
     * Returns {@code true} if the manufacturer-specific advertisement payload
     * of {@code device} indicates it is a LEGO hub of the specified type.
     *
     * @param device the device whose manufacturer data should be inspected
     * @param type   the expected hub type
     * @return {@code true} if the device matches
     */
    private static boolean matchesLegoHubType(
            final @NonNull BleDevice device,
            final @NonNull LegoHubType type) {
        final byte[] data = device.manufacturerData();
        if (data.length < LegoProtocolConstants.MANUFACTURER_DATA_MIN_LENGTH) {
            return false;
        }
        final int systemTypeByte =
                data[LegoProtocolConstants.MANUFACTURER_DATA_IDX_SYSTEM_TYPE] & 0xFF;
        return systemTypeByte == type.systemTypeDeviceByte();
    }

    // =========================================================================
    // Scan control
    // =========================================================================

    /**
     * Stops the scan automatically after the given number of seconds.
     *
     * <p>A value of {@code 0} (the default) means the scan runs until
     * {@link BleScanner#stopScan()} is called explicitly or until
     * {@link #first()} / {@link #collect(int)} receives enough devices.
     *
     * @param seconds scan duration in seconds; must be ≥ 0
     * @return this builder for chaining
     * @throws IllegalArgumentException if {@code seconds} is negative
     */
    public @NonNull ScanDsl timeoutSeconds(long seconds) {
        if (seconds < 0) {
            throw new IllegalArgumentException(
                    "Scan timeout must be >= 0, got: " + seconds);
        }
        this.timeoutSeconds = seconds;
        return this;
    }

    /**
     * Returns the scan timeout (in seconds) configured on this builder.
     *
     * <p>A value of {@code 0} means no timeout.
     *
     * @return timeout in seconds, ≥ 0
     */
    long timeoutSeconds() {
        return timeoutSeconds;
    }

    // =========================================================================
    // Terminal operations — start the scan and wait for results
    // =========================================================================

    /**
     * Starts a scan and blocks until the first device is discovered, then
     * stops the scan.
     *
     * <p>If a timeout was set via {@link #timeoutSeconds(long)} and no device
     * is found within that period, this method completes exceptionally with a
     * {@link BleException}.
     *
     * @return a {@link DeviceDsl} wrapping the first discovered device;
     *         never {@code null}
     * @throws BleException if the scan fails or times out without finding a device
     */
    public @NonNull DeviceDsl first() throws BleException {
        final List<BleDevice> found = collect(1);
        return new DeviceDsl(found.get(0));
    }

    /**
     * Starts a scan and blocks until {@code count} devices have been discovered
     * or the configured timeout elapses, then stops the scan.
     *
     * <p>The timeout set via {@link #timeoutSeconds(long)} is a <em>total</em>
     * budget shared across both the scan-start phase and the discovery phase.
     * A deadline is computed at the moment {@code collect} is called, and both
     * phases together must complete within that deadline.
     *
     * @param count the number of devices to collect; must be ≥ 1
     * @return an immutable list of at most {@code count} discovered devices;
     *         never {@code null}
     * @throws BleException             if the scan cannot be started or if it
     *                                  times out before {@code count} devices are found
     * @throws IllegalArgumentException if {@code count} is less than 1
     */
    public @NonNull List<BleDevice> collect(int count) throws BleException {
        if (count < 1) {
            throw new IllegalArgumentException(
                    "count must be >= 1, got: " + count);
        }
        this.maxDevices = count;
        final List<BleDevice> devices = new ArrayList<>();
        final CompletableFuture<Void> done = new CompletableFuture<>();

        LOG.info(() -> "Starting scan: serviceUuidFilter=" + serviceUuidFilter
                + " deviceFilter=" + (deviceFilter != null ? "set" : "none")
                + " count=" + count
                + " timeout=" + (timeoutSeconds > NO_TIMEOUT ? timeoutSeconds + "s" : "none"));

        /*
         * Record the deadline in nanoseconds before starting the scan so that
         * the timeout budget is shared (not doubled) across the startScan and
         * discovery phases.
         */
        final long deadlineNanos = timeoutSeconds > NO_TIMEOUT
                ? System.nanoTime() + TimeUnit.SECONDS.toNanos(timeoutSeconds)
                : Long.MAX_VALUE;

        final CompletableFuture<Void> scanStarted = scanner.startScan(
                serviceUuidFilter,
                device -> onDeviceFound(device, devices, done));

        try {
            awaitWithDeadline(scanStarted, done, deadlineNanos, count);
        } finally {
            stopScanAndAwait();
        }

        final List<BleDevice> result = List.copyOf(devices);
        LOG.info(() -> "Scan completed: found " + result.size() + " device(s)");
        return result;
    }

    /**
     * Callback invoked by the scanner for each discovered peripheral.
     *
     * <p>Applies the device filter, appends matching devices to the collection
     * list, and completes {@code done} once enough devices have been found.
     *
     * @param device  the discovered peripheral
     * @param devices the shared accumulator list (guarded by its own monitor)
     * @param done    the future to complete once {@link #maxDevices} devices are collected
     */
    private void onDeviceFound(
            final @NonNull BleDevice device,
            final @NonNull List<BleDevice> devices,
            final @NonNull CompletableFuture<Void> done) {
        final byte[] mfrData = device.manufacturerData();
        LOG.fine(() -> {
            final String hex = mfrDataHex(mfrData);
            final String byte3 = mfrData.length >= LegoProtocolConstants.MANUFACTURER_DATA_MIN_LENGTH
                    ? String.format("0x%02X", mfrData[LegoProtocolConstants.MANUFACTURER_DATA_IDX_SYSTEM_TYPE])
                    : "n/a (len=" + mfrData.length + ")";
            return "onDeviceFound pre-filter: id=" + device.id()
                    + " name='" + device.name() + "'"
                    + " mfrData=[" + hex + "]"
                    + " byte[3]=" + byte3;
        });
        if (deviceFilter != null && !deviceFilter.test(device)) {
            LOG.fine(() -> "Device rejected by filter: " + device.id());
            return;
        }
        LOG.fine(() -> "Device accepted by filter: " + device.id());
        synchronized (devices) {
            if (devices.size() < maxDevices) {
                devices.add(device);
            }
            if (devices.size() >= maxDevices) {
                done.complete(null);
            }
        }
    }

    /**
     * Waits for {@code scanStarted} then {@code done}, respecting the given
     * nanosecond deadline.  When {@code deadlineNanos} is {@link Long#MAX_VALUE}
     * the waits are unbounded.
     *
     * @param scanStarted   future that completes once the scan has been started
     * @param done          future that completes once enough devices are found
     * @param deadlineNanos absolute deadline in nanoseconds ({@link Long#MAX_VALUE} = no limit)
     * @param count         number of devices expected (used in the error message)
     * @throws BleException if either future times out, is interrupted, or fails
     */
    private void awaitWithDeadline(
            final @NonNull CompletableFuture<Void> scanStarted,
            final @NonNull CompletableFuture<Void> done,
            final long deadlineNanos,
            final int count) throws BleException {
        try {
            if (deadlineNanos == Long.MAX_VALUE) {
                scanStarted.get();
                done.get();
            } else {
                awaitWithTimeout(scanStarted, done, deadlineNanos);
            }
        } catch (TimeoutException ex) {
            throw new BleException(
                    "Scan timed out after " + timeoutSeconds
                    + " second(s) without finding " + count + " device(s).",
                    ex);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BleException("Scan interrupted.", ex);
        } catch (ExecutionException ex) {
            throw new BleException("Scan failed: " + ex.getCause().getMessage(),
                    ex.getCause());
        }
    }

    /**
     * Waits for {@code scanStarted} then {@code done} using the remaining time
     * before {@code deadlineNanos}.
     *
     * <p>If the remaining time is zero or negative when either {@code get} call
     * is reached, {@code CompletableFuture.get} will immediately throw
     * {@link java.util.concurrent.TimeoutException}, so no explicit guard is needed.
     *
     * @param scanStarted   the scan-start future
     * @param done          the discovery-complete future
     * @param deadlineNanos absolute deadline in nanoseconds
     * @throws InterruptedException if interrupted while waiting
     * @throws ExecutionException   if a future completes exceptionally
     * @throws TimeoutException     if the deadline is exceeded
     */
    private static void awaitWithTimeout(
            final @NonNull CompletableFuture<Void> scanStarted,
            final @NonNull CompletableFuture<Void> done,
            final long deadlineNanos)
            throws InterruptedException, ExecutionException, TimeoutException {

        scanStarted.get(deadlineNanos - System.nanoTime(), TimeUnit.NANOSECONDS);
        done.get(deadlineNanos - System.nanoTime(), TimeUnit.NANOSECONDS);
    }

    /**
     * Calls {@link BleScanner#stopScan()} and blocks until it completes.
     *
     * <p>Blocking here ensures that no stale {@code onDeviceFound} callbacks
     * can fire after {@link #collect} returns, and that a subsequent scan
     * starts from a clean state.  Errors are logged but not propagated so
     * that the original exception (if any) is preserved.
     */
    private void stopScanAndAwait() {
        try {
            scanner.stopScan().get();
        } catch (InterruptedException ie) {
            Thread.currentThread().interrupt();
            LOG.warning("Interrupted while waiting for scan to stop");
        } catch (ExecutionException ee) {
            LOG.warning("Error stopping scan: " + ee.getCause().getMessage());
        }
    }

    /**
     * Returns the service UUID filter currently configured on this builder.
     *
     * <p>This method is intended for testing and introspection only.
     *
     * @return the UUID string, or {@code null} if no filter is set
     */
    @Nullable
    String serviceUuidFilter() {
        return serviceUuidFilter;
    }

    /**
     * Returns the device predicate filter currently configured on this builder.
     *
     * <p>This method is intended for testing and introspection only.
     *
     * @return the predicate, or {@code null} if no device filter is set
     */
    @Nullable
    Predicate<BleDevice> deviceFilter() {
        return deviceFilter;
    }

    /**
     * Formats a byte array as an uppercase hex string with no separator.
     * Returns an empty string for an empty array.
     *
     * @param data a non-null byte array (callers guarantee non-null via {@code @NonNull})
     * @return uppercase hex string, or {@code ""} if {@code data} is empty
     */
    private static String mfrDataHex(final byte @NonNull [] data) {
        if (data.length == 0) {
            return "";
        }
        return HexFormat.of().withUpperCase().formatHex(data);
    }
}
