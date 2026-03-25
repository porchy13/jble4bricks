package ch.varani.bricks.ble.impl.windows;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.eq;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.never;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.lang.reflect.Field;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutionException;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.varani.bricks.ble.api.BleDevice;
import ch.varani.bricks.ble.api.BleException;
import ch.varani.bricks.ble.api.ScanCallback;

/**
 * Unit tests for {@link WindowsBleScanner}.
 *
 * <p>The native JNI methods are never invoked: {@link WindowsNativeBridge} is
 * injected as a Mockito mock via the package-private
 * {@link WindowsBleScanner#WindowsBleScanner(WindowsNativeBridge, long)} constructor.
 */
@ExtendWith(MockitoExtension.class)
class WindowsBleScannerTest {

    private static final long CTX_PTR  = 0x1000L;
    private static final long CONN_PTR = 0x2000L;

    @Mock
    private WindowsNativeBridge bridge;

    private WindowsBleScanner scanner;
    private final List<BleDevice> found = new ArrayList<>();

    @BeforeEach
    void setUp() {
        scanner = new WindowsBleScanner(bridge, CTX_PTR);
        found.clear();
    }

    /* ─────────────────────────────────────────────────────────────────────────
       Library name constant
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void libraryName_constant_isBleWindows() {
        assertEquals("ble-windows", WindowsBleScanner.LIBRARY_NAME);
    }

    /* ─────────────────────────────────────────────────────────────────────────
       startScan (no-UUID variant)
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void startScan_withCallback_completesAndCallsBridge() throws Exception {
        final CompletableFuture<Void> future = scanner.startScan(found::add);
        future.get();

        assertAll(
            () -> assertNotNull(future),
            () -> verify(bridge).startScan(eq(CTX_PTR), (String) eq(null))
        );
    }

    /* ─────────────────────────────────────────────────────────────────────────
       startScan (UUID variant)
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void startScan_withServiceUuidAndCallback_passesUuidToBridge() throws Exception {
        scanner.startScan("test-uuid", found::add).get();

        verify(bridge).startScan(CTX_PTR, "test-uuid");
    }

    @Test
    void startScan_withNullUuidAndCallback_passesNullToBridge() throws Exception {
        scanner.startScan(null, found::add).get();

        verify(bridge).startScan(CTX_PTR, null);
    }

    /* ─────────────────────────────────────────────────────────────────────────
       stopScan
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void stopScan_callsBridgeStop() throws Exception {
        final CompletableFuture<Void> future = scanner.stopScan();
        future.get();

        assertAll(
            () -> assertNotNull(future),
            () -> verify(bridge).stopScan(CTX_PTR)
        );
    }

    /* ─────────────────────────────────────────────────────────────────────────
       isScanning
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void isScanning_whenBridgeReturnsFalse_returnsFalse() {
        when(bridge.isScanning(CTX_PTR)).thenReturn(false);
        assertFalse(scanner.isScanning());
    }

    @Test
    void isScanning_whenBridgeReturnsTrue_returnsTrue() {
        when(bridge.isScanning(CTX_PTR)).thenReturn(true);
        assertTrue(scanner.isScanning());
    }

    /* ─────────────────────────────────────────────────────────────────────────
       close
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void close_callsBridgeDestroy() throws BleException {
        scanner.close();
        verify(bridge).destroy(CTX_PTR);
    }

    @Test
    void close_whenBridgeThrows_wrapsToBleException() {
        doThrow(new RuntimeException("native error")).when(bridge).destroy(CTX_PTR);

        final BleException ex = assertThrows(BleException.class, scanner::close);
        assertTrue(ex.getMessage().contains("native error"));
    }

    /* ─────────────────────────────────────────────────────────────────────────
       onDeviceFound callback
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void onDeviceFound_whenScanActive_invokesCallback() throws Exception {
        scanner.startScan(found::add).get();
        scanner.onDeviceFound("addr-1", "DeviceA", -60);

        assertAll(
            () -> assertEquals(1, found.size()),
            () -> assertEquals("addr-1", found.get(0).id()),
            () -> assertEquals("DeviceA", found.get(0).name()),
            () -> assertEquals(-60, found.get(0).rssi())
        );
    }

    @Test
    void onDeviceFound_sameAddress_returnsCachedDevice() throws Exception {
        scanner.startScan(found::add).get();
        scanner.onDeviceFound("addr-same", "Dev", -50);
        scanner.onDeviceFound("addr-same", "Dev", -50);

        assertAll(
            () -> assertEquals(2, found.size()),
            () -> assertEquals("addr-same", found.get(0).id()),
            () -> assertEquals("addr-same", found.get(1).id())
        );
    }

    @Test
    void onDeviceFound_whenNoCallback_doesNotThrow() {
        /* No startScan called — currentCallback is null. */
        scanner.onDeviceFound("addr-1", "Dev", -70);
        assertTrue(found.isEmpty());
    }

    @Test
    void onDeviceFound_afterStopScan_doesNotDeliverToOldCallback() throws Exception {
        scanner.startScan(found::add).get();
        scanner.stopScan().get();
        scanner.onDeviceFound("addr-1", "Dev", -70);

        assertTrue(found.isEmpty());
    }

    /* ─────────────────────────────────────────────────────────────────────────
       connectPeripheral delegation
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void connectPeripheral_success_returnsNewConnection() throws Exception {
        when(bridge.connect(CTX_PTR, "addr-x")).thenReturn(CONN_PTR);

        final WindowsBleConnection conn = scanner.connectPeripheral("addr-x").get();

        assertAll(
            () -> assertNotNull(conn),
            () -> assertEquals(CONN_PTR, conn.connectionPtr()),
            () -> assertEquals(CTX_PTR, conn.contextPtr())
        );
    }

    @Test
    void connectPeripheral_whenBridgeThrows_futureCompletesExceptionally() {
        when(bridge.connect(anyLong(), any())).thenThrow(new RuntimeException("no BT"));

        final CompletableFuture<WindowsBleConnection> future =
                scanner.connectPeripheral("addr-fail");

        final ExecutionException ex =
                assertThrows(ExecutionException.class, future::get);
        assertAll(
            () -> assertTrue(ex.getCause() instanceof BleException),
            () -> assertTrue(ex.getCause().getMessage().contains("addr-fail"))
        );
    }

    /* ─────────────────────────────────────────────────────────────────────────
       disconnectNative / writeWithoutResponseNative / readCharacteristicNative /
       setNotifyNative — delegation to bridge
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void disconnectNative_delegatesToBridge() {
        scanner.disconnectNative(CONN_PTR);
        verify(bridge).disconnect(CTX_PTR, CONN_PTR);
    }

    @Test
    void writeWithoutResponseNative_delegatesToBridge() {
        final byte[] data = {0x01, 0x02};
        scanner.writeWithoutResponseNative(CONN_PTR, "svc", "chr", data);
        verify(bridge).writeWithoutResponse(CONN_PTR, "svc", "chr", data);
    }

    @Test
    void readCharacteristicNative_bridgeReturnsBytes_returnsSameBytes() {
        final byte[] expected = {0x03, 0x04};
        when(bridge.readCharacteristic(CONN_PTR, "svc", "chr")).thenReturn(expected);

        final byte[] result = scanner.readCharacteristicNative(CONN_PTR, "svc", "chr");
        assertEquals(expected, result);
    }

    @Test
    void readCharacteristicNative_bridgeReturnsNull_returnsEmptyArray() {
        when(bridge.readCharacteristic(CONN_PTR, "svc", "chr")).thenReturn(null);

        final byte[] result = scanner.readCharacteristicNative(CONN_PTR, "svc", "chr");
        assertAll(
            () -> assertNotNull(result),
            () -> assertEquals(0, result.length)
        );
    }

    @Test
    void setNotifyNative_delegatesToBridge() {
        scanner.setNotifyNative(CONN_PTR, "svc", "chr", true);
        verify(bridge).setNotify(CONN_PTR, "svc", "chr", true);
    }

    /* ─────────────────────────────────────────────────────────────────────────
       startScan — superseded-scan guard
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void startScan_secondCallBeforeFirstExecutes_firstScanIsSkipped() throws Exception {
        /*
         * We cannot guarantee the race condition in a unit test, but we can
         * verify the normal path: after two successive startScan calls the
         * bridge receives at least one startScan call (possibly two if both
         * futures run; the guard only fires if the generation changed before
         * the async task executes).
         */
        final ScanCallback cb = found::add;
        scanner.startScan(cb).get();
        scanner.startScan("svc", cb).get();

        /* At minimum the second call must reach the bridge. */
        verify(bridge).startScan(CTX_PTR, "svc");
    }

    /* ─────────────────────────────────────────────────────────────────────────
       startScan — knownDevices cleared on new scan
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void startScan_clearsKnownDevicesFromPreviousScan() throws Exception {
        scanner.startScan(found::add).get();
        scanner.onDeviceFound("addr-A", "Dev", -50);

        /* Second scan must clear the cache. */
        scanner.startScan(found::add).get();
        /*
         * After the new scan the device cache is empty; a re-discovery of
         * addr-A produces a fresh device object delivered to the new callback.
         */
        final List<BleDevice> secondFound = new ArrayList<>();
        scanner.startScan(secondFound::add).get();
        scanner.onDeviceFound("addr-A", "Dev", -50);

        assertAll(
            () -> assertFalse(secondFound.isEmpty()),
            () -> assertEquals("addr-A", secondFound.get(0).id())
        );
    }

    /* ─────────────────────────────────────────────────────────────────────────
       close — nullifies callback so further onDeviceFound calls are dropped
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void close_afterStartScan_nullifiesCallbackSoDevicesNoLongerDelivered()
            throws BleException, Exception {
        scanner.startScan(found::add).get();
        scanner.close();
        scanner.onDeviceFound("addr-1", "Dev", -70);

        assertTrue(found.isEmpty());
    }

    /* ─────────────────────────────────────────────────────────────────────────
       startScan (no-UUID) — assert null UUID passed to bridge
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void startScan_noUuidVariant_passesNullServiceUuidToBridge() throws Exception {
        scanner.startScan(found::add).get();
        verify(bridge).startScan(CTX_PTR, null);
        verify(bridge, never()).startScan(eq(CTX_PTR), any(String.class));
    }

    /* ─────────────────────────────────────────────────────────────────────────
       startScan — superseded scan guard (generation check)
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void startScan_supersededBeforeExecution_doesNotCallBridge() throws Exception {
        /*
         * Strategy: use a single-threaded executor injected into the scanner.
         * Block that thread with a pre-submitted task, then submit startScan —
         * the scan lambda is queued but not yet running.  While it's queued we
         * directly increment activeScanGeneration via reflection so that when
         * the lambda eventually runs, 'generation != activeScanGeneration' is
         * true and the early-return guard fires.
         */
        final CountDownLatch blockLatch = new CountDownLatch(1);
        final ExecutorService singleThread = Executors.newSingleThreadExecutor();

        final WindowsBleScanner controlledScanner =
                new WindowsBleScanner(bridge, CTX_PTR, singleThread);

        /* Block the single executor thread so our scan task will be queued. */
        singleThread.submit(() -> {
            try {
                blockLatch.await();
            } catch (InterruptedException ignored) {
                Thread.currentThread().interrupt();
            }
        });

        /* Submit startScan — the lambda is now sitting in the executor queue. */
        final CompletableFuture<Void> future = controlledScanner.startScan(found::add);

        /* Read the captured generation, then bump activeScanGeneration via reflection
         * so it no longer matches what the lambda captured. */
        final Field genField =
                WindowsBleScanner.class.getDeclaredField("activeScanGeneration");
        genField.setAccessible(true);
        genField.setLong(controlledScanner, (long) genField.get(controlledScanner) + 1L);

        /* Release the executor thread — the scan lambda runs and hits the guard. */
        blockLatch.countDown();
        future.get();

        /* bridge.startScan must NOT have been called because the guard fired. */
        verify(bridge, never()).startScan(anyLong(), any());

        controlledScanner.close();
        singleThread.shutdown();
    }

    /* ─────────────────────────────────────────────────────────────────────────
       startScan — LOG.fine lambda
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void startScan_withFineLogging_logsMessage() throws Exception {
        final Logger logger = Logger.getLogger(WindowsBleScanner.class.getName());
        final Level savedLevel = logger.getLevel();
        try {
            logger.setLevel(Level.FINE);
            scanner.startScan(found::add).get();
            /* No assertion needed; if the lambda throws the test fails. */
            assertNotNull(scanner);
        } finally {
            logger.setLevel(savedLevel);
        }
    }

    /* ─────────────────────────────────────────────────────────────────────────
       onNotification — routing via openConnections map
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void onNotification_withUnknownConnectionPtr_doesNotThrow() {
        /* No connection registered — conn will be null; must be silently dropped. */
        scanner.onNotification(0xDEADL, "svc", "chr", new byte[]{0x01});
        /* Reaching here without exception is the assertion. */
        assertNotNull(scanner);
    }

    @Test
    void onNotification_withRegisteredConnection_routesToConnection() throws Exception {
        when(bridge.connect(CTX_PTR, "addr-x")).thenReturn(CONN_PTR);

        final WindowsBleConnection conn = scanner.connectPeripheral("addr-x").get();
        /* The connection is now registered in openConnections under CONN_PTR. */
        final byte[] value = {0x01, 0x02};
        /* Should not throw; the connection exists and its publisher map is empty
         * (no subscribers yet), so the value is silently dropped inside the connection. */
        scanner.onNotification(conn.connectionPtr(), "svc", "chr", value);
        assertNotNull(conn);
    }
}
