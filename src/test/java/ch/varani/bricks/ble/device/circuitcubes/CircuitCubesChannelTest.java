package ch.varani.bricks.ble.device.circuitcubes;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CircuitCubesChannel}.
 */
class CircuitCubesChannelTest {

    @Test
    void values_hasThreeConstants() {
        assertEquals(3, CircuitCubesChannel.values().length);
    }

    @Test
    void valueOf_allConstants_roundTrips() {
        for (final CircuitCubesChannel channel : CircuitCubesChannel.values()) {
            assertEquals(channel, CircuitCubesChannel.valueOf(channel.name()));
        }
    }

    @Test
    void code_channelA_isLowercaseA() {
        assertEquals(CircuitCubesProtocolConstants.CHANNEL_A, CircuitCubesChannel.A.code());
    }

    @Test
    void code_channelB_isLowercaseB() {
        assertEquals(CircuitCubesProtocolConstants.CHANNEL_B, CircuitCubesChannel.B.code());
    }

    @Test
    void code_channelC_isLowercaseC() {
        assertEquals(CircuitCubesProtocolConstants.CHANNEL_C, CircuitCubesChannel.C.code());
    }

    @Test
    void allConstants_codes_haveExpectedValues() {
        assertAll(
            () -> assertEquals('a', CircuitCubesChannel.A.code()),
            () -> assertEquals('b', CircuitCubesChannel.B.code()),
            () -> assertEquals('c', CircuitCubesChannel.C.code())
        );
    }

    @Test
    void allConstants_areNotNull() {
        for (final CircuitCubesChannel channel : CircuitCubesChannel.values()) {
            assertNotNull(channel);
        }
    }
}
