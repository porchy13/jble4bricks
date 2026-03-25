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
}
