package dev.vfyjxf.cloudlib.testutil;

/**
 * A manually advanced frame clock. Time moves only when the test calls
 * {@link #advance(double)} — there is no wall-clock input — which is exactly
 * what frame-rate-independence tests need: the same logical frame sequence can
 * be replayed with any dt sampling and must drive the subject to the same
 * state.
 * <p>
 * Every {@code advance} call is one frame: {@link #now()} reports the seconds
 * elapsed since construction and {@link #frameCount()} how many frames were
 * stepped. Zero-length frames are allowed (a tick that passes no time); NaN,
 * infinite and negative dt are rejected.
 */
public final class ManualClock {

    private double timeSeconds;
    private long frameCount;

    /** Seconds elapsed since construction. */
    public double now() {
        return timeSeconds;
    }

    /** How many frames have been advanced so far. */
    public long frameCount() {
        return frameCount;
    }

    /**
     * Advances the clock by one frame of {@code dtSeconds}.
     *
     * @return this, for chaining
     * @throws IllegalArgumentException when dt is NaN, infinite or negative
     */
    public ManualClock advance(double dtSeconds) {
        requireFrameDt(dtSeconds);
        timeSeconds += dtSeconds;
        frameCount++;
        return this;
    }

    /**
     * Advances the clock by {@code frames} frames of {@code dtSeconds} each.
     *
     * @return this, for chaining
     * @throws IllegalArgumentException when frames is negative or dt is NaN, infinite or negative
     */
    public ManualClock advanceFrames(int frames, double dtSeconds) {
        if (frames < 0) {
            throw new IllegalArgumentException("frames must not be negative: " + frames);
        }
        requireFrameDt(dtSeconds);
        for (int i = 0; i < frames; i++) {
            advance(dtSeconds);
        }
        return this;
    }

    private static void requireFrameDt(double dtSeconds) {
        if (Double.isNaN(dtSeconds) || Double.isInfinite(dtSeconds) || dtSeconds < 0) {
            throw new IllegalArgumentException("frame dt must be finite and non-negative: " + dtSeconds);
        }
    }
}
