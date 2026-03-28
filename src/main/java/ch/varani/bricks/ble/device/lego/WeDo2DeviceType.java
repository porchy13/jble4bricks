package ch.varani.bricks.ble.device.lego;

/**
 * Device-type identifiers for peripherals attached to the WeDo 2.0 Smart Hub.
 *
 * <p>These values are used as the {@code deviceType} byte (byte&nbsp;3) in the
 * 11-byte sensor-subscription command written to
 * {@link LegoProtocolConstants#WEDO2_PORT_TYPE_WRITE_UUID}.  They identify
 * which peripheral is connected to a port, allowing the hub firmware to route
 * sensor notifications correctly.
 *
 * <p>Note: {@link #MOTOR} ({@code 0x01}) also doubles as the {@code typeId}
 * byte in the 4-byte motor command written to
 * {@link LegoProtocolConstants#WEDO2_MOTOR_VALUE_WRITE_UUID}.
 *
 * <p>Reference: nathankellenicki/node-poweredup (MIT) —
 * https://github.com/nathankellenicki/node-poweredup — {@code src/consts.ts}
 *
 * <p>Usage example:
 * <pre>{@code
 * weDo2Dsl.subscribeSensor(WeDo2Port.B, WeDo2DeviceType.MOTION_SENSOR,
 *                           WeDo2SensorMode.MOTION_DISTANCE).get();
 * }</pre>
 */
public enum WeDo2DeviceType {

    /**
     * Simple / Medium Linear Motor — type ID {@code 0x01}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#WEDO2_MOTOR_TYPE_ID}.
     * This value is also written as the {@code typeId} byte in motor
     * power commands.
     */
    MOTOR(LegoProtocolConstants.WEDO2_MOTOR_TYPE_ID),

    /**
     * Piezo Buzzer (internal) — type ID {@code 0x02}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#WEDO2_PIEZO_TYPE_ID}.
     */
    PIEZO_BUZZER(LegoProtocolConstants.WEDO2_PIEZO_TYPE_ID),

    /**
     * RGB LED (internal) — type ID {@code 0x22} (34).
     *
     * <p>Corresponds to {@link LegoProtocolConstants#WEDO2_RGB_LED_TYPE_ID}.
     */
    RGB_LED(LegoProtocolConstants.WEDO2_RGB_LED_TYPE_ID),

    /**
     * Motion / Distance Sensor — type ID {@code 0x23} (35).
     *
     * <p>Corresponds to
     * {@link LegoProtocolConstants#WEDO2_MOTION_SENSOR_TYPE_ID}.
     * This is the most commonly subscribed sensor on the WeDo 2.0 hub.
     */
    MOTION_SENSOR(LegoProtocolConstants.WEDO2_MOTION_SENSOR_TYPE_ID);

    /** The raw device-type byte sent over BLE. */
    private final int typeId;

    /**
     * Constructs a device-type constant.
     *
     * @param typeId the raw wire byte value
     */
    WeDo2DeviceType(final int typeId) {
        this.typeId = typeId;
    }

    /**
     * Returns the raw device-type byte for use in BLE command payloads.
     *
     * @return the device type identifier
     */
    public int code() {
        return typeId;
    }
}
