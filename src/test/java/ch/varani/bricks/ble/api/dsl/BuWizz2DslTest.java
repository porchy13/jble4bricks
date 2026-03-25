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
import ch.varani.bricks.ble.device.buwizz.BuWizz2ProtocolConstants;

/**
 * Unit tests for {@link BuWizz2Dsl}.
 */
class BuWizz2DslTest {

    private BleConnection connection;
    private BuWizz2Dsl dsl;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        connection = mock(BleConnection.class);
        when(connection.writeWithoutResponse(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(connection.notifications(any(), any())).thenReturn(mock(Publisher.class));
        dsl = new BuWizz2Dsl(connection);
    }

    // =========================================================================
    // notifications
    // =========================================================================

    @Test
    @SuppressWarnings("unchecked")
    void notifications_returnsPublisherFromConnection() {
        final Publisher<byte[]> publisher = mock(Publisher.class);
        when(connection.notifications(
                BuWizz2ProtocolConstants.APPLICATION_SERVICE_UUID,
                BuWizz2ProtocolConstants.APPLICATION_DATA_UUID))
                .thenReturn(publisher);

        assertSame(publisher, dsl.notifications());
    }

    // =========================================================================
    // setMotorData — brake flag combinations
    // =========================================================================

    @Test
    void setMotorData_noBrakeFlags_sendsCorrectBytes() {
        dsl.setMotorData(10, 20, 30, 40, false, false, false, false);

        final byte[] expected = {
            (byte) BuWizz2ProtocolConstants.CMD_SET_MOTOR_DATA,
            10, 20, 30, 40, 0x00
        };
        verifyWrite(expected);
    }

    @Test
    void setMotorData_allBrakeFlags_sendsCorrectBytes() {
        dsl.setMotorData(10, 20, 30, 40, true, true, true, true);

        // 0x01 | 0x02 | 0x04 | 0x08 = 0x0F
        final byte[] expected = {
            (byte) BuWizz2ProtocolConstants.CMD_SET_MOTOR_DATA,
            10, 20, 30, 40, 0x0F
        };
        verifyWrite(expected);
    }

    @Test
    void setMotorData_brake1Only_setsCorrectBit() {
        dsl.setMotorData(0, 0, 0, 0, true, false, false, false);

        final byte[] expected = {
            (byte) BuWizz2ProtocolConstants.CMD_SET_MOTOR_DATA,
            0, 0, 0, 0, 0x01
        };
        verifyWrite(expected);
    }

    @Test
    void setMotorData_brake2Only_setsCorrectBit() {
        dsl.setMotorData(0, 0, 0, 0, false, true, false, false);

        final byte[] expected = {
            (byte) BuWizz2ProtocolConstants.CMD_SET_MOTOR_DATA,
            0, 0, 0, 0, 0x02
        };
        verifyWrite(expected);
    }

    @Test
    void setMotorData_brake3Only_setsCorrectBit() {
        dsl.setMotorData(0, 0, 0, 0, false, false, true, false);

        final byte[] expected = {
            (byte) BuWizz2ProtocolConstants.CMD_SET_MOTOR_DATA,
            0, 0, 0, 0, 0x04
        };
        verifyWrite(expected);
    }

    @Test
    void setMotorData_brake4Only_setsCorrectBit() {
        dsl.setMotorData(0, 0, 0, 0, false, false, false, true);

        final byte[] expected = {
            (byte) BuWizz2ProtocolConstants.CMD_SET_MOTOR_DATA,
            0, 0, 0, 0, 0x08
        };
        verifyWrite(expected);
    }

    @Test
    void setMotorData_fullReverseAndForward_encodesCorrectSpeeds() {
        dsl.setMotorData(
                BuWizz2ProtocolConstants.MOTOR_SPEED_FULL_REVERSE,
                BuWizz2ProtocolConstants.MOTOR_SPEED_STOP,
                BuWizz2ProtocolConstants.MOTOR_SPEED_FULL_FORWARD,
                BuWizz2ProtocolConstants.MOTOR_SPEED_STOP,
                false, false, false, false);

        final byte[] expected = {
            (byte) BuWizz2ProtocolConstants.CMD_SET_MOTOR_DATA,
            (byte) BuWizz2ProtocolConstants.MOTOR_SPEED_FULL_REVERSE,
            (byte) BuWizz2ProtocolConstants.MOTOR_SPEED_STOP,
            (byte) BuWizz2ProtocolConstants.MOTOR_SPEED_FULL_FORWARD,
            (byte) BuWizz2ProtocolConstants.MOTOR_SPEED_STOP,
            0x00
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
            (byte) BuWizz2ProtocolConstants.CMD_SET_MOTOR_DATA,
            0x00, 0x00, 0x00, 0x00, 0x00
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // setPowerLevel
    // =========================================================================

    @Test
    void setPowerLevel_sendsCorrectBytes() {
        dsl.setPowerLevel(BuWizz2ProtocolConstants.POWER_LEVEL_FAST);

        final byte[] expected = {
            (byte) BuWizz2ProtocolConstants.CMD_SET_POWER_LEVEL,
            (byte) BuWizz2ProtocolConstants.POWER_LEVEL_FAST
        };
        verifyWrite(expected);
    }

    @Test
    void setPowerLevel_disabled_sendsCorrectBytes() {
        dsl.setPowerLevel(BuWizz2ProtocolConstants.POWER_LEVEL_DISABLED);

        final byte[] expected = {
            (byte) BuWizz2ProtocolConstants.CMD_SET_POWER_LEVEL,
            (byte) BuWizz2ProtocolConstants.POWER_LEVEL_DISABLED
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // setCurrentLimits
    // =========================================================================

    @Test
    void setCurrentLimits_sendsCorrectBytes() {
        dsl.setCurrentLimits(23, 23, 23, 23);

        final byte[] expected = {
            (byte) BuWizz2ProtocolConstants.CMD_SET_CURRENT_LIMITS,
            23, 23, 23, 23
        };
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
                eq(BuWizz2ProtocolConstants.APPLICATION_SERVICE_UUID),
                eq(BuWizz2ProtocolConstants.APPLICATION_DATA_UUID),
                captor.capture());
        assertArrayEquals(expected, captor.getValue());
    }
}
