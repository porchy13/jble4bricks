package ch.varani.bricks.ble.api.dsl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;

import org.jspecify.annotations.NonNull;

import ch.varani.bricks.ble.api.BleConnection;
import ch.varani.bricks.ble.api.BleDevice;
import ch.varani.bricks.ble.api.BleException;

/**
 * Fluent builder that wraps a discovered (but not yet connected)
 * {@link BleDevice}.
 *
 * <p>Instances are returned by {@link ScanDsl#first()} and are the
 * bridge between the scan phase and the connection phase of the DSL.
 *
 * <p>Thread safety: not thread-safe; do not share across threads.
 */
public final class DeviceDsl {

    private final BleDevice device;

    /**
     * Creates a {@code DeviceDsl} wrapping the given device.
     *
     * @param device the discovered peripheral; must not be {@code null}
     */
    DeviceDsl(@NonNull BleDevice device) {
        this.device = device;
    }

    /**
     * Returns the advertised name of the underlying device.
     *
     * @return the device name; never {@code null}
     */
    public @NonNull String name() {
        return device.name();
    }

    /**
     * Returns the platform-specific identifier of the underlying device.
     *
     * @return the device identifier; never {@code null}
     */
    public @NonNull String id() {
        return device.id();
    }

    /**
     * Returns the RSSI of the underlying device at discovery time, in dBm.
     *
     * @return RSSI in dBm
     */
    public int rssi() {
        return device.rssi();
    }

    /**
     * Returns the underlying {@link BleDevice} for callers that need direct
     * access to the raw API.
     *
     * @return the device; never {@code null}
     */
    public @NonNull BleDevice device() {
        return device;
    }

    /**
     * Initiates a BLE connection to the wrapped device and returns a
     * {@link ConnectionDsl} for the active session.
     *
     * <p>This method blocks until the connection is established and service
     * discovery has completed.
     *
     * @return a {@link ConnectionDsl} for the active connection; never {@code null}
     * @throws BleException if the connection or service discovery fails
     */
    public @NonNull ConnectionDsl thenConnect() throws BleException {
        final CompletableFuture<BleConnection> future = device.connect();
        try {
            final BleConnection connection = future.get();
            return new ConnectionDsl(connection);
        } catch (InterruptedException ex) {
            Thread.currentThread().interrupt();
            throw new BleException("Connection interrupted for device: " + device.id(), ex);
        } catch (ExecutionException ex) {
            throw new BleException(
                    "Connection failed for device '" + device.name() + "': "
                    + ex.getCause().getMessage(),
                    ex.getCause());
        }
    }
}
