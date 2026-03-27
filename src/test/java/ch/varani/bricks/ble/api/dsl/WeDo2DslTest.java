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
                LegoProtocolConstants.WEDO2_SERVICE_UUID,
                LegoProtocolConstants.WEDO2_SENSOR_VALUE_UUID))
                .thenReturn(publisher);

        assertSame(publisher, dsl.sensorNotifications());
    }

    // =========================================================================
    // motorPower
    // =========================================================================

    @Test
    void motorPower_portA_forwardPower_sendsCorrectBytes() {
        dsl.motorPower(LegoProtocolConstants.WEDO2_PORT_A, 75);

        final byte[] expected = {
            (byte) LegoProtocolConstants.WEDO2_PORT_A,
            (byte) LegoProtocolConstants.WEDO2_MOTOR_TYPE_ID,
            (byte) 0x02,
            (byte) 75
        };
        verifyMotorWrite(expected);
    }

    @Test
    void motorPower_portB_reversePower_sendsCorrectBytes() {
        dsl.motorPower(LegoProtocolConstants.WEDO2_PORT_B, -50);

        final byte[] expected = {
            (byte) LegoProtocolConstants.WEDO2_PORT_B,
            (byte) LegoProtocolConstants.WEDO2_MOTOR_TYPE_ID,
            (byte) 0x02,
            (byte) (-50 & 0xFF)
        };
        verifyMotorWrite(expected);
    }

    @Test
    void motorPower_zeroPower_sendsStopBytes() {
        dsl.motorPower(LegoProtocolConstants.WEDO2_PORT_A, 0);

        final byte[] expected = {
            (byte) LegoProtocolConstants.WEDO2_PORT_A,
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
        dsl.stopMotor(LegoProtocolConstants.WEDO2_PORT_A);

        final byte[] expected = {
            (byte) LegoProtocolConstants.WEDO2_PORT_A,
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
        dsl.setLedColor(LegoProtocolConstants.WEDO2_LED_COLOR_RED);

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
            (byte) LegoProtocolConstants.WEDO2_LED_COLOR_RED
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
    void setLedColor_greenIndex_sendsGreenIndexByte() {
        dsl.setLedColor(LegoProtocolConstants.WEDO2_LED_COLOR_GREEN);

        final byte[] expectedIdx = {
            (byte) LegoProtocolConstants.WEDO2_PORT_LED,
            (byte) LegoProtocolConstants.WEDO2_LED_IDX_CMD_B1,
            (byte) LegoProtocolConstants.WEDO2_LED_IDX_CMD_B2,
            (byte) LegoProtocolConstants.WEDO2_LED_COLOR_GREEN
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
    void subscribeSensor_sendsCorrectBytes() {
        dsl.subscribeSensor(
                LegoProtocolConstants.WEDO2_PORT_A,
                LegoProtocolConstants.WEDO2_MOTION_SENSOR_TYPE_ID,
                0);

        final byte[] expected = {
            0x01, 0x02,
            (byte) LegoProtocolConstants.WEDO2_PORT_A,
            (byte) LegoProtocolConstants.WEDO2_MOTION_SENSOR_TYPE_ID,
            (byte) 0,
            0x01, 0x00, 0x00, 0x00, 0x00,
            0x01
        };
        verifyPortTypeWrite(expected);
    }

    @Test
    void unsubscribeSensor_sendsCorrectBytes() {
        dsl.unsubscribeSensor(
                LegoProtocolConstants.WEDO2_PORT_B,
                LegoProtocolConstants.WEDO2_MOTION_SENSOR_TYPE_ID,
                0);

        final byte[] expected = {
            0x01, 0x02,
            (byte) LegoProtocolConstants.WEDO2_PORT_B,
            (byte) LegoProtocolConstants.WEDO2_MOTION_SENSOR_TYPE_ID,
            (byte) 0,
            0x01, 0x00, 0x00, 0x00, 0x00,
            0x00
        };
        verifyPortTypeWrite(expected);
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
