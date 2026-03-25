package ch.varani.lego.ble.api;

import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.NonNull;

/**
 * Represents a discovered BLE peripheral that has not yet been connected.
 *
 * <p>Instances are created by the scanning layer and passed to the caller's
 * {@link ScanCallback}. All fields are immutable.
 *
 * <p>Thread safety: instances are immutable and therefore safe for use from
 * multiple threads without synchronisation.
 */
public interface BleDevice {

    /**
     * Returns the platform-specific peripheral identifier.
     *
     * <p>On macOS this is the CoreBluetooth {@code NSUUID} string.
     * On Windows it is the BLE address as a hex string.
     * On Linux it is the BlueZ D-Bus object path.
     *
     * @return a non-null, non-empty identifier string
     */
    @NonNull
    String id();

    /**
     * Returns the advertised device name, or an empty string if the device
     * did not include a Local Name AD structure in its advertisement.
     *
     * @return advertised name, never {@code null}
     */
    @NonNull
    String name();

    /**
     * Returns the received signal strength indicator (RSSI) at the time the
     * advertisement was received, in dBm.
     *
     * @return RSSI value in dBm
     */
    int rssi();

    /**
     * Initiates a connection to this peripheral.
     *
     * <p>The returned future completes with a {@link BleConnection} when the
     * underlying BLE connection is established and service discovery has
     * finished. The future completes exceptionally with a
     * {@link BleException} if the connection or service discovery fails.
     *
     * <p>This method may be called from any thread.
     *
     * @return a future that completes with the open connection
     */
    @NonNull
    CompletableFuture<BleConnection> connect();
}
