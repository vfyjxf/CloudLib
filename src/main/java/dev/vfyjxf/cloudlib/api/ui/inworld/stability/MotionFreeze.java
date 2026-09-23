package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * The moving-target layout freeze (de Berg et al., ESA '13 — labels of
 * moving objects re-solve only at rest): a Schmitt trigger on the tracked
 * point's speed that freezes re-layout while the point moves fast and
 * releases after it has demonstrably settled.
 * <p>
 * Speed is the frame displacement over {@code dtSeconds}, low-passed with a
 * short half-life so single-frame spikes neither trip the freeze nor hold
 * it. The trigger enters freeze strictly above {@code enterSpeedPxPerSec}
 * and only starts counting the settle dwell strictly below
 * {@code releaseSpeedPxPerSec} — the band between the two speeds holds
 * whatever state the gate is in (the hysteresis). The freeze releases once
 * the sub-release speed has persisted for {@code settleSeconds} of
 * accumulated frame time; one resolve typically follows on the release
 * frame.
 * <p>
 * A null observation resets the gate entirely (the point is gone — no speed,
 * no freeze); the next observation re-seeds the position at zero speed.
 * Non-finite {@code dtSeconds} is rejected; a zero or negative dt holds the
 * previous speed and advances neither timer.
 */
public final class MotionFreeze {

    /**
     * The three knobs.
     *
     * @param enterSpeedPxPerSec the speed strictly above which the gate
     *        enters freeze; must be positive
     * @param releaseSpeedPxPerSec the speed strictly below which the settle
     *        dwell accumulates; must be positive and strictly below the
     *        enter speed
     * @param settleSeconds how long the speed must stay below the release
     *        speed before the freeze lifts; must be positive
     * @param speedHalfLifeSeconds the low-pass half-life of the speed
     *        estimate; must be positive
     */
    public record Config(
        double enterSpeedPxPerSec,
        double releaseSpeedPxPerSec,
        double settleSeconds,
        double speedHalfLifeSeconds
    ) {

        public Config {
            requireFinitePositive("enterSpeedPxPerSec", enterSpeedPxPerSec);
            requireFinitePositive("releaseSpeedPxPerSec", releaseSpeedPxPerSec);
            requireFinitePositive("settleSeconds", settleSeconds);
            requireFinitePositive("speedHalfLifeSeconds", speedHalfLifeSeconds);
            if (releaseSpeedPxPerSec >= enterSpeedPxPerSec) {
                throw new IllegalArgumentException(
                    "releaseSpeedPxPerSec must be strictly below enterSpeedPxPerSec: " + releaseSpeedPxPerSec + " >= "
                            + enterSpeedPxPerSec
                );
            }
        }

        public static Config of(
            double enterSpeedPxPerSec,
            double releaseSpeedPxPerSec,
            double settleSeconds,
            double speedHalfLifeSeconds
        ) {
            return new Config(enterSpeedPxPerSec, releaseSpeedPxPerSec, settleSeconds, speedHalfLifeSeconds);
        }

        private static void requireFinitePositive(String name, double value) {
            if (!Double.isFinite(value) || value <= 0.0) {
                throw new IllegalArgumentException(name + " must be finite and positive: " + value);
            }
        }
    }

    private final Config config;
    private @Nullable FloatPos last;
    private double speed;
    private boolean frozen;
    private double settleElapsed;

    /** A thawed gate — the first observation seeds the position at zero speed. */
    public MotionFreeze(Config config) {
        this.config = Objects.requireNonNull(config, "config");
    }

    /**
     * One frame: observes the point and the frame delta, updates the speed
     * estimate and the trigger state.
     *
     * @return whether re-layout is frozen for this frame
     */
    public boolean observe(@Nullable FloatPos point, double dtSeconds) {
        if (!Double.isFinite(dtSeconds)) {
            throw new IllegalArgumentException("dtSeconds must be finite: " + dtSeconds);
        }
        if (point == null) {
            last = null;
            speed = 0.0;
            frozen = false;
            settleElapsed = 0.0;
            return false;
        }
        if (last != null && dtSeconds > 0.0) {
            double instantaneous = Math.hypot(point.x() - last.x(), point.y() - last.y()) / dtSeconds;
            speed = Smoothing.dampHalfLife(speed, instantaneous, config.speedHalfLifeSeconds(), dtSeconds);
        }
        last = new FloatPos(point.x(), point.y());
        if (!frozen) {
            if (speed > config.enterSpeedPxPerSec()) {
                frozen = true;
                settleElapsed = 0.0;
            }
        } else {
            if (speed < config.releaseSpeedPxPerSec()) {
                settleElapsed += dtSeconds;
                if (settleElapsed >= config.settleSeconds()) {
                    frozen = false;
                    settleElapsed = 0.0;
                }
            } else {
                settleElapsed = 0.0;
            }
        }
        return frozen;
    }

    /** The frozen state without observing. */
    public boolean frozen() {
        return frozen;
    }

    /** The low-passed speed estimate in px/s (the last observed frame's). */
    public double speedPxPerSec() {
        return speed;
    }

    /** Drops all state — position, speed and trigger. */
    public void reset() {
        last = null;
        speed = 0.0;
        frozen = false;
        settleElapsed = 0.0;
    }
}
