package ch.varani.bricks.ble.device.lego;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link LegoProtocolConstants}.
 */
class LegoProtocolConstantsTest {

    @Test
    void constructor_isPrivate_throwsAssertionError() throws Exception {
        final Constructor<LegoProtocolConstants> ctor =
                LegoProtocolConstants.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final InvocationTargetException ex =
                assertThrows(InvocationTargetException.class, ctor::newInstance);
        assertThrows(AssertionError.class, () -> {
            throw ex.getCause();
        });
    }

    @Test
    void manufacturerId_hasExpectedValue() {
        assertEquals(0x0397, LegoProtocolConstants.MANUFACTURER_ID);
    }

    @Test
    void hubServiceUuid_hasExpectedValue() {
        assertEquals(
                "00001623-1212-efde-1623-785feabcd123",
                LegoProtocolConstants.HUB_SERVICE_UUID
        );
    }

    @Test
    void hubCharacteristicUuid_hasExpectedValue() {
        assertEquals(
                "00001624-1212-efde-1623-785feabcd123",
                LegoProtocolConstants.HUB_CHARACTERISTIC_UUID
        );
    }

    @Test
    void messageTypes_haveDistinctValues() {
        final int[] types = {
            LegoProtocolConstants.MSG_HUB_PROPERTIES,
            LegoProtocolConstants.MSG_HUB_ACTIONS,
            LegoProtocolConstants.MSG_HUB_ALERTS,
            LegoProtocolConstants.MSG_HUB_ATTACHED_IO,
            LegoProtocolConstants.MSG_GENERIC_ERROR,
            LegoProtocolConstants.MSG_PORT_OUTPUT_COMMAND,
            LegoProtocolConstants.MSG_PORT_OUTPUT_COMMAND_FEEDBACK
        };
        // All values must be in valid byte range
        for (final int type : types) {
            assertTrue(type >= 0x00 && type <= 0xFF,
                    "Message type 0x" + Integer.toHexString(type) + " out of byte range");
        }
    }

    @Test
    void hubId_isZero() {
        assertEquals(0x00, LegoProtocolConstants.HUB_ID);
    }

