package ch.varani.bricks.ble.device.lego;

/**
 * Constants for the LEGO Wireless Protocol 3.0 (LWP3).
 *
 * <p>All UUIDs are normalised to lower-case.
 * All byte values are expressed as {@code int} to avoid sign-extension issues
 * when combining with bitwise operations.
 *
 * <p>Reference: <a href="https://lego.github.io/lego-ble-wireless-protocol-docs/">
 * LEGO BLE Wireless Protocol 3.0 specification</a>
 */
public final class LegoProtocolConstants {

    // =========================================================================
    // Discovery
    // =========================================================================

    /** BLE manufacturer ID for LEGO System A/S (little-endian: 0x0397). */
    public static final int MANUFACTURER_ID = 0x0397;

    /** Length of the manufacturer-specific advertisement data payload in bytes. */
    public static final int MANUFACTURER_DATA_LENGTH = 10;

    // =========================================================================
    // GATT Service and Characteristic UUIDs
    // =========================================================================

    /** 128-bit UUID of the LEGO Hub GATT service (LWP3 — City Hub, Technic Hub, …). */
    public static final String HUB_SERVICE_UUID =
            "00001623-1212-efde-1623-785feabcd123";

    /**
     * 128-bit UUID of the LEGO Hub GATT characteristic (LWP3).
     *
     * <p>Supports Write Without Response (or Write With Response on iOS) and Notify.
     * All upstream and downstream messages use this single characteristic.
     */
    public static final String HUB_CHARACTERISTIC_UUID =
            "00001624-1212-efde-1623-785feabcd123";

    // ── WeDo 2.0 proprietary GATT UUIDs (NOT LWP3) ───────────────────────────
    //
    // The WeDo 2.0 hub does NOT use the LWP3 protocol.  It has its own set of
    // dedicated GATT characteristics, one per function.  All UUIDs belong to
    // the WeDo 2.0 service (00001523-...).
    //
    // Reference: nathankellenicki/node-poweredup — src/consts.ts and
    //            src/hubs/wedo2smarthub.ts

    /** 128-bit UUID of the WeDo 2.0 primary GATT service (notifications). */
    public static final String WEDO2_SERVICE_UUID =
            "00001523-1212-efde-1523-785feabcd123";

    /**
     * 128-bit UUID of the WeDo 2.0 secondary GATT service.
     *
     * <p>The write characteristics {@link #WEDO2_PORT_TYPE_WRITE_UUID} and
     * {@link #WEDO2_MOTOR_VALUE_WRITE_UUID} live in this service, not in
     * {@link #WEDO2_SERVICE_UUID}.
     * Reference: nathankellenicki/node-poweredup — src/consts.ts {@code WEDO2_SMART_HUB_2}.
     */
    public static final String WEDO2_SERVICE_2_UUID =
            "00004f0e-1212-efde-1523-785feabcd123";

    /**
     * Hub name characteristic (read/write).
     *
     * <p><b>Not</b> a general-purpose command channel — writing motor commands
     * here will rename the hub, not move the motor.
     */
    public static final String WEDO2_NAME_UUID =
            "00001524-1212-efde-1523-785feabcd123";

    /**
     * Button + general-notification characteristic (notify).
     *
     * <p>Subscribe here to receive button-state change notifications.
     */
    public static final String WEDO2_BUTTON_UUID =
            "00001526-1212-efde-1523-785feabcd123";

    /**
     * Port-type attachment/detachment notification characteristic (notify).
     *
     * <p>Subscribe to receive events when a peripheral is plugged in or out.
     */
    public static final String WEDO2_PORT_TYPE_UUID =
            "00001527-1212-efde-1523-785feabcd123";

    /** Low-voltage-alert notification characteristic. */
    public static final String WEDO2_LOW_VOLTAGE_ALERT_UUID =
            "00001528-1212-efde-1523-785feabcd123";

    /** High-current-alert notification characteristic. */
    public static final String WEDO2_HIGH_CURRENT_ALERT_UUID =
            "00001529-1212-efde-1523-785feabcd123";

    /** Low-signal-alert notification characteristic. */
    public static final String WEDO2_LOW_SIGNAL_ALERT_UUID =
            "0000152a-1212-efde-1523-785feabcd123";

    /** Disconnect characteristic (write to request hub disconnect). */
    public static final String WEDO2_DISCONNECT_UUID =
            "0000152b-1212-efde-1523-785feabcd123";

    /**
     * Sensor-value notification characteristic (notify).
     *
     * <p>Subscribe to receive sensor readings.  Each notification payload
     * starts with the port ID followed by the value bytes.
     *
     * <p><b>Service placement note:</b> hardware testing confirms this
     * characteristic resides in {@link #WEDO2_SERVICE_2_UUID} ({@code 0x4F0E}),
     * not in {@link #WEDO2_SERVICE_UUID} ({@code 0x1523}) as might be assumed
     * from the UUID suffix.  Callers that pass {@code WEDO2_SERVICE_UUID} when
     * subscribing will still succeed because the library performs a cross-service
     * fallback search when the characteristic is not found in the specified
     * service.
     */
    public static final String WEDO2_SENSOR_VALUE_UUID =
            "00001560-1212-efde-1523-785feabcd123";

    /**
     * Sensor value-format characteristic (write).
     *
     * <p><b>Service placement note:</b> resides in {@link #WEDO2_SERVICE_2_UUID}
     * ({@code 0x4F0E}); see {@link #WEDO2_SENSOR_VALUE_UUID} for details.
     */
    public static final String WEDO2_VALUE_FORMAT_UUID =
            "00001561-1212-efde-1523-785feabcd123";

    /**
     * Sensor-subscription configuration characteristic (write).
     *
     * <p>Write an 11-byte subscribe/unsubscribe command here to start or stop
     * receiving sensor readings on {@link #WEDO2_SENSOR_VALUE_UUID}.
     *
     * <p>Subscribe command format:
     * <pre>
     * [0x01, 0x02, portId, deviceType, mode, 0x01, 0x00, 0x00, 0x00, 0x00, 0x01]
     * </pre>
     * Unsubscribe: same bytes but last byte = {@code 0x00}.
     */
    public static final String WEDO2_PORT_TYPE_WRITE_UUID =
            "00001563-1212-efde-1523-785feabcd123";

    /**
     * Motor command characteristic (write without response).
     *
     * <p>Write a 4-byte command here to control a motor:
     * <pre>
     * [portId, typeId, mode, power]
     * </pre>
     * where {@code power} is a signed byte in the range −100 to +100.
     */
    public static final String WEDO2_MOTOR_VALUE_WRITE_UUID =
            "00001565-1212-efde-1523-785feabcd123";

    // ── WeDo 2.0 — battery (standard BLE Battery Service) ────────────────────

    /**
     * Standard BLE Battery Service UUID ({@code 0x180F}).
     *
     * <p>Used by the WeDo 2.0 hub to expose battery level.
     */
    public static final String WEDO2_BATTERY_SERVICE_UUID =
            "0000180f-0000-1000-8000-00805f9b34fb";

