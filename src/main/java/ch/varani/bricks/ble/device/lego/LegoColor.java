package ch.varani.bricks.ble.device.lego;

/**
 * Named colour constants for the LEGO Powered Up hub LED, expressed as indexed
 * colour mode values compatible with the {@code WriteDirectModeData} Port Output
 * Command sent over the LEGO Wireless Protocol 3.0 (LWP3).
 *
 * <p>Each constant wraps the corresponding {@code COLOR_*} raw integer defined
 * in {@link LegoProtocolConstants}.  Use {@link #code()} to obtain the wire
 * byte value.
 *
 * <p>Reference: nathankellenicki/node-poweredup (MIT) —
 * https://github.com/nathankellenicki/node-poweredup — {@code src/consts.ts Color}
 *
 * <p>Usage example — set the City Hub LED to red:
 * <pre>{@code
 * legoDsl.setHubLedColor(LegoProtocolConstants.CITY_HUB_PORT_LED, LegoColor.RED).get();
 * }</pre>
 */
public enum LegoColor {

    /**
     * Black (LED off) — colour index {@code 0}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#COLOR_BLACK}.
     */
    BLACK(LegoProtocolConstants.COLOR_BLACK),

    /**
     * Pink — colour index {@code 1}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#COLOR_PINK}.
     */
    PINK(LegoProtocolConstants.COLOR_PINK),

    /**
     * Purple — colour index {@code 2}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#COLOR_PURPLE}.
     */
    PURPLE(LegoProtocolConstants.COLOR_PURPLE),

    /**
     * Blue — colour index {@code 3}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#COLOR_BLUE}.
     */
    BLUE(LegoProtocolConstants.COLOR_BLUE),

    /**
     * Light blue — colour index {@code 4}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#COLOR_LIGHT_BLUE}.
     */
    LIGHT_BLUE(LegoProtocolConstants.COLOR_LIGHT_BLUE),

    /**
     * Cyan — colour index {@code 5}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#COLOR_CYAN}.
     */
    CYAN(LegoProtocolConstants.COLOR_CYAN),

    /**
     * Green — colour index {@code 6}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#COLOR_GREEN}.
     */
    GREEN(LegoProtocolConstants.COLOR_GREEN),

    /**
     * Yellow — colour index {@code 7}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#COLOR_YELLOW}.
     */
    YELLOW(LegoProtocolConstants.COLOR_YELLOW),

    /**
     * Orange — colour index {@code 8}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#COLOR_ORANGE}.
     */
    ORANGE(LegoProtocolConstants.COLOR_ORANGE),

    /**
     * Red — colour index {@code 9}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#COLOR_RED}.
     */
    RED(LegoProtocolConstants.COLOR_RED),

    /**
     * White — colour index {@code 10}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#COLOR_WHITE}.
     */
    WHITE(LegoProtocolConstants.COLOR_WHITE),

    /**
     * None / LED off — colour index {@code 255}.
     *
     * <p>Corresponds to {@link LegoProtocolConstants#COLOR_NONE}.
     */
    NONE(LegoProtocolConstants.COLOR_NONE);

    /**
     * The raw indexed-colour byte value as sent in the {@code WriteDirectModeData}
     * Port Output Command payload over LWP3.
     */
    private final int colorIndex;

    /**
     * Constructs a colour constant.
     *
     * @param colorIndex the raw wire byte value (0–10 or 255 for NONE)
     */
    LegoColor(final int colorIndex) {
        this.colorIndex = colorIndex;
    }

    /**
     * Returns the raw indexed-colour byte value for use in the BLE command
     * payload.
     *
     * @return the colour index ({@code 0}–{@code 10}, or {@code 255} for
     *         {@link #NONE})
     */
    public int code() {
        return colorIndex;
    }
}
