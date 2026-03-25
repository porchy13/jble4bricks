package ch.varani.bricks.ble.api.dsl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;

import org.junit.jupiter.api.Test;

import ch.varani.bricks.ble.api.BleConnection;
import ch.varani.bricks.ble.api.BleDevice;
import ch.varani.bricks.ble.api.BleException;

/**
 * Unit tests for {@link DeviceDsl}.
 */
class DeviceDslTest {

    @Test
    void name_delegatesToDevice() {
        final BleDevice device = mock(BleDevice.class);
        when(device.name()).thenReturn("MyHub");

        assertEquals("MyHub", new DeviceDsl(device).name());
    }

    @Test
    void id_delegatesToDevice() {
        final BleDevice device = mock(BleDevice.class);
        when(device.id()).thenReturn("device-id-123");

        assertEquals("device-id-123", new DeviceDsl(device).id());
    }

    @Test
    void rssi_delegatesToDevice() {
        final BleDevice device = mock(BleDevice.class);
        when(device.rssi()).thenReturn(-65);

        assertEquals(-65, new DeviceDsl(device).rssi());
    }

    @Test
    void device_returnsWrappedDevice() {
        final BleDevice device = mock(BleDevice.class);

        assertSame(device, new DeviceDsl(device).device());
    }

    @Test
    void thenConnect_returnsConnectionDsl() throws BleException {
        final BleDevice device = mock(BleDevice.class);
        final BleConnection connection = mock(BleConnection.class);
        when(device.connect())
                .thenReturn(CompletableFuture.completedFuture(connection));

        final ConnectionDsl result = new DeviceDsl(device).thenConnect();

        assertNotNull(result);
        assertSame(connection, result.connection());
    }

    @Test
    void thenConnect_failedFuture_throwsBleException() {
        final BleDevice device = mock(BleDevice.class);
        final CompletableFuture<BleConnection> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("connection refused"));
        when(device.connect()).thenReturn(failed);

        assertThrows(BleException.class, () -> new DeviceDsl(device).thenConnect());
    }

    @Test
    void thenConnect_interruptedFuture_throwsBleException()
            throws Exception {
        final BleDevice device = mock(BleDevice.class);
        final CompletableFuture<BleConnection> future = new CompletableFuture<>();
        when(device.connect()).thenReturn(future);

        final Thread thread = new Thread(() ->
                assertThrows(BleException.class, () -> new DeviceDsl(device).thenConnect()));
        thread.start();
        thread.interrupt();
        thread.join(1000);
    }

    // Static import shortcut used only within this test class
    private static void assertEquals(Object expected, Object actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }

    private static void assertEquals(int expected, int actual) {
        org.junit.jupiter.api.Assertions.assertEquals(expected, actual);
    }
}
