package ch.varani.lego.ble.api;

import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Scans for nearby BLE peripherals.
 *
 * <p>Obtain an instance via {@link BleScannerFactory#create()}.
 *
 * <p>Usage pattern:
 * <pre>{@code
 * try (BleScanner scanner = BleScannerFactory.create()) {
 *     scanner.startScan(device -> System.out.println("Found: " + device.name()))
 *            .thenRun(() -> System.out.println("Scan started"));
 *     // ... wait, then:
 *     scanner.stopScan();
 * }
 * }</pre>
 *
 * <p>Thread safety: all methods are safe to call from any thread.
 */
public interface BleScanner extends AutoCloseable {

    /**
     * Starts a passive BLE scan and delivers discovered peripherals to the
     * given callback.
     *
     * <p>The returned future completes when the platform has confirmed that
     * scanning has started, or completes exceptionally with a
     * {@link BleException} if scanning cannot be started (e.g. Bluetooth
     * is disabled or permission was denied).
     *
     * <p>If a scan is already running, calling this method again stops the
     * previous scan and starts a fresh one with the new callback.
     *
     * @param callback invoked for each discovered peripheral; must not be {@code null}
     * @return a future that completes when scanning has started
     */
    @NonNull
    CompletableFuture<Void> startScan(@NonNull ScanCallback callback);

    /**
     * Starts a filtered BLE scan that only reports peripherals advertising
     * the specified GATT service UUID.
     *
     * @param serviceUuid the 128-bit UUID string to filter on; {@code null} means no filter
     * @param callback    invoked for each matching peripheral; must not be {@code null}
     * @return a future that completes when scanning has started
     */
    @NonNull
    CompletableFuture<Void> startScan(
            @Nullable String serviceUuid,
            @NonNull ScanCallback callback);

    /**
     * Stops the active scan.
     *
     * <p>If no scan is running this method is a no-op. The returned future
     * completes when the platform has confirmed that scanning has stopped.
     *
     * @return a future that completes when scanning has stopped
     */
    @NonNull
    CompletableFuture<Void> stopScan();

    /**
     * Returns {@code true} if a scan is currently running.
     *
     * @return {@code true} while a scan is active
     */
    boolean isScanning();

    /**
     * Stops any active scan and releases all platform resources held by this
     * scanner.
     *
     * @throws BleException if releasing resources fails
     */
    @Override
    void close() throws BleException;
}
