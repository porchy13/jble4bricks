package ch.varani.bricks.ble.impl.linux;

import java.util.Arrays;
import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.NonNull;

import ch.varani.bricks.ble.api.BleConnection;
import ch.varani.bricks.ble.api.BleDevice;

/**
 * Linux implementation of {@link BleDevice}.
 *
 * <p>Instances are created by {@link LinuxBleScanner#onDeviceFound} when the
 * BlueZ D-Bus layer reports a new device via the
 * {@code org.freedesktop.DBus.ObjectManager.InterfacesAdded} signal.
 * All fields are set at construction time and never mutated, making instances
 * thread-safe.
 *
 * <p>The {@link #connect()} method delegates to the owning
 * {@link LinuxBleScanner} so that the scanner's native context pointer can be
 * used to initiate the BlueZ GATT connection.
 *
 * @since 1.0
 */
final class LinuxBleDevice implements BleDevice {

    /** The BlueZ D-Bus object path (stable identifier on Linux). */
    private final String devicePath;

    /** Advertised local name (may be an empty string). */
    private final String deviceName;

    /** Received signal strength in dBm at discovery time. */
    private final int rssiValue;

    /** Manufacturer-specific advertisement payload bytes (copy). */
    private final byte[] mfrData;

    /** Back-reference to the scanner that discovered this device. */
    private final LinuxBleScanner owner;

    /**
     * Constructs a new {@code LinuxBleDevice}.
     *
     * @param devicePath       the BlueZ D-Bus object path; must not be {@code null}
     * @param name             advertised device name; must not be {@code null}
     * @param rssi             signal strength in dBm
     * @param manufacturerData raw manufacturer-specific payload bytes (copied);
     *                         must not be {@code null}
     * @param owner            the scanner instance that discovered this device;
     *                         must not be {@code null}
     */
    LinuxBleDevice(
            final @NonNull String devicePath,
            final @NonNull String name,
            final int rssi,
            final @NonNull byte[] manufacturerData,
            final @NonNull LinuxBleScanner owner) {
        this.devicePath = devicePath;
        this.deviceName = name;
        this.rssiValue  = rssi;
        this.mfrData    = Arrays.copyOf(manufacturerData, manufacturerData.length);
        this.owner      = owner;
    }

    /**
     * {@inheritDoc}
     *
     * <p>On Linux this is the BlueZ D-Bus object path of the device, for
     * example {@code /org/bluez/hci0/dev_AA_BB_CC_DD_EE_FF}.
     */
    @Override
    public @NonNull String id() {
        return devicePath;
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
     * extracted from the BlueZ {@code org.bluez.Device1.ManufacturerData}
     * property delivered via the {@code InterfacesAdded} D-Bus signal.
     */
    @Override
    public @NonNull byte[] manufacturerData() {
        return Arrays.copyOf(mfrData, mfrData.length);
    }

    /**
     * {@inheritDoc}
     *
     * <p>Delegates to {@link LinuxBleScanner#connectDevice(String)}, which
     * calls {@code org.bluez.Device1.Connect} over D-Bus and waits for full
     * GATT service and characteristic discovery before completing the returned
     * future.
     */
    @Override
    public @NonNull CompletableFuture<BleConnection> connect() {
        return owner.connectDevice(devicePath).thenApply(conn -> (BleConnection) conn);
    }
}