    /**
     * Standard BLE Battery Level characteristic UUID ({@code 0x2A19}).
     *
     * <p>Read or subscribe to get the battery percentage (0–100) as a single
     * unsigned byte.
     */
    public static final String WEDO2_BATTERY_LEVEL_UUID =
            "00002a19-0000-1000-8000-00805f9b34fb";

    // ── WeDo 2.0 — port IDs ───────────────────────────────────────────────────

    /**
     * WeDo 2.0 physical port A identifier ({@code 0x01}).
     *
     * <p>In the WeDo 2.0 protocol ports are numbered 1-based
     * (node-poweredup convention: PORT_A = 1, PORT_B = 2).
     */
    public static final int WEDO2_PORT_A = 0x01;

    /**
     * WeDo 2.0 physical port B identifier ({@code 0x02}).
     */
    public static final int WEDO2_PORT_B = 0x02;

    // ── WeDo 2.0 — device type IDs ────────────────────────────────────────────

    /**
     * WeDo 2.0 device type: Simple/Medium Linear Motor ({@code 0x01}).
     *
     * <p>Used as {@code typeId} in the 4-byte motor command written to
     * {@link #WEDO2_MOTOR_VALUE_WRITE_UUID}.
     */
    public static final int WEDO2_MOTOR_TYPE_ID = 0x01;

    /**
     * WeDo 2.0 device type: Motion/Distance Sensor ({@code 0x23} = 35).
     *
     * <p>Used as {@code deviceType} in the sensor-subscription command written
     * to {@link #WEDO2_PORT_TYPE_WRITE_UUID}.
     */
    public static final int WEDO2_MOTION_SENSOR_TYPE_ID = 0x23;

    /**
     * WeDo 2.0 device type: RGB LED ({@code 0x22} = 34).
     *
     * <p>The internal RGB LED is also addressed through
     * {@link #WEDO2_MOTOR_VALUE_WRITE_UUID} using a 4-byte command where the
     * power byte encodes the colour index (0–10).  For direct RGB colour control
     * use a separate LED characteristic if available, or rely on the colour-index
     * mode.
     */
    public static final int WEDO2_RGB_LED_TYPE_ID = 0x22;

    // =========================================================================
    // Message Types (LWP3 §3)
    // =========================================================================

    /** Hub Properties message (bidirectional). */
    public static final int MSG_HUB_PROPERTIES = 0x01;

    /** Hub Actions message (bidirectional). */
    public static final int MSG_HUB_ACTIONS = 0x02;

    /** Hub Alerts message (bidirectional). */
    public static final int MSG_HUB_ALERTS = 0x03;

    /** Hub Attached I/O notification (upstream only). */
    public static final int MSG_HUB_ATTACHED_IO = 0x04;

    /** Generic Error message (upstream only). */
    public static final int MSG_GENERIC_ERROR = 0x05;

    /** Hardware Network Commands (bidirectional). */
    public static final int MSG_HW_NETWORK_COMMANDS = 0x08;

    /** Firmware Update — Go Into Boot Mode (downstream only). */
    public static final int MSG_FW_UPDATE_GO_BOOT = 0x10;

    /** Firmware Update Lock Memory (downstream only). */
    public static final int MSG_FW_UPDATE_LOCK_MEMORY = 0x11;

    /** Firmware Update Lock Status Request (downstream only). */
    public static final int MSG_FW_UPDATE_LOCK_STATUS_REQUEST = 0x12;

    /** Firmware Lock Status notification (upstream only). */
    public static final int MSG_FW_LOCK_STATUS = 0x13;

    /** Port Information Request (downstream only). */
    public static final int MSG_PORT_INFO_REQUEST = 0x21;

    /** Port Mode Information Request (downstream only). */
    public static final int MSG_PORT_MODE_INFO_REQUEST = 0x22;

    /** Port Input Format Setup — single mode (downstream only). */
    public static final int MSG_PORT_INPUT_FORMAT_SETUP_SINGLE = 0x41;

    /** Port Input Format Setup — combined mode (downstream only). */
    public static final int MSG_PORT_INPUT_FORMAT_SETUP_COMBINED = 0x42;

    /** Port Information notification (upstream only). */
    public static final int MSG_PORT_INFO = 0x43;

    /** Port Mode Information notification (upstream only). */
    public static final int MSG_PORT_MODE_INFO = 0x44;

    /** Port Value — single mode (upstream only). */
    public static final int MSG_PORT_VALUE_SINGLE = 0x45;

    /** Port Value — combined mode (upstream only). */
    public static final int MSG_PORT_VALUE_COMBINED = 0x46;

    /** Port Input Format — single mode (upstream only). */
    public static final int MSG_PORT_INPUT_FORMAT_SINGLE = 0x47;

    /** Port Input Format — combined mode (upstream only). */
    public static final int MSG_PORT_INPUT_FORMAT_COMBINED = 0x48;

    /** Virtual Port Setup (downstream only). */
    public static final int MSG_VIRTUAL_PORT_SETUP = 0x61;

    /** Port Output Command (downstream only). */
    public static final int MSG_PORT_OUTPUT_COMMAND = 0x81;

    /** Port Output Command Feedback (upstream only). */
    public static final int MSG_PORT_OUTPUT_COMMAND_FEEDBACK = 0x82;

    // =========================================================================
    // Hub Property References (LWP3 §3.5.1)
    // =========================================================================

    /** Hub property: Advertising Name. */
    public static final int HUB_PROP_ADVERTISING_NAME = 0x01;

    /** Hub property: Button state. */
    public static final int HUB_PROP_BUTTON = 0x02;

    /** Hub property: Firmware Version. */
    public static final int HUB_PROP_FW_VERSION = 0x03;

    /** Hub property: Hardware Version. */
    public static final int HUB_PROP_HW_VERSION = 0x04;

    /** Hub property: RSSI. */
    public static final int HUB_PROP_RSSI = 0x05;

    /** Hub property: Battery Voltage (%). */
    public static final int HUB_PROP_BATTERY_VOLTAGE = 0x06;

    /** Hub property: Battery Type. */
    public static final int HUB_PROP_BATTERY_TYPE = 0x07;

    /**
     * Hub property: Manufacturer Name ({@code 0x08}).
     *
     * <p>Operations: Request Update, Update.
     */
    public static final int HUB_PROP_MANUFACTURER_NAME = 0x08;

    /**
     * Hub property: Radio Firmware Version ({@code 0x09}).
     *
     * <p>Operations: Request Update, Update.
     */
    public static final int HUB_PROP_RADIO_FIRMWARE_VERSION = 0x09;

    /**
     * Hub property: LEGO Wireless Protocol Version ({@code 0x0A}).
     *
     * <p>Operations: Request Update, Update.
     * Encoded as 2-byte BCD: {@code MMMM MMMM mmmm mmmm}.
     */
    public static final int HUB_PROP_LWP_VERSION = 0x0A;

    /** Hub property: System Type ID. */
    public static final int HUB_PROP_SYSTEM_TYPE_ID = 0x0B;

