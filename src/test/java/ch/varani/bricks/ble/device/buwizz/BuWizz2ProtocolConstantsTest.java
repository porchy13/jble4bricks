package ch.varani.bricks.ble.device.buwizz;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BuWizz2ProtocolConstants}.
 */
class BuWizz2ProtocolConstantsTest {

    @Test
    void constructor_isPrivate_throwsAssertionError() throws Exception {
        final Constructor<BuWizz2ProtocolConstants> ctor =
                BuWizz2ProtocolConstants.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final InvocationTargetException ex =
                assertThrows(InvocationTargetException.class, ctor::newInstance);
        assertThrows(AssertionError.class, () -> {
            throw ex.getCause();
        });
    }

    @Test
    void deviceName_isCorrect() {
        assertEquals("BuWizz", BuWizz2ProtocolConstants.DEVICE_NAME);
    }

    @Test
    void commands_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x00, BuWizz2ProtocolConstants.CMD_STATUS_REPORT),
            () -> assertEquals(0x10, BuWizz2ProtocolConstants.CMD_SET_MOTOR_DATA),
            () -> assertEquals(0x11, BuWizz2ProtocolConstants.CMD_SET_POWER_LEVEL),
            () -> assertEquals(0x20, BuWizz2ProtocolConstants.CMD_SET_CURRENT_LIMITS)
        );
    }

    @Test
    void motorSpeedLimits_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x81, BuWizz2ProtocolConstants.MOTOR_SPEED_FULL_REVERSE),
            () -> assertEquals(0x00, BuWizz2ProtocolConstants.MOTOR_SPEED_STOP),
            () -> assertEquals(0x7F, BuWizz2ProtocolConstants.MOTOR_SPEED_FULL_FORWARD)
        );
    }

    @Test
    void batteryVoltageFormula_baseAndStep_haveExpectedValues() {
        assertAll(
            () -> assertEquals(3.00, BuWizz2ProtocolConstants.BATTERY_VOLTAGE_BASE, 0.0001),
            () -> assertEquals(0.01, BuWizz2ProtocolConstants.BATTERY_VOLTAGE_STEP, 0.0001)
        );
    }

    @Test
    void powerLevels_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0, BuWizz2ProtocolConstants.POWER_LEVEL_DISABLED),
            () -> assertEquals(1, BuWizz2ProtocolConstants.POWER_LEVEL_SLOW),
            () -> assertEquals(2, BuWizz2ProtocolConstants.POWER_LEVEL_NORMAL),
            () -> assertEquals(3, BuWizz2ProtocolConstants.POWER_LEVEL_FAST),
            () -> assertEquals(4, BuWizz2ProtocolConstants.POWER_LEVEL_LDCRS)
        );
    }
}
