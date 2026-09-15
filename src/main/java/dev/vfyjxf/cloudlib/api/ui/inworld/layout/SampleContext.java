package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import java.util.Objects;

/**
 * The frame clock handed to {@link SourceProvider#sample} — a monotonically
 * increasing frame index, wall seconds plus this frame's delta, the render
 * partial tick and the dimension sources are being sampled in.
 */
public record SampleContext(long frameIndex, double seconds, double dt, double partialTick, String dimension) {

    public SampleContext {
        if (!Double.isFinite(seconds + dt + partialTick) || dt < 0.0) {
            throw new IllegalArgumentException("frame clock");
        }
        Objects.requireNonNull(dimension);
    }
}
