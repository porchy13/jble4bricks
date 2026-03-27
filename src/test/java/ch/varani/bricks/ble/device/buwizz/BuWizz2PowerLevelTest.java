package ch.varani.bricks.ble.device.buwizz;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BuWizz2PowerLevel}.
 */
class BuWizz2PowerLevelTest {

    @Test
    void values_hasFiveConstants() {
        assertEquals(5, BuWizz2PowerLevel.values().length);
    }

    @Test
    void valueOf_allConstants_roundTrips() {
        for (final BuWizz2PowerLevel level : BuWizz2PowerLevel.values()) {
            assertEquals(level, BuWizz2PowerLevel.valueOf(level.name()));
        }
    }

    @Test
    void code_disabled_isZero() {
        assertEquals(BuWizz2ProtocolConstants.POWER_LEVEL_DISABLED,
                BuWizz2PowerLevel.DISABLED.code());
    }

    @Test
    void code_slow_is1() {
        assertEquals(BuWizz2ProtocolConstants.POWER_LEVEL_SLOW,
                BuWizz2PowerLevel.SLOW.code());
    }

    @Test
    void code_normal_is2() {
        assertEquals(BuWizz2ProtocolConstants.POWER_LEVEL_NORMAL,
                BuWizz2PowerLevel.NORMAL.code());
    }

    @Test
    void code_fast_is3() {
        assertEquals(BuWizz2ProtocolConstants.POWER_LEVEL_FAST,
                BuWizz2PowerLevel.FAST.code());
    }

    @Test
    void code_ldcrs_is4() {
        assertEquals(BuWizz2ProtocolConstants.POWER_LEVEL_LDCRS,
                BuWizz2PowerLevel.LDCRS.code());
    }

    @Test
    void allConstants_codes_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0, BuWizz2PowerLevel.DISABLED.code()),
            () -> assertEquals(1, BuWizz2PowerLevel.SLOW.code()),
            () -> assertEquals(2, BuWizz2PowerLevel.NORMAL.code()),
            () -> assertEquals(3, BuWizz2PowerLevel.FAST.code()),
            () -> assertEquals(4, BuWizz2PowerLevel.LDCRS.code())
        );
    }

    @Test
    void allConstants_areNotNull() {
        for (final BuWizz2PowerLevel level : BuWizz2PowerLevel.values()) {
            assertNotNull(level);
        }
    }
}
