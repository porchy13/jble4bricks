package ch.varani.bricks.ble.api.dsl;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;

import org.jspecify.annotations.NonNull;

import ch.varani.bricks.ble.api.BleConnection;
import ch.varani.bricks.ble.api.BleException;
import ch.varani.bricks.ble.device.circuitcubes.CircuitCubesProtocolConstants;

/**
 * Fluent DSL sub-builder for Circuit Cubes motor control operations.
 *
 * <p>Obtained via {@link ConnectionDsl#asCircuitCubes()}. Commands are ASCII
 * strings written to the NUS TX characteristic. Responses (including battery
 * readings) arrive as ASCII strings on the NUS RX characteristic.
 *
 * <p>Usage example:
 * <pre>{@code
 * connectionDsl.asCircuitCubes()
 *     .motorForward(CircuitCubesProtocolConstants.CHANNEL_A, 128)
 *     .motorStop(CircuitCubesProtocolConstants.CHANNEL_B)
 *     .queryBattery()
 *     .done();
 * }</pre>
 *
 * <p>Thread safety: not thread-safe; do not share across threads.
 *
 * <p><b>DSL maintenance rule:</b> any change to
 * {@link CircuitCubesProtocolConstants} must be reflected here.
 * See {@code AGENTS.md §16}.
 */
public final class CircuitCubesDsl {

    private final BleConnection connection;

    /**
     * Creates a {@code CircuitCubesDsl} wrapping the given connection.
     *
     * @param connection the active BLE connection; must not be {@code null}
     */
    CircuitCubesDsl(@NonNull BleConnection connection) {
        this.connection = connection;
    }

    // =========================================================================
    // Notifications (RX)
    // =========================================================================

    /**
     * Returns a publisher that emits raw bytes received on the NUS RX
     * characteristic (e.g. battery voltage response).
     *
     * @return a publisher of response payloads; never {@code null}
     */
    public @NonNull Publisher<byte[]> notifications() {
        return connection.notifications(
                CircuitCubesProtocolConstants.NUS_SERVICE_UUID,
                CircuitCubesProtocolConstants.RX_CHARACTERISTIC_UUID);
    }

    // =========================================================================
    // Motor commands
    // =========================================================================

    /**
     * Drives a motor channel in the forward direction at the given velocity.
     *
     * <p>The internal velocity is clamped to the range
     * 0–{@value CircuitCubesProtocolConstants#MAX_INTERNAL_VELOCITY}.
     *
     * @param channel  motor channel character (e.g.
     *                 {@link CircuitCubesProtocolConstants#CHANNEL_A})
     * @param velocity forward speed in the range
     *                 0–{@value CircuitCubesProtocolConstants#MAX_INTERNAL_VELOCITY}
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> motorForward(char channel, int velocity) {
        return sendMotorCommand(channel, Math.abs(velocity));
    }

    /**
     * Drives a motor channel in the reverse direction at the given velocity.
     *
     * @param channel  motor channel character
     * @param velocity reverse speed in the range
     *                 0–{@value CircuitCubesProtocolConstants#MAX_INTERNAL_VELOCITY}
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> motorReverse(char channel, int velocity) {
        return sendMotorCommand(channel, -Math.abs(velocity));
    }

    /**
     * Stops a motor channel.
     *
     * @param channel motor channel character
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> motorStop(char channel) {
        return sendMotorCommand(channel, 0);
    }

    /**
     * Sends a motor command with the given signed velocity.
     *
     * <p>The velocity is encoded as per the Circuit Cubes protocol:
     * <ul>
     *   <li>velocity == 0 → magnitude = 000, sign = '+'</li>
     *   <li>velocity != 0 → magnitude = 55 + abs(velocity), sign = '+'/'-'</li>
     * </ul>
     *
     * @param channel  motor channel character
     * @param velocity signed velocity (negative = reverse);
     *                 clamped to ±{@value CircuitCubesProtocolConstants#MAX_INTERNAL_VELOCITY}
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> sendMotorCommand(char channel, int velocity) {
        final int clamped = Math.max(
                -CircuitCubesProtocolConstants.MAX_INTERNAL_VELOCITY,
                Math.min(CircuitCubesProtocolConstants.MAX_INTERNAL_VELOCITY, velocity));

        final char sign;
        final int magnitude;
        if (clamped == 0) {
            sign = CircuitCubesProtocolConstants.SIGN_FORWARD;
            magnitude = CircuitCubesProtocolConstants.MAGNITUDE_STOP;
        } else {
            sign = clamped > 0
                    ? CircuitCubesProtocolConstants.SIGN_FORWARD
                    : CircuitCubesProtocolConstants.SIGN_REVERSE;
            magnitude = CircuitCubesProtocolConstants.MAGNITUDE_MIN_NONZERO + Math.abs(clamped);
        }

        final String cmd = String.format("%c%03d%c", sign, magnitude, channel);
        return writeString(cmd);
    }

    // =========================================================================
    // Battery query
    // =========================================================================

    /**
     * Sends the battery voltage query command.
     *
     * <p>The battery voltage response is delivered as an ASCII string on the
     * RX characteristic notification.
     *
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> queryBattery() {
        final byte[] msg = {CircuitCubesProtocolConstants.BATTERY_QUERY_COMMAND};
        return write(msg);
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Disconnects the Circuit Cubes brick and releases all resources.
     *
     * @throws BleException if the disconnect fails
     */
    public void done() throws BleException {
        connection.close();
    }

    /**
     * Returns the underlying {@link BleConnection}.
     *
     * @return the connection; never {@code null}
     */
    public @NonNull BleConnection connection() {
        return connection;
    }

    // =========================================================================
    // Internal helpers
    // =========================================================================

    /**
     * Encodes {@code text} as UTF-8 and writes it to the NUS TX characteristic.
     *
     * @param text the ASCII command string
     * @return a future that completes when the write is submitted
     */
    private @NonNull CompletableFuture<Void> writeString(@NonNull String text) {
        return write(text.getBytes(StandardCharsets.UTF_8));
    }

    /**
     * Writes {@code payload} to the NUS TX characteristic.
     *
     * @param payload the raw bytes to write
     * @return a future that completes when the write is submitted
     */
    private @NonNull CompletableFuture<Void> write(byte[] payload) {
        return connection.writeWithoutResponse(
                CircuitCubesProtocolConstants.NUS_SERVICE_UUID,
                CircuitCubesProtocolConstants.TX_CHARACTERISTIC_UUID,
                payload);
    }
}
