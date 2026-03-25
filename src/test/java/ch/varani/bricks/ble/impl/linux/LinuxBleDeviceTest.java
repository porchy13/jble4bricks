package ch.varani.bricks.ble.impl.linux;

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

import ch.varani.bricks.ble.api.BleConnection;

/**
 * Unit tests for {@link LinuxBleDevice}.
 */
@ExtendWith(MockitoExtension.class)
class LinuxBleDeviceTest {

    private static final String DEVICE_PATH = "/org/bluez/hci0/dev_AA_BB_CC_DD_EE_FF";
    private static final String DEVICE_NAME = "TestDevice";
    private static final int RSSI = -55;

    @Mock
    private LinuxBleScanner mockScanner;

    private LinuxBleDevice device;

    @BeforeEach
    void setUp() {
        device = new LinuxBleDevice(DEVICE_PATH, DEVICE_NAME, RSSI, mockScanner);
    }

    @Test
    void id_returnsDevicePath() {
        assertEquals(DEVICE_PATH, device.id());
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
    void connect_delegatesToScannerConnectDevice() {
        final LinuxBleConnection mockConnection = Mockito.mock(LinuxBleConnection.class);
        final CompletableFuture<LinuxBleConnection> future =
                CompletableFuture.completedFuture(mockConnection);
        Mockito.when(mockScanner.connectDevice(DEVICE_PATH)).thenReturn(future);

        final CompletableFuture<BleConnection> result = device.connect();

        assertAll(
            () -> assertNotNull(result),
            () -> assertSame(mockConnection, result.join())
        );
        Mockito.verify(mockScanner).connectDevice(DEVICE_PATH);
    }
}
