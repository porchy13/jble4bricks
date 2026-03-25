package ch.varani.bricks.ble.api.dsl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.Arrays;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ch.varani.bricks.ble.api.BleConnection;
import ch.varani.bricks.ble.api.BleException;
import ch.varani.bricks.ble.device.buwizz.BuWizz3ProtocolConstants;

/**
 * Unit tests for {@link BuWizz3Dsl}.
 */
class BuWizz3DslTest {

    private BleConnection connection;
    private BuWizz3Dsl dsl;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        connection = mock(BleConnection.class);
        when(connection.writeWithoutResponse(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(connection.notifications(any(), any())).thenReturn(mock(Publisher.class));
        dsl = new BuWizz3Dsl(connection);
    }

    // =========================================================================
    // notifications
    // =========================================================================

    @Test
    @SuppressWarnings("unchecked")
    void notifications_returnsPublisherFromConnection() {
        final Publisher<byte[]> publisher = mock(Publisher.class);
        when(connection.notifications(
                BuWizz3ProtocolConstants.APPLICATION_SERVICE_UUID,
                BuWizz3ProtocolConstants.APPLICATION_CHARACTERISTIC_UUID))
                .thenReturn(publisher);

        assertSame(publisher, dsl.notifications());
    }

    // =========================================================================
    // setMotorData — brake flag combinations
    // =========================================================================

    @Test
    void setMotorData_noBrakeFlags_sendsCorrectBytes() {
        dsl.setMotorData(10, 20, 30, 40, 50, 60, false, false, false, false, false, false);

        final byte[] expected = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_MOTOR_DATA,
            10, 20, 30, 40, 50, 60, 0x00, 0x00
        };
        verifyWrite(expected);
    }

