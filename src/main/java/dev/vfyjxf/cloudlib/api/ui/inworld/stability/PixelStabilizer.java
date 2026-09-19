package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

import java.util.Objects;

/**
 * The pixel-space stabilizer behind a follow panel: a chatter-proof lattice
 * latch whose snap strength fades continuously with sustained motion, so a
 * panel that is <em>almost</em> stationary renders on a locked integer
 * lattice while a panel that is genuinely moving renders sub-pixel accurate —
 * with no seam between the two regimes. This is the survey-synthesis answer
 * to "static text shimmers, moving text smears", finished with the
 * continuous-deadzone pass: the earlier hard rest/move two-state left a 1 px
 * single-frame step at every hysteresis crossing and a pop of up to the full
 * dead-zone trail at the rest→move handoff — the residual micro-jitter.
 * <p>
 * <strong>The latch.</strong> Each axis keeps {@code snapped}, a lattice
 * position that only steps when the float input leaves the band
 * {@code [snapped − (snapHalf + hysteresis), snapped + (snapHalf +
 * hysteresis)]} — with the defaults a 1.5 px dead zone straddling every
 * half-pixel boundary. A signal oscillating near {@code k + 0.5} cannot flip
 * the latch at all: sensor chatter is absorbed exactly, and the quantized
 * value is never fed back — the state machine only ever reads the float
 * inputs, so quantization error cannot accumulate or drive the speed
 * estimate.
 * <p>
 * <strong>The continuous deadzone.</strong> The output is
 * {@code out = in + s · c} per axis, where {@code c} is the <em>correction</em>
 * — a low-passed {@code snapped − in}, the pull onto the lattice — and
 * {@code s ∈ [0, 1]} is the snap weight. At rest {@code s = 1} and the
 * correction is un-filtered, so {@code out = snapped} exactly: a parked panel
 * is bit-for-bit still on the lattice. Sustained motion fades {@code s}
 * toward 0 — the panel blends into exact float passthrough — and every latch
 * step eases through the correction's glide instead of teleporting. Three
 * properties make the blend jerk-free:
 * <ul>
 *   <li><strong>s is a function of latch-step density, not raw speed.</strong>
 *       Each latch step pulses a density target that then decays; the weight
 *       follows that target through a first-order rise. Zero-mean chatter
 *       never steps the latch, so it never lowers {@code s} — the dead-still
 *       contract survives a noisy anchor at any speed below the move gate.
 *       Conversely any drift that actually crosses cells steps the latch, so
 *       even a 0.5 px/s creep blends toward passthrough — no speed threshold
 *       separates "noise" from "drift" because the latch already does.</li>
 *   <li><strong>The correction is time-continuous.</strong> Its low-pass time
 *       constant is armed by recent latch steps and decays to zero otherwise;
 *       a lattice crossing therefore slides the output across the cell over
 *       roughly {@code settleSeconds} instead of stepping it a whole pixel in
 *       one frame, and a quiet panel converges back to the exact lattice.</li>
 *   <li><strong>The move phase is exact, by state.</strong> While the
 *       smoothed speed is above {@code moveVelocity} the output <em>is</em>
 *       the input, bit-exact — the rigid-tracking contract; the correction is
 *       held at zero there, so leaving the move phase starts the landing from
 *       the input itself, with no pop and no velocity-proportional lag ever
 *       (the OneEuro lesson: smoothing lives only in the rest domain).</li>
 * </ul>
 * The measured consequences (frame-sequence harness): a sweep across the
 * 9–45 px/s band stays within 0.5 px of its float target with no more than
 * 0.06 px of beyond-input motion at the state switches; a 0.5 px/s drift
 * crosses cells in ≤ 0.25 px frame steps; the deviation from the float
 * target is monotonically non-increasing in speed.
 * <p>
 * <strong>The settle.</strong> When the speed stays below
 * {@code restVelocity} for {@code dwellFrames} consecutive frames, the
 * landing begins: the latch recenters once onto the nearest cell (the
 * glide carries it — the old quantizer's ease-out-from-a-frozen-start is
 * gone, so micro-motion during the landing no longer stutters), and the
 * state returns to rest once the glide has converged and the snap weight is
 * back. If the input speeds up past {@code moveVelocity} mid-landing, the
 * landing aborts and exact passthrough resumes immediately.
 * <p>
 * <strong>Escape hatch.</strong> {@link #snapBypass(double, double, double)}
 * is the mechanism half of the survey's escape gate: the <em>caller</em>
 * judges the trigger (tracking error above ~4% of screen height, or camera
 * angular speed above ~100°/s — this class has no notion of screen size or
 * cameras) and calls bypass to re-stick the panel at a fresh position with
 * all history dropped, skipping both the glide and any phantom velocity.
 * <p>
 * Parameter provenance (survey synthesis): {@code snapHalf = 0.5} is the
 * nearest-pixel half cell; {@code hysteresis = 0.25} widens the boundary dead
 * zone until ±0.3 px of sensor noise is fully absorbed; {@code moveVelocity
 * = 45 px/s} / {@code restVelocity = 9 px/s} (a 5:1 ratio) straddle the
 * perceptual band where screen-space motion stops reading as rest;
 * {@code dwellFrames = 5} (~83 ms at 60 Hz) sits inside the 80–100 ms fusion
 * window so a brief stop does not trigger a landing; {@code settleSeconds =
 * 0.12} now scales the continuous layer's whole time base — the lattice
 * crossing glide, the step-density decay and the arm window that re-locks a
 * quiet panel. Positions are device pixels; time is seconds from the
 * caller's clock. A call whose {@code t} does not advance is ignored: state
 * and output unchanged.
 */
