package ch.varani.lego.ble.api;

import static org.junit.jupiter.api.Assertions.assertAll;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

import org.junit.jupiter.api.Test;

/**
 * Unit tests for {@link BleException}.
 */
class BleExceptionTest {

    @Test
    void constructor_messageOnly_storesMessage() {
        final BleException ex = new BleException("test message");
        assertAll(
            () -> assertEquals("test message", ex.getMessage()),
            () -> assertNotNull(ex.getMessage())
        );
    }

    @Test
    void constructor_messageAndNullCause_storesBoth() {
        final BleException ex = new BleException("msg", null);
        assertAll(
            () -> assertEquals("msg", ex.getMessage()),
            () -> assertEquals(null, ex.getCause())
        );
    }

    @Test
    void constructor_messageAndCause_storesBoth() {
        final RuntimeException cause = new RuntimeException("root");
        final BleException ex = new BleException("wrapper", cause);
        assertAll(
            () -> assertEquals("wrapper", ex.getMessage()),
            () -> assertEquals(cause, ex.getCause())
        );
    }

    @Test
    void bleException_isThrowable_canBeThrown() {
        assertThrows(BleException.class, () -> {
            throw new BleException("thrown");
        });
    }
}