    /**
     * Hub property: Hardware Network ID ({@code 0x0C}).
     *
     * <p>Operations: Set, Request Update, Update.
     */
    public static final int HUB_PROP_HW_NETWORK_ID = 0x0C;

    /** Hub property: Primary MAC Address. */
    public static final int HUB_PROP_PRIMARY_MAC = 0x0D;

    /**
     * Hub property: Secondary MAC Address ({@code 0x0E}).
     *
     * <p>Operations: Request Update, Update.
     */
    public static final int HUB_PROP_SECONDARY_MAC = 0x0E;

    /**
     * Hub property: Hardware Network Family ({@code 0x0F}).
     *
     * <p>Operations: Set, Request Update, Update.
     */
    public static final int HUB_PROP_HW_NETWORK_FAMILY = 0x0F;

    // =========================================================================
    // Hub Property Operations (LWP3 §3.5.1)
    // =========================================================================

    /** Property operation: Set value. */
    public static final int HUB_PROP_OP_SET = 0x01;

    /** Property operation: Enable updates (subscribe). */
    public static final int HUB_PROP_OP_ENABLE_UPDATES = 0x02;

    /** Property operation: Disable updates (unsubscribe). */
    public static final int HUB_PROP_OP_DISABLE_UPDATES = 0x03;

    /** Property operation: Reset to default. */
    public static final int HUB_PROP_OP_RESET = 0x04;

    /** Property operation: Request current value. */
    public static final int HUB_PROP_OP_REQUEST_UPDATE = 0x05;

    /** Property operation: Value update notification (upstream). */
    public static final int HUB_PROP_OP_UPDATE = 0x06;

    // =========================================================================
    // Hub Action Types (LWP3 §3.6.1)
    // =========================================================================

    /** Hub action: Switch off hub. */
    public static final int HUB_ACTION_SWITCH_OFF = 0x01;

    /** Hub action: Disconnect. */
    public static final int HUB_ACTION_DISCONNECT = 0x02;

    /** Hub action: VCC port control on. */
    public static final int HUB_ACTION_VCC_PORT_ON = 0x03;

    /** Hub action: VCC port control off. */
    public static final int HUB_ACTION_VCC_PORT_OFF = 0x04;

    /**
     * Hub action: Activate busy indication ({@code 0x05}).
     *
     * <p>Downstream only. Signals the hub to show a "busy" LED state.
     */
    public static final int HUB_ACTION_ACTIVATE_BUSY_INDICATION = 0x05;

    /**
     * Hub action: Reset busy indication ({@code 0x06}).
     *
     * <p>Downstream only. Clears the "busy" LED state set by
     * {@link #HUB_ACTION_ACTIVATE_BUSY_INDICATION}.
     */
    public static final int HUB_ACTION_RESET_BUSY_INDICATION = 0x06;

    /**
     * Hub action: Shutdown ({@code 0x2F}).
     *
     * <p>Downstream only. Instructs the hub to power off immediately without
     * sending an upstream "will switch off" notification.
     */
    public static final int HUB_ACTION_SHUTDOWN = 0x2F;

    /** Hub action (upstream): hub will switch off. */
    public static final int HUB_ACTION_WILL_SWITCH_OFF = 0x30;

    /** Hub action (upstream): hub will disconnect. */
    public static final int HUB_ACTION_WILL_DISCONNECT = 0x31;

    /** Hub action (upstream): hub will go into boot mode. */
    public static final int HUB_ACTION_WILL_GO_BOOT = 0x32;

    // =========================================================================
    // Hub Alert Types (LWP3 §3.7)
    // =========================================================================

    /** Alert type: Low voltage. */
    public static final int HUB_ALERT_LOW_VOLTAGE = 0x01;

    /** Alert type: High current. */
    public static final int HUB_ALERT_HIGH_CURRENT = 0x02;

    /** Alert type: Low signal strength. */
    public static final int HUB_ALERT_LOW_SIGNAL = 0x03;

    /** Alert type: Over power condition. */
    public static final int HUB_ALERT_OVER_POWER = 0x04;

    // =========================================================================
    // Alert Operations
    // =========================================================================

    /** Alert operation: Enable updates. */
    public static final int HUB_ALERT_OP_ENABLE_UPDATES = 0x01;

    /** Alert operation: Disable updates. */
    public static final int HUB_ALERT_OP_DISABLE_UPDATES = 0x02;

    /** Alert operation: Request update. */
    public static final int HUB_ALERT_OP_REQUEST_UPDATE = 0x03;

    /** Alert operation: Update notification (upstream, 1-byte payload). */
    public static final int HUB_ALERT_OP_UPDATE = 0x04;

    // =========================================================================
    // Port Output Sub-commands for motors (LWP3 §3.27.1)
    // =========================================================================

    /** Motor sub-command: SetAccTime (acceleration ramp time). */
    public static final int MOTOR_CMD_SET_ACC_TIME = 0x01;

    /** Motor sub-command: SetDecTime (deceleration ramp time). */
    public static final int MOTOR_CMD_SET_DEC_TIME = 0x02;

    /** Motor sub-command: StartSpeed. */
    public static final int MOTOR_CMD_START_SPEED = 0x07;

    /** Motor sub-command: StartSpeed2 (two synchronised motors). */
    public static final int MOTOR_CMD_START_SPEED2 = 0x08;

    /** Motor sub-command: StartSpeedForTime. */
    public static final int MOTOR_CMD_START_SPEED_FOR_TIME = 0x09;

    /** Motor sub-command: StartSpeedForTime2. */
    public static final int MOTOR_CMD_START_SPEED_FOR_TIME2 = 0x0A;

    /** Motor sub-command: StartSpeedForDegrees. */
    public static final int MOTOR_CMD_START_SPEED_FOR_DEGREES = 0x0B;

    /** Motor sub-command: StartSpeedForDegrees2. */
    public static final int MOTOR_CMD_START_SPEED_FOR_DEGREES2 = 0x0C;

    /** Motor sub-command: GotoAbsolutePosition. */
    public static final int MOTOR_CMD_GOTO_ABSOLUTE_POSITION = 0x0D;

    /** Motor sub-command: GotoAbsolutePosition2. */
    public static final int MOTOR_CMD_GOTO_ABSOLUTE_POSITION2 = 0x0E;

    /** Motor sub-command: WriteDirect. */
    public static final int MOTOR_CMD_WRITE_DIRECT = 0x50;

    /** Motor sub-command: WriteDirectModeData. */
    public static final int MOTOR_CMD_WRITE_DIRECT_MODE_DATA = 0x51;

    // =========================================================================
    // System Type and Device Number encoding (manufacturer data byte index MANUFACTURER_DATA_IDX_SYSTEM_TYPE = 3)
    // =========================================================================

    /** Device: WeDo 2.0 Hub (SSS=000, DDDDD=00000). */
    public static final int DEVICE_WEDO2_HUB = 0x00;

