package ch.varani.bricks.ble.api;

import org.jspecify.annotations.NonNull;
import org.jspecify.annotations.Nullable;

/**
 * Signals that a BLE operation has failed.
 *
 * <p>This is the single checked exception type used throughout the public API.
 * Callers should inspect the {@link #getMessage()} for a human-readable
 * description and, where applicable, the {@link #getCause()} for the
 * underlying platform exception.
 *
 * <p>Thread safety: immutable after construction; safe for use from multiple
 * threads without synchronisation.
 */
public final class BleException extends Exception {

    private static final long serialVersionUID = 1L;

    /**
     * Constructs a new {@code BleException} with the given message and no cause.
     *
     * @param message a human-readable description of the failure; must not be {@code null}
     */
    public BleException(final @NonNull String message) {
        super(message);
    }

    /**
     * Constructs a new {@code BleException} with the given message and cause.
     *
     * @param message a human-readable description of the failure; must not be {@code null}
     * @param cause   the underlying exception that triggered this failure; may be {@code null}
     */
    public BleException(final @NonNull String message, final @Nullable Throwable cause) {
        super(message, cause);
    }
}
