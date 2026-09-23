package dev.vfyjxf.cloudlib.testutil;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class FrameReplayTest {

    /** Integrates {@code rate} over dt — the smallest stateful subject imaginable. */
    private static final class Integrator {

        private double value;

        double step(double dtSeconds, double rate) {
            value += dtSeconds * rate;
            return value;
        }
    }

    @Test
    void recordsDtInputStartTimesAndOutputsPerFrame() {
        List<FrameReplay.Step<Double>> steps = List
                .of(FrameReplay.Step.of(0.5, 2.0), FrameReplay.Step.of(0.25, 4.0), FrameReplay.Step.of(1.0, -1.0));

        FrameReplay<Double, Double> replay = FrameReplay
                .run(new Integrator(), steps, (subject, dt, input) -> subject.step(dt, input));

        assertEquals(3, replay.frameCount());
        assertEquals(List.of(2.0, 4.0, -1.0), replay.inputs());
        assertEquals(List.of(0.5, 0.25, 1.0), replay.dts());
        assertEquals(List.of(1.0, 2.0, 1.0), replay.outputs());
        assertEquals(1.0, replay.lastOutput());

        assertEquals(0.0, replay.frames().get(0).startTime(), 1.0e-12);
        assertEquals(0.5, replay.frames().get(1).startTime(), 1.0e-12);
        assertEquals(0.75, replay.frames().get(2).startTime(), 1.0e-12);
        assertEquals(0, replay.frames().get(0).index());
        assertEquals(2, replay.frames().get(2).index());
    }

    @Test
    void uniformRunDrivesAConstantFrameRate() {
        FrameReplay<Double, Double> replay = FrameReplay
                .runUniform(new Integrator(), 0.25, 4, 1.0, (subject, dt, input) -> subject.step(dt, input));

        assertEquals(List.of(0.25, 0.5, 0.75, 1.0), replay.outputs());
        assertEquals(
            List.of(0.0, 0.25, 0.5, 0.75),
            replay.frames().stream().map(FrameReplay.Frame::startTime).toList()
        );
    }

    @Test
    void sameStepsAlwaysProduceTheSameOutputs() {
        List<FrameReplay.Step<Double>> steps = List.of(
            FrameReplay.Step.of(1.0 / 60.0, 3.0),
            FrameReplay.Step.of(1.0 / 30.0, -1.0),
            FrameReplay.Step.of(0.5, 0.5)
        );

        FrameReplay<Double, Double> first = FrameReplay.run(new Integrator(), steps, (s, dt, in) -> s.step(dt, in));
        FrameReplay<Double, Double> second = FrameReplay.run(new Integrator(), steps, (s, dt, in) -> s.step(dt, in));

        assertEquals(first.outputs(), second.outputs());
        assertEquals(first.frames(), second.frames());
    }

    @Test
    void emptyScriptProducesAnEmptyReplay() {
        FrameReplay<Double, Double> replay = FrameReplay
                .run(new Integrator(), List.of(), (subject, dt, input) -> subject.step(dt, input));

        assertEquals(0, replay.frameCount());
        assertTrue(replay.outputs().isEmpty());
        assertThrows(IndexOutOfBoundsException.class, replay::lastOutput);
    }

    @Test
    void invalidDtFailsBeforeTheSubjectIsEverDriven() {
        int[] driven = {0};

        assertThrows(
            IllegalArgumentException.class,
            () -> FrameReplay.run(
                new Object(),
                List.of(FrameReplay.Step.of(0.1, 1.0), FrameReplay.Step.of(-0.1, 1.0)),
                (subject, dt, input) -> {
                    driven[0]++;
                    return input;
                }
            )
        );

        assertEquals(0, driven[0]);
    }
}
