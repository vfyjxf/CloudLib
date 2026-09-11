package dev.vfyjxf.cloudlib.ui.inworld;

import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Coverage of the pure selection/layout rules in {@link InworldLayout}:
 * soft-focus cone scoring and same-edge indicator spreading.
 */
class InworldLayoutTest {

    private static final Vec3 EYE = Vec3.ZERO;
    /** Camera looking down -Z. */
    private static final Vec3 LOOK = new Vec3(0, 0, -1);

    @Test
    void deadAheadScoresBest() {
        double ahead = InworldLayout.softFocusScore(EYE, LOOK, new Vec3(0, 0, -5));
        assertEquals(0, ahead, 1e-6);
    }

    @Test
    void insideConeBeatsOutside() {
        double inside = InworldLayout.softFocusScore(EYE, LOOK, new Vec3(1, 0, -4)); //~14°
        double edge = InworldLayout.softFocusScore(EYE, LOOK, new Vec3(2.9, 0, -4)); //~36°
        assertTrue(inside >= 0);
        assertTrue(edge < 0);
    }

    @Test
    void behindCameraRejected() {
        assertTrue(InworldLayout.softFocusScore(EYE, LOOK, new Vec3(0, 0, 5)) < 0);
    }

    @Test
    void nearerToAxisWins() {
        double small = InworldLayout.softFocusScore(EYE, LOOK, new Vec3(0.2, 0, -4));
        double large = InworldLayout.softFocusScore(EYE, LOOK, new Vec3(1.5, 0, -4));
        assertTrue(small >= 0 && large >= 0);
        assertTrue(small < large);
    }

    @Test
    void spreadKeepsGap() {
        double[] tan = {10, 11, 12};
        InworldLayout.spreadEdgeSlots(tan, 20, 400, 26);
        //first stays (10 < lo → whole run shifted to lo)
        assertEquals(20, tan[0], 1e-6);
        assertEquals(46, tan[1], 1e-6);
        assertEquals(72, tan[2], 1e-6);
    }

    @Test
    void spreadKeepsOrderAndShiftsBack() {
        double[] tan = {390, 395};
        InworldLayout.spreadEdgeSlots(tan, 20, 400, 26);
        //overflow at hi → run shifted so last lands on hi
        assertEquals(400, tan[1], 1e-6);
        assertEquals(374, tan[0], 1e-6);
        assertTrue(tan[0] < tan[1]);
    }

    @Test
    void spreadUntouchedWhenAlreadySpaced() {
        double[] tan = {50, 100, 150};
        InworldLayout.spreadEdgeSlots(tan, 20, 400, 26);
        assertEquals(50, tan[0], 1e-6);
        assertEquals(100, tan[1], 1e-6);
        assertEquals(150, tan[2], 1e-6);
    }

    @Test
    void spreadShrinksGapWhenCrowded() {
        double[] tan = new double[20];
        for (int i = 0; i < tan.length; i++) tan[i] = 100;
        InworldLayout.spreadEdgeSlots(tan, 20, 120, 26);
        //19 gaps must fit inside 100 → gap ~5.26, first pinned at lo
        assertEquals(20, tan[0], 1e-6);
        assertEquals(120, tan[19], 1e-6);
        for (int i = 1; i < tan.length; i++) {
            assertTrue(tan[i] > tan[i - 1]);
        }
    }

    //---- occlusion tolerance ----

    private static final List<Rect2i> PANEL = List.of(new Rect2i(100, 100, 80, 40));

    @Test
    void noOverlapIsTolerable() {
        var oc = InworldLayout.occlusion(10, 10, 40, 12, PANEL);
        assertTrue(oc.tolerable(40, 12));
        assertNull(oc.blocker());
    }

    @Test
    void thinGrazeIsTolerated() {
        //tag bottom 3px dips under the panel's top — a sliver, keep it
        var oc = InworldLayout.occlusion(100, 91, 40, 12, PANEL); //y 91..103 vs panel 100..140 → 3px graze
        assertTrue(oc.tolerable(40, 12));
    }

    @Test
    void smallCornerOverlapIsTolerated() {
        var oc = InworldLayout.occlusion(70, 95, 40, 12, PANEL); //10x7 corner bite ≈ 14.6% area
        assertTrue(oc.tolerable(40, 12));
    }

    @Test
    void deepCoverageIsNotTolerated() {
        var oc = InworldLayout.occlusion(110, 105, 40, 12, PANEL); //mostly inside the panel
        assertFalse(oc.tolerable(40, 12));
        assertNotNull(oc.blocker());
    }

    @Test
    void dominantBlockerIsLargestContributor() {
        var rects = List.of(
                new Rect2i(100, 100, 80, 40),   //huge overlap
                new Rect2i(80, 100, 20, 10));   //small side graze
        var oc = InworldLayout.occlusion(85, 102, 40, 12, rects);
        //85..125: 25x12=300 covered by the panel, 15x8=120 by the graze
        assertEquals(100, oc.blocker().getX());
    }

}