public final class PixelStabilizer {

    /** Exponential factor of the smoothed-speed estimate. */
    private static final double velocitySmoothing = 0.3;

    /** The glide arm window after a latch step, in multiples of {@code settleSeconds}. */
    private static final double glideArmFactor = 2.5;

    /** The step-density target decay, in multiples of {@code settleSeconds}. */
    private static final double densityDecayFactor = 1.5;

    /** The step-density rise time constant, in multiples of {@code settleSeconds}. */
    private static final double densityRiseFactor = 0.5;

    /** The density-target deadband: below this the target is exactly zero (bit-exact rest). */
    private static final double densityZero = 0.02;

    /** The settle recenter threshold: {@code snapHalf + hysteresis/2} (chatter near a half-cell never triggers it). */
    private static final double recenterMarginFactor = 0.5;

    /** The motion phases of the stabilizer, in cycle {@code rest → move → settle → rest}. */
    public enum State {

        /** Latched toward the integer lattice; the snap weight is (re)forming. */
        rest,

        /** Exact sub-pixel passthrough of the float input. */
        move,

        /** Landing after motion: the correction glides the output onto the lattice. */
        settle
    }

    /** One stabilized sample: the output position the caller should render. */
    public record Output(double x, double y) {}

    /**
     * The six knobs; units are pixels, pixels/second and seconds.
     *
     * @param snapHalf half the quantization cell, in pixels; positive
     * @param hysteresis the extra dead zone added on each side of
     *        {@code snapHalf}; non-negative, and {@code snapHalf +
     *        hysteresis} must stay below 1 so adjacent hysteresis bands
     *        overlap instead of fighting
     * @param moveVelocity the smoothed speed above which the panel renders
     *        exact sub-pixel passthrough (move phase); positive, strictly
     *        above {@code restVelocity}
     * @param restVelocity the smoothed speed below which dwell toward a
     *        landing accumulates; non-negative
     * @param dwellFrames consecutive slow frames required before the settle
     *        landing begins; at least 1
     * @param settleSeconds the continuous layer's time base — the lattice
     *        crossing glide, the step-density decay and the arm window; 0
     *        degenerates to the pure latch (instant crossings, instant rest)
     */
    public record Config(
            double snapHalf,
            double hysteresis,
            double moveVelocity,
            double restVelocity,
            int dwellFrames,
            double settleSeconds) {

        /** The survey-synthesis defaults: {@code 0.5, 0.25, 45, 9, 5, 0.12}. */
        public static Config ofDefaults() {
            return new Config(0.5, 0.25, 45.0, 9.0, 5, 0.12);
        }

        public Config {
            if (!Double.isFinite(snapHalf) || snapHalf <= 0.0) {
                throw new IllegalArgumentException("snapHalf must be finite and positive: " + snapHalf);
            }
            if (!Double.isFinite(hysteresis) || hysteresis < 0.0) {
                throw new IllegalArgumentException("hysteresis must be finite and non-negative: " + hysteresis);
            }
            if (snapHalf + hysteresis >= 1.0) {
                throw new IllegalArgumentException(
                        "snapHalf + hysteresis must stay below 1 so adjacent hysteresis bands overlap: "
                                + (snapHalf + hysteresis));
            }
            if (!Double.isFinite(moveVelocity) || moveVelocity <= 0.0) {
                throw new IllegalArgumentException("moveVelocity must be finite and positive: " + moveVelocity);
            }
            if (!Double.isFinite(restVelocity) || restVelocity < 0.0) {
                throw new IllegalArgumentException("restVelocity must be finite and non-negative: " + restVelocity);
            }
            if (moveVelocity <= restVelocity) {
                throw new IllegalArgumentException(
                        "moveVelocity must be strictly above restVelocity: " + moveVelocity + " <= " + restVelocity);
            }
            if (dwellFrames < 1) {
                throw new IllegalArgumentException("dwellFrames must be at least 1: " + dwellFrames);
            }
            if (!Double.isFinite(settleSeconds) || settleSeconds < 0.0) {
                throw new IllegalArgumentException("settleSeconds must be finite and non-negative: " + settleSeconds);
            }
        }
    }

