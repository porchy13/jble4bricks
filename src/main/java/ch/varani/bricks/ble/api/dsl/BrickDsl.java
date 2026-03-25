package ch.varani.bricks.ble.api.dsl;

import org.jspecify.annotations.NonNull;

import ch.varani.bricks.ble.api.BleException;
import ch.varani.bricks.ble.api.BleScanner;
import ch.varani.bricks.ble.api.BleScannerFactory;

/**
 * Entry point for the fluent BLE brick DSL.
 *
 * <p>Obtain a session with {@link #open()} and chain DSL calls from there:
 * <pre>{@code
 * try (BrickDsl dsl = BrickDsl.open()) {
 *     dsl.scan()
 *        .forLegoHubs()
 *        .first()
 *        .thenConnect()
 *        .asLego()
 *        .motor(0x00).startSpeed(80)
 *        .hubAction().switchOff()
 *        .done();
 * }
 * }</pre>
 *
 * <p>{@code BrickDsl} implements {@link AutoCloseable} so it may be used in a
 * try-with-resources block. Closing the DSL stops any active scan and releases
 * all underlying platform resources.
 *
 * <p>Thread safety: a {@code BrickDsl} instance is not thread-safe. Each
 * thread that requires independent control of BLE hardware should create its
 * own instance.
 */
public final class BrickDsl implements AutoCloseable {

    /** The scanner that backs this DSL session. */
    private final BleScanner scanner;

    /**
     * Creates a {@code BrickDsl} wrapping the given scanner.
     *
     * <p>This constructor is package-private to allow test injection of a
     * mock scanner. Production code must use {@link #open()}.
     *
     * @param scanner the BLE scanner to delegate to; must not be {@code null}
     */
    BrickDsl(@NonNull BleScanner scanner) {
        this.scanner = scanner;
    }

    /**
     * Creates a new {@code BrickDsl} session backed by the platform BLE stack.
     *
     * <p>Internally calls {@link BleScannerFactory#create()} to obtain the
     * platform-appropriate scanner. The caller is responsible for closing the
     * returned instance (or using try-with-resources).
     *
     * @return a new {@code BrickDsl} session; never {@code null}
     * @throws BleException if the platform BLE stack cannot be initialised
     */
    public static @NonNull BrickDsl open() throws BleException {
        return new BrickDsl(BleScannerFactory.create());
    }

    /**
     * Returns the scan DSL builder for this session.
     *
     * <p>The returned {@link ScanDsl} wraps the scanner held by this session.
     * Use it to start a passive or filtered scan and collect discovered devices.
     *
     * @return a new {@link ScanDsl} backed by this session's scanner;
     *         never {@code null}
     */
    public @NonNull ScanDsl scan() {
        return new ScanDsl(scanner);
    }

    /**
     * Returns the underlying {@link BleScanner} for callers that need direct
     * access to the raw API.
     *
     * @return the scanner; never {@code null}
     */
    public @NonNull BleScanner scanner() {
        return scanner;
    }

    /**
     * Stops any active scan and releases all platform resources held by the
     * underlying scanner.
     *
     * @throws BleException if releasing resources fails
     */
    @Override
    public void close() throws BleException {
        scanner.close();
    }
}
