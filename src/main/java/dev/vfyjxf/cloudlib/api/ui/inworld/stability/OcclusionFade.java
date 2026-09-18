package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

import dev.vfyjxf.cloudlib.api.ui.inworld.zone.VisibilityPolicy;

/**
 * The occlusion-fade policy: the hysteresis thresholds and consecutive-frame
 * gate a visibility consumer applies to an {@code OcclusionProbe}'s measured
 * visibility fraction — hard occlusion must persist before the panel fades
 * and must clear for the same span before it returns. Between the thresholds
 * the state holds — the band that stops a panel stuttering while an edge of
 * its anchor slides behind a corner.
 * <p>
 * The drive side lives here too, as companion constants and helpers: a panel
 * under the {@link VisibilityPolicy#fade} policy eases its alpha toward
 * {@link #occludedAlpha} while occluded (and neither selected nor inspected)
 * and back to 1 otherwise, stepping linearly on frame deltas so the fade
 * completes within {@link #transitionSeconds} without ever snapping.
 */
public record OcclusionFade(double exitThreshold, double enterThreshold, int frames) {

    /** 0.25/0.55 hysteresis, five consecutive frames — the plan's baseline. */
    public static final OcclusionFade standard = new OcclusionFade(0.25, 0.55, 5);

    /** The alpha a faded panel presents at while occluded. */
    public static final float occludedAlpha = 0.25f;

    /** The full transition length in seconds, both directions. */
    public static final double transitionSeconds = 0.2;

    public OcclusionFade {
        if (!(exitThreshold >= 0 && exitThreshold < enterThreshold && enterThreshold <= 1)) {
            throw new IllegalArgumentException(
                    "thresholds must satisfy 0 <= exit < enter <= 1: (" + exitThreshold + ", " + enterThreshold + ")");
        }
        if (frames < 1) {
            throw new IllegalArgumentException("frames must be >= 1: " + frames);
        }
    }

    /**
     * The target alpha for this frame: the fade policy dims while the gaze
     * pass measures occlusion and neither selection nor the inspect
     * presentation holds the panel up.
     *
     * @param occluded whether this frame's sight lines are blocked (false
     *                 when no gaze decision exists yet)
     * @param selected whether the crosshair currently selects the panel
     * @param inspecting whether the inspect presentation holds panels clear
     */
    public static float target(VisibilityPolicy policy, boolean occluded, boolean selected, boolean inspecting) {
        if (policy != VisibilityPolicy.fade || inspecting) return 1f;
        if (!occluded || selected) return 1f;
        return occludedAlpha;
    }

    /**
     * One easing step: linear toward {@code target} at the full-scale rate of
     * {@link #transitionSeconds} — any transition completes within 0.2 s
     * without snapping.
     */
    public static float step(float current, float target, double dtSeconds) {
        if (current == target) return current;
        double maxStep = Math.max(0.0, dtSeconds) / transitionSeconds;
        double delta = Math.max(-maxStep, Math.min(maxStep, target - current));
        double next = current + delta;
        if (Math.abs(next - target) < 1.0e-4) {
            return target;
        }
        return (float) next;
    }
}
