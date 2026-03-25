package ch.varani.bricks.ble.api.dsl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
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
import ch.varani.bricks.ble.device.sbrick.SBrickProtocolConstants;

/**
 * Unit tests for {@link SBrickDsl}.
 */
class SBrickDslTest {

    private BleConnection connection;
    private SBrickDsl dsl;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        connection = mock(BleConnection.class);
        when(connection.writeWithoutResponse(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(connection.notifications(any(), any())).thenReturn(mock(Publisher.class));
        dsl = new SBrickDsl(connection);
    }

    // =========================================================================
    // notifications
    // =========================================================================

    @Test
    @SuppressWarnings("unchecked")
    void notifications_returnsPublisherFromConnection() {
        final Publisher<byte[]> publisher = mock(Publisher.class);
        when(connection.notifications(
                SBrickProtocolConstants.REMOTE_CONTROL_SERVICE_UUID,
                SBrickProtocolConstants.REMOTE_CONTROL_COMMANDS_UUID))
                .thenReturn(publisher);

        assertSame(publisher, dsl.notifications());
    }

    // =========================================================================
    // drive
    // =========================================================================

    @Test
    void drive_clockwise_sendsCorrectBytes() {
        dsl.drive(SBrickProtocolConstants.CHANNEL_A, false, 200);

        final byte[] expected = {
            (byte) SBrickProtocolConstants.CMD_DRIVE,
            (byte) SBrickProtocolConstants.CHANNEL_A,
            (byte) SBrickProtocolConstants.DIRECTION_CLOCKWISE,
            (byte) (200 & 0xFF)
        };
        verifyWrite(expected);
    }

    @Test
    void drive_counterClockwise_sendsCorrectDirectionByte() {
        dsl.drive(SBrickProtocolConstants.CHANNEL_B, true, 128);

        final byte[] expected = {
            (byte) SBrickProtocolConstants.CMD_DRIVE,
            (byte) SBrickProtocolConstants.CHANNEL_B,
            (byte) SBrickProtocolConstants.DIRECTION_COUNTER_CLOCKWISE,
            (byte) 128
        };
        verifyWrite(expected);
    }

    @Test
    void drive_powerMaskedTo8Bits() {
        // 256 & 0xFF == 0
        dsl.drive(SBrickProtocolConstants.CHANNEL_C, false, 256);

        final byte[] expected = {
            (byte) SBrickProtocolConstants.CMD_DRIVE,
            (byte) SBrickProtocolConstants.CHANNEL_C,
            (byte) SBrickProtocolConstants.DIRECTION_CLOCKWISE,
            (byte) 0
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // brake
    // =========================================================================

    @Test
    void brake_singleChannel_sendsCorrectBytes() {
        dsl.brake(SBrickProtocolConstants.CHANNEL_A);

        final byte[] expected = {
            (byte) SBrickProtocolConstants.CMD_BRAKE,
            (byte) SBrickProtocolConstants.CHANNEL_A
        };
        verifyWrite(expected);
    }

    @Test
    void brake_multipleChannels_sendsCorrectBytes() {
        dsl.brake(SBrickProtocolConstants.CHANNEL_A,
                  SBrickProtocolConstants.CHANNEL_B,
                  SBrickProtocolConstants.CHANNEL_C,
                  SBrickProtocolConstants.CHANNEL_D);

        final byte[] expected = {
            (byte) SBrickProtocolConstants.CMD_BRAKE,
            (byte) SBrickProtocolConstants.CHANNEL_A,
            (byte) SBrickProtocolConstants.CHANNEL_B,
            (byte) SBrickProtocolConstants.CHANNEL_C,
            (byte) SBrickProtocolConstants.CHANNEL_D
        };
        verifyWrite(expected);
    }

    @Test
    void brake_noChannels_throwsIllegalArgumentException() {
        assertThrows(IllegalArgumentException.class, () -> dsl.brake());
    }

    // =========================================================================
    // queryAdc / convenience shortcuts
    // =========================================================================

    @Test
    void queryAdc_sendsCorrectBytes() {
        dsl.queryAdc(SBrickProtocolConstants.ADC_CHANNEL_BATTERY);

        final byte[] expected = {
            (byte) SBrickProtocolConstants.CMD_QUERY_ADC,
            (byte) SBrickProtocolConstants.ADC_CHANNEL_BATTERY
        };
        verifyWrite(expected);
    }

    @Test
    void queryBatteryVoltage_delegatesToQueryAdc() {
        dsl.queryBatteryVoltage();

        final byte[] expected = {
            (byte) SBrickProtocolConstants.CMD_QUERY_ADC,
            (byte) SBrickProtocolConstants.ADC_CHANNEL_BATTERY
        };
        verifyWrite(expected);
    }

    @Test
    void queryTemperature_delegatesToQueryAdc() {
        dsl.queryTemperature();

        final byte[] expected = {
            (byte) SBrickProtocolConstants.CMD_QUERY_ADC,
            (byte) SBrickProtocolConstants.ADC_CHANNEL_TEMPERATURE
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // watchdog
    // =========================================================================

    @Test
    void setWatchdogTimeout_sendsCorrectBytes() {
        dsl.setWatchdogTimeout(5);

        final byte[] expected = {
            (byte) SBrickProtocolConstants.CMD_SET_WATCHDOG,
            0x05
        };
        verifyWrite(expected);
    }

    @Test
    void getWatchdogTimeout_sendsCorrectBytes() {
        dsl.getWatchdogTimeout();

        final byte[] expected = {(byte) SBrickProtocolConstants.CMD_GET_WATCHDOG};
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
                eq(SBrickProtocolConstants.REMOTE_CONTROL_SERVICE_UUID),
                eq(SBrickProtocolConstants.REMOTE_CONTROL_COMMANDS_UUID),
                captor.capture());
        assertArrayEquals(expected, captor.getValue());
    }
}