    @Test
    void setMotorData_allBrakeFlags_sendsCorrectBytes() {
        dsl.setMotorData(0, 0, 0, 0, 0, 0, true, true, true, true, true, true);

        // 0x01 | 0x02 | 0x04 | 0x08 | 0x10 | 0x20 = 0x3F
        final byte[] expected = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_MOTOR_DATA,
            0, 0, 0, 0, 0, 0, 0x3F, 0x00
        };
        verifyWrite(expected);
    }

    @Test
    void setMotorData_brake1Only_setsCorrectBit() {
        dsl.setMotorData(0, 0, 0, 0, 0, 0, true, false, false, false, false, false);

        final byte[] expected = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_MOTOR_DATA,
            0, 0, 0, 0, 0, 0, 0x01, 0x00
        };
        verifyWrite(expected);
    }

    @Test
    void setMotorData_brake2Only_setsCorrectBit() {
        dsl.setMotorData(0, 0, 0, 0, 0, 0, false, true, false, false, false, false);

        final byte[] expected = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_MOTOR_DATA,
            0, 0, 0, 0, 0, 0, 0x02, 0x00
        };
        verifyWrite(expected);
    }

    @Test
    void setMotorData_brake3Only_setsCorrectBit() {
        dsl.setMotorData(0, 0, 0, 0, 0, 0, false, false, true, false, false, false);

        final byte[] expected = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_MOTOR_DATA,
            0, 0, 0, 0, 0, 0, 0x04, 0x00
        };
        verifyWrite(expected);
    }

    @Test
    void setMotorData_brake4Only_setsCorrectBit() {
        dsl.setMotorData(0, 0, 0, 0, 0, 0, false, false, false, true, false, false);

        final byte[] expected = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_MOTOR_DATA,
            0, 0, 0, 0, 0, 0, 0x08, 0x00
        };
        verifyWrite(expected);
    }

    @Test
    void setMotorData_brake5Only_setsCorrectBit() {
        dsl.setMotorData(0, 0, 0, 0, 0, 0, false, false, false, false, true, false);

        final byte[] expected = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_MOTOR_DATA,
            0, 0, 0, 0, 0, 0, 0x10, 0x00
        };
        verifyWrite(expected);
    }

    @Test
    void setMotorData_brake6Only_setsCorrectBit() {
        dsl.setMotorData(0, 0, 0, 0, 0, 0, false, false, false, false, false, true);

        final byte[] expected = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_MOTOR_DATA,
            0, 0, 0, 0, 0, 0, 0x20, 0x00
        };
        verifyWrite(expected);
    }

    @Test
    void setMotorData_fullReverseAndForward_encodesCorrectSpeeds() {
        dsl.setMotorData(
                BuWizz3ProtocolConstants.MOTOR_SPEED_FULL_REVERSE,
                BuWizz3ProtocolConstants.MOTOR_SPEED_STOP,
                BuWizz3ProtocolConstants.MOTOR_SPEED_FULL_FORWARD,
                BuWizz3ProtocolConstants.MOTOR_SPEED_STOP,
                BuWizz3ProtocolConstants.MOTOR_SPEED_FULL_REVERSE,
                BuWizz3ProtocolConstants.MOTOR_SPEED_FULL_FORWARD,
                false, false, false, false, false, false);

        final byte[] expected = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_MOTOR_DATA,
            (byte) BuWizz3ProtocolConstants.MOTOR_SPEED_FULL_REVERSE,
            (byte) BuWizz3ProtocolConstants.MOTOR_SPEED_STOP,
            (byte) BuWizz3ProtocolConstants.MOTOR_SPEED_FULL_FORWARD,
            (byte) BuWizz3ProtocolConstants.MOTOR_SPEED_STOP,
            (byte) BuWizz3ProtocolConstants.MOTOR_SPEED_FULL_REVERSE,
            (byte) BuWizz3ProtocolConstants.MOTOR_SPEED_FULL_FORWARD,
            0x00, 0x00
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // stopAllMotors
    // =========================================================================

    @Test
    void stopAllMotors_sendsZeroSpeedsNoBrake() {
        dsl.stopAllMotors();

        final byte[] expected = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_MOTOR_DATA,
            0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // setDataTransferPeriod
    // =========================================================================

    @Test
    void setDataTransferPeriod_sendsCorrectBytes() {
        dsl.setDataTransferPeriod(50);

        final byte[] expected = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_DATA_PERIOD,
            50
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // setMotorTimeout
    // =========================================================================

    @Test
    void setMotorTimeout_sendsCorrectBytes() {
        dsl.setMotorTimeout(1);

        final byte[] expected = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_MOTOR_TIMEOUT,
            0x01
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // setWatchdog
    // =========================================================================

    @Test
    void setWatchdog_nonZero_sendsCorrectBytes() {
        dsl.setWatchdog(10);

        final byte[] expected = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_WATCHDOG,
            0x0A
        };
        verifyWrite(expected);
    }

    @Test
    void setWatchdog_zero_disablesWatchdog() {
        dsl.setWatchdog(0);

        final byte[] expected = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_WATCHDOG,
            0x00
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // setAllLeds
    // =========================================================================

    @Test
    void setAllLeds_sendsCorrectBytes() {
        dsl.setAllLeds(
                0xFF, 0x00, 0x00,
                0x00, 0xFF, 0x00,
                0x00, 0x00, 0xFF,
                0xFF, 0xFF, 0xFF);

        final byte[] expected = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_LED_STATUS,
            (byte) 0xFF, 0x00, 0x00,
            0x00, (byte) 0xFF, 0x00,
            0x00, 0x00, (byte) 0xFF,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // setLedsUniform
    // =========================================================================

    @Test
    void setLedsUniform_setsAllLedsToSameColour() {
        dsl.setLedsUniform(0x80, 0x40, 0x20);

        final byte[] expected = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_LED_STATUS,
            (byte) 0x80, 0x40, 0x20,
            (byte) 0x80, 0x40, 0x20,
            (byte) 0x80, 0x40, 0x20,
            (byte) 0x80, 0x40, 0x20
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // resetLeds
    // =========================================================================

    @Test
    void resetLeds_sendsCommandByteOnly() {
        dsl.resetLeds();

        final byte[] expected = {(byte) BuWizz3ProtocolConstants.CMD_SET_LED_STATUS};
        verifyWrite(expected);
    }

    // =========================================================================
    // setCurrentLimits
    // =========================================================================

    @Test
    void setCurrentLimits_sendsCorrectBytes() {
        dsl.setCurrentLimits(50, 50, 50, 50, 100, 100);

        final byte[] expected = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_CURRENT_LIMITS,
            50, 50, 50, 50, 100, 100
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // setDeviceName
    // =========================================================================

    @Test
    void setDeviceName_shortName_sendsNamePaddedWithNuls() {
        dsl.setDeviceName("Test");

        final ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(connection).writeWithoutResponse(
                eq(BuWizz3ProtocolConstants.APPLICATION_SERVICE_UUID),
                eq(BuWizz3ProtocolConstants.APPLICATION_CHARACTERISTIC_UUID),
                captor.capture());
        final byte[] payload = captor.getValue();
        // Total = 1 (cmd) + 12 (name field) = 13 bytes
        assertArrayEquals(new byte[]{(byte) BuWizz3ProtocolConstants.CMD_SET_DEVICE_NAME},
                Arrays.copyOfRange(payload, 0, 1));
        final byte[] nameBytes = "Test".getBytes(StandardCharsets.US_ASCII);
        assertArrayEquals(nameBytes, Arrays.copyOfRange(payload, 1, 1 + nameBytes.length));
    }

    @Test
    void setDeviceName_exactlyMaxLength_doesNotTruncate() {
        final String name = "123456789012"; // exactly 12 chars
        dsl.setDeviceName(name);

        final ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(connection).writeWithoutResponse(
                eq(BuWizz3ProtocolConstants.APPLICATION_SERVICE_UUID),
                eq(BuWizz3ProtocolConstants.APPLICATION_CHARACTERISTIC_UUID),
                captor.capture());
        final byte[] payload = captor.getValue();
        assertArrayEquals(
                name.getBytes(StandardCharsets.US_ASCII),
                Arrays.copyOfRange(payload, 1, 13));
    }

    @Test
    void setDeviceName_longerThanMax_truncatesTo12Chars() {
        final String name = "1234567890123456"; // 16 chars — only first 12 should appear
        dsl.setDeviceName(name);

        final ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(connection).writeWithoutResponse(
                eq(BuWizz3ProtocolConstants.APPLICATION_SERVICE_UUID),
                eq(BuWizz3ProtocolConstants.APPLICATION_CHARACTERISTIC_UUID),
                captor.capture());
        final byte[] payload = captor.getValue();
        assertArrayEquals(
                "123456789012".getBytes(StandardCharsets.US_ASCII),
                Arrays.copyOfRange(payload, 1, 13));
    }

    // =========================================================================
    // setPuPortFunctions
    // =========================================================================

    @Test
    void setPuPortFunctions_sendsCorrectBytes() {
        dsl.setPuPortFunctions(
                BuWizz3ProtocolConstants.PU_FUNCTION_PU_SIMPLE_PWM,
                BuWizz3ProtocolConstants.PU_FUNCTION_PU_SPEED_SERVO,
                BuWizz3ProtocolConstants.PU_FUNCTION_PU_POSITION_SERVO,
                BuWizz3ProtocolConstants.PU_FUNCTION_PU_ABS_POSITION_SERVO);

        final byte[] expected = {
            (byte) BuWizz3ProtocolConstants.CMD_SET_PU_PORT_FUNCTION,
            (byte) BuWizz3ProtocolConstants.PU_FUNCTION_PU_SIMPLE_PWM,
            (byte) BuWizz3ProtocolConstants.PU_FUNCTION_PU_SPEED_SERVO,
            (byte) BuWizz3ProtocolConstants.PU_FUNCTION_PU_POSITION_SERVO,
            (byte) BuWizz3ProtocolConstants.PU_FUNCTION_PU_ABS_POSITION_SERVO
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // activateShelfMode
    // =========================================================================

    @Test
    void activateShelfMode_sendsCommandByteOnly() {
        dsl.activateShelfMode();

        final byte[] expected = {(byte) BuWizz3ProtocolConstants.CMD_ACTIVATE_SHELF_MODE};
        verifyWrite(expected);
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

    private void verifyWrite(byte[] expected) {
        final ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(connection).writeWithoutResponse(
                eq(BuWizz3ProtocolConstants.APPLICATION_SERVICE_UUID),
                eq(BuWizz3ProtocolConstants.APPLICATION_CHARACTERISTIC_UUID),
                captor.capture());
        assertArrayEquals(expected, captor.getValue());
    }
}
