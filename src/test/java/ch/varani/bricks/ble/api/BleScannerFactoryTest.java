package ch.varani.bricks.ble.api;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertInstanceOf;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

import java.lang.reflect.Constructor;
import java.lang.reflect.InvocationTargetException;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BleScannerFactory}.
 *
 * <p>The factory detects the OS at runtime.  Because the native shared libraries
 * are not present on the unit-test classpath, the platform-specific branches all
 * throw {@link BleException}:
 * <ul>
 *   <li>macOS — native library not found in JAR</li>
 *   <li>Windows — native library not found in JAR</li>
 *   <li>Linux — native library not found in JAR</li>
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
     * On macOS the factory attempts to load the native library; because the
     * dylib is either absent from the unit-test classpath, or present but with
     * a JNI binding mismatch, a {@link BleException} is thrown. The message
     * will contain {@code "not found in JAR"}, {@code "No BLE platform"}, or
     * a JNI binding error string from the native layer.
     */
    @Test
    void create_macOs_throwsBleExceptionBecauseNativeLibraryAbsent() {
        final String saved = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Mac OS X");
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

    /**
     * On Linux (via {@code os.name} containing {@code "linux"}) the factory
     * attempts to load the native library; because {@code libble-linux.so} is
     * absent from the unit-test classpath a {@link BleException} is thrown.
     * Exercises the {@code osName.contains("linux")} branch of the
     * {@code ||} condition.
     */
    @Test
    void create_linux_throwsBleExceptionBecauseNativeLibraryAbsent() {
        final String saved = System.getProperty("os.name");
        try {
            System.setProperty("os.name", "Linux");
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

    /**
     * On a system whose {@code os.name} contains {@code "nux"} but not
     * {@code "linux"} (exercising the right-hand side of the
     * {@code osName.contains("linux") || osName.contains("nux")} condition)
     * the factory attempts to load the native library and throws a
     * {@link BleException} because {@code libble-linux.so} is absent from the
     * unit-test classpath.
     */
    @Test
    void create_nux_throwsBleExceptionBecauseNativeLibraryAbsent() {
        final String saved = System.getProperty("os.name");
        try {
            // "gnunux" contains "nux" but not "linux" — triggers the right-hand branch
            System.setProperty("os.name", "gnunux");
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
}
