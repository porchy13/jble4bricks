package ch.varani.bricks.ble.api.dsl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;

import org.jspecify.annotations.NonNull;

import ch.varani.bricks.ble.api.BleConnection;
import ch.varani.bricks.ble.api.BleException;
import ch.varani.bricks.ble.device.buwizz.BuWizz3ProtocolConstants;

/**
 * Fluent DSL sub-builder for BuWizz 3.0 protocol operations.
 *
 * <p>Obtained via {@link ConnectionDsl#asBuWizz3()}. Commands are written to
 * the Application characteristic ({@link BuWizz3ProtocolConstants#APPLICATION_CHARACTERISTIC_UUID})
 * within the Application service.
 *
 * <p>Usage example:
 * <pre>{@code
 * connectionDsl.asBuWizz3()
 *     .setPowerLevel(BuWizz3ProtocolConstants.CMD_SET_MOTOR_DATA)
 *     .setMotorData(100, -100, 0, 0, 0, 0, false, false, false, false, false, false)
 *     .setLed(0, 0xFF, 0x00, 0x00)
 *     .done();
 * }</pre>
 *
 * <p>Thread safety: not thread-safe; do not share across threads.
 *
 * <p><b>DSL maintenance rule:</b> any change to
 * {@link BuWizz3ProtocolConstants} must be reflected here.
 * See {@code AGENTS.md §16}.
 */
public final class BuWizz3Dsl {

    /** Brake-flag bit for channel 3. */
    private static final int BRAKE_BIT_CH3 = 0x04;

    /** Brake-flag bit for channel 4. */
    private static final int BRAKE_BIT_CH4 = 0x08;

    /** Brake-flag bit for channel 5. */
    private static final int BRAKE_BIT_CH5 = 0x10;

    /** Brake-flag bit for channel 6. */
    private static final int BRAKE_BIT_CH6 = 0x20;

    private final BleConnection connection;

    /**
     * Creates a {@code BuWizz3Dsl} wrapping the given connection.
     *
     * @param connection the active BLE connection; must not be {@code null}
     */
    BuWizz3Dsl(@NonNull BleConnection connection) {
        this.connection = connection;
    }

    // =========================================================================
    // Notifications (device status ~20 Hz)
    // =========================================================================

    /**
     * Returns a publisher that emits BuWizz 3.0 status report packets at
     * approximately 20 Hz (command {@code 0x01}).
     *
     * @return a publisher of status report payloads; never {@code null}
     */
    public @NonNull Publisher<byte[]> notifications() {
        return connection.notifications(
                BuWizz3ProtocolConstants.APPLICATION_SERVICE_UUID,
                BuWizz3ProtocolConstants.APPLICATION_CHARACTERISTIC_UUID);
    }

    // =========================================================================
    // Motor data (command 0x30)
    // =========================================================================

