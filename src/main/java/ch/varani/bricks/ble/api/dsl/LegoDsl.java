package ch.varani.bricks.ble.api.dsl;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;

import org.jspecify.annotations.NonNull;

import ch.varani.bricks.ble.api.BleConnection;
import ch.varani.bricks.ble.api.BleException;
import ch.varani.bricks.ble.device.lego.LegoColor;
import ch.varani.bricks.ble.device.lego.LegoProtocolConstants;

/**
 * Fluent DSL sub-builder for LEGO Wireless Protocol 3.0 (LWP3) operations.
 *
 * <p>Obtained via {@link ConnectionDsl#asLego()}. Provides named methods for
 * every downstream command and upstream notification defined in LWP3, mapping
 * to writes/reads on the single {@link LegoProtocolConstants#HUB_CHARACTERISTIC_UUID}
 * characteristic.
 *
 * <p>Usage example:
 * <pre>{@code
 * connectionDsl.asLego()
 *     .requestBatteryVoltage()
 *     .motor(0x00).startSpeed(80)
 *     .hubAction().switchOff()
 *     .done();
 * }</pre>
 *
 * <p>Thread safety: not thread-safe; do not share across threads.
 *
 * <p><b>DSL maintenance rule:</b> any change to
 * {@link LegoProtocolConstants} must be reflected here. See {@code AGENTS.md §16}.
 */
public final class LegoDsl {

    private final BleConnection connection;

    /** GATT service UUID used for all reads, writes and notifications. */
    private final String serviceUuid;

    /** GATT characteristic UUID used for write (downstream) operations. */
    private final String writeCharacteristicUuid;

    /** GATT characteristic UUID used for notify (upstream) operations. */
    private final String notifyCharacteristicUuid;

    /**
     * Creates a {@code LegoDsl} wrapping the given connection using the
     * default LWP3 GATT service and characteristic UUIDs.
     *
     * @param connection the active BLE connection; must not be {@code null}
     */
    LegoDsl(@NonNull BleConnection connection) {
        this(connection,
             LegoProtocolConstants.HUB_SERVICE_UUID,
             LegoProtocolConstants.HUB_CHARACTERISTIC_UUID,
             LegoProtocolConstants.HUB_CHARACTERISTIC_UUID);
    }

    /**
     * Creates a {@code LegoDsl} wrapping the given connection using custom
     * GATT service and characteristic UUIDs, with separate write and notify
     * characteristics.
     *
     * <p>Use this constructor for hubs that implement the LEGO Wireless
     * Protocol over non-standard GATT UUIDs — e.g. the WeDo 2.0 hub which
     * uses LWP2 UUIDs and exposes separate write ({@code 00001524-...}) and
     * notify ({@code 00001526-...}) characteristics.
     *
     * @param connection              the active BLE connection; must not be {@code null}
     * @param serviceUuid             GATT service UUID; must not be {@code null}
     * @param writeCharacteristicUuid GATT characteristic UUID used for write (downstream);
     *                                must not be {@code null}
     * @param notifyCharacteristicUuid GATT characteristic UUID used for notify (upstream);
     *                                must not be {@code null}
     */
    public LegoDsl(
            @NonNull BleConnection connection,
            @NonNull String serviceUuid,
            @NonNull String writeCharacteristicUuid,
            @NonNull String notifyCharacteristicUuid) {
        this.connection              = connection;
        this.serviceUuid             = serviceUuid;
        this.writeCharacteristicUuid = writeCharacteristicUuid;
        this.notifyCharacteristicUuid = notifyCharacteristicUuid;
    }

    // =========================================================================
    // Notifications (upstream)
    // =========================================================================

    /**
     * Returns a publisher that emits raw LWP3 messages received from the hub.
     *
     * <p>Enables BLE notifications on the LEGO Hub characteristic.
     *
     * @return a publisher of raw upstream message bytes; never {@code null}
     */
    public @NonNull Publisher<byte[]> notifications() {
        return connection.notifications(serviceUuid, notifyCharacteristicUuid);
    }

    // =========================================================================
    // Hub Property requests (message type 0x01)
    // =========================================================================

