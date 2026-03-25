package ch.varani.bricks.ble.device.lego;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LegoHubType}.
 */
class LegoHubTypeTest {

    @Test
    void values_hasSevenConstants() {
        assertEquals(7, LegoHubType.values().length);
    }

    @Test
    void valueOf_allConstants_roundTrips() {
        for (final LegoHubType type : LegoHubType.values()) {
            assertEquals(type, LegoHubType.valueOf(type.name()));
        }
    }

    @Test
    void systemTypeDeviceByte_wedo2Hub_isZero() {
        assertEquals(LegoProtocolConstants.DEVICE_WEDO2_HUB,
                LegoHubType.WEDO2_HUB.systemTypeDeviceByte());
    }

    @Test
    void systemTypeDeviceByte_dupleTrain_is0x20() {
        assertEquals(LegoProtocolConstants.DEVICE_DUPLO_TRAIN,
                LegoHubType.DUPLO_TRAIN.systemTypeDeviceByte());
    }

    @Test
    void systemTypeDeviceByte_boostMoveHub_is0x40() {
        assertEquals(LegoProtocolConstants.DEVICE_BOOST_HUB,
                LegoHubType.BOOST_MOVE_HUB.systemTypeDeviceByte());
    }

    @Test
    void systemTypeDeviceByte_cityHub_is0x41() {
        assertEquals(LegoProtocolConstants.DEVICE_2PORT_HUB,
                LegoHubType.CITY_HUB.systemTypeDeviceByte());
    }

    @Test
    void systemTypeDeviceByte_handset2Port_is0x42() {
        assertEquals(LegoProtocolConstants.DEVICE_2PORT_HANDSET,
                LegoHubType.HANDSET_2PORT.systemTypeDeviceByte());
    }

    @Test
    void systemTypeDeviceByte_technicHub_is0x50() {
        assertEquals(LegoProtocolConstants.DEVICE_TECHNIC_HUB,
                LegoHubType.TECHNIC_HUB.systemTypeDeviceByte());
    }

    @Test
    void systemTypeDeviceByte_marioHub_is0x60() {
        assertEquals(LegoProtocolConstants.DEVICE_MARIO_HUB,
                LegoHubType.MARIO_HUB.systemTypeDeviceByte());
    }

    @Test
    void allConstants_systemTypeDeviceBytes_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x00, LegoHubType.WEDO2_HUB.systemTypeDeviceByte()),
            () -> assertEquals(0x20, LegoHubType.DUPLO_TRAIN.systemTypeDeviceByte()),
            () -> assertEquals(0x40, LegoHubType.BOOST_MOVE_HUB.systemTypeDeviceByte()),
            () -> assertEquals(0x41, LegoHubType.CITY_HUB.systemTypeDeviceByte()),
            () -> assertEquals(0x42, LegoHubType.HANDSET_2PORT.systemTypeDeviceByte()),
            () -> assertEquals(0x50, LegoHubType.TECHNIC_HUB.systemTypeDeviceByte()),
            () -> assertEquals(0x60, LegoHubType.MARIO_HUB.systemTypeDeviceByte())
        );
    }

    @Test
    void allConstants_areNotNull() {
        for (final LegoHubType type : LegoHubType.values()) {
            assertNotNull(type);
        }
    }
}
