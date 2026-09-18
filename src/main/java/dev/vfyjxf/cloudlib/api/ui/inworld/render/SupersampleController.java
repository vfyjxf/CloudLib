package dev.vfyjxf.cloudlib.api.ui.inworld.render;

/**
 * The quantization/hysteresis half of adaptive supersampling: the desired
 * factor changes every frame with camera distance and viewing angle, and
 * following it directly would resize the panel's FBO (and re-rasterize at a
 * new texel density) constantly. This controller only switches after the
 * desired value has stayed stable for {@link #stableFramesNeeded} consecutive
 * frames, so a smoothly approaching camera still lands on a few discrete
 * steps and a jittering projection (hand-held camera micro-motion, animation
 * blending) never thrashes the surface.
 * <p>
 * Semantics of {@link #observe}:
 * <ul>
 *   <li>the first observed value is adopted immediately — the initial FBO
 *       allocation should already be the right size;</li>
 *   <li>a value matching the current factor clears any pending candidate
 *       (single-frame blips after a switch never rebound);</li>
 *   <li>a non-positive value is "no observation this frame" (hidden panel,
 *       degenerate projection) — the current factor is held, pending state
 *       untouched;</li>
 *   <li>a genuinely new value must survive {@link #stableFramesNeeded}
 *       consecutive frames before it is applied.</li>
 * </ul>
 */
public final class SupersampleController {

    /** The default stability window — at 60 fps a switch lands within ~170 ms of the change. */
    public static final int defaultStableFrames = 10;

    private final int stableFramesNeeded;
    private int current = -1;
    private int candidate;
    private int candidateFrames;

    /** A controller using the {@link #defaultStableFrames} window. */
    public SupersampleController() {
        this(defaultStableFrames);
    }

    /** @param stableFramesNeeded how many consecutive stable frames a new value needs before it is applied */
    public SupersampleController(int stableFramesNeeded) {
        this.stableFramesNeeded = Math.max(1, stableFramesNeeded);
    }

    /**
     * Feeds this frame's desired factor and returns the factor to apply.
     *
     * @param desired the frame's desired supersample factor, or {@code ≤ 0}
     *     when no observation is available this frame
     */
    public int observe(int desired) {
        if (desired <= 0) return current();
        if (current < 0) {
            current = candidate = desired;
            candidateFrames = 0;
            return current;
        }
        if (desired == current) {
            candidate = current;
            candidateFrames = 0;
            return current;
        }
        if (desired == candidate) candidateFrames++;
        else {
            candidate = desired;
            candidateFrames = 1;
        }
        if (candidateFrames >= stableFramesNeeded) {
            current = candidate;
            candidateFrames = 0;
        }
        return current;
    }

    /** The factor currently applied; {@code ≤ 0} before the first observation. */
    public int current() {
        return current;
    }
}
