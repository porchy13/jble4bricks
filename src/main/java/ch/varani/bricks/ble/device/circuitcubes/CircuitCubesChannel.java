package ch.varani.bricks.ble.device.circuitcubes;

/**
 * Named motor-channel constants for the Circuit Cubes (Tenka) BLE motor control protocol.
 *
 * <p>Each constant wraps the corresponding {@code CHANNEL_*} raw {@code char} defined in
 * {@link CircuitCubesProtocolConstants}. Use {@link #code()} to obtain the ASCII character
 * appended at the end of every motor command string
 * (format: {@code <sign><magnitude:03d><channel>}).
 *
 * <p><strong>Note:</strong> {@link CircuitCubesProtocolConstants#CHANNEL_B} shares the same
 * ASCII byte value ({@code 'b'}) as
 * {@link CircuitCubesProtocolConstants#BATTERY_QUERY_COMMAND}. These are distinct constants
 * with different semantics; {@code CircuitCubesChannel.B} refers exclusively to the motor
 * channel, never to the battery query.
 *
 * <p>Reference:
 * <a href="https://github.com/made-by-simon/circuit-cubes-python-interface">
 * Circuit Cubes Python Interface</a>
 *
 * <p>Usage example — drive channel A forward and stop channel C:
 * <pre>{@code
 * circuitCubesDsl.motorForward(CircuitCubesChannel.A, 100)
 *                .thenCompose(v -> circuitCubesDsl.motorStop(CircuitCubesChannel.C))
 *                .get();
 * }</pre>
 */
public enum CircuitCubesChannel {

    /**
     * Motor channel A — ASCII letter {@code 'a'}.
     *
     * <p>Corresponds to {@link CircuitCubesProtocolConstants#CHANNEL_A}.
     */
    A(CircuitCubesProtocolConstants.CHANNEL_A),

    /**
     * Motor channel B — ASCII letter {@code 'b'}.
     *
     * <p>Corresponds to {@link CircuitCubesProtocolConstants#CHANNEL_B}.
     * Note: this shares the byte value {@code 'b'} with
     * {@link CircuitCubesProtocolConstants#BATTERY_QUERY_COMMAND}; they are distinct
     * protocol elements.
     */
    B(CircuitCubesProtocolConstants.CHANNEL_B),

    /**
     * Motor channel C — ASCII letter {@code 'c'}.
     *
     * <p>Corresponds to {@link CircuitCubesProtocolConstants#CHANNEL_C}.
     */
    C(CircuitCubesProtocolConstants.CHANNEL_C);

    /**
     * The ASCII character appended as the channel letter in every motor command string.
     */
    private final char channelChar;

    /**
     * Constructs a channel constant.
     *
     * @param channelChar the ASCII channel letter for this channel
     */
    CircuitCubesChannel(final char channelChar) {
        this.channelChar = channelChar;
    }

    /**
     * Returns the ASCII channel letter used in the Circuit Cubes motor command string.
     *
     * @return the channel character ({@code 'a'}, {@code 'b'}, or {@code 'c'})
     */
    public char code() {
        return channelChar;
    }
}
