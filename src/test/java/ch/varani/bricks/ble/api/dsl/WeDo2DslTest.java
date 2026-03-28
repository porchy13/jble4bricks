package ch.varani.bricks.ble.api.dsl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ch.varani.bricks.ble.api.BleConnection;
import ch.varani.bricks.ble.api.BleException;
import ch.varani.bricks.ble.device.lego.LegoProtocolConstants;
import ch.varani.bricks.ble.device.lego.WeDo2DeviceType;
import ch.varani.bricks.ble.device.lego.WeDo2LedColor;
import ch.varani.bricks.ble.device.lego.WeDo2Port;
import ch.varani.bricks.ble.device.lego.WeDo2SensorMode;

/**
 * Unit tests for {@link WeDo2Dsl}.
 */
class WeDo2DslTest {

    private BleConnection connection;
    private WeDo2Dsl dsl;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        connection = mock(BleConnection.class);
        when(connection.writeWithoutResponse(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(connection.notifications(any(), any())).thenReturn(mock(Publisher.class));
        dsl = new WeDo2Dsl(connection);
    }

    // =========================================================================
    // notifications
    // =========================================================================

    @Test
    @SuppressWarnings("unchecked")
    void notifications_returnsPublisherFromConnection() {
        final Publisher<byte[]> publisher = mock(Publisher.class);
        when(connection.notifications(
                LegoProtocolConstants.WEDO2_SERVICE_UUID,
                LegoProtocolConstants.WEDO2_BUTTON_UUID))
                .thenReturn(publisher);

        assertSame(publisher, dsl.notifications());
    }

    @Test
    @SuppressWarnings("unchecked")
    void sensorNotifications_returnsPublisherFromConnection() {
        final Publisher<byte[]> publisher = mock(Publisher.class);
        when(connection.notifications(
                LegoProtocolConstants.WEDO2_SERVICE_2_UUID,
                LegoProtocolConstants.WEDO2_SENSOR_VALUE_UUID))
                .thenReturn(publisher);

        assertSame(publisher, dsl.sensorNotifications());
    }

    // =========================================================================
    // motorPower
    // =========================================================================

    @Test
    void motorPower_portA_forwardPower_sendsCorrectBytes() {
        dsl.motorPower(WeDo2Port.A, 75);

        final byte[] expected = {
            (byte) WeDo2Port.A.code(),
            (byte) LegoProtocolConstants.WEDO2_MOTOR_TYPE_ID,
            (byte) 0x02,
            (byte) 75
        };
        verifyMotorWrite(expected);
    }

    @Test
    void motorPower_portB_reversePower_sendsCorrectBytes() {
        dsl.motorPower(WeDo2Port.B, -50);

        final byte[] expected = {
            (byte) WeDo2Port.B.code(),
            (byte) LegoProtocolConstants.WEDO2_MOTOR_TYPE_ID,
            (byte) 0x02,
            (byte) (-50 & 0xFF)
        };
        verifyMotorWrite(expected);
    }

    @Test
    void motorPower_zeroPower_sendsStopBytes() {
        dsl.motorPower(WeDo2Port.A, 0);

        final byte[] expected = {
            (byte) WeDo2Port.A.code(),
            (byte) LegoProtocolConstants.WEDO2_MOTOR_TYPE_ID,
            (byte) 0x02,
            (byte) 0
        };
        verifyMotorWrite(expected);
    }

    // =========================================================================
    // stopMotor
    // =========================================================================

    @Test
    void stopMotor_delegatesToMotorPowerZero() {
        dsl.stopMotor(WeDo2Port.A);

        final byte[] expected = {
            (byte) WeDo2Port.A.code(),
            (byte) LegoProtocolConstants.WEDO2_MOTOR_TYPE_ID,
            (byte) 0x02,
            (byte) 0
        };
        verifyMotorWrite(expected);
    }

    // =========================================================================
    // setLedColor
    // =========================================================================

    @Test
    void setLedColor_sendsModeCmdThenIndexCmd() {
        dsl.setLedColor(WeDo2LedColor.RED);

        final byte[] expectedMode = {
            (byte) LegoProtocolConstants.WEDO2_PORT_LED,
            (byte) LegoProtocolConstants.WEDO2_LED_MODE_SETUP_B1,
            (byte) LegoProtocolConstants.WEDO2_LED_MODE_SETUP_B2,
            (byte) LegoProtocolConstants.WEDO2_LED_IDX_MODE_SETUP_B3
        };
        final byte[] expectedIdx = {
            (byte) LegoProtocolConstants.WEDO2_PORT_LED,
            (byte) LegoProtocolConstants.WEDO2_LED_IDX_CMD_B1,
            (byte) LegoProtocolConstants.WEDO2_LED_IDX_CMD_B2,
            (byte) WeDo2LedColor.RED.code()
        };

        final ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(connection).writeWithoutResponse(
                eq(LegoProtocolConstants.WEDO2_SERVICE_UUID),
                eq(LegoProtocolConstants.WEDO2_PORT_TYPE_WRITE_UUID),
                captor.capture());
        assertArrayEquals(expectedMode, captor.getValue());

        final ArgumentCaptor<byte[]> captor2 = ArgumentCaptor.forClass(byte[].class);
        verify(connection).writeWithoutResponse(
                eq(LegoProtocolConstants.WEDO2_SERVICE_UUID),
                eq(LegoProtocolConstants.WEDO2_MOTOR_VALUE_WRITE_UUID),
                captor2.capture());
        assertArrayEquals(expectedIdx, captor2.getValue());
    }

    @Test
    void setLedColor_greenColor_sendsGreenIndexByte() {
        dsl.setLedColor(WeDo2LedColor.GREEN);

        final byte[] expectedIdx = {
            (byte) LegoProtocolConstants.WEDO2_PORT_LED,
            (byte) LegoProtocolConstants.WEDO2_LED_IDX_CMD_B1,
            (byte) LegoProtocolConstants.WEDO2_LED_IDX_CMD_B2,
            (byte) WeDo2LedColor.GREEN.code()
        };

        final ArgumentCaptor<byte[]> captor2 = ArgumentCaptor.forClass(byte[].class);
        verify(connection).writeWithoutResponse(
                eq(LegoProtocolConstants.WEDO2_SERVICE_UUID),
                eq(LegoProtocolConstants.WEDO2_MOTOR_VALUE_WRITE_UUID),
                captor2.capture());
        assertArrayEquals(expectedIdx, captor2.getValue());
    }

    // =========================================================================
    // subscribeSensor / unsubscribeSensor
    // =========================================================================

    @Test
    void subscribeSensor_portA_motionSensor_distanceMode_sendsCorrectBytes() {
        dsl.subscribeSensor(WeDo2Port.A, WeDo2DeviceType.MOTION_SENSOR,
                WeDo2SensorMode.MOTION_DISTANCE);

        final byte[] expected = {
            0x01, 0x02,
            (byte) WeDo2Port.A.code(),
            (byte) WeDo2DeviceType.MOTION_SENSOR.code(),
            (byte) WeDo2SensorMode.MOTION_DISTANCE.code(),
            0x01, 0x00, 0x00, 0x00, 0x00,
            0x01
        };
        verifyPortTypeWrite(expected);
    }

    @Test
    void subscribeSensor_portB_motionSensor_defaultMode_sendsCorrectBytes() {
        dsl.subscribeSensor(WeDo2Port.B, WeDo2DeviceType.MOTION_SENSOR,
                WeDo2SensorMode.DEFAULT);

        final byte[] expected = {
            0x01, 0x02,
            (byte) WeDo2Port.B.code(),
            (byte) WeDo2DeviceType.MOTION_SENSOR.code(),
            (byte) WeDo2SensorMode.DEFAULT.code(),
            0x01, 0x00, 0x00, 0x00, 0x00,
            0x01
        };
        verifyPortTypeWrite(expected);
    }

    @Test
    void subscribeSensor_motorDeviceType_encodesCorrectTypeId() {
        dsl.subscribeSensor(WeDo2Port.A, WeDo2DeviceType.MOTOR,
                WeDo2SensorMode.DEFAULT);

        final byte[] expected = {
            0x01, 0x02,
            (byte) WeDo2Port.A.code(),
            (byte) WeDo2DeviceType.MOTOR.code(),
            (byte) WeDo2SensorMode.DEFAULT.code(),
            0x01, 0x00, 0x00, 0x00, 0x00,
            0x01
        };
        verifyPortTypeWrite(expected);
    }

    @Test
    void subscribeSensor_piezoBuzzerDeviceType_encodesCorrectTypeId() {
        dsl.subscribeSensor(WeDo2Port.A, WeDo2DeviceType.PIEZO_BUZZER,
                WeDo2SensorMode.DEFAULT);

        final byte[] expected = {
            0x01, 0x02,
            (byte) WeDo2Port.A.code(),
            (byte) WeDo2DeviceType.PIEZO_BUZZER.code(),
            (byte) WeDo2SensorMode.DEFAULT.code(),
            0x01, 0x00, 0x00, 0x00, 0x00,
            0x01
        };
        verifyPortTypeWrite(expected);
    }

    @Test
    void subscribeSensor_rgbLedDeviceType_indexMode_encodesCorrectBytes() {
        dsl.subscribeSensor(WeDo2Port.A, WeDo2DeviceType.RGB_LED,
                WeDo2SensorMode.LED_INDEX);

        final byte[] expected = {
            0x01, 0x02,
            (byte) WeDo2Port.A.code(),
            (byte) WeDo2DeviceType.RGB_LED.code(),
            (byte) WeDo2SensorMode.LED_INDEX.code(),
            0x01, 0x00, 0x00, 0x00, 0x00,
            0x01
        };
        verifyPortTypeWrite(expected);
    }

    @Test
    void subscribeSensor_motionSensor_detectMode_encodesCorrectModeIndex() {
        dsl.subscribeSensor(WeDo2Port.B, WeDo2DeviceType.MOTION_SENSOR,
                WeDo2SensorMode.MOTION_DETECT);

        final byte[] expected = {
            0x01, 0x02,
            (byte) WeDo2Port.B.code(),
            (byte) WeDo2DeviceType.MOTION_SENSOR.code(),
            (byte) WeDo2SensorMode.MOTION_DETECT.code(),
            0x01, 0x00, 0x00, 0x00, 0x00,
            0x01
        };
        verifyPortTypeWrite(expected);
    }

    @Test
    void unsubscribeSensor_portB_motionSensor_distanceMode_sendsCorrectBytes() {
        dsl.unsubscribeSensor(WeDo2Port.B, WeDo2DeviceType.MOTION_SENSOR,
                WeDo2SensorMode.MOTION_DISTANCE);

        final byte[] expected = {
            0x01, 0x02,
            (byte) WeDo2Port.B.code(),
            (byte) WeDo2DeviceType.MOTION_SENSOR.code(),
            (byte) WeDo2SensorMode.MOTION_DISTANCE.code(),
            0x01, 0x00, 0x00, 0x00, 0x00,
            0x00
        };
        verifyPortTypeWrite(expected);
    }

    @Test
    void unsubscribeSensor_portA_motionSensor_defaultMode_sendsCorrectBytes() {
        dsl.unsubscribeSensor(WeDo2Port.A, WeDo2DeviceType.MOTION_SENSOR,
                WeDo2SensorMode.DEFAULT);

        final byte[] expected = {
            0x01, 0x02,
            (byte) WeDo2Port.A.code(),
            (byte) WeDo2DeviceType.MOTION_SENSOR.code(),
            (byte) WeDo2SensorMode.DEFAULT.code(),
            0x01, 0x00, 0x00, 0x00, 0x00,
            0x00
        };
        verifyPortTypeWrite(expected);
    }

    // =========================================================================
    // playTone
    // =========================================================================

    @Test
    void playTone_validFrequencyAndDuration_sendsCorrectSevenBytePacket() {
        dsl.playTone(440, 1000);

        final byte[] expected = {
            (byte) LegoProtocolConstants.WEDO2_PORT_PIEZO_BUZZER,
            (byte) LegoProtocolConstants.WEDO2_PIEZO_TYPE_ID,
            (byte) LegoProtocolConstants.WEDO2_PIEZO_WRITE_DIRECT_CMD,
            (byte) (440 & 0xFF),
            (byte) ((440 >> 8) & 0xFF),
            (byte) (1000 & 0xFF),
            (byte) ((1000 >> 8) & 0xFF)
        };
        verifyMotorWrite(expected);
    }

    @Test
    void playTone_highFrequency_encodesUint16LittleEndianCorrectly() {
        // 1000 Hz = 0x03E8 → lo=0xE8, hi=0x03 ; 500 ms = 0x01F4 → lo=0xF4, hi=0x01
        dsl.playTone(1000, 500);

        final byte[] expected = {
            (byte) LegoProtocolConstants.WEDO2_PORT_PIEZO_BUZZER,
            (byte) LegoProtocolConstants.WEDO2_PIEZO_TYPE_ID,
            (byte) LegoProtocolConstants.WEDO2_PIEZO_WRITE_DIRECT_CMD,
            (byte) 0xE8, (byte) 0x03,
            (byte) 0xF4, (byte) 0x01
        };
        verifyMotorWrite(expected);
    }

    @Test
    void playTone_zeroFrequencyAndDuration_sendsAllZeroPayloadBytes() {
        dsl.playTone(0, 0);

        final byte[] expected = {
            (byte) LegoProtocolConstants.WEDO2_PORT_PIEZO_BUZZER,
            (byte) LegoProtocolConstants.WEDO2_PIEZO_TYPE_ID,
            (byte) LegoProtocolConstants.WEDO2_PIEZO_WRITE_DIRECT_CMD,
            (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00
        };
        verifyMotorWrite(expected);
    }

    // =========================================================================
    // stopTone
    // =========================================================================

    @Test
    void stopTone_delegatesToPlayToneWithZeroFrequencyAndDuration() {
        dsl.stopTone();

        final byte[] expected = {
            (byte) LegoProtocolConstants.WEDO2_PORT_PIEZO_BUZZER,
            (byte) LegoProtocolConstants.WEDO2_PIEZO_TYPE_ID,
            (byte) LegoProtocolConstants.WEDO2_PIEZO_WRITE_DIRECT_CMD,
            (byte) 0x00, (byte) 0x00,
            (byte) 0x00, (byte) 0x00
        };
        verifyMotorWrite(expected);
    }

    // =========================================================================
    // Lifecycle
    // =========================================================================

    @Test
    void done_closesConnection() throws BleException {
        dsl.done();
        verify(connection).close();
    }

    @Test
    void connection_returnsWrappedConnection() {
        assertSame(connection, dsl.connection());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void verifyMotorWrite(byte[] expected) {
        final ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(connection).writeWithoutResponse(
                eq(LegoProtocolConstants.WEDO2_SERVICE_UUID),
                eq(LegoProtocolConstants.WEDO2_MOTOR_VALUE_WRITE_UUID),
                captor.capture());
        assertArrayEquals(expected, captor.getValue());
    }

    private void verifyPortTypeWrite(byte[] expected) {
        final ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(connection).writeWithoutResponse(
                eq(LegoProtocolConstants.WEDO2_SERVICE_UUID),
                eq(LegoProtocolConstants.WEDO2_PORT_TYPE_WRITE_UUID),
                captor.capture());
        assertArrayEquals(expected, captor.getValue());
    }
}
