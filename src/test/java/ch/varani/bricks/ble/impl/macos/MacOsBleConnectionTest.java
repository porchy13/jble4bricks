package ch.varani.bricks.ble.impl.macos;

import static org.awaitility.Awaitility.await;
import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.doThrow;
import static org.mockito.Mockito.verify;

import java.time.Duration;
import java.util.ArrayList;
import java.util.List;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;
import java.util.concurrent.Flow.Subscriber;
import java.util.concurrent.Flow.Subscription;
import java.util.concurrent.atomic.AtomicReference;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.extension.ExtendWith;
import org.mockito.Mock;
import org.mockito.junit.jupiter.MockitoExtension;

import ch.varani.bricks.ble.api.BleException;

/**
 * Unit tests for {@link MacOsBleConnection}.
 *
 * <p>All native calls are isolated via the {@link NativeBridge} mock injected
 * into the owning {@link MacOsBleScanner}.  No shared library is loaded.
 */
@ExtendWith(MockitoExtension.class)
class MacOsBleConnectionTest {

    private static final long CTX_PTR  = 0x1000L;
    private static final long CONN_PTR = 0x2000L;
    private static final String SVC  = "0000-svc";
    private static final String CHR  = "0000-chr";

    @Mock
    private NativeBridge bridge;

    private MacOsBleScanner scanner;
    private MacOsBleDevice  device;
    private MacOsBleConnection connection;

    @BeforeEach
    void setUp() {
        scanner    = new MacOsBleScanner(bridge, CTX_PTR);
        device     = new MacOsBleDevice("uuid-1", "TestDev", -55, new byte[0], scanner);
        connection = new MacOsBleConnection(CONN_PTR, CTX_PTR, scanner, device);
    }