    private final Config config;

    private boolean started;
    private double tPrev;

    private State state = State.rest;
    private double smoothedSpeed;
    private int dwell;

    private double prevInputX;
    private double prevInputY;
    private double snappedX;
    private double snappedY;
    private double outX;
    private double outY;

    // the continuous deadzone: per-axis eased correction, shared step density
    private double corrX;
    private double corrY;
    private double density;
    private double densityTarget;
    private double tLastLatchStep = Double.NEGATIVE_INFINITY;

    /** A stabilizer on the survey-synthesis defaults. */
    public PixelStabilizer() {
        this(Config.ofDefaults());
    }

    /** @param config the knob set; see {@link Config} */
    public PixelStabilizer(Config config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * One frame: the float input position {@code (x, y)} at absolute time
     * {@code tSeconds}.
     *
     * @return the position to render — the latched lattice position at rest,
     *         the float input itself in move, the eased blend between them
     *         while motion ramps up or down
     */
    public Output accept(double tSeconds, double x, double y) {
        requireFinite(tSeconds, "tSeconds");
        requireFinite(x, "x");
        requireFinite(y, "y");
        if (!started) {
            started = true;
            tPrev = tSeconds;
            prevInputX = x;
            prevInputY = y;
            smoothedSpeed = 0.0;
            snappedX = Math.round(x);
            snappedY = Math.round(y);
            corrX = snappedX - x;
            corrY = snappedY - y;
            outX = snappedX;
            outY = snappedY;
            return new Output(outX, outY);
        }
        double dt = tSeconds - tPrev;
        if (dt <= 0.0) {
            return new Output(outX, outY);
        }
        tPrev = tSeconds;

        double speed = Math.hypot(x - prevInputX, y - prevInputY) / dt;
        smoothedSpeed += velocitySmoothing * (speed - smoothedSpeed);
        prevInputX = x;
        prevInputY = y;

        stepStateMachine(x, y);

        double nX = hysteresisStep(snappedX, x);
        double nY = hysteresisStep(snappedY, y);
        boolean stepped = nX != snappedX || nY != snappedY;
        snappedX = nX;
        snappedY = nY;

        if (stepped) {
            densityTarget = 1.0;
            tLastLatchStep = tSeconds;
        }
        densityTarget *= Math.exp(-dt / (densityDecayFactor * config.settleSeconds() + 1.0e-9));
        if (densityTarget < densityZero) densityTarget = 0.0;
        density += (densityTarget - density) * (dt / (densityRiseFactor * config.settleSeconds() + dt));
        if (density < densityZero) density = 0.0;

        double arm =
                Math.max(0.0, 1.0 - (tSeconds - tLastLatchStep) / (glideArmFactor * config.settleSeconds() + 1.0e-9));
        updateCorrection(arm, dt, x, y);

        if (state == State.settle && arm <= 0.0 && density <= 0.1) {
            // the landing is complete: force the snap weight back so rest is bit-exact
            density = 0.0;
            state = State.rest;
        }

        double s = state == State.move ? 0.0 : 1.0 - density;
        if (s >= 1.0 && corrX == snappedX - x && corrY == snappedY - y) {
            outX = snappedX;
            outY = snappedY;
        } else {
            outX = x + s * corrX;
            outY = y + s * corrY;
        }
        return new Output(outX, outY);
    }

    /** The phase transitions; thresholds and dwell semantics are the original survey set. */
    private void stepStateMachine(double x, double y) {
        switch (state) {
            case rest -> {
                if (smoothedSpeed > config.moveVelocity()) {
                    state = State.move;
                    dwell = 0;
                }
            }
            case move -> {
                if (smoothedSpeed < config.restVelocity()) {
                    dwell++;
                } else {
                    dwell = 0;
                }
                if (dwell >= config.dwellFrames()) {
                    state = State.settle;
                    dwell = 0;
                    recenterOntoNearestCell(x, y);
                }
            }
            case settle -> {
                if (smoothedSpeed > config.moveVelocity()) {
                    state = State.move;
                    dwell = 0;
                }
            }
        }
    }

    /**
     * The settle entry's one-shot recenter: a panel that stopped mid-cell
     * lands on the <em>nearest</em> lattice cell (the old quantizer's landing
     * semantics). The glide carries the move; the threshold sits inside the
     * hysteresis cushion so chatter parked near a half-cell never triggers
     * it.
     */
    private void recenterOntoNearestCell(double x, double y) {
        double threshold = config.snapHalf() + config.hysteresis() * recenterMarginFactor;
        boolean recentered = false;
        if (Math.abs(x - snappedX) > threshold) {
            snappedX = Math.round(x);
            recentered = true;
        }
        if (Math.abs(y - snappedY) > threshold) {
            snappedY = Math.round(y);
            recentered = true;
        }
        if (recentered) {
            tLastLatchStep = tPrev; // arm the glide so the recenter slides
        }
    }

    /**
     * The eased correction toward the latch. In move the output ignores it
     * (exact passthrough), so it is pinned to zero there — leaving the move
     * phase then starts the landing from the input itself, with no pop. With
     * no recent latch step the arm is gone and the correction is the raw
     * pull, so a quiet panel converges to the bit-exact lattice.
     */
    private void updateCorrection(double arm, double dt, double x, double y) {
        if (state == State.move) {
            corrX = 0.0;
            corrY = 0.0;
            return;
        }
        if (arm <= 0.0) {
            corrX = snappedX - x;
            corrY = snappedY - y;
            return;
        }
        double alpha = dt / (config.settleSeconds() * arm + dt);
        corrX += (snappedX - x - corrX) * alpha;
        corrY += (snappedY - y - corrY) * alpha;
    }

    /**
     * The escape hatch: drops all history and sticks the panel directly at
     * {@code (x, y)} — quantized onto the lattice, rest phase, zeroed speed
     * estimate. The caller owns the trigger condition; see the class javadoc
     * for the survey's thresholds.
     *
     * @return the re-stuck output position
     */
    public Output snapBypass(double tSeconds, double x, double y) {
        requireFinite(tSeconds, "tSeconds");
        requireFinite(x, "x");
        requireFinite(y, "y");
        started = true;
        tPrev = tSeconds;
        state = State.rest;
        smoothedSpeed = 0.0;
        dwell = 0;
        prevInputX = x;
        prevInputY = y;
        snappedX = Math.round(x);
        snappedY = Math.round(y);
        corrX = snappedX - x;
        corrY = snappedY - y;
        density = 0.0;
        densityTarget = 0.0;
        tLastLatchStep = Double.NEGATIVE_INFINITY;
        outX = snappedX;
        outY = snappedY;
        return new Output(outX, outY);
    }

    /** Forgets everything; the next {@link #accept} re-seeds from scratch. */
    public void reset() {
        started = false;
        tPrev = 0.0;
        state = State.rest;
        smoothedSpeed = 0.0;
        dwell = 0;
        prevInputX = 0.0;
        prevInputY = 0.0;
        snappedX = 0.0;
        snappedY = 0.0;
        outX = 0.0;
        outY = 0.0;
        corrX = 0.0;
        corrY = 0.0;
        density = 0.0;
        densityTarget = 0.0;
        tLastLatchStep = Double.NEGATIVE_INFINITY;
    }

    /** The current output x. */
    public double x() {
        return outX;
    }

    /** The current output y. */
    public double y() {
        return outY;
    }

    /** Whether the output is not yet lattice-locked: move or settle phase. */
    public boolean isMoving() {
        return state != State.rest;
    }

    /** The current phase. */
    public State state() {
        return state;
    }

    /**
     * Steps {@code snapped} toward the lattice cell containing {@code p}:
     * while {@code p} lies outside {@code ±(snapHalf + hysteresis)} of the
     * current cell, step one cell toward it. The band overlap guaranteed by
     * {@link Config} makes this terminate and never oscillate.
     */
    private double hysteresisStep(double snapped, double p) {
        double threshold = config.snapHalf() + config.hysteresis();
        while (p > snapped + threshold) {
            snapped += 1.0;
        }
        while (p < snapped - threshold) {
            snapped -= 1.0;
        }
        return snapped;
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite: " + value);
        }
    }
}
