package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.internal.ui.inworld.layout.RecoveryGate;
import dev.vfyjxf.cloudlib.internal.ui.inworld.layout.StableFrame;
import dev.vfyjxf.cloudlib.internal.ui.inworld.layout.TemporalDebounce;

import java.util.HashMap;
import java.util.Map;

/**
 * The solver's carry-over state between frames: per-panel memory,
 * temporal debounce windows, recovery observations and the stable-frame
 * cache. Create one per layout session and hand it to every
 * {@code solve} call; {@link #reset()} clears everything (done
 * automatically on dimension or time discontinuity).
 * <p>
 * The fields are public for the solver internals — hosts should treat
 * them as read-only.
 */
public final class LayoutState {

    public final TemporalDebounce debounce = new TemporalDebounce();
    public StableFrame cachedInput;
    public LayoutResult cachedResult;
    public final Map<String, RecoveryGate.Observation> recovery = new HashMap<>();
    public final Map<String, PanelMemory> panels = new HashMap<>();
    public GuiRect dock;
    public GuiRect drawer;
    public String dimension;
    public double lastTime = Double.NEGATIVE_INFINITY;

    public void reset() {
        debounce.reset();
        cachedInput = null;
        cachedResult = null;
        recovery.clear();
        panels.clear();
        dock = null;
        drawer = null;
        dimension = null;
        lastTime = Double.NEGATIVE_INFINITY;
    }
}
