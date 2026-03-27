package ch.varani.bricks.ble.device.buwizz;

/**
 * Named power-level constants for the BuWizz 2.0 BLE protocol (API version 1.3).
 *
 * <p>Each constant wraps the corresponding {@code POWER_LEVEL_*} raw integer defined in
 * {@link BuWizz2ProtocolConstants}. Use {@link #code()} to obtain the wire byte value
 * written as the payload of the Set Power Level command ({@code 0x11}).
 *
 * <p>The BuWizz 2.0 defaults to {@link #DISABLED} (power level 0) after connection.
 * Set it to {@link #NORMAL} or higher before sending motor commands.
 *
 * <p>Reference: BuWizz 2.0 BLE Protocol, API version 1.3.
 *
 * <p>Usage example — enable normal power and drive motors:
 * <pre>{@code
 * buWizz2Dsl.setPowerLevel(BuWizz2PowerLevel.NORMAL)
 *           .thenCompose(v -> buWizz2Dsl.setMotorData(100, -100, 0, 0, false, false, false, false))
 *           .get();
 * }</pre>
 */
public enum BuWizz2PowerLevel {

    /**
     * Power level disabled — motors receive no power.
     *
     * <p>This is the default after connecting. Value: {@code 0}.
     *
     * <p>Corresponds to {@link BuWizz2ProtocolConstants#POWER_LEVEL_DISABLED}.
     */
    DISABLED(BuWizz2ProtocolConstants.POWER_LEVEL_DISABLED),

    /**
     * Slow power level. Value: {@code 1}.
     *
     * <p>Corresponds to {@link BuWizz2ProtocolConstants#POWER_LEVEL_SLOW}.
     */
    SLOW(BuWizz2ProtocolConstants.POWER_LEVEL_SLOW),

    /**
     * Normal power level. Value: {@code 2}.
     *
     * <p>Corresponds to {@link BuWizz2ProtocolConstants#POWER_LEVEL_NORMAL}.
     */
    NORMAL(BuWizz2ProtocolConstants.POWER_LEVEL_NORMAL),

    /**
     * Fast power level. Value: {@code 3}.
     *
     * <p>Corresponds to {@link BuWizz2ProtocolConstants#POWER_LEVEL_FAST}.
     */
    FAST(BuWizz2ProtocolConstants.POWER_LEVEL_FAST),

    /**
     * LDCRS (low-distortion current regulation) power level. Value: {@code 4}.
     *
     * <p>Corresponds to {@link BuWizz2ProtocolConstants#POWER_LEVEL_LDCRS}.
     */
    LDCRS(BuWizz2ProtocolConstants.POWER_LEVEL_LDCRS);

    /**
     * The raw power-level byte transmitted in the Set Power Level command payload.
     */
    private final int levelByte;

    /**
     * Constructs a power-level constant.
     *
     * @param levelByte the raw wire byte value for this power level
     */
    BuWizz2PowerLevel(final int levelByte) {
        this.levelByte = levelByte;
    }

    /**
     * Returns the raw power-level byte used in the BuWizz 2.0 Set Power Level
     * ({@code 0x11}) command payload.
     *
     * @return the power level ({@code 0}–{@code 4})
     */
    public int code() {
        return levelByte;
    }
}
