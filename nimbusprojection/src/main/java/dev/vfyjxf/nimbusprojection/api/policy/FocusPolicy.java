package dev.vfyjxf.nimbusprojection.api.policy;

/**
 * Per-panel override of focus selection — how eagerly this panel claims
 * the in-world focus.
 * <p>
 * A policy only <em>scores</em>: the runtime collects every candidate's
 * score each frame, applies a uniform incumbent-hysteresis multiplier, and
 * the winner takes focus. Score-based so policies compose with the shared
 * selection model instead of replacing it — consistent targeting UX across
 * panels.
 * <p>
 * Declared via {@code PanelSpec.focusPolicy(...)}; {@code null} =
 * {@link #softCone()}.
 */
@FunctionalInterface
public interface FocusPolicy {

    /** Return from {@link #score} to exclude the panel this frame. */
    double UNFOCUSABLE = Double.NEGATIVE_INFINITY;

    /**
     * Scores the panel as a focus candidate. Higher wins; angle should
     * dominate, distance should break ties.
     */
    double score(FocusContext ctx);

    /**
     * The runtime default: ~30° soft cone around the look vector,
     * angle-dominant scoring with distance as the tiebreak; an exact
     * crosshair hit outranks everything.
     */
    static FocusPolicy softCone() {
        return ctx -> {
            if (ctx.exactHit()) return 2.0;
            double cos30 = 0.8660254;
            if (ctx.angleCos() < cos30) return UNFOCUSABLE;
            return ctx.angleCos() - ctx.distance() * 1e-3;
        };
    }

    /** Only an exact crosshair hit on the anchor/surface can focus this panel. */
    static FocusPolicy exactPick() {
        return ctx -> ctx.exactHit() ? 1.0 - ctx.distance() * 1e-3 : UNFOCUSABLE;
    }

    /** The panel can never take focus (equivalent to {@code interactive(false)}). */
    static FocusPolicy never() {
        return ctx -> UNFOCUSABLE;
    }

}
