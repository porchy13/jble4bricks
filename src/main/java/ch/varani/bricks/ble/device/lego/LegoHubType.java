package ch.varani.bricks.ble.device.lego;

/**
 * Enumeration of known LEGO Powered Up hub types, identified by the
 * System Type and Device Number byte (byte index
 * {@link LegoProtocolConstants#MANUFACTURER_DATA_IDX_SYSTEM_TYPE} = 3)
 * of the manufacturer-specific advertisement payload.
 *
 * <p>The encoding is {@code SSS DDDDD} where the three high bits select
 * the system family and the five low bits select the device within that
 * family.  All values are defined in the LEGO Wireless Protocol 3.0
 * specification, section "Discovery".
 *
 * <p>Reference:
 * <a href="https://lego.github.io/lego-ble-wireless-protocol-docs/">
 * LEGO BLE Wireless Protocol 3.0 specification</a>
 *
 * <p>Usage example — wait for the first City Hub within 15 seconds:
 * <pre>{@code
 * dsl.scan()
 *    .forLegoHubType(LegoHubType.CITY_HUB)
 *    .timeoutSeconds(15)
 *    .first()
 *    .thenConnect()
 *    .asLego()
 *    .motor(0x00).startSpeed(60)
 *    .done();
 * }</pre>
 */
public enum LegoHubType {

    /**
     * WeDo 2.0 Hub — System Type 0 ({@code SSS=000}), Device Number 0
     * ({@code DDDDD=00000}).
     *
     * <p>Corresponds to {@link LegoProtocolConstants#DEVICE_WEDO2_HUB}.
     */
    WEDO2_HUB(LegoProtocolConstants.DEVICE_WEDO2_HUB),

    /**
     * Duplo Train Hub — System Type 1 ({@code SSS=001}), Device Number 0
     * ({@code DDDDD=00000}).
     *
     * <p>Corresponds to {@link LegoProtocolConstants#DEVICE_DUPLO_TRAIN}.
     */
    DUPLO_TRAIN(LegoProtocolConstants.DEVICE_DUPLO_TRAIN),

    /**
     * Boost Move Hub — System Type 2 ({@code SSS=010}), Device Number 0
     * ({@code DDDDD=00000}).
     *
     * <p>Corresponds to {@link LegoProtocolConstants#DEVICE_BOOST_HUB}.
     */
    BOOST_MOVE_HUB(LegoProtocolConstants.DEVICE_BOOST_HUB),

    /**
     * 2-Port Hub (City Hub / Hub 2) — System Type 2 ({@code SSS=010}),
     * Device Number 1 ({@code DDDDD=00001}).
     *
     * <p>Corresponds to {@link LegoProtocolConstants#DEVICE_2PORT_HUB}.
     */
    CITY_HUB(LegoProtocolConstants.DEVICE_2PORT_HUB),

    /**
     * 2-Port Handset (remote controller) — System Type 2 ({@code SSS=010}),
     * Device Number 2 ({@code DDDDD=00010}).
     *
     * <p>Corresponds to {@link LegoProtocolConstants#DEVICE_2PORT_HANDSET}.
     */
    HANDSET_2PORT(LegoProtocolConstants.DEVICE_2PORT_HANDSET),

    /**
     * Technic Hub (4-Port Hub, item no. 88012) — System Type 2
     * ({@code SSS=010}), Device Number 16 ({@code DDDDD=10000}).
     *
     * <p>Corresponds to {@link LegoProtocolConstants#DEVICE_TECHNIC_HUB}.
     */
    TECHNIC_HUB(LegoProtocolConstants.DEVICE_TECHNIC_HUB),

    /**
     * Mario Hub (LEGO Super Mario interactive starter course hub) — System
     * Type 3 ({@code SSS=011}), Device Number 0 ({@code DDDDD=00000}).
     *
     * <p>Corresponds to {@link LegoProtocolConstants#DEVICE_MARIO_HUB}.
     */
    MARIO_HUB(LegoProtocolConstants.DEVICE_MARIO_HUB);

    /**
     * The raw {@code System Type and Device Number} byte as broadcast in the
     * LEGO manufacturer-specific advertisement payload (byte index
     * {@link LegoProtocolConstants#MANUFACTURER_DATA_IDX_SYSTEM_TYPE} = 3
     * of the payload as received from CoreBluetooth / BlueZ, i.e. after
     * stripping the AD Length and AD Type 0xFF prefix bytes).
     */
    private final int systemTypeDeviceByte;

    /**
     * Constructs a hub-type constant.
     *
     * @param systemTypeDeviceByte the raw {@code SSSDDDD} byte value
     */
    LegoHubType(final int systemTypeDeviceByte) {
        this.systemTypeDeviceByte = systemTypeDeviceByte;
    }

    /**
     * Returns the raw System Type and Device Number byte used in the LEGO
     * manufacturer-specific advertisement payload.
     *
     * @return the byte value ({@code 0x00}–{@code 0xFF})
     */
    public int systemTypeDeviceByte() {
        return systemTypeDeviceByte;
    }
}
