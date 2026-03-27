package ch.varani.bricks.ble.api.dsl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;

import org.jspecify.annotations.NonNull;

import ch.varani.bricks.ble.api.BleConnection;
import ch.varani.bricks.ble.api.BleException;
import ch.varani.bricks.ble.device.lego.LegoProtocolConstants;

/**
 * Fluent DSL sub-builder for WeDo 2.0 hub BLE protocol operations.
 *
 * <p>Obtained via {@link ConnectionDsl#asWeDo2()}. The WeDo 2.0 hub uses its
 * own proprietary GATT layout (LWP2, not LWP3): it exposes two services and
 * separate write characteristics — one for motor commands
 * ({@link LegoProtocolConstants#WEDO2_MOTOR_VALUE_WRITE_UUID}) and one for
 * port-type / sensor-subscription setup
 * ({@link LegoProtocolConstants#WEDO2_PORT_TYPE_WRITE_UUID}) — and a dedicated
 * notify characteristic ({@link LegoProtocolConstants#WEDO2_BUTTON_UUID}) for
 * button and general hub events.
 *
 * <p>Usage example:
 * <pre>{@code
 * connectionDsl.asWeDo2()
 *     .motorPower(LegoProtocolConstants.WEDO2_PORT_A, 75)
 *     .setLedColor(LegoProtocolConstants.WEDO2_LED_COLOR_GREEN)
 *     .done();
 * }</pre>
 *
 * <p>Thread safety: not thread-safe; do not share across threads.
 *
 * <p><b>DSL maintenance rule:</b> any change to
 * {@link LegoProtocolConstants} WeDo 2.0 constants (prefix {@code WEDO2_})
 * must be reflected here. See {@code AGENTS.md §16}.
 */
public final class WeDo2Dsl {

    /** Byte mask for clamping an integer to the signed-byte range used by the motor command. */
    private static final int BYTE_MASK = 0xFF;

    private final BleConnection connection;

    /**
     * Creates a {@code WeDo2Dsl} wrapping the given connection.
     *
     * @param connection the active BLE connection; must not be {@code null}
     */
    WeDo2Dsl(@NonNull BleConnection connection) {
        this.connection = connection;
    }

    // =========================================================================
    // Notifications
    // =========================================================================

    /**
     * Returns a publisher that emits raw notification bytes from the WeDo 2.0
     * button / general-event characteristic
     * ({@link LegoProtocolConstants#WEDO2_BUTTON_UUID}).
     *
     * <p>Subscribe here to receive button-state change events from the hub.
     *
     * @return a publisher of notification payloads; never {@code null}
     */
    public @NonNull Publisher<byte[]> notifications() {
        return connection.notifications(
                LegoProtocolConstants.WEDO2_SERVICE_UUID,
                LegoProtocolConstants.WEDO2_BUTTON_UUID);
    }

    /**
     * Returns a publisher that emits raw sensor-value notification bytes from
     * the WeDo 2.0 sensor-value characteristic
     * ({@link LegoProtocolConstants#WEDO2_SENSOR_VALUE_UUID}).
     *
     * <p>Each notification payload has the following layout (confirmed against
     * nathankellenicki/node-poweredup
     * {@code src/hubs/wedo2smarthub.ts _parseSensorMessage()}):
     * <pre>
     * [indicator, portId, value, overflow]
     * </pre>
     * {@code msg[1]} is the port ID, {@code msg[2]} is the raw sensor value
     * (distance in cm for the motion sensor in mode 0), and {@code msg[3]} is
     * an overflow flag ({@code 0x01} means add 255 to {@code msg[2]}).
     * Call {@link #subscribeSensor(int, int, int)} before subscribing here.
     *
     * @return a publisher of sensor-value notification payloads; never {@code null}
     */
    public @NonNull Publisher<byte[]> sensorNotifications() {
        return connection.notifications(
                LegoProtocolConstants.WEDO2_SERVICE_UUID,
                LegoProtocolConstants.WEDO2_SENSOR_VALUE_UUID);
    }

    // =========================================================================
    // Motor commands
    // =========================================================================

