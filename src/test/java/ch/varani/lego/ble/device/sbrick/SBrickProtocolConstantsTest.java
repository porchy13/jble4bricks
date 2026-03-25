package ch.varani.lego.ble.device.sbrick;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link SBrickProtocolConstants}.
 */
class SBrickProtocolConstantsTest {

    @Test
    void constructor_isPrivate_throwsAssertionError() throws Exception {
        final Constructor<SBrickProtocolConstants> ctor =
                SBrickProtocolConstants.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final InvocationTargetException ex =
                assertThrows(InvocationTargetException.class, ctor::newInstance);
        assertThrows(AssertionError.class, () -> {
            throw ex.getCause();
        });
    }

    @Test
    void manufacturerId_hasExpectedValue() {
        assertEquals(0x0198, SBrickProtocolConstants.MANUFACTURER_ID);
    }

    @Test
    void remoteControlCommandsUuid_hasExpectedValue() {
        assertEquals(
                "02b8cbcc-0e25-4bda-8790-a15f53e6010f",
                SBrickProtocolConstants.REMOTE_CONTROL_COMMANDS_UUID
        );
    }

    @Test
    void adcChannels_haveExpectedValues() {
        assertEquals(0x08, SBrickProtocolConstants.ADC_CHANNEL_BATTERY);
        assertEquals(0x09, SBrickProtocolConstants.ADC_CHANNEL_TEMPERATURE);
    }

    @Test
    void commands_haveExpectedValues() {
        assertEquals(0x00, SBrickProtocolConstants.CMD_BRAKE);
        assertEquals(0x01, SBrickProtocolConstants.CMD_DRIVE);
        assertEquals(0x0F, SBrickProtocolConstants.CMD_QUERY_ADC);
        assertEquals(0x0D, SBrickProtocolConstants.CMD_SET_WATCHDOG);
        assertEquals(0x0E, SBrickProtocolConstants.CMD_GET_WATCHDOG);
    }

    @Test
    void responseCodes_haveExpectedValues() {
        assertEquals(0x00, SBrickProtocolConstants.RESPONSE_SUCCESS);
        assertEquals(0x05, SBrickProtocolConstants.RESPONSE_AUTH_ERROR);
        assertEquals(0x08, SBrickProtocolConstants.RESPONSE_THERMAL_PROTECTION);
    }
}
