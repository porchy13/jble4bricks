package ch.varani.bricks.ble.device.circuitcubes;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link CircuitCubesProtocolConstants}.
 */
class CircuitCubesProtocolConstantsTest {

    @Test
    void constructor_isPrivate_throwsAssertionError() throws Exception {
        final Constructor<CircuitCubesProtocolConstants> ctor =
                CircuitCubesProtocolConstants.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final InvocationTargetException ex =
                assertThrows(InvocationTargetException.class, ctor::newInstance);
        assertThrows(AssertionError.class, () -> {
            throw ex.getCause();
        });
    }

    @Test
    void deviceName_isCorrect() {
        assertEquals("Tenka", CircuitCubesProtocolConstants.DEVICE_NAME);
    }

    @Test
    void nusServiceUuid_hasExpectedValue() {
        assertEquals(
                "6e400001-b5a3-f393-e0a9-e50e24dcca9e",
                CircuitCubesProtocolConstants.NUS_SERVICE_UUID
        );
    }

    @Test
    void characteristicUuids_haveExpectedValues() {
        assertAll(
            () -> assertEquals(
                    "6e400002-b5a3-f393-e0a9-e50e24dcca9e",
                    CircuitCubesProtocolConstants.TX_CHARACTERISTIC_UUID),
            () -> assertEquals(
                    "6e400003-b5a3-f393-e0a9-e50e24dcca9e",
                    CircuitCubesProtocolConstants.RX_CHARACTERISTIC_UUID)
        );
    }

    @Test
    void channels_haveExpectedLetters() {
        assertAll(
            () -> assertEquals('a', CircuitCubesProtocolConstants.CHANNEL_A),
            () -> assertEquals('b', CircuitCubesProtocolConstants.CHANNEL_B),
            () -> assertEquals('c', CircuitCubesProtocolConstants.CHANNEL_C)
        );
    }

    @Test
    void magnitudeEncoding_stopIsZero() {
        assertEquals(0, CircuitCubesProtocolConstants.MAGNITUDE_STOP);
    }

    @Test
    void magnitudeEncoding_minNonZeroIs55() {
        assertEquals(55, CircuitCubesProtocolConstants.MAGNITUDE_MIN_NONZERO);
    }

    @Test
    void magnitudeEncoding_maxIs255() {
        assertEquals(255, CircuitCubesProtocolConstants.MAGNITUDE_MAX);
    }

    @Test
    void batteryQueryCommand_isLowercaseB() {
        assertEquals((byte) 'b', CircuitCubesProtocolConstants.BATTERY_QUERY_COMMAND);
    }
}
