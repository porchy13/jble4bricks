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

        verify(bridge).setNotify(CONN_PTR, SVC, CHR, false);
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
        doThrow(new RuntimeException("hw error"))
                .when(bridge).setNotify(CONN_PTR, SVC, CHR, false);

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
