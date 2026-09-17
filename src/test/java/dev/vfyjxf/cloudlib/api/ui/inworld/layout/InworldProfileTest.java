package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.inworld.algorithm.AlgorithmProfile;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.CoordinationResult;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.VisibilityTracker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Every built-in profile, headless: assembled through the
 * {@link PipelineAssembler} and driven by a real {@code InworldCoordinator}
 * — facet presets validate, proposals land, placements grant at the
 * strongest rung, and each profile's signature behavior shows up in the
 * granted placement.
 */
class InworldProfileTest {

    private static final float anchorX = 200.0f;

    @Test
    void everyProfilePresetValidates() {
        for (InworldProfile profile : InworldProfile.values()) {
            ElementFacets facets = profile.facets();
            assertNotNull(facets.anchor(), profile.name());
            assertNotNull(facets.orientation(), profile.name());
            assertNotNull(facets.spaces(), profile.name());
            assertNotNull(facets.avoidance(), profile.name());
            assertNotNull(facets.stability(), profile.name());
            assertNotNull(facets.degrade().ladder().strongest(), profile.name());
            assertNotNull(facets.group(), profile.name());
            ElementSpec.from(profile, "spec-" + profile.name());
        }
    }

    @Test
    void everyProfileBindsAnAlgorithmCombination() {
        assertEquals(AlgorithmProfile.nameplate, InworldProfile.nameplate.algorithm());
        assertEquals(AlgorithmProfile.facePanel, InworldProfile.facePanel.algorithm());
        assertEquals(AlgorithmProfile.facePanel, InworldProfile.ground.algorithm());
        assertEquals(AlgorithmProfile.facePanel, InworldProfile.hologram.algorithm());
        assertEquals(AlgorithmProfile.nameplate, InworldProfile.follow.algorithm());
        assertEquals(AlgorithmProfile.orbit, InworldProfile.orbit.algorithm());
        assertEquals(AlgorithmProfile.dock, InworldProfile.dock.algorithm());
        assertEquals(AlgorithmProfile.waypoint, InworldProfile.waypoint.algorithm());
        assertEquals(AlgorithmProfile.transientUi, InworldProfile.transientUi.algorithm());
        assertEquals(AlgorithmProfile.excentric, InworldProfile.excentric.algorithm());
    }

    @Test
    void nameplateLandsOnItsOrbitSlot() {
        LayoutHarness harness = new LayoutHarness();
        AssembledElement element = harness.assemble(ElementSpec.from(InworldProfile.nameplate, "np"));
        harness.register(element);
        CoordinationResult result = harness.frame(LayoutHarness.anchored(LayoutHarness.pos(200, 150)), element);

        InworldPlacement placement = result.placementOf("np");
        assertNotNull(placement);
        assertEquals(0, placement.variant().level());
        assertEquals(10.0, placement.offsetRect().x(), 0.01, "ring-0 slot 0: base radius 60 minus half the width");
        assertEquals(-13.0, placement.offsetRect().y(), 0.01);
        assertEquals(
                VisibilityTracker.Phase.appearing, result.elementState("np").phase());
    }

    @Test
    void followUsesTheSameRingDynamicsAtPanelWidth() {
        LayoutHarness harness = new LayoutHarness();
        AssembledElement element = harness.assemble(ElementSpec.from(InworldProfile.follow, "fp"));
        harness.register(element);
        CoordinationResult result = harness.frame(LayoutHarness.anchored(LayoutHarness.pos(200, 150)), element);

        InworldPlacement placement = result.placementOf("fp");
        assertNotNull(placement);
        assertEquals(10.0, placement.offsetRect().x(), 0.01, "base radius 80 minus half the 140-wide panel");
        assertEquals(-50.0, placement.offsetRect().y(), 0.01);
    }

    @Test
    void orbitRingCandidatesLayOutAroundTheAnchor() {
        LayoutHarness harness = new LayoutHarness();
        AssembledElement element = harness.assemble(ElementSpec.from(InworldProfile.orbit, "ring"));
        harness.register(element);
        CoordinationResult result = harness.frame(LayoutHarness.anchored(LayoutHarness.pos(200, 150)), element);

        InworldPlacement placement = result.placementOf("ring");
        assertNotNull(placement);
        assertEquals(10.0, placement.offsetRect().x(), 0.01, "base radius 58 minus half the 96-wide row");
        assertEquals(-14.0, placement.offsetRect().y(), 0.01);
    }

