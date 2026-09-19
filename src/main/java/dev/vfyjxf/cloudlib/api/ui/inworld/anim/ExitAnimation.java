package dev.vfyjxf.cloudlib.api.ui.inworld.anim;

/**
 * How a UI surface animates away when it disappears (hidden, closed or
 * withdrawn): a pure envelope — alpha, rise and scale factors over a
 * duration — evaluated against a caller-supplied monotonic clock (both the
 * in-screen and the in-world render channel drive it; the channel decides
 * what "rise" and "scale" mean in its own space).
 * <p>
 * The record is the declarative vocabulary; the state machine around it
 * (freeze, interruption, completion) belongs to the host runtime. Two
 * normalizations keep every instance well-behaved: a {@link Kind#none} kind
 * is always a 0&nbsp;ms instant exit, and any duration is clamped to
 * {@link #maxDurationMs} — an exit overlay must never linger as a zombie
 * over content that has logically moved on.
 */
public record ExitAnimation(Kind kind, int durationMs, Easing easing) {

    /** The exit vocabulary. */
    public enum Kind {
        /** No animation — the surface disappears the moment it is hidden. */
        none,
        /** Alpha 1 → 0 over the duration. */
        fade,
        /** {@link Kind#fade} plus a slight rise (the surface drifts up as it fades). */
        fadeRise,
        /** {@link Kind#fade} plus a shrink toward {@link #endScale} around the surface's center. */
        fadeScale
    }

    /**
     * The envelope's time curve. {@link #apply(float)} maps normalized
     * progress {@code t ∈ [0, 1]} through the curve, endpoints exact.
     */
    public enum Easing {
        /** Constant speed. */
        linear {
            @Override
            public float apply(float t) {
                return t;
            }
        },
        /** Slow start, accelerating exit. */
        easeIn {
            @Override
            public float apply(float t) {
                return t * t;
            }
        },
        /** Fast start, decelerating into invisibility — the default feel. */
        easeOut {
            @Override
            public float apply(float t) {
                float u = 1 - t;
                return 1 - u * u;
            }
        },
        /** Slow at both ends. */
        easeInOut {
            @Override
            public float apply(float t) {
                return t < 0.5f ? 2 * t * t : 1 - 2 * (1 - t) * (1 - t);
            }
        };

        /** Maps clamped progress through the curve; {@code apply(0) = 0}, {@code apply(1) = 1}. */
        public abstract float apply(float t);

        private static float clamped(float t) {
            return Math.max(0f, Math.min(1f, t));
        }
    }

    /** The hard ceiling on any exit animation — zombie overlays must die young. */
    public static final int maxDurationMs = 300;

    /** The scale {@link Kind#fadeScale} shrinks to by the end of the exit. */
    public static final float endScale = 0.8f;

    /** The instant exit: 0 ms, no envelope. */
    public static final ExitAnimation none = new ExitAnimation(Kind.none, 0, Easing.linear);

    /** An alpha-only exit with the default easing. */
    public static ExitAnimation fade(int durationMs) {
        return fade(durationMs, Easing.easeOut);
    }

    /** An alpha-only exit. */
    public static ExitAnimation fade(int durationMs, Easing easing) {
        return new ExitAnimation(Kind.fade, durationMs, easing);
    }

    /** A fading exit with a slight rise, with the default easing. */
    public static ExitAnimation fadeRise(int durationMs) {
        return fadeRise(durationMs, Easing.easeOut);
    }

    /** A fading exit with a slight rise. */
    public static ExitAnimation fadeRise(int durationMs, Easing easing) {
        return new ExitAnimation(Kind.fadeRise, durationMs, easing);
    }

    /** A fading exit shrinking around its center, with the default easing. */
    public static ExitAnimation fadeScale(int durationMs) {
        return fadeScale(durationMs, Easing.easeOut);
    }

    /** A fading exit shrinking around its center. */
    public static ExitAnimation fadeScale(int durationMs, Easing easing) {
        return new ExitAnimation(Kind.fadeScale, durationMs, easing);
    }

    public ExitAnimation {
        if (kind == null) throw new IllegalArgumentException("kind must not be null");
        if (easing == null) easing = Easing.linear;
        if (kind == Kind.none || durationMs <= 0) {
            // a zero-duration animation is no animation — normalize to the
            // canonical instant exit so equality comparisons stay meaningful
            kind = Kind.none;
            durationMs = 0;
            easing = Easing.linear;
        } else if (durationMs > maxDurationMs) {
            durationMs = maxDurationMs;
        }
    }

    /** Whether this is the instant exit (0 ms, no envelope). */
    public boolean instant() {
        return kind == Kind.none;
    }

    /** The duration in seconds — the clock unit the envelope evaluates against. */
    public double durationSeconds() {
        return durationMs / 1000.0;
    }

    /**
     * Whether {@code elapsedSeconds} (on the driving clock) has reached the
     * end. Deliberately agrees with the envelope's float rounding: the last
     * sliver of a frame may round the eased progress to exactly 1 (alpha 0)
     * a hair before the double comparison crosses the duration — the exit
     * is over the moment the envelope reads spent, never one frame later.
     */
    public boolean finished(double elapsedSeconds) {
        return progress(elapsedSeconds) >= 1f;
    }

    /**
     * The alpha factor at {@code elapsedSeconds}: 1 at the start of the exit,
     * eased to 0 at the end. An instant exit is always 0 (already gone).
     */
    public float alphaAt(double elapsedSeconds) {
        if (instant()) return 0f;
        return 1f - progress(elapsedSeconds);
    }

    /**
     * The rise factor at {@code elapsedSeconds}: 0 → 1, the fraction of the
     * channel's rise travel. Always 0 outside {@link Kind#fadeRise}.
     */
    public float riseAt(double elapsedSeconds) {
        return kind == Kind.fadeRise ? progress(elapsedSeconds) : 0f;
    }

    /**
     * The scale factor at {@code elapsedSeconds}: 1 → {@link #endScale}.
     * Always 1 outside {@link Kind#fadeScale}.
     */
    public float scaleAt(double elapsedSeconds) {
        return kind == Kind.fadeScale ? 1f - (1f - endScale) * progress(elapsedSeconds) : 1f;
    }

    /** Eased progress with exact endpoints; 1 once the duration has elapsed. */
    private float progress(double elapsedSeconds) {
        double duration = durationSeconds();
        if (elapsedSeconds >= duration) return 1f; // covers the instant exit (duration 0) from t = 0 on
        if (elapsedSeconds <= 0) return 0f;
        return easing.apply(Easing.clamped((float) (elapsedSeconds / duration)));
    }
}