    /**
     * Sends a Hub Properties request-update message for the given property.
     *
     * <p>The hub will respond with a Properties Update notification.
     *
     * @param propertyRef the hub property reference byte (e.g.
     *                    {@link LegoProtocolConstants#HUB_PROP_BATTERY_VOLTAGE})
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> requestHubProperty(int propertyRef) {
        final byte[] msg = {
            0x05,
            (byte) LegoProtocolConstants.HUB_ID,
            (byte) LegoProtocolConstants.MSG_HUB_PROPERTIES,
            (byte) propertyRef,
            (byte) LegoProtocolConstants.HUB_PROP_OP_REQUEST_UPDATE
        };
        return write(msg);
    }

    /**
     * Requests the current battery voltage percentage from the hub.
     *
     * <p>Convenience shortcut for
     * {@code requestHubProperty(HUB_PROP_BATTERY_VOLTAGE)}.
     *
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> requestBatteryVoltage() {
        return requestHubProperty(LegoProtocolConstants.HUB_PROP_BATTERY_VOLTAGE);
    }

    /**
     * Requests the firmware version string from the hub.
     *
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> requestFirmwareVersion() {
        return requestHubProperty(LegoProtocolConstants.HUB_PROP_FW_VERSION);
    }

    /**
     * Requests the hardware version string from the hub.
     *
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> requestHardwareVersion() {
        return requestHubProperty(LegoProtocolConstants.HUB_PROP_HW_VERSION);
    }

    /**
     * Requests the RSSI value from the hub.
     *
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> requestRssi() {
        return requestHubProperty(LegoProtocolConstants.HUB_PROP_RSSI);
    }

    /**
     * Requests the manufacturer name string from the hub.
     *
     * <p>Convenience shortcut for
     * {@code requestHubProperty(HUB_PROP_MANUFACTURER_NAME)}.
     *
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> requestManufacturerName() {
        return requestHubProperty(LegoProtocolConstants.HUB_PROP_MANUFACTURER_NAME);
    }

    /**
     * Requests the radio firmware version string from the hub.
     *
     * <p>Convenience shortcut for
     * {@code requestHubProperty(HUB_PROP_RADIO_FIRMWARE_VERSION)}.
     *
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> requestRadioFirmwareVersion() {
        return requestHubProperty(LegoProtocolConstants.HUB_PROP_RADIO_FIRMWARE_VERSION);
    }

    /**
     * Requests the LEGO Wireless Protocol version from the hub.
     *
     * <p>Convenience shortcut for
     * {@code requestHubProperty(HUB_PROP_LWP_VERSION)}.
     * The response payload is a 2-byte BCD-encoded version number.
     *
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> requestLwpVersion() {
        return requestHubProperty(LegoProtocolConstants.HUB_PROP_LWP_VERSION);
    }

    /**
     * Requests the System Type ID from the hub.
     *
     * <p>Convenience shortcut for
     * {@code requestHubProperty(HUB_PROP_SYSTEM_TYPE_ID)}.
     *
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> requestSystemTypeId() {
        return requestHubProperty(LegoProtocolConstants.HUB_PROP_SYSTEM_TYPE_ID);
    }

    /**
     * Requests the primary MAC address from the hub.
     *
     * <p>Convenience shortcut for
     * {@code requestHubProperty(HUB_PROP_PRIMARY_MAC)}.
     *
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> requestPrimaryMac() {
        return requestHubProperty(LegoProtocolConstants.HUB_PROP_PRIMARY_MAC);
    }

    /**
     * Requests the secondary MAC address from the hub.
     *
     * <p>Convenience shortcut for
     * {@code requestHubProperty(HUB_PROP_SECONDARY_MAC)}.
     *
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> requestSecondaryMac() {
        return requestHubProperty(LegoProtocolConstants.HUB_PROP_SECONDARY_MAC);
    }

    /**
     * Enables periodic battery voltage updates from the hub.
     *
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> enableBatteryVoltageUpdates() {
        return hubPropertyOperation(
                LegoProtocolConstants.HUB_PROP_BATTERY_VOLTAGE,
                LegoProtocolConstants.HUB_PROP_OP_ENABLE_UPDATES);
    }

    /**
     * Disables periodic battery voltage updates from the hub.
     *
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> disableBatteryVoltageUpdates() {
        return hubPropertyOperation(
                LegoProtocolConstants.HUB_PROP_BATTERY_VOLTAGE,
                LegoProtocolConstants.HUB_PROP_OP_DISABLE_UPDATES);
    }

    /**
     * Sends a Hub Properties message with the given property reference and
     * operation code.
     *
     * @param propertyRef the hub property reference byte
     * @param operation   the property operation byte
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> hubPropertyOperation(
            int propertyRef,
            int operation) {
        final byte[] msg = {
            0x05,
            (byte) LegoProtocolConstants.HUB_ID,
            (byte) LegoProtocolConstants.MSG_HUB_PROPERTIES,
            (byte) propertyRef,
            (byte) operation
        };
        return write(msg);
    }

    // =========================================================================
    // Hub Action commands (message type 0x02)
    // =========================================================================

    /**
     * Returns a fluent sub-builder for Hub Action commands.
     *
     * @return the hub-action builder; never {@code null}
     */
    public @NonNull HubActionBuilder hubAction() {
        return new HubActionBuilder(this);
    }

