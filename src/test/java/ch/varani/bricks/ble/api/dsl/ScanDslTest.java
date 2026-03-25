package ch.varani.bricks.ble.api.dsl;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.isNull;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.TimeUnit;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import ch.varani.bricks.ble.api.BleDevice;
import ch.varani.bricks.ble.api.BleException;
import ch.varani.bricks.ble.api.BleScanner;
import ch.varani.bricks.ble.api.ScanCallback;
import ch.varani.bricks.ble.device.buwizz.BuWizz2ProtocolConstants;
import ch.varani.bricks.ble.device.buwizz.BuWizz3ProtocolConstants;
import ch.varani.bricks.ble.device.circuitcubes.CircuitCubesProtocolConstants;
import ch.varani.bricks.ble.device.lego.LegoProtocolConstants;
import ch.varani.bricks.ble.device.sbrick.SBrickProtocolConstants;

/**
 * Unit tests for {@link ScanDsl}.
 */
class ScanDslTest {

    @Test
    void forLegoHubs_setsLegoServiceUuid() {
        final BleScanner scanner = mock(BleScanner.class);
        final ScanDsl dsl = new ScanDsl(scanner).forLegoHubs();

        assertEquals(LegoProtocolConstants.HUB_SERVICE_UUID, dsl.serviceUuidFilter());
    }

    @Test
    void forSBricks_setsSBrickServiceUuid() {
        final BleScanner scanner = mock(BleScanner.class);
        final ScanDsl dsl = new ScanDsl(scanner).forSBricks();

        assertEquals(SBrickProtocolConstants.REMOTE_CONTROL_SERVICE_UUID,
                dsl.serviceUuidFilter());
    }

    @Test
    void forCircuitCubes_setsNusServiceUuid() {
        final BleScanner scanner = mock(BleScanner.class);
        final ScanDsl dsl = new ScanDsl(scanner).forCircuitCubes();

        assertEquals(CircuitCubesProtocolConstants.NUS_SERVICE_UUID, dsl.serviceUuidFilter());
    }

    @Test
    void forBuWizz2_setsBuWizz2ServiceUuid() {
        final BleScanner scanner = mock(BleScanner.class);
        final ScanDsl dsl = new ScanDsl(scanner).forBuWizz2();

        assertEquals(BuWizz2ProtocolConstants.APPLICATION_SERVICE_UUID,
                dsl.serviceUuidFilter());
    }

    @Test
    void forBuWizz3_setsBuWizz3ServiceUuid() {
        final BleScanner scanner = mock(BleScanner.class);
        final ScanDsl dsl = new ScanDsl(scanner).forBuWizz3();

        assertEquals(BuWizz3ProtocolConstants.APPLICATION_SERVICE_UUID,
                dsl.serviceUuidFilter());
    }

    @Test
    void forService_setsCustomUuid() {
        final BleScanner scanner = mock(BleScanner.class);
        final String uuid = "cafebabe-0000-1234-abcd-deadbeef0000";
        final ScanDsl dsl = new ScanDsl(scanner).forService(uuid);

        assertEquals(uuid, dsl.serviceUuidFilter());
    }

    @Test
    void forService_null_clearsFilter() {
        final BleScanner scanner = mock(BleScanner.class);
        final ScanDsl dsl = new ScanDsl(scanner).forLegoHubs().forService(null);

        assertNull(dsl.serviceUuidFilter());
    }

    @Test
    void timeoutSeconds_setsTimeout() {
        final BleScanner scanner = mock(BleScanner.class);
        final ScanDsl dsl = new ScanDsl(scanner).timeoutSeconds(5);

        assertEquals(5L, dsl.timeoutSeconds());
    }

    @Test
    void timeoutSeconds_negativeValue_throwsIllegalArgumentException() {
        final BleScanner scanner = mock(BleScanner.class);

        assertThrows(IllegalArgumentException.class,
                () -> new ScanDsl(scanner).timeoutSeconds(-1));
    }

    @Test
    void collect_invalidCount_throwsIllegalArgumentException() {
        final BleScanner scanner = mock(BleScanner.class);

        assertThrows(IllegalArgumentException.class,
                () -> new ScanDsl(scanner).collect(0));
    }

    @Test
    void collect_oneDevice_returnsDeviceAndStopsScan() throws BleException {
        final BleScanner scanner = mock(BleScanner.class);
        final BleDevice device = mock(BleDevice.class);

        when(scanner.startScan(isNull(), any(ScanCallback.class)))
                .thenAnswer(inv -> {
                    final ScanCallback cb = inv.getArgument(1);
                    cb.onDeviceFound(device);
                    return CompletableFuture.completedFuture(null);
                });
        when(scanner.stopScan()).thenReturn(CompletableFuture.completedFuture(null));

        final List<BleDevice> result = new ScanDsl(scanner).collect(1);

        assertNotNull(result);
        assertEquals(1, result.size());
        verify(scanner).stopScan();
    }

