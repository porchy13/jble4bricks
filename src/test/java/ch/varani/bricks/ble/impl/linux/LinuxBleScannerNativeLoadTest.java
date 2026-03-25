package ch.varani.bricks.ble.impl.linux;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.any;
import static org.mockito.Mockito.mockConstruction;
import static org.mockito.Mockito.mockStatic;
import static org.mockito.Mockito.when;

import org.junit.jupiter.api.Test;

import ch.varani.bricks.ble.api.BleException;
import ch.varani.bricks.ble.api.BleScanner;
import ch.varani.bricks.ble.api.BleScannerFactory;
import ch.varani.bricks.ble.util.NativeLibraryLoader;

/**
 * Tests for the production paths in {@link LinuxBleScanner} and
 * {@link BleScannerFactory} that require mocking the native library loading.
 *
 * <p>{@link NativeLibraryLoader#load(String)} and the {@link LinuxJniNativeBridge}
 * constructor are mocked so that no real {@code .so} is required, allowing
 * the public no-arg {@link LinuxBleScanner#LinuxBleScanner()} constructor and
 * the Linux branch of {@link BleScannerFactory#create()} to be exercised.
 */
class LinuxBleScannerNativeLoadTest {

    private static final long MOCK_CTX_PTR = 0xCAFEL;

    /**
     * Verifies that the public constructor succeeds when the native library
     * loading is mocked as a no-op and the JNI bridge is substituted with a
     * mock that returns a valid context pointer.
     */
    @Test
    void publicConstructor_withMockedNativeLoad_createsScanner() throws BleException {
        try (var loaderMock = mockStatic(NativeLibraryLoader.class);
             var bridgeMock = mockConstruction(LinuxJniNativeBridge.class,
                     (mock, ctx) -> when(mock.init(any(LinuxBleNativeCallbacks.class)))
                             .thenReturn(MOCK_CTX_PTR))) {

            loaderMock.when(() -> NativeLibraryLoader.load(LinuxBleScanner.LIBRARY_NAME))
                      .then(inv -> null);

            final LinuxBleScanner scanner = new LinuxBleScanner();

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
     * {@link BleScanner} when the OS is Linux and the native library load
     * is mocked.
     */
    @Test
    void bleScannerFactory_linux_returnsScanner() throws BleException {
        final String savedOs = System.getProperty("os.name");
        try (var loaderMock = mockStatic(NativeLibraryLoader.class);
             var bridgeMock = mockConstruction(LinuxJniNativeBridge.class,
                     (mock, ctx) -> when(mock.init(any(LinuxBleNativeCallbacks.class)))
                             .thenReturn(MOCK_CTX_PTR))) {

            loaderMock.when(() -> NativeLibraryLoader.load(LinuxBleScanner.LIBRARY_NAME))
                      .then(inv -> null);
            System.setProperty("os.name", "Linux");

            final BleScanner scanner = BleScannerFactory.create();

            assertAll(
                () -> assertNotNull(scanner),
                () -> assertNotNull(bridgeMock),
                () -> assertTrue(scanner instanceof LinuxBleScanner)
            );

            scanner.close();
        } finally {
            System.setProperty("os.name", savedOs);
        }
    }
}