    /** Device: Duplo Train (SSS=001, DDDDD=00000). */
    public static final int DEVICE_DUPLO_TRAIN = 0x20;

    /** Device: Boost Move Hub (SSS=010, DDDDD=00000). */
    public static final int DEVICE_BOOST_HUB = 0x40;

    /** Device: 2-Port Hub / City Hub (SSS=010, DDDDD=00001). */
    public static final int DEVICE_2PORT_HUB = 0x41;

    /** Device: 2-Port Handset (SSS=010, DDDDD=00010). */
    public static final int DEVICE_2PORT_HANDSET = 0x42;

    /** Device: Technic Hub / 4-Port Hub (SSS=010, DDDDD=10000). */
    public static final int DEVICE_TECHNIC_HUB = 0x50;

    /** Device: Mario Hub (SSS=011, DDDDD=00000). */
    public static final int DEVICE_MARIO_HUB = 0x60;

    // ── BLE manufacturer data hub-model IDs (node-poweredup BLEManufacturerData) ─
    //
    // These are the numeric identifiers reported in the manufacturer-specific
    // advertisement payload to distinguish hub models.  They map to the values
    // in the node-poweredup BLEManufacturerData enum and match the LWP3
    // System Type + Device Number byte encoding where applicable.
    //
    // Reference: nathankellenicki/node-poweredup — src/consts.ts BLEManufacturerData

    /** BLE manufacturer data ID for the Duplo Train Base ({@code 32} = {@code 0x20}). */
    public static final int BLE_MFR_ID_DUPLO_TRAIN_BASE = 32;

    /** BLE manufacturer data ID for the Boost Move Hub ({@code 64} = {@code 0x40}). */
    public static final int BLE_MFR_ID_MOVE_HUB = 64;

    /** BLE manufacturer data ID for the 2-Port City Hub ({@code 65} = {@code 0x41}). */
    public static final int BLE_MFR_ID_HUB = 65;

    /** BLE manufacturer data ID for the Remote Control handset ({@code 66} = {@code 0x42}). */
    public static final int BLE_MFR_ID_REMOTE_CONTROL = 66;

    /** BLE manufacturer data ID for the Mario Hub ({@code 67} = {@code 0x43}). */
    public static final int BLE_MFR_ID_MARIO = 67;

    /**
     * BLE manufacturer data ID for the Technic Medium (Control+) Hub ({@code 128} = {@code 0x80}).
     *
     * <p>Note: this value differs from {@link #DEVICE_TECHNIC_HUB} ({@code 0x50}).
     * {@link #DEVICE_TECHNIC_HUB} encodes the LWP3 System Type + Device Number byte
     * as defined in the official LEGO BLE Wireless Protocol 3.0 specification.
     * This constant is the numeric hub-model identifier reported in the advertisement
     * payload as catalogued by node-poweredup and may be used in a different byte
     * position or protocol layer.
     */
    public static final int BLE_MFR_ID_TECHNIC_MEDIUM_HUB = 128;

    /** BLE manufacturer data ID for the Technic Small Hub ({@code 131} = {@code 0x83}). */
    public static final int BLE_MFR_ID_TECHNIC_SMALL_HUB = 131;

    /**
     * Byte index within the LEGO manufacturer-specific advertisement payload
     * (after stripping the AD Length and AD Type prefix) at which the System
     * Type and Device Number byte is located.
     *
     * <p>The 10-byte payload layout is: Length (1), Data Type 0xFF (1),
     * Manufacturer ID LSB (1), Manufacturer ID MSB (1),
     * Button State (1), <em>System Type + Device Number (1)</em>,
     * Device Capabilities (1), Last Network ID (1), Status (1), Option (1).
     * After stripping the AD Length and Data Type bytes the raw payload
     * starts at the Manufacturer ID; the System Type byte is therefore at
     * index {@value #MANUFACTURER_DATA_IDX_SYSTEM_TYPE}.
     */
    public static final int MANUFACTURER_DATA_IDX_SYSTEM_TYPE = 3;

    /**
     * Byte index within the LEGO manufacturer-specific advertisement payload
     * at which the Manufacturer ID LSB is located (value {@code 0x97}).
     */
    public static final int MANUFACTURER_DATA_IDX_MFR_ID_LSB = 0;

    /**
     * Byte index within the LEGO manufacturer-specific advertisement payload
     * at which the Manufacturer ID MSB is located (value {@code 0x03}).
     */
    public static final int MANUFACTURER_DATA_IDX_MFR_ID_MSB = 1;

    /**
     * Expected LSB of the LEGO System A/S manufacturer ID ({@code 0x97})
     * as stored in the manufacturer-specific advertisement payload.
     */
    public static final int MANUFACTURER_ID_LSB = 0x97;

    /**
     * Expected MSB of the LEGO System A/S manufacturer ID ({@code 0x03})
     * as stored in the manufacturer-specific advertisement payload.
     */
    public static final int MANUFACTURER_ID_MSB = 0x03;

    /**
     * Minimum length of the LEGO manufacturer-specific advertisement payload
     * required to safely read the System Type and Device Number byte.
     */
    public static final int MANUFACTURER_DATA_MIN_LENGTH = 4;

    // =========================================================================
    // Common message header
    // =========================================================================

    /** Hub ID field value — always {@code 0x00} (reserved). */
    public static final int HUB_ID = 0x00;

    /**
     * Bit mask applied to the first length byte to detect two-byte encoding.
     * If bit 7 is set the length occupies two bytes.
     */
    public static final int LENGTH_TWO_BYTE_FLAG = 0x80;

    // =========================================================================
    // Device Type IDs (LWP3 §3.18 — IO Type ID / node-poweredup DeviceType)
    // =========================================================================
    //
    // These IDs are reported in Hub Attached I/O messages (message type 0x04)
    // and identify the peripheral connected to a port.
    //
    // Reference: LEGO BLE Wireless Protocol 3.0 §3.18 and
    //            nathankellenicki/node-poweredup — src/consts.ts DeviceType

    /** Device type: unknown / not identified ({@code 0}). */
    public static final int DEVICE_TYPE_UNKNOWN = 0;

    /** Device type: Simple / Medium Linear Motor ({@code 1}). */
    public static final int DEVICE_TYPE_SIMPLE_MEDIUM_LINEAR_MOTOR = 1;

    /** Device type: Train Motor ({@code 2}). */
    public static final int DEVICE_TYPE_TRAIN_MOTOR = 2;

    /** Device type: Light ({@code 8}). */
    public static final int DEVICE_TYPE_LIGHT = 8;

    /** Device type: Voltage Sensor ({@code 20}). */
    public static final int DEVICE_TYPE_VOLTAGE_SENSOR = 20;

    /** Device type: Current Sensor ({@code 21}). */
    public static final int DEVICE_TYPE_CURRENT_SENSOR = 21;

    /** Device type: Piezo Buzzer ({@code 22}). */
    public static final int DEVICE_TYPE_PIEZO_BUZZER = 22;

    /** Device type: Hub LED ({@code 23}). */
    public static final int DEVICE_TYPE_HUB_LED = 23;

