package ch.varani.bricks.ble.device.lego;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LegoProtocolConstants}.
 */
class LegoProtocolConstantsTest {

    @Test
    void constructor_isPrivate_throwsAssertionError() throws Exception {
        final Constructor<LegoProtocolConstants> ctor =
                LegoProtocolConstants.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final InvocationTargetException ex =
                assertThrows(InvocationTargetException.class, ctor::newInstance);
        assertThrows(AssertionError.class, () -> {
            throw ex.getCause();
        });
    }

    @Test
    void manufacturerId_hasExpectedValue() {
        assertEquals(0x0397, LegoProtocolConstants.MANUFACTURER_ID);
    }

    @Test
    void hubServiceUuid_hasExpectedValue() {
        assertEquals(
                "00001623-1212-efde-1623-785feabcd123",
                LegoProtocolConstants.HUB_SERVICE_UUID
        );
    }

    @Test
    void hubCharacteristicUuid_hasExpectedValue() {
        assertEquals(
                "00001624-1212-efde-1623-785feabcd123",
                LegoProtocolConstants.HUB_CHARACTERISTIC_UUID
        );
    }

    @Test
    void messageTypes_haveDistinctValues() {
        final int[] types = {
            LegoProtocolConstants.MSG_HUB_PROPERTIES,
            LegoProtocolConstants.MSG_HUB_ACTIONS,
            LegoProtocolConstants.MSG_HUB_ALERTS,
            LegoProtocolConstants.MSG_HUB_ATTACHED_IO,
            LegoProtocolConstants.MSG_GENERIC_ERROR,
            LegoProtocolConstants.MSG_PORT_OUTPUT_COMMAND,
            LegoProtocolConstants.MSG_PORT_OUTPUT_COMMAND_FEEDBACK
        };
        // All values must be in valid byte range
        for (final int type : types) {
            assertTrue(type >= 0x00 && type <= 0xFF,
                    "Message type 0x" + Integer.toHexString(type) + " out of byte range");
        }
    }

    @Test
    void hubId_isZero() {
        assertEquals(0x00, LegoProtocolConstants.HUB_ID);
    }

    @Test
    void motorSubCommands_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x07, LegoProtocolConstants.MOTOR_CMD_START_SPEED),
            () -> assertEquals(0x0B, LegoProtocolConstants.MOTOR_CMD_START_SPEED_FOR_DEGREES),
            () -> assertEquals(0x0D, LegoProtocolConstants.MOTOR_CMD_GOTO_ABSOLUTE_POSITION),
            () -> assertEquals(0x50, LegoProtocolConstants.MOTOR_CMD_WRITE_DIRECT),
            () -> assertEquals(0x51, LegoProtocolConstants.MOTOR_CMD_WRITE_DIRECT_MODE_DATA)
        );
    }

    @Test
    void deviceTypeConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x00, LegoProtocolConstants.DEVICE_WEDO2_HUB),
            () -> assertEquals(0x20, LegoProtocolConstants.DEVICE_DUPLO_TRAIN),
            () -> assertEquals(0x40, LegoProtocolConstants.DEVICE_BOOST_HUB),
            () -> assertEquals(0x41, LegoProtocolConstants.DEVICE_2PORT_HUB),
            () -> assertEquals(0x42, LegoProtocolConstants.DEVICE_2PORT_HANDSET),
            () -> assertEquals(0x50, LegoProtocolConstants.DEVICE_TECHNIC_HUB),
            () -> assertEquals(0x60, LegoProtocolConstants.DEVICE_MARIO_HUB)
        );
    }

    @Test
    void manufacturerDataIndexConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0, LegoProtocolConstants.MANUFACTURER_DATA_IDX_MFR_ID_LSB),
            () -> assertEquals(1, LegoProtocolConstants.MANUFACTURER_DATA_IDX_MFR_ID_MSB),
            () -> assertEquals(3, LegoProtocolConstants.MANUFACTURER_DATA_IDX_SYSTEM_TYPE),
            () -> assertEquals(4, LegoProtocolConstants.MANUFACTURER_DATA_MIN_LENGTH),
            () -> assertEquals(0x97, LegoProtocolConstants.MANUFACTURER_ID_LSB),
            () -> assertEquals(0x03, LegoProtocolConstants.MANUFACTURER_ID_MSB)
        );
    }
}
