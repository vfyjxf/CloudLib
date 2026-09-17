package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

/**
 * Frame-rate-independent exponential smoothing — the continuous layer's
 * workhorse for scalars that must glide toward a target instead of snapping.
 * <p>
 * {@code damp(cur, target, lambda, dt) = lerp(cur, target, 1 - exp(-lambda * dt))}
 * closes the same fraction {@code 1 - exp(-lambda * dt)} of the remaining gap
 * per step, so the result depends only on {@code lambda} and the total elapsed
 * time — never on how that time was partitioned into frames. This is why the
 * deliberately frame-rate-dependent form {@code lerp(a, b, r * dt)} is not (and
 * must not be) part of this API: composing it across a different frame rate
 * changes the trajectory.
 * <p>
 * {@link #dampHalfLife(double, double, double, double)} re-parameterizes the
 * same curve with the half-life (the time after which half the remaining
 * distance is closed), which is usually easier to reason about; {@code lambda
 * = ln(2) / halfLife}. {@link #dampAngle(double, double, double, double)}
 * applies the same damping on the shortest arc between two angles.
 * <p>
 * All times are seconds; angles are radians.
 */
public final class Smoothing {

    private static final double ln2 = Math.log(2.0);
    private static final double twoPi = 2.0 * Math.PI;

    private Smoothing() {}

    /**
     * Moves {@code current} toward {@code target} by the fraction
     * {@code 1 - exp(-lambda * dtSeconds)} of the remaining distance.
     * <p>
     * {@code dtSeconds} must be finite; a negative value (a rewound frame
     * clock) is defensively clamped to zero — a zero-length step returning
     * {@code current} — rather than rejected, so clock glitches upstream
     * cannot crash the render thread.
     *
     * @param lambda the rate constant in 1/s; larger closes faster
     * @return a value in the closed interval between {@code current} and {@code target}
     */
    public static double damp(double current, double target, double lambda, double dtSeconds) {
        requireRate(lambda);
        double dt = requireDt(dtSeconds);
        return current + (target - current) * (1.0 - Math.exp(-lambda * dt));
    }

    /**
     * {@link #damp(double, double, double, double)} parameterized by half-life:
     * after one {@code halfLifeSeconds} of elapsed time, half the remaining
     * distance to the target has been closed.
     */
    public static double dampHalfLife(double current, double target, double halfLifeSeconds, double dtSeconds) {
        if (!Double.isFinite(halfLifeSeconds) || halfLifeSeconds <= 0.0) {
            throw new IllegalArgumentException("halfLifeSeconds must be finite and positive: " + halfLifeSeconds);
        }
        return damp(current, target, ln2 / halfLifeSeconds, dtSeconds);
    }

    /**
     * {@link #damp(double, double, double, double)} over the shortest arc
     * between two angles: the angular difference is wrapped into
     * {@code [-pi, pi]} before damping, so {@code 350°} glides up to
     * {@code 10°} across the wrap instead of taking the 340° detour.
     * <p>
     * The output stays on {@code currentRadians}' branch (it may exceed
     * {@code ±pi} or {@code ±2pi}); wrap for display purposes only.
     */
    public static double dampAngle(double currentRadians, double targetRadians, double lambda, double dtSeconds) {
        requireRate(lambda);
        double dt = requireDt(dtSeconds);
        double delta = shortestArc(targetRadians - currentRadians);
        return currentRadians + delta * (1.0 - Math.exp(-lambda * dt));
    }

    private static double shortestArc(double deltaRadians) {
        double wrapped = deltaRadians % twoPi;
        if (wrapped < -Math.PI) {
            wrapped += twoPi;
        } else if (wrapped > Math.PI) {
            wrapped -= twoPi;
        }
        return wrapped;
    }

    private static void requireRate(double lambda) {
        if (!Double.isFinite(lambda) || lambda <= 0.0) {
            throw new IllegalArgumentException("lambda must be finite and positive: " + lambda);
        }
    }

    /**
     * Validates a frame dt: finite is required (NaN/∞ is corruption, not a
     * clock artifact), while negative values are clamped to zero — the
     * defensive treatment of a rewound upstream clock.
     *
     * @return the clamped dt
     */
    private static double requireDt(double dtSeconds) {
        if (!Double.isFinite(dtSeconds)) {
            throw new IllegalArgumentException("dtSeconds must be finite: " + dtSeconds);
        }
        return Math.max(0.0, dtSeconds);
    }
}
