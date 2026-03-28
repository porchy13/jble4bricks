package ch.varani.bricks.ble.device.lego;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WeDo2Port}.
 */
class WeDo2PortTest {

    @Test
    void values_hasTwoConstants() {
        assertEquals(2, WeDo2Port.values().length);
    }

    @Test
    void valueOf_allConstants_roundTrips() {
        for (final WeDo2Port port : WeDo2Port.values()) {
            assertEquals(port, WeDo2Port.valueOf(port.name()));
        }
    }

    @Test
    void code_portA_is0x01() {
        assertEquals(LegoProtocolConstants.WEDO2_PORT_A, WeDo2Port.A.code());
    }

    @Test
    void code_portB_is0x02() {
        assertEquals(LegoProtocolConstants.WEDO2_PORT_B, WeDo2Port.B.code());
    }

    @Test
    void allConstants_codes_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x01, WeDo2Port.A.code()),
            () -> assertEquals(0x02, WeDo2Port.B.code())
        );
    }

    @Test
    void allConstants_areNotNull() {
        for (final WeDo2Port port : WeDo2Port.values()) {
            assertNotNull(port);
        }
    }
}
