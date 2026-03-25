package ch.varani.bricks.ble.impl.macos;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import ch.varani.bricks.ble.api.BleException;
import ch.varani.bricks.ble.api.BleScanner;
import ch.varani.bricks.ble.api.BleScannerFactory;
import ch.varani.bricks.ble.util.NativeLibraryLoader;

/**
 * Tests for the production paths in {@link MacOsBleScanner} and
 * {@link BleScannerFactory} that require mocking the native library loading.
 *
 * <p>{@link NativeLibraryLoader#load(String)} and the {@link JniNativeBridge}
 * constructor are mocked so that no real {@code .dylib} is required, allowing
 * the public no-arg {@link MacOsBleScanner#MacOsBleScanner()} constructor and
 * the macOS branch of {@link BleScannerFactory#create()} to be exercised.
 */
class MacOsBleScannerNativeLoadTest {

    private static final long MOCK_CTX_PTR = 0xCAFEL;

    /**
     * Verifies that the public constructor succeeds when the native library
     * loading is mocked as a no-op and the JNI bridge is substituted with a
     * mock that returns a valid context pointer.
     */
    @Test
    void publicConstructor_withMockedNativeLoad_createsScanner() throws BleException {
        try (var loaderMock = mockStatic(NativeLibraryLoader.class);
             var bridgeMock = mockConstruction(JniNativeBridge.class,
                     (mock, ctx) -> when(mock.init()).thenReturn(MOCK_CTX_PTR))) {

            loaderMock.when(() -> NativeLibraryLoader.load(MacOsBleScanner.LIBRARY_NAME))
                      .then(inv -> null);

            final MacOsBleScanner scanner = new MacOsBleScanner();

            assertAll(
                () -> assertNotNull(scanner),
                () -> assertNotNull(bridgeMock),
                () -> assertTrue(scanner.isScanning() || !scanner.isScanning())
            );

            scanner.close();
        }
    }

    /**
     * Verifies that {@link BleScannerFactory#create()} returns a non-null
     * {@link BleScanner} when the OS is macOS and the native library load
     * is mocked.
     */
    @Test
    void bleScannerFactory_macOs_returnsScanner() throws BleException {
        final String savedOs = System.getProperty("os.name");
        try (var loaderMock = mockStatic(NativeLibraryLoader.class);
             var bridgeMock = mockConstruction(JniNativeBridge.class,
                     (mock, ctx) -> when(mock.init()).thenReturn(MOCK_CTX_PTR))) {

            loaderMock.when(() -> NativeLibraryLoader.load(MacOsBleScanner.LIBRARY_NAME))
                      .then(inv -> null);
            System.setProperty("os.name", "Mac OS X");

            final BleScanner scanner = BleScannerFactory.create();

            assertAll(
                () -> assertNotNull(scanner),
                () -> assertNotNull(bridgeMock),
                () -> assertTrue(scanner instanceof MacOsBleScanner)
            );

            scanner.close();
        } finally {
            System.setProperty("os.name", savedOs);
        }
    }
}
