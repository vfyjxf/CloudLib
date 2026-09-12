package dev.vfyjxf.inworldui.internal;

import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage of {@link ExpandPlacer}: the keep-fast-path, the absolute rescan
 * margin, and the show/hide deadband — the rules that keep holograms glued in
 * place instead of wandering between near-tied spots.
 */
class ExpandPlacerTest {

    //---- keep fast path ----

    @Test
    void clearSpotIsKept() {
        assertTrue(ExpandPlacer.keepSpot(false, 0.0, 10));
        assertTrue(ExpandPlacer.keepSpot(false, ExpandPlacer.KEEP_COVER, 100));
    }

    @Test
    void contestedSpotRescans() {
        //another hologram claimed this volume — keeping it would stack quads
        assertFalse(ExpandPlacer.keepSpot(true, 0.0, 0));
    }

    @Test
    void heavyCoverRescans() {
        assertFalse(ExpandPlacer.keepSpot(false, ExpandPlacer.KEEP_COVER + 0.01, 100));
    }

    @Test
    void terribleScoreRescans() {
        //e.g. the spot filled with solid blocks since last frame
        assertFalse(ExpandPlacer.keepSpot(false, 0.0, ExpandPlacer.KEEP_SCORE));
    }

    //---- rescan margin ----

    @Test
    void challengerNeedsAbsoluteMargin() {
        //exactly at the margin the incumbent still wins — the margin is a
        //"clearly better" bar, not a coin flip
        assertTrue(ExpandPlacer.preferCurrent(100, 100 - ExpandPlacer.MARGIN));
        assertFalse(ExpandPlacer.preferCurrent(100, 100 - ExpandPlacer.MARGIN - 1));
    }

    @Test
    void noisyScoresDontFlipChoice() {
        //a challenger hovering just inside the margin, jittering ±20 per frame,
        //never displaces the incumbent
        for (double noise = -20; noise <= 20; noise++) {
            assertTrue(ExpandPlacer.preferCurrent(200, 200 - ExpandPlacer.MARGIN * 0.5 + noise),
                    "noise " + noise + " flipped the incumbent");
        }
        //and a challenger that's clearly better always wins
        assertFalse(ExpandPlacer.preferCurrent(200, 200 - ExpandPlacer.MARGIN - 1));
    }

    @Test
    void clearlyBetterChallengerWins() {
        assertFalse(ExpandPlacer.preferCurrent(400, 100));
    }

    //---- hide hysteresis ----

    @Test
    void shownHidesAboveHideCover() {
        assertTrue(ExpandPlacer.shouldHide(ExpandPlacer.HIDE_COVER + 0.01, false));
        assertFalse(ExpandPlacer.shouldHide(ExpandPlacer.HIDE_COVER, false));
    }

    @Test
    void hiddenStaysHiddenThroughDeadband() {
        //between SHOW and HIDE the hidden panel stays hidden — no flicker
        assertTrue(ExpandPlacer.shouldHide(0.20, true));
        assertTrue(ExpandPlacer.shouldHide(ExpandPlacer.SHOW_COVER + 0.001, true));
        assertFalse(ExpandPlacer.shouldHide(ExpandPlacer.SHOW_COVER, true));
    }

    @Test
    void deadbandSeparatesEdges() {
        assertTrue(ExpandPlacer.SHOW_COVER < ExpandPlacer.HIDE_COVER);
        //inside the deadband the outcome depends on prior state — the anti-flicker
        double mid = (ExpandPlacer.SHOW_COVER + ExpandPlacer.HIDE_COVER) * 0.5;
        assertTrue(ExpandPlacer.shouldHide(mid, true));
        assertFalse(ExpandPlacer.shouldHide(mid, false));
    }

}
