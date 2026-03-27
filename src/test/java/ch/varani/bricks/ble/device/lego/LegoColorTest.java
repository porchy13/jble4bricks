package ch.varani.bricks.ble.device.lego;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LegoColor}.
 */
class LegoColorTest {

    @Test
    void values_hasTwelveConstants() {
        assertEquals(12, LegoColor.values().length);
    }

    @Test
    void valueOf_allConstants_roundTrips() {
        for (final LegoColor color : LegoColor.values()) {
            assertEquals(color, LegoColor.valueOf(color.name()));
        }
    }

    @Test
    void code_black_isZero() {
        assertEquals(LegoProtocolConstants.COLOR_BLACK, LegoColor.BLACK.code());
    }

    @Test
    void code_pink_is1() {
        assertEquals(LegoProtocolConstants.COLOR_PINK, LegoColor.PINK.code());
    }

    @Test
    void code_purple_is2() {
        assertEquals(LegoProtocolConstants.COLOR_PURPLE, LegoColor.PURPLE.code());
    }

    @Test
    void code_blue_is3() {
        assertEquals(LegoProtocolConstants.COLOR_BLUE, LegoColor.BLUE.code());
    }

    @Test
    void code_lightBlue_is4() {
        assertEquals(LegoProtocolConstants.COLOR_LIGHT_BLUE, LegoColor.LIGHT_BLUE.code());
    }

    @Test
    void code_cyan_is5() {
        assertEquals(LegoProtocolConstants.COLOR_CYAN, LegoColor.CYAN.code());
    }

    @Test
    void code_green_is6() {
        assertEquals(LegoProtocolConstants.COLOR_GREEN, LegoColor.GREEN.code());
    }

    @Test
    void code_yellow_is7() {
        assertEquals(LegoProtocolConstants.COLOR_YELLOW, LegoColor.YELLOW.code());
    }

    @Test
    void code_orange_is8() {
        assertEquals(LegoProtocolConstants.COLOR_ORANGE, LegoColor.ORANGE.code());
    }

    @Test
    void code_red_is9() {
        assertEquals(LegoProtocolConstants.COLOR_RED, LegoColor.RED.code());
    }

    @Test
    void code_white_is10() {
        assertEquals(LegoProtocolConstants.COLOR_WHITE, LegoColor.WHITE.code());
    }

    @Test
    void code_none_is255() {
        assertEquals(LegoProtocolConstants.COLOR_NONE, LegoColor.NONE.code());
    }

    @Test
    void allConstants_codes_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0,   LegoColor.BLACK.code()),
            () -> assertEquals(1,   LegoColor.PINK.code()),
            () -> assertEquals(2,   LegoColor.PURPLE.code()),
            () -> assertEquals(3,   LegoColor.BLUE.code()),
            () -> assertEquals(4,   LegoColor.LIGHT_BLUE.code()),
            () -> assertEquals(5,   LegoColor.CYAN.code()),
            () -> assertEquals(6,   LegoColor.GREEN.code()),
            () -> assertEquals(7,   LegoColor.YELLOW.code()),
            () -> assertEquals(8,   LegoColor.ORANGE.code()),
            () -> assertEquals(9,   LegoColor.RED.code()),
            () -> assertEquals(10,  LegoColor.WHITE.code()),
            () -> assertEquals(255, LegoColor.NONE.code())
        );
    }

    @Test
    void allConstants_areNotNull() {
        for (final LegoColor color : LegoColor.values()) {
            assertNotNull(color);
        }
    }
}
