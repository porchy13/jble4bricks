package ch.varani.bricks.ble.device.lego;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WeDo2LedColor}.
 */
class WeDo2LedColorTest {

    @Test
    void values_hasElevenConstants() {
        assertEquals(11, WeDo2LedColor.values().length);
    }

    @Test
    void valueOf_allConstants_roundTrips() {
        for (final WeDo2LedColor color : WeDo2LedColor.values()) {
            assertEquals(color, WeDo2LedColor.valueOf(color.name()));
        }
    }

    @Test
    void code_black_isZero() {
        assertEquals(LegoProtocolConstants.WEDO2_LED_COLOR_BLACK, WeDo2LedColor.BLACK.code());
    }

    @Test
    void code_pink_is0x01() {
        assertEquals(LegoProtocolConstants.WEDO2_LED_COLOR_PINK, WeDo2LedColor.PINK.code());
    }

    @Test
    void code_purple_is0x02() {
        assertEquals(LegoProtocolConstants.WEDO2_LED_COLOR_PURPLE, WeDo2LedColor.PURPLE.code());
    }

    @Test
    void code_blue_is0x03() {
        assertEquals(LegoProtocolConstants.WEDO2_LED_COLOR_BLUE, WeDo2LedColor.BLUE.code());
    }

    @Test
    void code_lightBlue_is0x04() {
        assertEquals(LegoProtocolConstants.WEDO2_LED_COLOR_LIGHT_BLUE,
                WeDo2LedColor.LIGHT_BLUE.code());
    }

    @Test
    void code_cyan_is0x05() {
        assertEquals(LegoProtocolConstants.WEDO2_LED_COLOR_CYAN, WeDo2LedColor.CYAN.code());
    }

    @Test
    void code_green_is0x06() {
        assertEquals(LegoProtocolConstants.WEDO2_LED_COLOR_GREEN, WeDo2LedColor.GREEN.code());
    }

    @Test
    void code_yellow_is0x07() {
        assertEquals(LegoProtocolConstants.WEDO2_LED_COLOR_YELLOW, WeDo2LedColor.YELLOW.code());
    }

    @Test
    void code_orange_is0x08() {
        assertEquals(LegoProtocolConstants.WEDO2_LED_COLOR_ORANGE, WeDo2LedColor.ORANGE.code());
    }

    @Test
    void code_red_is0x09() {
        assertEquals(LegoProtocolConstants.WEDO2_LED_COLOR_RED, WeDo2LedColor.RED.code());
    }

    @Test
    void code_white_is0x0A() {
        assertEquals(LegoProtocolConstants.WEDO2_LED_COLOR_WHITE, WeDo2LedColor.WHITE.code());
    }

    @Test
    void allConstants_codes_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x00, WeDo2LedColor.BLACK.code()),
            () -> assertEquals(0x01, WeDo2LedColor.PINK.code()),
            () -> assertEquals(0x02, WeDo2LedColor.PURPLE.code()),
            () -> assertEquals(0x03, WeDo2LedColor.BLUE.code()),
            () -> assertEquals(0x04, WeDo2LedColor.LIGHT_BLUE.code()),
            () -> assertEquals(0x05, WeDo2LedColor.CYAN.code()),
            () -> assertEquals(0x06, WeDo2LedColor.GREEN.code()),
            () -> assertEquals(0x07, WeDo2LedColor.YELLOW.code()),
            () -> assertEquals(0x08, WeDo2LedColor.ORANGE.code()),
            () -> assertEquals(0x09, WeDo2LedColor.RED.code()),
            () -> assertEquals(0x0A, WeDo2LedColor.WHITE.code())
        );
    }

    @Test
    void allConstants_areNotNull() {
        for (final WeDo2LedColor color : WeDo2LedColor.values()) {
            assertNotNull(color);
        }
    }
}
