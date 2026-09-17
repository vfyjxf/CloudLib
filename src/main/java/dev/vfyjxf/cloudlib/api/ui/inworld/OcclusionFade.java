package dev.vfyjxf.cloudlib.api.ui.inworld;

/**
 * The fade-policy value object occlusion consumers feed their visibility
 * gate: hysteresis thresholds around the {@link OcclusionProbe} visibility
 * fraction plus a consecutive-frame gate, so hard occlusion must persist
 * before the panel fades and must clear for the same span before it returns.
 * Between the thresholds the state holds — the band that stops a panel
 * stuttering while an edge of its anchor slides behind a corner.
 * <p>
 * Pure data; the state machine itself belongs to the stability kit that
 * consumes this.
 */
public record OcclusionFade(double exitThreshold, double enterThreshold, int frames) {

    /** 0.25/0.55 hysteresis, five consecutive frames — the plan's baseline. */
    public static final OcclusionFade standard = new OcclusionFade(0.25, 0.55, 5);

    public OcclusionFade {
        if (!(exitThreshold >= 0 && exitThreshold < enterThreshold && enterThreshold <= 1)) {
            throw new IllegalArgumentException(
                    "thresholds must satisfy 0 <= exit < enter <= 1: (" + exitThreshold + ", " + enterThreshold + ")");
        }
        if (frames < 1) {
            throw new IllegalArgumentException("frames must be >= 1: " + frames);
        }
    }
}
