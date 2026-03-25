package ch.varani.bricks.ble.util;

import java.io.IOException;
import java.io.InputStream;
import java.io.OutputStream;
import java.nio.file.Files;
import java.nio.file.Path;
import java.nio.file.StandardOpenOption;
import java.util.Locale;
import java.util.logging.Logger;

import ch.varani.bricks.ble.api.BleException;

/**
 * Extracts the platform-appropriate native shared library from the JAR and
 * loads it into the current JVM process via {@link System#load(String)}.
 *
 * <p>The library is bundled under {@code natives/<os>/<arch>/} inside the
 * JAR.  On first call the bytes are copied to a temporary file so that
 * {@code System.load()} can map them into the process.  The temporary file
 * is deleted when the JVM exits.
 *
 * <p>Thread safety: {@link #load(String)} is safe to call concurrently from
 * multiple threads; the actual loading is performed at most once per library
 * name per JVM lifetime thanks to a {@code synchronized} block.
 *
 * @since 1.0
 */
public final class NativeLibraryLoader {

    private static final Logger LOG = Logger.getLogger(NativeLibraryLoader.class.getName());

    /** Size of the buffer used when copying library bytes to the temp file. */
    private static final int COPY_BUFFER_SIZE = 8192;

    /**
     * Private constructor — utility class, not instantiable.
     */
    private NativeLibraryLoader() {
        throw new AssertionError("NativeLibraryLoader must not be instantiated");
    }

    /**
     * Loads the native shared library with the given base name.
     *
     * <p>The library resource path inside the JAR is constructed as:
     * {@code /natives/<os>/<arch>/lib<name>.<extension>}
     * where {@code <os>} and {@code <arch>} are derived from the JVM system
     * properties {@code os.name} and {@code os.arch}.
     *
     * <p>If loading fails because the native library is not bundled for the
     * current platform, a descriptive {@link BleException} is thrown.  Falling
     * back to any third-party BLE library is explicitly forbidden by the
     * project mandate.
     *
     * @param libraryName the base name of the library (e.g. {@code "ble-macos"})
     * @throws BleException if the library cannot be found or loaded
     */
    public static synchronized void load(final String libraryName) throws BleException {
        final String resourcePath = buildResourcePath(libraryName);
        LOG.fine(() -> "Loading native library from JAR resource: " + resourcePath);

        final InputStream in = NativeLibraryLoader.class.getResourceAsStream(resourcePath);
        if (in == null) {
            throw new BleException(
                    "Native BLE library not found in JAR: " + resourcePath
                    + ". This build does not support the current platform ("
                    + osName() + " / " + archName() + ").");
        }

        try {
            final Path tmp = extractToTemp(libraryName, in);
            loadAbsolutePath(tmp.toAbsolutePath().toString());
            LOG.fine(() -> "Loaded native library: " + tmp);
        } catch (IOException e) {
            throw new BleException(
                    "Failed to extract native library '" + libraryName
                    + "' to a temporary file: " + e.getMessage(),
                    e);
        } catch (UnsatisfiedLinkError e) {
            throw new BleException(
                    "Failed to link native library '" + libraryName
                    + "': " + e.getMessage(),
                    e);
        }
    }

    /**
     * Builds the JAR-internal resource path for the given library name.
     *
     * <p>Example on macOS Apple Silicon:
     * {@code /natives/macos/aarch64/libble-macos.dylib}
     *
     * @param libraryName the base library name
     * @return the full resource path, starting with {@code /}
     */
    static String buildResourcePath(final String libraryName) {
        return "/natives/" + osName() + "/" + archName()
                + "/" + System.mapLibraryName(libraryName);
    }

    /**
     * Derives a normalised OS token from {@code os.name}.
     *
     * @return {@code "macos"}, {@code "windows"}, or {@code "linux"}
     */
    static String osName() {
        final String raw = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (raw.contains("mac")) {
            return "macos";
        }
        if (raw.contains("win")) {
            return "windows";
        }
        return "linux";
    }

    /**
     * Derives a normalised architecture token from {@code os.arch}.
     *
     * @return {@code "aarch64"} or {@code "amd64"}
     */
    static String archName() {
        final String raw = System.getProperty("os.arch", "").toLowerCase(Locale.ROOT);
        if (raw.equals("aarch64") || raw.equals("arm64")) {
            return "aarch64";
        }
        return "amd64";
    }

    /**
     * Calls {@link System#load(String)} with the given absolute path.
     *
     * <p>This wrapper method exists so that the {@code @SuppressWarnings("restricted")}
     * annotation can be applied at the method level.  {@code System.load()} is a
     * restricted method in Java 25 (it maps arbitrary native code into the JVM
     * process), but it is the only mechanism for loading a shared library that was
     * extracted from a JAR to a temp file at runtime without requiring callers to
     * configure {@code java.library.path}.
     *
     * <p>Package-private visibility allows unit tests to intercept this call via
     * {@code Mockito.mockStatic} in order to cover the success and failure paths
     * of {@link #load(String)} without requiring a real shared library on the
     * test classpath.
     *
     * @param absolutePath absolute path to the shared library file
     * @throws UnsatisfiedLinkError if the library cannot be loaded
     */
    @SuppressWarnings("restricted")
    static void loadAbsolutePath(final String absolutePath) {
        LOG.fine(() -> "Calling System.load: " + absolutePath);
        System.load(absolutePath);
    }

    /**
     * Copies the stream contents to a temporary file and registers a shutdown
     * hook to delete it when the JVM exits.
     *
     * @param libraryName base name used as the temp-file prefix
     * @param in          stream of library bytes
     * @return path to the extracted temporary file
     * @throws IOException if I/O fails during the copy
     */
    private static Path extractToTemp(final String libraryName, final InputStream in)
            throws IOException {

        final String suffix = "." + libExtension();
        final Path tmp = Files.createTempFile("ble-" + libraryName + "-", suffix);
        tmp.toFile().deleteOnExit();

        try (InputStream src = in;
             OutputStream dst = Files.newOutputStream(tmp, StandardOpenOption.WRITE)) {

            final byte[] buf = new byte[COPY_BUFFER_SIZE];
            int read;
            while ((read = src.read(buf)) != -1) {
                dst.write(buf, 0, read);
            }
        }

        return tmp;
    }

    /**
     * Returns the file extension for shared libraries on the current platform.
     *
     * @return {@code "dylib"}, {@code "dll"}, or {@code "so"}
     */
    private static String libExtension() {
        final String os = osName();
        if ("macos".equals(os)) {
            return "dylib";
        }
        if ("windows".equals(os)) {
            return "dll";
        }
        return "so";
    }
}
