package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SupersamplingTest {

    private static final double eps = 1.0e-3; // projection matrices are float-precision

    // region desired

    @Test
    void desiredMatchesOneTexelPerFramebufferPixel() {
        // a 100px-tall panel projected at 300 gui px on a guiScale-2 window
        // spans 600 framebuffer px → 6 texels per logical pixel
        assertEquals(6, Supersampling.desired(300 * 2, 100, 2));
        assertEquals(4, Supersampling.desired(400, 100, 2));
        assertEquals(1, Supersampling.desired(100, 100, 1));
    }

    @Test
    void desiredQuantizesToWholeFactors() {
        assertEquals(1, Supersampling.desired(149, 100, 1));
        assertEquals(2, Supersampling.desired(151, 100, 1));
        // Math.round halves up
        assertEquals(3, Supersampling.desired(250, 100, 1));
    }

    @Test
    void desiredClampsToUpperBound() {
        assertEquals(Supersampling.maxSupersample, Supersampling.desired(10_000, 100, 2));
        assertEquals(Supersampling.maxSupersample, Supersampling.desired(Double.MAX_VALUE, 1, 1));
    }

    @Test
    void profileFloorIsALowerBound() {
        // an explicit user floor of 3 is a minimum, not a target
        assertEquals(3, Supersampling.desired(10, 100, 3));
        assertEquals(3, Supersampling.desired(300, 100, 3));
        // but never caps growth
        assertEquals(5, Supersampling.desired(500, 100, 3));
        // the default floor of 2 keeps distant panels from aliasing
        assertEquals(2, Supersampling.desired(10, 100, 2));
    }

    @Test
    void desiredClampsRunawayFloorToMax() {
        assertEquals(Supersampling.maxSupersample, Supersampling.desired(10, 100, 99));
        assertEquals(Supersampling.maxSupersample, Supersampling.desired(10_000, 100, 99));
    }

    @Test
    void desiredReportsNoObservationOnInvalidInput() {
        // a degenerate projection is "no data this frame", not "no
        // magnification": 0 lets the caller's SupersampleController hold its
        // current factor (desired ≤ 0 = no observation) instead of reading
        // the floor and resizing the surface
        assertEquals(0, Supersampling.desired(0, 100, 2));
        assertEquals(0, Supersampling.desired(-5, 100, 2));
        assertEquals(0, Supersampling.desired(Double.NaN, 100, 2));
        assertEquals(0, Supersampling.desired(200, 0, 2));
        assertEquals(0, Supersampling.desired(200, -1, 2));
    }

    @Test
    void degenerateProjectionHoldsTheControllerFactor() {
        // the composition WorldUiRenderer drives: a quad going edge-on (or a
        // corner crossing the near plane) yields no projection for a few
        // frames — the panel must keep its granted factor, not step to the
        // floor and back (one wasted resize each way)
        SupersampleController controller = new SupersampleController(2);
        assertEquals(4, controller.observe(Supersampling.desired(400, 100, 2)));
        assertEquals(0, Supersampling.desired(0, 100, 2));
        assertEquals(4, controller.observe(Supersampling.desired(0, 100, 2)));
        assertEquals(4, controller.observe(Supersampling.desired(-1, 100, 2)));
        // and the hold never poisons a pending switch
        assertEquals(4, controller.observe(Supersampling.desired(600, 100, 2)));
        assertEquals(6, controller.observe(Supersampling.desired(600, 100, 2)));
    }

    // endregion

    // region allocate

    @Test
    void allocateWithinBudgetGrantsEveryDesired() {
        List<Supersampling.Request> requests = List.of(
            new Supersampling.Request(176, 100, 8, 2, 1_000_000),
            new Supersampling.Request(96, 44, 4, 2, 200_000)
        );
        // 17600·64 + 4224·16 ≈ 1.19M texels — far under the default budget
        int[] granted = Supersampling.allocate(requests, Supersampling.defaultTexelBudget);
        assertEquals(8, granted[0]);
        assertEquals(4, granted[1]);
    }

    @Test
    void allocateOverBudgetProtectsLargePanelsFirst() {
        // two panels wanting 1.126M texels each (176×100 at ss8) under a
        // 1.5M budget: the larger projected area keeps ss8, the smaller
        // steps down until it fits
        List<Supersampling.Request> requests = List.of(
            new Supersampling.Request(176, 100, 8, 2, 500_000),
            new Supersampling.Request(176, 100, 8, 2, 400_000)
        );
        int[] granted = Supersampling.allocate(requests, 1_500_000);
        assertEquals(8, granted[0]);
        // 17600·6² = 633.6k ≤ 1.5M − 1.126M = 374k? no → 5² = 440k? no → 4² = 281.6k ✓
        assertEquals(4, granted[1]);
        assertTrue(17600L * 64 + 17600L * 16 <= 1_500_000);
    }

    @Test
    void allocateNeverDropsBelowTheFloor() {
        // even a zero budget cannot push a panel under its floor — floors
        // outrank the budget (an all-floors frame may overshoot)
        List<Supersampling.Request> requests = List.of(
            new Supersampling.Request(176, 100, 8, 3, 500_000),
            new Supersampling.Request(96, 44, 6, 2, 100_000)
        );
        int[] granted = Supersampling.allocate(requests, 0);
        assertEquals(3, granted[0]);
        assertEquals(2, granted[1]);
    }

    @Test
    void allocateServesByDescendingScreenArea() {
        // the small-area panel is listed first but served last
        List<Supersampling.Request> requests = List.of(
            new Supersampling.Request(176, 100, 8, 1, 100_000),
            new Supersampling.Request(176, 100, 8, 1, 900_000)
        );
        int[] granted = Supersampling.allocate(requests, 1_500_000);
        assertEquals(8, granted[1]);
        assertEquals(4, granted[0]);
    }

    @Test
    void allocateTieBreaksByListOrderDeterministically() {
        Supersampling.Request a = new Supersampling.Request(176, 100, 8, 1, 500_000);
        Supersampling.Request b = new Supersampling.Request(176, 100, 8, 1, 500_000);
        int[] first = Supersampling.allocate(List.of(a, b), 1_500_000);
        int[] second = Supersampling.allocate(List.of(b, a), 1_500_000);
        // whichever comes first in the list keeps ss8
        assertEquals(8, first[0]);
        assertEquals(4, first[1]);
        assertEquals(8, second[0]);
        assertEquals(4, second[1]);
    }

    @Test
    void allocateEmptyListGrantsNothing() {
        assertEquals(0, Supersampling.allocate(List.of(), Supersampling.defaultTexelBudget).length);
    }

    // endregion

    // region mipmap

    @Test
    void mipmapNeededOnlyWhileMinifying() {
        // surface smaller than the projection → magnified: level 0 only
        assertFalse(Supersampling.needsMipmap(352, 200, 704, 400));
        assertFalse(Supersampling.needsMipmap(352, 200, 352, 200));
        // minified on either axis → the mip chain filters the shrink
        assertTrue(Supersampling.needsMipmap(352, 200, 300, 400));
        assertTrue(Supersampling.needsMipmap(352, 200, 704, 100));
    }

    @Test
    void mipmapUnknownProjectionGeneratesConservatively() {
        assertTrue(Supersampling.needsMipmap(352, 200, 0, 0));
        assertTrue(Supersampling.needsMipmap(352, 200, -1, 400));
        assertTrue(Supersampling.needsMipmap(352, 200, Double.NaN, Double.NaN));
    }

    // endregion

    // region projectQuad

    private static final Matrix4f identity = new Matrix4f();

    @Test
    void projectQuadMeasuresOrthographicExtent() {
        Matrix4f ortho = new Matrix4f().setOrtho(0, 1920, 0, 1080, 0.1f, 100f);
        QuadBasis quad = QuadBasis.of(new Vec3(0, 0, -5), new Vec3(1, 0, 0), new Vec3(0, 1, 0));
        Supersampling.ProjectedSize size = Supersampling.projectQuad(identity, ortho, quad, 100, 50, 1920, 1080);
        assertNotNull(size);
        assertEquals(100, size.widthPx(), eps);
        assertEquals(50, size.heightPx(), eps);
        assertEquals(5000, size.areaPx(), eps);
    }

    @Test
    void projectQuadScalesWithPerspectiveDistance() {
        // 90° fov, square viewport: at depth 10 the frustum cross-section is
        // 20×20 world units, so a 100×50 panel projects to 5×2.5 viewports
        Matrix4f perspective = new Matrix4f().setPerspective((float) Math.toRadians(90), 1f, 0.1f, 100f);
        QuadBasis quad = QuadBasis.of(new Vec3(-50, -25, -10), new Vec3(1, 0, 0), new Vec3(0, 1, 0));
        Supersampling.ProjectedSize size = Supersampling.projectQuad(identity, perspective, quad, 100, 50, 512, 512);
        assertNotNull(size);
        assertEquals(5 * 512, size.widthPx(), 1.0e-3);
        assertEquals(2.5 * 512, size.heightPx(), 1.0e-3);
    }

    @Test
    void projectQuadBehindCameraReturnsNull() {
        Matrix4f perspective = new Matrix4f().setPerspective((float) Math.toRadians(90), 1f, 0.1f, 100f);
        QuadBasis behind = QuadBasis.of(new Vec3(-50, -25, 10), new Vec3(1, 0, 0), new Vec3(0, 1, 0));
        assertNull(Supersampling.projectQuad(identity, perspective, behind, 100, 50, 512, 512));
        // a quad straddling the camera plane is just as invalid
        QuadBasis straddling = QuadBasis.of(new Vec3(-50, -25, -10), new Vec3(1, 0, 0), new Vec3(0, 0, 1));
        assertNull(Supersampling.projectQuad(identity, perspective, straddling, 100, 50, 512, 512));
    }

    // endregion

    // region pipeline

    @Test
    void pipelineStabilizesASmoothApproachIntoDiscreteSteps() {
        // a panel approaching the camera: 400 → 800 framebuffer px of
        // projected height over 60 frames (100px logical, floor 2), budget
        // generous. Desired crosses ~5 quantized values; the controller only
        // switches after 10 stable frames, so the applied factor forms a
        // short staircase instead of resizing the FBO every frame.
        SupersampleController controller = new SupersampleController();
        int first = -1;
        int switches = 0;
        for (int frame = 0; frame < 60; frame++) {
            double projected = 400 + frame * (400.0 / 59);
            int desired = Supersampling.desired(projected, 100, 2);
            int applied = controller.observe(desired);
            int granted = Supersampling.allocate(
                List.of(new Supersampling.Request(100, 100, applied, 2, projected * projected)),
                Supersampling.defaultTexelBudget
            )[0];
            if (applied != first) {
                switches++;
                first = applied;
            }
            assertEquals(applied, granted);
            assertTrue(applied >= 2 && applied <= Supersampling.maxSupersample);
        }
        // initial adoption + up to one step per ~14 frames of ramp
        assertTrue(switches >= 2, "the approach should step up at least once, got " + switches);
        assertTrue(switches <= 6, "expected a staircase, got " + switches + " switches");
        assertTrue(first >= 2);
    }

    // endregion
}
