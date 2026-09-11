package dev.vfyjxf.cloudlib.ui.inworld;

import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelSpec;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPlacement;
import net.minecraft.core.BlockPos;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicBoolean;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** API-level coverage of {@link InworldPanelSpec} fluent configuration. */
class InworldPanelSpecTest {

    private static InworldPanelSpec spec(String key) {
        return InworldPanelSpec.of(key,
                InworldAnchor.of(new BlockPos(0, 64, 0)),
                InworldPlacement.dock(),
                ctx -> null);
    }

    @Test
    void groupDerivesFromKeyParentPath() {
        assertEquals("tracker/ent", spec("tracker/ent/12").group());
        assertEquals("solo", spec("solo").group());
    }

    @Test
    void explicitGroupWins() {
        assertEquals("zone/a", spec("x/y/1").group("zone/a").group());
    }

    @Test
    void groupLimitClampsToAtLeastOne() {
        assertEquals(1, spec("a").groupLimit(0).groupLimit());
        assertEquals(4, spec("a").groupLimit(4).groupLimit());
        assertEquals(Integer.MAX_VALUE, spec("a").groupLimit());
    }

    @Test
    void offscreenCollapseDefaultsOff() {
        assertFalse(spec("a").collapsesOffscreen());
    }

    @Test
    void offscreenIndicatorAlwaysOn() {
        assertTrue(spec("a").offscreenIndicator().collapsesOffscreen());
    }

    @Test
    void offscreenIndicatorFollowsSupplier() {
        AtomicBoolean allow = new AtomicBoolean(false);
        InworldPanelSpec s = spec("a").offscreenIndicator(allow::get);
        assertFalse(s.collapsesOffscreen());
        allow.set(true);
        assertTrue(s.collapsesOffscreen());
    }

    @Test
    void actionDefaultsNullAndIsStored() {
        assertNull(spec("a").action());
        AtomicInteger calls = new AtomicInteger();
        InworldPanelSpec s = spec("a").action(ctx -> calls.incrementAndGet());
        assertNotNull(s.action());
        s.action().accept(null);
        assertEquals(1, calls.get());
    }

    @Test
    void fluentDefaults() {
        InworldPanelSpec s = spec("a");
        assertTrue(s.interactive());
        assertTrue(s.leaderLine());
        assertEquals(32, s.maxDistance());
        assertSame(s, s.interactive(false));
        assertFalse(s.interactive());
    }

}