    /** Device type: Tilt Sensor ({@code 34}). */
    public static final int DEVICE_TYPE_TILT_SENSOR = 34;

    /** Device type: Motion / Distance Sensor ({@code 35}). */
    public static final int DEVICE_TYPE_MOTION_SENSOR = 35;

    /** Device type: Color and Distance Sensor ({@code 37}). */
    public static final int DEVICE_TYPE_COLOR_DISTANCE_SENSOR = 37;

    /** Device type: Medium Linear Motor ({@code 38}). */
    public static final int DEVICE_TYPE_MEDIUM_LINEAR_MOTOR = 38;

    /** Device type: Move Hub Medium Linear Motor ({@code 39}). */
    public static final int DEVICE_TYPE_MOVE_HUB_MEDIUM_LINEAR_MOTOR = 39;

    /** Device type: Move Hub Tilt Sensor ({@code 40}). */
    public static final int DEVICE_TYPE_MOVE_HUB_TILT_SENSOR = 40;

    /** Device type: Duplo Train Base Motor ({@code 41}). */
    public static final int DEVICE_TYPE_DUPLO_TRAIN_BASE_MOTOR = 41;

    /** Device type: Duplo Train Base Speaker ({@code 42}). */
    public static final int DEVICE_TYPE_DUPLO_TRAIN_BASE_SPEAKER = 42;

    /** Device type: Duplo Train Base Color Sensor ({@code 43}). */
    public static final int DEVICE_TYPE_DUPLO_TRAIN_BASE_COLOR_SENSOR = 43;

    /** Device type: Duplo Train Base Speedometer ({@code 44}). */
    public static final int DEVICE_TYPE_DUPLO_TRAIN_BASE_SPEEDOMETER = 44;

    /** Device type: Technic Large Linear Motor / Control+ ({@code 46}). */
    public static final int DEVICE_TYPE_TECHNIC_LARGE_LINEAR_MOTOR = 46;

    /** Device type: Technic XLarge Linear Motor / Control+ ({@code 47}). */
    public static final int DEVICE_TYPE_TECHNIC_XLARGE_LINEAR_MOTOR = 47;

    /** Device type: Technic Medium Angular Motor / Spike Prime ({@code 48}). */
    public static final int DEVICE_TYPE_TECHNIC_MEDIUM_ANGULAR_MOTOR = 48;

    /** Device type: Technic Large Angular Motor / Spike Prime ({@code 49}). */
    public static final int DEVICE_TYPE_TECHNIC_LARGE_ANGULAR_MOTOR = 49;

    /** Device type: Technic Medium Hub Gesture Sensor ({@code 54}). */
    public static final int DEVICE_TYPE_TECHNIC_MEDIUM_HUB_GEST_SENSOR = 54;

    /** Device type: Remote Control Button ({@code 55}). */
    public static final int DEVICE_TYPE_REMOTE_CONTROL_BUTTON = 55;

    /** Device type: Remote Control RSSI ({@code 56}). */
    public static final int DEVICE_TYPE_REMOTE_CONTROL_RSSI = 56;

    /** Device type: Technic Medium Hub Accelerometer ({@code 57}). */
    public static final int DEVICE_TYPE_TECHNIC_MEDIUM_HUB_ACCELEROMETER = 57;

    /** Device type: Technic Medium Hub Gyro Sensor ({@code 58}). */
    public static final int DEVICE_TYPE_TECHNIC_MEDIUM_HUB_GYRO_SENSOR = 58;

    /** Device type: Technic Medium Hub Tilt Sensor ({@code 59}). */
    public static final int DEVICE_TYPE_TECHNIC_MEDIUM_HUB_TILT_SENSOR = 59;

    /** Device type: Technic Medium Hub Temperature Sensor ({@code 60}). */
    public static final int DEVICE_TYPE_TECHNIC_MEDIUM_HUB_TEMPERATURE_SENSOR = 60;

    /** Device type: Technic Color Sensor / Spike Prime ({@code 61}). */
    public static final int DEVICE_TYPE_TECHNIC_COLOR_SENSOR = 61;

    /** Device type: Technic Distance Sensor / Spike Prime ({@code 62}). */
    public static final int DEVICE_TYPE_TECHNIC_DISTANCE_SENSOR = 62;

    /** Device type: Technic Force Sensor / Spike Prime ({@code 63}). */
    public static final int DEVICE_TYPE_TECHNIC_FORCE_SENSOR = 63;

    /** Device type: Technic 3×3 Color Light Matrix / Spike Essential ({@code 64}). */
    public static final int DEVICE_TYPE_TECHNIC_3X3_COLOR_LIGHT_MATRIX = 64;

    /** Device type: Technic Small Angular Motor / Spike Essential ({@code 65}). */
    public static final int DEVICE_TYPE_TECHNIC_SMALL_ANGULAR_MOTOR = 65;

    /** Device type: Mario Accelerometer ({@code 71}). */
    public static final int DEVICE_TYPE_MARIO_ACCELEROMETER = 71;

    /** Device type: Mario Barcode Sensor ({@code 73}). */
    public static final int DEVICE_TYPE_MARIO_BARCODE_SENSOR = 73;

    /** Device type: Mario Pants Sensor ({@code 74}). */
    public static final int DEVICE_TYPE_MARIO_PANTS_SENSOR = 74;

    /** Device type: Technic Medium Angular Motor Grey / Mindstorms ({@code 75}). */
    public static final int DEVICE_TYPE_TECHNIC_MEDIUM_ANGULAR_MOTOR_GREY = 75;

    /** Device type: Technic Large Angular Motor Grey / Control+ ({@code 76}). */
    public static final int DEVICE_TYPE_TECHNIC_LARGE_ANGULAR_MOTOR_GREY = 76;

    // =========================================================================
    // IO Type IDs (LWP3 §3.18 — low-level hardware type identifiers)
    // =========================================================================
    //
    // These 16-bit identifiers are the raw IO Type IDs reported in Port
    // Information messages and map directly to the LWP3 §3.18 table.
    //
    // Reference: LEGO BLE Wireless Protocol 3.0 §3.18 and
    //            nathankellenicki/node-poweredup — src/consts.ts IOTypeID

    /** IO Type: Motor ({@code 0x0001}). */
    public static final int IO_TYPE_MOTOR = 0x0001;

    /** IO Type: System Train Motor ({@code 0x0002}). */
    public static final int IO_TYPE_SYSTEM_TRAIN_MOTOR = 0x0002;

    /** IO Type: Button ({@code 0x0005}). */
    public static final int IO_TYPE_BUTTON = 0x0005;

    /** IO Type: LED Light ({@code 0x0008}). */
    public static final int IO_TYPE_LED_LIGHT = 0x0008;

    /** IO Type: Voltage sensor ({@code 0x0014}). */
    public static final int IO_TYPE_VOLTAGE = 0x0014;

    /** IO Type: Current sensor ({@code 0x0015}). */
    public static final int IO_TYPE_CURRENT = 0x0015;

