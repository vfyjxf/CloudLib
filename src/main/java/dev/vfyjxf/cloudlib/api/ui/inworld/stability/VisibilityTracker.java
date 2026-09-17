package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

import java.util.Objects;

/**
 * The appear/linger/fade state machine that keeps rejected elements from
 * blinking: {@code hidden → appearing → visible → lingering → fading}. A
 * rejection does not remove the element — it lingers at full alpha for
 * {@code lingerSeconds} (configure it to at least one decision-epoch length,
 * so a single missed re-present never tears the element down), and only then
 * fades out. New appearances fade in; a rescue (re-present) during linger
 * keeps the current alpha untouched, and even a rescue mid-fade resumes
 * fading in from the current alpha instead of restarting from zero.
 * <p>
 * The machine is driven by two explicit inputs only: presentation requests
 * ({@link #setPresent(boolean, double)}) and the query time — every method
 * takes the time as a parameter, never reads a clock. Transitions are
 * evaluated lazily at query time but back-dated to when they actually
 * happened (the request time for presentation-driven transitions, the phase
 * boundary for timed ones), so any number of epochs may pass between queries
 * without losing fade progress. Time is also defensively clamped to be
 * monotonic: a rewound timestamp is treated as the last seen time, so a
 * non-monotonic upstream clock (e.g. a server time re-sync) can never run a
 * fade backwards or throw on the render thread — fix the time source
 * upstream, but nothing in here will crash because of it. Only non-finite
 * times are rejected.
 * <p>
 * Alpha is piecewise linear over the configured fade durations and always in
 * {@code [0, 1]}: 0 in {@code hidden}, ramping from the phase's base alpha to
 * 1 in {@code appearing}, 1 in {@code visible}, constant in {@code
 * lingering}, decaying toward 0 in {@code fading}.
 */
public final class VisibilityTracker {

    /** The lifecycle phases, in transition order. */
    public enum Phase {
        hidden,
        appearing,
        visible,
        lingering,
        fading
    }

    /**
     * @param fadeInSeconds duration of the hidden → visible alpha ramp
     * @param fadeOutSeconds duration of the alpha ramp down to hidden
     * @param lingerSeconds how long a rejected element stays on screen before
     *        fading — at least one decision epoch in practice
     */
    public record Config(double fadeInSeconds, double fadeOutSeconds, double lingerSeconds) {

        public Config {
            if (!Double.isFinite(fadeInSeconds) || fadeInSeconds <= 0.0) {
                throw new IllegalArgumentException("fadeInSeconds must be finite and positive: " + fadeInSeconds);
            }
            if (!Double.isFinite(fadeOutSeconds) || fadeOutSeconds <= 0.0) {
                throw new IllegalArgumentException("fadeOutSeconds must be finite and positive: " + fadeOutSeconds);
            }
            if (!Double.isFinite(lingerSeconds) || lingerSeconds <= 0.0) {
                throw new IllegalArgumentException("lingerSeconds must be finite and positive: " + lingerSeconds);
            }
        }

        public static Config of(double fadeInSeconds, double fadeOutSeconds, double lingerSeconds) {
            return new Config(fadeInSeconds, fadeOutSeconds, lingerSeconds);
        }
    }

    private final Config config;

    private Phase phase = Phase.hidden;
    private double phaseStartSeconds;
    private double baseAlpha;
    private boolean present;
    private double presentSinceSeconds;
    private double lastSeenSeconds = Double.NEGATIVE_INFINITY;

