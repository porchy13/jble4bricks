package ch.varani.bricks.ble.device.lego;

/**
 * Sensor-mode indices for peripherals attached to the WeDo 2.0 Smart Hub.
 *
 * <p>The mode index is the {@code mode} byte (byte&nbsp;4) in the 11-byte
 * sensor-subscription command written to
 * {@link LegoProtocolConstants#WEDO2_PORT_TYPE_WRITE_UUID}.  It selects which
 * measurement the hub firmware should report for the attached device.
 *
 * <p>Each constant documents which {@link WeDo2DeviceType} it applies to.
 * Only one mode is currently documented and tested per device type; if the
 * WeDo 2.0 firmware exposes additional modes in the future, new constants
 * should be added here.
 *
 * <p>Reference: nathankellenicki/node-poweredup (MIT) —
 * https://github.com/nathankellenicki/node-poweredup —
 * {@code src/hubs/wedo2smarthub.ts _parseSensorMessage()}
 *
 * <p>Usage example:
 * <pre>{@code
 * weDo2Dsl.subscribeSensor(WeDo2Port.B, WeDo2DeviceType.MOTION_SENSOR,
 *                           WeDo2SensorMode.MOTION_DISTANCE).get();
 * }</pre>
 */
public enum WeDo2SensorMode {

    /**
     * Motion / Distance Sensor — distance mode ({@code 0}).
     *
     * <p>Applicable device type: {@link WeDo2DeviceType#MOTION_SENSOR}.
     *
     * <p>In this mode the sensor reports distance in centimetres.  The raw
     * notification payload layout (confirmed against
     * nathankellenicki/node-poweredup
     * {@code src/hubs/wedo2smarthub.ts _parseSensorMessage()}) is:
     * <pre>
     * [indicator, portId, value, overflow]
     * </pre>
     * {@code msg[2]} is the distance in cm; add 255 to it if
     * {@code msg[3] == 0x01} (overflow flag).
     */
    MOTION_DISTANCE(0),

    /**
     * Motion / Distance Sensor — detect mode ({@code 1}).
     *
     * <p>Applicable device type: {@link WeDo2DeviceType#MOTION_SENSOR}.
     *
     * <p>In this mode the sensor reports a simple near/far detection value
     * rather than a calibrated distance.
     */
    MOTION_DETECT(1),

    /**
     * RGB LED — indexed colour mode ({@code 1}).
     *
     * <p>Applicable device type: {@link WeDo2DeviceType#RGB_LED}.
     * Use in combination with the LED colour-index command.
     */
    LED_INDEX(1),

    /**
     * Generic mode zero ({@code 0}) — default / fallback for device types
     * that only expose a single measurement mode (e.g. the Simple Motor or
     * Piezo Buzzer when used as a subscription target).
     */
    DEFAULT(0);

    /** The raw mode-index byte sent over BLE. */
    private final int modeIndex;

    /**
     * Constructs a sensor-mode constant.
     *
     * @param modeIndex the raw wire byte value (0-based mode index)
     */
    WeDo2SensorMode(final int modeIndex) {
        this.modeIndex = modeIndex;
    }

    /**
     * Returns the raw mode-index byte for use in the sensor-subscription
     * command payload.
     *
     * @return the mode index ({@code 0}-based)
     */
    public int code() {
        return modeIndex;
    }
}
