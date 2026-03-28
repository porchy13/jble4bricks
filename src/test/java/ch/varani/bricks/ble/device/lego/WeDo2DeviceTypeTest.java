package ch.varani.bricks.ble.device.lego;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link WeDo2DeviceType}.
 */
class WeDo2DeviceTypeTest {

    @Test
    void values_hasFourConstants() {
        assertEquals(4, WeDo2DeviceType.values().length);
    }

    @Test
    void valueOf_allConstants_roundTrips() {
        for (final WeDo2DeviceType type : WeDo2DeviceType.values()) {
            assertEquals(type, WeDo2DeviceType.valueOf(type.name()));
        }
    }

    @Test
    void code_motor_is0x01() {
        assertEquals(LegoProtocolConstants.WEDO2_MOTOR_TYPE_ID, WeDo2DeviceType.MOTOR.code());
    }

    @Test
    void code_piezoBuzzer_is0x02() {
        assertEquals(LegoProtocolConstants.WEDO2_PIEZO_TYPE_ID, WeDo2DeviceType.PIEZO_BUZZER.code());
    }

    @Test
    void code_rgbLed_is0x22() {
        assertEquals(LegoProtocolConstants.WEDO2_RGB_LED_TYPE_ID, WeDo2DeviceType.RGB_LED.code());
    }

    @Test
    void code_motionSensor_is0x23() {
        assertEquals(LegoProtocolConstants.WEDO2_MOTION_SENSOR_TYPE_ID,
                WeDo2DeviceType.MOTION_SENSOR.code());
    }

    @Test
    void allConstants_codes_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x01, WeDo2DeviceType.MOTOR.code()),
            () -> assertEquals(0x02, WeDo2DeviceType.PIEZO_BUZZER.code()),
            () -> assertEquals(0x22, WeDo2DeviceType.RGB_LED.code()),
            () -> assertEquals(0x23, WeDo2DeviceType.MOTION_SENSOR.code())
        );
    }

    @Test
    void allConstants_areNotNull() {
        for (final WeDo2DeviceType type : WeDo2DeviceType.values()) {
            assertNotNull(type);
        }
    }
}