    @Test
    void motorSubCommands_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x07, LegoProtocolConstants.MOTOR_CMD_START_SPEED),
            () -> assertEquals(0x0B, LegoProtocolConstants.MOTOR_CMD_START_SPEED_FOR_DEGREES),
            () -> assertEquals(0x0D, LegoProtocolConstants.MOTOR_CMD_GOTO_ABSOLUTE_POSITION),
            () -> assertEquals(0x50, LegoProtocolConstants.MOTOR_CMD_WRITE_DIRECT),
            () -> assertEquals(0x51, LegoProtocolConstants.MOTOR_CMD_WRITE_DIRECT_MODE_DATA)
        );
    }

    @Test
    void deviceTypeConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x00, LegoProtocolConstants.DEVICE_WEDO2_HUB),
            () -> assertEquals(0x20, LegoProtocolConstants.DEVICE_DUPLO_TRAIN),
            () -> assertEquals(0x40, LegoProtocolConstants.DEVICE_BOOST_HUB),
            () -> assertEquals(0x41, LegoProtocolConstants.DEVICE_2PORT_HUB),
            () -> assertEquals(0x42, LegoProtocolConstants.DEVICE_2PORT_HANDSET),
            () -> assertEquals(0x50, LegoProtocolConstants.DEVICE_TECHNIC_HUB),
            () -> assertEquals(0x60, LegoProtocolConstants.DEVICE_MARIO_HUB)
        );
    }

    @Test
    void manufacturerDataIndexConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0, LegoProtocolConstants.MANUFACTURER_DATA_IDX_MFR_ID_LSB),
            () -> assertEquals(1, LegoProtocolConstants.MANUFACTURER_DATA_IDX_MFR_ID_MSB),
            () -> assertEquals(3, LegoProtocolConstants.MANUFACTURER_DATA_IDX_SYSTEM_TYPE),
            () -> assertEquals(4, LegoProtocolConstants.MANUFACTURER_DATA_MIN_LENGTH),
            () -> assertEquals(0x97, LegoProtocolConstants.MANUFACTURER_ID_LSB),
            () -> assertEquals(0x03, LegoProtocolConstants.MANUFACTURER_ID_MSB)
        );
    }

    @Test
    void wedo2ServiceUuids_haveExpectedValues() {
        assertAll(
            () -> assertEquals(
                    "00001523-1212-efde-1523-785feabcd123",
                    LegoProtocolConstants.WEDO2_SERVICE_UUID),
            () -> assertEquals(
                    "00004f0e-1212-efde-1523-785feabcd123",
                    LegoProtocolConstants.WEDO2_SERVICE_2_UUID)
        );
    }

    @Test
    void wedo2CharacteristicUuids_haveExpectedValues() {
        assertAll(
            () -> assertEquals(
                    "00001524-1212-efde-1523-785feabcd123",
                    LegoProtocolConstants.WEDO2_NAME_UUID),
            () -> assertEquals(
                    "00001526-1212-efde-1523-785feabcd123",
                    LegoProtocolConstants.WEDO2_BUTTON_UUID),
            () -> assertEquals(
                    "00001527-1212-efde-1523-785feabcd123",
                    LegoProtocolConstants.WEDO2_PORT_TYPE_UUID),
            () -> assertEquals(
                    "00001528-1212-efde-1523-785feabcd123",
                    LegoProtocolConstants.WEDO2_LOW_VOLTAGE_ALERT_UUID),
            () -> assertEquals(
                    "00001529-1212-efde-1523-785feabcd123",
                    LegoProtocolConstants.WEDO2_HIGH_CURRENT_ALERT_UUID),
            () -> assertEquals(
                    "0000152a-1212-efde-1523-785feabcd123",
                    LegoProtocolConstants.WEDO2_LOW_SIGNAL_ALERT_UUID),
            () -> assertEquals(
                    "0000152b-1212-efde-1523-785feabcd123",
                    LegoProtocolConstants.WEDO2_DISCONNECT_UUID),
            () -> assertEquals(
                    "00001560-1212-efde-1523-785feabcd123",
                    LegoProtocolConstants.WEDO2_SENSOR_VALUE_UUID),
            () -> assertEquals(
                    "00001561-1212-efde-1523-785feabcd123",
                    LegoProtocolConstants.WEDO2_VALUE_FORMAT_UUID),
            () -> assertEquals(
                    "00001563-1212-efde-1523-785feabcd123",
                    LegoProtocolConstants.WEDO2_PORT_TYPE_WRITE_UUID),
            () -> assertEquals(
                    "00001565-1212-efde-1523-785feabcd123",
                    LegoProtocolConstants.WEDO2_MOTOR_VALUE_WRITE_UUID)
        );
    }

    @Test
    void wedo2BatteryUuids_haveExpectedValues() {
        assertAll(
            () -> assertEquals(
                    "0000180f-0000-1000-8000-00805f9b34fb",
                    LegoProtocolConstants.WEDO2_BATTERY_SERVICE_UUID),
            () -> assertEquals(
                    "00002a19-0000-1000-8000-00805f9b34fb",
                    LegoProtocolConstants.WEDO2_BATTERY_LEVEL_UUID)
        );
    }

    @Test
    void wedo2PortIds_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x01, LegoProtocolConstants.WEDO2_PORT_A),
            () -> assertEquals(0x02, LegoProtocolConstants.WEDO2_PORT_B)
        );
    }

    @Test
    void wedo2DeviceTypeIds_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x01, LegoProtocolConstants.WEDO2_MOTOR_TYPE_ID),
            () -> assertEquals(0x23, LegoProtocolConstants.WEDO2_MOTION_SENSOR_TYPE_ID),
            () -> assertEquals(0x22, LegoProtocolConstants.WEDO2_RGB_LED_TYPE_ID)
        );
    }

    @Test
    void wedo2LedCommandConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x17, LegoProtocolConstants.WEDO2_LED_MODE_SETUP_B1),
            () -> assertEquals(0x01, LegoProtocolConstants.WEDO2_LED_MODE_SETUP_B2),
            () -> assertEquals(0x02, LegoProtocolConstants.WEDO2_LED_MODE_SETUP_B3),
            () -> assertEquals(0x04, LegoProtocolConstants.WEDO2_LED_RGB_CMD_B1),
            () -> assertEquals(0x03, LegoProtocolConstants.WEDO2_LED_RGB_CMD_B2)
        );
    }

    @Test
    void bleMfrIds_haveExpectedValues() {
        assertAll(
            () -> assertEquals(32,  LegoProtocolConstants.BLE_MFR_ID_DUPLO_TRAIN_BASE),
            () -> assertEquals(64,  LegoProtocolConstants.BLE_MFR_ID_MOVE_HUB),
            () -> assertEquals(65,  LegoProtocolConstants.BLE_MFR_ID_HUB),
            () -> assertEquals(66,  LegoProtocolConstants.BLE_MFR_ID_REMOTE_CONTROL),
            () -> assertEquals(67,  LegoProtocolConstants.BLE_MFR_ID_MARIO),
            () -> assertEquals(128, LegoProtocolConstants.BLE_MFR_ID_TECHNIC_MEDIUM_HUB),
            () -> assertEquals(131, LegoProtocolConstants.BLE_MFR_ID_TECHNIC_SMALL_HUB)
        );
    }

    @Test
    void missingHubProperties_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x08, LegoProtocolConstants.HUB_PROP_MANUFACTURER_NAME),
            () -> assertEquals(0x09, LegoProtocolConstants.HUB_PROP_RADIO_FIRMWARE_VERSION),
            () -> assertEquals(0x0A, LegoProtocolConstants.HUB_PROP_LWP_VERSION),
            () -> assertEquals(0x0C, LegoProtocolConstants.HUB_PROP_HW_NETWORK_ID),
            () -> assertEquals(0x0E, LegoProtocolConstants.HUB_PROP_SECONDARY_MAC),
            () -> assertEquals(0x0F, LegoProtocolConstants.HUB_PROP_HW_NETWORK_FAMILY)
        );
    }

    @Test
    void missingHubActions_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x05, LegoProtocolConstants.HUB_ACTION_ACTIVATE_BUSY_INDICATION),
            () -> assertEquals(0x06, LegoProtocolConstants.HUB_ACTION_RESET_BUSY_INDICATION),
            () -> assertEquals(0x2F, LegoProtocolConstants.HUB_ACTION_SHUTDOWN)
        );
    }

    @Test
    void ioDeviceTypeConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0,  LegoProtocolConstants.DEVICE_TYPE_UNKNOWN),
            () -> assertEquals(1,  LegoProtocolConstants.DEVICE_TYPE_SIMPLE_MEDIUM_LINEAR_MOTOR),
            () -> assertEquals(2,  LegoProtocolConstants.DEVICE_TYPE_TRAIN_MOTOR),
            () -> assertEquals(8,  LegoProtocolConstants.DEVICE_TYPE_LIGHT),
            () -> assertEquals(20, LegoProtocolConstants.DEVICE_TYPE_VOLTAGE_SENSOR),
            () -> assertEquals(21, LegoProtocolConstants.DEVICE_TYPE_CURRENT_SENSOR),
            () -> assertEquals(22, LegoProtocolConstants.DEVICE_TYPE_PIEZO_BUZZER),
            () -> assertEquals(23, LegoProtocolConstants.DEVICE_TYPE_HUB_LED),
            () -> assertEquals(34, LegoProtocolConstants.DEVICE_TYPE_TILT_SENSOR),
            () -> assertEquals(35, LegoProtocolConstants.DEVICE_TYPE_MOTION_SENSOR),
            () -> assertEquals(37, LegoProtocolConstants.DEVICE_TYPE_COLOR_DISTANCE_SENSOR),
            () -> assertEquals(38, LegoProtocolConstants.DEVICE_TYPE_MEDIUM_LINEAR_MOTOR),
            () -> assertEquals(39, LegoProtocolConstants.DEVICE_TYPE_MOVE_HUB_MEDIUM_LINEAR_MOTOR),
            () -> assertEquals(40, LegoProtocolConstants.DEVICE_TYPE_MOVE_HUB_TILT_SENSOR),
            () -> assertEquals(41, LegoProtocolConstants.DEVICE_TYPE_DUPLO_TRAIN_BASE_MOTOR),
            () -> assertEquals(42, LegoProtocolConstants.DEVICE_TYPE_DUPLO_TRAIN_BASE_SPEAKER),
            () -> assertEquals(43, LegoProtocolConstants.DEVICE_TYPE_DUPLO_TRAIN_BASE_COLOR_SENSOR),
            () -> assertEquals(44, LegoProtocolConstants.DEVICE_TYPE_DUPLO_TRAIN_BASE_SPEEDOMETER),
            () -> assertEquals(46, LegoProtocolConstants.DEVICE_TYPE_TECHNIC_LARGE_LINEAR_MOTOR),
            () -> assertEquals(47, LegoProtocolConstants.DEVICE_TYPE_TECHNIC_XLARGE_LINEAR_MOTOR),
            () -> assertEquals(48, LegoProtocolConstants.DEVICE_TYPE_TECHNIC_MEDIUM_ANGULAR_MOTOR),
            () -> assertEquals(49, LegoProtocolConstants.DEVICE_TYPE_TECHNIC_LARGE_ANGULAR_MOTOR),
            () -> assertEquals(54, LegoProtocolConstants.DEVICE_TYPE_TECHNIC_MEDIUM_HUB_GEST_SENSOR),
            () -> assertEquals(55, LegoProtocolConstants.DEVICE_TYPE_REMOTE_CONTROL_BUTTON),
            () -> assertEquals(56, LegoProtocolConstants.DEVICE_TYPE_REMOTE_CONTROL_RSSI),
            () -> assertEquals(57, LegoProtocolConstants.DEVICE_TYPE_TECHNIC_MEDIUM_HUB_ACCELEROMETER),
            () -> assertEquals(58, LegoProtocolConstants.DEVICE_TYPE_TECHNIC_MEDIUM_HUB_GYRO_SENSOR),
            () -> assertEquals(59, LegoProtocolConstants.DEVICE_TYPE_TECHNIC_MEDIUM_HUB_TILT_SENSOR),
            () -> assertEquals(60, LegoProtocolConstants.DEVICE_TYPE_TECHNIC_MEDIUM_HUB_TEMPERATURE_SENSOR),
            () -> assertEquals(61, LegoProtocolConstants.DEVICE_TYPE_TECHNIC_COLOR_SENSOR),
            () -> assertEquals(62, LegoProtocolConstants.DEVICE_TYPE_TECHNIC_DISTANCE_SENSOR),
            () -> assertEquals(63, LegoProtocolConstants.DEVICE_TYPE_TECHNIC_FORCE_SENSOR),
            () -> assertEquals(64, LegoProtocolConstants.DEVICE_TYPE_TECHNIC_3X3_COLOR_LIGHT_MATRIX),
            () -> assertEquals(65, LegoProtocolConstants.DEVICE_TYPE_TECHNIC_SMALL_ANGULAR_MOTOR),
            () -> assertEquals(71, LegoProtocolConstants.DEVICE_TYPE_MARIO_ACCELEROMETER),
            () -> assertEquals(73, LegoProtocolConstants.DEVICE_TYPE_MARIO_BARCODE_SENSOR),
            () -> assertEquals(74, LegoProtocolConstants.DEVICE_TYPE_MARIO_PANTS_SENSOR),
            () -> assertEquals(75, LegoProtocolConstants.DEVICE_TYPE_TECHNIC_MEDIUM_ANGULAR_MOTOR_GREY),
            () -> assertEquals(76, LegoProtocolConstants.DEVICE_TYPE_TECHNIC_LARGE_ANGULAR_MOTOR_GREY)
        );
    }

    @Test
    void ioTypeConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x0001, LegoProtocolConstants.IO_TYPE_MOTOR),
            () -> assertEquals(0x0002, LegoProtocolConstants.IO_TYPE_SYSTEM_TRAIN_MOTOR),
            () -> assertEquals(0x0005, LegoProtocolConstants.IO_TYPE_BUTTON),
            () -> assertEquals(0x0008, LegoProtocolConstants.IO_TYPE_LED_LIGHT),
            () -> assertEquals(0x0014, LegoProtocolConstants.IO_TYPE_VOLTAGE),
            () -> assertEquals(0x0015, LegoProtocolConstants.IO_TYPE_CURRENT),
            () -> assertEquals(0x0016, LegoProtocolConstants.IO_TYPE_PIEZO_TONE_SOUND),
            () -> assertEquals(0x0017, LegoProtocolConstants.IO_TYPE_RGB_LIGHT),
            () -> assertEquals(0x0022, LegoProtocolConstants.IO_TYPE_EXTERNAL_TILT_SENSOR),
            () -> assertEquals(0x0023, LegoProtocolConstants.IO_TYPE_MOTION_SENSOR),
            () -> assertEquals(0x0025, LegoProtocolConstants.IO_TYPE_VISION_SENSOR),
            () -> assertEquals(0x0026, LegoProtocolConstants.IO_TYPE_EXTERNAL_MOTOR),
            () -> assertEquals(0x0027, LegoProtocolConstants.IO_TYPE_INTERNAL_MOTOR),
            () -> assertEquals(0x0028, LegoProtocolConstants.IO_TYPE_INTERNAL_TILT)
        );
    }

    @Test
    void colorConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0,   LegoProtocolConstants.COLOR_BLACK),
            () -> assertEquals(1,   LegoProtocolConstants.COLOR_PINK),
            () -> assertEquals(2,   LegoProtocolConstants.COLOR_PURPLE),
            () -> assertEquals(3,   LegoProtocolConstants.COLOR_BLUE),
            () -> assertEquals(4,   LegoProtocolConstants.COLOR_LIGHT_BLUE),
            () -> assertEquals(5,   LegoProtocolConstants.COLOR_CYAN),
            () -> assertEquals(6,   LegoProtocolConstants.COLOR_GREEN),
            () -> assertEquals(7,   LegoProtocolConstants.COLOR_YELLOW),
            () -> assertEquals(8,   LegoProtocolConstants.COLOR_ORANGE),
            () -> assertEquals(9,   LegoProtocolConstants.COLOR_RED),
            () -> assertEquals(10,  LegoProtocolConstants.COLOR_WHITE),
            () -> assertEquals(255, LegoProtocolConstants.COLOR_NONE)
        );
    }

    @Test
    void buttonStateConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0,   LegoProtocolConstants.BUTTON_STATE_RELEASED),
            () -> assertEquals(1,   LegoProtocolConstants.BUTTON_STATE_UP),
            () -> assertEquals(2,   LegoProtocolConstants.BUTTON_STATE_PRESSED),
            () -> assertEquals(127, LegoProtocolConstants.BUTTON_STATE_STOP),
            () -> assertEquals(255, LegoProtocolConstants.BUTTON_STATE_DOWN)
        );
    }

    @Test
    void brakingStyleConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0,   LegoProtocolConstants.BRAKING_STYLE_FLOAT),
            () -> assertEquals(126, LegoProtocolConstants.BRAKING_STYLE_HOLD),
            () -> assertEquals(127, LegoProtocolConstants.BRAKING_STYLE_BRAKE)
        );
    }

    @Test
    void errorCodeConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x01, LegoProtocolConstants.ERROR_CODE_ACK),
            () -> assertEquals(0x02, LegoProtocolConstants.ERROR_CODE_MACK),
            () -> assertEquals(0x03, LegoProtocolConstants.ERROR_CODE_BUFFER_OVERFLOW),
            () -> assertEquals(0x04, LegoProtocolConstants.ERROR_CODE_TIMEOUT),
            () -> assertEquals(0x05, LegoProtocolConstants.ERROR_CODE_COMMAND_NOT_RECOGNIZED),
            () -> assertEquals(0x06, LegoProtocolConstants.ERROR_CODE_INVALID_USE),
            () -> assertEquals(0x07, LegoProtocolConstants.ERROR_CODE_OVERCURRENT),
            () -> assertEquals(0x08, LegoProtocolConstants.ERROR_CODE_INTERNAL_ERROR)
        );
    }

    @Test
    void commandFeedbackConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x00, LegoProtocolConstants.FEEDBACK_TRANSMISSION_PENDING),
            () -> assertEquals(0x10, LegoProtocolConstants.FEEDBACK_TRANSMISSION_BUSY),
            () -> assertEquals(0x44, LegoProtocolConstants.FEEDBACK_TRANSMISSION_DISCARDED),
            () -> assertEquals(0x20, LegoProtocolConstants.FEEDBACK_EXECUTION_PENDING),
            () -> assertEquals(0x21, LegoProtocolConstants.FEEDBACK_EXECUTION_BUSY),
            () -> assertEquals(0x22, LegoProtocolConstants.FEEDBACK_EXECUTION_COMPLETED),
            () -> assertEquals(0x24, LegoProtocolConstants.FEEDBACK_EXECUTION_DISCARDED),
            () -> assertEquals(0x26, LegoProtocolConstants.FEEDBACK_DISABLED),
            () -> assertEquals(0x66, LegoProtocolConstants.FEEDBACK_MISSING)
        );
    }

    @Test
    void modeInfoTypeConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x00, LegoProtocolConstants.MODE_INFO_NAME),
            () -> assertEquals(0x01, LegoProtocolConstants.MODE_INFO_RAW),
            () -> assertEquals(0x02, LegoProtocolConstants.MODE_INFO_PCT),
            () -> assertEquals(0x03, LegoProtocolConstants.MODE_INFO_SI),
            () -> assertEquals(0x04, LegoProtocolConstants.MODE_INFO_SYMBOL),
            () -> assertEquals(0x05, LegoProtocolConstants.MODE_INFO_MAPPING),
            () -> assertEquals(0x06, LegoProtocolConstants.MODE_INFO_USED_INTERNALLY),
            () -> assertEquals(0x07, LegoProtocolConstants.MODE_INFO_MOTOR_BIAS),
            () -> assertEquals(0x08, LegoProtocolConstants.MODE_INFO_CAPABILITY_BITS),
            () -> assertEquals(0x80, LegoProtocolConstants.MODE_INFO_VALUE_FORMAT)
        );
    }

    @Test
    void hwNetCmdConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x02, LegoProtocolConstants.HW_NET_CMD_CONNECTION_REQUEST),
            () -> assertEquals(0x03, LegoProtocolConstants.HW_NET_CMD_FAMILY_REQUEST),
            () -> assertEquals(0x04, LegoProtocolConstants.HW_NET_CMD_FAMILY_SET),
            () -> assertEquals(0x05, LegoProtocolConstants.HW_NET_CMD_JOIN_DENIED),
            () -> assertEquals(0x06, LegoProtocolConstants.HW_NET_CMD_GET_FAMILY),
            () -> assertEquals(0x07, LegoProtocolConstants.HW_NET_CMD_FAMILY),
            () -> assertEquals(0x08, LegoProtocolConstants.HW_NET_CMD_GET_SUBFAMILY),
            () -> assertEquals(0x09, LegoProtocolConstants.HW_NET_CMD_SUBFAMILY),
            () -> assertEquals(0x0A, LegoProtocolConstants.HW_NET_CMD_SUBFAMILY_SET),
            () -> assertEquals(0x0B, LegoProtocolConstants.HW_NET_CMD_GET_EXTENDED_FAMILY),
            () -> assertEquals(0x0C, LegoProtocolConstants.HW_NET_CMD_EXTENDED_FAMILY),
            () -> assertEquals(0x0D, LegoProtocolConstants.HW_NET_CMD_EXTENDED_FAMILY_SET),
            () -> assertEquals(0x0E, LegoProtocolConstants.HW_NET_CMD_RESET_LONG_PRESS_TIMING)
        );
    }

    @Test
    void portInputSubCmdConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x01, LegoProtocolConstants.PORT_INPUT_SUB_CMD_SET_MODE_AND_DATASET_COMBINATIONS),
            () -> assertEquals(0x02, LegoProtocolConstants.PORT_INPUT_SUB_CMD_LOCK_LPF2_DEVICE_FOR_SETUP),
            () -> assertEquals(0x03, LegoProtocolConstants.PORT_INPUT_SUB_CMD_UNLOCK_AND_START_MULTI_UPDATE_ENABLED),
            () -> assertEquals(0x04, LegoProtocolConstants.PORT_INPUT_SUB_CMD_UNLOCK_AND_START_MULTI_UPDATE_DISABLED),
            () -> assertEquals(0x05, LegoProtocolConstants.PORT_INPUT_SUB_CMD_NOT_USED),
            () -> assertEquals(0x06, LegoProtocolConstants.PORT_INPUT_SUB_CMD_RESET_SENSOR)
        );
    }

    @Test
    void duploSoundConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(3,  LegoProtocolConstants.DUPLO_SOUND_BRAKE),
            () -> assertEquals(5,  LegoProtocolConstants.DUPLO_SOUND_STATION_DEPARTURE),
            () -> assertEquals(7,  LegoProtocolConstants.DUPLO_SOUND_WATER_REFILL),
            () -> assertEquals(9,  LegoProtocolConstants.DUPLO_SOUND_HORN),
            () -> assertEquals(10, LegoProtocolConstants.DUPLO_SOUND_STEAM)
        );
    }

    @Test
    void moveHubPortConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x00, LegoProtocolConstants.MOVE_HUB_PORT_A),
            () -> assertEquals(0x01, LegoProtocolConstants.MOVE_HUB_PORT_B),
            () -> assertEquals(0x02, LegoProtocolConstants.MOVE_HUB_PORT_C),
            () -> assertEquals(0x03, LegoProtocolConstants.MOVE_HUB_PORT_D),
            () -> assertEquals(0x32, LegoProtocolConstants.MOVE_HUB_PORT_LED),
            () -> assertEquals(0x3A, LegoProtocolConstants.MOVE_HUB_PORT_TILT_SENSOR),
            () -> assertEquals(0x3B, LegoProtocolConstants.MOVE_HUB_PORT_CURRENT_SENSOR),
            () -> assertEquals(0x3C, LegoProtocolConstants.MOVE_HUB_PORT_VOLTAGE_SENSOR)
        );
    }

    @Test
    void cityHubPortConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x00, LegoProtocolConstants.CITY_HUB_PORT_A),
            () -> assertEquals(0x01, LegoProtocolConstants.CITY_HUB_PORT_B),
            () -> assertEquals(0x32, LegoProtocolConstants.CITY_HUB_PORT_LED),
            () -> assertEquals(0x3B, LegoProtocolConstants.CITY_HUB_PORT_CURRENT_SENSOR),
            () -> assertEquals(0x3C, LegoProtocolConstants.CITY_HUB_PORT_VOLTAGE_SENSOR)
        );
    }

    @Test
    void remotePortConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x00, LegoProtocolConstants.REMOTE_PORT_LEFT),
            () -> assertEquals(0x01, LegoProtocolConstants.REMOTE_PORT_RIGHT),
            () -> assertEquals(0x34, LegoProtocolConstants.REMOTE_PORT_LED),
            () -> assertEquals(0x3B, LegoProtocolConstants.REMOTE_PORT_VOLTAGE_SENSOR),
            () -> assertEquals(0x3C, LegoProtocolConstants.REMOTE_PORT_RSSI)
        );
    }

    @Test
    void technicHubPortConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x00, LegoProtocolConstants.TECHNIC_HUB_PORT_A),
            () -> assertEquals(0x01, LegoProtocolConstants.TECHNIC_HUB_PORT_B),
            () -> assertEquals(0x02, LegoProtocolConstants.TECHNIC_HUB_PORT_C),
            () -> assertEquals(0x03, LegoProtocolConstants.TECHNIC_HUB_PORT_D),
            () -> assertEquals(0x32, LegoProtocolConstants.TECHNIC_HUB_PORT_LED),
            () -> assertEquals(0x3B, LegoProtocolConstants.TECHNIC_HUB_PORT_CURRENT_SENSOR),
            () -> assertEquals(0x3C, LegoProtocolConstants.TECHNIC_HUB_PORT_VOLTAGE_SENSOR),
            () -> assertEquals(0x61, LegoProtocolConstants.TECHNIC_HUB_PORT_ACCELEROMETER),
            () -> assertEquals(0x62, LegoProtocolConstants.TECHNIC_HUB_PORT_GYRO_SENSOR),
            () -> assertEquals(0x63, LegoProtocolConstants.TECHNIC_HUB_PORT_TILT_SENSOR)
        );
    }

    @Test
    void wedo2VirtualPortConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x03, LegoProtocolConstants.WEDO2_PORT_CURRENT_SENSOR),
            () -> assertEquals(0x04, LegoProtocolConstants.WEDO2_PORT_VOLTAGE_SENSOR),
            () -> assertEquals(0x05, LegoProtocolConstants.WEDO2_PORT_PIEZO_BUZZER),
            () -> assertEquals(0x06, LegoProtocolConstants.WEDO2_PORT_LED)
        );
    }

    @Test
    void wedo2DeviceInfoUuids_haveExpectedValues() {
        assertAll(
            () -> assertEquals(
                    "0000180a-0000-1000-8000-00805f9b34fb",
                    LegoProtocolConstants.WEDO2_DEVICE_INFO_SERVICE_UUID),
            () -> assertEquals(
                    "00002a26-0000-1000-8000-00805f9b34fb",
                    LegoProtocolConstants.WEDO2_FIRMWARE_REVISION_UUID)
        );
    }

    @Test
    void wedo2LedIndexedModeConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x01, LegoProtocolConstants.WEDO2_LED_IDX_MODE_SETUP_B3),
            () -> assertEquals(0x04, LegoProtocolConstants.WEDO2_LED_IDX_CMD_B1),
            () -> assertEquals(0x01, LegoProtocolConstants.WEDO2_LED_IDX_CMD_B2)
        );
    }

    @Test
    void wedo2LedColorIndexConstants_haveExpectedValues() {
        assertAll(
            () -> assertEquals(0x00, LegoProtocolConstants.WEDO2_LED_COLOR_BLACK),
            () -> assertEquals(0x01, LegoProtocolConstants.WEDO2_LED_COLOR_PINK),
            () -> assertEquals(0x02, LegoProtocolConstants.WEDO2_LED_COLOR_PURPLE),
            () -> assertEquals(0x03, LegoProtocolConstants.WEDO2_LED_COLOR_BLUE),
            () -> assertEquals(0x04, LegoProtocolConstants.WEDO2_LED_COLOR_LIGHT_BLUE),
            () -> assertEquals(0x05, LegoProtocolConstants.WEDO2_LED_COLOR_CYAN),
            () -> assertEquals(0x06, LegoProtocolConstants.WEDO2_LED_COLOR_GREEN),
            () -> assertEquals(0x07, LegoProtocolConstants.WEDO2_LED_COLOR_YELLOW),
            () -> assertEquals(0x08, LegoProtocolConstants.WEDO2_LED_COLOR_ORANGE),
            () -> assertEquals(0x09, LegoProtocolConstants.WEDO2_LED_COLOR_RED),
            () -> assertEquals(0x0A, LegoProtocolConstants.WEDO2_LED_COLOR_WHITE)
        );
    }
}
