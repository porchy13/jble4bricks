package ch.varani.bricks.ble.device.lego;

/**
 * Named colour constants for the WeDo 2.0 hub LED, expressed as indexed colour
 * mode values compatible with the 4-byte indexed-colour command written to
 * {@link LegoProtocolConstants#WEDO2_MOTOR_VALUE_WRITE_UUID}.
 *
 * <p>Each constant wraps the corresponding {@code WEDO2_LED_COLOR_*} raw integer
 * defined in {@link LegoProtocolConstants}.  Use {@link #code()} to obtain the
 * wire byte value.
 *
 * <p>Reference: nathankellenicki/node-poweredup (MIT) —
 * https://github.com/nathankellenicki/node-poweredup — {@code src/consts.ts Color}
 *
 * <p>Usage example — set the hub LED to red:
 * <pre>{@code
 * weDo2Dsl.setLedColor(WeDo2LedColor.RED).get();
 * }</pre>
 */
public enum WeDo2LedColor {

    /**
     * Black (LED off) — colour index {@code 0x00}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#WEDO2_LED_COLOR_BLACK}.
     */
    BLACK(LegoProtocolConstants.WEDO2_LED_COLOR_BLACK),

    /**
     * Pink — colour index {@code 0x01}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#WEDO2_LED_COLOR_PINK}.
     */
    PINK(LegoProtocolConstants.WEDO2_LED_COLOR_PINK),

    /**
     * Purple — colour index {@code 0x02}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#WEDO2_LED_COLOR_PURPLE}.
     */
    PURPLE(LegoProtocolConstants.WEDO2_LED_COLOR_PURPLE),

    /**
     * Blue — colour index {@code 0x03}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#WEDO2_LED_COLOR_BLUE}.
     */
    BLUE(LegoProtocolConstants.WEDO2_LED_COLOR_BLUE),

    /**
     * Light blue — colour index {@code 0x04}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#WEDO2_LED_COLOR_LIGHT_BLUE}.
     */
    LIGHT_BLUE(LegoProtocolConstants.WEDO2_LED_COLOR_LIGHT_BLUE),

    /**
     * Cyan — colour index {@code 0x05}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#WEDO2_LED_COLOR_CYAN}.
     */
    CYAN(LegoProtocolConstants.WEDO2_LED_COLOR_CYAN),

    /**
     * Green — colour index {@code 0x06}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#WEDO2_LED_COLOR_GREEN}.
     */
    GREEN(LegoProtocolConstants.WEDO2_LED_COLOR_GREEN),

    /**
     * Yellow — colour index {@code 0x07}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#WEDO2_LED_COLOR_YELLOW}.
     */
    YELLOW(LegoProtocolConstants.WEDO2_LED_COLOR_YELLOW),

    /**
     * Orange — colour index {@code 0x08}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#WEDO2_LED_COLOR_ORANGE}.
     */
    ORANGE(LegoProtocolConstants.WEDO2_LED_COLOR_ORANGE),

    /**
     * Red — colour index {@code 0x09}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#WEDO2_LED_COLOR_RED}.
     */
    RED(LegoProtocolConstants.WEDO2_LED_COLOR_RED),

    /**
     * White — colour index {@code 0x0A}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#WEDO2_LED_COLOR_WHITE}.
     */
    WHITE(LegoProtocolConstants.WEDO2_LED_COLOR_WHITE);

    /**
     * The raw indexed-colour byte value as sent over BLE in the
     * indexed-colour data command written to
     * {@link LegoProtocolConstants#WEDO2_MOTOR_VALUE_WRITE_UUID}.
     */
    private final int colorIndex;

    /**
     * Constructs a colour constant.
     *
     * @param colorIndex the raw wire byte value (0–10)
     */
    WeDo2LedColor(final int colorIndex) {
        this.colorIndex = colorIndex;
    }

    /**
     * Returns the raw indexed-colour byte value for use in the BLE command
     * payload.
     *
     * @return the colour index ({@code 0x00}–{@code 0x0A})
     */
    public int code() {
        return colorIndex;
    }
}