    /**
     * Sends a raw Hub Action command.
     *
     * @param actionType the action type byte (e.g.
     *                   {@link LegoProtocolConstants#HUB_ACTION_SWITCH_OFF})
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> sendHubAction(int actionType) {
        final byte[] msg = {
            0x04,
            (byte) LegoProtocolConstants.HUB_ID,
            (byte) LegoProtocolConstants.MSG_HUB_ACTIONS,
            (byte) actionType
        };
        return write(msg);
    }

    // =========================================================================
    // Hub Alert operations (message type 0x03)
    // =========================================================================

    /**
     * Enables upstream alert updates for the given alert type.
     *
     * @param alertType the alert type byte (e.g.
     *                  {@link LegoProtocolConstants#HUB_ALERT_LOW_VOLTAGE})
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> enableAlert(int alertType) {
        return sendAlertOperation(alertType, LegoProtocolConstants.HUB_ALERT_OP_ENABLE_UPDATES);
    }

    /**
     * Disables upstream alert updates for the given alert type.
     *
     * @param alertType the alert type byte
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> disableAlert(int alertType) {
        return sendAlertOperation(alertType, LegoProtocolConstants.HUB_ALERT_OP_DISABLE_UPDATES);
    }

    /**
     * Requests the current status of the given alert type.
     *
     * @param alertType the alert type byte
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> requestAlert(int alertType) {
        return sendAlertOperation(alertType, LegoProtocolConstants.HUB_ALERT_OP_REQUEST_UPDATE);
    }

    /**
     * Sends a Hub Alerts message with the given type and operation.
     *
     * @param alertType  the alert type byte
     * @param operation  the alert operation byte
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> sendAlertOperation(int alertType, int operation) {
        final byte[] msg = {
            0x05,
            (byte) LegoProtocolConstants.HUB_ID,
            (byte) LegoProtocolConstants.MSG_HUB_ALERTS,
            (byte) alertType,
            (byte) operation
        };
        return write(msg);
    }

    // =========================================================================
    // Port Output commands for motors (message type 0x81)
    // =========================================================================

    /**
     * Returns a fluent motor sub-builder for the specified port.
     *
     * @param portId the port identifier byte (e.g. {@code 0x00} for port A)
     * @return the motor builder; never {@code null}
     */
    public @NonNull MotorBuilder motor(int portId) {
        return new MotorBuilder(this, portId);
    }

