package ch.varani.lego.ble.api;

import org.jspecify.annotations.NonNull;

/**
 * Receives BLE scan results as they are discovered.
 *
 * <p>Implementations must be thread-safe because the callback may be invoked
 * from a background thread managed by the platform BLE stack.
 */
@FunctionalInterface
public interface ScanCallback {

    /**
     * Called each time a BLE peripheral is discovered or its advertisement
     * data is updated.
     *
     * <p>The same peripheral may be reported multiple times as its
     * advertisement is re-broadcast. Callers should use {@link BleDevice#id()}
     * as a stable identifier.
     *
     * @param device the discovered peripheral; never {@code null}
     */
    void onDeviceFound(@NonNull BleDevice device);
}
