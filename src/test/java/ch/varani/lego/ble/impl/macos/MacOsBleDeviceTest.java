package ch.varani.lego.ble.impl.macos;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.Mockito;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.varani.lego.ble.api.BleConnection;

/**
 * Unit tests for {@link MacOsBleDevice}.
 */
@ExtendWith(MockitoExtension.class)
class MacOsBleDeviceTest {

    private static final String PERIPHERAL_ID = "test-uuid-1234";
    private static final String DEVICE_NAME = "TestDevice";
    private static final int RSSI = -55;

    @Mock
    private MacOsBleScanner mockScanner;

    private MacOsBleDevice device;

    @BeforeEach
    void setUp() {
        device = new MacOsBleDevice(PERIPHERAL_ID, DEVICE_NAME, RSSI, mockScanner);
    }

    @Test
    void id_returnsPeripheralId() {
        assertEquals(PERIPHERAL_ID, device.id());
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
    void connect_delegatesToScannerConnectPeripheral() {
        final MacOsBleConnection mockConnection = Mockito.mock(MacOsBleConnection.class);
        final CompletableFuture<MacOsBleConnection> future =
                CompletableFuture.completedFuture(mockConnection);
        Mockito.when(mockScanner.connectPeripheral(PERIPHERAL_ID)).thenReturn(future);

        final CompletableFuture<BleConnection> result = device.connect();

        assertAll(
            () -> assertNotNull(result),
            () -> assertSame(mockConnection, result.join())
        );
        Mockito.verify(mockScanner).connectPeripheral(PERIPHERAL_ID);
    }
}
