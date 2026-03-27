package ch.varani.bricks.ble.api.dsl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;

import org.jspecify.annotations.NonNull;

import ch.varani.bricks.ble.api.BleConnection;
import ch.varani.bricks.ble.api.BleException;
import ch.varani.bricks.ble.device.buwizz.BuWizz2PowerLevel;
import ch.varani.bricks.ble.device.buwizz.BuWizz2ProtocolConstants;

/**
 * Fluent DSL sub-builder for BuWizz 2.0 protocol operations.
 *
 * <p>Obtained via {@link ConnectionDsl#asBuWizz2()}. Commands are written to
 * the Application Data characteristic ({@link BuWizz2ProtocolConstants#APPLICATION_DATA_UUID})
 * within the Application service.
 *
 * <p>Usage example:
 * <pre>{@code
 * connectionDsl.asBuWizz2()
 *     .setPowerLevel(BuWizz2PowerLevel.FAST)
 *     .setMotorData(100, -100, 0, 0, false, false, false, false)
 *     .done();
 * }</pre>
 *
 * <p>Thread safety: not thread-safe; do not share across threads.
 *
 * <p><b>DSL maintenance rule:</b> any change to
 * {@link BuWizz2ProtocolConstants} must be reflected here.
 * See {@code AGENTS.md §16}.
 */
public final class BuWizz2Dsl {

    /** Brake-flag bit for channel 3. */
    private static final int BRAKE_BIT_CH3 = 0x04;

    /** Brake-flag bit for channel 4. */
    private static final int BRAKE_BIT_CH4 = 0x08;

    private final BleConnection connection;

    /**
     * Creates a {@code BuWizz2Dsl} wrapping the given connection.
     *
     * @param connection the active BLE connection; must not be {@code null}
     */
    BuWizz2Dsl(@NonNull BleConnection connection) {
        this.connection = connection;
    }

    // =========================================================================
    // Notifications (device status ~25 Hz)
    // =========================================================================

    /**
     * Returns a publisher that emits BuWizz 2.0 status report packets at
     * approximately 25 Hz.
     *
     * <p>Each emitted array is a raw command {@code 0x00} status payload as
     * defined in {@link BuWizz2ProtocolConstants}.
     *
     * @return a publisher of status report payloads; never {@code null}
     */
    public @NonNull Publisher<byte[]> notifications() {
        return connection.notifications(
                BuWizz2ProtocolConstants.APPLICATION_SERVICE_UUID,
                BuWizz2ProtocolConstants.APPLICATION_DATA_UUID);
    }

    // =========================================================================
    // Motor data (command 0x10)
    // =========================================================================

    /**
     * Sends a Set Motor Data command ({@code 0x10}).
     *
     * <p>Speed values are signed bytes where:
     * {@link BuWizz2ProtocolConstants#MOTOR_SPEED_FULL_REVERSE} = full reverse,
     * {@link BuWizz2ProtocolConstants#MOTOR_SPEED_STOP} = stop,
     * {@link BuWizz2ProtocolConstants#MOTOR_SPEED_FULL_FORWARD} = full forward.
     *
     * @param speed1  signed speed for channel 1 (−127 to 127)
     * @param speed2  signed speed for channel 2 (−127 to 127)
     * @param speed3  signed speed for channel 3 (−127 to 127)
     * @param speed4  signed speed for channel 4 (−127 to 127)
     * @param brake1  {@code true} = slow-decay (brake) on channel 1
     * @param brake2  {@code true} = slow-decay (brake) on channel 2
     * @param brake3  {@code true} = slow-decay (brake) on channel 3
     * @param brake4  {@code true} = slow-decay (brake) on channel 4
     * @return a future that completes when the write is submitted; never {@code null}
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public @NonNull CompletableFuture<Void> setMotorData(
            int speed1, int speed2, int speed3, int speed4,
            boolean brake1, boolean brake2, boolean brake3, boolean brake4) {
        final int brakeFlags = (brake1 ? 0x01 : 0)
                | (brake2 ? 0x02 : 0)
                | (brake3 ? BRAKE_BIT_CH3 : 0)
                | (brake4 ? BRAKE_BIT_CH4 : 0);
        final byte[] msg = {
            (byte) BuWizz2ProtocolConstants.CMD_SET_MOTOR_DATA,
            (byte) speed1,
            (byte) speed2,
            (byte) speed3,
            (byte) speed4,
            (byte) brakeFlags
        };
        return write(msg);
    }

    /**
     * Sends a Set Motor Data command with all channels stopped and coast mode.
     *
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> stopAllMotors() {
        return setMotorData(
                BuWizz2ProtocolConstants.MOTOR_SPEED_STOP,
                BuWizz2ProtocolConstants.MOTOR_SPEED_STOP,
                BuWizz2ProtocolConstants.MOTOR_SPEED_STOP,
                BuWizz2ProtocolConstants.MOTOR_SPEED_STOP,
                false, false, false, false);
    }

    // =========================================================================
    // Power level (command 0x11)
    // =========================================================================

    /**
     * Sends a Set Power Level command ({@code 0x11}).
     *
     * <p>The BuWizz defaults to power level {@link BuWizz2PowerLevel#DISABLED} after
     * connection. Set it to {@link BuWizz2PowerLevel#NORMAL} or higher before sending
     * motor commands.
     *
     * @param level the power level to apply
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> setPowerLevel(@NonNull BuWizz2PowerLevel level) {
        final byte[] msg = {
            (byte) BuWizz2ProtocolConstants.CMD_SET_POWER_LEVEL,
            (byte) level.code()
        };
        return write(msg);
    }

    // =========================================================================
    // Current limits (command 0x20)
    // =========================================================================

    /**
     * Sends a Set Current Limits command ({@code 0x20}).
     *
     * <p>Each limit is expressed in steps of 33 mA.
     * The default on connect is approximately 750 mA per channel.
     *
     * @param limit1 current limit for channel 1 (unsigned, steps of 33 mA)
     * @param limit2 current limit for channel 2
     * @param limit3 current limit for channel 3
     * @param limit4 current limit for channel 4
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> setCurrentLimits(
            int limit1, int limit2, int limit3, int limit4) {
        final byte[] msg = {
            (byte) BuWizz2ProtocolConstants.CMD_SET_CURRENT_LIMITS,
            (byte) limit1,
            (byte) limit2,
            (byte) limit3,
            (byte) limit4
        };
        return write(msg);
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Disconnects the BuWizz 2.0 brick and releases all resources.
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
     * Writes {@code payload} to the BuWizz 2.0 Application Data characteristic.
     *
     * @param payload the raw bytes to write
     * @return a future that completes when the write is submitted
     */
    private @NonNull CompletableFuture<Void> write(byte[] payload) {
        return connection.writeWithoutResponse(
                BuWizz2ProtocolConstants.APPLICATION_SERVICE_UUID,
                BuWizz2ProtocolConstants.APPLICATION_DATA_UUID,
                payload);
    }
}