    /** IO Type: Piezo Tone / Sound ({@code 0x0016}). */
    public static final int IO_TYPE_PIEZO_TONE_SOUND = 0x0016;

    /** IO Type: RGB Light ({@code 0x0017}). */
    public static final int IO_TYPE_RGB_LIGHT = 0x0017;

    /** IO Type: External Tilt Sensor ({@code 0x0022}). */
    public static final int IO_TYPE_EXTERNAL_TILT_SENSOR = 0x0022;

    /** IO Type: Motion Sensor ({@code 0x0023}). */
    public static final int IO_TYPE_MOTION_SENSOR = 0x0023;

    /** IO Type: Vision / Color-Distance Sensor ({@code 0x0025}). */
    public static final int IO_TYPE_VISION_SENSOR = 0x0025;

    /** IO Type: External Motor ({@code 0x0026}). */
    public static final int IO_TYPE_EXTERNAL_MOTOR = 0x0026;

    /** IO Type: Internal Motor ({@code 0x0027}). */
    public static final int IO_TYPE_INTERNAL_MOTOR = 0x0027;

    /** IO Type: Internal Tilt ({@code 0x0028}). */
    public static final int IO_TYPE_INTERNAL_TILT = 0x0028;

    // =========================================================================
    // Color constants (LWP3 LED colour index)
    // =========================================================================
    //
    // These indices are used when setting LED colour via the Hub LED device
    // (device type {@link #DEVICE_TYPE_HUB_LED}).
    //
    // Reference: nathankellenicki/node-poweredup — src/consts.ts Color

    /** LED colour: Black ({@code 0}). */
    public static final int COLOR_BLACK = 0;

    /** LED colour: Pink ({@code 1}). */
    public static final int COLOR_PINK = 1;

    /** LED colour: Purple ({@code 2}). */
    public static final int COLOR_PURPLE = 2;

    /** LED colour: Blue ({@code 3}). */
    public static final int COLOR_BLUE = 3;

    /** LED colour: Light Blue ({@code 4}). */
    public static final int COLOR_LIGHT_BLUE = 4;

    /** LED colour: Cyan ({@code 5}). */
    public static final int COLOR_CYAN = 5;

    /** LED colour: Green ({@code 6}). */
    public static final int COLOR_GREEN = 6;

    /** LED colour: Yellow ({@code 7}). */
    public static final int COLOR_YELLOW = 7;

    /** LED colour: Orange ({@code 8}). */
    public static final int COLOR_ORANGE = 8;

    /** LED colour: Red ({@code 9}). */
    public static final int COLOR_RED = 9;

    /** LED colour: White ({@code 10}). */
    public static final int COLOR_WHITE = 10;

    /** LED colour: None / off ({@code 255}). */
    public static final int COLOR_NONE = 255;

    // =========================================================================
    // Button State constants
    // =========================================================================
    //
    // These values are reported in Hub Property Update payloads when the hub
    // button property ({@link #HUB_PROP_BUTTON}) is subscribed.
    //
    // Reference: nathankellenicki/node-poweredup — src/consts.ts ButtonState

    /** Button state: Released ({@code 0}). */
    public static final int BUTTON_STATE_RELEASED = 0;

    /** Button state: Up ({@code 1}) — used by Remote Control handset. */
    public static final int BUTTON_STATE_UP = 1;

    /** Button state: Pressed ({@code 2}). */
    public static final int BUTTON_STATE_PRESSED = 2;

    /** Button state: Stop ({@code 127}) — used by Remote Control handset. */
    public static final int BUTTON_STATE_STOP = 127;

    /** Button state: Down ({@code 255}) — used by Remote Control handset. */
    public static final int BUTTON_STATE_DOWN = 255;

    // =========================================================================
    // Braking Style constants (LWP3 motor end-state / braking mode)
    // =========================================================================
    //
    // These values are passed in the end-state field of motor output commands.
    //
    // Reference: nathankellenicki/node-poweredup — src/consts.ts BrakingStyle

    /** Braking style: Float / coast (motor allowed to spin freely, {@code 0}). */
    public static final int BRAKING_STYLE_FLOAT = 0;

    /** Braking style: Hold position (motor actively holds current position, {@code 126}). */
    public static final int BRAKING_STYLE_HOLD = 126;

    /** Braking style: Brake (motor actively brakes to stop, {@code 127}). */
    public static final int BRAKING_STYLE_BRAKE = 127;

    // =========================================================================
    // Error Codes (LWP3 §3.12 — Generic Error Messages)
    // =========================================================================
    //
    // These codes are carried in Generic Error messages (message type 0x05).
    //
    // Reference: LEGO BLE Wireless Protocol 3.0 §3.12 and
    //            nathankellenicki/node-poweredup — src/consts.ts ErrorCode

    /** Error code: ACK — command acknowledged ({@code 0x01}). */
    public static final int ERROR_CODE_ACK = 0x01;

    /** Error code: MACK — message acknowledged ({@code 0x02}). */
    public static final int ERROR_CODE_MACK = 0x02;

    /** Error code: Buffer overflow ({@code 0x03}). */
    public static final int ERROR_CODE_BUFFER_OVERFLOW = 0x03;

    /** Error code: Timeout ({@code 0x04}). */
    public static final int ERROR_CODE_TIMEOUT = 0x04;

    /** Error code: Command not recognised ({@code 0x05}). */
    public static final int ERROR_CODE_COMMAND_NOT_RECOGNIZED = 0x05;

    /** Error code: Invalid use ({@code 0x06}). */
    public static final int ERROR_CODE_INVALID_USE = 0x06;

    /** Error code: Overcurrent ({@code 0x07}). */
    public static final int ERROR_CODE_OVERCURRENT = 0x07;

    /** Error code: Internal error ({@code 0x08}). */
    public static final int ERROR_CODE_INTERNAL_ERROR = 0x08;

    // =========================================================================
    // Command Feedback (LWP3 §3.28 — Port Output Command Feedback)
    // =========================================================================
    //
    // These values appear in Port Output Command Feedback messages
    // (message type {@link #MSG_PORT_OUTPUT_COMMAND_FEEDBACK}).
    //
    // Reference: nathankellenicki/node-poweredup — src/consts.ts CommandFeedback

    /** Feedback: Transmission pending — waiting for previous command ({@code 0x00}). */
    public static final int FEEDBACK_TRANSMISSION_PENDING = 0x00;

    /** Feedback: Transmission busy — waiting for device ACK ({@code 0x10}). */
    public static final int FEEDBACK_TRANSMISSION_BUSY = 0x10;

    /** Feedback: Transmission discarded — interrupt received ({@code 0x44}). */
    public static final int FEEDBACK_TRANSMISSION_DISCARDED = 0x44;

    /** Feedback: Execution pending — waiting for previous command to complete ({@code 0x20}). */
    public static final int FEEDBACK_EXECUTION_PENDING = 0x20;

    /** Feedback: Execution busy — device is executing command ({@code 0x21}). */
    public static final int FEEDBACK_EXECUTION_BUSY = 0x21;

