package ch.varani.bricks.ble.device.sbrick;

/**
 * Named motor-channel constants for the SBrick BLE remote control protocol.
 *
 * <p>Each constant wraps the corresponding {@code CHANNEL_*} raw integer defined in
 * {@link SBrickProtocolConstants}. Use {@link #code()} to obtain the wire byte value
 * written in the {@code Drive} ({@code 0x01}) and {@code Brake} ({@code 0x00}) commands.
 *
 * <p><strong>Note on port index ordering:</strong> the SBrick protocol assigns port indices
 * non-alphabetically: A=0x00, C=0x01, B=0x02, D=0x03. This ordering is preserved by the
 * underlying constants; {@code SBrickChannel} exposes them with their correct alphabetical
 * labels.
 *
 * <p>Reference: SBrick BLE Protocol PDF (Rev. 26, 2020-10-28), Vengit Limited.
 *
 * <p>Usage example — drive channel A forward and brake channel B:
 * <pre>{@code
 * sBrickDsl.drive(SBrickChannel.A, SBrickDirection.CLOCKWISE, 200)
 *          .thenCompose(v -> sBrickDsl.brake(SBrickChannel.B))
 *          .get();
 * }</pre>
 */
public enum SBrickChannel {

    /**
     * Motor channel A — port index {@code 0x00}.
     *
     * <p>Corresponds to {@link SBrickProtocolConstants#CHANNEL_A}.
     */
    A(SBrickProtocolConstants.CHANNEL_A),

    /**
     * Motor channel B — port index {@code 0x02}.
     *
     * <p>Corresponds to {@link SBrickProtocolConstants#CHANNEL_B}.
     * Note: the wire index is 0x02, not 0x01 — the SBrick protocol orders
     * channels as A=0x00, C=0x01, B=0x02, D=0x03.
     */
    B(SBrickProtocolConstants.CHANNEL_B),

    /**
     * Motor channel C — port index {@code 0x01}.
     *
     * <p>Corresponds to {@link SBrickProtocolConstants#CHANNEL_C}.
     * Note: the wire index is 0x01, not 0x02 — the SBrick protocol orders
     * channels as A=0x00, C=0x01, B=0x02, D=0x03.
     */
    C(SBrickProtocolConstants.CHANNEL_C),

    /**
     * Motor channel D — port index {@code 0x03}.
     *
     * <p>Corresponds to {@link SBrickProtocolConstants#CHANNEL_D}.
     */
    D(SBrickProtocolConstants.CHANNEL_D);

    /**
     * The raw port index byte transmitted in Drive and Brake command payloads.
     */
    private final int portIndex;

    /**
     * Constructs a channel constant.
     *
     * @param portIndex the raw wire byte value for this channel
     */
    SBrickChannel(final int portIndex) {
        this.portIndex = portIndex;
    }

    /**
     * Returns the raw port index byte used in the SBrick Drive and Brake command payloads.
     *
     * @return the port index ({@code 0x00}–{@code 0x03})
     */
    public int code() {
        return portIndex;
    }
}
