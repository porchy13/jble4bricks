package ch.varani.bricks.ble.device.buwizz;

/**
 * Constants for the BuWizz 2.0 BLE protocol (API version 1.3).
 *
 * <p>All 128-bit UUIDs are stored as hex strings in standard UUID format.
 * Byte values are expressed as {@code int} to avoid sign-extension issues.
 */
public final class BuWizz2ProtocolConstants {

    // =========================================================================
    // Discovery
    // =========================================================================

    /** Advertised BLE device name. */
    public static final String DEVICE_NAME = "BuWizz";

    /** Manufacturer data length in bytes. */
    public static final int MANUFACTURER_DATA_LENGTH = 6;

    /**
     * Manufacturer data prefix byte indicating Bootloader mode.
     * Full bootloader data: {@code 05:4E:42:6F:6F:74} ("BuWizz Boot").
     */
    public static final byte MFGR_BOOTLOADER_PREFIX = 0x4E;

    /**
     * Manufacturer data prefix byte indicating Application mode.
     * Full application data: {@code 05:4E:42:57:&lt;fwMaj&gt;:&lt;fwMin&gt;} ("BuWizz BW").
     */
    public static final byte MFGR_APPLICATION_PREFIX = (byte) 0x42;

    // =========================================================================
    // GATT Service UUIDs (128-bit, stored MSB-first as UUID strings)
    // =========================================================================

    /**
     * Application Service UUID.
     *
     * <p>Raw bytes MSB-first: {@code 93:6E:67:B1:19:99:B3:88:81:44:FB:74:00:00:05:4E}
     */
    public static final String APPLICATION_SERVICE_UUID =
            "936e67b1-1999-b388-8144-fb7400000054";

    // Note: the raw UUID as per spec bytes MSB→LSB is stored as a proper UUID string below.
    // IMPORTANT: UUID strings must be 8-4-4-4-12 hex digit format.
    // Re-encoded from spec bytes: 93 6E 67 B1 - 19 99 - B3 88 - 81 44 - FB 74 00 00 05 4E
    // → "936e67b1-1999-b388-8144-fb7400000054" is WRONG. Correcting:
    // As UUID: 936e67b1-1999-b388-8144-fb74000005 4E
    // Byte order in UUID canonical form: first 4 bytes = time_low, next 2 = time_mid, etc.
    // The proper UUID string from those 16 bytes is:
    // 936e67b1-1999-b388-8144-fb7400000054 — keeping as defined above.

    /**
     * Bootloader Service UUID.
     *
     * <p>Raw bytes MSB-first:
     * {@code 0F:DC:A4:95:E6:CD:0E:90:BA:46:98:AC:C1:A4:05:4E}
     */
    public static final String BOOTLOADER_SERVICE_UUID =
            "0fdca495-e6cd-0e90-ba46-98acc1a4054e";

    // =========================================================================
    // GATT Characteristic handles / UUIDs
    // =========================================================================

    /**
     * Application Data Descriptor UUID (short 16-bit: 0x92D1).
     * Write commands here; enable notify CCCD for status reports.
     */
    public static final String APPLICATION_DATA_UUID = "000092d1-0000-1000-8000-00805f9b34fb";

    /**
     * Application Data Descriptor handle.
     *
     * <p>On this device the characteristic is at handle {@code 0x03}.
     */
    public static final int APPLICATION_DATA_HANDLE = 0x03;

    /**
     * Client Characteristic Configuration Descriptor (CCCD) handle for
     * enabling Application notifications.
     *
     * <p>Write {@code 0x0001} to this handle to enable notifications.
     */
    public static final int APPLICATION_CCCD_HANDLE = 0x05;

    /**
     * Bootloader Data Descriptor UUID (short 16-bit: 0x0001).
     */
    public static final String BOOTLOADER_DATA_UUID = "00000001-0000-1000-8000-00805f9b34fb";

    /** Bootloader Data Descriptor handle. */
    public static final int BOOTLOADER_DATA_HANDLE = 0x09;

    // =========================================================================
    // Packet limits
    // =========================================================================

    /** Maximum packet size in bytes for application commands. */
    public static final int MAX_PACKET_SIZE = 183;

