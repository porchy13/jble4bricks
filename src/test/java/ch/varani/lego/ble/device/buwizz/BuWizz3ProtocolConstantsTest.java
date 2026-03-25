package ch.varani.lego.ble.device.buwizz;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BuWizz3ProtocolConstants}.
 */
class BuWizz3ProtocolConstantsTest {

    @Test
    void constructor_isPrivate_throwsAssertionError() throws Exception {
        final Constructor<BuWizz3ProtocolConstants> ctor =
                BuWizz3ProtocolConstants.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final InvocationTargetException ex =
                assertThrows(InvocationTargetException.class, ctor::newInstance);
        assertThrows(AssertionError.class, () -> {
            throw ex.getCause();
        });
    }

    @Test
    void deviceName_isCorrect() {
        assertEquals("BuWizz3", BuWizz3ProtocolConstants.DEVICE_NAME);
    }

    @Test
    void statusReport_commandIsOne() {
        assertEquals(0x01, BuWizz3ProtocolConstants.CMD_STATUS_REPORT);
    }

    @Test
    void commands_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x30, BuWizz3ProtocolConstants.CMD_SET_MOTOR_DATA),
            () -> assertEquals(0x31, BuWizz3ProtocolConstants.CMD_SET_MOTOR_DATA_EXTENDED),
            () -> assertEquals(0x35, BuWizz3ProtocolConstants.CMD_SET_WATCHDOG),
            () -> assertEquals(0x36, BuWizz3ProtocolConstants.CMD_SET_LED_STATUS),
            () -> assertEquals(0xA1, BuWizz3ProtocolConstants.CMD_ACTIVATE_SHELF_MODE),
            () -> assertEquals(0xAC, BuWizz3ProtocolConstants.CMD_CHECK_CHARGER_SETTINGS)
        );
    }

    @Test
    void puPortFunctions_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x00, BuWizz3ProtocolConstants.PU_FUNCTION_GENERIC_PWM),
            () -> assertEquals(0x10, BuWizz3ProtocolConstants.PU_FUNCTION_PU_SIMPLE_PWM),
            () -> assertEquals(0x14, BuWizz3ProtocolConstants.PU_FUNCTION_PU_SPEED_SERVO),
            () -> assertEquals(0x15, BuWizz3ProtocolConstants.PU_FUNCTION_PU_POSITION_SERVO),
            () -> assertEquals(0x16, BuWizz3ProtocolConstants.PU_FUNCTION_PU_ABS_POSITION_SERVO)
        );
    }

    @Test
    void batteryVoltageFormula_baseAndStep_haveExpectedValues() {
        assertAll(
            () -> assertEquals(9.00, BuWizz3ProtocolConstants.BATTERY_VOLTAGE_BASE, 0.0001),
            () -> assertEquals(0.05, BuWizz3ProtocolConstants.BATTERY_VOLTAGE_STEP, 0.0001)
        );
    }

    @Test
    void pidSampleRate_isOneHundredHz() {
        assertAll(
            () -> assertEquals(100, BuWizz3ProtocolConstants.PID_SAMPLE_RATE_HZ),
            () -> assertEquals(0.01, BuWizz3ProtocolConstants.PID_SAMPLE_PERIOD_S, 0.0001)
        );
    }
}
