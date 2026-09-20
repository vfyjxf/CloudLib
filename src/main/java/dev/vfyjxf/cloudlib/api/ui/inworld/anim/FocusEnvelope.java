package dev.vfyjxf.cloudlib.api.ui.inworld.anim;

/**
 * The focus frame's opacity envelope: a one-shot brightness pulse on the
 * acquire, an ease-in dim on the loss. Geometry never changes — only
 * opacity moves, so the frame reads as a state, not an animation. The
 * acquire reuses {@link PairingPulse}'s curve and cue budget; the loss is a
 * quadratic ease-in (slow to start, accelerating out) with no pulse and no
 * bounce, so a lost selection simply sinks.
 * <p>
 * Pure state machine: no wall clock, no GL — the caller injects the frame
 * delta. {@link #advance} once per frame with the frame's focus flag; the
 * returned {@link Sample} is the frame's opacity for that frame.
 */
public final class FocusEnvelope {

    /**
     * The envelope's shape.
     *
     * @param pulseSeconds the acquire pulse's full length — onset to silence;
     *        positive
     * @param opacityDelta the pulse's brightness bonus at its curve's peak, an
     *        absolute opacity delta; positive and at most 0.35 (a selection
     *        cue, not a flash)
     * @param lossSeconds the loss fade's full length — full ink to gone;
     *        positive
     */
    public record Config(double pulseSeconds, double opacityDelta, double lossSeconds) {

        public Config {
            if (!Double.isFinite(pulseSeconds) || pulseSeconds <= 0) {
                throw new IllegalArgumentException("pulseSeconds must be finite and positive: " + pulseSeconds);
            }
            if (!Double.isFinite(opacityDelta) || opacityDelta <= 0 || opacityDelta > 0.35) {
                throw new IllegalArgumentException(
                        "opacityDelta must be finite, positive and at most 0.35: " + opacityDelta);
            }
            if (!Double.isFinite(lossSeconds) || lossSeconds <= 0) {
                throw new IllegalArgumentException("lossSeconds must be finite and positive: " + lossSeconds);
            }
        }

        /** The survey default: a 160 ms acquire pulse at +0.3, a 100 ms loss. */
        public static Config ofDefaults() {
            return new Config(0.16, 0.3, 0.10);
        }
    }

    /**
     * One frame's frame: {@code alpha} is the frame's overall opacity in
     * [0, 1], {@code brighten} the acquire pulse's absolute opacity bonus in
     * [0, delta] — the drawn ink is their sum, clamped by the caller.
     */
    public record Sample(float alpha, float brighten) {

        /** Whether the frame draws at all this frame. */
        public boolean visible() {
            return alpha > 0f;
        }
    }

    private final Config config;
    private final PairingPulse pulse;
    private boolean focused;
    private boolean losing;
    private double lossElapsed;
    private Sample last = new Sample(0f, 0f);
    private double phase;

    public FocusEnvelope(Config config) {
        this.config = config == null ? Config.ofDefaults() : config;
        pulse = new PairingPulse(new PairingPulse.Config(this.config.pulseSeconds(), this.config.opacityDelta()));
    }

    public Config config() {
        return config;
    }

    /**
     * Advances one frame and answers the focus frame's opacity for it: full
     * ink while focused (plus the pulse's bonus for one pulse after each
     * gain — the frame focus was gained on already answers above zero),
     * then an ease-in dim over the loss window, then nothing until the next
     * gain. A regain mid-loss snaps straight back to full ink and pulses
     * from its onset.
     *
     * @param focusedNow whether the panel holds focus this frame
     * @param dtSeconds the frame's delta; non-negative
     */
    public Sample advance(boolean focusedNow, double dtSeconds) {
        if (!Double.isFinite(dtSeconds) || dtSeconds < 0) {
            throw new IllegalArgumentException("dtSeconds must be finite and non-negative: " + dtSeconds);
        }
        boolean gained = focusedNow && !focused;
        boolean lost = !focusedNow && focused;
        focused = focusedNow;
        if (gained) {
            pulse.fire();
            losing = false;
        }
        if (focusedNow) {
            double weight = pulse.advance(dtSeconds);
            last = new Sample(1f, (float) (config.opacityDelta() * weight));
        } else if (lost) {
            // the loss never pulses, and the next gain starts a fresh onset
            pulse.reset();
            losing = true;
            lossElapsed = dtSeconds;
            last = dimmed();
        } else if (losing) {
            lossElapsed += dtSeconds;
            last = dimmed();
        } else {
            last = new Sample(0f, 0f);
        }
        boolean animating = focusedNow ? pulse.running() : last.alpha() > 0f;
        phase = animating ? phase + dtSeconds : focusedNow ? 1.0 : 0.0;
        return last;
    }

    /** The loss's ease-in dim at the current elapsed: full ink decaying as 1 − t². */
    private Sample dimmed() {
        if (lossElapsed >= config.lossSeconds()) {
            return new Sample(0f, 0f);
        }
        double t = lossElapsed / config.lossSeconds();
        return new Sample((float) (1.0 - t * t), 0f);
    }

    /**
     * The animation phase — strictly increasing while a pulse or a loss is
     * running, constant while idle (1 held, 0 unfocused). The repaint-version
     * input for cached surfaces: a changing phase forces the repaint an
     * animating frame needs, an idle one costs nothing.
     */
    public double phase() {
        return phase;
    }

    /** Back to the never-focused state (a new scene, a teleport). */
    public void reset() {
        focused = false;
        losing = false;
        lossElapsed = 0.0;
        last = new Sample(0f, 0f);
        phase = 0.0;
        pulse.reset();
    }
}