    /** Feedback: Execution completed — device reported success ({@code 0x22}). */
    public static final int FEEDBACK_EXECUTION_COMPLETED = 0x22;

    /** Feedback: Execution discarded — device discarded command ({@code 0x24}). */
    public static final int FEEDBACK_EXECUTION_DISCARDED = 0x24;

    /** Feedback: Feedback disabled — not implemented for this command ({@code 0x26}). */
    public static final int FEEDBACK_DISABLED = 0x26;

    /** Feedback: Feedback missing — device disconnected or failed to report ({@code 0x66}). */
    public static final int FEEDBACK_MISSING = 0x66;

    // =========================================================================
    // Mode Information Types (LWP3 §3.20.4)
    // =========================================================================
    //
    // Used in Port Mode Information Request messages (message type 0x22) to
    // specify which mode-information field to query.
    //
    // Reference: LEGO BLE Wireless Protocol 3.0 §3.20.4 and
    //            nathankellenicki/node-poweredup — src/consts.ts ModeInformationType

    /** Mode information type: Name ({@code 0x00}). */
    public static final int MODE_INFO_NAME = 0x00;

    /** Mode information type: RAW value range ({@code 0x01}). */
    public static final int MODE_INFO_RAW = 0x01;

    /** Mode information type: Percent value range ({@code 0x02}). */
    public static final int MODE_INFO_PCT = 0x02;

    /** Mode information type: SI value range ({@code 0x03}). */
    public static final int MODE_INFO_SI = 0x03;

    /** Mode information type: Symbol string ({@code 0x04}). */
    public static final int MODE_INFO_SYMBOL = 0x04;

    /** Mode information type: Mapping flags ({@code 0x05}). */
    public static final int MODE_INFO_MAPPING = 0x05;

    /** Mode information type: Used internally ({@code 0x06}). */
    public static final int MODE_INFO_USED_INTERNALLY = 0x06;

    /** Mode information type: Motor bias ({@code 0x07}). */
    public static final int MODE_INFO_MOTOR_BIAS = 0x07;

    /** Mode information type: Capability bits ({@code 0x08}). */
    public static final int MODE_INFO_CAPABILITY_BITS = 0x08;

    /** Mode information type: Value format ({@code 0x80}). */
    public static final int MODE_INFO_VALUE_FORMAT = 0x80;

    // =========================================================================
    // H/W Network Command Types (LWP3 §3.8)
    // =========================================================================
    //
    // Sub-command types within H/W Network Command messages
    // (message type {@link #MSG_HW_NETWORK_COMMANDS}).
    //
    // Reference: LEGO BLE Wireless Protocol 3.0 §3.8 and
    //            nathankellenicki/node-poweredup — src/consts.ts HWNetWorkCommandType

    /** H/W Network command: Connection Request ({@code 0x02}). */
    public static final int HW_NET_CMD_CONNECTION_REQUEST = 0x02;

    /** H/W Network command: Family Request ({@code 0x03}). */
    public static final int HW_NET_CMD_FAMILY_REQUEST = 0x03;

    /** H/W Network command: Family Set ({@code 0x04}). */
    public static final int HW_NET_CMD_FAMILY_SET = 0x04;

    /** H/W Network command: Join Denied ({@code 0x05}). */
    public static final int HW_NET_CMD_JOIN_DENIED = 0x05;

    /** H/W Network command: Get Family ({@code 0x06}). */
    public static final int HW_NET_CMD_GET_FAMILY = 0x06;

    /** H/W Network command: Family ({@code 0x07}). */
    public static final int HW_NET_CMD_FAMILY = 0x07;

    /** H/W Network command: Get Sub-Family ({@code 0x08}). */
    public static final int HW_NET_CMD_GET_SUBFAMILY = 0x08;

    /** H/W Network command: Sub-Family ({@code 0x09}). */
    public static final int HW_NET_CMD_SUBFAMILY = 0x09;

    /** H/W Network command: Sub-Family Set ({@code 0x0A}). */
    public static final int HW_NET_CMD_SUBFAMILY_SET = 0x0A;

    /** H/W Network command: Get Extended Family ({@code 0x0B}). */
    public static final int HW_NET_CMD_GET_EXTENDED_FAMILY = 0x0B;

    /** H/W Network command: Extended Family ({@code 0x0C}). */
    public static final int HW_NET_CMD_EXTENDED_FAMILY = 0x0C;

    /** H/W Network command: Extended Family Set ({@code 0x0D}). */
    public static final int HW_NET_CMD_EXTENDED_FAMILY_SET = 0x0D;

    /** H/W Network command: Reset Long-Press Timing ({@code 0x0E}). */
    public static final int HW_NET_CMD_RESET_LONG_PRESS_TIMING = 0x0E;

    // =========================================================================
    // Port Input Format Setup Sub-Commands (LWP3 §3.19.4)
    // =========================================================================
    //
    // Used in Port Input Format Setup (Combined Mode) messages
    // (message type {@link #MSG_PORT_INPUT_FORMAT_SETUP_COMBINED}).
    //
    // Reference: LEGO BLE Wireless Protocol 3.0 §3.19.4 and
    //            nathankellenicki/node-poweredup — src/consts.ts PortInputFormatSetupSubCommand

    /** Sub-command: Set Mode and Dataset Combinations ({@code 0x01}). */
    public static final int PORT_INPUT_SUB_CMD_SET_MODE_AND_DATASET_COMBINATIONS = 0x01;

    /** Sub-command: Lock LPF2 Device for Setup ({@code 0x02}). */
    public static final int PORT_INPUT_SUB_CMD_LOCK_LPF2_DEVICE_FOR_SETUP = 0x02;

    /** Sub-command: Unlock and Start with Multi-Update Enabled ({@code 0x03}). */
    public static final int PORT_INPUT_SUB_CMD_UNLOCK_AND_START_MULTI_UPDATE_ENABLED = 0x03;

    /** Sub-command: Unlock and Start with Multi-Update Disabled ({@code 0x04}). */
    public static final int PORT_INPUT_SUB_CMD_UNLOCK_AND_START_MULTI_UPDATE_DISABLED = 0x04;

    /** Sub-command: Not Used ({@code 0x05}). */
    public static final int PORT_INPUT_SUB_CMD_NOT_USED = 0x05;

    /** Sub-command: Reset Sensor ({@code 0x06}). */
    public static final int PORT_INPUT_SUB_CMD_RESET_SENSOR = 0x06;

    // =========================================================================
    // Duplo Train Base Sound IDs
    // =========================================================================
    //
    // These IDs are sent as the payload of a WriteDirectModeData command
    // ({@link #MOTOR_CMD_WRITE_DIRECT_MODE_DATA}) to the Duplo Train Base
    // Speaker device type ({@link #DEVICE_TYPE_DUPLO_TRAIN_BASE_SPEAKER}).
    //
    // Reference: nathankellenicki/node-poweredup — src/consts.ts DuploTrainBaseSound

