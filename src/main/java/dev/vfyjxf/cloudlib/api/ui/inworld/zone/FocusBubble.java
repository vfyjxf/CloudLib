package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * The radius-hysteresis state machine for "is the anchor inside the focus
 * bubble": one scalar distance per decision tick (anchor-to-crosshair, in
 * pixels — any consistently scaled radius works), two thresholds with
 * {@code expandRadius < collapseRadius}. The bubble <em>enters</em> when the
 * distance is at or below {@code expandRadius} and <em>leaves</em> only when
 * it exceeds {@code collapseRadius}; between the two thresholds the state
 * holds. A distance jittering 129/131 around an expand threshold of 130
 * therefore flips at most once (on the first 129) and never back — the
 * standard Schmitt-trigger answer to boundary ping-pong, the same control
 * idea as {@code stability/SwitchGate} but for a boolean with a null origin
 * state.
 * <p>
 * The state is the tri-valued {@link #active()}: {@code null} means the
 * bubble has never activated (the element was never near the crosshair), and
 * once activated it reports {@code true}/{@code false} forever after. Time
 * does not participate — only the driven distance sequence does, so the
 * machine is trivially headless-testable.
 */
public final class FocusBubble {

    /**
     * The two radii, in the driven distance's units (pixels).
     *
     * @param expandRadius the entry radius — the bubble enters at distances
     *        at or below it
     * @param collapseRadius the exit radius — the bubble leaves at distances
     *        above it; must be strictly greater than {@code expandRadius}
     */
    public record Config(double expandRadius, double collapseRadius) {

        public Config {
            if (!Double.isFinite(expandRadius) || expandRadius <= 0.0) {
                throw new IllegalArgumentException("expandRadius must be finite and positive: " + expandRadius);
            }
            if (!Double.isFinite(collapseRadius) || collapseRadius <= expandRadius) {
                throw new IllegalArgumentException(
                        "collapseRadius must be finite and greater than expandRadius: " + collapseRadius);
            }
        }

        public static Config defaults() {
            return new Config(130.0, 180.0);
        }

        public static Config of(double expandRadius, double collapseRadius) {
            return new Config(expandRadius, collapseRadius);
        }
    }

    private final Config config;

    private @Nullable Boolean active;

    public FocusBubble(Config config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /** A bubble with {@link Config#defaults()}. */
    public static FocusBubble withDefaults() {
        return new FocusBubble(Config.defaults());
    }

    public Config config() {
        return config;
    }

    /**
     * One decision tick with the measured distance (finite, non-negative).
     *
     * @return whether the bubble's state flipped on this tick — the first
     *         activation counts as a flip
     */
    public boolean update(double distance) {
        if (!Double.isFinite(distance) || distance < 0.0) {
            throw new IllegalArgumentException("distance must be finite and non-negative: " + distance);
        }
        Boolean previous = active;
        if (Boolean.TRUE.equals(active)) {
            if (distance > config.collapseRadius()) {
                active = false;
            }
        } else if (distance <= config.expandRadius()) {
            active = true;
        }
        return !Objects.equals(previous, active);
    }

    /**
     * The bubble's state: {@code null} = never activated, else whether the
     * anchor is currently inside the bubble.
     */
    public @Nullable Boolean active() {
        return active;
    }

    /** Whether the anchor is inside the bubble — {@code false} when never activated. */
    public boolean inside() {
        return Boolean.TRUE.equals(active);
    }

    /** Whether the bubble has ever activated. */
    public boolean everActivated() {
        return active != null;
    }
}
