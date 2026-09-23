package dev.vfyjxf.cloudlib.api.ui.inworld;

import dev.vfyjxf.cloudlib.api.ui.inworld.stability.OcclusionFade;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.VisibilityPolicy;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

class OcclusionProbeTest {

    // eye at (0, 0, 5) looking at a 100×100px quad centered at the origin,
    // 0.02 world units per pixel
    private final Vec3 eye = new Vec3(0, 0, 5);
    private final Vec3 center = new Vec3(0, 0, 0);
    private final Vec3 uAxis = new Vec3(0.02, 0, 0);
    private final Vec3 vAxis = new Vec3(0, 0.02, 0);

    /** A probe whose blocked segments are keyed by their endpoint. */
    private final class ScriptedProbe implements OcclusionProbe {

        private final Set<Vec3> blockedEndpoints = new HashSet<>();
        private int segmentsSampled;

        @Override
        public boolean segmentClear(Vec3 from, Vec3 to) {
            segmentsSampled++;
            assertEquals(eye, from, "sampling must start at the eye");
            return !blockedEndpoints.contains(to);
        }

        double visibility() {
            return visibility(eye, center, uAxis, vAxis, 100, 100);
        }
    }

    @Test
    void visibilitySamplesCenterAndFourCornersInFiveSegments() {
        ScriptedProbe probe = new ScriptedProbe();

        probe.visibility();

        assertEquals(5, probe.segmentsSampled);
    }

    @Test
    void clearProbeReportsFullVisibility() {
        assertEquals(1.0, new ScriptedProbe().visibility(), 0.0);
        assertEquals(1.0, OcclusionProbe.alwaysClear().visibility(eye, center, uAxis, vAxis, 100, 100), 0.0);
    }

    @Test
    void fullyBlockedProbeReportsZeroVisibility() {
        assertEquals(0.0, OcclusionProbe.alwaysOccluded().visibility(eye, center, uAxis, vAxis, 100, 100), 0.0);
    }

    @Test
    void eachBlockedSegmentRemovesOneFifth() {
        ScriptedProbe probe = new ScriptedProbe();
        Vec3 u = uAxis.scale(100 * 0.5);
        Vec3 v = vAxis.scale(100 * 0.5);

        probe.blockedEndpoints.add(center);
        assertEquals(0.8, probe.visibility(), 1.0e-9);

        probe.blockedEndpoints.add(center.add(u).add(v));
        assertEquals(0.6, probe.visibility(), 1.0e-9);

        probe.blockedEndpoints.add(center.add(u).subtract(v));
        probe.blockedEndpoints.add(center.subtract(u).add(v));
        probe.blockedEndpoints.add(center.subtract(u).subtract(v));
        assertEquals(0.0, probe.visibility(), 1.0e-9);
    }

    @Test
    void segmentEndpointsAreTheQuadCorners() {
        // the default sampling must query exactly the center and the four
        // corner endpoints implied by uAxis·w/2 and vAxis·h/2
        ScriptedProbe probe = new ScriptedProbe();
        Vec3 u = uAxis.scale(100 * 0.5);
        Vec3 v = vAxis.scale(100 * 0.5);
        probe.blockedEndpoints.add(center.add(u).add(v));
        probe.blockedEndpoints.add(center.add(u).subtract(v));
        probe.blockedEndpoints.add(center.subtract(u).add(v));
        probe.blockedEndpoints.add(center.subtract(u).subtract(v));

        assertEquals(0.2, probe.visibility(), 1.0e-9);
    }

    @Test
    void standardFadeCarriesThePlannedBaseline() {
        assertEquals(0.25, OcclusionFade.standard.exitThreshold(), 0.0);
        assertEquals(0.55, OcclusionFade.standard.enterThreshold(), 0.0);
        assertEquals(5, OcclusionFade.standard.frames());
    }

    @Test
    void fadeValidatesItsThresholdBandAndFrameGate() {
        assertThrows(IllegalArgumentException.class, () -> new OcclusionFade(-0.1, 0.55, 5));
        assertThrows(IllegalArgumentException.class, () -> new OcclusionFade(0.6, 0.55, 5));
        assertThrows(IllegalArgumentException.class, () -> new OcclusionFade(0.55, 0.55, 5));
        assertThrows(IllegalArgumentException.class, () -> new OcclusionFade(0.25, 1.1, 5));
        assertThrows(IllegalArgumentException.class, () -> new OcclusionFade(0.25, 0.55, 0));

        // the full valid band constructs
        new OcclusionFade(0.0, 1.0, 1);
        new OcclusionFade(0.25, 0.55, 12);
    }

    @Test
    void theParameterizedTargetHonorsItsOwnDimmedAlpha() {
        // the same policy math at a caller's alpha: the shipped constant and
        // the parameter agree on the shape, differ only in the floor
        assertEquals(
            OcclusionFade.occludedAlpha,
            OcclusionFade.target(VisibilityPolicy.fade, true, false, false),
            0.0f
        );
        assertEquals(0.5f, OcclusionFade.target(VisibilityPolicy.fade, true, false, false, 0.5f), 0.0f);
        assertEquals(1f, OcclusionFade.target(VisibilityPolicy.fade, true, true, false, 0.5f), "selected holds");
        assertEquals(1f, OcclusionFade.target(VisibilityPolicy.fade, true, false, true, 0.5f), "inspecting holds");
        assertEquals(1f, OcclusionFade.target(VisibilityPolicy.hardOcclusion, true, false, false, 0.5f), "fade only");
    }

    @Test
    void theParameterizedStepRunsAtItsOwnTransitionLength() {
        // a 0.2 s step at dt = 0.05 moves a quarter of the scale; a 0.5 s
        // transition moves a tenth — the shipped form and the parameterized
        // one agree at the shipped constant
        assertEquals(OcclusionFade.step(1f, 0f, 0.05), OcclusionFade.step(1f, 0f, 0.05, 0.2), 0.0f);
        assertEquals(0.9f, OcclusionFade.step(1f, 0f, 0.05, 0.5), 1.0e-6);
        assertEquals(0.75f, OcclusionFade.step(1f, 0f, 0.05, 0.2), 1.0e-6);
        assertThrows(IllegalArgumentException.class, () -> OcclusionFade.step(1f, 0f, 0.05, 0.0));
        assertThrows(IllegalArgumentException.class, () -> OcclusionFade.step(1f, 0f, 0.05, -0.1));
    }
}
