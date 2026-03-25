package ch.varani.bricks.ble.impl.windows;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNotSame;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.varani.bricks.ble.api.BleConnection;

/**
 * Unit tests for {@link WindowsBleDevice}.
 */
@ExtendWith(MockitoExtension.class)
class WindowsBleDeviceTest {

    private static final String DEVICE_ADDRESS = "AA:BB:CC:DD:EE:FF";
    private static final String DEVICE_NAME = "TestDevice";
    private static final int RSSI = -55;
    private static final byte[] MFR_DATA = {0x01, 0x02, 0x03};

    @Mock
    private WindowsBleScanner mockScanner;

    private WindowsBleDevice device;

    @BeforeEach
    void setUp() {
        device = new WindowsBleDevice(DEVICE_ADDRESS, DEVICE_NAME, RSSI, MFR_DATA, mockScanner);
    }

    @Test
    void id_returnsDeviceAddress() {
        assertEquals(DEVICE_ADDRESS, device.id());
    }

    @Test
    void name_returnsDeviceName() {
        assertEquals(DEVICE_NAME, device.name());
    }

    @Test
    void rssi_returnsRssiValue() {
        assertEquals(RSSI, device.rssi());
    }

    @Test
    void manufacturerData_returnsDefensiveCopy() {
        final byte[] copy1 = device.manufacturerData();
        final byte[] copy2 = device.manufacturerData();
        assertAll(
            () -> assertArrayEquals(MFR_DATA, copy1),
            () -> assertNotSame(copy1, copy2)
        );
    }

    @Test
    void manufacturerData_mutatingReturnedArray_doesNotAffectStoredData() {
        final byte[] copy = device.manufacturerData();
        copy[0] = (byte) 0xFF;
        assertArrayEquals(MFR_DATA, device.manufacturerData());
    }

    @Test
    void connect_delegatesToScannerConnectPeripheral() {
        final WindowsBleConnection mockConnection = Mockito.mock(WindowsBleConnection.class);
        final CompletableFuture<WindowsBleConnection> future =
                CompletableFuture.completedFuture(mockConnection);
        Mockito.when(mockScanner.connectPeripheral(DEVICE_ADDRESS)).thenReturn(future);

        final CompletableFuture<BleConnection> result = device.connect();

        assertAll(
            () -> assertNotNull(result),
            () -> assertSame(mockConnection, result.join())
        );
        Mockito.verify(mockScanner).connectPeripheral(DEVICE_ADDRESS);
    }
}
