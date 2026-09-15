package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.internal.ui.inworld.layout.LayoutEngine;

/**
 * The public entry point of the in-world UI layout solver.
 * <p>
 * One {@code InworldLayout} owns one solver session: a {@link LayoutEngine}
 * plus the {@link LayoutState} carrying panel memory, debounce windows and
 * the stable-frame cache between frames. Hosts build a {@link LayoutFrame}
 * per rendered frame — camera, HUD regions, world obstacles, sources and
 * requests — and call {@link #solve} (or {@link #prepare} when the frame is
 * needed for hit-testing and the render adapters).
 * <p>
 * The solver is deterministic and allocation-conscious but not real-time
 * free; {@link #lastStats} reports the effort of the latest solve.
 */
public final class InworldLayout {

    private final LayoutEngine engine;
    private final LayoutState state = new LayoutState();

    public InworldLayout() {
        this(SearchBudget.defaults());
    }

    public InworldLayout(SearchBudget budget) {
        this.engine = new LayoutEngine(budget);
    }

    /** The search-effort counters of the latest {@link #solve}. */
    public SearchStats lastStats() {
        return engine.lastStats();
    }

    /** Solves one frame — the full panel/leader/overflow plan. */
    public LayoutResult solve(LayoutFrame frame) {
        return engine.solve(frame, state);
    }

    /** Solves and bundles the frame with its result for drawing and hit-testing. */
    public PreparedLayout prepare(LayoutFrame frame) {
        return new PreparedLayout(frame, solve(frame));
    }

    /**
     * Drops all carried-over state — memory, debounce, caches. Called
     * automatically on dimension or time discontinuity; hosts call this
     * when the view restarts (e.g. leaving a world).
     */
    public void resetView() {
        state.reset();
    }
}
