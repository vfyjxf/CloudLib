package dev.vfyjxf.cloudlib.api.ui.inworld.anim;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GuideLineAnimationTest {

    private static final double frame = 1.0 / 60.0;

    private static final GuideLineAnimation.Config config = GuideLineAnimation.Config.ofDefaults();

    private static GuideLineAnimation.Sample shown(GuideLineAnimation animation, double lengthPx) {
        GuideLineAnimation.Sample sample = null;
        for (int i = 0; i < 60; i++) {
            sample = animation.advance(true, lengthPx, 7L, false, frame);
        }
        return sample;
    }

    @Test
    void theEntryGrowsFromThePanelEndOverItsDuration() {
        GuideLineAnimation animation = new GuideLineAnimation(config);
        double seconds = config.enterSeconds(400) + config.staggerSeconds();
        int frames = (int) Math.ceil(seconds / frame) + 1;
        List<Double> arc = new ArrayList<>();
        for (int i = 0; i < frames; i++) {
            GuideLineAnimation.Sample sample = animation.advance(true, 400, 1L, false, frame);
            if (sample.visible()) {
                assertEquals(0.0, sample.arcStart(), 1.0e-9, "the growth is anchored at the panel end");
                arc.add(sample.arcEnd());
            }
        }

        assertFalse(arc.isEmpty(), "the line becomes visible while growing");
        assertTrue(arc.get(arc.size() - 1) > 0.98, "grown in by the end of the window: " + arc.get(arc.size() - 1));
        for (int i = 1; i < arc.size(); i++) {
            assertTrue(arc.get(i) >= arc.get(i - 1) - 1.0e-9, "the reveal only ever grows");
        }
        assertEquals(GuideLineAnimation.Phase.shown, animation.phase());
    }

    @Test
    void theEntryDurationFollowsTheLengthInsideItsWindow() {
        assertEquals(0.12, config.enterSeconds(0), 1.0e-9, "a short stroke takes the floor");
        assertEquals(200 / 900.0, config.enterSeconds(200), 1.0e-9, "200 px at 900 px/s");
        assertEquals(0.32, config.enterSeconds(1000), 1.0e-9, "a long stroke takes the ceiling");
    }

    @Test
    void thePanelLeadsAndTheLineFollowsByTheStaggerDelay() {
        GuideLineAnimation animation = new GuideLineAnimation(config);

        // two frames in — the stagger is 50 ms, so nothing has been drawn yet
        GuideLineAnimation.Sample early = animation.advance(true, 400, 1L, false, frame);
        early = animation.advance(true, 400, 1L, false, frame);
        assertFalse(early.visible(), "the line waits out the stagger");
        assertEquals(GuideLineAnimation.Phase.entering, animation.phase());

        GuideLineAnimation.Sample later = null;
        for (int i = 0; i < 5; i++) {
            later = animation.advance(true, 400, 1L, false, frame);
        }
        assertTrue(later.visible(), "and grows once the delay has elapsed");
    }

    @Test
    void theExitIsQuickerThanTheEntryAndRetractsIntoTheWorldEnd() {
        GuideLineAnimation animation = new GuideLineAnimation(config);
        shown(animation, 400);

        int frames = (int) Math.ceil(config.exitSeconds() / frame);
        double previousStart = 0;
        GuideLineAnimation.Sample sample = null;
        for (int i = 0; i < frames; i++) {
            sample = animation.advance(false, 400, 1L, false, frame);
            assertTrue(sample.arcEnd() >= 1.0 - 1.0e-9, "the world end holds while the tail retracts");
            assertTrue(sample.arcStart() >= previousStart - 1.0e-9, "the retract only ever advances");
            previousStart = sample.arcStart();
        }

        assertTrue(config.exitSeconds() < config.enterSeconds(400), "the exit is the quicker of the two");
        assertTrue(sample.arcStart() > 0.9, "retracted by the end of the window: " + sample.arcStart());
        GuideLineAnimation.Sample gone = animation.advance(false, 400, 1L, false, frame);
        assertFalse(gone.visible());
        assertEquals(GuideLineAnimation.Phase.hidden, animation.phase());
    }

    @Test
    void anInterruptedEntryUnwindsIntoThePanel() {
        GuideLineAnimation animation = new GuideLineAnimation(config);
        // grow about a third of the way, then pull the leader
        for (int i = 0; i < 10; i++) {
            animation.advance(true, 400, 1L, false, frame);
        }
        GuideLineAnimation.Sample mid = animation.advance(true, 400, 1L, false, 0.0);
        assertTrue(mid.arcEnd() > 0.0 && mid.arcEnd() < 1.0, "mid-growth: " + mid.arcEnd());

        GuideLineAnimation.Sample pulling = animation.advance(false, 400, 1L, false, frame * 2);
        assertEquals(0.0, pulling.arcStart(), 1.0e-9, "an unwinding entry never jumps to the other half");
        assertTrue(pulling.arcEnd() <= mid.arcEnd(), "the drawn arc only shrinks");
    }

    @Test
    void aReturnInsideTheWindowRestoresAlphaWithoutReplayingTheGrowth() {
        GuideLineAnimation animation = new GuideLineAnimation(config);
        shown(animation, 400);
        for (int i = 0; i < 6; i++) {
            animation.advance(false, 400, 1L, false, frame);
        }

        GuideLineAnimation.Sample back = animation.advance(true, 400, 1L, false, frame);
        assertTrue(back.visible());
        assertEquals(0.0, back.arcStart(), 1.0e-9);
        assertEquals(1.0, back.arcEnd(), 1.0e-9, "the geometry is restored whole, not grown again");
        assertTrue(back.alpha() < 1.0, "only the alpha is ramping: " + back.alpha());

        GuideLineAnimation.Sample settled = back;
        for (int i = 0; i < 20; i++) {
            settled = animation.advance(true, 400, 1L, false, frame);
        }
        assertEquals(1.0, settled.alpha(), 1.0e-9, "the ramp completes");
        assertEquals(1.0, settled.arcEnd(), 1.0e-9);
    }

    @Test
    void aReturnAfterTheWindowReplaysTheGrowth() {
        GuideLineAnimation animation = new GuideLineAnimation(config);
        shown(animation, 400);
        for (double t = 0; t < config.respawnSeconds() + 0.1; t += frame) {
            animation.advance(false, 400, 1L, false, frame);
        }

        GuideLineAnimation.Sample back = animation.advance(true, 400, 1L, false, frame);
        assertFalse(back.visible(), "the growth restarts from nothing");
        GuideLineAnimation.Sample growing = null;
        for (int i = 0; i < 6; i++) {
            growing = animation.advance(true, 400, 1L, false, frame);
        }
        assertTrue(growing.arcEnd() > 0.0 && growing.arcEnd() < 1.0, "growing again: " + growing.arcEnd());
    }

    @Test
    void hoverSwellsAndRelaxesOverItsTransition() {
        GuideLineAnimation animation = new GuideLineAnimation(config);
        shown(animation, 400);

        GuideLineAnimation.Sample partial = animation.advance(true, 400, 1L, true, config.hoverSeconds() * 0.5);
        assertEquals(0.5, partial.hover(), 0.05, "half the transition is half the swell");
        GuideLineAnimation.Sample full = animation.advance(true, 400, 1L, true, config.hoverSeconds());
        assertEquals(1.0, full.hover(), 1.0e-9);
        // hover never gates visibility
        assertTrue(full.visible());

        GuideLineAnimation.Sample relaxed = animation.advance(true, 400, 1L, false, config.hoverSeconds());
        assertEquals(0.0, relaxed.hover(), 1.0e-9);
    }

    @Test
    void aChangedRouteOpensTheLayoutSettle() {
        GuideLineAnimation animation = new GuideLineAnimation(config);
        for (int i = 0; i < 60; i++) {
            animation.advance(true, 400, 7L, false, frame);
        }
        assertEquals(1.0, animation.advance(true, 400, 7L, false, 0.0).morph(), 1.0e-9);

        GuideLineAnimation.Sample changed = animation.advance(true, 400, 8L, false, frame);
        assertEquals(
                frame / config.yieldSeconds(),
                changed.morph(),
                1.0e-9,
                "a new shape starts the settle from the old one, on the frame it changes");
        GuideLineAnimation.Sample half = animation.advance(true, 400, 8L, false, config.yieldSeconds() * 0.5 - frame);
        assertTrue(half.morph() > 0.3 && half.morph() < 0.7, "settling: " + half.morph());
        GuideLineAnimation.Sample settled = animation.advance(true, 400, 8L, false, config.yieldSeconds());
        assertEquals(1.0, settled.morph(), 1.0e-9);
        assertTrue(config.yieldSeconds() <= 0.1, "the yield stays inside its 100 ms budget");
    }

    @Test
    void theStateMachineIsReplayStableAndResettable() {
        List<Double> one = replay();
        List<Double> two = replay();
        assertEquals(one, two, "same dt sequence, same envelope");

        GuideLineAnimation animation = new GuideLineAnimation(config);
        shown(animation, 400);
        animation.reset();
        assertEquals(GuideLineAnimation.Phase.hidden, animation.phase());
        assertFalse(animation.advance(false, 400, 1L, false, 0.0).visible());
    }

    private static List<Double> replay() {
        GuideLineAnimation animation = new GuideLineAnimation(config);
        List<Double> trace = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            long geometry = i < 20 ? 1L : 2L;
            boolean present = i % 7 != 0;
            boolean hovered = i > 25;
            GuideLineAnimation.Sample sample = animation.advance(present, 300, geometry, hovered, frame);
            trace.add(sample.arcStart());
            trace.add(sample.arcEnd());
            trace.add(sample.alpha());
            trace.add(sample.hover());
            trace.add(sample.morph());
        }
        return trace;
    }

    @Test
    void theAntsFlowAtTheBaseSpeedAndAccelerateOnHover() {
        GuideLineAnimation animation = new GuideLineAnimation(config);
        // 60 frames at 1/60 s is 1.0 s: two and a half 400 ms cycles, so the
        // pattern stands half a period along
        shown(animation, 400);
        double halfPeriod = animation.advance(true, 400, 7L, false, 0.0).dashPhasePx();
        assertEquals(config.dashPeriodPx() * 0.5, halfPeriod, 1.0e-6, "half a cycle of travel");

        // a whole cycle advances the pattern by exactly one period — the phase
        // is an uniform, never geometry
        double after = animation
                .advance(true, 400, 7L, false, config.dashCycleSeconds())
                .dashPhasePx();
        assertEquals(halfPeriod, after, 1.0e-6);

        // and the integration matches the closed form at a constant speed
        GuideLineAnimation alone = new GuideLineAnimation(config);
        double step = config.dashCycleSeconds() / 8;
        double integrated = 0;
        for (int i = 0; i < 8; i++) {
            integrated = alone.advance(true, 400, 1L, false, step).dashPhasePx();
        }
        assertEquals(
                GuideLineSdf.dashPhasePx(
                        config.dashCycleSeconds(), config.dashPeriodPx(), config.dashCycleSeconds(), 1),
                integrated,
                1.0e-6);
    }

    @Test
    void hoverAcceleratesTheAntsWithoutJumpingTheirPhase() {
        GuideLineAnimation animation = new GuideLineAnimation(config);
        shown(animation, 400);
        animation.advance(true, 400, 7L, false, config.dashCycleSeconds() * 0.25);
        double before = animation.advance(true, 400, 7L, false, 0.0).dashPhasePx();
        double step = config.dashCycleSeconds() * 0.05;
        double baseStep = step / config.dashCycleSeconds() * config.dashPeriodPx();

        // the first hovered frame continues from where the phase stood: the
        // hover scales the rate, it never re-seeds the pattern
        double hovered = animation.advance(true, 400, 7L, true, step).dashPhasePx();
        assertTrue(hovered > before, "the ants keep flowing");
        assertTrue(hovered < before + baseStep * config.dashHoverSpeed(), "the swell is still ramping up");

        // once the swell has settled the flow runs at the hover speed
        for (int i = 0; i < 30; i++) {
            animation.advance(true, 400, 7L, true, config.hoverSeconds());
        }
        double beforeFast = animation.advance(true, 400, 7L, true, 0.0).dashPhasePx();
        double fast = animation.advance(true, 400, 7L, true, step).dashPhasePx();
        assertEquals(beforeFast + baseStep * config.dashHoverSpeed(), fast, 1.0e-9);
        assertTrue(config.dashHoverSpeed() >= 1.5 && config.dashHoverSpeed() <= 2.0, "the hover flow is 1.5–2×");
    }

    @Test
    void rejectsInvalidTimingsAndDeltas() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new GuideLineAnimation.Config(0, 0.12, 0.32, 0.17, 0.25, 0.125, 0.09, 0.05, 8, 0.4, 1.75));
        assertThrows(
                IllegalArgumentException.class,
                () -> new GuideLineAnimation.Config(900, 0.4, 0.3, 0.17, 0.25, 0.125, 0.09, 0.05, 8, 0.4, 1.75));
        assertThrows(
                IllegalArgumentException.class,
                () -> new GuideLineAnimation.Config(900, 0.12, 0.32, 0, 0.25, 0.125, 0.09, 0.05, 8, 0.4, 1.75));
        assertThrows(
                IllegalArgumentException.class,
                () -> new GuideLineAnimation.Config(900, 0.12, 0.32, 0.17, -1, 0.125, 0.09, 0.05, 8, 0.4, 1.75));
        assertThrows(
                IllegalArgumentException.class,
                () -> new GuideLineAnimation.Config(900, 0.12, 0.32, 0.17, 0.25, 0, 0.09, 0.05, 8, 0.4, 1.75));
        assertThrows(
                IllegalArgumentException.class,
                () -> new GuideLineAnimation.Config(900, 0.12, 0.32, 0.17, 0.25, 0.125, -1, 0.05, 8, 0.4, 1.75));
        assertThrows(
                IllegalArgumentException.class,
                () -> new GuideLineAnimation.Config(900, 0.12, 0.32, 0.17, 0.25, 0.125, 0.09, 0.05, -8, 0.4, 1.75));
        assertThrows(
                IllegalArgumentException.class,
                () -> new GuideLineAnimation.Config(900, 0.12, 0.32, 0.17, 0.25, 0.125, 0.09, 0.05, 8, 0, 1.75));
        assertThrows(
                IllegalArgumentException.class,
                () -> new GuideLineAnimation.Config(900, 0.12, 0.32, 0.17, 0.25, 0.125, 0.09, 0.05, 8, 0.4, 0.5));

        GuideLineAnimation animation = new GuideLineAnimation(config);
        assertThrows(IllegalArgumentException.class, () -> animation.advance(true, 100, 1L, false, Double.NaN));
        assertThrows(IllegalArgumentException.class, () -> animation.advance(true, 100, 1L, false, -0.1));
    }
}
