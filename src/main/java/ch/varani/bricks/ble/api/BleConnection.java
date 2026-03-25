package ch.varani.bricks.ble.api;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;

import org.jspecify.annotations.NonNull;

/**
 * Represents an active BLE connection to a peripheral.
 *
 * <p>A connection is obtained by calling {@link BleDevice#connect()}.
 * When the application is finished with the peripheral it must call
 * {@link #disconnect()} to release the underlying platform resource.
 *
 * <p>Thread safety: implementations must be thread-safe. Methods may be
 * called concurrently from any thread.
 */
public interface BleConnection extends AutoCloseable {

    /**
     * Returns the device this connection belongs to.
     *
     * @return the associated {@link BleDevice}, never {@code null}
     */
    @NonNull
    BleDevice device();

    /**
     * Writes a byte array to the specified GATT characteristic without
     * waiting for a response from the peripheral (Write Without Response).
     *
     * <p>The returned future completes with {@link Void} ({@code null}) when
     * the write has been handed to the platform layer, or completes
     * exceptionally with a {@link BleException} on failure.
     *
     * @param serviceUuid        the UUID string of the GATT service
     * @param characteristicUuid the UUID string of the GATT characteristic
     * @param data               the bytes to write; must not be empty
     * @return a future that completes when the write is submitted
     */
    @NonNull
    CompletableFuture<Void> writeWithoutResponse(
            @NonNull String serviceUuid,
            @NonNull String characteristicUuid,
            @NonNull byte[] data);

    /**
     * Subscribes to notifications from the specified GATT characteristic.
     *
     * <p>Returns a {@link Publisher} that emits byte arrays for every
     * notification received from the peripheral. The publisher completes
     * when the connection is closed or when the subscriber cancels.
     *
     * @param serviceUuid        the UUID string of the GATT service
     * @param characteristicUuid the UUID string of the GATT characteristic
     * @return a publisher of notification payloads
     */
    @NonNull
    Publisher<byte[]> notifications(
            @NonNull String serviceUuid,
            @NonNull String characteristicUuid);

    /**
     * Reads the current value of the specified GATT characteristic.
     *
     * <p>The returned future completes with the characteristic value, or
     * completes exceptionally with a {@link BleException} on failure.
     *
     * @param serviceUuid        the UUID string of the GATT service
     * @param characteristicUuid the UUID string of the GATT characteristic
     * @return a future that completes with the read bytes
     */
    @NonNull
    CompletableFuture<byte[]> read(
            @NonNull String serviceUuid,
            @NonNull String characteristicUuid);

    /**
     * Disconnects from the peripheral and releases all associated resources.
     *
     * <p>After this call the connection object must not be used. This method
     * is idempotent — calling it more than once has no additional effect.
     *
     * @return a future that completes when the disconnect is acknowledged
     */
    @NonNull
    CompletableFuture<Void> disconnect();

    /**
     * Convenience method that calls {@link #disconnect()} and blocks until it
     * completes, to satisfy the {@link AutoCloseable} contract for
     * try-with-resources usage.
     *
     * @throws BleException if the disconnect fails
     */
    @Override
    void close() throws BleException;
}
