package ch.varani.bricks.ble.api;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;
import org.junit.jupiter.api.condition.DisabledOnOs;
import org.junit.jupiter.api.condition.EnabledOnOs;
import org.junit.jupiter.api.condition.OS;
import org.junit.jupiter.params.ParameterizedTest;
import org.junit.jupiter.params.provider.ValueSource;

/**
 * Unit tests for {@link BleScannerFactory}.
 *
 * <p>The factory detects the OS at runtime.  On the current build platform the
 * native shared library is compiled and embedded in the JAR during the
 * {@code generate-resources} phase (via the OS-specific Maven profile), so
 * {@link BleScannerFactory#create()} succeeds on the host OS.  On every other
 * platform the native library is absent and a {@link BleException} is thrown:
 * <ul>
 *   <li>macOS — native library present when built on macOS; absent otherwise</li>
 *   <li>Windows — native library present when built on Windows; absent otherwise</li>
 *   <li>Linux — native library present when built on Linux; absent otherwise</li>
 *   <li>other OS — unsupported platform message</li>
 * </ul>
 */
class BleScannerFactoryTest {

    /**
     * On an unsupported OS the factory must throw a {@link BleException}
     * whose message contains {@code "No BLE platform"}.
     */
    @Test
    void create_unsupportedOs_throwsBleExceptionWithNoPlatformMessage() {
        final String saved = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "SomeUnknownOS 42");
            final BleException ex = assertThrows(BleException.class, BleScannerFactory::create);
            assertAll(
                () -> assertNotNull(ex.getMessage()),
                () -> assertTrue(ex.getMessage().contains("No BLE platform"))
            );
        } finally {
            System.setProperty("os.name", saved);
        }
    }

    /**
     * On macOS the native library is compiled and embedded by the {@code native-macos}
     * Maven profile, so the factory must succeed without throwing.
     */
    @Test
    @EnabledOnOs(OS.MAC)
    void create_macOs_succeedsWithNativeLibraryPresent() {
        assertDoesNotThrow(BleScannerFactory::create);
    }

    /**
     * On Windows the native library is compiled and embedded by the {@code native-windows}
     * Maven profile, so the factory must succeed without throwing.
     */
    @Test
    @EnabledOnOs(OS.WINDOWS)
    void create_windows_succeedsWithNativeLibraryPresent() {
        assertDoesNotThrow(BleScannerFactory::create);
    }

    /**
     * On Linux the native library is compiled and embedded by the {@code native-linux}
     * Maven profile, so the factory must succeed without throwing.
     */
    @Test
    @EnabledOnOs(OS.LINUX)
    void create_linux_succeedsWithNativeLibraryPresent() {
        assertDoesNotThrow(BleScannerFactory::create);
    }

    /**
     * When the OS name is forced to a foreign platform that is not the current
     * host, the native library for that platform is absent from the JAR and the
     * factory must throw {@link BleException}.
     *
     * <p>This test is skipped on macOS because "Mac OS X" is the host OS and the
     * macOS library is present; the other two foreign values ("Linux", "gnunux")
     * are tested instead.
     *
     * <p>"gnunux" contains "nux" but not "linux" — covers the right-hand
     * Linux-detection branch in {@link BleScannerFactory}.
     */
    @ParameterizedTest
    @ValueSource(strings = {"Linux", "gnunux"})
    @DisabledOnOs(OS.LINUX)
    void create_foreignLinux_throwsBleExceptionBecauseNativeLibraryAbsent(final String osName) {
        assertForeignOsThrows(osName);
    }

    /**
     * Forces the OS name to Windows on a non-Windows host; the Windows DLL is
     * absent from the JAR so the factory must throw {@link BleException}.
     */
    @Test
    @DisabledOnOs(OS.WINDOWS)
    void create_foreignWindows_throwsBleExceptionBecauseNativeLibraryAbsent() {
        assertForeignOsThrows("Windows 11");
    }

    /**
     * Forces the OS name to macOS on a non-macOS host; the macOS dylib is
     * absent from the JAR so the factory must throw {@link BleException}.
     */
    @Test
    @DisabledOnOs(OS.MAC)
    void create_foreignMacOs_throwsBleExceptionBecauseNativeLibraryAbsent() {
        assertForeignOsThrows("Mac OS X");
    }

    /**
     * The thrown exception must be an instance of {@link BleException}.
     */
    @Test
    void create_throwsBleException_whichIsThrowable() {
        final String saved = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "SomeUnknownOS 99");
            final Throwable ex = assertThrows(BleException.class, BleScannerFactory::create);
            assertInstanceOf(BleException.class, ex);
        } finally {
            System.setProperty("os.name", saved);
        }
    }

    /**
     * {@link BleScannerFactory} is a utility class; its constructor must be
     * private and must throw {@link AssertionError} when invoked reflectively.
     */
    @Test
    void constructor_isPrivate_throwsAssertionError() throws Exception {
        final Constructor<BleScannerFactory> ctor =
            BleScannerFactory.class.getDeclaredConstructor();
        ctor.setAccessible(true);
        final InvocationTargetException ex =
            assertThrows(InvocationTargetException.class, ctor::newInstance);
        assertThrows(AssertionError.class, () -> {
            throw ex.getCause();
        });
    }

    /**
     * Helper: forces {@code os.name} to {@code osName}, calls the factory, asserts that a
     * {@link BleException} is thrown whose message contains one of the expected error tokens,
     * then restores the original {@code os.name}.
     *
     * @param osName the OS name to inject
     */
    private static void assertForeignOsThrows(final String osName) {
        final String saved = System.getProperty("os.name");
        try {
            System.setProperty("os.name", osName);
            final BleException ex = assertThrows(BleException.class, BleScannerFactory::create);
            assertAll(
                () -> assertNotNull(ex.getMessage()),
                () -> assertTrue(
                    ex.getMessage().contains("not found in JAR")
                        || ex.getMessage().contains("No BLE platform")
                        || ex.getMessage().contains("JNI binding error")
                        || ex.getMessage().contains("method not found")
                        || ex.getMessage().contains("Failed to link"),
                    "Unexpected message: " + ex.getMessage())
            );
        } finally {
            System.setProperty("os.name", saved);
        }
    }
}

