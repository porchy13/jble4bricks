package ch.varani.bricks.ble.impl.windows;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.NonNull;

import ch.varani.bricks.ble.api.BleConnection;
import ch.varani.bricks.ble.api.BleDevice;

/**
 * Windows implementation of {@link BleDevice}.
 *
 * <p>Instances are created by {@link WindowsBleScanner#onDeviceFound} when
 * the WinRT advertisement watcher reports a new or updated advertisement.
 * All fields are set at construction time and never mutated, making instances
 * thread-safe.
 *
 * <p>The {@link #connect()} method delegates to the owning
 * {@link WindowsBleScanner} so that the scanner's native context pointer can
 * be used to initiate the WinRT GATT connection.
 *
 * @since 1.0
 */
final class WindowsBleDevice implements BleDevice {

    /** The BLE device address string (stable identifier on Windows). */
    private final String deviceAddress;

    /** Advertised local name (may be an empty string). */
    private final String deviceName;

    /** Received signal strength in dBm at discovery time. */
    private final int rssiValue;

    /** Manufacturer-specific advertisement payload bytes (copy). */
    private final byte[] mfrData;

    /** Back-reference to the scanner that discovered this device. */
    private final WindowsBleScanner owner;

    /**
     * Constructs a new {@code WindowsBleDevice}.
     *
     * @param deviceAddress    the BLE device address string; must not be {@code null}
     * @param name             advertised device name; must not be {@code null}
     * @param rssi             signal strength in dBm
     * @param manufacturerData raw manufacturer-specific payload bytes (copied);
     *                         must not be {@code null}
     * @param owner            the scanner instance that discovered this device;
     *                         must not be {@code null}
     */
    WindowsBleDevice(
            final @NonNull String deviceAddress,
            final @NonNull String name,
            final int rssi,
            final @NonNull byte[] manufacturerData,
            final @NonNull WindowsBleScanner owner) {
        this.deviceAddress = deviceAddress;
        this.deviceName    = name;
        this.rssiValue     = rssi;
        this.mfrData       = Arrays.copyOf(manufacturerData, manufacturerData.length);
        this.owner         = owner;
    }

    /**
     * {@inheritDoc}
     *
     * <p>On Windows this is the BLE device address encoded as a hex string,
     * as provided by
     * {@code BluetoothLEAdvertisementReceivedEventArgs.BluetoothAddress}.
     */
    @Override
    public @NonNull String id() {
        return deviceAddress;
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
     * <p>Returns a defensive copy of the manufacturer-specific data bytes
     * extracted from the WinRT
     * {@code BluetoothLEManufacturerData.Data} property of the received
     * {@code BluetoothLEAdvertisementReceivedEventArgs}.
     */
    @Override
    public @NonNull byte[] manufacturerData() {
        return Arrays.copyOf(mfrData, mfrData.length);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link WindowsBleScanner#connectPeripheral(String)},
     * which calls {@code BluetoothLEDevice.FromBluetoothAddressAsync()} and
     * waits for full service and characteristic discovery before completing
     * the returned future.
     */
    @Override
    public @NonNull CompletableFuture<BleConnection> connect() {
        return owner.connectPeripheral(deviceAddress).thenApply(conn -> (BleConnection) conn);
    }
}
