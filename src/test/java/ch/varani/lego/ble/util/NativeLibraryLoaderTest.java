package ch.varani.lego.ble.util;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;
import static org.mockito.ArgumentMatchers.anyString;
import static org.mockito.Mockito.CALLS_REAL_METHODS;
import static org.mockito.Mockito.mockStatic;

import java.io.IOException;
import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;
import java.nio.file.Files;
import java.util.logging.Level;
import java.util.logging.Logger;

import org.junit.jupiter.api.Test;
import org.mockito.MockedStatic;

import ch.varani.lego.ble.api.BleException;

/**
 * Unit tests for {@link NativeLibraryLoader}.
 *
 * <p>Tests cover all static helper methods directly without invoking
 * {@code System.load()} (which would require a real shared library on the
 * test classpath).  The {@link NativeLibraryLoader#load(String)} method
 * itself is tested through the "library not found" path by requesting a
 * name whose resource does not exist on the classpath, and through the
 * "library found but not loadable" path using dummy resource files placed
 * under {@code src/test/resources/natives/}.
 */
class NativeLibraryLoaderTest {

    @Test
    void constructor_isPrivate_throwsAssertionError() throws Exception {
        final Constructor<NativeLibraryLoader> ctor =
                NativeLibraryLoader.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final InvocationTargetException ex =
                assertThrows(InvocationTargetException.class, ctor::newInstance);
        assertThrows(AssertionError.class, () -> {
            throw ex.getCause();
        });
    }

