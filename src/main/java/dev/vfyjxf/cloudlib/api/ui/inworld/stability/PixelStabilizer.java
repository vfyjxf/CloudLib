package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

import java.util.Objects;

/**
 * The pixel-space stabilizer behind a follow panel: a two-state (rest / move)
 * motion classifier with hysteresis on both the state switch and the integer
 * quantizer, so a panel that is <em>almost</em> stationary renders on a locked
 * integer lattice while a panel that is genuinely moving renders sub-pixel
 * accurate — the survey-synthesis answer to "static text shimmers, moving text
 * smears".
 * <p>
 * <strong>Rest.</strong> Each axis is quantized by a Schmitt-trigger
 * rounder: {@code snapped} starts at {@code round(p)} and only steps when the
 * float input leaves the band {@code [snapped - snapHalf - hysteresis,
 * snapped + snapHalf + hysteresis]} — with the defaults a 1.5 px dead zone
 * straddling every half-pixel boundary. A signal oscillating near
 * {@code k + 0.5} therefore cannot flip the output at all: the hysteresis
 * overlap between adjacent lattice cells swallows the chatter. The hard rule
 * is that the quantized value is never fed back: the state machine only ever
 * reads the float inputs, so quantization error cannot accumulate or drive
 * the speed estimate.
 * <p>
 * <strong>Move.</strong> When the smoothed speed (an exponential average,
 * factor 0.3, of the per-frame Euclidean input speed) exceeds
 * {@code moveVelocity}, the quantizer is abandoned and the output is the float
 * input itself — sub-pixel motion is perceptible and desirable at speed, and
 * pixel snapping there reads as stutter. The shared speed drives one state
 * machine for both axes; each axis keeps its own lattice position.
 * <p>
 * <strong>Settle.</strong> When the speed stays below {@code restVelocity}
 * for {@code dwellFrames} consecutive frames, the panel lands: an
 * ease-out-cubic glide from the current float position to the lattice over
 * {@code settleSeconds} — monotone, never overshooting — after which
 * rest-phase snapping resumes. If the input speeds up past
 * {@code moveVelocity} mid-landing, the landing aborts and move-phase
 * passthrough resumes immediately.
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
 * 0.12} is just past that window — long enough to read as a deliberate
 * settle, short enough to never feel like lag. Positions are device pixels;
 * time is seconds from the caller's clock. A call whose {@code t} does not
 * advance is ignored: state and output unchanged.
 */
public final class PixelStabilizer {

    /** Exponential factor of the smoothed-speed estimate. */
    private static final double velocitySmoothing = 0.3;

    /** The motion phases of the stabilizer, in cycle {@code rest → move → settle → rest}. */
    public enum State {

        /** Locked to the integer lattice via hysteresis snapping. */
        rest,

        /** Sub-pixel passthrough of the float input. */
        move,

        /** Landing: the post-move ease-out glide onto the lattice. */
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
     *        sub-pixel (move phase); positive, strictly above
     *        {@code restVelocity}
     * @param restVelocity the smoothed speed below which dwell toward a
     *        landing accumulates; non-negative
     * @param dwellFrames consecutive slow frames required before the settle
     *        glide begins; at least 1
     * @param settleSeconds duration of the landing glide; non-negative (0
     *        lands on the lattice in a single frame)
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
    private double settleFromX;
    private double settleFromY;
    private double settleElapsed;

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
     * @return the position to render — integer-valued in rest, the float
     *         input itself in move, the eased landing in settle
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

        switch (state) {
            case rest -> acceptRest(x, y);
            case move -> acceptMove(x, y);
            case settle -> acceptSettle(dt, x, y);
        }
        return new Output(outX, outY);
    }

    private void acceptRest(double x, double y) {
        if (smoothedSpeed > config.moveVelocity()) {
            state = State.move;
            dwell = 0;
            outX = x;
            outY = y;
            return;
        }
        snappedX = hysteresisStep(snappedX, x);
        snappedY = hysteresisStep(snappedY, y);
        outX = snappedX;
        outY = snappedY;
    }

    private void acceptMove(double x, double y) {
        outX = x;
        outY = y;
        if (smoothedSpeed < config.restVelocity()) {
            dwell++;
        } else {
            dwell = 0;
        }
        if (dwell >= config.dwellFrames()) {
            state = State.settle;
            dwell = 0;
            settleElapsed = 0.0;
            settleFromX = x;
            settleFromY = y;
            snappedX = Math.round(x);
            snappedY = Math.round(y);
        }
    }

    private void acceptSettle(double dt, double x, double y) {
        if (smoothedSpeed > config.moveVelocity()) {
            state = State.move;
            dwell = 0;
            outX = x;
            outY = y;
            return;
        }
        settleElapsed += dt;
        snappedX = hysteresisStep(snappedX, x);
        snappedY = hysteresisStep(snappedY, y);
        double progress = settleProgress();
        double eased = easeOutCubic(progress);
        outX = settleFromX + (snappedX - settleFromX) * eased;
        outY = settleFromY + (snappedY - settleFromY) * eased;
        if (progress >= 1.0) {
            state = State.rest;
            outX = snappedX;
            outY = snappedY;
        }
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
        settleFromX = 0.0;
        settleFromY = 0.0;
        settleElapsed = 0.0;
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

    private double settleProgress() {
        if (config.settleSeconds() <= 0.0) {
            return 1.0;
        }
        return Math.min(1.0, settleElapsed / config.settleSeconds());
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

    /** {@code 1 - (1 - u)^3}: monotone on [0, 1], flattening at the end — no overshoot. */
    private static double easeOutCubic(double u) {
        double inverse = 1.0 - u;
        return 1.0 - inverse * inverse * inverse;
    }

    private static void requireFinite(double value, String name) {
        if (!Double.isFinite(value)) {
            throw new IllegalArgumentException(name + " must be finite: " + value);
        }
    }
}
