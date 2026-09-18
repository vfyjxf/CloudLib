package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;

/**
 * {@link ContentVersions} — the adapters producing content-version longs for
 * the {@link WorldUiPanel} dirty protocol. The token adapter is a stateful
 * monotonic counter over equals-compared tokens: the long must move exactly
 * once per semantic change and stay put while the token is equal, whatever
 * instance carries the value.
 */
class ContentVersionsTest {

    record Token(int a, String b, List<Integer> list) {}

    @Test
    void fixedNeverChanges() {
        LongSupplier version = ContentVersions.fixed(7);
        assertEquals(7, version.getAsLong());
        assertEquals(7, version.getAsLong());
        assertEquals(7, version.getAsLong());
    }

    @Test
    void tokenVersionStepsOncePerChangeAndHoldsBetween() {
        AtomicReference<Token> token = new AtomicReference<>(new Token(1, "a", List.of(1, 2)));
        LongSupplier version = ContentVersions.of(token::get);

        long first = version.getAsLong();
        assertEquals(first, version.getAsLong(), "equal token (same instance) must not step the version");
        token.set(new Token(1, "a", List.of(1, 2)));
        assertEquals(first, version.getAsLong(), "equal token (new instance, same values) must not step the version");

        token.set(new Token(2, "a", List.of(1, 2)));
        long second = version.getAsLong();
        assertNotEquals(first, second, "a changed field must step the version");
        assertEquals(second, version.getAsLong(), "holding at the new value must not step again");

        token.set(new Token(2, "b", List.of(1, 2)));
        assertNotEquals(second, version.getAsLong(), "a different changed field must step too");

        // stepping back to a previously seen value is still a change — the
        // version is a change counter, not a value hash
        token.set(new Token(1, "a", List.of(1, 2)));
        assertNotEquals(first, version.getAsLong());
    }

    @Test
    void nullTokenTransitionsStep() {
        AtomicReference<Token> token = new AtomicReference<>();
        LongSupplier version = ContentVersions.of(token::get);

        long empty = version.getAsLong();
        assertEquals(empty, version.getAsLong());

        token.set(new Token(1, "x", List.of()));
        long set = version.getAsLong();
        assertNotEquals(empty, set);

        token.set(null);
        assertNotEquals(set, version.getAsLong(), "clearing the token is a change");
    }
}
