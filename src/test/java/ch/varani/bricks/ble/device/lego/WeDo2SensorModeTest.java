package ch.varani.bricks.ble.device.lego;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WeDo2SensorMode}.
 */
class WeDo2SensorModeTest {

    @Test
    void values_hasFourConstants() {
        assertEquals(4, WeDo2SensorMode.values().length);
    }

    @Test
    void valueOf_allConstants_roundTrips() {
        for (final WeDo2SensorMode mode : WeDo2SensorMode.values()) {
            assertEquals(mode, WeDo2SensorMode.valueOf(mode.name()));
        }
    }

    @Test
    void code_motionDistance_isZero() {
        assertEquals(0, WeDo2SensorMode.MOTION_DISTANCE.code());
    }

    @Test
    void code_motionDetect_isOne() {
        assertEquals(1, WeDo2SensorMode.MOTION_DETECT.code());
    }

    @Test
    void code_ledIndex_isOne() {
        assertEquals(1, WeDo2SensorMode.LED_INDEX.code());
    }

    @Test
    void code_default_isZero() {
        assertEquals(0, WeDo2SensorMode.DEFAULT.code());
    }

    @Test
    void allConstants_codes_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0, WeDo2SensorMode.MOTION_DISTANCE.code()),
            () -> assertEquals(1, WeDo2SensorMode.MOTION_DETECT.code()),
            () -> assertEquals(1, WeDo2SensorMode.LED_INDEX.code()),
            () -> assertEquals(0, WeDo2SensorMode.DEFAULT.code())
        );
    }

    @Test
    void allConstants_areNotNull() {
        for (final WeDo2SensorMode mode : WeDo2SensorMode.values()) {
            assertNotNull(mode);
        }
    }
}