    @Test
    void osName_macos_returnsMacos() {
        final String saved = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Mac OS X");
            assertEquals("macos", NativeLibraryLoader.osName());
        } finally {
            System.setProperty("os.name", saved);
        }
    }

    @Test
    void osName_windows_returnsWindows() {
        final String saved = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Windows 11");
            assertEquals("windows", NativeLibraryLoader.osName());
        } finally {
            System.setProperty("os.name", saved);
        }
    }

    @Test
    void osName_linux_returnsLinux() {
        final String saved = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Linux");
            assertEquals("linux", NativeLibraryLoader.osName());
        } finally {
            System.setProperty("os.name", saved);
        }
    }

    @Test
    void archName_aarch64_returnsAarch64() {
        final String saved = System.getProperty("os.arch");
        try {
            System.setProperty("os.arch", "aarch64");
            assertEquals("aarch64", NativeLibraryLoader.archName());
        } finally {
            System.setProperty("os.arch", saved);
        }
    }

    @Test
    void archName_arm64_returnsAarch64() {
        final String saved = System.getProperty("os.arch");
        try {
            System.setProperty("os.arch", "arm64");
            assertEquals("aarch64", NativeLibraryLoader.archName());
        } finally {
            System.setProperty("os.arch", saved);
        }
    }

    @Test
    void archName_amd64_returnsAmd64() {
        final String saved = System.getProperty("os.arch");
        try {
            System.setProperty("os.arch", "amd64");
            assertEquals("amd64", NativeLibraryLoader.archName());
        } finally {
            System.setProperty("os.arch", saved);
        }
    }

    @Test
    void archName_x86_64_returnsAmd64() {
        final String saved = System.getProperty("os.arch");
        try {
            System.setProperty("os.arch", "x86_64");
            assertEquals("amd64", NativeLibraryLoader.archName());
        } finally {
            System.setProperty("os.arch", saved);
        }
    }

    @Test
    void buildResourcePath_macos_aarch64_correctPath() {
        final String savedOs   = System.getProperty("os.name");
        final String savedArch = System.getProperty("os.arch");
        try {
            System.setProperty("os.name", "Mac OS X");
            System.setProperty("os.arch", "aarch64");
            final String path = NativeLibraryLoader.buildResourcePath("ble-macos");
            assertAll(
                () -> assertTrue(path.startsWith("/natives/macos/aarch64/")),
                () -> assertTrue(path.contains("ble-macos"))
            );
        } finally {
            System.setProperty("os.name", savedOs);
            System.setProperty("os.arch", savedArch);
        }
    }

    @Test
    void buildResourcePath_linux_amd64_correctPath() {
        final String savedOs   = System.getProperty("os.name");
        final String savedArch = System.getProperty("os.arch");
        try {
            System.setProperty("os.name", "Linux");
            System.setProperty("os.arch", "amd64");
            final String path = NativeLibraryLoader.buildResourcePath("ble-linux");
            assertAll(
                () -> assertTrue(path.startsWith("/natives/linux/amd64/")),
                () -> assertTrue(path.contains("ble-linux"))
            );
        } finally {
            System.setProperty("os.name", savedOs);
            System.setProperty("os.arch", savedArch);
        }
    }

    @Test
    void buildResourcePath_windows_amd64_correctPath() {
        final String savedOs   = System.getProperty("os.name");
        final String savedArch = System.getProperty("os.arch");
        try {
            System.setProperty("os.name", "Windows 11");
            System.setProperty("os.arch", "amd64");
            final String path = NativeLibraryLoader.buildResourcePath("ble-windows");
            assertAll(
                () -> assertTrue(path.startsWith("/natives/windows/amd64/")),
                () -> assertTrue(path.contains("ble-windows"))
            );
        } finally {
            System.setProperty("os.name", savedOs);
            System.setProperty("os.arch", savedArch);
        }
    }

    @Test
    void load_libraryNotFound_throwsBleExceptionWithPlatformInfo() {
        final BleException ex = assertThrows(
                BleException.class,
                () -> NativeLibraryLoader.load("__nonexistent_library_xyz__")
        );
        assertAll(
            () -> assertTrue(ex.getMessage().contains("not found in JAR")),
            () -> assertTrue(ex.getMessage().contains("__nonexistent_library_xyz__"))
        );
    }

    /**
     * Verifies that {@link NativeLibraryLoader#load(String)} covers the
     * {@code extractToTemp} and {@code loadAbsolutePath} paths when a dummy
     * resource is present on the test classpath.
     *
     * <p>The dummy file {@code libble-test-dummy.dylib} placed under
     * {@code src/test/resources/natives/macos/aarch64/} is found, extracted
     * to a temp file, and then {@code System.load()} fails with
     * {@link UnsatisfiedLinkError} because the content is not a valid library.
     * That error is caught and re-thrown as {@link BleException}.
     *
     * <p>This test also covers the {@code LOG.fine()} lambdas on lines 61 and 74
     * of {@link NativeLibraryLoader} by enabling {@code FINE} logging for the
     * class logger.
     */
    @Test
    void load_dummyResourcePresent_throwsBleExceptionAfterExtraction() {
        final String savedOs   = System.getProperty("os.name");
        final String savedArch = System.getProperty("os.arch");
        final Logger logger = Logger.getLogger(NativeLibraryLoader.class.getName());
        final Level savedLevel = logger.getLevel();
        try {
            System.setProperty("os.name", "Mac OS X");
            System.setProperty("os.arch", "aarch64");
            logger.setLevel(Level.FINE);

            final BleException ex = assertThrows(
                    BleException.class,
                    () -> NativeLibraryLoader.load("ble-test-dummy")
            );
            assertTrue(ex.getMessage().contains("ble-test-dummy"));
        } finally {
            System.setProperty("os.name", savedOs);
            System.setProperty("os.arch", savedArch);
            logger.setLevel(savedLevel);
        }
    }

    /**
     * Verifies that {@link NativeLibraryLoader#load(String)} covers the
     * {@code libExtension()} windows branch ({@code "dll"}) when a dummy
     * resource is available under {@code natives/windows/amd64/}.
     */
    @Test
    void load_windowsOs_libExtensionIsDll() {
        final String savedOs   = System.getProperty("os.name");
        final String savedArch = System.getProperty("os.arch");
        try {
            System.setProperty("os.name", "Windows 11");
            System.setProperty("os.arch", "amd64");

            /*
             * The resource file is named with a .dylib extension because
             * System.mapLibraryName() always uses the JVM host OS (macOS
             * here) for the file name, regardless of the os.name property.
             * libExtension() reads os.name and returns "dll", which is used
             * only as the temp-file suffix — it does not affect resource lookup.
             */
            final BleException ex = assertThrows(
                    BleException.class,
                    () -> NativeLibraryLoader.load("ble-test-dummy")
            );
            assertTrue(ex.getMessage().contains("ble-test-dummy"));
        } finally {
            System.setProperty("os.name", savedOs);
            System.setProperty("os.arch", savedArch);
        }
    }

    /**
     * Verifies that {@link NativeLibraryLoader#load(String)} covers the
     * {@code libExtension()} linux branch ({@code "so"}) when a dummy
     * resource is available under {@code natives/linux/amd64/}.
     */
    @Test
    void load_linuxOs_libExtensionIsSo() {
        final String savedOs   = System.getProperty("os.name");
        final String savedArch = System.getProperty("os.arch");
        try {
            System.setProperty("os.name", "Linux");
            System.setProperty("os.arch", "amd64");

            final BleException ex = assertThrows(
                    BleException.class,
                    () -> NativeLibraryLoader.load("ble-test-dummy")
            );
            assertTrue(ex.getMessage().contains("ble-test-dummy"));
        } finally {
            System.setProperty("os.name", savedOs);
            System.setProperty("os.arch", savedArch);
        }
    }

    /**
     * Verifies the success path of {@link NativeLibraryLoader#load(String)}:
     * after a resource is extracted to a temp file {@code loadAbsolutePath} is
     * called and, on success, {@code LOG.fine} records the path.
     *
     * <p>{@code loadAbsolutePath} is stubbed via {@code mockStatic} so that
     * {@code System.load()} is never invoked, making the test runnable without
     * a real shared library on the classpath.
     */
    @Test
    void load_dummyResource_successPath_logsAndReturns() {
        final String savedOs   = System.getProperty("os.name");
        final String savedArch = System.getProperty("os.arch");
        final Logger logger = Logger.getLogger(NativeLibraryLoader.class.getName());
        final Level savedLevel = logger.getLevel();
        try (MockedStatic<NativeLibraryLoader> mock =
                mockStatic(NativeLibraryLoader.class, CALLS_REAL_METHODS)) {
            mock.when(() -> NativeLibraryLoader.loadAbsolutePath(anyString()))
                    .thenAnswer(inv -> null);

            System.setProperty("os.name", "Mac OS X");
            System.setProperty("os.arch", "aarch64");
            logger.setLevel(Level.FINE);

            assertDoesNotThrow(() -> NativeLibraryLoader.load("ble-test-dummy"));
        } finally {
            System.setProperty("os.name", savedOs);
            System.setProperty("os.arch", savedArch);
            logger.setLevel(savedLevel);
        }
    }

    /**
     * Verifies the {@link IOException} catch branch in
     * {@link NativeLibraryLoader#load(String)}: when {@code Files.createTempFile}
     * throws an {@link IOException} the exception is wrapped in a
     * {@link BleException} with a descriptive message.
     */
    @Test
    void load_extractToTempFails_throwsBleExceptionWithIoMessage() {
        final String savedOs   = System.getProperty("os.name");
        final String savedArch = System.getProperty("os.arch");
        try (MockedStatic<Files> filesMock = mockStatic(Files.class, CALLS_REAL_METHODS)) {
            filesMock.when(() -> Files.createTempFile(anyString(), anyString()))
                    .thenThrow(new IOException("disk full"));

            System.setProperty("os.name", "Mac OS X");
            System.setProperty("os.arch", "aarch64");

            final BleException ex = assertThrows(
                    BleException.class,
                    () -> NativeLibraryLoader.load("ble-test-dummy")
            );
            assertTrue(ex.getMessage().contains("disk full"));
        } finally {
            System.setProperty("os.name", savedOs);
            System.setProperty("os.arch", savedArch);
        }
    }

    /**
     * Directly exercises {@link NativeLibraryLoader#loadAbsolutePath(String)}
     * with a path that does not point to a valid shared library so that
     * JaCoCo records the method body as covered.  The call is expected to
     * throw {@link UnsatisfiedLinkError}.
     */
    @Test
    void loadAbsolutePath_invalidPath_throwsUnsatisfiedLinkError() {
        assertThrows(
                UnsatisfiedLinkError.class,
                () -> NativeLibraryLoader.loadAbsolutePath("/nonexistent/path/libfake.dylib")
        );
    }
}
