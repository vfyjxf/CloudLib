package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.concurrent.atomic.AtomicReference;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;

/**
 * {@link WorldUiPanel}'s content-version protocol: the panel is pure data
 * (safe to build headless), the version supplier is stored as supplied, and
 * the convenience overloads adapt — a constant long stays constant, an
 * equals-compared token steps once per change. Default: null = always dirty.
 */
class WorldUiPanelContentVersionTest {

    @Test
    void defaultsToNullVersion() {
        WorldUiPanel panel = new WorldUiPanel(10, 10);
        assertNull(panel.contentVersion(), "no version declared — always dirty, the pre-protocol behavior");
    }

    @Test
    void longSupplierIsStoredAsSupplied() {
        WorldUiPanel panel = new WorldUiPanel(10, 10);
        LongSupplier version = () -> 42;
        assertSame(panel, panel.contentVersion(version));
        assertSame(version, panel.contentVersion());

        panel.contentVersion((LongSupplier) null);
        assertNull(panel.contentVersion(), "null clears the version back to always-dirty");
    }

    @Test
    void constantLongIsAConstantVersion() {
        WorldUiPanel panel = new WorldUiPanel(10, 10).contentVersion(9);
        assertEquals(9, Objects.requireNonNull(panel.contentVersion()).getAsLong());
        assertEquals(9, panel.contentVersion().getAsLong());
    }

    record Token(int count, List<String> labels) {}

    @Test
    void tokenSupplierAdaptsThroughEqualsComparison() {
        AtomicReference<Token> token = new AtomicReference<>(new Token(1, List.of("a")));
        WorldUiPanel panel = new WorldUiPanel(10, 10).contentVersion(token::get);

        long first = Objects.requireNonNull(panel.contentVersion()).getAsLong();
        assertEquals(first, panel.contentVersion().getAsLong(), "unchanged token holds the version");

        token.set(new Token(1, List.of("a")));
        assertEquals(first, panel.contentVersion().getAsLong(), "equal token (new instance) holds the version");

        token.set(new Token(2, List.of("a")));
        long changed = panel.contentVersion().getAsLong();
        assertNotEquals(first, changed, "a token change must move the version");
        assertEquals(changed, panel.contentVersion().getAsLong());
    }
}
