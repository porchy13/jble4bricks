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
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.awaitility.Awaitility;
import org.junit.jupiter.api.Test;

import ch.varani.bricks.ble.api.BleDevice;
import ch.varani.bricks.ble.api.BleException;
import ch.varani.bricks.ble.api.BleScanner;
import ch.varani.bricks.ble.api.ScanCallback;
import ch.varani.bricks.ble.device.buwizz.BuWizz2ProtocolConstants;
import ch.varani.bricks.ble.device.buwizz.BuWizz3ProtocolConstants;
import ch.varani.bricks.ble.device.circuitcubes.CircuitCubesProtocolConstants;
import ch.varani.bricks.ble.device.lego.LegoHubType;
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

    /* ─────────────────────────────────────────────────────────────────────────
       withDeviceId — device predicate filter
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void withDeviceId_setsNonNullDeviceFilter() {
        final BleScanner scanner = mock(BleScanner.class);
        final ScanDsl dsl = new ScanDsl(scanner).withDeviceId("some-id");

        assertNotNull(dsl.deviceFilter());
    }

    @Test
    void withDeviceId_filterAcceptsMatchingDevice() {
        final BleScanner scanner = mock(BleScanner.class);
        final BleDevice device = mock(BleDevice.class);
        when(device.id()).thenReturn("target-id");

        final ScanDsl dsl = new ScanDsl(scanner).withDeviceId("target-id");

        assertTrue(dsl.deviceFilter().test(device));
    }

    @Test
    void withDeviceId_filterRejectsNonMatchingDevice() {
        final BleScanner scanner = mock(BleScanner.class);
        final BleDevice device = mock(BleDevice.class);
        when(device.id()).thenReturn("other-id");

        final ScanDsl dsl = new ScanDsl(scanner).withDeviceId("target-id");

        org.junit.jupiter.api.Assertions.assertFalse(dsl.deviceFilter().test(device));
    }

    @Test
    void withDeviceId_onlyMatchingDeviceCollected() throws BleException {
        final BleScanner scanner = mock(BleScanner.class);
        final BleDevice matchingDevice = mock(BleDevice.class);
        final BleDevice otherDevice = mock(BleDevice.class);
        when(matchingDevice.id()).thenReturn("target-id");
        when(otherDevice.id()).thenReturn("other-id");

        when(scanner.startScan(isNull(), any(ScanCallback.class)))
                .thenAnswer(inv -> {
                    final ScanCallback cb = inv.getArgument(1);
                    cb.onDeviceFound(otherDevice);
                    cb.onDeviceFound(matchingDevice);
                    return CompletableFuture.completedFuture(null);
                });
        when(scanner.stopScan()).thenReturn(CompletableFuture.completedFuture(null));

        final java.util.List<BleDevice> result =
                new ScanDsl(scanner).withDeviceId("target-id").collect(1);

        assertEquals(1, result.size());
        assertEquals("target-id", result.get(0).id());
    }

    /* ─────────────────────────────────────────────────────────────────────────
       forLegoHubType — hub type predicate filter
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void forLegoHubType_setsNonNullDeviceFilter() {
        final BleScanner scanner = mock(BleScanner.class);
        final ScanDsl dsl = new ScanDsl(scanner).forLegoHubType(LegoHubType.CITY_HUB);

        assertNotNull(dsl.deviceFilter());
    }

    @Test
    void forLegoHubType_filterAcceptsDeviceWithMatchingSystemTypeByte() {
        final BleScanner scanner = mock(BleScanner.class);
        final BleDevice device = mock(BleDevice.class);
        /* City Hub system type byte = 0x41; place it at index
         * MANUFACTURER_DATA_IDX_SYSTEM_TYPE (3) in a 4-byte payload. */
        final byte[] mfrData = new byte[LegoProtocolConstants.MANUFACTURER_DATA_MIN_LENGTH];
        mfrData[LegoProtocolConstants.MANUFACTURER_DATA_IDX_SYSTEM_TYPE] =
                (byte) LegoProtocolConstants.DEVICE_2PORT_HUB;
        when(device.manufacturerData()).thenReturn(mfrData);

        final ScanDsl dsl = new ScanDsl(scanner).forLegoHubType(LegoHubType.CITY_HUB);

        assertTrue(dsl.deviceFilter().test(device));
    }

    @Test
    void forLegoHubType_filterRejectsDeviceWithDifferentSystemTypeByte() {
        final BleScanner scanner = mock(BleScanner.class);
        final BleDevice device = mock(BleDevice.class);
        final byte[] mfrData = new byte[LegoProtocolConstants.MANUFACTURER_DATA_MIN_LENGTH];
        mfrData[LegoProtocolConstants.MANUFACTURER_DATA_IDX_SYSTEM_TYPE] =
                (byte) LegoProtocolConstants.DEVICE_BOOST_HUB;
        when(device.manufacturerData()).thenReturn(mfrData);

        final ScanDsl dsl = new ScanDsl(scanner).forLegoHubType(LegoHubType.CITY_HUB);

        org.junit.jupiter.api.Assertions.assertFalse(dsl.deviceFilter().test(device));
    }

    @Test
    void forLegoHubType_filterRejectsDeviceWithTooShortPayload() {
        final BleScanner scanner = mock(BleScanner.class);
        final BleDevice device = mock(BleDevice.class);
        /* Payload shorter than MANUFACTURER_DATA_MIN_LENGTH. */
        when(device.manufacturerData())
                .thenReturn(new byte[LegoProtocolConstants.MANUFACTURER_DATA_MIN_LENGTH - 1]);

        final ScanDsl dsl = new ScanDsl(scanner).forLegoHubType(LegoHubType.CITY_HUB);

        org.junit.jupiter.api.Assertions.assertFalse(dsl.deviceFilter().test(device));
    }

    @Test
    void forLegoHubType_filterRejectsDeviceWithEmptyPayload() {
        final BleScanner scanner = mock(BleScanner.class);
        final BleDevice device = mock(BleDevice.class);
        when(device.manufacturerData()).thenReturn(new byte[0]);

        final ScanDsl dsl = new ScanDsl(scanner).forLegoHubType(LegoHubType.TECHNIC_HUB);

        org.junit.jupiter.api.Assertions.assertFalse(dsl.deviceFilter().test(device));
    }

    /* ─────────────────────────────────────────────────────────────────────────
       forWeDo2 — convenience shortcut
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void forWeDo2_setsNonNullDeviceFilter() {
        final BleScanner scanner = mock(BleScanner.class);
        final ScanDsl dsl = new ScanDsl(scanner).forWeDo2();

        assertNotNull(dsl.deviceFilter());
    }

    @Test
    void forWeDo2_filterAcceptsWeDo2Device() {
        final BleScanner scanner = mock(BleScanner.class);
        final BleDevice device = mock(BleDevice.class);
        final byte[] mfrData = new byte[LegoProtocolConstants.MANUFACTURER_DATA_MIN_LENGTH];
        mfrData[LegoProtocolConstants.MANUFACTURER_DATA_IDX_SYSTEM_TYPE] =
                (byte) LegoProtocolConstants.DEVICE_WEDO2_HUB;
        when(device.manufacturerData()).thenReturn(mfrData);

        final ScanDsl dsl = new ScanDsl(scanner).forWeDo2();

        assertTrue(dsl.deviceFilter().test(device));
    }

    @Test
    void forWeDo2_filterRejectsNonWeDo2Device() {
        final BleScanner scanner = mock(BleScanner.class);
        final BleDevice device = mock(BleDevice.class);
        final byte[] mfrData = new byte[LegoProtocolConstants.MANUFACTURER_DATA_MIN_LENGTH];
        mfrData[LegoProtocolConstants.MANUFACTURER_DATA_IDX_SYSTEM_TYPE] =
                (byte) LegoProtocolConstants.DEVICE_2PORT_HUB;
        when(device.manufacturerData()).thenReturn(mfrData);

        final ScanDsl dsl = new ScanDsl(scanner).forWeDo2();

        org.junit.jupiter.api.Assertions.assertFalse(dsl.deviceFilter().test(device));
    }

    /* ─────────────────────────────────────────────────────────────────────────
       deviceFilter initially null
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void deviceFilter_initiallyNull() {
        final BleScanner scanner = mock(BleScanner.class);
        final ScanDsl dsl = new ScanDsl(scanner);

        assertNull(dsl.deviceFilter());
    }

    /* ─────────────────────────────────────────────────────────────────────────
       Combined withDeviceId + forLegoHubType — AND semantics
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void withDeviceId_andForLegoHubType_combinedFilterRejectsMismatchedId() {
        final BleScanner scanner = mock(BleScanner.class);
        final BleDevice device = mock(BleDevice.class);
        final byte[] mfrData = new byte[LegoProtocolConstants.MANUFACTURER_DATA_MIN_LENGTH];
        mfrData[LegoProtocolConstants.MANUFACTURER_DATA_IDX_SYSTEM_TYPE] =
                (byte) LegoProtocolConstants.DEVICE_2PORT_HUB;
        when(device.id()).thenReturn("wrong-id");
        when(device.manufacturerData()).thenReturn(mfrData);

        final ScanDsl dsl = new ScanDsl(scanner)
                .withDeviceId("target-id")
                .forLegoHubType(LegoHubType.CITY_HUB);

        org.junit.jupiter.api.Assertions.assertFalse(dsl.deviceFilter().test(device));
    }

    @Test
    void withDeviceId_andForLegoHubType_combinedFilterRejectsMismatchedType() {
        final BleScanner scanner = mock(BleScanner.class);
        final BleDevice device = mock(BleDevice.class);
        final byte[] mfrData = new byte[LegoProtocolConstants.MANUFACTURER_DATA_MIN_LENGTH];
        mfrData[LegoProtocolConstants.MANUFACTURER_DATA_IDX_SYSTEM_TYPE] =
                (byte) LegoProtocolConstants.DEVICE_BOOST_HUB; // wrong type
        when(device.id()).thenReturn("target-id");
        when(device.manufacturerData()).thenReturn(mfrData);

        final ScanDsl dsl = new ScanDsl(scanner)
                .withDeviceId("target-id")
                .forLegoHubType(LegoHubType.CITY_HUB);

        org.junit.jupiter.api.Assertions.assertFalse(dsl.deviceFilter().test(device));
    }

    @Test
    void withDeviceId_andForLegoHubType_combinedFilterAcceptsBothMatching() {
        final BleScanner scanner = mock(BleScanner.class);
        final BleDevice device = mock(BleDevice.class);
        final byte[] mfrData = new byte[LegoProtocolConstants.MANUFACTURER_DATA_MIN_LENGTH];
        mfrData[LegoProtocolConstants.MANUFACTURER_DATA_IDX_SYSTEM_TYPE] =
                (byte) LegoProtocolConstants.DEVICE_2PORT_HUB;
        when(device.id()).thenReturn("target-id");
        when(device.manufacturerData()).thenReturn(mfrData);

        final ScanDsl dsl = new ScanDsl(scanner)
                .withDeviceId("target-id")
                .forLegoHubType(LegoHubType.CITY_HUB);

        assertTrue(dsl.deviceFilter().test(device));
    }

    /* ─────────────────────────────────────────────────────────────────────────
       stopScanAndAwait — error handling branches
       ───────────────────────────────────────────────────────────────────────── */

    /**
     * Verifies that an {@link InterruptedException} thrown by
     * {@code scanner.stopScan().get()} is swallowed (not propagated) and that
     * the thread's interrupt flag is restored.
     */
    @Test
    void collect_stopScanInterrupted_doesNotPropagateAndRestoresInterruptFlag()
            throws BleException, InterruptedException {
        final BleScanner scanner = mock(BleScanner.class);
        final BleDevice device = mock(BleDevice.class);

        when(scanner.startScan(isNull(), any(ScanCallback.class)))
                .thenAnswer(inv -> {
                    final ScanCallback cb = inv.getArgument(1);
                    cb.onDeviceFound(device);
                    return CompletableFuture.completedFuture(null);
                });

        // stopScan returns a future that, when get() is called, interrupts the
        // calling thread and then throws InterruptedException.
        when(scanner.stopScan()).thenAnswer(inv -> {
            final CompletableFuture<Void> f = new CompletableFuture<>() {
                @Override
                public Void get() throws InterruptedException {
                    Thread.currentThread().interrupt();
                    throw new InterruptedException("stop interrupted");
                }
            };
            return f;
        });

        final AtomicReference<Boolean> interruptFlag = new AtomicReference<>();
        final AtomicReference<List<BleDevice>> result = new AtomicReference<>();

        // Run in a fresh thread so the interrupt flag does not leak
        final Thread thread = new Thread(() -> {
            try {
                result.set(new ScanDsl(scanner).collect(1));
            } catch (BleException ex) {
                result.set(null);
            }
            interruptFlag.set(Thread.currentThread().isInterrupted());
        });
        thread.start();
        thread.join(5000);

        assertNotNull(result.get(), "collect() should succeed despite stopScan interruption");
        assertEquals(Boolean.TRUE, interruptFlag.get(),
                "Interrupt flag should be restored after stopScanAndAwait");
    }

    /**
     * Verifies that an {@link ExecutionException} thrown by
     * {@code scanner.stopScan().get()} is swallowed (not propagated) and
     * {@code collect} still returns normally.
     */
    @Test
    void collect_stopScanExecutionException_doesNotPropagate() throws BleException {
        final BleScanner scanner = mock(BleScanner.class);
        final BleDevice device = mock(BleDevice.class);

        when(scanner.startScan(isNull(), any(ScanCallback.class)))
                .thenAnswer(inv -> {
                    final ScanCallback cb = inv.getArgument(1);
                    cb.onDeviceFound(device);
                    return CompletableFuture.completedFuture(null);
                });

        // stopScan returns a future that fails with a RuntimeException.
        final CompletableFuture<Void> failedStop = new CompletableFuture<>();
        failedStop.completeExceptionally(new RuntimeException("stop failed"));
        when(scanner.stopScan()).thenReturn(failedStop);

        final List<BleDevice> found = new ScanDsl(scanner).collect(1);

        assertNotNull(found);
        assertEquals(1, found.size());
    }

    /* ─────────────────────────────────────────────────────────────────────────
       LOG.fine lambda coverage — mfrData branches + filter paths at FINE level
       ───────────────────────────────────────────────────────────────────────── */

    /**
     * FINE-level lambda: mfrData is empty (length == 0) → hex is "" and byte3 is "n/a (len=0)".
     * Also exercises: device rejected by filter, device accepted by filter.
     */
    @Test
    void onDeviceFound_filterRejectsDevice_fineLogLambdaExecuted() throws BleException {
        final Logger scanDslLogger = Logger.getLogger(ScanDsl.class.getName());
        final Level savedLevel = scanDslLogger.getLevel();
        scanDslLogger.setLevel(Level.FINE);
        try {
            final BleScanner scanner = mock(BleScanner.class);
            final BleDevice rejected = mock(BleDevice.class);
            final BleDevice accepted = mock(BleDevice.class);
            when(rejected.id()).thenReturn("rejected-id");
            when(rejected.name()).thenReturn("Rejected");
            when(rejected.manufacturerData()).thenReturn(new byte[0]);
            when(accepted.id()).thenReturn("accepted-id");
            when(accepted.name()).thenReturn("Accepted");
            when(accepted.manufacturerData()).thenReturn(new byte[0]);

            when(scanner.startScan(isNull(), any(ScanCallback.class)))
                    .thenAnswer(inv -> {
                        final ScanCallback cb = inv.getArgument(1);
                        cb.onDeviceFound(rejected);
                        cb.onDeviceFound(accepted);
                        return CompletableFuture.completedFuture(null);
                    });
            when(scanner.stopScan()).thenReturn(CompletableFuture.completedFuture(null));

            final List<BleDevice> result =
                    new ScanDsl(scanner).withDeviceId("accepted-id").collect(1);

            assertEquals(1, result.size());
            assertEquals("accepted-id", result.get(0).id());
        } finally {
            scanDslLogger.setLevel(savedLevel);
        }
    }

    /**
     * FINE-level lambda: mfrData has fewer than MANUFACTURER_DATA_MIN_LENGTH bytes →
     * byte3 reports "n/a (len=N)".
     */
    @Test
    void onDeviceFound_fineLevelLog_mfrDataShort_byte3ReportsLength() throws BleException {
        final Logger scanDslLogger = Logger.getLogger(ScanDsl.class.getName());
        final Level savedLevel = scanDslLogger.getLevel();
        scanDslLogger.setLevel(Level.FINE);
        try {
            final BleScanner scanner = mock(BleScanner.class);
            final BleDevice device = mock(BleDevice.class);
            when(device.id()).thenReturn("dev-short");
            when(device.name()).thenReturn("Short");
            /* 2 bytes — below MANUFACTURER_DATA_MIN_LENGTH (4) */
            when(device.manufacturerData()).thenReturn(new byte[]{0x01, 0x02});

            when(scanner.startScan(isNull(), any(ScanCallback.class)))
                    .thenAnswer(inv -> {
                        final ScanCallback cb = inv.getArgument(1);
                        cb.onDeviceFound(device);
                        return CompletableFuture.completedFuture(null);
                    });
            when(scanner.stopScan()).thenReturn(CompletableFuture.completedFuture(null));

            final List<BleDevice> result = new ScanDsl(scanner).collect(1);

            assertEquals(1, result.size());
        } finally {
            scanDslLogger.setLevel(savedLevel);
        }
    }

    /**
     * FINE-level lambda: mfrData has at least MANUFACTURER_DATA_MIN_LENGTH bytes →
     * byte3 is formatted as "0xXX".
     */
    @Test
    void onDeviceFound_fineLevelLog_mfrDataFull_byte3FormattedAsHex() throws BleException {
        final Logger scanDslLogger = Logger.getLogger(ScanDsl.class.getName());
        final Level savedLevel = scanDslLogger.getLevel();
        scanDslLogger.setLevel(Level.FINE);
        try {
            final BleScanner scanner = mock(BleScanner.class);
            final BleDevice device = mock(BleDevice.class);
            when(device.id()).thenReturn("dev-full");
            when(device.name()).thenReturn("Full");
            /* 10 bytes — LEGO full manufacturer data, byte[3] = 0x20 */
            when(device.manufacturerData()).thenReturn(
                    new byte[]{(byte) 0x97, 0x03, 0x00, 0x20, 0x00, 0x00, 0x00, 0x00, 0x00, 0x00});

            when(scanner.startScan(isNull(), any(ScanCallback.class)))
                    .thenAnswer(inv -> {
                        final ScanCallback cb = inv.getArgument(1);
                        cb.onDeviceFound(device);
                        return CompletableFuture.completedFuture(null);
                    });
            when(scanner.stopScan()).thenReturn(CompletableFuture.completedFuture(null));

            final List<BleDevice> result = new ScanDsl(scanner).collect(1);

            assertEquals(1, result.size());
        } finally {
            scanDslLogger.setLevel(savedLevel);
        }
    }
}
