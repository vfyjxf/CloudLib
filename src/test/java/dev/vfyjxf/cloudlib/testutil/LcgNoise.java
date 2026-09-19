package dev.vfyjxf.cloudlib.testutil;

/**
 * A deterministic noise source for stability tests: the classic 32-bit
 * numerical-recipes linear congruential generator
 * ({@code x = 1664525·x + 1013904223 mod 2^32}), mapped to {@code [-1, 1)}.
 * Fixed seed → identical sequence on every run and every JVM, which is the
 * whole point: filter behavior under noise must be assertable, not sampled.
 */
public final class LcgNoise {

    private static final long multiplier = 1664525L;
    private static final long increment = 1013904223L;
    private static final long modulus = 1L << 32;

    private long state;

    /** @param seed any long; the sequence depends on it deterministically */
    public LcgNoise(long seed) {
        this.state = seed & 0xffffffffL;
    }

    /** The next sample in {@code [-1, 1)}. */
    public double next() {
        state = (multiplier * state + increment) & (modulus - 1);
        return (state / (double) modulus) * 2.0 - 1.0;
    }

    /** The next sample scaled to {@code [-amplitude, amplitude)}. */
    public double next(double amplitude) {
        return next() * amplitude;
    }
}
