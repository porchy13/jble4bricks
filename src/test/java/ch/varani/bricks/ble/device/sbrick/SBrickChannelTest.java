package ch.varani.bricks.ble.device.sbrick;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SBrickChannel}.
 */
class SBrickChannelTest {

    @Test
    void values_hasFourConstants() {
        assertEquals(4, SBrickChannel.values().length);
    }

    @Test
    void valueOf_allConstants_roundTrips() {
        for (final SBrickChannel channel : SBrickChannel.values()) {
            assertEquals(channel, SBrickChannel.valueOf(channel.name()));
        }
    }

    @Test
    void code_channelA_is0x00() {
        assertEquals(SBrickProtocolConstants.CHANNEL_A, SBrickChannel.A.code());
    }

    @Test
    void code_channelB_is0x02() {
        assertEquals(SBrickProtocolConstants.CHANNEL_B, SBrickChannel.B.code());
    }

    @Test
    void code_channelC_is0x01() {
        assertEquals(SBrickProtocolConstants.CHANNEL_C, SBrickChannel.C.code());
    }

    @Test
    void code_channelD_is0x03() {
        assertEquals(SBrickProtocolConstants.CHANNEL_D, SBrickChannel.D.code());
    }

    @Test
    void allConstants_codes_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x00, SBrickChannel.A.code()),
            () -> assertEquals(0x02, SBrickChannel.B.code()),
            () -> assertEquals(0x01, SBrickChannel.C.code()),
            () -> assertEquals(0x03, SBrickChannel.D.code())
        );
    }

    @Test
    void allConstants_areNotNull() {
        for (final SBrickChannel channel : SBrickChannel.values()) {
            assertNotNull(channel);
        }
    }
}
