package ch.varani.bricks.ble.device.circuitcubes;

/**
 * Constants for the Circuit Cubes (Tenka) BLE motor control protocol.
 *
 * <p>Reference:
 * <a href="https://github.com/made-by-simon/circuit-cubes-python-interface">
 * Circuit Cubes Python Interface</a>
 *
 * <p>All commands are ASCII strings written to the TX characteristic. Responses
 * (e.g. battery voltage) arrive as UTF-8 strings on the RX characteristic.
 */
public final class CircuitCubesProtocolConstants {

    // =========================================================================
    // Discovery
    // =========================================================================

    /** Advertised BLE device name for Circuit Cubes. */
    public static final String DEVICE_NAME = "Tenka";

    // =========================================================================
    // GATT Service UUID (Nordic UART Service base)
    // =========================================================================

    /** UUID of the Nordic UART Service (NUS) used by Circuit Cubes. */
    public static final String NUS_SERVICE_UUID =
            "6e400001-b5a3-f393-e0a9-e50e24dcca9e";

    // =========================================================================
    // GATT Characteristic UUIDs
    // =========================================================================

    /**
     * UUID of the TX characteristic (write-without-response).
     * Commands are written here to control motors.
     */
    public static final String TX_CHARACTERISTIC_UUID =
            "6e400002-b5a3-f393-e0a9-e50e24dcca9e";

    /**
     * UUID of the RX characteristic (notify).
     * Battery voltage responses arrive here.
     */
    public static final String RX_CHARACTERISTIC_UUID =
            "6e400003-b5a3-f393-e0a9-e50e24dcca9e";

    // =========================================================================
    // Motor channel letters
    // =========================================================================

    /** Channel A motor letter (ASCII 'a'). */
    public static final char CHANNEL_A = 'a';

    /** Channel B motor letter (ASCII 'b'). */
    public static final char CHANNEL_B = 'b';

    /** Channel C motor letter (ASCII 'c'). */
    public static final char CHANNEL_C = 'c';

    // =========================================================================
    // Command sign characters
    // =========================================================================

    /** Sign character for forward / positive direction. */
    public static final char SIGN_FORWARD = '+';

    /** Sign character for reverse / negative direction. */
    public static final char SIGN_REVERSE = '-';

    // =========================================================================
    // Magnitude encoding
    // =========================================================================

    /**
     * Magnitude value that represents a stopped motor.
     *
     * <p>Command wire format: {@code "000"}.
     */
    public static final int MAGNITUDE_STOP = 0;

    /**
     * Minimum non-zero magnitude value.
     *
     * <p>For a non-zero internal velocity {@code v} (range 1–200):
     * {@code magnitude = MAGNITUDE_MIN_NONZERO + abs(v)}.
     */
    public static final int MAGNITUDE_MIN_NONZERO = 55;

    /**
     * Maximum magnitude value (full speed).
     *
     * <p>Internal velocity range is −200 to +200; maximum magnitude = 55 + 200 = 255.
     */
    public static final int MAGNITUDE_MAX = 255;

    /**
     * Maximum internal velocity (absolute value), used when encoding magnitudes.
     */
    public static final int MAX_INTERNAL_VELOCITY = 200;

    // =========================================================================
    // Battery query command
    // =========================================================================

    /**
     * ASCII byte to write to TX in order to request the battery voltage.
     * The response is a UTF-8 string on the RX characteristic.
     */
    public static final byte BATTERY_QUERY_COMMAND = (byte) 'b';

    // =========================================================================
    // Constructor
    // =========================================================================

    /**
     * Private constructor — this is a constants class with no instances.
     */
    private CircuitCubesProtocolConstants() {
        throw new AssertionError("CircuitCubesProtocolConstants must not be instantiated");
    }
}