    @Test
    void first_returnsDeviceDslWrappingFirstDevice() throws BleException {
        final BleScanner scanner = mock(BleScanner.class);
        final BleDevice device = mock(BleDevice.class);
        when(device.name()).thenReturn("TestHub");

        when(scanner.startScan(isNull(), any(ScanCallback.class)))
                .thenAnswer(inv -> {
                    final ScanCallback cb = inv.getArgument(1);
                    cb.onDeviceFound(device);
                    return CompletableFuture.completedFuture(null);
                });
        when(scanner.stopScan()).thenReturn(CompletableFuture.completedFuture(null));

        final DeviceDsl result = new ScanDsl(scanner).first();

        assertNotNull(result);
        assertEquals("TestHub", result.name());
    }

    @Test
    void collect_withTimeout_completesNormallyWhenDeviceFoundBeforeTimeout() throws BleException {
        final BleScanner scanner = mock(BleScanner.class);
        final BleDevice device = mock(BleDevice.class);

        when(scanner.startScan(isNull(), any(ScanCallback.class)))
                .thenAnswer(inv -> {
                    final ScanCallback cb = inv.getArgument(1);
                    cb.onDeviceFound(device);
                    return CompletableFuture.completedFuture(null);
                });
        when(scanner.stopScan()).thenReturn(CompletableFuture.completedFuture(null));

        final List<BleDevice> result =
                new ScanDsl(scanner).timeoutSeconds(30).collect(1);

        assertEquals(1, result.size());
    }

    @Test
    void collect_twoDevices_collectsBothAndIgnoresExtraCallbacks() throws BleException {
        final BleScanner scanner = mock(BleScanner.class);
        final BleDevice device1 = mock(BleDevice.class);
        final BleDevice device2 = mock(BleDevice.class);
        final BleDevice device3 = mock(BleDevice.class);

        when(scanner.startScan(isNull(), any(ScanCallback.class)))
                .thenAnswer(inv -> {
                    final ScanCallback cb = inv.getArgument(1);
                    cb.onDeviceFound(device1);   // size 0->1, not yet at limit (covers < false then >=false)
                    cb.onDeviceFound(device2);   // size 1->2, at limit (covers >= true)
                    cb.onDeviceFound(device3);   // size already 2, ignored (covers < false)
                    return CompletableFuture.completedFuture(null);
                });
        when(scanner.stopScan()).thenReturn(CompletableFuture.completedFuture(null));

        final List<BleDevice> result = new ScanDsl(scanner).collect(2);

        assertEquals(2, result.size());
    }

    /**
     * Verifies that a {@link java.util.concurrent.TimeoutException} from the
     * underlying {@code CompletableFuture} is wrapped in a {@link BleException}.
     */
    @Test
    void collect_withTimeout_timeoutExceptionWrappedAsBleException() {
        final BleScanner scanner = mock(BleScanner.class);

        // Return a future that never completes, so the 1-second timeout will fire
        when(scanner.startScan(isNull(), any(ScanCallback.class)))
                .thenReturn(new CompletableFuture<>());
        when(scanner.stopScan()).thenReturn(CompletableFuture.completedFuture(null));

        assertThrows(BleException.class,
                () -> new ScanDsl(scanner).timeoutSeconds(1).collect(1));
    }

    /**
     * Verifies that an {@link ExecutionException} from the scan future is
     * wrapped in a {@link BleException}.
     */
    @Test
    void collect_executionExceptionFromFuture_wrappedAsBleException() {
        final BleScanner scanner = mock(BleScanner.class);
        final CompletableFuture<Void> failed = new CompletableFuture<>();
        failed.completeExceptionally(new RuntimeException("scan error"));

        when(scanner.startScan(isNull(), any(ScanCallback.class))).thenReturn(failed);
        when(scanner.stopScan()).thenReturn(CompletableFuture.completedFuture(null));

        assertThrows(BleException.class, () -> new ScanDsl(scanner).collect(1));
    }

    /**
     * Verifies that thread interruption during {@code collect} propagates as a
     * {@link BleException} and that the interrupt flag is restored.
     */
    @Test
    void collect_interruptedExceptionFromFuture_wrappedAsBleException() throws Exception {
        final BleScanner scanner = mock(BleScanner.class);
        // Use a future that will never complete (so get() blocks)
        final CompletableFuture<Void> never = new CompletableFuture<>();
        when(scanner.startScan(isNull(), any(ScanCallback.class))).thenReturn(never);
        when(scanner.stopScan()).thenReturn(CompletableFuture.completedFuture(null));

        final BleException[] caught = new BleException[1];
        final Thread thread = new Thread(() -> {
            try {
                new ScanDsl(scanner).collect(1);
            } catch (BleException ex) {
                caught[0] = ex;
            }
        });
        thread.start();
        // Wait for the thread to enter WAITING state (blocking on Future.get()), then interrupt
        Awaitility.await()
                .atMost(5, TimeUnit.SECONDS)
                .until(() -> thread.getState() == Thread.State.WAITING);
        thread.interrupt();
        thread.join(2000);

        assertNotNull(caught[0]);
        assertTrue(caught[0].getMessage().contains("interrupted"),
                "Expected 'interrupted' in message but got: " + caught[0].getMessage());
    }
}
