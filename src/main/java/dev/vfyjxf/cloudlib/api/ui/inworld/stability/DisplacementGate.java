package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * The input-level displacement dead zone (§3.5's dirty rule, the sub-threshold
 * half): holds a reference point and only re-anchors it when the live point
 * has moved at least {@link Config#releasePx} away. Sub-threshold drift is
 * invisible downstream — the held point is what layout consumers read, so a
 * jittering input cannot re-argue a decision, while the <em>rendering</em>
 * path may keep following the live point (the dead zone is at the input
 * level, not the presentation level).
 * <p>
 * Unlike {@link PixelStabilizer} (a presentation filter with sub-pixel
 * continuity), this gate quantizes the <em>decision input</em> in release-px
 * steps: the output is always a previously seen live position, moves in
 * jumps of at least {@code releasePx}, and never invents positions. Boundary
 * values are inclusive — a displacement of exactly {@code releasePx}
 * releases. A null observation resets the gate (no anchor to hold); the next
 * non-null observation re-seeds the reference.
 */
public final class DisplacementGate {

    /**
     * The one knob.
     *
     * @param releasePx the displacement, in the point's units, that the live
     *        point must travel from the held reference before the reference
     *        re-anchors; must be finite and positive
     */
    public record Config(double releasePx) {

        public Config {
            if (!Double.isFinite(releasePx) || releasePx <= 0.0) {
                throw new IllegalArgumentException("releasePx must be finite and positive: " + releasePx);
            }
        }

        public static Config of(double releasePx) {
            return new Config(releasePx);
        }
    }

    private final Config config;
    private @Nullable FloatPos reference;

    /** A gate holding nothing — the first observation seeds the reference. */
    public DisplacementGate(Config config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * Observes the live point and returns the held point: the reference until
     * the live point leaves the release radius, the live point itself the
     * moment it does. A null observation resets the gate and returns null.
     */
    public @Nullable FloatPos observe(@Nullable FloatPos live) {
        if (live == null) {
            reference = null;
            return null;
        }
        if (reference == null || distance(live, reference) >= config.releasePx()) {
            reference = new FloatPos(live.x(), live.y());
        }
        return reference;
    }

    /** The held point without observing — null while the gate holds nothing. */
    public @Nullable FloatPos held() {
        return reference;
    }

    /** Drops the reference; the next observation re-seeds it. */
    public void reset() {
        reference = null;
    }

    private static double distance(FloatPos a, FloatPos b) {
        return Math.hypot(a.x() - b.x(), a.y() - b.y());
    }
}
