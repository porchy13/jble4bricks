package ch.varani.bricks.ble.device.sbrick;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SBrickDirection}.
 */
class SBrickDirectionTest {

    @Test
    void values_hasTwoConstants() {
        assertEquals(2, SBrickDirection.values().length);
    }

    @Test
    void valueOf_allConstants_roundTrips() {
        for (final SBrickDirection direction : SBrickDirection.values()) {
            assertEquals(direction, SBrickDirection.valueOf(direction.name()));
        }
    }

    @Test
    void code_clockwise_is0x00() {
        assertEquals(SBrickProtocolConstants.DIRECTION_CLOCKWISE,
                SBrickDirection.CLOCKWISE.code());
    }

    @Test
    void code_counterClockwise_is0x01() {
        assertEquals(SBrickProtocolConstants.DIRECTION_COUNTER_CLOCKWISE,
                SBrickDirection.COUNTER_CLOCKWISE.code());
    }

    @Test
    void allConstants_codes_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x00, SBrickDirection.CLOCKWISE.code()),
            () -> assertEquals(0x01, SBrickDirection.COUNTER_CLOCKWISE.code())
        );
    }

    @Test
    void allConstants_areNotNull() {
        for (final SBrickDirection direction : SBrickDirection.values()) {
            assertNotNull(direction);
        }
    }
}
