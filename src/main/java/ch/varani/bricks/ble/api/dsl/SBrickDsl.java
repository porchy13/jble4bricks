package ch.varani.bricks.ble.api.dsl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;

import org.jspecify.annotations.NonNull;

import ch.varani.bricks.ble.api.BleConnection;
import ch.varani.bricks.ble.api.BleException;
import ch.varani.bricks.ble.device.sbrick.SBrickChannel;
import ch.varani.bricks.ble.device.sbrick.SBrickDirection;
import ch.varani.bricks.ble.device.sbrick.SBrickProtocolConstants;

/**
 * Fluent DSL sub-builder for SBrick BLE protocol operations.
 *
 * <p>Obtained via {@link ConnectionDsl#asSBrick()}. All commands are sent as
 * Write Without Response to
 * {@link SBrickProtocolConstants#REMOTE_CONTROL_COMMANDS_UUID} within the
 * {@link SBrickProtocolConstants#REMOTE_CONTROL_SERVICE_UUID} service.
 *
 * <p>Usage example:
 * <pre>{@code
 * connectionDsl.asSBrick()
 *     .drive(SBrickChannel.A, SBrickDirection.CLOCKWISE, 200)
 *     .drive(SBrickChannel.B, SBrickDirection.COUNTER_CLOCKWISE, 128)
 *     .brake(SBrickChannel.A)
 *     .done();
 * }</pre>
 *
 * <p>Thread safety: not thread-safe; do not share across threads.
 *
 * <p><b>DSL maintenance rule:</b> any change to
 * {@link SBrickProtocolConstants} must be reflected here. See {@code AGENTS.md §16}.
 */
public final class SBrickDsl {

    /** Byte mask for extracting the lowest 8 bits of an integer (power value). */
    private static final int BYTE_MASK = 0xFF;

    private final BleConnection connection;

    /**
     * Creates an {@code SBrickDsl} wrapping the given connection.
     *
     * @param connection the active BLE connection; must not be {@code null}
     */
    SBrickDsl(@NonNull BleConnection connection) {
        this.connection = connection;
    }

    // =========================================================================
    // Notifications
    // =========================================================================

    /**
     * Returns a publisher that emits raw command response bytes from the
     * SBrick Remote Control characteristic.
     *
     * @return a publisher of response payloads; never {@code null}
     */
    public @NonNull Publisher<byte[]> notifications() {
        return connection.notifications(
                SBrickProtocolConstants.REMOTE_CONTROL_SERVICE_UUID,
                SBrickProtocolConstants.REMOTE_CONTROL_COMMANDS_UUID);
    }

    // =========================================================================
    // Motor commands
    // =========================================================================

    /**
     * Sends a Drive command ({@code 0x01}) for one channel.
     *
     * <p>Power {@code 0x00} is freewheel; {@code 0xFF} is full power.
     *
     * @param channel   the motor channel (e.g. {@link SBrickChannel#A})
     * @param direction the rotation direction (e.g. {@link SBrickDirection#CLOCKWISE})
     * @param power     power value in the range 0–255
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> drive(
            @NonNull SBrickChannel channel,
            @NonNull SBrickDirection direction,
            int power) {
        final byte[] msg = {
            (byte) SBrickProtocolConstants.CMD_DRIVE,
            (byte) channel.code(),
            (byte) direction.code(),
            (byte) (power & BYTE_MASK)
        };
        return write(msg);
    }

    /**
     * Sends a Brake command ({@code 0x00}) for one or more channels.
     *
     * @param channels one or more motor channels (e.g. {@link SBrickChannel#A},
     *                 {@link SBrickChannel#B})
     * @return a future that completes when the write is submitted; never {@code null}
     * @throws IllegalArgumentException if no channels are specified
     */
    public @NonNull CompletableFuture<Void> brake(@NonNull SBrickChannel... channels) {
        if (channels.length == 0) {
            throw new IllegalArgumentException("At least one channel must be specified.");
        }
        final byte[] msg = new byte[1 + channels.length];
        msg[0] = (byte) SBrickProtocolConstants.CMD_BRAKE;
        for (int i = 0; i < channels.length; i++) {
            msg[1 + i] = (byte) channels[i].code();
        }
        return write(msg);
    }

    // =========================================================================
    // ADC queries
    // =========================================================================

    /**
     * Sends a Query ADC command ({@code 0x0F}) for the given channel.
     *
     * <p>The SBrick will respond via the Remote Control characteristic
     * notification.
     *
     * @param adcChannel ADC channel (0–9; see
     *                   {@link SBrickProtocolConstants#ADC_CHANNEL_BATTERY})
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> queryAdc(int adcChannel) {
        final byte[] msg = {
            (byte) SBrickProtocolConstants.CMD_QUERY_ADC,
            (byte) adcChannel
        };
        return write(msg);
    }

    /**
     * Queries the battery voltage ADC channel.
     *
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> queryBatteryVoltage() {
        return queryAdc(SBrickProtocolConstants.ADC_CHANNEL_BATTERY);
    }

    /**
     * Queries the internal temperature ADC channel.
     *
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> queryTemperature() {
        return queryAdc(SBrickProtocolConstants.ADC_CHANNEL_TEMPERATURE);
    }

    // =========================================================================
    // Watchdog
    // =========================================================================

    /**
     * Sets the watchdog timeout.
     *
     * <p>Each unit equals 0.1 s. A value of {@code 0} disables the watchdog.
     * The recommended range is 2–5 (0.2–0.5 s).
     *
     * @param timeout watchdog timeout in units of 0.1 s; 0 = disabled
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> setWatchdogTimeout(int timeout) {
        final byte[] msg = {
            (byte) SBrickProtocolConstants.CMD_SET_WATCHDOG,
            (byte) timeout
        };
        return write(msg);
    }

    /**
     * Requests the current watchdog timeout setting.
     *
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> getWatchdogTimeout() {
        final byte[] msg = {(byte) SBrickProtocolConstants.CMD_GET_WATCHDOG};
        return write(msg);
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Disconnects the SBrick and releases all resources.
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
     * Writes {@code payload} to the SBrick Remote Control Commands characteristic.
     *
     * @param payload the raw bytes to write
     * @return a future that completes when the write is submitted
     */
    private @NonNull CompletableFuture<Void> write(byte[] payload) {
        return connection.writeWithoutResponse(
                SBrickProtocolConstants.REMOTE_CONTROL_SERVICE_UUID,
                SBrickProtocolConstants.REMOTE_CONTROL_COMMANDS_UUID,
                payload);
    }
}
