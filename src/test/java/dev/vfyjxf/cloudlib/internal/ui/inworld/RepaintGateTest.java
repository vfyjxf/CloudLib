package dev.vfyjxf.cloudlib.internal.ui.inworld;

import dev.vfyjxf.cloudlib.api.ui.inworld.render.WorldUiPanel;
import org.junit.jupiter.api.Test;

import java.util.concurrent.atomic.AtomicLong;
import java.util.function.LongSupplier;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link RepaintGate} — the per-panel dirty-check cache behind the world
 * surface repaint. GL is unavailable headless, so the render action is a
 * counting runnable standing in for {@code UiSurface.render}: its invocation
 * count is exactly "how many times the surface was repainted".
 */
class RepaintGateTest {

    private static final class Counter {
        int runs;
    }

    private static boolean frame(RepaintGate gate, WorldUiPanel panel, int ss, Counter counter) {
        return gate.render(panel, ss, () -> counter.runs++);
    }

    @Test
    void nullVersionAlwaysRepaints() {
        WorldUiPanel panel = new WorldUiPanel(64, 32);
        RepaintGate gate = new RepaintGate();
        Counter counter = new Counter();
        for (int i = 0; i < 5; i++) assertTrue(frame(gate, panel, 2, counter));
        assertEquals(5, counter.runs, "no version declared — every frame repaints (backward compatibility)");
    }

    @Test
    void unchangedVersionSkipsTheRepaint() {
        AtomicLong version = new AtomicLong(1);
        WorldUiPanel panel = new WorldUiPanel(64, 32).contentVersion(version::get);
        RepaintGate gate = new RepaintGate();
        Counter counter = new Counter();

        assertTrue(frame(gate, panel, 2, counter), "the first frame always paints");
        for (int i = 0; i < 10; i++) {
            assertFalse(
                    frame(gate, panel, 2, counter),
                    "same version, same size, same supersample — the surface repaint is skipped");
        }
        assertEquals(1, counter.runs, "the painter ran exactly once");
    }

    @Test
    void versionChangeRepaints() {
        AtomicLong version = new AtomicLong(1);
        WorldUiPanel panel = new WorldUiPanel(64, 32).contentVersion(version::get);
        RepaintGate gate = new RepaintGate();
        Counter counter = new Counter();

        frame(gate, panel, 2, counter);
        version.set(2);
        assertTrue(frame(gate, panel, 2, counter), "a content change must repaint");
        assertFalse(frame(gate, panel, 2, counter), "and only once");

        version.set(3);
        assertTrue(frame(gate, panel, 2, counter));
        assertEquals(3, counter.runs);
    }

    @Test
    void sizeChangeForcesOneRepaint() {
        WorldUiPanel panel = new WorldUiPanel(64, 32).contentVersion(1);
        RepaintGate gate = new RepaintGate();
        Counter counter = new Counter();

        frame(gate, panel, 2, counter);
        assertFalse(frame(gate, panel, 2, counter));

        panel.size(96, 48); // the panel's logical surface changed — texture is new
        assertTrue(frame(gate, panel, 2, counter), "a size change must repaint once");
        assertFalse(frame(gate, panel, 2, counter), "and settle back to skipping");
    }

    @Test
    void supersampleChangeForcesOneRepaint() {
        WorldUiPanel panel = new WorldUiPanel(64, 32).contentVersion(1);
        RepaintGate gate = new RepaintGate();
        Counter counter = new Counter();

        frame(gate, panel, 2, counter);
        assertFalse(frame(gate, panel, 2, counter));

        int newGrant = 3; // the adaptive grant stepped — the texture storage is new
        assertTrue(frame(gate, panel, newGrant, counter), "a supersample change must repaint once");
        assertFalse(frame(gate, panel, newGrant, counter), "and settle back to skipping");
    }

    @Test
    void unconditionalFrameClearsTheCachedVersion() {
        AtomicLong version = new AtomicLong(1);
        WorldUiPanel panel = new WorldUiPanel(64, 32).contentVersion(version::get);
        RepaintGate gate = new RepaintGate();
        Counter counter = new Counter();

        frame(gate, panel, 2, counter);
        assertFalse(frame(gate, panel, 2, counter));

        // a pointer-interactive frame: the orchestrator drops the version and
        // the panel repaints unconditionally
        LongSupplier versioned = panel.contentVersion();
        panel.contentVersion((LongSupplier) null);
        assertTrue(frame(gate, panel, 2, counter));

        // pointer left — the version is back, and the gate must not trust the
        // stale comparison: one conservative repaint, then skipping resumes
        panel.contentVersion(versioned);
        assertTrue(frame(gate, panel, 2, counter), "the first versioned frame after an unconditional one repaints");
        assertFalse(frame(gate, panel, 2, counter));
        assertEquals(3, counter.runs);
    }

    @Test
    void versionReappearingAfterNullProfileRepaints() {
        WorldUiPanel panel = new WorldUiPanel(64, 32);
        RepaintGate gate = new RepaintGate();
        Counter counter = new Counter();

        frame(gate, panel, 2, counter); // version-less frame
        panel.contentVersion(5L);
        assertTrue(frame(gate, panel, 2, counter), "a version appearing must repaint (first versioned sight)");
        assertFalse(frame(gate, panel, 2, counter));
    }
}
