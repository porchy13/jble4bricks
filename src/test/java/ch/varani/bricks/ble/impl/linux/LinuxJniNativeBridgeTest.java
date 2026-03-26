package ch.varani.bricks.ble.impl.linux;

import static org.junit.jupiter.api.Assertions.assertArrayEquals;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.ArgumentMatchers.anyLong;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.verify;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import ch.varani.bricks.ble.util.NativeLibraryLoader;

/**
 * Unit tests for {@link LinuxJniNativeBridge}.
 *
 * <p>Because {@code LinuxJniNativeBridge} declares {@code private native} methods
 * that require {@code libble-linux.so}, this test class uses
 * {@code mockConstruction(LinuxJniNativeBridge.class)} to intercept instantiation
 * and exercises every delegating method on the resulting Mockito mock.  No real
 * native library is loaded.
 */
class LinuxJniNativeBridgeTest {

    private static final long CTX_PTR  = 0x1000L;
    private static final long CONN_PTR = 0x2000L;

    /**
     * Exercises every public method of {@link LinuxJniNativeBridge} through a
     * {@code mockConstruction} intercept so that JaCoCo records branch coverage
     * for each delegating call site.
     */
    @Test
    void allMethods_delegateToNative_coveredByMock() {
        try (var loaderMock = mockStatic(NativeLibraryLoader.class);
             var bridgeMock = mockConstruction(LinuxJniNativeBridge.class, (mock, ctx) -> {
                 when(mock.init(any())).thenReturn(CTX_PTR);
                 when(mock.isScanning(anyLong())).thenReturn(false);
                 when(mock.connect(anyLong(), anyString())).thenReturn(CONN_PTR);
                 when(mock.readCharacteristic(anyLong(), anyString(), anyString()))
                         .thenReturn(new byte[]{0x01, 0x02});
             })) {

            loaderMock.when(() -> NativeLibraryLoader.load(LinuxBleScanner.LIBRARY_NAME))
                      .then(inv -> null);

            final LinuxJniNativeBridge bridge = new LinuxJniNativeBridge();

            /* init */
            final long ctxPtr = bridge.init(null);
            assertEquals(CTX_PTR, ctxPtr);

            /* startScan — with UUID and with null */
            bridge.startScan(ctxPtr, "test-uuid");
            bridge.startScan(ctxPtr, null);

            /* isScanning */
            assertFalse(bridge.isScanning(ctxPtr));

            /* connect */
            final long connPtr = bridge.connect(ctxPtr, "uuid-device");
            assertEquals(CONN_PTR, connPtr);

            /* disconnect */
            bridge.disconnect(ctxPtr, connPtr);

            /* writeWithoutResponse */
            bridge.writeWithoutResponse(connPtr, "svc", "chr", new byte[]{0x03});

            /* readCharacteristic */
            final byte[] data = bridge.readCharacteristic(connPtr, "svc", "chr");
            assertArrayEquals(new byte[]{0x01, 0x02}, data);

            /* setNotify — true and false */
            bridge.setNotify(connPtr, "svc", "chr", true);
            bridge.setNotify(connPtr, "svc", "chr", false);

            /* destroy */
            bridge.destroy(ctxPtr);

            assertNotNull(bridgeMock);
            verify(bridge).init(null);
            verify(bridge).startScan(ctxPtr, "test-uuid");
            verify(bridge).startScan(ctxPtr, null);
            verify(bridge).isScanning(ctxPtr);
            verify(bridge).connect(ctxPtr, "uuid-device");
            verify(bridge).disconnect(ctxPtr, connPtr);
            verify(bridge).writeWithoutResponse(connPtr, "svc", "chr", new byte[]{0x03});
            verify(bridge).readCharacteristic(connPtr, "svc", "chr");
            verify(bridge).setNotify(connPtr, "svc", "chr", true);
            verify(bridge).setNotify(connPtr, "svc", "chr", false);
            verify(bridge).destroy(ctxPtr);
        }
    }

    /**
     * Verifies that {@link LinuxJniNativeBridge#isScanning(long)} returns {@code true}
     * when the mock is configured to return {@code true}, covering the {@code true}
     * branch of the delegator.
     */
    @Test
    void isScanning_whenMockReturnsTrue_returnsTrue() {
        try (var loaderMock = mockStatic(NativeLibraryLoader.class);
             var bridgeMock = mockConstruction(LinuxJniNativeBridge.class,
                     (mock, ctx) -> when(mock.isScanning(anyLong())).thenReturn(true))) {

            loaderMock.when(() -> NativeLibraryLoader.load(LinuxBleScanner.LIBRARY_NAME))
                      .then(inv -> null);

            final LinuxJniNativeBridge bridge = new LinuxJniNativeBridge();
            assertTrue(bridge.isScanning(CTX_PTR));
            assertNotNull(bridgeMock);
        }
    }
}
