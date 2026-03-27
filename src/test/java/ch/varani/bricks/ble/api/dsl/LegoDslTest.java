package ch.varani.bricks.ble.api.dsl;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
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
 * Unit tests for {@link LegoDsl} and its nested builders.
 */
class LegoDslTest {

    private BleConnection connection;
    private LegoDsl dsl;

    @BeforeEach
    @SuppressWarnings("unchecked")
    void setUp() {
        connection = mock(BleConnection.class);
        when(connection.writeWithoutResponse(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(connection.notifications(any(), any())).thenReturn(mock(Publisher.class));
        dsl = new LegoDsl(connection);
    }

    // =========================================================================
    // notifications
    // =========================================================================

    @Test
    @SuppressWarnings("unchecked")
    void notifications_returnsPublisherFromConnection() {
        final Publisher<byte[]> publisher = mock(Publisher.class);
        when(connection.notifications(
                LegoProtocolConstants.HUB_SERVICE_UUID,
                LegoProtocolConstants.HUB_CHARACTERISTIC_UUID))
                .thenReturn(publisher);

        assertSame(publisher, dsl.notifications());
    }

    // =========================================================================
    // requestHubProperty / convenience shortcuts
    // =========================================================================

    @Test
    void requestHubProperty_sendsCorrectBytes() {
        dsl.requestHubProperty(LegoProtocolConstants.HUB_PROP_BATTERY_VOLTAGE);

        final byte[] expected = {
            0x05, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_PROPERTIES,
            (byte) LegoProtocolConstants.HUB_PROP_BATTERY_VOLTAGE,
            (byte) LegoProtocolConstants.HUB_PROP_OP_REQUEST_UPDATE
        };
        verifyWrite(expected);
    }

    @Test
    void requestBatteryVoltage_delegatesToRequestHubProperty() {
        dsl.requestBatteryVoltage();

        final byte[] expected = {
            0x05, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_PROPERTIES,
            (byte) LegoProtocolConstants.HUB_PROP_BATTERY_VOLTAGE,
            (byte) LegoProtocolConstants.HUB_PROP_OP_REQUEST_UPDATE
        };
        verifyWrite(expected);
    }

    @Test
    void requestFirmwareVersion_sendsCorrectBytes() {
        dsl.requestFirmwareVersion();

        final byte[] expected = {
            0x05, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_PROPERTIES,
            (byte) LegoProtocolConstants.HUB_PROP_FW_VERSION,
            (byte) LegoProtocolConstants.HUB_PROP_OP_REQUEST_UPDATE
        };
        verifyWrite(expected);
    }

    @Test
    void requestHardwareVersion_sendsCorrectBytes() {
        dsl.requestHardwareVersion();

        final byte[] expected = {
            0x05, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_PROPERTIES,
            (byte) LegoProtocolConstants.HUB_PROP_HW_VERSION,
            (byte) LegoProtocolConstants.HUB_PROP_OP_REQUEST_UPDATE
        };
        verifyWrite(expected);
    }

    @Test
    void requestRssi_sendsCorrectBytes() {
        dsl.requestRssi();

        final byte[] expected = {
            0x05, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_PROPERTIES,
            (byte) LegoProtocolConstants.HUB_PROP_RSSI,
            (byte) LegoProtocolConstants.HUB_PROP_OP_REQUEST_UPDATE
        };
        verifyWrite(expected);
    }

    @Test
    void requestManufacturerName_sendsCorrectBytes() {
        dsl.requestManufacturerName();

        final byte[] expected = {
            0x05, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_PROPERTIES,
            (byte) LegoProtocolConstants.HUB_PROP_MANUFACTURER_NAME,
            (byte) LegoProtocolConstants.HUB_PROP_OP_REQUEST_UPDATE
        };
        verifyWrite(expected);
    }

    @Test
    void requestRadioFirmwareVersion_sendsCorrectBytes() {
        dsl.requestRadioFirmwareVersion();

        final byte[] expected = {
            0x05, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_PROPERTIES,
            (byte) LegoProtocolConstants.HUB_PROP_RADIO_FIRMWARE_VERSION,
            (byte) LegoProtocolConstants.HUB_PROP_OP_REQUEST_UPDATE
        };
        verifyWrite(expected);
    }

    @Test
    void requestLwpVersion_sendsCorrectBytes() {
        dsl.requestLwpVersion();

        final byte[] expected = {
            0x05, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_PROPERTIES,
            (byte) LegoProtocolConstants.HUB_PROP_LWP_VERSION,
            (byte) LegoProtocolConstants.HUB_PROP_OP_REQUEST_UPDATE
        };
        verifyWrite(expected);
    }

    @Test
    void requestSystemTypeId_sendsCorrectBytes() {
        dsl.requestSystemTypeId();

        final byte[] expected = {
            0x05, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_PROPERTIES,
            (byte) LegoProtocolConstants.HUB_PROP_SYSTEM_TYPE_ID,
            (byte) LegoProtocolConstants.HUB_PROP_OP_REQUEST_UPDATE
        };
        verifyWrite(expected);
    }

    @Test
    void requestPrimaryMac_sendsCorrectBytes() {
        dsl.requestPrimaryMac();

        final byte[] expected = {
            0x05, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_PROPERTIES,
            (byte) LegoProtocolConstants.HUB_PROP_PRIMARY_MAC,
            (byte) LegoProtocolConstants.HUB_PROP_OP_REQUEST_UPDATE
        };
        verifyWrite(expected);
    }

    @Test
    void requestSecondaryMac_sendsCorrectBytes() {
        dsl.requestSecondaryMac();

        final byte[] expected = {
            0x05, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_PROPERTIES,
            (byte) LegoProtocolConstants.HUB_PROP_SECONDARY_MAC,
            (byte) LegoProtocolConstants.HUB_PROP_OP_REQUEST_UPDATE
        };
        verifyWrite(expected);
    }

    @Test
    void enableBatteryVoltageUpdates_sendsCorrectBytes() {
        dsl.enableBatteryVoltageUpdates();

        final byte[] expected = {
            0x05, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_PROPERTIES,
            (byte) LegoProtocolConstants.HUB_PROP_BATTERY_VOLTAGE,
            (byte) LegoProtocolConstants.HUB_PROP_OP_ENABLE_UPDATES
        };
        verifyWrite(expected);
    }

    @Test
    void disableBatteryVoltageUpdates_sendsCorrectBytes() {
        dsl.disableBatteryVoltageUpdates();

        final byte[] expected = {
            0x05, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_PROPERTIES,
            (byte) LegoProtocolConstants.HUB_PROP_BATTERY_VOLTAGE,
            (byte) LegoProtocolConstants.HUB_PROP_OP_DISABLE_UPDATES
        };
        verifyWrite(expected);
    }

    @Test
    void hubPropertyOperation_sendsCorrectBytes() {
        dsl.hubPropertyOperation(
                LegoProtocolConstants.HUB_PROP_ADVERTISING_NAME,
                LegoProtocolConstants.HUB_PROP_OP_SET);

        final byte[] expected = {
            0x05, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_PROPERTIES,
            (byte) LegoProtocolConstants.HUB_PROP_ADVERTISING_NAME,
            (byte) LegoProtocolConstants.HUB_PROP_OP_SET
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // sendHubAction
    // =========================================================================

    @Test
    void sendHubAction_sendsCorrectBytes() {
        dsl.sendHubAction(LegoProtocolConstants.HUB_ACTION_SWITCH_OFF);

        final byte[] expected = {
            0x04, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_ACTIONS,
            (byte) LegoProtocolConstants.HUB_ACTION_SWITCH_OFF
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // HubActionBuilder
    // =========================================================================

    @Test
    void hubAction_switchOff_sendsCorrectBytes() {
        dsl.hubAction().switchOff();

        final byte[] expected = {
            0x04, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_ACTIONS,
            (byte) LegoProtocolConstants.HUB_ACTION_SWITCH_OFF
        };
        verifyWrite(expected);
    }

    @Test
    void hubAction_disconnect_sendsCorrectBytes() {
        dsl.hubAction().disconnect();

        final byte[] expected = {
            0x04, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_ACTIONS,
            (byte) LegoProtocolConstants.HUB_ACTION_DISCONNECT
        };
        verifyWrite(expected);
    }

    @Test
    void hubAction_vccPortOn_sendsCorrectBytes() {
        dsl.hubAction().vccPortOn();

        final byte[] expected = {
            0x04, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_ACTIONS,
            (byte) LegoProtocolConstants.HUB_ACTION_VCC_PORT_ON
        };
        verifyWrite(expected);
    }

    @Test
    void hubAction_vccPortOff_sendsCorrectBytes() {
        dsl.hubAction().vccPortOff();

        final byte[] expected = {
            0x04, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_ACTIONS,
            (byte) LegoProtocolConstants.HUB_ACTION_VCC_PORT_OFF
        };
        verifyWrite(expected);
    }

    @Test
    void hubAction_and_returnsParentDsl() {
        assertSame(dsl, dsl.hubAction().and());
    }

    @Test
    void hubAction_activateBusyIndication_sendsCorrectBytes() {
        dsl.hubAction().activateBusyIndication();

        final byte[] expected = {
            0x04, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_ACTIONS,
            (byte) LegoProtocolConstants.HUB_ACTION_ACTIVATE_BUSY_INDICATION
        };
        verifyWrite(expected);
    }

    @Test
    void hubAction_resetBusyIndication_sendsCorrectBytes() {
        dsl.hubAction().resetBusyIndication();

        final byte[] expected = {
            0x04, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_ACTIONS,
            (byte) LegoProtocolConstants.HUB_ACTION_RESET_BUSY_INDICATION
        };
        verifyWrite(expected);
    }

    @Test
    void hubAction_shutdown_sendsCorrectBytes() {
        dsl.hubAction().shutdown();

        final byte[] expected = {
            0x04, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_ACTIONS,
            (byte) LegoProtocolConstants.HUB_ACTION_SHUTDOWN
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // Alert operations
    // =========================================================================

    @Test
    void enableAlert_sendsCorrectBytes() {
        dsl.enableAlert(LegoProtocolConstants.HUB_ALERT_LOW_VOLTAGE);

        final byte[] expected = {
            0x05, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_ALERTS,
            (byte) LegoProtocolConstants.HUB_ALERT_LOW_VOLTAGE,
            (byte) LegoProtocolConstants.HUB_ALERT_OP_ENABLE_UPDATES
        };
        verifyWrite(expected);
    }

    @Test
    void disableAlert_sendsCorrectBytes() {
        dsl.disableAlert(LegoProtocolConstants.HUB_ALERT_HIGH_CURRENT);

        final byte[] expected = {
            0x05, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_ALERTS,
            (byte) LegoProtocolConstants.HUB_ALERT_HIGH_CURRENT,
            (byte) LegoProtocolConstants.HUB_ALERT_OP_DISABLE_UPDATES
        };
        verifyWrite(expected);
    }

    @Test
    void requestAlert_sendsCorrectBytes() {
        dsl.requestAlert(LegoProtocolConstants.HUB_ALERT_OVER_POWER);

        final byte[] expected = {
            0x05, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_ALERTS,
            (byte) LegoProtocolConstants.HUB_ALERT_OVER_POWER,
            (byte) LegoProtocolConstants.HUB_ALERT_OP_REQUEST_UPDATE
        };
        verifyWrite(expected);
    }

    @Test
    void sendAlertOperation_sendsCorrectBytes() {
        dsl.sendAlertOperation(
                LegoProtocolConstants.HUB_ALERT_LOW_SIGNAL,
                LegoProtocolConstants.HUB_ALERT_OP_ENABLE_UPDATES);

        final byte[] expected = {
            0x05, 0x00,
            (byte) LegoProtocolConstants.MSG_HUB_ALERTS,
            (byte) LegoProtocolConstants.HUB_ALERT_LOW_SIGNAL,
            (byte) LegoProtocolConstants.HUB_ALERT_OP_ENABLE_UPDATES
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // portOutputCommand
    // =========================================================================

    @Test
    void portOutputCommand_noParams_sendsCorrectBytes() {
        dsl.portOutputCommand(0x00, 0x11, 0x07);

        final byte[] expected = {0x06, 0x00, (byte) 0x81, 0x00, 0x11, 0x07};
        verifyWrite(expected);
    }

    @Test
    void portOutputCommand_withParams_sendsCorrectBytes() {
        dsl.portOutputCommand(0x01, 0x11, 0x07, (byte) 50, (byte) 100, (byte) 0x00);

        final byte[] expected = {0x09, 0x00, (byte) 0x81, 0x01, 0x11, 0x07, 50, 100, 0x00};
        verifyWrite(expected);
    }

    // =========================================================================
    // requestPortInfo / requestPortModeInfo
    // =========================================================================

    @Test
    void requestPortInfo_sendsCorrectBytes() {
        dsl.requestPortInfo(0x00, 0x01);

        final byte[] expected = {
            0x05, 0x00,
            (byte) LegoProtocolConstants.MSG_PORT_INFO_REQUEST,
            0x00, 0x01
        };
        verifyWrite(expected);
    }

    @Test
    void requestPortModeInfo_sendsCorrectBytes() {
        dsl.requestPortModeInfo(0x00, 0x02, 0x05);

        final byte[] expected = {
            0x06, 0x00,
            (byte) LegoProtocolConstants.MSG_PORT_MODE_INFO_REQUEST,
            0x00, 0x02, 0x05
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // MotorBuilder — startSpeed
    // =========================================================================

    @Test
    void motor_startSpeed_twoArgs_sendsCorrectBytes() {
        dsl.motor(0x00).startSpeed(80, 90);

        // portOutputCommand: [length=9, hubId=0, msgType=0x81, portId=0, startup=0x11, cmd=0x07, 80, 90, 0]
        final byte[] expected = {0x09, 0x00, (byte) 0x81, 0x00, 0x11,
            (byte) LegoProtocolConstants.MOTOR_CMD_START_SPEED, 80, 90, 0x00};
        verifyWrite(expected);
    }

    @Test
    void motor_startSpeed_oneArg_usesDefaultMaxPower() {
        dsl.motor(0x00).startSpeed(50);

        final byte[] expected = {0x09, 0x00, (byte) 0x81, 0x00, 0x11,
            (byte) LegoProtocolConstants.MOTOR_CMD_START_SPEED, 50, 100, 0x00};
        verifyWrite(expected);
    }

    @Test
    void motor_startSpeed_negativeSpeed_encodesCorrectly() {
        dsl.motor(0x01).startSpeed(-75, 80);

        final byte[] expected = {0x09, 0x00, (byte) 0x81, 0x01, 0x11,
            (byte) LegoProtocolConstants.MOTOR_CMD_START_SPEED, (byte) -75, 80, 0x00};
        verifyWrite(expected);
    }

    // =========================================================================
    // MotorBuilder — startSpeedForTime
    // =========================================================================

    @Test
    void motor_startSpeedForTime_encodesTimeLittleEndian() {
        // timeMs = 1000 = 0x03E8
        dsl.motor(0x00).startSpeedForTime(1000, 60, 100);

        final byte[] expected = {
            0x0C, 0x00, (byte) 0x81, 0x00, 0x11,
            (byte) LegoProtocolConstants.MOTOR_CMD_START_SPEED_FOR_TIME,
            (byte) 0xE8, 0x03, 60, 100, 0x7E, 0x00
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // MotorBuilder — startSpeedForDegrees
    // =========================================================================

    @Test
    void motor_startSpeedForDegrees_encodesDegreesLittleEndian() {
        // degrees = 360 = 0x00000168
        dsl.motor(0x00).startSpeedForDegrees(360, 50, 100);

        final byte[] expected = {
            0x0E, 0x00, (byte) 0x81, 0x00, 0x11,
            (byte) LegoProtocolConstants.MOTOR_CMD_START_SPEED_FOR_DEGREES,
            0x68, 0x01, 0x00, 0x00, 50, 100, 0x7E, 0x00
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // MotorBuilder — gotoAbsolutePosition
    // =========================================================================

    @Test
    void motor_gotoAbsolutePosition_encodesPositionLittleEndian() {
        // position = -1 = 0xFFFFFFFF
        dsl.motor(0x00).gotoAbsolutePosition(-1, 50, 100);

        final byte[] expected = {
            0x0E, 0x00, (byte) 0x81, 0x00, 0x11,
            (byte) LegoProtocolConstants.MOTOR_CMD_GOTO_ABSOLUTE_POSITION,
            (byte) 0xFF, (byte) 0xFF, (byte) 0xFF, (byte) 0xFF,
            50, 100, 0x7E, 0x00
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // MotorBuilder — setAccTime / setDecTime
    // =========================================================================

    @Test
    void motor_setAccTime_sendsCorrectBytes() {
        // timeMs = 500 = 0x01F4
        dsl.motor(0x00).setAccTime(500);

        final byte[] expected = {
            0x09, 0x00, (byte) 0x81, 0x00, 0x11,
            (byte) LegoProtocolConstants.MOTOR_CMD_SET_ACC_TIME,
            (byte) 0xF4, 0x01, 0x00
        };
        verifyWrite(expected);
    }

    @Test
    void motor_setDecTime_sendsCorrectBytes() {
        // timeMs = 200 = 0x00C8
        dsl.motor(0x00).setDecTime(200);

        final byte[] expected = {
            0x09, 0x00, (byte) 0x81, 0x00, 0x11,
            (byte) LegoProtocolConstants.MOTOR_CMD_SET_DEC_TIME,
            (byte) 0xC8, 0x00, 0x00
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // MotorBuilder — and
    // =========================================================================

    @Test
    void motor_and_returnsParentDsl() {
        assertSame(dsl, dsl.motor(0x00).and());
    }

    // =========================================================================
    // MotorBuilder — startSpeedForTime with explicit endState
    // =========================================================================

    @Test
    void motor_startSpeedForTime_explicitEndState_encodesCorrectly() {
        // timeMs = 500 = 0x01F4; endState = BRAKING_STYLE_BRAKE (0x7F)
        dsl.motor(0x00).startSpeedForTime(500, 50, 100,
                LegoProtocolConstants.BRAKING_STYLE_BRAKE);

        final byte[] expected = {
            0x0C, 0x00, (byte) 0x81, 0x00, 0x11,
            (byte) LegoProtocolConstants.MOTOR_CMD_START_SPEED_FOR_TIME,
            (byte) 0xF4, 0x01, 50, 100,
            (byte) LegoProtocolConstants.BRAKING_STYLE_BRAKE, 0x00
        };
        verifyWrite(expected);
    }

    @Test
    void motor_startSpeedForTime_explicitFloat_encodesCorrectly() {
        // endState = BRAKING_STYLE_FLOAT (0x00)
        dsl.motor(0x01).startSpeedForTime(100, 30, 80,
                LegoProtocolConstants.BRAKING_STYLE_FLOAT);

        final byte[] expected = {
            0x0C, 0x00, (byte) 0x81, 0x01, 0x11,
            (byte) LegoProtocolConstants.MOTOR_CMD_START_SPEED_FOR_TIME,
            100, 0x00, 30, 80,
            (byte) LegoProtocolConstants.BRAKING_STYLE_FLOAT, 0x00
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // MotorBuilder — startSpeedForDegrees with explicit endState
    // =========================================================================

    @Test
    void motor_startSpeedForDegrees_explicitEndState_encodesCorrectly() {
        // degrees = 180 = 0x000000B4; endState = BRAKING_STYLE_BRAKE
        dsl.motor(0x00).startSpeedForDegrees(180, 50, 100,
                LegoProtocolConstants.BRAKING_STYLE_BRAKE);

        final byte[] expected = {
            0x0E, 0x00, (byte) 0x81, 0x00, 0x11,
            (byte) LegoProtocolConstants.MOTOR_CMD_START_SPEED_FOR_DEGREES,
            (byte) 0xB4, 0x00, 0x00, 0x00, 50, 100,
            (byte) LegoProtocolConstants.BRAKING_STYLE_BRAKE, 0x00
        };
        verifyWrite(expected);
    }

    @Test
    void motor_startSpeedForDegrees_explicitFloat_encodesCorrectly() {
        dsl.motor(0x00).startSpeedForDegrees(90, 40, 70,
                LegoProtocolConstants.BRAKING_STYLE_FLOAT);

        final byte[] expected = {
            0x0E, 0x00, (byte) 0x81, 0x00, 0x11,
            (byte) LegoProtocolConstants.MOTOR_CMD_START_SPEED_FOR_DEGREES,
            90, 0x00, 0x00, 0x00, 40, 70,
            (byte) LegoProtocolConstants.BRAKING_STYLE_FLOAT, 0x00
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // MotorBuilder — gotoAbsolutePosition with explicit endState
    // =========================================================================

    @Test
    void motor_gotoAbsolutePosition_explicitEndState_encodesCorrectly() {
        // position = 90 = 0x0000005A; endState = BRAKING_STYLE_BRAKE
        dsl.motor(0x00).gotoAbsolutePosition(90, 50, 100,
                LegoProtocolConstants.BRAKING_STYLE_BRAKE);

        final byte[] expected = {
            0x0E, 0x00, (byte) 0x81, 0x00, 0x11,
            (byte) LegoProtocolConstants.MOTOR_CMD_GOTO_ABSOLUTE_POSITION,
            0x5A, 0x00, 0x00, 0x00, 50, 100,
            (byte) LegoProtocolConstants.BRAKING_STYLE_BRAKE, 0x00
        };
        verifyWrite(expected);
    }

    @Test
    void motor_gotoAbsolutePosition_explicitFloat_encodesCorrectly() {
        dsl.motor(0x00).gotoAbsolutePosition(45, 30, 80,
                LegoProtocolConstants.BRAKING_STYLE_FLOAT);

        final byte[] expected = {
            0x0E, 0x00, (byte) 0x81, 0x00, 0x11,
            (byte) LegoProtocolConstants.MOTOR_CMD_GOTO_ABSOLUTE_POSITION,
            45, 0x00, 0x00, 0x00, 30, 80,
            (byte) LegoProtocolConstants.BRAKING_STYLE_FLOAT, 0x00
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // MotorBuilder — writeDirect
    // =========================================================================

    @Test
    void motor_writeDirect_sendsCorrectBytes() {
        dsl.motor(0x32).writeDirect((byte) 0x03);

        // portOutputCommand: [length, hubId, 0x81, portId, 0x11, 0x50, data...]
        final byte[] expected = {
            0x07, 0x00, (byte) 0x81, 0x32, 0x11,
            (byte) LegoProtocolConstants.MOTOR_CMD_WRITE_DIRECT,
            0x03
        };
        verifyWrite(expected);
    }

    @Test
    void motor_writeDirect_multipleBytes_sendsCorrectBytes() {
        dsl.motor(0x00).writeDirect((byte) 0x01, (byte) 0x02, (byte) 0x03);

        final byte[] expected = {
            0x09, 0x00, (byte) 0x81, 0x00, 0x11,
            (byte) LegoProtocolConstants.MOTOR_CMD_WRITE_DIRECT,
            0x01, 0x02, 0x03
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // MotorBuilder — writeDirectModeData
    // =========================================================================

    @Test
    void motor_writeDirectModeData_sendsCorrectBytes() {
        // LED colour: mode=0, colorIndex=RED(9)
        dsl.motor(0x32).writeDirectModeData(0x00,
                (byte) LegoProtocolConstants.COLOR_RED);

        final byte[] expected = {
            0x08, 0x00, (byte) 0x81, 0x32, 0x11,
            (byte) LegoProtocolConstants.MOTOR_CMD_WRITE_DIRECT_MODE_DATA,
            0x00,
            (byte) LegoProtocolConstants.COLOR_RED
        };
        verifyWrite(expected);
    }

    @Test
    void motor_writeDirectModeData_soundMode_sendsCorrectBytes() {
        // Duplo sound: mode=1, soundId=HORN(9)
        dsl.motor(0x01).writeDirectModeData(0x01,
                (byte) LegoProtocolConstants.DUPLO_SOUND_HORN);

        final byte[] expected = {
            0x08, 0x00, (byte) 0x81, 0x01, 0x11,
            (byte) LegoProtocolConstants.MOTOR_CMD_WRITE_DIRECT_MODE_DATA,
            0x01,
            (byte) LegoProtocolConstants.DUPLO_SOUND_HORN
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // setupPortInputFormatSingle
    // =========================================================================

    @Test
    void setupPortInputFormatSingle_notifyOn_sendsCorrectBytes() {
        // portId=0, mode=0, deltaInterval=1 (0x00000001), notifyOnChange=true
        dsl.setupPortInputFormatSingle(0x00, 0x00, 1, true);

        final byte[] expected = {
            0x0A, 0x00,
            (byte) LegoProtocolConstants.MSG_PORT_INPUT_FORMAT_SETUP_SINGLE,
            0x00, 0x00,
            0x01, 0x00, 0x00, 0x00,
            0x01
        };
        verifyWrite(expected);
    }

    @Test
    void setupPortInputFormatSingle_notifyOff_sendsCorrectBytes() {
        // portId=1, mode=2, deltaInterval=256=0x00000100, notifyOnChange=false
        dsl.setupPortInputFormatSingle(0x01, 0x02, 256, false);

        final byte[] expected = {
            0x0A, 0x00,
            (byte) LegoProtocolConstants.MSG_PORT_INPUT_FORMAT_SETUP_SINGLE,
            0x01, 0x02,
            0x00, 0x01, 0x00, 0x00,
            0x00
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // setHubLedColor
    // =========================================================================

    @Test
    void setHubLedColor_sendsCorrectBytes() {
        dsl.setHubLedColor(
                LegoProtocolConstants.CITY_HUB_PORT_LED,
                LegoProtocolConstants.COLOR_GREEN);

        // portOutputCommand with MOTOR_CMD_WRITE_DIRECT_MODE_DATA, mode=0x00, colour=6
        final byte[] expected = {
            0x08, 0x00, (byte) 0x81,
            (byte) LegoProtocolConstants.CITY_HUB_PORT_LED,
            0x11,
            (byte) LegoProtocolConstants.MOTOR_CMD_WRITE_DIRECT_MODE_DATA,
            0x00,
            (byte) LegoProtocolConstants.COLOR_GREEN
        };
        verifyWrite(expected);
    }

    // =========================================================================
    // playDuploSound
    // =========================================================================

    @Test
    void playDuploSound_sendsCorrectBytes() {
        dsl.playDuploSound(0x01, LegoProtocolConstants.DUPLO_SOUND_STEAM);

        final byte[] expected = {
            0x08, 0x00, (byte) 0x81,
            0x01,
            0x11,
            (byte) LegoProtocolConstants.MOTOR_CMD_WRITE_DIRECT_MODE_DATA,
            0x01,
            (byte) LegoProtocolConstants.DUPLO_SOUND_STEAM
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
    // writeRaw — default constructor routes to HUB_SERVICE_UUID / HUB_CHARACTERISTIC_UUID
    // =========================================================================

    @Test
    void writeRaw_defaultConstructor_routesToDefaultUuids() {
        final byte[] payload = {0x05, 0x00, 0x01, 0x06, 0x05};
        dsl.writeRaw(payload);

        final ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(connection).writeWithoutResponse(
                eq(LegoProtocolConstants.HUB_SERVICE_UUID),
                eq(LegoProtocolConstants.HUB_CHARACTERISTIC_UUID),
                captor.capture());
        assertArrayEquals(payload, captor.getValue());
    }

    // =========================================================================
    // 4-arg constructor — custom UUIDs
    // =========================================================================

    @Test
    @SuppressWarnings("unchecked")
    void fourArgConstructor_writeRaw_routesToCustomWriteCharacteristic() {
        final String customSvc   = "00001523-1212-efde-1523-785feabcd123";
        final String customWrite = "00001565-1212-efde-1523-785feabcd123";
        final String customNotify = "00001526-1212-efde-1523-785feabcd123";

        final BleConnection conn2 = mock(BleConnection.class);
        when(conn2.writeWithoutResponse(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(conn2.notifications(any(), any())).thenReturn(mock(Publisher.class));

        final LegoDsl wedo2Dsl = new LegoDsl(conn2, customSvc, customWrite, customNotify);
        final byte[] payload = {0x04, 0x00, 0x02, 0x01};
        wedo2Dsl.writeRaw(payload);

        final ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(conn2).writeWithoutResponse(
                eq(customSvc),
                eq(customWrite),
                captor.capture());
        assertArrayEquals(payload, captor.getValue());
    }

    @Test
    @SuppressWarnings("unchecked")
    void fourArgConstructor_notifications_routesToCustomNotifyCharacteristic() {
        final String customSvc    = "00001523-1212-efde-1523-785feabcd123";
        final String customWrite  = "00001565-1212-efde-1523-785feabcd123";
        final String customNotify = "00001526-1212-efde-1523-785feabcd123";

        final BleConnection conn2 = mock(BleConnection.class);
        final Publisher<byte[]> publisher = mock(Publisher.class);
        when(conn2.writeWithoutResponse(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));
        when(conn2.notifications(customSvc, customNotify)).thenReturn(publisher);

        final LegoDsl wedo2Dsl = new LegoDsl(conn2, customSvc, customWrite, customNotify);

        assertSame(publisher, wedo2Dsl.notifications());
    }

    // =========================================================================
    // Helpers
    // =========================================================================

    private void verifyWrite(byte[] expected) {
        final ArgumentCaptor<byte[]> captor = ArgumentCaptor.forClass(byte[].class);
        verify(connection).writeWithoutResponse(
                eq(LegoProtocolConstants.HUB_SERVICE_UUID),
                eq(LegoProtocolConstants.HUB_CHARACTERISTIC_UUID),
                captor.capture());
        assertArrayEquals(expected, captor.getValue());
    }

    /** Suppress unused-import false positive; assertNotNull is used via assertSame. */
    @SuppressWarnings("unused")
    private static void suppressUnused() {
        assertNotNull(new Object());
    }
}
