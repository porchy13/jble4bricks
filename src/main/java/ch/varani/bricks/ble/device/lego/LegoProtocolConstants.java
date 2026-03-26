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
     */
    public static final String WEDO2_SENSOR_VALUE_UUID =
            "00001560-1212-efde-1523-785feabcd123";

    /** Sensor value-format characteristic (write). */
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

    /** Hub property: System Type ID. */
    public static final int HUB_PROP_SYSTEM_TYPE_ID = 0x0B;

    /** Hub property: Primary MAC Address. */
    public static final int HUB_PROP_PRIMARY_MAC = 0x0D;

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
    // Constructor
    // =========================================================================

    /**
     * Private constructor — this is a constants class with no instances.
     */
    private LegoProtocolConstants() {
        throw new AssertionError("LegoProtocolConstants must not be instantiated");
    }
}
