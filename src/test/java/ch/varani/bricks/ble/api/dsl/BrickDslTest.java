package ch.varani.bricks.ble.api.dsl;

import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.mockito.Mockito.mock;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import ch.varani.bricks.ble.api.BleException;
import ch.varani.bricks.ble.api.BleScanner;
import ch.varani.bricks.ble.api.BleScannerFactory;

/**
 * Unit tests for {@link BrickDsl}.
 */
class BrickDslTest {

    @Test
    void scan_returnsNonNullScanDsl() {
        final BleScanner scanner = mock(BleScanner.class);
        final BrickDsl dsl = new BrickDsl(scanner);

        final ScanDsl result = dsl.scan();

        assertNotNull(result);
    }

    @Test
    void scanner_returnsInjectedScanner() {
        final BleScanner scanner = mock(BleScanner.class);
        final BrickDsl dsl = new BrickDsl(scanner);

        assertSame(scanner, dsl.scanner());
    }

    @Test
    void close_delegatesToScannerClose() throws BleException {
        final BleScanner scanner = mock(BleScanner.class);
        final BrickDsl dsl = new BrickDsl(scanner);

        dsl.close();

        verify(scanner).close();
    }

    @Test
    void close_propagatesBleException() throws BleException {
        final BleScanner scanner = mock(BleScanner.class);
        org.mockito.Mockito.doThrow(new BleException("close failed"))
                .when(scanner).close();
        final BrickDsl dsl = new BrickDsl(scanner);

        assertThrows(BleException.class, dsl::close);
    }

    /**
     * Verifies that {@link BrickDsl#open()} delegates to
     * {@link BleScannerFactory#create()} and returns a non-null {@link BrickDsl}.
     */
    @Test
    void open_usesScannerFactoryAndReturnsNonNull() throws BleException {
        final BleScanner scanner = mock(BleScanner.class);
        try (MockedStatic<BleScannerFactory> factory = mockStatic(BleScannerFactory.class)) {
            factory.when(BleScannerFactory::create).thenReturn(scanner);
            final BrickDsl dsl = BrickDsl.open();
            assertNotNull(dsl);
            assertSame(scanner, dsl.scanner());
        }
    }
}
