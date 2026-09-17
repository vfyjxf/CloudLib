package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

/**
 * A critically damped 2D spring (damping ratio zeta = 1) with the exact
 * analytic step — the follow-and-recoil primitive for elements that track a
 * moving anchor or get pushed out of the way and ease back into place.
 * <p>
 * Each {@link #step(double, double, double)} advances position and velocity
 * along the closed-form solution of {@code x'' = omega^2 * (target - x) - 2 *
 * omega * x'} over {@code dt}, so for a fixed target the result depends only
 * on the total elapsed time: 240 steps of 1/240 s land on the same state as
 * 60 steps of 1/60 s. From rest the approach is monotone and never overshoots;
 * {@link #impulse(double, double)} adds a velocity kick (a shove from a
 * neighbor) that is carried and decayed by the same dynamics, and
 * {@link #snap(double, double)} teleports to a target with zero velocity.
 * <p>
 * Mutual exclusion with {@link FlipPlanner}: for one element, a discrete
 * rect change is animated by a FLIP while its tracking spring is
 * {@link #snap(double, double) snapped} to the flip target (discrete change
 * → FLIP + spring snap); continuous per-frame target motion is handled by the
 * spring alone (continuous change → spring). Never let both chase the same
 * target in the same frame — the flip would fight the spring.
 * <p>
 * {@code omega} is the undamped angular frequency in rad/s — the stiffness
 * knob. The rest-phase error decays as {@code (1 + omega * t) * e^(-omega *
 * t)}; when tracking a target moving at constant speed {@code v} the steady
 * state trails it by {@code 2 * v / omega}.
 * <p>
 * Positions are in pixels, time in seconds; dt is always an explicit
 * parameter, never a wall clock.
 */
public final class Spring2 {

    private final double omega;

    private double x;
    private double y;
    private double vx;
    private double vy;

    /** A spring at rest at the origin. */
    public Spring2(double omega) {
        this(omega, 0.0, 0.0);
    }

    /** A spring at rest at {@code (x, y)}. */
    public Spring2(double omega, double x, double y) {
        if (!Double.isFinite(omega) || omega <= 0.0) {
            throw new IllegalArgumentException("omega must be finite and positive: " + omega);
        }
        requireFinite(x, "x");
        requireFinite(y, "y");
        this.omega = omega;
        this.x = x;
        this.y = y;
    }

    /**
     * Advances the spring toward {@code (targetX, targetY)} over
     * {@code dtSeconds} of exact critically damped motion.
     * <p>
     * {@code dtSeconds} must be finite; a negative value (a rewound frame
     * clock) is defensively clamped to zero — the spring simply does not move
     * that frame — rather than rejected.
     *
     * @return this, for chaining
     */
    public Spring2 step(double targetX, double targetY, double dtSeconds) {
        requireFinite(targetX, "targetX");
        requireFinite(targetY, "targetY");
        double dt = requireDt(dtSeconds);
        if (dt == 0.0) {
            return this;
        }
        double decay = Math.exp(-omega * dt);
        double ax = x - targetX;
        double bx = vx + omega * ax;
        double cx = ax + bx * dt;
        double ay = y - targetY;
        double by = vy + omega * ay;
        double cy = ay + by * dt;
        this.x = targetX + cx * decay;
        this.vx = (bx - omega * cx) * decay;
        this.y = targetY + cy * decay;
        this.vy = (by - omega * cy) * decay;
        return this;
    }

    /**
     * Adds a velocity kick — a shove that the spring then carries and decays.
     *
     * @return this, for chaining
     */
    public Spring2 impulse(double dvx, double dvy) {
        requireFinite(dvx, "dvx");
        requireFinite(dvy, "dvy");
        this.vx += dvx;
        this.vy += dvy;
        return this;
    }

    /**
     * Teleports to {@code (x, y)} with zero velocity — the partner of a FLIP
     * on a discrete change, and the reset for a teleporting anchor.
     *
     * @return this, for chaining
     */
    public Spring2 snap(double x, double y) {
        requireFinite(x, "x");
        requireFinite(y, "y");
        this.x = x;
        this.y = y;
        this.vx = 0.0;
        this.vy = 0.0;
        return this;
    }

    /**
     * Displaces the state by {@code (dx, dy)} — the position moves, the
     * velocity rides along, and the target is untouched: an external push
     * (a neighbor shoving the element out of an overlap) relocates it without
     * discarding the motion it already had, so it keeps easing toward the
     * same target from where it was actually pushed to.
     *
     * @return this, for chaining
     */
    public Spring2 translate(double dx, double dy) {
        requireFinite(dx, "dx");
        requireFinite(dy, "dy");
        this.x += dx;
        this.y += dy;
        return this;
    }

    public double x() {
        return x;
    }

    public double y() {
        return y;
    }

    public double velocityX() {
        return vx;
    }

    public double velocityY() {
        return vy;
    }

    public double omega() {
        return omega;
    }

    private static double requireDt(double dtSeconds) {
        if (!Double.isFinite(dtSeconds)) {
            throw new IllegalArgumentException("dtSeconds must be finite: " + dtSeconds);
        }
        return Math.max(0.0, dtSeconds);
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite: " + value);
        }
    }
}