    /**
     * Sends a Set Motor Data command ({@code 0x30}) for all 6 channels.
     *
     * <p>Speed values are signed bytes where:
     * {@link BuWizz3ProtocolConstants#MOTOR_SPEED_FULL_REVERSE} = full reverse,
     * {@link BuWizz3ProtocolConstants#MOTOR_SPEED_STOP} = stop,
     * {@link BuWizz3ProtocolConstants#MOTOR_SPEED_FULL_FORWARD} = full forward.
     *
     * @param s1       signed speed for channel 1 (−127 to 127)
     * @param s2       signed speed for channel 2
     * @param s3       signed speed for channel 3
     * @param s4       signed speed for channel 4
     * @param s5       signed speed for channel 5
     * @param s6       signed speed for channel 6
     * @param brake1   {@code true} = slow-decay on channel 1
     * @param brake2   {@code true} = slow-decay on channel 2
     * @param brake3   {@code true} = slow-decay on channel 3
     * @param brake4   {@code true} = slow-decay on channel 4
     * @param brake5   {@code true} = slow-decay on channel 5
     * @param brake6   {@code true} = slow-decay on channel 6
     * @return a future that completes when the write is submitted; never {@code null}
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public @NonNull CompletableFuture<Void> setMotorData(
            int s1, int s2, int s3, int s4, int s5, int s6,
            boolean brake1, boolean brake2, boolean brake3,
            boolean brake4, boolean brake5, boolean brake6) {
        final int brakeFlags = (brake1 ? 0x01 : 0)
                | (brake2 ? 0x02 : 0)
                | (brake3 ? BRAKE_BIT_CH3 : 0)
                | (brake4 ? BRAKE_BIT_CH4 : 0)
                | (brake5 ? BRAKE_BIT_CH5 : 0)
                | (brake6 ? BRAKE_BIT_CH6 : 0);
        final byte[] msg = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_MOTOR_DATA,
            (byte) s1, (byte) s2, (byte) s3,
            (byte) s4, (byte) s5, (byte) s6,
            (byte) brakeFlags,
            (byte) 0x00   // LUT disable flags: all enabled
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
                BuWizz3ProtocolConstants.MOTOR_SPEED_STOP,
                BuWizz3ProtocolConstants.MOTOR_SPEED_STOP,
                BuWizz3ProtocolConstants.MOTOR_SPEED_STOP,
                BuWizz3ProtocolConstants.MOTOR_SPEED_STOP,
                BuWizz3ProtocolConstants.MOTOR_SPEED_STOP,
                BuWizz3ProtocolConstants.MOTOR_SPEED_STOP,
                false, false, false, false, false, false);
    }

    // =========================================================================
    // Data transfer period (command 0x32)
    // =========================================================================

    /**
     * Sends a Set Data Transfer Period command ({@code 0x32}).
     *
     * <p>Valid range: 20–255 ms in steps of 5 ms.
     *
     * @param periodMs the desired notification period in milliseconds
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> setDataTransferPeriod(int periodMs) {
        final byte[] msg = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_DATA_PERIOD,
            (byte) periodMs
        };
        return write(msg);
    }

    // =========================================================================
    // Motor timeout (command 0x34)
    // =========================================================================

    /**
     * Sends a Set Motor Timeout command ({@code 0x34}).
     *
     * <p>Configuration values:
     * <ul>
     *   <li>{@code 0} — stop immediately and brake</li>
     *   <li>{@code 1} — stop immediately and coast</li>
     *   <li>{@code 2}–{@code 254} — coast to stop in (N-1) seconds</li>
     * </ul>
     *
     * @param configuration the timeout configuration byte
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> setMotorTimeout(int configuration) {
        final byte[] msg = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_MOTOR_TIMEOUT,
            (byte) configuration
        };
        return write(msg);
    }

    // =========================================================================
    // Connection watchdog (command 0x35)
    // =========================================================================

    /**
     * Activates or deactivates the connection watchdog ({@code 0x35}).
     *
     * <p>A value of {@code 0} disables the watchdog. On expiry, the device
     * drops the connection and applies the motor-timeout stop configuration.
     *
     * @param timeoutSeconds watchdog timeout in seconds; 0 = disabled
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> setWatchdog(int timeoutSeconds) {
        final byte[] msg = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_WATCHDOG,
            (byte) timeoutSeconds
        };
        return write(msg);
    }

    // =========================================================================
    // LED control (command 0x36)
    // =========================================================================

    /**
     * Sends a Set LED Status command ({@code 0x36}) for all four motor LEDs.
     *
     * @param r1 red channel of LED 1 (0–255)
     * @param g1 green channel of LED 1 (0–255)
     * @param b1 blue channel of LED 1 (0–255)
     * @param r2 red channel of LED 2
     * @param g2 green channel of LED 2
     * @param b2 blue channel of LED 2
     * @param r3 red channel of LED 3
     * @param g3 green channel of LED 3
     * @param b3 blue channel of LED 3
     * @param r4 red channel of LED 4
     * @param g4 green channel of LED 4
     * @param b4 blue channel of LED 4
     * @return a future that completes when the write is submitted; never {@code null}
     */
    @SuppressWarnings("checkstyle:ParameterNumber")
    public @NonNull CompletableFuture<Void> setAllLeds(
            int r1, int g1, int b1,
            int r2, int g2, int b2,
            int r3, int g3, int b3,
            int r4, int g4, int b4) {
        final byte[] msg = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_LED_STATUS,
            (byte) r1, (byte) g1, (byte) b1,
            (byte) r2, (byte) g2, (byte) b2,
            (byte) r3, (byte) g3, (byte) b3,
            (byte) r4, (byte) g4, (byte) b4
        };
        return write(msg);
    }

    /**
     * Sets all four LEDs to the same colour.
     *
     * @param red   red component (0–255)
     * @param green green component (0–255)
     * @param blue  blue component (0–255)
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> setLedsUniform(int red, int green, int blue) {
        return setAllLeds(
                red, green, blue,
                red, green, blue,
                red, green, blue,
                red, green, blue);
    }

    /**
     * Reverts all LEDs to default behaviour by sending the command byte alone.
     *
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> resetLeds() {
        final byte[] msg = {(byte) BuWizz3ProtocolConstants.CMD_SET_LED_STATUS};
        return write(msg);
    }

    // =========================================================================
    // Current limits (command 0x38)
    // =========================================================================

    /**
     * Sends a Set Current Limits command ({@code 0x38}) for all 6 channels.
     *
     * <p>Each limit is expressed in steps of 30 mA.
     * Default on connect: 1.5 A for ports 1–4 (value 50), 3.0 A for ports 5–6 (value 100).
     *
     * @param limit1 current limit for channel 1
     * @param limit2 current limit for channel 2
     * @param limit3 current limit for channel 3
     * @param limit4 current limit for channel 4
     * @param limit5 current limit for channel 5
     * @param limit6 current limit for channel 6
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> setCurrentLimits(
            int limit1, int limit2, int limit3,
            int limit4, int limit5, int limit6) {
        final byte[] msg = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_CURRENT_LIMITS,
            (byte) limit1, (byte) limit2, (byte) limit3,
            (byte) limit4, (byte) limit5, (byte) limit6
        };
        return write(msg);
    }

    // =========================================================================
    // Device name (command 0x20)
    // =========================================================================

    /**
     * Sets the BuWizz 3.0 device name ({@code 0x20}).
     *
     * <p>The name is NUL-terminated and truncated to 12 ASCII characters.
     *
     * @param name the desired device name (max 12 ASCII characters)
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> setDeviceName(@NonNull String name) {
        final byte[] nameBytes = name.getBytes(java.nio.charset.StandardCharsets.US_ASCII);
        final int maxLen = 12;
        final byte[] msg = new byte[1 + maxLen];
        msg[0] = (byte) BuWizz3ProtocolConstants.CMD_SET_DEVICE_NAME;
        final int copyLen = Math.min(nameBytes.length, maxLen);
        System.arraycopy(nameBytes, 0, msg, 1, copyLen);
        return write(msg);
    }

    // =========================================================================
    // PU port function (command 0x50)
    // =========================================================================

    /**
     * Sends a Set PU Port Function command ({@code 0x50}) for all 4 PU ports.
     *
     * @param func1 port function for port 1 (see {@code PU_PORT_FUNCTION_*} constants)
     * @param func2 port function for port 2
     * @param func3 port function for port 3
     * @param func4 port function for port 4
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> setPuPortFunctions(
            int func1, int func2, int func3, int func4) {
        final byte[] msg = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_PU_PORT_FUNCTION,
            (byte) func1, (byte) func2, (byte) func3, (byte) func4
        };
        return write(msg);
    }

    // =========================================================================
    // Shelf mode (command 0xA1)
    // =========================================================================

    /**
     * Activates shelf mode ({@code 0xA1}).
     *
     * <p>The device disconnects the battery immediately. It wakes only when a
     * charger is connected.
     *
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> activateShelfMode() {
        final byte[] msg = {(byte) BuWizz3ProtocolConstants.CMD_ACTIVATE_SHELF_MODE};
        return write(msg);
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Disconnects the BuWizz 3.0 brick and releases all resources.
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
     * Writes {@code payload} to the BuWizz 3.0 Application characteristic.
     *
     * @param payload the raw bytes to write
     * @return a future that completes when the write is submitted
     */
    private @NonNull CompletableFuture<Void> write(byte[] payload) {
        return connection.writeWithoutResponse(
                BuWizz3ProtocolConstants.APPLICATION_SERVICE_UUID,
                BuWizz3ProtocolConstants.APPLICATION_CHARACTERISTIC_UUID,
                payload);
    }
}
