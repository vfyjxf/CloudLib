package dev.vfyjxf.cloudlib.api.ui.inworld.stability;

import dev.vfyjxf.cloudlib.api.ui.inworld.stability.VisibilityTracker.Phase;
import dev.vfyjxf.cloudlib.testutil.FrameReplay;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class VisibilityTrackerTest {

    private static VisibilityTracker tracker(double fadeIn, double fadeOut, double linger) {
        return new VisibilityTracker(VisibilityTracker.Config.of(fadeIn, fadeOut, linger));
    }

    @Test
    void startsHiddenWithZeroAlpha() {
        VisibilityTracker tracker = tracker(1, 1, 2);

        assertEquals(Phase.hidden, tracker.phase(0.0));
        assertEquals(0.0, tracker.alpha(0.0), 0.0);
        assertFalse(tracker.isRendered(0.0));
        assertFalse(tracker.present());
    }

    @Test
    void fadesInLinearlyToVisible() {
        VisibilityTracker tracker = tracker(1, 1, 2);
        tracker.setPresent(true, 0.0);

        assertEquals(Phase.appearing, tracker.phase(0.5));
        assertEquals(0.5, tracker.alpha(0.5), 1.0e-12);
        assertEquals(Phase.visible, tracker.phase(1.0));
        assertEquals(1.0, tracker.alpha(1.0), 1.0e-12);
        assertTrue(tracker.isRendered(1.0));
        assertEquals(1.0, tracker.alpha(5.0), 1.0e-12);
        assertTrue(tracker.present());
    }

    @Test
    void rejectionLingersThenFadesToHidden() {
        VisibilityTracker tracker = tracker(1, 1, 2);
        tracker.setPresent(true, 0.0);
        assertEquals(Phase.visible, tracker.phase(2.0));

        tracker.setPresent(false, 10.0);

        assertEquals(Phase.lingering, tracker.phase(10.0));
        assertEquals(1.0, tracker.alpha(11.9), 1.0e-12);
        assertEquals(Phase.fading, tracker.phase(12.1));
        assertEquals(0.9, tracker.alpha(12.1), 1.0e-12);
        assertEquals(0.5, tracker.alpha(12.5), 1.0e-12);
        assertEquals(Phase.hidden, tracker.phase(13.0));
        assertEquals(0.0, tracker.alpha(13.0), 0.0);
        assertEquals(0.0, tracker.alpha(100.0), 0.0);
        assertFalse(tracker.isRendered(100.0));
    }

    @Test
    void lingerRescueKeepsFullVisibilityWithoutADip() {
        VisibilityTracker tracker = tracker(1, 1, 2);
        tracker.setPresent(true, 0.0);
        tracker.phase(2.0);
        tracker.setPresent(false, 10.0);

        assertEquals(1.0, tracker.alpha(11.0), 1.0e-12);
        tracker.setPresent(true, 11.5);

        assertEquals(Phase.visible, tracker.phase(11.5));
        assertEquals(1.0, tracker.alpha(11.5), 1.0e-12);
        assertEquals(1.0, tracker.alpha(13.0), 1.0e-12);
        assertTrue(tracker.isRendered(13.0));
    }

    @Test
    void rescueDuringFadeRestoresFromTheCurrentAlpha() {
        VisibilityTracker tracker = tracker(1, 1, 2);
        tracker.setPresent(true, 0.0);
        tracker.phase(2.0);
        tracker.setPresent(false, 10.0);
        assertEquals(0.5, tracker.alpha(12.5), 1.0e-12);

        tracker.setPresent(true, 12.5);

        assertEquals(Phase.appearing, tracker.phase(12.5));
        assertEquals(0.5, tracker.alpha(12.5), 1.0e-12);
        assertEquals(0.75, tracker.alpha(13.0), 1.0e-12);
        assertEquals(Phase.visible, tracker.phase(13.5));
        assertEquals(1.0, tracker.alpha(13.5), 1.0e-12);
    }

    @Test
    void rejectionDuringAppearingLingersAtThePartialAlpha() {
        VisibilityTracker tracker = tracker(1, 1, 2);
        tracker.setPresent(true, 0.0);
        assertEquals(0.4, tracker.alpha(0.4), 1.0e-12);

        tracker.setPresent(false, 0.4);

        assertEquals(Phase.lingering, tracker.phase(0.4));
        assertEquals(0.4, tracker.alpha(2.0), 1.0e-12);
        assertEquals(0.4, tracker.alpha(2.4), 1.0e-12);
        assertEquals(0.2, tracker.alpha(2.9), 1.0e-12);
        assertEquals(Phase.hidden, tracker.phase(3.4));
        assertEquals(0.0, tracker.alpha(3.4), 0.0);
    }

    @Test
    void lingerDurationIsConfigurable() {
        VisibilityTracker shortLinger = tracker(0.2, 0.2, 0.5);
        shortLinger.setPresent(true, 0.0);
        shortLinger.phase(1.0);
        shortLinger.setPresent(false, 1.0);

        VisibilityTracker longLinger = tracker(0.2, 0.2, 2.0);
        longLinger.setPresent(true, 0.0);
        longLinger.phase(1.0);
        longLinger.setPresent(false, 1.0);

        assertEquals(0.5, shortLinger.alpha(1.6), 1.0e-12);
        assertEquals(Phase.fading, shortLinger.phase(1.6));
        assertEquals(1.0, longLinger.alpha(1.6), 1.0e-12);
        assertEquals(Phase.lingering, longLinger.phase(1.6));
    }

    @Test
    void isRenderedInEveryPhaseExceptHidden() {
        VisibilityTracker tracker = tracker(1, 1, 2);

        assertFalse(tracker.isRendered(0.0));
        tracker.setPresent(true, 0.0);
        assertTrue(tracker.isRendered(0.5));
        assertTrue(tracker.isRendered(2.0));
        tracker.setPresent(false, 10.0);
        assertTrue(tracker.isRendered(11.0));
        assertTrue(tracker.isRendered(12.5));
        assertFalse(tracker.isRendered(13.0));
    }

    @Test
    void rewoundTimeIsClampedToTheLastSeenTime() {
        VisibilityTracker tracker = tracker(1, 1, 1);
        tracker.setPresent(true, 0.0);
        assertEquals(1.0, tracker.alpha(5.0), 1.0e-12);
        tracker.setPresent(false, 5.0);

        assertEquals(0.5, tracker.alpha(6.5), 1.0e-12);

        assertEquals(0.5, tracker.alpha(6.1), 1.0e-12);
        assertEquals(Phase.fading, tracker.phase(5.5));
        assertTrue(tracker.isRendered(5.8));

        tracker.setPresent(true, 4.0);
        assertEquals(Phase.appearing, tracker.phase(6.5));
        assertEquals(0.5, tracker.alpha(6.5), 1.0e-12);
        assertEquals(0.75, tracker.alpha(7.0), 1.0e-12);
    }

    @Test
    void rewoundAndJumpingTimeSequencesAreSafe() {
        record At(double time, @Nullable Boolean present) {}

        VisibilityTracker tracker = tracker(1, 1, 2);
        List<FrameReplay.Step<At>> steps = List.of(
            new FrameReplay.Step<>(0.016, new At(0.0, true)),
            new FrameReplay.Step<>(0.016, new At(1.0, null)),
            new FrameReplay.Step<>(0.016, new At(10.0, false)),
            new FrameReplay.Step<>(0.016, new At(12.5, null)),
            new FrameReplay.Step<>(0.016, new At(12.1, null)),
            new FrameReplay.Step<>(0.016, new At(12.9, null)),
            new FrameReplay.Step<>(0.016, new At(9.0, null)),
            new FrameReplay.Step<>(0.016, new At(13.5, null)),
            new FrameReplay.Step<>(0.016, new At(13.1, null))
        );

        FrameReplay<At, Double> replay = FrameReplay.run(tracker, steps, (subject, dt, at) -> {
            if (at.present() != null) {
                subject.setPresent(at.present(), at.time());
            }
            return subject.alpha(at.time());
        });

        List<Double> alphas = replay.outputs();
        for (double alpha : alphas) {
            assertTrue(Double.isFinite(alpha) && alpha >= 0.0 && alpha <= 1.0, "alpha out of range: " + alpha);
        }
        double[] expected = {0.0, 1.0, 1.0, 0.5, 0.5, 0.1, 0.1, 0.0, 0.0};
        assertEquals(expected.length, alphas.size());
        for (int i = 0; i < expected.length; i++) {
            assertEquals(
                expected[i],
                alphas.get(i),
                1.0e-9,
                "rewinds must hold the last-seen state, never run fades backwards (frame " + i + ")"
            );
        }
    }

    @Test
    @SuppressWarnings("NullAway")
    void rejectsInvalidConfigAndTimes() {
        assertThrows(IllegalArgumentException.class, () -> VisibilityTracker.Config.of(0, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> VisibilityTracker.Config.of(-1, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> VisibilityTracker.Config.of(Double.NaN, 1, 1));
        assertThrows(IllegalArgumentException.class, () -> VisibilityTracker.Config.of(1, 0, 1));
        assertThrows(IllegalArgumentException.class, () -> VisibilityTracker.Config.of(1, Double.POSITIVE_INFINITY, 1));
        assertThrows(IllegalArgumentException.class, () -> VisibilityTracker.Config.of(1, 1, 0));
        assertThrows(IllegalArgumentException.class, () -> VisibilityTracker.Config.of(1, 1, -1));
        assertThrows(NullPointerException.class, () -> new VisibilityTracker(null));

        VisibilityTracker tracker = tracker(1, 1, 1);
        assertThrows(IllegalArgumentException.class, () -> tracker.alpha(Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> tracker.setPresent(true, Double.NEGATIVE_INFINITY));
    }
}
