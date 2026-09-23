package dev.vfyjxf.cloudlib.testutil;

import java.util.ArrayList;
import java.util.List;

/**
 * Frame-sequence replay harness: drives a subject one frame at a time over a
 * scripted list of {@link Step}s — one dt plus one input per frame — and
 * records a {@link Frame} per step carrying the frame's start time, its input
 * and the output the {@link Driver} produced. The recorded sequence is what
 * snapshot assertions lock down: same steps in, same outputs out.
 * <p>
 * The subject never sees a wall clock — dt comes from the steps and frame
 * start times from an internal {@link ManualClock}, so replays are
 * deterministic and reproducible at any frame rate. All step dt values are
 * validated before the first frame runs, so a malformed script never leaves
 * the subject half-driven.
 *
 * @param <I> the per-frame input type
 * @param <O> the per-frame output type
 */
public final class FrameReplay<I, O> {

    /** One scripted frame: how long it lasts and what goes into the subject. */
    public record Step<I>(double dtSeconds, I input) {

        public static <I> Step<I> of(double dtSeconds, I input) {
            return new Step<>(dtSeconds, input);
        }
    }

    /** Applies one frame to the subject and returns the value to record. */
    @FunctionalInterface
    public interface Driver<S, I, O> {

        O drive(S subject, double dtSeconds, I input);
    }

    /** The record of one executed frame. */
    public record Frame<I, O>(int index, double dtSeconds, double startTime, I input, O output) {}

    private final List<Frame<I, O>> frames;

    private FrameReplay(List<Frame<I, O>> frames) {
        this.frames = List.copyOf(frames);
    }

    /**
     * Replays {@code steps} against {@code subject}, recording one output per
     * frame.
     */
    public static <S, I, O> FrameReplay<I, O> run(S subject, List<Step<I>> steps, Driver<S, I, O> driver) {
        for (Step<I> step : steps) {
            requireStepDt(step.dtSeconds());
        }
        ManualClock clock = new ManualClock();
        List<Frame<I, O>> frames = new ArrayList<>(steps.size());
        for (Step<I> step : steps) {
            double startTime = clock.now();
            clock.advance(step.dtSeconds());
            O output = driver.drive(subject, step.dtSeconds(), step.input());
            frames.add(new Frame<>(frames.size(), step.dtSeconds(), startTime, step.input(), output));
        }
        return new FrameReplay<>(frames);
    }

    /**
     * Replays {@code frameCount} frames of a constant dt with an unchanging
     * input — the shape frame-rate-independence tests usually want.
     */
    public static <S, I, O> FrameReplay<I, O> runUniform(
        S subject,
        double dtSeconds,
        int frameCount,
        I input,
        Driver<S, I, O> driver
    ) {
        List<Step<I>> steps = new ArrayList<>(Math.max(0, frameCount));
        for (int i = 0; i < frameCount; i++) {
            steps.add(new Step<>(dtSeconds, input));
        }
        return run(subject, steps, driver);
    }

    public List<Frame<I, O>> frames() {
        return frames;
    }

    /** The recorded outputs, one per frame, in frame order. */
    public List<O> outputs() {
        List<O> outputs = new ArrayList<>(frames.size());
        for (Frame<I, O> frame : frames) {
            outputs.add(frame.output());
        }
        return outputs;
    }

    /** The scripted inputs, one per frame, in frame order. */
    public List<I> inputs() {
        List<I> inputs = new ArrayList<>(frames.size());
        for (Frame<I, O> frame : frames) {
            inputs.add(frame.input());
        }
        return inputs;
    }

    /** The scripted frame durations, in frame order. */
    public List<Double> dts() {
        List<Double> dts = new ArrayList<>(frames.size());
        for (Frame<I, O> frame : frames) {
            dts.add(frame.dtSeconds());
        }
        return dts;
    }

    public int frameCount() {
        return frames.size();
    }

    /**
     * @return the output of the last frame
     * @throws IndexOutOfBoundsException when the replay has no frames
     */
    public O lastOutput() {
        return frames.get(frames.size() - 1).output();
    }

    private static void requireStepDt(double dtSeconds) {
        if (Double.isNaN(dtSeconds) || Double.isInfinite(dtSeconds) || dtSeconds < 0) {
            throw new IllegalArgumentException("frame dt must be finite and non-negative: " + dtSeconds);
        }
    }
}
