package ch.varani.bricks.ble.api.dsl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;

import org.jspecify.annotations.NonNull;

import ch.varani.bricks.ble.api.BleConnection;
import ch.varani.bricks.ble.api.BleDevice;
import ch.varani.bricks.ble.api.BleException;

/**
 * Fluent builder for an active BLE connection.
 *
 * <p>Obtained via {@link DeviceDsl#thenConnect()}. Provides access to the
 * raw connection operations as well as device-specific DSL sub-builders:
 * <pre>{@code
 * connectionDsl.asLego()
 *              .motor(0x00).startSpeed(80)
 *              .hubAction().switchOff()
 *              .done();
 * }</pre>
 *
 * <p>Closing this builder (via {@link #done()} or try-with-resources)
 * disconnects the underlying peripheral and releases all native resources.
 *
 * <p>Thread safety: not thread-safe; do not share across threads.
 */
public final class ConnectionDsl implements AutoCloseable {

    private final BleConnection connection;

    /**
     * Creates a {@code ConnectionDsl} wrapping the given connection.
     *
     * @param connection the active BLE connection; must not be {@code null}
     */
    ConnectionDsl(@NonNull BleConnection connection) {
        this.connection = connection;
    }

    // =========================================================================
    // Raw BLE operations (pass-through to BleConnection)
    // =========================================================================

    /**
     * Returns the device this connection belongs to.
     *
     * @return the associated {@link BleDevice}; never {@code null}
     */
    public @NonNull BleDevice device() {
        return connection.device();
    }

    /**
     * Writes a byte array to a GATT characteristic without waiting for a
     * response (Write Without Response).
     *
     * @param serviceUuid        GATT service UUID
     * @param characteristicUuid GATT characteristic UUID
     * @param data               bytes to write; must not be empty
     * @return a future that completes when the write is submitted
     */
    public @NonNull CompletableFuture<Void> writeWithoutResponse(
            @NonNull String serviceUuid,
            @NonNull String characteristicUuid,
            byte[] data) {
        return connection.writeWithoutResponse(serviceUuid, characteristicUuid, data);
    }

    /**
     * Subscribes to notifications from a GATT characteristic.
     *
     * @param serviceUuid        GATT service UUID
     * @param characteristicUuid GATT characteristic UUID
     * @return a publisher of notification payloads; never {@code null}
     */
    public @NonNull Publisher<byte[]> notifications(
            @NonNull String serviceUuid,
            @NonNull String characteristicUuid) {
        return connection.notifications(serviceUuid, characteristicUuid);
    }

    /**
     * Reads the current value of a GATT characteristic.
     *
     * @param serviceUuid        GATT service UUID
     * @param characteristicUuid GATT characteristic UUID
     * @return a future that completes with the read bytes; never {@code null}
     */
    public @NonNull CompletableFuture<byte[]> read(
            @NonNull String serviceUuid,
            @NonNull String characteristicUuid) {
        return connection.read(serviceUuid, characteristicUuid);
    }

    /**
     * Disconnects the peripheral asynchronously.
     *
     * @return a future that completes when the disconnect is acknowledged;
     *         never {@code null}
     */
    public @NonNull CompletableFuture<Void> disconnect() {
        return connection.disconnect();
    }

    // =========================================================================
    // Device-specific DSL sub-builders
    // =========================================================================

    /**
     * Returns a {@link LegoDsl} sub-builder for LEGO Wireless Protocol 3.0
     * operations on this connection.
     *
     * @return the LEGO DSL sub-builder; never {@code null}
     */
    public @NonNull LegoDsl asLego() {
        return new LegoDsl(connection);
    }

    /**
     * Returns an {@link SBrickDsl} sub-builder for SBrick protocol operations
     * on this connection.
     *
     * @return the SBrick DSL sub-builder; never {@code null}
     */
    public @NonNull SBrickDsl asSBrick() {
        return new SBrickDsl(connection);
    }

    /**
     * Returns a {@link CircuitCubesDsl} sub-builder for Circuit Cubes
     * operations on this connection.
     *
     * @return the Circuit Cubes DSL sub-builder; never {@code null}
     */
    public @NonNull CircuitCubesDsl asCircuitCubes() {
        return new CircuitCubesDsl(connection);
    }

    /**
     * Returns a {@link BuWizz2Dsl} sub-builder for BuWizz 2.0 protocol
     * operations on this connection.
     *
     * @return the BuWizz 2.0 DSL sub-builder; never {@code null}
     */
    public @NonNull BuWizz2Dsl asBuWizz2() {
        return new BuWizz2Dsl(connection);
    }

    /**
     * Returns a {@link BuWizz3Dsl} sub-builder for BuWizz 3.0 protocol
     * operations on this connection.
     *
     * @return the BuWizz 3.0 DSL sub-builder; never {@code null}
     */
    public @NonNull BuWizz3Dsl asBuWizz3() {
        return new BuWizz3Dsl(connection);
    }

    /**
     * Returns the underlying {@link BleConnection} for callers that need
     * direct access to the raw API.
     *
     * @return the connection; never {@code null}
     */
    public @NonNull BleConnection connection() {
        return connection;
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Disconnects the peripheral and releases all associated resources.
     *
     * <p>Equivalent to {@code connection.close()}. This method satisfies
     * both the {@link AutoCloseable} contract and the DSL terminal
     * convention of ending a chain with {@code .done()}.
     *
     * @throws BleException if the disconnect fails
     */
    public void done() throws BleException {
        connection.close();
    }

    /**
     * Closes the connection; delegates to {@link #done()}.
     *
     * @throws BleException if the disconnect fails
     */
    @Override
    public void close() throws BleException {
        done();
    }
}