    /* ─────────────────────────────────────────────────────────────────────────
       Constructor / pointer accessors
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void connectionPtr_returnsInjectedPtr() {
        assertEquals(CONN_PTR, connection.connectionPtr());
    }

    @Test
    void contextPtr_returnsInjectedPtr() {
        assertEquals(CTX_PTR, connection.contextPtr());
    }

    /* ─────────────────────────────────────────────────────────────────────────
       device()
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void device_whenDeviceProvided_returnsDevice() {
        assertSame(device, connection.device());
    }

    @Test
    void device_whenDeviceNotProvided_throwsIllegalStateException() {
        final MacOsBleConnection noDevice = new MacOsBleConnection(CONN_PTR, CTX_PTR, scanner);
        assertThrows(IllegalStateException.class, noDevice::device);
    }

    /* ─────────────────────────────────────────────────────────────────────────
       writeWithoutResponse
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void writeWithoutResponse_delegatesToScanner() throws Exception {
        final byte[] data = {0x01, 0x02, 0x03};
        connection.writeWithoutResponse(SVC, CHR, data).get();

        verify(bridge).writeWithoutResponse(CONN_PTR, SVC, CHR, data);
    }

    @Test
    void writeWithoutResponse_afterDisconnect_throwsIllegalStateException() throws Exception {
        connection.disconnect().get();

        assertThrows(
            IllegalStateException.class,
            () -> connection.writeWithoutResponse(SVC, CHR, new byte[0])
        );
    }

    /* ─────────────────────────────────────────────────────────────────────────
       read
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void read_delegatesToScanner() throws Exception {
        final byte[] expected = {0x0A, 0x0B};
        org.mockito.Mockito.when(bridge.readCharacteristic(CONN_PTR, SVC, CHR))
                .thenReturn(expected);

        final byte[] result = connection.read(SVC, CHR).get();
        assertArrayEquals(expected, result);
    }

    @Test
    void read_afterDisconnect_throwsIllegalStateException() throws Exception {
        connection.disconnect().get();

        assertThrows(IllegalStateException.class, () -> connection.read(SVC, CHR));
    }

    /* ─────────────────────────────────────────────────────────────────────────
       notifications
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void notifications_firstCall_enablesNotifyOnBridge() {
        connection.notifications(SVC, CHR);
        verify(bridge).setNotify(CONN_PTR, SVC, CHR, true);
    }

    @Test
    void notifications_samePairTwice_returnsSamePublisher() {
        final Publisher<byte[]> p1 = connection.notifications(SVC, CHR);
        final Publisher<byte[]> p2 = connection.notifications(SVC, CHR);
        assertSame(p1, p2);
    }

    @Test
    void notifications_afterDisconnect_throwsIllegalStateException() throws Exception {
        connection.disconnect().get();

        assertThrows(IllegalStateException.class, () -> connection.notifications(SVC, CHR));
    }

    @Test
    void onNotification_withActiveSubscriber_deliversValue() throws Exception {
        final Publisher<byte[]> publisher = connection.notifications(SVC, CHR);

        final List<byte[]> received = new ArrayList<>();
        final AtomicReference<Subscription> subRef = new AtomicReference<>();

        publisher.subscribe(new Subscriber<byte[]>() {
            @Override
            public void onSubscribe(final Subscription s) {
                subRef.set(s);
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(final byte[] item) {
                received.add(item);
            }

            @Override
            public void onError(final Throwable t) { /* not expected */ }

            @Override
            public void onComplete() { /* not expected */ }
        });

        final byte[] value = {0x01, 0x02};
        connection.onNotification(SVC, CHR, value);

        await().atMost(Duration.ofSeconds(2))
               .until(() -> !received.isEmpty());

        assertAll(
            () -> assertEquals(1, received.size()),
            () -> assertArrayEquals(value, received.get(0))
        );
    }

    @Test
    void onNotification_withNoSubscriber_doesNotThrow() {
        /* No call to notifications() — publisher map is empty. */
        connection.onNotification(SVC, CHR, new byte[]{0x01});
        /* Reaching here without exception is the assertion. */
        assertNotNull(connection);
    }

    /**
     * Regression test for the CoreBluetooth UUID case-mismatch bug.
     *
     * <p>Java constants use lower-case UUID strings; CoreBluetooth's
     * {@code UUIDString} property always returns upper-case strings.
     * The notification publisher must be found regardless of which case was
     * used to register it versus which case arrives from the native layer.
     */
    @Test
    void onNotification_lowerCaseRegistration_upperCaseCallback_deliversValue()
            throws Exception {

        final String svcLower = "00001623-1212-efde-1623-785feabcd123";
        final String chrLower = "00001624-1212-efde-1623-785feabcd123";
        final String svcUpper = svcLower.toUpperCase(java.util.Locale.ROOT);
        final String chrUpper = chrLower.toUpperCase(java.util.Locale.ROOT);

        /* Register with lower-case constants (as Java code does). */
        final Publisher<byte[]> publisher = connection.notifications(svcLower, chrLower);

        final List<byte[]> received = new ArrayList<>();
        final AtomicReference<Subscription> subRef = new AtomicReference<>();
        publisher.subscribe(new Subscriber<byte[]>() {
            @Override
            public void onSubscribe(final Subscription s) {
                subRef.set(s);
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(final byte[] item) {
                received.add(item);
            }

            @Override
            public void onError(final Throwable t)  { /* not expected */ }

            @Override
            public void onComplete()                 { /* not expected */ }
        });

        /* Simulate CoreBluetooth callback arriving with upper-case UUIDs. */
        final byte[] value = {0x05, 0x00, 0x01, 0x06, 0x06, 0x42};
        connection.onNotification(svcUpper, chrUpper, value);

        await().atMost(Duration.ofSeconds(2))
               .until(() -> !received.isEmpty());

        assertAll(
            () -> assertEquals(1, received.size()),
            () -> assertArrayEquals(value, received.get(0))
        );
    }

    /**
     * Verifies that registering with mixed-case UUIDs for the same logical
     * characteristic returns the same publisher (idempotent key lookup).
     */
    @Test
    void notifications_mixedCaseUuids_samePair_returnsSamePublisher() {
        final String svcLower = "00001623-1212-efde-1623-785feabcd123";
        final String chrLower = "00001624-1212-efde-1623-785feabcd123";
        final String svcUpper = svcLower.toUpperCase(java.util.Locale.ROOT);
        final String chrUpper = chrLower.toUpperCase(java.util.Locale.ROOT);

        final Publisher<byte[]> p1 = connection.notifications(svcLower, chrLower);
        final Publisher<byte[]> p2 = connection.notifications(svcUpper, chrUpper);

        assertSame(p1, p2);
    }

    /* ─────────────────────────────────────────────────────────────────────────
       disconnect
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void disconnect_callsBridgeDisconnect() throws Exception {
        connection.disconnect().get();
        verify(bridge).disconnect(CTX_PTR, CONN_PTR);
    }

    @Test
    void disconnect_whenNotifyEnabled_disablesNotifyBeforeDisconnect() throws Exception {
        connection.notifications(SVC, CHR);
        connection.disconnect().get();

        /*
         * CharacteristicKey normalises UUIDs to upper-case on construction, so
         * closeNotificationPublishers() iterates upper-case keys and passes the
         * upper-case forms to setNotify(). The registration call (true) still
         * receives the original caller-supplied string; only the disable call
         * (false) uses the normalised key.
         */
        verify(bridge).setNotify(CONN_PTR, SVC.toUpperCase(java.util.Locale.ROOT),
                CHR.toUpperCase(java.util.Locale.ROOT), false);
        verify(bridge).disconnect(CTX_PTR, CONN_PTR);
    }

    @Test
    void disconnect_calledTwice_isIdempotent() throws Exception {
        connection.disconnect().get();
        final CompletableFuture<Void> second = connection.disconnect();

        assertAll(
            () -> assertTrue(second.isDone()),
            () -> assertTrue(second.isDone() && !second.isCompletedExceptionally())
        );
        /* Bridge disconnect must be called only once. */
        verify(bridge).disconnect(CTX_PTR, CONN_PTR);
    }

    @Test
    void disconnect_whenNotifyDisableFails_logsAndContinues() throws Exception {
        connection.notifications(SVC, CHR);
        /*
         * closeNotificationPublishers() calls setNotify with the upper-case
         * normalised key — stub with the same upper-case forms so the exception
         * is triggered on the actual call path.
         */
        doThrow(new RuntimeException("hw error"))
                .when(bridge).setNotify(CONN_PTR, SVC.toUpperCase(java.util.Locale.ROOT),
                        CHR.toUpperCase(java.util.Locale.ROOT), false);

        /* Should not throw — warning is logged internally. */
        connection.disconnect().get();

        verify(bridge).disconnect(CTX_PTR, CONN_PTR);
    }

    /* ─────────────────────────────────────────────────────────────────────────
       close (AutoCloseable)
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void close_delegatesToDisconnect() throws BleException {
        connection.close();
        verify(bridge).disconnect(CTX_PTR, CONN_PTR);
    }

    @Test
    void close_whenInterrupted_setsInterruptFlag() {
        /*
         * Interrupt the current thread before calling close().
         * The close() implementation calls disconnect().get(), which will
         * throw InterruptedException because the thread is already interrupted.
         */
        Thread.currentThread().interrupt();
        final BleException ex = assertThrows(BleException.class, connection::close);
        assertAll(
            () -> assertTrue(ex.getMessage().contains("interrupted")
                    || ex.getCause() instanceof InterruptedException),
            () -> assertTrue(Thread.interrupted()) /* clears flag; also asserts it was set */
        );
    }

    @Test
    void close_whenDisconnectFails_wrapsToBleException() {
        /*
         * Make bridge.disconnect() throw so that the runAsync future
         * completes exceptionally, causing disconnect().get() inside close()
         * to throw an ExecutionException whose cause is a RuntimeException.
         */
        doThrow(new RuntimeException("native disconnect error"))
                .when(bridge).disconnect(CTX_PTR, CONN_PTR);

        final BleException ex = assertThrows(BleException.class, connection::close);
        assertTrue(ex.getMessage().contains("Disconnect failed")
                || ex.getCause() != null);
    }

    /* ─────────────────────────────────────────────────────────────────────────
       NoOpSubscription inner class
       ───────────────────────────────────────────────────────────────────────── */

    @Test
    void noOpSubscription_request_doesNotThrow() {
        MacOsBleConnection.NoOpSubscription.INSTANCE.request(1L);
        /* No exception = pass */
        assertNotNull(MacOsBleConnection.NoOpSubscription.INSTANCE);
    }

    @Test
    void noOpSubscription_cancel_doesNotThrow() {
        MacOsBleConnection.NoOpSubscription.INSTANCE.cancel();
        assertNotNull(MacOsBleConnection.NoOpSubscription.INSTANCE);
    }

    @Test
    void noOpSubscription_isSingleton() {
        assertSame(
            MacOsBleConnection.NoOpSubscription.INSTANCE,
            MacOsBleConnection.NoOpSubscription.INSTANCE
        );
    }

    /* ─────────────────────────────────────────────────────────────────────────
       expandUuid
       ───────────────────────────────────────────────────────────────────────── */

    /**
     * Verifies that a 16-bit SIG UUID string is expanded to its full 128-bit form.
     *
     * <p>CoreBluetooth returns {@code "180F"} for the Battery Service; the Java
     * constant stores it as {@code "0000180F-0000-1000-8000-00805F9B34FB"}.
     */
    @Test
    void expandUuid_16bitUuid_expandsToFull128Bit() {
        assertEquals(
            "0000180F-0000-1000-8000-00805F9B34FB",
            MacOsBleConnection.expandUuid("180F")
        );
    }

    /**
     * Verifies that a 32-bit SIG UUID string is expanded to its full 128-bit form.
     */
    @Test
    void expandUuid_32bitUuid_expandsToFull128Bit() {
        assertEquals(
            "0000180F-0000-1000-8000-00805F9B34FB",
            MacOsBleConnection.expandUuid("0000180F")
        );
    }

    /**
     * Verifies that a UUID already in full 128-bit form is returned unchanged.
     */
    @Test
    void expandUuid_full128BitUuid_returnedUnchanged() {
        final String full = "0000180F-0000-1000-8000-00805F9B34FB";
        assertEquals(full, MacOsBleConnection.expandUuid(full));
    }

    /**
     * Verifies that a non-standard UUID string (not 4 or 8 pure-hex characters) is
     * returned unchanged — the method must not throw on unknown lengths or non-hex content.
     */
    @Test
    void expandUuid_nonStandardLength_returnedUnchanged() {
        final String arbitrary = "0000-svc-A";
        assertEquals(arbitrary, MacOsBleConnection.expandUuid(arbitrary));
    }

    /**
     * Verifies that an 8-character string containing non-hex characters is NOT expanded.
     * This guards the hex-only validation path in {@link MacOsBleConnection#expandUuid}.
     */
    @Test
    void expandUuid_8charNonHex_returnedUnchanged() {
        /* "0000-SVC" is 8 chars but contains a hyphen — not pure hex. */
        final String nonHex = "0000-SVC";
        assertEquals(nonHex, MacOsBleConnection.expandUuid(nonHex));
    }

    /**
     * Verifies that a 4-character string containing non-hex characters is NOT expanded.
     */
    @Test
    void expandUuid_4charNonHex_returnedUnchanged() {
        /* "SV-C" is 4 chars but contains a hyphen — not pure hex. */
        final String nonHex = "SV-C";
        assertEquals(nonHex, MacOsBleConnection.expandUuid(nonHex));
    }

    /**
     * End-to-end: registers a publisher under the full 128-bit Battery Service UUID
     * (as stored in {@code LegoProtocolConstants.WEDO2_BATTERY_SERVICE_UUID}) and
     * verifies that a notification arriving with CoreBluetooth's short-form UUIDs
     * ({@code "180F"} / {@code "2A19"}) is correctly routed — no fallback needed.
     *
     * <p>This is the exact scenario that was previously causing the
     * {@code "notification dropped"} log entries for the City Hub.
     */
    @Test
    void onNotification_shortFormSigUuid_expandedAndDelivered() throws Exception {
        /* Full 128-bit form as stored by Java constants (lower-case). */
        final String fullSvc = "0000180f-0000-1000-8000-00805f9b34fb";
        final String fullChr = "00002a19-0000-1000-8000-00805f9b34fb";

        final Publisher<byte[]> publisher = connection.notifications(fullSvc, fullChr);

        final List<byte[]> received = new ArrayList<>();
        publisher.subscribe(new Subscriber<byte[]>() {
            @Override
            public void onSubscribe(final Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(final byte[] item) {
                received.add(item);
            }

            @Override
            public void onError(final Throwable t)  { /* not expected */ }

            @Override
            public void onComplete()                 { /* not expected */ }
        });

        /* CoreBluetooth delivers notification with short-form UUIDs. */
        final byte[] value = {(byte) 0x5A};
        connection.onNotification("180F", "2A19", value);

        await().atMost(Duration.ofSeconds(2))
               .until(() -> !received.isEmpty());

        assertAll(
            () -> assertEquals(1, received.size()),
            () -> assertArrayEquals(value, received.get(0))
        );
    }

    /**
     * Verifies the chr-UUID-only fallback in
     * {@link MacOsBleConnection#onNotification(String, String, byte[])}.
     *
     * <p>If a subscriber registers under service UUID {@code A} but CoreBluetooth
     * delivers the notification under a different service UUID {@code B} for the
     * same characteristic, the fallback scan must still route the notification to
     * the registered publisher.
     *
     * <p>Note: the WeDo 2.0 sensor characteristic {@code 0x1560} used to trigger
     * this path because it was registered under service {@code 0x1523} while
     * CoreBluetooth reported it under {@code 0x4F0E}.  That mismatch has been
     * corrected in {@link ch.varani.bricks.ble.api.dsl.WeDo2Dsl#sensorNotifications()} (now uses
     * {@code WEDO2_SERVICE_2_UUID}).  The fallback is retained as a safety net for
     * other devices or future characteristics where the service boundary may not
     * be known in advance.
     */
    @Test
    void onNotification_differentServiceUuid_chrOnlyFallback_deliversValue() throws Exception {
        final String registeredSvc = "00001523-1212-efde-1523-785feabcd123";
        final String actualSvc     = "00004F0E-1212-EFDE-1523-785FEABCD123";
        final String chr           = "00001560-1212-efde-1523-785feabcd123";

        /* Register under a different service UUID than what CoreBluetooth will report. */
        final Publisher<byte[]> publisher = connection.notifications(registeredSvc, chr);

        final List<byte[]> received = new ArrayList<>();
        publisher.subscribe(new Subscriber<byte[]>() {
            @Override
            public void onSubscribe(final Subscription s) {
                s.request(Long.MAX_VALUE);
            }

            @Override
            public void onNext(final byte[] item) {
                received.add(item);
            }

            @Override
            public void onError(final Throwable t)  { /* not expected */ }

            @Override
            public void onComplete()                 { /* not expected */ }
        });

        /* Notification arrives with the actual service UUID (0x4F0E). */
        final byte[] value = {0x23, 0x00, 0x10, 0x00};
        connection.onNotification(actualSvc, chr, value);

        await().atMost(Duration.ofSeconds(2))
               .until(() -> !received.isEmpty());

        assertAll(
            () -> assertEquals(1, received.size()),
            () -> assertArrayEquals(value, received.get(0))
        );
    }

    /**
     * Verifies that a notification whose characteristic UUID does not match any
     * registered publisher is silently dropped (no exception, no delivery).
     *
     * <p>This exercises the fallback scan path where the loop finds nothing.
     */
    @Test
    void onNotification_unknownChr_afterFallbackScan_isDropped() throws Exception {
        /* Register a publisher for one characteristic. */
        connection.notifications(SVC, CHR);

        /* Deliver a notification for a completely different characteristic. */
        connection.onNotification(SVC, "unknown-chr", new byte[]{(byte) 0x99});

        /* No assertion other than no exception; reaching here is the pass. */
        assertNotNull(connection);
    }

    /**
     * Ensures the {@code FINE}-level log lambdas inside
     * {@link MacOsBleConnection#onNotification} are executed (coverage).
     *
     * <p>Three paths are exercised while {@code FINE} logging is enabled:
     * <ol>
     *   <li>Exact-key miss + successful chr-only fallback match</li>
     *   <li>Exact-key miss + chr-only fallback finds nothing (dropped)</li>
     * </ol>
     */
    @Test
    void onNotification_fineLevelLogs_lambdasExecuted() throws Exception {
        final Logger logger = Logger.getLogger(MacOsBleConnection.class.getName());
        final Level savedLevel = logger.getLevel();
        logger.setLevel(Level.FINE);
        try {
            final String registeredSvc = "0000-svc-A";
            final String actualSvc     = "0000-svc-B";
            final String chr           = "0000-chr-A";

            /* Register under svc-A. */
            final Publisher<byte[]> publisher = connection.notifications(registeredSvc, chr);
            final List<byte[]> received = new ArrayList<>();
            publisher.subscribe(new Subscriber<byte[]>() {
                @Override
                public void onSubscribe(final Subscription s) {
                    s.request(Long.MAX_VALUE);
                }

                @Override
                public void onNext(final byte[] item) {
                    received.add(item);
                }

                @Override
                public void onError(final Throwable t)  { /* not expected */ }

                @Override
                public void onComplete()                 { /* not expected */ }
            });

            /* Path 1: exact-key miss → fallback match. */
            connection.onNotification(actualSvc, chr, new byte[]{0x01});
            await().atMost(Duration.ofSeconds(2)).until(() -> !received.isEmpty());

            /* Path 2: exact-key miss → fallback scan finds nothing → dropped. */
            connection.onNotification(actualSvc, "0000-chr-unknown", new byte[]{0x02});
        } finally {
            logger.setLevel(savedLevel);
        }
    }

    @Test
    void fineLevelLogs_writeNotificationsRead_lambdasExecuted() throws Exception {
        final Logger logger = Logger.getLogger(MacOsBleConnection.class.getName());
        final Level savedLevel = logger.getLevel();
        logger.setLevel(Level.FINE);
        try {
            org.mockito.Mockito.when(bridge.readCharacteristic(CONN_PTR, SVC, CHR))
                    .thenReturn(new byte[]{0x01});

            connection.writeWithoutResponse(SVC, CHR, new byte[]{0x42}).get();
            connection.notifications(SVC, CHR);
            connection.read(SVC, CHR).get();
        } finally {
            logger.setLevel(savedLevel);
        }
    }
}
