package ch.varani.lego.ble.api;

import java.util.Locale;

import org.jspecify.annotations.NonNull;

import ch.varani.lego.ble.impl.macos.MacOsBleScanner;

/**
 * Factory that creates a platform-appropriate {@link BleScanner} instance.
 *
 * <p>The factory detects the current OS at runtime via the {@code os.name}
 * system property and returns the matching native implementation:
 * <ul>
 *   <li><b>macOS</b> — {@code MacOsBleScanner} backed by CoreBluetooth (JNI)</li>
 * </ul>
 *
 * <p>Usage:
 * <pre>{@code
 * BleScanner scanner = BleScannerFactory.create();
 * }</pre>
 *
 * <p>Thread safety: {@link #create()} is thread-safe and may be called
 * concurrently from multiple threads.
 */
public final class BleScannerFactory {

    /**
     * Private constructor — this is a utility class with no instances.
     */
    private BleScannerFactory() {
        throw new AssertionError("BleScannerFactory must not be instantiated");
    }

    /**
     * Creates and returns a new {@link BleScanner} backed by the platform BLE
     * stack appropriate for the current operating system.
     *
     * <p>The returned scanner is not yet scanning; call
     * {@link BleScanner#startScan(ScanCallback)} to begin discovery.
     *
     * <p>Supported platforms:
     * <ul>
     *   <li>macOS 13+ (aarch64 / amd64) — CoreBluetooth via JNI</li>
     * </ul>
     *
     * @return a new platform-specific {@link BleScanner}; never {@code null}
     * @throws BleException if the platform BLE stack cannot be initialised
     *                      (e.g. Bluetooth hardware is absent or disabled),
     *                      or if the current OS is not yet supported
     */
    public static @NonNull BleScanner create() throws BleException {
        final String osName = System.getProperty("os.name", "").toLowerCase(Locale.ROOT);
        if (osName.contains("mac")) {
            return new MacOsBleScanner();
        }
        throw new BleException(
                "No BLE platform implementation available for this OS ("
                + System.getProperty("os.name", "unknown")
                + "). Supported platforms: macOS 13+.");
    }
}
