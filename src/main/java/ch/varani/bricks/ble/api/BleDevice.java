package ch.varani.bricks.ble.api;

import java.util.concurrent.CompletableFuture;

import org.jspecify.annotations.NonNull;

/**
 * Represents a discovered BLE peripheral that has not yet been connected.
 *
 * <p>Instances are created by the scanning layer and passed to the caller's
 * {@link ScanCallback}. All fields are immutable.
 *
 * <p>Thread safety: instances are immutable and therefore safe for use from
 * multiple threads without synchronisation.
 */
public interface BleDevice {

    /**
     * Returns the platform-specific peripheral identifier.
     *
     * <p>On macOS this is the CoreBluetooth {@code NSUUID} string.
     * On Windows it is the BLE address as a hex string.
     * On Linux it is the BlueZ D-Bus object path.
     *
     * @return a non-null, non-empty identifier string
     */
    @NonNull
    String id();

    /**
     * Returns the advertised device name, or an empty string if the device
     * did not include a Local Name AD structure in its advertisement.
     *
     * @return advertised name, never {@code null}
     */
    @NonNull
    String name();

    /**
     * Returns the received signal strength indicator (RSSI) at the time the
     * advertisement was received, in dBm.
     *
     * @return RSSI value in dBm
     */
    int rssi();

    /**
     * Returns the raw manufacturer-specific advertisement data payload for
     * this peripheral, as received in the BLE advertisement packet.
     *
     * <p>The returned byte array contains the full content of the
     * Manufacturer Specific Data AD structure, starting from the 2-byte
     * company identifier (little-endian) followed by the brand-specific
     * data bytes.  For LEGO hubs the full payload is therefore 10 bytes:
     * <pre>
     * [0] Manufacturer ID LSB  (0x97 for LEGO System A/S)
     * [1] Manufacturer ID MSB  (0x03 for LEGO System A/S)
     * [2] Button State
     * [3] System Type + Device Number  (SSS DDDDD encoding)
     * [4] Device Capabilities
     * [5] Last Network ID
     * [6] Status
     * [7] Option
     * </pre>
     * The constants {@link ch.varani.bricks.ble.device.lego.LegoProtocolConstants#MANUFACTURER_DATA_IDX_SYSTEM_TYPE}
     * and {@link ch.varani.bricks.ble.device.lego.LegoProtocolConstants#MANUFACTURER_DATA_MIN_LENGTH}
     * are defined relative to this layout.
     *
     * <p>Returns an empty array if the advertisement did not include a
     * Manufacturer Specific Data AD structure, or if the platform did not
     * expose that data.
     *
     * @return a copy of the manufacturer-specific payload bytes starting with
     *         the 2-byte company identifier; never {@code null}, may be empty
     */
    @NonNull
    byte[] manufacturerData();

    /**
     * Initiates a connection to this peripheral.
     *
     * <p>The returned future completes with a {@link BleConnection} when the
     * underlying BLE connection is established and service discovery has
     * finished. The future completes exceptionally with a
     * {@link BleException} if the connection or service discovery fails.
     *
     * <p>This method may be called from any thread.
     *
     * @return a future that completes with the open connection
     */
    @NonNull
    CompletableFuture<BleConnection> connect();
}
