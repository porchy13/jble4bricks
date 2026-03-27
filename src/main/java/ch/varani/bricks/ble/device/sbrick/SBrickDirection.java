package ch.varani.bricks.ble.device.sbrick;

/**
 * Named motor-direction constants for the SBrick BLE remote control protocol.
 *
 * <p>Each constant wraps the corresponding {@code DIRECTION_*} raw integer defined in
 * {@link SBrickProtocolConstants}. Use {@link #code()} to obtain the wire byte value
 * written as the direction field of the {@code Drive} ({@code 0x01}) command.
 *
 * <p>Reference: SBrick BLE Protocol PDF (Rev. 26, 2020-10-28), Vengit Limited.
 *
 * <p>Usage example — drive channel A clockwise at half power:
 * <pre>{@code
 * sBrickDsl.drive(SBrickChannel.A, SBrickDirection.CLOCKWISE, 128).get();
 * }</pre>
 */
public enum SBrickDirection {

    /**
     * Clockwise rotation — direction byte {@code 0x00}.
     *
     * <p>Corresponds to {@link SBrickProtocolConstants#DIRECTION_CLOCKWISE}.
     */
    CLOCKWISE(SBrickProtocolConstants.DIRECTION_CLOCKWISE),

    /**
     * Counter-clockwise rotation — direction byte {@code 0x01}.
     *
     * <p>Corresponds to {@link SBrickProtocolConstants#DIRECTION_COUNTER_CLOCKWISE}.
     */
    COUNTER_CLOCKWISE(SBrickProtocolConstants.DIRECTION_COUNTER_CLOCKWISE);

    /**
     * The raw direction byte transmitted in the Drive command payload.
     */
    private final int directionByte;

    /**
     * Constructs a direction constant.
     *
     * @param directionByte the raw wire byte value for this direction
     */
    SBrickDirection(final int directionByte) {
        this.directionByte = directionByte;
    }

    /**
     * Returns the raw direction byte used in the SBrick Drive command payload.
     *
     * @return the direction byte ({@code 0x00} = clockwise, {@code 0x01} = counter-clockwise)
     */
    public int code() {
        return directionByte;
    }
}