    /** A tracker starting hidden, alpha 0, not presented. */
    public VisibilityTracker(Config config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * Records the decision layer's presentation request at
     * {@code nowSeconds}; the resulting transitions surface on the next
     * query, back-dated to {@code nowSeconds} (clamped to the last seen time
     * when it points backwards).
     */
    public void setPresent(boolean present, double nowSeconds) {
        double effective = advance(nowSeconds);
        this.present = present;
        this.presentSinceSeconds = effective;
    }

    /** The most recent presentation request. */
    public boolean present() {
        return present;
    }

    /** The lifecycle phase as of {@code nowSeconds}. */
    public Phase phase(double nowSeconds) {
        advance(nowSeconds);
        return phase;
    }

    /**
     * The visibility alpha in {@code [0, 1]} as of {@code nowSeconds} — the
     * value to feed the renderer. A rewound {@code nowSeconds} yields the
     * alpha at the last seen time.
     */
    public double alpha(double nowSeconds) {
        return alphaInPhase(advance(nowSeconds));
    }

    /** Whether anything should be drawn at all — every phase except {@code hidden}. */
    public boolean isRendered(double nowSeconds) {
        return phase(nowSeconds) != Phase.hidden;
    }

    /**
     * Walks the machine up to {@code nowSeconds}, which is clamped to the
     * last seen time when it points backwards — the machine only ever moves
     * forward.
     *
     * @return the effective (clamped) time the machine is now at
     */
    private double advance(double nowSeconds) {
        requireTime(nowSeconds);
        double effective = Math.max(nowSeconds, lastSeenSeconds);
        lastSeenSeconds = effective;
        boolean transitioned = true;
        while (transitioned) {
            transitioned = false;
            switch (phase) {
                case hidden -> {
                    if (present) {
                        enter(Phase.appearing, presentSinceSeconds, 0.0);
                        transitioned = true;
                    }
                }
                case appearing -> {
                    if (!present) {
                        enter(Phase.lingering, presentSinceSeconds, alphaInPhase(presentSinceSeconds));
                        transitioned = true;
                    } else if (effective >= phaseStartSeconds + config.fadeInSeconds()) {
                        enter(Phase.visible, phaseStartSeconds + config.fadeInSeconds(), 1.0);
                        transitioned = true;
                    }
                }
                case visible -> {
                    if (!present) {
                        enter(Phase.lingering, presentSinceSeconds, 1.0);
                        transitioned = true;
                    }
                }
                case lingering -> {
                    if (present) {
                        enter(baseAlpha < 1.0 ? Phase.appearing : Phase.visible, presentSinceSeconds, baseAlpha);
                        transitioned = true;
                    } else if (effective >= phaseStartSeconds + config.lingerSeconds()) {
                        enter(Phase.fading, phaseStartSeconds + config.lingerSeconds(), baseAlpha);
                        transitioned = true;
                    }
                }
                case fading -> {
                    if (present) {
                        enter(Phase.appearing, presentSinceSeconds, alphaInPhase(presentSinceSeconds));
                        transitioned = true;
                    } else if (effective >= phaseStartSeconds + config.fadeOutSeconds()) {
                        enter(Phase.hidden, phaseStartSeconds + config.fadeOutSeconds(), 0.0);
                        transitioned = true;
                    }
                }
            }
        }
        return effective;
    }

    private void enter(Phase next, double atSeconds, double alphaAtBoundary) {
        this.phase = next;
        this.phaseStartSeconds = atSeconds;
        this.baseAlpha = alphaAtBoundary;
    }

    private double alphaInPhase(double nowSeconds) {
        return switch (phase) {
            case hidden -> 0.0;
            case appearing -> {
                double progress = clamp01((nowSeconds - phaseStartSeconds) / config.fadeInSeconds());
                yield baseAlpha + (1.0 - baseAlpha) * progress;
            }
            case visible -> 1.0;
            case lingering -> baseAlpha;
            case fading -> {
                double progress = clamp01((nowSeconds - phaseStartSeconds) / config.fadeOutSeconds());
                yield baseAlpha * (1.0 - progress);
            }
        };
    }

    private static double clamp01(double value) {
        return Math.min(1.0, Math.max(0.0, value));
    }

    private static void requireTime(double nowSeconds) {
        if (!Double.isFinite(nowSeconds)) {
            throw new IllegalArgumentException("nowSeconds must be finite: " + nowSeconds);
        }
    }
}
