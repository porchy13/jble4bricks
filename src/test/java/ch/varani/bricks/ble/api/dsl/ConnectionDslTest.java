package ch.varani.bricks.ble.api.dsl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Flow.Publisher;

import org.junit.jupiter.api.BeforeEach;
import org.junit.jupiter.api.Test;

import ch.varani.bricks.ble.api.BleConnection;
import ch.varani.bricks.ble.api.BleDevice;
import ch.varani.bricks.ble.api.BleException;

/**
 * Unit tests for {@link ConnectionDsl}.
 */
class ConnectionDslTest {

    private BleConnection connection;
    private ConnectionDsl dsl;

    @BeforeEach
    void setUp() {
        connection = mock(BleConnection.class);
        dsl = new ConnectionDsl(connection);
    }

    @Test
    void device_delegatesToConnection() {
        final BleDevice device = mock(BleDevice.class);
        when(connection.device()).thenReturn(device);

        assertSame(device, dsl.device());
    }

    @Test
    void writeWithoutResponse_delegatesToConnection() {
        when(connection.writeWithoutResponse(any(), any(), any()))
                .thenReturn(CompletableFuture.completedFuture(null));

        dsl.writeWithoutResponse("svc", "char", new byte[]{0x01});

        verify(connection).writeWithoutResponse("svc", "char", new byte[]{0x01});
    }

    @Test
    @SuppressWarnings("unchecked")
    void notifications_delegatesToConnection() {
        final Publisher<byte[]> publisher = mock(Publisher.class);
        when(connection.notifications(any(), any())).thenReturn(publisher);

        assertSame(publisher, dsl.notifications("svc", "char"));
    }

    @Test
    void read_delegatesToConnection() {
        when(connection.read(any(), any()))
                .thenReturn(CompletableFuture.completedFuture(new byte[0]));

        dsl.read("svc", "char");

        verify(connection).read("svc", "char");
    }

    @Test
    void disconnect_delegatesToConnection() {
        when(connection.disconnect())
                .thenReturn(CompletableFuture.completedFuture(null));

        dsl.disconnect();

        verify(connection).disconnect();
    }

    @Test
    void asLego_returnsNonNullLegoDsl() {
        assertNotNull(dsl.asLego());
    }

    @Test
    void asSBrick_returnsNonNullSBrickDsl() {
        assertNotNull(dsl.asSBrick());
    }

    @Test
    void asCircuitCubes_returnsNonNullCircuitCubesDsl() {
        assertNotNull(dsl.asCircuitCubes());
    }

    @Test
    void asBuWizz2_returnsNonNullBuWizz2Dsl() {
        assertNotNull(dsl.asBuWizz2());
    }

    @Test
    void asBuWizz3_returnsNonNullBuWizz3Dsl() {
        assertNotNull(dsl.asBuWizz3());
    }

    @Test
    void asWeDo2_returnsNonNullWeDo2Dsl() {
        assertNotNull(dsl.asWeDo2());
    }

    @Test
    void connection_returnsWrappedConnection() {
        assertSame(connection, dsl.connection());
    }

    @Test
    void done_delegatesToConnectionClose() throws BleException {
        dsl.done();
        verify(connection).close();
    }

    @Test
    void close_delegatesToDone() throws BleException {
        dsl.close();
        verify(connection).close();
    }
}