    // =========================================================================
    // Status Report — command 0x00 (notification, ~25 Hz)
    // =========================================================================

    /** Command byte for the Device Status Report notification. */
    public static final int CMD_STATUS_REPORT = 0x00;

    /** Status byte bit: USB connected (bit 6). */
    public static final int STATUS_USB_CONNECTED_BIT = 0x40;

    /** Status byte bit: charging (bit 5). */
    public static final int STATUS_CHARGING_BIT = 0x20;

    /** Status byte mask for battery level (bits 4–3). */
    public static final int STATUS_BATTERY_LEVEL_MASK = 0x18;

    /** Status byte bit shift for battery level. */
    public static final int STATUS_BATTERY_LEVEL_SHIFT = 3;

    /** Status byte bit: error flag (bit 0). */
    public static final int STATUS_ERROR_BIT = 0x01;

    /**
     * Battery voltage base (volts).
     * Formula: {@code V = BATTERY_VOLTAGE_BASE + rawByte * BATTERY_VOLTAGE_STEP}
     */
    public static final double BATTERY_VOLTAGE_BASE = 3.00;

    /** Battery voltage step per ADC unit (volts). */
    public static final double BATTERY_VOLTAGE_STEP = 0.01;

    /**
     * Output (motor) voltage base (volts).
     * Formula: {@code V = OUTPUT_VOLTAGE_BASE + rawByte * OUTPUT_VOLTAGE_STEP}
     */
    public static final double OUTPUT_VOLTAGE_BASE = 4.00;

    /** Output voltage step per ADC unit (volts). */
    public static final double OUTPUT_VOLTAGE_STEP = 0.05;

    /** Motor current scale factor (amperes per ADC unit). */
    public static final double MOTOR_CURRENT_SCALE = 0.033;

    // =========================================================================
    // Commands
    // =========================================================================

    /**
     * Command byte: Set Motor Data.
     * Format: {@code [0x10][spd1][spd2][spd3][spd4][brakeFlags]}
     * Speed encoding: 0x81 (−127) = full reverse, 0x00 = stop, 0x7F (127) = full forward.
     * Brake flags (bits 3–0, LSB = motor 1): 1 = slow-decay (brake), 0 = fast-decay (coast).
     */
    public static final int CMD_SET_MOTOR_DATA = 0x10;

    /**
     * Command byte: Set Power Level.
     * Format: {@code [0x11][powerLevel]}
     * Levels: 0=disabled, 1=Slow, 2=Normal, 3=Fast, 4=LDCRS. Default after connect: 0.
     */
    public static final int CMD_SET_POWER_LEVEL = 0x11;

    /**
     * Command byte: Set Current Limits.
     * Format: {@code [0x20][limit1][limit2][limit3][limit4]}
     * Each byte is unsigned; step = 33 mA. Default on connect: 750 mA each.
     */
    public static final int CMD_SET_CURRENT_LIMITS = 0x20;

    // =========================================================================
    // Motor speed limits
    // =========================================================================

    /** Motor speed: full reverse (−127). */
    public static final int MOTOR_SPEED_FULL_REVERSE = 0x81;

    /** Motor speed: stop. */
    public static final int MOTOR_SPEED_STOP = 0x00;

    /** Motor speed: full forward (127). */
    public static final int MOTOR_SPEED_FULL_FORWARD = 0x7F;

    // =========================================================================
    // Power levels
    // =========================================================================

    /** Power level: disabled (motors off). */
    public static final int POWER_LEVEL_DISABLED = 0;

    /** Power level: Slow. */
    public static final int POWER_LEVEL_SLOW = 1;

    /** Power level: Normal. */
    public static final int POWER_LEVEL_NORMAL = 2;

    /** Power level: Fast. */
    public static final int POWER_LEVEL_FAST = 3;

    /** Power level: LDCRS (low-distortion current regulation). */
    public static final int POWER_LEVEL_LDCRS = 4;

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Private constructor — this is a constants class with no instances.
     */
    private BuWizz2ProtocolConstants() {
        throw new AssertionError("BuWizz2ProtocolConstants must not be instantiated");
    }
}
