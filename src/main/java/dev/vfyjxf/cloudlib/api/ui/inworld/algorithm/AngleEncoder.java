package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.inworld.OffscreenProjector;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.Smoothing;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.SwitchGate;

/**
 * The ANGLE encoding for off-screen indicators (Lin et al.'s user-tested
 * variant, §3.0): the indicator slides along <em>all four</em> screen edges —
 * its position around the perimeter is the target's direction angle. A target
 * circling the camera drags the indicator smoothly around the whole
 * rectangle, across corners included, with no teleport anywhere; the edge
 * landing point is the ray from the screen center at the smoothed angle,
 * exactly the construction {@link OffscreenProjector} uses, so the two never
 * disagree.
 * <p>
 * Two stabilizers sit on top of the geometry. The angle is smoothed with
 * {@link Smoothing#dampAngle} — frame-rate-independent exponential damping on
 * the shortest arc — so the indicator glides instead of snapping. The
 * reported edge goes through a {@link SwitchGate} keyed on the (unwrapped)
 * smoothed angle: consumers that orient an arrow or swap skins by edge see a
 * hysteresis band plus dwell ticks around the corner-crossing point, so a
 * target hovering near a diagonal cannot flip the edge back and forth. The
 * position itself is never gated — it comes straight from the smoothed angle
 * and is continuous by construction.
 * <p>
 * Behind-the-camera targets need no special case: the projector's direction
 * is orthogonal-projection based and stays valid there, and this encoder only
 * consumes that direction. On-screen results are reported with
 * {@code active = false} — where to park or hide a live indicator is the
 * caller's policy.
 */
public final class AngleEncoder {

    private static final double degenerateEpsilon = 1.0e-9;
    private static final double minHalfExtent = 1.0;

    /**
     * @param lambda the angle smoothing rate in 1/s (see
     *        {@link Smoothing#dampAngle}); positive
     * @param edgeBand the edge-switch hysteresis band in radians — after an
     *        edge commit, the smoothed angle must move this far before
     *        another switch may commit; positive
     * @param edgeDwellTicks how many consecutive updates a candidate edge
     *        must survive before committing; at least 1
     */
    public record Config(double lambda, double edgeBand, int edgeDwellTicks) {

        public Config {
            if (!Double.isFinite(lambda) || lambda <= 0) {
                throw new IllegalArgumentException("lambda must be finite and positive: " + lambda);
            }
            if (!Double.isFinite(edgeBand) || edgeBand <= 0) {
                throw new IllegalArgumentException("edgeBand must be finite and positive: " + edgeBand);
            }
            if (edgeDwellTicks < 1) {
                throw new IllegalArgumentException("edgeDwellTicks must be at least 1: " + edgeDwellTicks);
            }
        }

        public static Config of(double lambda, double edgeBand, int edgeDwellTicks) {
            return new Config(lambda, edgeBand, edgeDwellTicks);
        }
    }

    /**
     * One encoder frame. {@code position} is the indicator's landing point on
     * the margin-inset rectangle; {@code edge} the gated edge it is reported
     * on; {@code angle} the raw target angle from the projector;
     * {@code smoothedAngle} the encoder's internal (possibly unwrapped)
     * smoothed angle; {@code active} whether the target is actually
     * off-screen.
     */
    public record Output(
            boolean active, FloatPos position, OffscreenProjector.Edge edge, double angle, double smoothedAngle) {}

    private final double screenWidth;
    private final double screenHeight;
    private final double insetMargin;
    private final Config config;
    private final SwitchGate<OffscreenProjector.Edge> edgeGate;

    private double smoothedAngle;

    /**
     * @throws IllegalArgumentException if screen dimensions are not positive
     *         or the inset margin is negative (see {@link Config} for the
     *         config validation)
     */
    public AngleEncoder(double screenWidth, double screenHeight, double insetMargin, Config config) {
        if (screenWidth <= 0 || !Double.isFinite(screenWidth)) {
            throw new IllegalArgumentException("screenWidth must be finite and positive: " + screenWidth);
        }
        if (screenHeight <= 0 || !Double.isFinite(screenHeight)) {
            throw new IllegalArgumentException("screenHeight must be finite and positive: " + screenHeight);
        }
        if (insetMargin < 0 || !Double.isFinite(insetMargin)) {
            throw new IllegalArgumentException("insetMargin must be finite and non-negative: " + insetMargin);
        }
        this.screenWidth = screenWidth;
        this.screenHeight = screenHeight;
        this.insetMargin = insetMargin;
        this.config = config;
        this.edgeGate = new SwitchGate<>(
                new SwitchGate.Config(config.edgeBand(), config.edgeDwellTicks(), 0.0, 0),
                OffscreenProjector.Edge.right,
                0.0);
    }

    public double screenWidth() {
        return screenWidth;
    }

    public double screenHeight() {
        return screenHeight;
    }

    /**
     * One frame: consumes a projector result and the frame's dt.
     *
     * @param dtSeconds the elapsed time of this frame; {@code 0} re-queries
     *        without smoothing
     */
    public Output update(OffscreenProjector.Result result, double dtSeconds) {
        smoothedAngle = Smoothing.dampAngle(smoothedAngle, result.angle(), config.lambda(), dtSeconds);

        double centerX = screenWidth * 0.5;
        double centerY = screenHeight * 0.5;
        double halfW = Math.max(screenWidth * 0.5 - insetMargin, minHalfExtent);
        double halfH = Math.max(screenHeight * 0.5 - insetMargin, minHalfExtent);
        double dx = Math.cos(smoothedAngle);
        double dy = Math.sin(smoothedAngle);
        double tx = Math.abs(dx) < degenerateEpsilon ? Double.POSITIVE_INFINITY : halfW / Math.abs(dx);
        double ty = Math.abs(dy) < degenerateEpsilon ? Double.POSITIVE_INFINITY : halfH / Math.abs(dy);
        double t = Math.min(tx, ty);
        FloatPos position = new FloatPos(centerX + dx * t, centerY + dy * t);

        OffscreenProjector.Edge desired = tx <= ty
                ? (dx < 0 ? OffscreenProjector.Edge.left : OffscreenProjector.Edge.right)
                : (dy < 0 ? OffscreenProjector.Edge.top : OffscreenProjector.Edge.bottom);
        edgeGate.propose(desired, smoothedAngle);

        return new Output(!result.onScreen(), position, edgeGate.current(), result.angle(), smoothedAngle);
    }

    /**
     * Snaps the smoothed angle to {@code angle} (any branch) — initializing a
     * fresh target or re-seizing a teleported one without the glide.
     */
    public void snap(double angle) {
        if (!Double.isFinite(angle)) {
            throw new IllegalArgumentException("angle must be finite: " + angle);
        }
        smoothedAngle = angle;
    }

    /** The current smoothed angle (possibly on an unwrapped branch). */
    public double smoothedAngle() {
        return smoothedAngle;
    }
}