    @Test
    void facePanelAndWorldDecalsAnchorSquarely() {
        LayoutHarness harness = new LayoutHarness();
        AssembledElement face = harness.assemble(ElementSpec.from(InworldProfile.facePanel, "face"));
        AssembledElement ground = harness.assemble(ElementSpec.from(InworldProfile.ground, "ground"));
        AssembledElement hologram = harness.assemble(ElementSpec.from(InworldProfile.hologram, "holo"));
        harness.register(face);
        harness.register(ground);
        harness.register(hologram);
        CoordinationResult result =
                harness.frame(LayoutHarness.anchored(LayoutHarness.pos(200, 150)), face, ground, hologram);

        InworldPlacement facePlacement = result.placementOf("face");
        assertNotNull(facePlacement);
        assertEquals(-60.0, facePlacement.offsetRect().x(), 0.01, "the anchored candidate centers on the anchor");
        assertEquals(-45.0, facePlacement.offsetRect().y(), 0.01);
        assertNotNull(result.placementOf("ground"));
        assertEquals(-32.0, result.placementOf("ground").offsetRect().x(), 0.01);
        assertNotNull(result.placementOf("holo"));
        assertEquals(-40.0, result.placementOf("holo").offsetRect().x(), 0.01);
    }

    @Test
    void dockCursorPlacesAlongTheNearestEdge() {
        LayoutHarness harness = new LayoutHarness();
        AssembledElement element = harness.assemble(ElementSpec.from(InworldProfile.dock, "dock"));
        harness.register(element);
        // The profile's default anchor (u 0.97, v 0.05) resolves to (388, 15)
        // — nearest edge: right, 12 px away.
        CoordinationResult result = harness.frame(LayoutHarness.unanchored(), element);

        InworldPlacement placement = result.placementOf("dock");
        assertNotNull(placement);
        assertEquals(388.0, placement.anchor().x(), 0.01, "camera-tracked anchors resolve from the screen size");
        assertEquals(15.0, placement.anchor().y(), 0.01);
        assertEquals(292.0, placement.screenRect().x(), 0.01, "margin 8 from the right edge: 400 - 8 - 100");
        assertEquals(8.0, placement.screenRect().y(), 0.01, "first-fit at the scanline origin");
    }

    @Test
    void waypointClampsItsOffscreenCandidateIntoTheWorkArea() {
        LayoutHarness harness = new LayoutHarness();
        AssembledElement offscreen = harness.assemble(ElementSpec.from(InworldProfile.waypoint, "wp-out"));
        harness.register(offscreen);
        LayoutEnvironment environment =
                LayoutHarness.unanchored().withAnchor(AnchorFrame.screen(new FloatPos(-20, 150)));
        CoordinationResult result = harness.frame(environment, offscreen);

        InworldPlacement clamped = result.placementOf("wp-out");
        assertNotNull(clamped, "the clamping ladder rung pulls the offscreen waypoint to the edge");
        assertTrue(clamped.screenRect().x() >= 0.0, "clamped into the work area: " + clamped.screenRect());
        assertTrue(clamped.screenRect().width() > 0.0);
    }

    @Test
    void transientUiIsGrantedEvenOverExclusions() {
        LayoutHarness harness = new LayoutHarness();
        AssembledElement element = harness.assemble(ElementSpec.from(InworldProfile.transientUi, "dmg"));
        harness.register(element);
        LayoutEnvironment environment = LayoutHarness.anchored(LayoutHarness.pos(200, 150))
                .withExclusions(List.of(new Rect(150, 125, 100, 50)));
        CoordinationResult result = harness.frame(environment, element);

        assertNotNull(result.placementOf("dmg"), "a ghost ignores exclusions and overlaps");
        assertEquals(0, result.placementOf("dmg").variant().level());
    }

    @Test
    void excentricPlacesItsColumnBesideTheFocus() {
        LayoutHarness harness = new LayoutHarness();
        AssembledElement element = harness.assemble(ElementSpec.from(InworldProfile.excentric, "hint"));
        harness.register(element);
        CoordinationResult result = harness.frame(LayoutHarness.anchored(LayoutHarness.pos(210, 150)), element);

        InworldPlacement placement = result.placementOf("hint");
        assertNotNull(placement);
        // anchor on the right half → column grows to the left: dx = -(8 + w/2)
        assertEquals(210.0 - 68.0, placement.screenRect().centerX(), 0.01);
        assertEquals(150.0, placement.screenRect().centerY(), 0.01);
    }

    @Test
    void goneAnchorRetractsIntoLingerAndRecoversSticky() {
        LayoutHarness harness = new LayoutHarness();
        AssembledElement element = harness.assemble(ElementSpec.from(InworldProfile.nameplate, "np"));
        harness.register(element);
        CoordinationResult placed = harness.frame(LayoutHarness.anchored(LayoutHarness.pos(200, 150)), element);
        assertNotNull(placed.placementOf("np"));

        CoordinationResult retracted = harness.frame(LayoutHarness.unanchored(), element);
        assertEquals(
                VisibilityTracker.Phase.lingering, retracted.elementState("np").phase());
        assertNull(retracted.placementOf("np"));

        CoordinationResult recovered = harness.frame(LayoutHarness.anchored(LayoutHarness.pos(200, 150)), element);
        assertNotNull(recovered.placementOf("np"));
        assertEquals(
                placed.placementOf("np").offsetRect().x(),
                recovered.placementOf("np").offsetRect().x(),
                0.01,
                "recovery returns to the sticky slot");
    }
}