    /**
     * Sends a motor-power command for the given port.
     *
     * <p>Writes a 4-byte command to
     * {@link LegoProtocolConstants#WEDO2_MOTOR_VALUE_WRITE_UUID}:
     * <pre>
     * [portId, 0x01, 0x02, power]
     * </pre>
     * where {@code power} is a signed byte in the range −100 to +100.
     * Byte[1] is the fixed WeDo 2.0 {@code typeId} ({@code 0x01}) and
     * byte[2] is the fixed {@code writeDirect} sub-command ({@code 0x02}).
     *
     * <p>Reference: nathankellenicki/node-poweredup (MIT) —
     * https://github.com/nathankellenicki/node-poweredup —
     * {@code src/devices/device.ts writeDirect()}.
     *
     * @param portId port identifier; use
     *               {@link LegoProtocolConstants#WEDO2_PORT_A} or
     *               {@link LegoProtocolConstants#WEDO2_PORT_B}
     * @param power  motor power in the range −100 (full reverse) to +100
     *               (full forward); 0 = stop
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> motorPower(int portId, int power) {
        final byte[] msg = {
            (byte) portId,
            (byte) LegoProtocolConstants.WEDO2_MOTOR_TYPE_ID,
            (byte) 0x02,                // fixed writeDirect sub-command byte
            (byte) (power & BYTE_MASK)
        };
        return writeMotor(msg);
    }

    /**
     * Stops the motor on the given port by sending a power value of zero.
     *
     * @param portId port identifier; use
     *               {@link LegoProtocolConstants#WEDO2_PORT_A} or
     *               {@link LegoProtocolConstants#WEDO2_PORT_B}
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> stopMotor(int portId) {
        return motorPower(portId, 0);
    }

    // =========================================================================
    // LED control
    // =========================================================================

    /**
     * Sets the WeDo 2.0 hub LED to a named colour using the indexed colour mode.
     *
     * <p>This is the <em>recommended</em> way to change the LED colour on the
     * WeDo 2.0 hub.  The hub firmware reliably responds to indexed colour mode;
     * arbitrary RGB mode ({@link #setLedRgb}) may be silently ignored by some
     * firmware versions.
     *
     * <p>Performs two successive writes:
     * <ol>
     *   <li>A 4-byte mode-setup packet to
     *       {@link LegoProtocolConstants#WEDO2_PORT_TYPE_WRITE_UUID}:
     *       {@code [WEDO2_PORT_LED, 0x17, 0x01, 0x01]}</li>
     *   <li>A 4-byte indexed-colour packet to
     *       {@link LegoProtocolConstants#WEDO2_MOTOR_VALUE_WRITE_UUID}:
     *       {@code [WEDO2_PORT_LED, 0x04, 0x01, colorIndex]}</li>
     * </ol>
     *
     * <p>Use the {@code WEDO2_LED_COLOR_*} constants in
     * {@link LegoProtocolConstants} to obtain valid colour index values
     * (e.g. {@link LegoProtocolConstants#WEDO2_LED_COLOR_RED},
     * {@link LegoProtocolConstants#WEDO2_LED_COLOR_GREEN}).
     *
     * <p>Reference: nathankellenicki/node-poweredup (MIT) —
     * https://github.com/nathankellenicki/node-poweredup — {@code src/devices/hubled.ts
     * setColor()}.
     *
     * @param colorIndex colour index 0–10; use {@code WEDO2_LED_COLOR_*} constants
     * @return a future that completes when both writes have been submitted;
     *         never {@code null}
     */
    public @NonNull CompletableFuture<Void> setLedColor(int colorIndex) {
        final byte[] modeCmd = {
            (byte) LegoProtocolConstants.WEDO2_PORT_LED,
            (byte) LegoProtocolConstants.WEDO2_LED_MODE_SETUP_B1,
            (byte) LegoProtocolConstants.WEDO2_LED_MODE_SETUP_B2,
            (byte) LegoProtocolConstants.WEDO2_LED_IDX_MODE_SETUP_B3
        };
        final byte[] idxCmd = {
            (byte) LegoProtocolConstants.WEDO2_PORT_LED,
            (byte) LegoProtocolConstants.WEDO2_LED_IDX_CMD_B1,
            (byte) LegoProtocolConstants.WEDO2_LED_IDX_CMD_B2,
            (byte) colorIndex
        };
        return connection.writeWithoutResponse(
                        LegoProtocolConstants.WEDO2_SERVICE_UUID,
                        LegoProtocolConstants.WEDO2_PORT_TYPE_WRITE_UUID,
                        modeCmd)
                .thenCompose(ignored -> connection.writeWithoutResponse(
                        LegoProtocolConstants.WEDO2_SERVICE_UUID,
                        LegoProtocolConstants.WEDO2_MOTOR_VALUE_WRITE_UUID,
                        idxCmd));
    }

