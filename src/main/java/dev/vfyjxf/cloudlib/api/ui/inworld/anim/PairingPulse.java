package dev.vfyjxf.cloudlib.api.ui.inworld.anim;

/**
 * The attach tier's one-shot pairing pulse: when a leader collapses into
 * its mark form, the panel-side marks and the anchor dot brighten together
 * — same frame, same curve, exactly once — so the two ends read as one
 * event (common fate: the onset window the eye groups by is a frame or
 * less, and one {@link #advance} per frame drives both ends from the same
 * weight). Never a loop — a repeating pulse would read as an alarm, not a
 * pairing.
 * <p>
 * Pure state machine: no wall clock, no GL — the caller injects the frame
 * delta. {@link #fire} on the transition into the attach tier, then
 * {@link #advance} every frame; the returned weight in [0, 1] is the
 * curve's value for that frame, which the caller scales its brightness
 * bonus by (an opacity delta of at most {@link Config#opacityDelta}).
 */
public final class PairingPulse {

    /**
     * The pulse's shape.
     *
     * @param durationSeconds the pulse's full length — onset to silence;
     *        positive
     * @param opacityDelta the brightness bonus at the curve's peak, an
     *        absolute opacity delta; positive and at most 0.35 (a pairing
     *        cue, not a flash)
     */
    public record Config(double durationSeconds, double opacityDelta) {

        public Config {
            if (!Double.isFinite(durationSeconds) || durationSeconds <= 0) {
                throw new IllegalArgumentException("durationSeconds must be finite and positive: " + durationSeconds);
            }
            if (!Double.isFinite(opacityDelta) || opacityDelta <= 0 || opacityDelta > 0.35) {
                throw new IllegalArgumentException(
                    "opacityDelta must be finite, positive and at most 0.35: " + opacityDelta
                );
            }
        }

        /** The survey pulse: 200 ms, a +0.3 opacity peak. */
        public static Config ofDefaults() {
            return new Config(0.2, 0.3);
        }
    }

    private static final double idle = Double.NEGATIVE_INFINITY;

    private final Config config;
    private double elapsed = idle;

    public PairingPulse(Config config) {
        this.config = config;
    }

    public Config config() {
        return config;
    }

    /** Starts a pulse; ignored while one runs — one pulse per entry. */
    public void fire() {
        if (!running()) {
            elapsed = 0.0;
        }
    }

    public boolean running() {
        return elapsed != idle;
    }

    /**
     * Advances one frame and answers the pulse's weight for it — 0 outside
     * the pulse, otherwise the sine bump from 0 at the onset over 1 at the
     * midpoint back to 0 at the end. The frame {@code fire} was called on
     * already answers above zero (the onset is that frame, not the next).
     *
     * @param dtSeconds the frame's delta; non-negative
     */
    public double advance(double dtSeconds) {
        if (!Double.isFinite(dtSeconds) || dtSeconds < 0) {
            throw new IllegalArgumentException("dtSeconds must be finite and non-negative: " + dtSeconds);
        }
        if (!running()) {
            return 0.0;
        }
        elapsed += dtSeconds;
        if (elapsed >= config.durationSeconds()) {
            elapsed = idle; // silence — and it stays silent until fired again
            return 0.0;
        }
        return Math.sin(Math.PI * elapsed / config.durationSeconds());
    }

    /** Back to the never-pulsed state (a new scene, a teleport). */
    public void reset() {
        elapsed = idle;
    }
}
