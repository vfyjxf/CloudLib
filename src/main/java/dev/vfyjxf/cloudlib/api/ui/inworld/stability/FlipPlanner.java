package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

import dev.vfyjxf.cloudlib.api.math.FloatRect;

import java.util.Objects;

/**
 * Constant-speed FLIP morphs for discrete rect changes: when a layout
 * decision moves an element to a different rect, the planner records the
 * current visual rect and animates toward the new target over
 * {@code clamp(travelDistance / speed, minDuration, maxDuration)} seconds —
 * by default 120–250 ms. The animation is a pure function of absolute time
 * (every query takes the time as an explicit parameter; no wall clock, no
 * per-frame dt), so the visual is identical no matter how the frames sample
 * it — including non-monotonic sample times, which are tolerated rather than
 * rejected (sampling is pure; a rewound {@code flipTo} time simply re-anchors
 * the leg at that time) — and retargeting mid-flight simply re-plans from the
 * <em>current visual rect</em> as the new origin, so the morph is continuous
 * across the interruption and never jumps.
 * <p>
 * Position and size interpolate linearly (constant speed along the straight
 * center path; the duration is derived from center travel distance, so a
 * pure resize still morphs over the minimum duration).
 * <p>
 * Mutual exclusion with {@link Spring2}: for one element, a discrete rect
 * change is animated by a FLIP while its tracking spring is
 * {@link Spring2#snap(double, double) snapped} to the flip target (discrete
 * change → FLIP + spring snap); continuous per-frame target motion is
 * handled by the spring alone (continuous change → spring). Never let both
 * chase the same target in the same frame — the flip would fight the spring.
 * <p>
 * Distances are in pixels, durations in seconds.
 */
public final class FlipPlanner {

    public static final double defaultMinDurationSeconds = 0.120;
    public static final double defaultMaxDurationSeconds = 0.250;

    private final double speedPixelsPerSecond;
    private final double minDurationSeconds;
    private final double maxDurationSeconds;

    private FloatRect target;
    private FloatRect origin;
    private double originTimeSeconds = Double.NaN;
    private double durationSeconds;

    /** A planner resting on {@code placedRect}, with the default 120–250 ms duration clamp. */
    public FlipPlanner(double speedPixelsPerSecond, FloatRect placedRect) {
        this(speedPixelsPerSecond, placedRect, defaultMinDurationSeconds, defaultMaxDurationSeconds);
    }

    /** A planner resting on {@code placedRect}, with an explicit duration clamp. */
    public FlipPlanner(
            double speedPixelsPerSecond, FloatRect placedRect, double minDurationSeconds, double maxDurationSeconds) {
        if (!Double.isFinite(speedPixelsPerSecond) || speedPixelsPerSecond <= 0.0) {
            throw new IllegalArgumentException(
                    "speedPixelsPerSecond must be finite and positive: " + speedPixelsPerSecond);
        }
        if (!Double.isFinite(minDurationSeconds)
                || minDurationSeconds <= 0.0
                || !Double.isFinite(maxDurationSeconds)
                || maxDurationSeconds < minDurationSeconds) {
            throw new IllegalArgumentException("duration clamp must satisfy 0 < minDuration <= maxDuration: "
                    + minDurationSeconds + ".." + maxDurationSeconds);
        }
        this.speedPixelsPerSecond = speedPixelsPerSecond;
        this.minDurationSeconds = minDurationSeconds;
        this.maxDurationSeconds = maxDurationSeconds;
        this.target = Objects.requireNonNull(placedRect, "placedRect");
        this.origin = placedRect;
    }

    /**
     * Starts (or re-plans) the morph to {@code newTarget}: the origin becomes
     * the visual rect at {@code nowSeconds} — the current mid-flight position
     * when interrupted, the resting rect when idle — and the duration is
     * re-derived from the remaining travel. A no-op when already committed to
     * {@code newTarget}; settles immediately when the target equals the
     * current visual.
     *
     * @return this, for chaining
     */
    public FlipPlanner flipTo(FloatRect newTarget, double nowSeconds) {
        Objects.requireNonNull(newTarget, "newTarget");
        requireTime(nowSeconds);
        if (newTarget.equals(target)) {
            return this;
        }
        FloatRect from = visual(nowSeconds);
        if (newTarget.equals(from)) {
            settle(newTarget);
            return this;
        }
        this.origin = from;
        this.originTimeSeconds = nowSeconds;
        this.durationSeconds =
                clamp(travelDistance(from, newTarget) / speedPixelsPerSecond, minDurationSeconds, maxDurationSeconds);
        this.target = newTarget;
        return this;
    }

    /**
     * Cancels any in-flight morph and rests on {@code newRect} — the discrete
     * jump counterpart of {@code flipTo}.
     *
     * @return this, for chaining
     */
    public FlipPlanner snap(FloatRect newRect) {
        Objects.requireNonNull(newRect, "newRect");
        settle(newRect);
        return this;
    }

    /**
     * Shifts the whole morph — origin and target alike — by {@code (dx, dy)}:
     * an external push (a neighbor shoving the visual out of an overlap) moves
     * the element without changing its phase, so the visual at any sample time
     * moves by exactly the same delta and the remaining travel, duration and
     * progress are untouched. Equivalent to re-planning from the displaced
     * visual toward the displaced target; {@link #snap} is the resting-rect
     * counterpart.
     *
     * @return this, for chaining
     */
    public FlipPlanner translate(double dx, double dy) {
        if (!Double.isFinite(dx) || !Double.isFinite(dy)) {
            throw new IllegalArgumentException("translate deltas must be finite: " + dx + ", " + dy);
        }
        this.origin = origin.translate(dx, dy);
        this.target = target.translate(dx, dy);
        return this;
    }

    /**
     * The visual rect at {@code nowSeconds} — the interpolated morph while
     * animating, the target once at rest. Pure: query order does not matter.
     */
    public FloatRect visual(double nowSeconds) {
        requireTime(nowSeconds);
        if (!isAnimating(nowSeconds)) {
            return target;
        }
        double progress = clamp01((nowSeconds - originTimeSeconds) / durationSeconds);
        return new FloatRect(
                origin.x() + (target.x() - origin.x()) * progress,
                origin.y() + (target.y() - origin.y()) * progress,
                origin.width() + (target.width() - origin.width()) * progress,
                origin.height() + (target.height() - origin.height()) * progress);
    }

    public boolean isAnimating(double nowSeconds) {
        requireTime(nowSeconds);
        return !Double.isNaN(originTimeSeconds) && nowSeconds < originTimeSeconds + durationSeconds;
    }

    /** The committed target rect. */
    public FloatRect target() {
        return target;
    }

    private void settle(FloatRect rect) {
        this.target = rect;
        this.origin = rect;
        this.originTimeSeconds = Double.NaN;
        this.durationSeconds = 0.0;
    }

    private static double travelDistance(FloatRect from, FloatRect to) {
        return Math.hypot(to.centerX() - from.centerX(), to.centerY() - from.centerY());
    }

    private static double clamp(double value, double min, double max) {
        return Math.min(max, Math.max(min, value));
    }

    private static double clamp01(double value) {
        return clamp(value, 0.0, 1.0);
    }

    private static void requireTime(double nowSeconds) {
        if (!Double.isFinite(nowSeconds)) {
            throw new IllegalArgumentException("nowSeconds must be finite: " + nowSeconds);
        }
    }
}
