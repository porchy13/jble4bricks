package ch.varani.bricks.ble.impl.macos;

import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.NonNull;

import ch.varani.bricks.ble.api.BleConnection;
import ch.varani.bricks.ble.api.BleDevice;

/**
 * macOS implementation of {@link BleDevice}.
 *
 * <p>Instances are created by {@link MacOsBleScanner#onDeviceFound} when the
 * CoreBluetooth delegate reports a new or updated advertisement.  All fields
 * are set at construction time and never mutated, making instances
 * thread-safe.
 *
 * <p>The {@link #connect()} method delegates to the owning
 * {@link MacOsBleScanner} so that the scanner's native context pointer can be
 * used to initiate the CoreBluetooth connection.
 *
 * @since 1.0
 */
final class MacOsBleDevice implements BleDevice {

    /** The CoreBluetooth peripheral UUID string (stable identifier). */
    private final String peripheralId;

    /** Advertised local name (may be an empty string). */
    private final String deviceName;

    /** Received signal strength in dBm at discovery time. */
    private final int rssiValue;

    /** Back-reference to the scanner that discovered this device. */
    private final MacOsBleScanner owner;

    /**
     * Constructs a new {@code MacOsBleDevice}.
     *
     * @param peripheralId the CoreBluetooth peripheral UUID string; must not be {@code null}
     * @param name         advertised device name; must not be {@code null}
     * @param rssi         signal strength in dBm
     * @param owner        the scanner instance that discovered this device; must not be {@code null}
     */
    MacOsBleDevice(
            final @NonNull String peripheralId,
            final @NonNull String name,
            final int rssi,
            final @NonNull MacOsBleScanner owner) {
        this.peripheralId = peripheralId;
        this.deviceName   = name;
        this.rssiValue    = rssi;
        this.owner        = owner;
    }

    /**
     * {@inheritDoc}
     *
     * <p>On macOS this is the {@code NSUUID} string assigned by CoreBluetooth
     * to the peripheral; it is stable within a single OS installation but may
     * differ across devices or OS re-installations.
     */
    @Override
    public @NonNull String id() {
        return peripheralId;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public @NonNull String name() {
        return deviceName;
    }

    /**
     * {@inheritDoc}
     */
    @Override
    public int rssi() {
        return rssiValue;
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link MacOsBleScanner#connectPeripheral(String)},
     * which calls {@code CBCentralManager connectPeripheral:options:} and
     * waits for full service and characteristic discovery before completing
     * the returned future.
     */
    @Override
    public @NonNull CompletableFuture<BleConnection> connect() {
        return owner.connectPeripheral(peripheralId).thenApply(conn -> (BleConnection) conn);
    }
}
