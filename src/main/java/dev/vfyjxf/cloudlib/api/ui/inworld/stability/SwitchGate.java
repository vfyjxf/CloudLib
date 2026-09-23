package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * The discrete-switch triple gate — the standard control-theory answer to
 * ping-ponging slot, corner, visibility and mode switches. The caller drives
 * it on <em>decision ticks</em> (once per layout epoch, not once per render
 * frame — decision frequency is deliberately not frame rate), each tick
 * proposing the state it would switch to plus one scalar measurement of the
 * situation in that candidate's favor: pixels of displacement, a score
 * margin, a coverage ratio — any consistently scaled quantity. For pixel
 * domains use pixels so {@code deadZonePixels} means what it says.
 * <p>
 * The four gates, in evaluation order:
 * <ol>
 *   <li><strong>Dead zone</strong> ({@code deadZonePixels}): the incoming
 *       measurement feeds a deadband filter — it only moves the gate's
 *       reference measurement when it differs from the reference by at least
 *       {@code deadZonePixels}. Sub-noise jitter therefore freezes the
 *       reference entirely, even when the raw value crosses thresholds.</li>
 *   <li><strong>Hysteresis band</strong> ({@code band}): a candidate becomes
 *       eligible only when the filtered measurement has moved at least
 *       {@code band} away from the measurement at which the incumbent state
 *       was accepted. Oscillation around the acceptance point cannot re-open
 *       the gate, in either direction.</li>
 *   <li><strong>Dwell</strong> ({@code dwellFrames}): an eligible candidate
 *       must be re-proposed on {@code dwellFrames} consecutive ticks — same
 *       candidate, still eligible — before it may commit.</li>
 *   <li><strong>Hold</strong> ({@code minFramesHeld}): after a commit the
 *       gate refuses further commits until {@code minFramesHeld} ticks have
 *       passed.</li>
 * </ol>
 * {@link #propose(Object, double)} returns whether a switch committed on this
 * tick; {@link #current()} is the accepted state. Boundary values are
 * inclusive: a difference of exactly {@code band} is eligible, exactly
 * {@code deadZonePixels} moves the reference.
 * <p>
 * Worked example (corner switching): the element is committed to
 * {@code left}, accepted at displacement 0. A drift proposes {@code right}
 * with the pixel distance it has traveled; the gate switches only once that
 * distance clears the band, has held for the dwell, and the hold timer allows
 * it. A wobble around the acceptance point — on either side of the band —
 * never re-opens the gate, so the corner does not flip back and forth.
 *
 * @param <S> the discrete state under gate (a slot id, a corner, a visibility
 *            flag, a mode)
 */
public final class SwitchGate<S> {

    /**
     * The four gate knobs. {@code band} and {@code deadZonePixels} share the
     * metric's units; {@code dwellFrames} and {@code minFramesHeld} count
     * decision ticks.
     *
     * @param band the hysteresis band — how far the filtered metric must move
     *        from the incumbent's entry measurement before a switch may be
     *        considered; must be positive
     * @param dwellFrames how many consecutive eligible ticks a candidate must
     *        survive before committing; at least 1
     * @param deadZonePixels the deadband of the measurement filter — smaller
     *        differences never move the reference; non-negative
     * @param minFramesHeld the cooldown in ticks after a commit; non-negative
     */
    public record Config(double band, int dwellFrames, double deadZonePixels, int minFramesHeld) {

        public Config {
            if (!Double.isFinite(band) || band <= 0.0) {
                throw new IllegalArgumentException("band must be finite and positive: " + band);
            }
            if (dwellFrames < 1) {
                throw new IllegalArgumentException("dwellFrames must be at least 1: " + dwellFrames);
            }
            if (!Double.isFinite(deadZonePixels) || deadZonePixels < 0.0) {
                throw new IllegalArgumentException("deadZonePixels must be finite and non-negative: " + deadZonePixels);
            }
            if (minFramesHeld < 0) {
                throw new IllegalArgumentException("minFramesHeld must not be negative: " + minFramesHeld);
            }
        }

        public static Config of(double band, int dwellFrames, double deadZonePixels, int minFramesHeld) {
            return new Config(band, dwellFrames, deadZonePixels, minFramesHeld);
        }
    }

    private final Config config;

    private S current;
    private double entryMetric;
    private double referenceMetric;
    private @Nullable S pending;
    private int dwellElapsed;
    private int ticksSinceSwitch;

    /**
     * @param initialState the state the gate opens on
     * @param initialMetric the measurement at which {@code initialState} was
     *        accepted; anchors the hysteresis band and seeds the deadband
     *        reference
     */
    public SwitchGate(Config config, S initialState, double initialMetric) {
        this.config = Objects.requireNonNull(config, "config");
        this.current = Objects.requireNonNull(initialState, "initialState");
        if (!Double.isFinite(initialMetric)) {
            throw new IllegalArgumentException("initialMetric must be finite: " + initialMetric);
        }
        this.entryMetric = initialMetric;
        this.referenceMetric = initialMetric;
    }

    /**
     * One decision tick: proposes {@code candidate} with its measurement.
     *
     * @return {@code true} exactly when a switch committed on this tick
     */
    public boolean propose(S candidate, double metric) {
        Objects.requireNonNull(candidate, "candidate");
        if (!Double.isFinite(metric)) {
            throw new IllegalArgumentException("metric must be finite: " + metric);
        }
        ticksSinceSwitch++;
        boolean incumbentTick = Objects.equals(candidate, current);
        refreshReference(metric);
        if (incumbentTick) {
            clearPending();
            return false;
        }
        boolean eligible = Math.abs(referenceMetric - entryMetric) >= config.band();
        if (!eligible) {
            clearPending();
            return false;
        }
        if (!Objects.equals(candidate, pending)) {
            pending = candidate;
            dwellElapsed = 0;
        }
        dwellElapsed++;
        if (dwellElapsed >= config.dwellFrames() && ticksSinceSwitch >= config.minFramesHeld()) {
            current = candidate;
            entryMetric = referenceMetric;
            clearPending();
            ticksSinceSwitch = 0;
            return true;
        }
        return false;
    }

    /** The accepted state. */
    public S current() {
        return current;
    }

    /** Whether a candidate is currently accumulating dwell. */
    public boolean isDwelling() {
        return pending != null;
    }

    private void clearPending() {
        pending = null;
        dwellElapsed = 0;
    }

    private void refreshReference(double metric) {
        if (Math.abs(metric - referenceMetric) >= config.deadZonePixels()) {
            referenceMetric = metric;
        }
    }
}