    /**
     * Sends a raw Port Output Command message.
     *
     * @param portId            the port identifier byte
     * @param startupCompletion the startup/completion info nibbles byte
     * @param subCommand        the motor sub-command byte
     * @param parameters        additional sub-command parameters; may be empty
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> portOutputCommand(
            int portId,
            int startupCompletion,
            int subCommand,
            byte... parameters) {
        // Layout: [length, hubId, msgType, portId, startupCompletion, subCmd, params...]
        final byte[] msg = new byte[PORT_OUTPUT_HEADER_SIZE + parameters.length];
        msg[IDX_MSG_LENGTH] = (byte) msg.length;
        msg[IDX_HUB_ID] = (byte) LegoProtocolConstants.HUB_ID;
        msg[IDX_MSG_TYPE] = (byte) LegoProtocolConstants.MSG_PORT_OUTPUT_COMMAND;
        msg[IDX_PORT_ID] = (byte) portId;
        msg[IDX_STARTUP_COMPLETION] = (byte) startupCompletion;
        msg[IDX_SUB_COMMAND] = (byte) subCommand;
        System.arraycopy(parameters, 0, msg, PORT_OUTPUT_HEADER_SIZE, parameters.length);
        return write(msg);
    }

    // =========================================================================
    // Port Input Format Setup (message type 0x41)
    // =========================================================================

    /**
     * Sends a Port Input Format Setup (Single) message ({@code 0x41}) to configure
     * a sensor port to report in a specific mode.
     *
     * <p>After calling this method the hub will begin sending Port Value (Single)
     * notifications ({@code 0x45}) whenever the value changes by at least
     * {@code deltaInterval} in the given {@code mode}.
     *
     * @param portId          the port identifier byte
     * @param mode            the sensor mode index (0-based)
     * @param deltaInterval   minimum value change that triggers a notification
     * @param notifyOnChange  {@code true} to enable automatic notifications;
     *                        {@code false} to disable them (polling only)
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> setupPortInputFormatSingle(
            int portId,
            int mode,
            int deltaInterval,
            boolean notifyOnChange) {
        final int headerSize = SETUP_SINGLE_MSG_SIZE;
        final byte[] msg = new byte[headerSize];
        msg[0] = (byte) headerSize;
        msg[1] = (byte) LegoProtocolConstants.HUB_ID;
        msg[2] = (byte) LegoProtocolConstants.MSG_PORT_INPUT_FORMAT_SETUP_SINGLE;
        msg[IDX_SETUP_PORT_ID] = (byte) portId;
        msg[IDX_SETUP_MODE] = (byte) mode;
        msg[IDX_SETUP_DELTA_0] = (byte) (deltaInterval & SETUP_BYTE_MASK);
        msg[IDX_SETUP_DELTA_1] = (byte) ((deltaInterval >> SETUP_SHIFT_8) & SETUP_BYTE_MASK);
        msg[IDX_SETUP_DELTA_2] = (byte) ((deltaInterval >> SETUP_SHIFT_16) & SETUP_BYTE_MASK);
        msg[IDX_SETUP_DELTA_3] = (byte) ((deltaInterval >> SETUP_SHIFT_24) & SETUP_BYTE_MASK);
        msg[IDX_SETUP_NOTIFY] = notifyOnChange ? (byte) 1 : (byte) 0;
        return write(msg);
    }

    // =========================================================================
    // Hub LED colour control
    // =========================================================================

    /**
     * Sets the Hub LED to the given colour by sending a
     * {@code WriteDirectModeData} ({@code 0x51}) Port Output Command to the
     * LED port.
     *
     * @param ledPortId the virtual port ID of the Hub LED (e.g.
     *                  {@link LegoProtocolConstants#MOVE_HUB_PORT_LED},
     *                  {@link LegoProtocolConstants#CITY_HUB_PORT_LED},
     *                  {@link LegoProtocolConstants#TECHNIC_HUB_PORT_LED})
     * @param color     the desired LED colour; use {@link LegoColor#NONE} to
     *                  switch the LED off; must not be {@code null}
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> setHubLedColor(int ledPortId,
            @NonNull LegoColor color) {
        return portOutputCommand(
                ledPortId,
                DEFAULT_STARTUP_COMPLETION_LED,
                LegoProtocolConstants.MOTOR_CMD_WRITE_DIRECT_MODE_DATA,
                (byte) 0x00,          // mode 0 = colour index
                (byte) color.code());
    }

    // =========================================================================
    // Duplo Train sound control
    // =========================================================================

    /**
     * Plays a sound on the Duplo Train Base by sending a
     * {@code WriteDirectModeData} ({@code 0x51}) Port Output Command to the
     * Duplo Train Base Speaker port.
     *
     * <p>Use the {@code DUPLO_SOUND_*} constants from
     * {@link LegoProtocolConstants} (e.g.
     * {@link LegoProtocolConstants#DUPLO_SOUND_HORN}).
     *
     * @param speakerPortId the port ID of the Duplo Train Base Speaker device
     * @param soundId       the sound identifier byte; use one of the
     *                      {@code DUPLO_SOUND_*} constants
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> playDuploSound(int speakerPortId, int soundId) {
        return portOutputCommand(
                speakerPortId,
                DEFAULT_STARTUP_COMPLETION_LED,
                LegoProtocolConstants.MOTOR_CMD_WRITE_DIRECT_MODE_DATA,
                (byte) 0x01,          // mode 1 = sound
                (byte) soundId);
    }

    // =========================================================================
    // Port Information requests (message types 0x21, 0x22)
    // =========================================================================

    /**
     * Sends a Port Information Request for the given port.
     *
     * @param portId          the port identifier byte
     * @param informationType the information type byte (0x01=value, 0x02=mode-info, 0x03=combinations)
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> requestPortInfo(int portId, int informationType) {
        final byte[] msg = {
            0x05,
            (byte) LegoProtocolConstants.HUB_ID,
            (byte) LegoProtocolConstants.MSG_PORT_INFO_REQUEST,
            (byte) portId,
            (byte) informationType
        };
        return write(msg);
    }

    /**
     * Sends a Port Mode Information Request for the given port and mode.
     *
     * @param portId          the port identifier byte
     * @param mode            the mode index byte
     * @param informationType the mode-information type byte
     * @return a future that completes when the write is submitted; never {@code null}
     */
    public @NonNull CompletableFuture<Void> requestPortModeInfo(
            int portId,
            int mode,
            int informationType) {
        final byte[] msg = {
            0x06,
            (byte) LegoProtocolConstants.HUB_ID,
            (byte) LegoProtocolConstants.MSG_PORT_MODE_INFO_REQUEST,
            (byte) portId,
            (byte) mode,
            (byte) informationType
        };
        return write(msg);
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    /**
     * Disconnects the hub and releases all resources.
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

    // =========================================================================
    // Internal constants
    // =========================================================================

    /** Number of fixed header bytes prepended to every Port Output Command. */
    private static final int PORT_OUTPUT_HEADER_SIZE = 6;

    /** Array index of the message-length field in a Port Output Command. */
    private static final int IDX_MSG_LENGTH = 0;

    /** Array index of the Hub ID field in a Port Output Command. */
    private static final int IDX_HUB_ID = 1;

    /** Array index of the message-type field in a Port Output Command. */
    private static final int IDX_MSG_TYPE = 2;

    /** Array index of the port-ID field in a Port Output Command. */
    private static final int IDX_PORT_ID = 3;

    /** Array index of the startup/completion field in a Port Output Command. */
    private static final int IDX_STARTUP_COMPLETION = 4;

    /** Array index of the sub-command field in a Port Output Command. */
    private static final int IDX_SUB_COMMAND = 5;

    /**
     * Startup/completion byte used for LED and sound output commands:
     * buffer if necessary + execute immediately ({@code 0x11}).
     */
    private static final int DEFAULT_STARTUP_COMPLETION_LED = 0x11;

    /** Byte mask for extracting the lowest 8 bits of an integer (Port Input Format Setup). */
    private static final int SETUP_BYTE_MASK = 0xFF;

    /** Bit-shift amount to reach bits 8–15 of a 32-bit integer (Port Input Format Setup). */
    private static final int SETUP_SHIFT_8 = 8;

    /** Bit-shift amount to reach bits 16–23 of a 32-bit integer (Port Input Format Setup). */
    private static final int SETUP_SHIFT_16 = 16;

    /** Bit-shift amount to reach bits 24–31 of a 32-bit integer (Port Input Format Setup). */
    private static final int SETUP_SHIFT_24 = 24;

    /** Total message size in bytes of a Port Input Format Setup (Single) message. */
    private static final int SETUP_SINGLE_MSG_SIZE = 10;

    /** Array index of the port-ID field in a Port Input Format Setup (Single) message. */
    private static final int IDX_SETUP_PORT_ID = 3;

    /** Array index of the mode field in a Port Input Format Setup (Single) message. */
    private static final int IDX_SETUP_MODE = 4;

    /** Array index of the delta-interval byte 0 (LSB) in a Port Input Format Setup (Single) message. */
    private static final int IDX_SETUP_DELTA_0 = 5;

    /** Array index of the delta-interval byte 1 in a Port Input Format Setup (Single) message. */
    private static final int IDX_SETUP_DELTA_1 = 6;

    /** Array index of the delta-interval byte 2 in a Port Input Format Setup (Single) message. */
    private static final int IDX_SETUP_DELTA_2 = 7;

    /** Array index of the delta-interval byte 3 (MSB) in a Port Input Format Setup (Single) message. */
    private static final int IDX_SETUP_DELTA_3 = 8;

    /** Array index of the notify-on-change flag in a Port Input Format Setup (Single) message. */
    private static final int IDX_SETUP_NOTIFY = 9;

    /**
     * Writes a raw LWP message payload to the hub characteristic.
     *
     * <p>Use this method to send protocol messages that are not yet covered
     * by a named DSL method (e.g. {@code PORT_INPUT_FORMAT_SETUP_SINGLE} for
     * sensor configuration).  The payload must be a complete, valid LWP message
     * including the length, hub-ID, and message-type bytes.
     *
     * @param payload the raw bytes to write; must not be {@code null} or empty
     * @return a future that completes when the write is submitted
     */
    public @NonNull CompletableFuture<Void> writeRaw(byte[] payload) {
        return connection.writeWithoutResponse(serviceUuid, writeCharacteristicUuid, payload);
    }

    /**
     * Writes {@code payload} to the LEGO hub characteristic.
     *
     * @param payload the raw bytes to write
     * @return a future that completes when the write is submitted
     */
    private @NonNull CompletableFuture<Void> write(byte[] payload) {
        return writeRaw(payload);
    }

    // =========================================================================
    // Nested fluent builders
    // =========================================================================

    /**
     * Fluent builder for Hub Action commands.
     */
    public static final class HubActionBuilder {

        private final LegoDsl parent;

        /**
         * Creates a {@code HubActionBuilder}.
         *
         * @param parent the owning {@link LegoDsl}; must not be {@code null}
         */
        HubActionBuilder(@NonNull LegoDsl parent) {
            this.parent = parent;
        }

        /**
         * Sends the Switch Off Hub action ({@code 0x01}).
         *
         * @return a future that completes when the write is submitted; never {@code null}
         */
        public @NonNull CompletableFuture<Void> switchOff() {
            return parent.sendHubAction(LegoProtocolConstants.HUB_ACTION_SWITCH_OFF);
        }

        /**
         * Sends the Disconnect action ({@code 0x02}).
         *
         * @return a future that completes when the write is submitted; never {@code null}
         */
        public @NonNull CompletableFuture<Void> disconnect() {
            return parent.sendHubAction(LegoProtocolConstants.HUB_ACTION_DISCONNECT);
        }

        /**
         * Sends the VCC Port Control On action ({@code 0x03}).
         *
         * @return a future that completes when the write is submitted; never {@code null}
         */
        public @NonNull CompletableFuture<Void> vccPortOn() {
            return parent.sendHubAction(LegoProtocolConstants.HUB_ACTION_VCC_PORT_ON);
        }

        /**
         * Sends the VCC Port Control Off action ({@code 0x04}).
         *
         * @return a future that completes when the write is submitted; never {@code null}
         */
        public @NonNull CompletableFuture<Void> vccPortOff() {
            return parent.sendHubAction(LegoProtocolConstants.HUB_ACTION_VCC_PORT_OFF);
        }

        /**
         * Sends the Activate Busy Indication action ({@code 0x05}).
         *
         * <p>Instructs the hub to display a "busy" LED state.
         *
         * @return a future that completes when the write is submitted; never {@code null}
         */
        public @NonNull CompletableFuture<Void> activateBusyIndication() {
            return parent.sendHubAction(
                    LegoProtocolConstants.HUB_ACTION_ACTIVATE_BUSY_INDICATION);
        }

        /**
         * Sends the Reset Busy Indication action ({@code 0x06}).
         *
         * <p>Clears the "busy" LED state set by {@link #activateBusyIndication()}.
         *
         * @return a future that completes when the write is submitted; never {@code null}
         */
        public @NonNull CompletableFuture<Void> resetBusyIndication() {
            return parent.sendHubAction(
                    LegoProtocolConstants.HUB_ACTION_RESET_BUSY_INDICATION);
        }

        /**
         * Sends the Shutdown action ({@code 0x2F}).
         *
         * <p>Powers off the hub immediately without sending an upstream
         * "will switch off" notification.
         *
         * @return a future that completes when the write is submitted; never {@code null}
         */
        public @NonNull CompletableFuture<Void> shutdown() {
            return parent.sendHubAction(LegoProtocolConstants.HUB_ACTION_SHUTDOWN);
        }

        /**
         * Returns the parent {@link LegoDsl} to continue chaining.
         *
         * @return the parent builder; never {@code null}
         */
        public @NonNull LegoDsl and() {
            return parent;
        }
    }

    /**
     * Fluent builder for Port Output motor commands.
     */
    public static final class MotorBuilder {

        /** Startup info: buffer if necessary + execute immediately. */
        private static final int DEFAULT_STARTUP_COMPLETION = 0x11;

        /** Default maximum power percentage used by the single-argument {@link #startSpeed(int)}. */
        private static final int DEFAULT_MAX_POWER = 100;

        /** Byte mask for extracting the lowest 8 bits of an integer. */
        private static final int BYTE_MASK = 0xFF;

        /** Bit-shift amount to reach bits 8–15 of a 32-bit integer. */
        private static final int SHIFT_8 = 8;

        /** Bit-shift amount to reach bits 16–23 of a 32-bit integer. */
        private static final int SHIFT_16 = 16;

        /** Bit-shift amount to reach bits 24–31 of a 32-bit integer. */
        private static final int SHIFT_24 = 24;

        /**
         * End-state byte: hold position ({@code 0x7E} =
         * {@link LegoProtocolConstants#BRAKING_STYLE_HOLD}).
         *
         * <p>Used as the default end-state parameter in timed and degree-limited
         * motor commands when the caller does not specify an explicit end state.
         */
        private static final byte END_STATE_BRAKE =
                (byte) LegoProtocolConstants.BRAKING_STYLE_HOLD;

        private final LegoDsl parent;
        private final int portId;

        /**
         * Creates a {@code MotorBuilder} for the given port.
         *
         * @param parent the owning {@link LegoDsl}; must not be {@code null}
         * @param portId the port identifier byte
         */
        MotorBuilder(@NonNull LegoDsl parent, int portId) {
            this.parent = parent;
            this.portId = portId;
        }

        /**
         * Sends a {@code StartSpeed} command ({@code 0x07}) on this port.
         *
         * @param speed   signed speed in the range −100 to 100
         * @param maxPower maximum power (0–100)
         * @return a future that completes when the write is submitted; never {@code null}
         */
        public @NonNull CompletableFuture<Void> startSpeed(int speed, int maxPower) {
            return parent.portOutputCommand(
                    portId,
                    DEFAULT_STARTUP_COMPLETION,
                    LegoProtocolConstants.MOTOR_CMD_START_SPEED,
                    (byte) speed,
                    (byte) maxPower,
                    (byte) 0x00);   // end state: float
        }

        /**
         * Sends a {@code StartSpeed} command with default max power (100).
         *
         * @param speed signed speed in the range −100 to 100
         * @return a future that completes when the write is submitted; never {@code null}
         */
        public @NonNull CompletableFuture<Void> startSpeed(int speed) {
            return startSpeed(speed, DEFAULT_MAX_POWER);
        }

        /**
         * Sends a {@code StartSpeedForTime} command ({@code 0x09}) on this port.
         *
         * @param timeMs  duration in milliseconds
         * @param speed   signed speed in the range −100 to 100
         * @param maxPower maximum power (0–100)
         * @return a future that completes when the write is submitted; never {@code null}
         */
        public @NonNull CompletableFuture<Void> startSpeedForTime(
                int timeMs, int speed, int maxPower) {
            return startSpeedForTime(timeMs, speed, maxPower,
                    LegoProtocolConstants.BRAKING_STYLE_HOLD);
        }

        /**
         * Sends a {@code StartSpeedForTime} command ({@code 0x09}) on this port
         * with an explicit end state.
         *
         * @param timeMs   duration in milliseconds
         * @param speed    signed speed in the range −100 to 100
         * @param maxPower maximum power (0–100)
         * @param endState braking style after the command completes; use one of the
         *                 {@code BRAKING_STYLE_*} constants from
         *                 {@link LegoProtocolConstants}
         * @return a future that completes when the write is submitted; never {@code null}
         */
        public @NonNull CompletableFuture<Void> startSpeedForTime(
                int timeMs, int speed, int maxPower, int endState) {
            return parent.portOutputCommand(
                    portId,
                    DEFAULT_STARTUP_COMPLETION,
                    LegoProtocolConstants.MOTOR_CMD_START_SPEED_FOR_TIME,
                    (byte) (timeMs & BYTE_MASK),
                    (byte) ((timeMs >> SHIFT_8) & BYTE_MASK),
                    (byte) speed,
                    (byte) maxPower,
                    (byte) endState,
                    (byte) 0x00);   // use acceleration profile
        }

        /**
         * Sends a {@code StartSpeedForDegrees} command ({@code 0x0B}) on this port.
         *
         * @param degrees  rotation in degrees (unsigned 32-bit)
         * @param speed    signed speed in the range −100 to 100
         * @param maxPower maximum power (0–100)
         * @return a future that completes when the write is submitted; never {@code null}
         */
        public @NonNull CompletableFuture<Void> startSpeedForDegrees(
                int degrees, int speed, int maxPower) {
            return startSpeedForDegrees(degrees, speed, maxPower,
                    LegoProtocolConstants.BRAKING_STYLE_HOLD);
        }

        /**
         * Sends a {@code StartSpeedForDegrees} command ({@code 0x0B}) on this port
         * with an explicit end state.
         *
         * @param degrees  rotation in degrees (unsigned 32-bit)
         * @param speed    signed speed in the range −100 to 100
         * @param maxPower maximum power (0–100)
         * @param endState braking style after the command completes; use one of the
         *                 {@code BRAKING_STYLE_*} constants from
         *                 {@link LegoProtocolConstants}
         * @return a future that completes when the write is submitted; never {@code null}
         */
        public @NonNull CompletableFuture<Void> startSpeedForDegrees(
                int degrees, int speed, int maxPower, int endState) {
            return parent.portOutputCommand(
                    portId,
                    DEFAULT_STARTUP_COMPLETION,
                    LegoProtocolConstants.MOTOR_CMD_START_SPEED_FOR_DEGREES,
                    (byte) (degrees & BYTE_MASK),
                    (byte) ((degrees >> SHIFT_8) & BYTE_MASK),
                    (byte) ((degrees >> SHIFT_16) & BYTE_MASK),
                    (byte) ((degrees >> SHIFT_24) & BYTE_MASK),
                    (byte) speed,
                    (byte) maxPower,
                    (byte) endState,
                    (byte) 0x00);   // use acceleration profile
        }

        /**
         * Sends a {@code GotoAbsolutePosition} command ({@code 0x0D}) on this port.
         *
         * @param position signed absolute position in degrees
         * @param speed    signed speed in the range −100 to 100
         * @param maxPower maximum power (0–100)
         * @return a future that completes when the write is submitted; never {@code null}
         */
        public @NonNull CompletableFuture<Void> gotoAbsolutePosition(
                int position, int speed, int maxPower) {
            return gotoAbsolutePosition(position, speed, maxPower,
                    LegoProtocolConstants.BRAKING_STYLE_HOLD);
        }

        /**
         * Sends a {@code GotoAbsolutePosition} command ({@code 0x0D}) on this port
         * with an explicit end state.
         *
         * @param position signed absolute position in degrees
         * @param speed    signed speed in the range −100 to 100
         * @param maxPower maximum power (0–100)
         * @param endState braking style after the command completes; use one of the
         *                 {@code BRAKING_STYLE_*} constants from
         *                 {@link LegoProtocolConstants}
         * @return a future that completes when the write is submitted; never {@code null}
         */
        public @NonNull CompletableFuture<Void> gotoAbsolutePosition(
                int position, int speed, int maxPower, int endState) {
            return parent.portOutputCommand(
                    portId,
                    DEFAULT_STARTUP_COMPLETION,
                    LegoProtocolConstants.MOTOR_CMD_GOTO_ABSOLUTE_POSITION,
                    (byte) (position & BYTE_MASK),
                    (byte) ((position >> SHIFT_8) & BYTE_MASK),
                    (byte) ((position >> SHIFT_16) & BYTE_MASK),
                    (byte) ((position >> SHIFT_24) & BYTE_MASK),
                    (byte) speed,
                    (byte) maxPower,
                    (byte) endState,
                    (byte) 0x00);   // use profile
        }

        /**
         * Sends a {@code WriteDirect} command ({@code 0x50}) on this port.
         *
         * <p>Used for low-level direct writes to output devices such as LEDs.
         * The meaning of {@code data} bytes depends on the connected device type.
         *
         * @param data the raw payload bytes to write; must not be {@code null}
         * @return a future that completes when the write is submitted; never {@code null}
         */
        public @NonNull CompletableFuture<Void> writeDirect(byte... data) {
            return parent.portOutputCommand(
                    portId,
                    DEFAULT_STARTUP_COMPLETION,
                    LegoProtocolConstants.MOTOR_CMD_WRITE_DIRECT,
                    data);
        }

        /**
         * Sends a {@code WriteDirectModeData} command ({@code 0x51}) on this port.
         *
         * <p>Used to write mode-specific data to output devices, for example:
         * <ul>
         *   <li>Hub LED colour: mode {@code 0x00}, one data byte = colour index</li>
         *   <li>Duplo Train speaker: mode {@code 0x01}, one data byte = sound ID</li>
         * </ul>
         *
         * @param mode the device mode index byte
         * @param data the mode-specific payload bytes; must not be {@code null}
         * @return a future that completes when the write is submitted; never {@code null}
         */
        public @NonNull CompletableFuture<Void> writeDirectModeData(int mode, byte... data) {
            final byte[] params = new byte[1 + data.length];
            params[0] = (byte) mode;
            System.arraycopy(data, 0, params, 1, data.length);
            return parent.portOutputCommand(
                    portId,
                    DEFAULT_STARTUP_COMPLETION,
                    LegoProtocolConstants.MOTOR_CMD_WRITE_DIRECT_MODE_DATA,
                    params);
        }

        /**
         * Sends a {@code SetAccTime} command ({@code 0x01}).
         *
         * @param timeMs ramp-up time in milliseconds
         * @return a future that completes when the write is submitted; never {@code null}
         */
        public @NonNull CompletableFuture<Void> setAccTime(int timeMs) {
            return parent.portOutputCommand(
                    portId,
                    DEFAULT_STARTUP_COMPLETION,
                    LegoProtocolConstants.MOTOR_CMD_SET_ACC_TIME,
                    (byte) (timeMs & BYTE_MASK),
                    (byte) ((timeMs >> SHIFT_8) & BYTE_MASK),
                    (byte) 0x00);
        }

        /**
         * Sends a {@code SetDecTime} command ({@code 0x02}).
         *
         * @param timeMs ramp-down time in milliseconds
         * @return a future that completes when the write is submitted; never {@code null}
         */
        public @NonNull CompletableFuture<Void> setDecTime(int timeMs) {
            return parent.portOutputCommand(
                    portId,
                    DEFAULT_STARTUP_COMPLETION,
                    LegoProtocolConstants.MOTOR_CMD_SET_DEC_TIME,
                    (byte) (timeMs & BYTE_MASK),
                    (byte) ((timeMs >> SHIFT_8) & BYTE_MASK),
                    (byte) 0x00);
        }

        /**
         * Returns the parent {@link LegoDsl} to continue chaining.
         *
         * @return the parent builder; never {@code null}
         */
        public @NonNull LegoDsl and() {
            return parent;
        }
    }
}