    // =========================================================================
    // Sensor subscription
    // =========================================================================

    /**
     * Subscribes to sensor readings from the given port.
     *
     * <p>Writes an 11-byte subscribe command to
     * {@link LegoProtocolConstants#WEDO2_PORT_TYPE_WRITE_UUID}:
     * <pre>
     * [0x01, 0x02, portId, deviceType, mode, 0x01, 0x00, 0x00, 0x00, 0x00, 0x01]
     * </pre>
     * After this call the hub will emit sensor readings via
     * {@link #sensorNotifications()}.
     *
     * @param portId     port identifier; use
     *                   {@link LegoProtocolConstants#WEDO2_PORT_A} or
     *                   {@link LegoProtocolConstants#WEDO2_PORT_B}
     * @param deviceType the device type ID (e.g.
     *                   {@link LegoProtocolConstants#WEDO2_MOTION_SENSOR_TYPE_ID})
     * @param mode       the sensor mode index (0-based)
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> subscribeSensor(int portId, int deviceType, int mode) {
        final byte[] msg = {
            0x01, 0x02,
            (byte) portId,
            (byte) deviceType,
            (byte) mode,
            0x01, 0x00, 0x00, 0x00, 0x00,
            0x01
        };
        return writePortType(msg);
    }

    /**
     * Unsubscribes from sensor readings on the given port.
     *
     * <p>Writes an 11-byte unsubscribe command to
     * {@link LegoProtocolConstants#WEDO2_PORT_TYPE_WRITE_UUID} — identical to
     * the subscribe command but with the last byte set to {@code 0x00}.
     *
     * @param portId     port identifier; use
     *                   {@link LegoProtocolConstants#WEDO2_PORT_A} or
     *                   {@link LegoProtocolConstants#WEDO2_PORT_B}
     * @param deviceType the device type ID (e.g.
     *                   {@link LegoProtocolConstants#WEDO2_MOTION_SENSOR_TYPE_ID})
     * @param mode       the sensor mode index (0-based)
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> unsubscribeSensor(
            int portId, int deviceType, int mode) {
        final byte[] msg = {
            0x01, 0x02,
            (byte) portId,
            (byte) deviceType,
            (byte) mode,
            0x01, 0x00, 0x00, 0x00, 0x00,
            0x00
        };
        return writePortType(msg);
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Disconnects the WeDo 2.0 hub and releases all resources.
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
     * Writes {@code payload} to the WeDo 2.0 motor-value characteristic
     * ({@link LegoProtocolConstants#WEDO2_MOTOR_VALUE_WRITE_UUID}).
     *
     * @param payload the raw bytes to write
     * @return a future that completes when the write is submitted
     */
    private @NonNull CompletableFuture<Void> writeMotor(byte[] payload) {
        return connection.writeWithoutResponse(
                LegoProtocolConstants.WEDO2_SERVICE_UUID,
                LegoProtocolConstants.WEDO2_MOTOR_VALUE_WRITE_UUID,
                payload);
    }

    /**
     * Writes {@code payload} to the WeDo 2.0 port-type write characteristic
     * ({@link LegoProtocolConstants#WEDO2_PORT_TYPE_WRITE_UUID}).
     *
     * @param payload the raw bytes to write
     * @return a future that completes when the write is submitted
     */
    private @NonNull CompletableFuture<Void> writePortType(byte[] payload) {
        return connection.writeWithoutResponse(
                LegoProtocolConstants.WEDO2_SERVICE_UUID,
                LegoProtocolConstants.WEDO2_PORT_TYPE_WRITE_UUID,
                payload);
    }
}
