package ch.varani.bricks.ble.device.lego;

/**
 * The two physical ports on the WeDo 2.0 Smart Hub.
 *
 * <p>WeDo 2.0 ports are numbered 1-based: Port&nbsp;A is {@code 0x01} and
 * Port&nbsp;B is {@code 0x02}.  These identifiers appear as byte&nbsp;0 of
 * the 4-byte motor command written to
 * {@link LegoProtocolConstants#WEDO2_MOTOR_VALUE_WRITE_UUID} and as
 * byte&nbsp;2 of the 11-byte sensor-subscription command written to
 * {@link LegoProtocolConstants#WEDO2_PORT_TYPE_WRITE_UUID}.
 *
 * <p>Reference: nathankellenicki/node-poweredup (MIT) —
 * https://github.com/nathankellenicki/node-poweredup — {@code src/consts.ts}
 *
 * <p>Usage example:
 * <pre>{@code
 * weDo2Dsl.motorPower(WeDo2Port.A, 75).get();
 * }</pre>
 */
public enum WeDo2Port {

    /**
     * Physical port A — wire value {@code 0x01}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#WEDO2_PORT_A}.
     */
    A(LegoProtocolConstants.WEDO2_PORT_A),

    /**
     * Physical port B — wire value {@code 0x02}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#WEDO2_PORT_B}.
     */
    B(LegoProtocolConstants.WEDO2_PORT_B);

    /** The raw port-identifier byte sent over BLE. */
    private final int portId;

    /**
     * Constructs a port constant.
     *
     * @param portId the raw wire byte value
     */
    WeDo2Port(final int portId) {
        this.portId = portId;
    }

    /**
     * Returns the raw port-identifier byte for use in BLE command payloads.
     *
     * @return the port identifier ({@code 0x01} for A, {@code 0x02} for B)
     */
    public int code() {
        return portId;
    }
}
