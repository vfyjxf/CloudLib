package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

import dev.vfyjxf.cloudlib.testutil.LcgNoise;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class PixelStabilizerTest {

    private static final double dt = 1.0 / 60.0;

    @Test
    void firstFrameSeedsAtTheLatticeRoundingHalfUp() {
        PixelStabilizer stabilizer = new PixelStabilizer();
        PixelStabilizer.Output out = stabilizer.accept(0.0, 10.5, 20.5);
        assertEquals(11.0, out.x(), 0.0);
        assertEquals(21.0, out.y(), 0.0);
        assertEquals(PixelStabilizer.State.rest, stabilizer.state());
        assertFalse(stabilizer.isMoving());
    }

    @Test
    void boundaryChatterIsAbsorbedByHysteresis() {
        // the core use case: the input wobbles ±0.1 px around a half-pixel
        // boundary (10.5) — without hysteresis, round() flips every frame
        PixelStabilizer stabilizer = new PixelStabilizer();
        LcgNoise noise = new LcgNoise(0xB0DA7);
        for (int i = 0; i < 240; i++) {
            double x = 10.5 + noise.next(0.1);
            PixelStabilizer.Output out = stabilizer.accept(i * dt, x, 30.0);
            assertEquals(11.0, out.x(), 0.0, "output must stay latched at frame " + i);
            assertEquals(30.0, out.y(), 0.0);
            assertEquals(PixelStabilizer.State.rest, stabilizer.state(), "frame " + i);
        }
    }

    @Test
    void slowRampStepsUniformlyAndKeepsTheResidual() {
        PixelStabilizer stabilizer = new PixelStabilizer();
        List<Integer> stepFrames = new ArrayList<>();
        double previousOut = Double.NaN;
        double lastInput = Double.NaN;
        for (int i = 0; i < 300; i++) {
            double x = 10.0 + 0.1 * i;
            lastInput = x;
            PixelStabilizer.Output out = stabilizer.accept(i * dt, x, 20.0);
            assertTrue(out.x() == Math.rint(out.x()), "rest output must be integer at frame " + i);
            if (!Double.isNaN(previousOut) && out.x() != previousOut) {
                assertEquals(1.0, out.x() - previousOut, 0.0, "ramp steps must be single pixels at frame " + i);
                stepFrames.add(i);
            }
            previousOut = out.x();
            assertEquals(PixelStabilizer.State.rest, stabilizer.state(), "6 px/s is rest, frame " + i);
        }

        assertEquals(8, stepFrames.get(0), "the first step happens when 10 + 0.1i clears 10.75");
        for (int i = 1; i < stepFrames.size(); i++) {
            assertEquals(10, stepFrames.get(i) - stepFrames.get(i - 1), "steps must keep a uniform cadence");
        }
        // 300 frames * 0.1 px = 30 px of input motion, 30 single-pixel steps
        assertEquals(30, stepFrames.size());
        assertEquals(40.0, previousOut, 0.0);
        assertTrue(Math.abs(previousOut - lastInput) <= 1.0, "no residual may be lost");
    }

    @Test
    void fastMotionSwitchesToMoveAndPassesSubpixelThrough() {
        PixelStabilizer stabilizer = new PixelStabilizer();
        boolean seenMove = false;
        for (int i = 0; i < 120; i++) {
            double x = 100.0 + 2.05 * i;
            PixelStabilizer.Output out = stabilizer.accept(i * dt, x, 50.0);
            if (stabilizer.state() == PixelStabilizer.State.move) {
                seenMove = true;
                assertEquals(x, out.x(), 0.0, "move phase must pass the sub-pixel input through at frame " + i);
                assertTrue(stabilizer.isMoving());
            }
            if (i >= 3) {
                assertTrue(seenMove, "122 px/s must have entered move by frame " + i);
            }
        }
        assertTrue(seenMove);
    }

    @Test
    void moveToRestDwellsFiveFramesThenSettlesMonotonically() {
        PixelStabilizer stabilizer = new PixelStabilizer();
        double frozen = 100.0 + 2.03 * 89; // 280.67 — a .67 fraction to land on 281
        List<Double> settleOutputs = new ArrayList<>();
        int settleStart = -1;
        int restResume = -1;
        for (int i = 0; i < 140; i++) {
            boolean moving = i < 90;
            double x = moving ? 100.0 + 2.03 * i : frozen;
            PixelStabilizer.Output out = stabilizer.accept(i * dt, x, 40.0);
            if (i < 90) {
                continue;
            }
            int freezeFrame = i - 90;
            if (stabilizer.state() == PixelStabilizer.State.settle) {
                if (settleStart < 0) {
                    settleStart = freezeFrame;
                }
                settleOutputs.add(out.x());
            } else if (stabilizer.state() == PixelStabilizer.State.rest && restResume < 0 && !settleOutputs.isEmpty()) {
                restResume = freezeFrame;
            }
        }

        // the smoothed speed needs 8 freeze frames to decay below 9 px/s
        // (121.8 * 0.7^k < 9 at k = 8), then 5 dwell frames accumulate, so the
        // landing begins at freeze frame 7 + 5 - 1 = 11
        assertEquals(11, settleStart, "settle must begin after the 5-frame dwell past the speed decay");
        assertEquals(19, restResume, "the 120 ms glide needs 8 more frames at 60 fps");

        // the glide: monotone, bounded by [from, target], never overshooting
        double previous = frozen;
        for (double value : settleOutputs) {
            assertTrue(value >= previous - 1.0e-12, "settle must be monotone: " + value + " after " + previous);
            assertTrue(value <= 281.0 + 1.0e-12, "settle must never overshoot the lattice: " + value);
            previous = value;
        }
        assertTrue(Math.abs(previous - 281.0) < 0.01, "settle must reach the lattice: " + previous);
        assertEquals(281.0, stabilizer.x(), 0.0, "rest resumes exactly on the lattice");
        assertEquals(PixelStabilizer.State.rest, stabilizer.state());
    }

    @Test
    void settleAbortsWhenMotionResumes() {
        PixelStabilizer stabilizer = new PixelStabilizer();
        for (int i = 0; i < 90; i++) {
            stabilizer.accept(i * dt, 100.0 + 2.03 * i, 40.0);
        }
        double frozen = 100.0 + 2.03 * 89;
        for (int i = 90; i < 106; i++) {
            stabilizer.accept(i * dt, frozen, 40.0); // dwell ends, settle begins at freeze frame 11
        }
        assertEquals(PixelStabilizer.State.settle, stabilizer.state(), "expected to be mid-landing");

        // one frame at 120 px/s lifts the smoothed speed to ~36 px/s — below
        // the 45 px/s abort threshold, so the landing holds one more frame
        double x = frozen + 2.0;
        PixelStabilizer.Output out = stabilizer.accept(106 * dt, x, 40.0);
        assertEquals(PixelStabilizer.State.settle, stabilizer.state(), "one frame at speed is not yet enough to abort");

        // the second fast frame pushes the smoothed speed past 45 px/s
        out = stabilizer.accept(107 * dt, x + 2.0, 40.0);
        assertEquals(PixelStabilizer.State.move, stabilizer.state(), "sustained speed must abort the landing");
        assertEquals(x + 2.0, out.x(), 0.0, "move passthrough resumes immediately");
    }

    @Test
    void degenerateConfigsLandInOneFrame() {
        PixelStabilizer stabilizer = new PixelStabilizer(new PixelStabilizer.Config(0.5, 0.25, 45.0, 9.0, 1, 0.0));
        double frozen = 100.0 + 2.03 * 14; // 128.42 — lands on 128
        int restFrame = -1;
        for (int i = 0; i < 30; i++) {
            double x = i < 15 ? 100.0 + 2.03 * i : frozen;
            PixelStabilizer.Output out = stabilizer.accept(i * dt, x, 0.0);
            if (i >= 15 && stabilizer.state() == PixelStabilizer.State.rest && restFrame < 0) {
                restFrame = i - 15;
                assertEquals(128.0, out.x(), 0.0, "a zero-length settle must land on the lattice directly");
            }
        }
        assertTrue(restFrame > 0 && restFrame <= 9, "dwell 1 + settle 0 must land almost immediately: " + restFrame);
    }

    @Test
    void zeroHysteresisFlipsExactlyAtTheHalfCellEdge() {
        // the degenerate config documents what hysteresis is for: with a zero
        // cushion the band edge is the half-cell itself, so inputs straddling
        // it flip the lattice every frame
        PixelStabilizer stabilizer = new PixelStabilizer(new PixelStabilizer.Config(0.5, 0.0, 45.0, 9.0, 5, 0.12));
        PixelStabilizer.Output out = stabilizer.accept(0.0, 10.5, 0.0);
        assertEquals(11.0, out.x(), 0.0);
        out = stabilizer.accept(dt, 10.49, 0.0);
        assertEquals(10.0, out.x(), 0.0, "10.49 is below 10.5 — no cushion, immediate step down");
        out = stabilizer.accept(2 * dt, 10.51, 0.0);
        assertEquals(11.0, out.x(), 0.0, "10.51 is above 10.5 — no cushion, immediate step up");
        out = stabilizer.accept(3 * dt, 10.2, 0.0);
        assertEquals(10.0, out.x(), 0.0);
    }

    @Test
    void bypassResticksImmediatelyWithoutPhantomVelocity() {
        PixelStabilizer stabilizer = new PixelStabilizer();
        for (int i = 0; i < 60; i++) {
            stabilizer.accept(i * dt, 2.0 * i, 1.0 * i);
        }
        assertTrue(stabilizer.isMoving());

        double t = 60 * dt;
        PixelStabilizer.Output out = stabilizer.snapBypass(t, 512.49, 300.51);
        assertEquals(512.0, out.x(), 0.0);
        assertEquals(301.0, out.y(), 0.0);
        assertEquals(PixelStabilizer.State.rest, stabilizer.state());
        assertFalse(stabilizer.isMoving());

        LcgNoise noise = new LcgNoise(0x5EED2);
        for (int i = 0; i < 60; i++) {
            out = stabilizer.accept(t + (i + 1) * dt, 512.45 + noise.next(0.03), 300.52);
            assertEquals(512.0, out.x(), 0.0, "no phantom velocity after the bypass, frame " + i);
            assertEquals(301.0, out.y(), 0.0);
            assertEquals(PixelStabilizer.State.rest, stabilizer.state(), "frame " + i);
        }
    }

    @Test
    void resetReseedsFromScratch() {
        PixelStabilizer stabilizer = new PixelStabilizer();
        for (int i = 0; i < 60; i++) {
            stabilizer.accept(i * dt, 2.0 * i, 0.0);
        }
        stabilizer.reset();
        assertEquals(PixelStabilizer.State.rest, stabilizer.state());

        PixelStabilizer.Output out = stabilizer.accept(5.0, 42.4, -7.2);
        assertEquals(42.0, out.x(), 0.0, "the first sample after reset must seed, not glide");
        assertEquals(-7.0, out.y(), 0.0);
    }

    @Test
    void nonAdvancingTimestampsAreIgnored() {
        PixelStabilizer stabilizer = new PixelStabilizer();
        PixelStabilizer.Output out = stabilizer.accept(1.0, 10.4, 30.0);
        assertEquals(10.0, out.x(), 0.0);
        out = stabilizer.accept(1.0, 999.0, -500.0);
        assertEquals(10.0, out.x(), 0.0, "a duplicate timestamp must not consume the sample");
        assertEquals(30.0, out.y(), 0.0);
        out = stabilizer.accept(0.5, 999.0, -500.0);
        assertEquals(10.0, out.x(), 0.0, "a rewound timestamp must not move state");
    }

    @Test
    void rejectsInvalidArguments() {
        assertThrows(IllegalArgumentException.class, () -> new PixelStabilizer.Config(0.0, 0.25, 45.0, 9.0, 5, 0.12));
        assertThrows(IllegalArgumentException.class, () -> new PixelStabilizer.Config(-0.5, 0.25, 45.0, 9.0, 5, 0.12));
        assertThrows(
                IllegalArgumentException.class, () -> new PixelStabilizer.Config(Double.NaN, 0.25, 45.0, 9.0, 5, 0.12));
        assertThrows(IllegalArgumentException.class, () -> new PixelStabilizer.Config(0.5, -0.01, 45.0, 9.0, 5, 0.12));
        assertThrows(IllegalArgumentException.class, () -> new PixelStabilizer.Config(0.5, 0.5, 45.0, 9.0, 5, 0.12));
        assertThrows(IllegalArgumentException.class, () -> new PixelStabilizer.Config(0.7, 0.4, 45.0, 9.0, 5, 0.12));
        assertThrows(IllegalArgumentException.class, () -> new PixelStabilizer.Config(0.5, 0.25, 0.0, 9.0, 5, 0.12));
        assertThrows(
                IllegalArgumentException.class,
                () -> new PixelStabilizer.Config(0.5, 0.25, Double.POSITIVE_INFINITY, 9.0, 5, 0.12));
        assertThrows(IllegalArgumentException.class, () -> new PixelStabilizer.Config(0.5, 0.25, 45.0, -1.0, 5, 0.12));
        assertThrows(IllegalArgumentException.class, () -> new PixelStabilizer.Config(0.5, 0.25, 9.0, 9.0, 5, 0.12));
        assertThrows(IllegalArgumentException.class, () -> new PixelStabilizer.Config(0.5, 0.25, 20.0, 45.0, 5, 0.12));
        assertThrows(IllegalArgumentException.class, () -> new PixelStabilizer.Config(0.5, 0.25, 45.0, 9.0, 0, 0.12));
        assertThrows(IllegalArgumentException.class, () -> new PixelStabilizer.Config(0.5, 0.25, 45.0, 9.0, 5, -0.01));
        assertThrows(
                IllegalArgumentException.class, () -> new PixelStabilizer.Config(0.5, 0.25, 45.0, 9.0, 5, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new PixelStabilizer().accept(Double.NaN, 0.0, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new PixelStabilizer().accept(0.0, Double.NaN, 0.0));
        assertThrows(IllegalArgumentException.class, () -> new PixelStabilizer().accept(0.0, 0.0, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> new PixelStabilizer()
                .snapBypass(0.0, Double.POSITIVE_INFINITY, 0.0));
        assertThrows(NullPointerException.class, () -> new PixelStabilizer(null));
    }
}
