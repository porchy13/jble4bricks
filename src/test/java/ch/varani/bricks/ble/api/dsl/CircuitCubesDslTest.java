package ch.varani.bricks.ble.api.dsl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.nio.charset.StandardCharsets;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.mockito.ArgumentCaptor;

import ch.varani.bricks.ble.api.BleConnection;
import ch.varani.bricks.ble.api.BleException;
import ch.varani.bricks.ble.device.circuitcubes.CircuitCubesChannel;
import ch.varani.bricks.ble.device.circuitcubes.CircuitCubesProtocolConstants;

/**
 * Unit tests for {@link CircuitCubesDsl}.
 */
class CircuitCubesDslTest {

    private BleConnection connection;
    private CircuitCubesDsl dsl;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        connection = mock(BleConnection.class);
        when(connection.writeWithoutResponse(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(connection.notifications(any(), any())).thenReturn(mock(Publisher.class));
        dsl = new CircuitCubesDsl(connection);
    }

    // =========================================================================
    // notifications
    // =========================================================================

    @Test
    @SuppressWarnings("unchecked")
    void notifications_returnsPublisherFromConnection() {
        final Publisher<byte[]> publisher = mock(Publisher.class);
        when(connection.notifications(
                CircuitCubesProtocolConstants.NUS_SERVICE_UUID,
                CircuitCubesProtocolConstants.RX_CHARACTERISTIC_UUID))
                .thenReturn(publisher);

        assertSame(publisher, dsl.notifications());
    }

    // =========================================================================
    // motorForward
    // =========================================================================

    @Test
    void motorForward_posiveVelocity_sendsForwardCommand() {
        dsl.motorForward(CircuitCubesChannel.A, 100);

        // magnitude = 55 + 100 = 155; sign = '+'
        verifyWriteString("+155a");
    }

    @Test
    void motorForward_velocityAboveMax_clampsToMax() {
        dsl.motorForward(CircuitCubesChannel.B,
                CircuitCubesProtocolConstants.MAX_INTERNAL_VELOCITY + 50);

        // magnitude = 55 + 200 = 255; sign = '+'
        verifyWriteString("+255b");
    }

    @Test
    void motorForward_negativeVelocityArg_usesAbsValue() {
        // motorForward wraps with Math.abs so -50 becomes forward 50
        dsl.motorForward(CircuitCubesChannel.C, -50);

        verifyWriteString("+105c");
    }

    // =========================================================================
    // motorReverse
    // =========================================================================

    @Test
    void motorReverse_positiveVelocity_sendsReverseCommand() {
        dsl.motorReverse(CircuitCubesChannel.A, 100);

        // magnitude = 55 + 100 = 155; sign = '-'
        verifyWriteString("-155a");
    }

    @Test
    void motorReverse_velocityAboveMax_clampsToMax() {
        dsl.motorReverse(CircuitCubesChannel.B,
                CircuitCubesProtocolConstants.MAX_INTERNAL_VELOCITY + 1);

        verifyWriteString("-255b");
    }

    // =========================================================================
    // motorStop
    // =========================================================================

    @Test
    void motorStop_sendsStopCommand() {
        dsl.motorStop(CircuitCubesChannel.A);

        verifyWriteString("+000a");
    }

    // =========================================================================
    // sendMotorCommand — edge cases
    // =========================================================================

    @Test
    void sendMotorCommand_zero_sendsStopCommand() {
        dsl.sendMotorCommand(CircuitCubesChannel.A, 0);

        verifyWriteString("+000a");
    }

    @Test
    void sendMotorCommand_positive_sendsForwardCommand() {
        dsl.sendMotorCommand(CircuitCubesChannel.B, 1);

        // magnitude = 55 + 1 = 56
        verifyWriteString("+056b");
    }

    @Test
    void sendMotorCommand_negative_sendsReverseCommand() {
        dsl.sendMotorCommand(CircuitCubesChannel.C, -1);

        // magnitude = 55 + 1 = 56
        verifyWriteString("-056c");
    }

    @Test
    void sendMotorCommand_exceedsMaxPositive_clampsToMax() {
        dsl.sendMotorCommand(CircuitCubesChannel.A,
                CircuitCubesProtocolConstants.MAX_INTERNAL_VELOCITY + 100);

        verifyWriteString("+255a");
    }

    @Test
    void sendMotorCommand_exceedsMaxNegative_clampsToNegativeMax() {
        dsl.sendMotorCommand(CircuitCubesChannel.A,
                -(CircuitCubesProtocolConstants.MAX_INTERNAL_VELOCITY + 100));

        verifyWriteString("-255a");
    }

    @Test
    void sendMotorCommand_atMaxPositive_sendsMaxMagnitude() {
        dsl.sendMotorCommand(CircuitCubesChannel.A,
                CircuitCubesProtocolConstants.MAX_INTERNAL_VELOCITY);

        verifyWriteString("+255a");
    }

    @Test
    void sendMotorCommand_atMaxNegative_sendsMaxMagnitude() {
        dsl.sendMotorCommand(CircuitCubesChannel.A,
                -CircuitCubesProtocolConstants.MAX_INTERNAL_VELOCITY);

        verifyWriteString("-255a");
    }

    // =========================================================================
    // queryBattery
    // =========================================================================

    @Test
    void queryBattery_sendsBatteryQueryCommand() {
        dsl.queryBattery();

        final ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(connection).writeWithoutResponse(
                eq(CircuitCubesProtocolConstants.NUS_SERVICE_UUID),
                eq(CircuitCubesProtocolConstants.TX_CHARACTERISTIC_UUID),
                captor.capture());
        assertArrayEquals(
                new byte[]{CircuitCubesProtocolConstants.BATTERY_QUERY_COMMAND},
                captor.getValue());
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

    private void verifyWriteString(String expected) {
        final ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(connection).writeWithoutResponse(
                eq(CircuitCubesProtocolConstants.NUS_SERVICE_UUID),
                eq(CircuitCubesProtocolConstants.TX_CHARACTERISTIC_UUID),
                captor.capture());
        assertArrayEquals(
                expected.getBytes(StandardCharsets.UTF_8),
                captor.getValue());
    }
}