    /** Duplo Train Base sound: Brake ({@code 3}). */
    public static final int DUPLO_SOUND_BRAKE = 3;

    /** Duplo Train Base sound: Station Departure ({@code 5}). */
    public static final int DUPLO_SOUND_STATION_DEPARTURE = 5;

    /** Duplo Train Base sound: Water Refill ({@code 7}). */
    public static final int DUPLO_SOUND_WATER_REFILL = 7;

    /** Duplo Train Base sound: Horn ({@code 9}). */
    public static final int DUPLO_SOUND_HORN = 9;

    /** Duplo Train Base sound: Steam ({@code 10}). */
    public static final int DUPLO_SOUND_STEAM = 10;

    // =========================================================================
    // Hub virtual port IDs (per hub — internal sensors and LEDs)
    // =========================================================================
    //
    // These port IDs are not physical connectors; they address the built-in
    // sensors and LED of each hub model.
    //
    // Reference: nathankellenicki/node-poweredup hub source files

    // ── Move Hub (Boost) ──────────────────────────────────────────────────────

    /** Move Hub: physical port A ({@code 0}). */
    public static final int MOVE_HUB_PORT_A = 0x00;

    /** Move Hub: physical port B ({@code 1}). */
    public static final int MOVE_HUB_PORT_B = 0x01;

    /** Move Hub: physical port C ({@code 2}). */
    public static final int MOVE_HUB_PORT_C = 0x02;

    /** Move Hub: physical port D ({@code 3}). */
    public static final int MOVE_HUB_PORT_D = 0x03;

    /** Move Hub: internal Hub LED ({@code 50}). */
    public static final int MOVE_HUB_PORT_LED = 0x32;

    /** Move Hub: internal Tilt Sensor ({@code 58}). */
    public static final int MOVE_HUB_PORT_TILT_SENSOR = 0x3A;

    /** Move Hub: internal Current Sensor ({@code 59}). */
    public static final int MOVE_HUB_PORT_CURRENT_SENSOR = 0x3B;

    /** Move Hub: internal Voltage Sensor ({@code 60}). */
    public static final int MOVE_HUB_PORT_VOLTAGE_SENSOR = 0x3C;

    // ── City Hub (2-Port Hub) ─────────────────────────────────────────────────

    /** City Hub: physical port A ({@code 0}). */
    public static final int CITY_HUB_PORT_A = 0x00;

    /** City Hub: physical port B ({@code 1}). */
    public static final int CITY_HUB_PORT_B = 0x01;

    /** City Hub: internal Hub LED ({@code 50}). */
    public static final int CITY_HUB_PORT_LED = 0x32;

    /** City Hub: internal Current Sensor ({@code 59}). */
    public static final int CITY_HUB_PORT_CURRENT_SENSOR = 0x3B;

    /** City Hub: internal Voltage Sensor ({@code 60}). */
    public static final int CITY_HUB_PORT_VOLTAGE_SENSOR = 0x3C;

    // ── Remote Control handset ────────────────────────────────────────────────

    /** Remote Control: left button port ({@code 0}). */
    public static final int REMOTE_PORT_LEFT = 0x00;

    /** Remote Control: right button port ({@code 1}). */
    public static final int REMOTE_PORT_RIGHT = 0x01;

    /** Remote Control: internal Hub LED ({@code 52}). */
    public static final int REMOTE_PORT_LED = 0x34;

    /** Remote Control: internal Voltage Sensor ({@code 59}). */
    public static final int REMOTE_PORT_VOLTAGE_SENSOR = 0x3B;

    /** Remote Control: RSSI sensor ({@code 60}). */
    public static final int REMOTE_PORT_RSSI = 0x3C;

    // ── Technic Hub (Control+) ────────────────────────────────────────────────

    /** Technic Hub: physical port A ({@code 0}). */
    public static final int TECHNIC_HUB_PORT_A = 0x00;

    /** Technic Hub: physical port B ({@code 1}). */
    public static final int TECHNIC_HUB_PORT_B = 0x01;

    /** Technic Hub: physical port C ({@code 2}). */
    public static final int TECHNIC_HUB_PORT_C = 0x02;

    /** Technic Hub: physical port D ({@code 3}). */
    public static final int TECHNIC_HUB_PORT_D = 0x03;

    /** Technic Hub: internal Hub LED ({@code 50}). */
    public static final int TECHNIC_HUB_PORT_LED = 0x32;

    /** Technic Hub: internal Current Sensor ({@code 59}). */
    public static final int TECHNIC_HUB_PORT_CURRENT_SENSOR = 0x3B;

    /** Technic Hub: internal Voltage Sensor ({@code 60}). */
    public static final int TECHNIC_HUB_PORT_VOLTAGE_SENSOR = 0x3C;

    /** Technic Hub: internal Accelerometer ({@code 97}). */
    public static final int TECHNIC_HUB_PORT_ACCELEROMETER = 0x61;

    /** Technic Hub: internal Gyro Sensor ({@code 98}). */
    public static final int TECHNIC_HUB_PORT_GYRO_SENSOR = 0x62;

    /** Technic Hub: internal Tilt Sensor ({@code 99}). */
    public static final int TECHNIC_HUB_PORT_TILT_SENSOR = 0x63;

    // ── WeDo 2.0 — additional virtual ports ───────────────────────────────────

    /** WeDo 2.0 internal Current Sensor port ({@code 3}). */
    public static final int WEDO2_PORT_CURRENT_SENSOR = 0x03;

    /** WeDo 2.0 internal Voltage Sensor port ({@code 4}). */
    public static final int WEDO2_PORT_VOLTAGE_SENSOR = 0x04;

    /** WeDo 2.0 internal Piezo Buzzer port ({@code 5}). */
    public static final int WEDO2_PORT_PIEZO_BUZZER = 0x05;

    /** WeDo 2.0 internal Hub LED port ({@code 6}). */
    public static final int WEDO2_PORT_LED = 0x06;

    // ── WeDo 2.0 — Device Information service and Firmware Revision ───────────

    /**
     * Standard BLE Device Information Service UUID ({@code 0x180A}).
     *
     * <p>Present on the WeDo 2.0 hub (and many other BLE devices).
     * Contains characteristics such as Firmware Revision String.
     */
    public static final String WEDO2_DEVICE_INFO_SERVICE_UUID =
            "0000180a-0000-1000-8000-00805f9b34fb";

    /**
     * Standard BLE Firmware Revision String characteristic UUID ({@code 0x2A26}).
     *
     * <p>Read to get the firmware version string from the WeDo 2.0 hub.
     * Reference: nathankellenicki/node-poweredup — src/consts.ts
     * {@code BLECharacteristic.WEDO2_FIRMWARE_REVISION}.
     */
    public static final String WEDO2_FIRMWARE_REVISION_UUID =
            "00002a26-0000-1000-8000-00805f9b34fb";

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Private constructor — this is a constants class with no instances.
     */
    private LegoProtocolConstants() {
        throw new AssertionError("LegoProtocolConstants must not be instantiated");
    }
}
