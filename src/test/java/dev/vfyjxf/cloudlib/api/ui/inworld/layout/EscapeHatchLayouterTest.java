package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.CoordinationResult;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.ElementMode;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.ElementProposal;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.PlacementCandidate;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.SpaceKind;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.VisibilityTracker;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The escape hatch (§7, plan A core): an embedded {@link InworldLayouter}
 * replaces the element-side stages, declares its space posture through
 * {@code reserve}, and hears every arbitration outcome through
 * {@code arbitrated}.
 */
class EscapeHatchLayouterTest {

    /**
     * A follow-panel layouter with the occluded→sidebar transfer the facets
     * cannot express: while the anchor is clear it proposes the anchored
     * candidate; when the caller flags occlusion it re-anchors the proposal
     * to a fixed screen sidebar position.
     */
    static final class SidebarLayouter implements InworldLayouter {

        int reserveCount;
        int proposeCount;
        int arbitratedCount;
        final List<InworldLayouter.Feedback> feedbacks = new ArrayList<>();
        boolean occluded;
        boolean proposeRetract;

        @Override
        public SpaceReservation reserve(InworldLayoutContext ctx) {
            reserveCount++;
            return new SpaceReservation(SpaceKind.world, 7, true, ElementMode.arbitrated);
        }

        @Override
        public ElementProposal propose(InworldLayoutContext ctx) {
            proposeCount++;
            if (proposeRetract) {
                return ElementProposal.retract(ctx.variant());
            }
            Size variantSize = ctx.variant().requestedSize();
            FloatPos anchor = ctx.environment().anchor() != null
                    ? ctx.environment().anchor().screen()
                    : new FloatPos(200, 150);
            if (occluded) {
                FloatPos sidebar = new FloatPos(12, 150);
                return ElementProposal.of(
                        ctx.variant(),
                        sidebar,
                        PlacementCandidate.screen(
                                FloatRect.around(sidebar, variantSize.width(), variantSize.height())));
            }
            return ElementProposal.of(
                    ctx.variant(),
                    anchor,
                    PlacementCandidate.screen(FloatRect.around(anchor, variantSize.width(), variantSize.height())));
        }

        @Override
        public void arbitrated(@Nullable InworldPlacement placement, Feedback feedback) {
            arbitratedCount++;
            feedbacks.add(feedback);
        }
    }

    @Test
    void customLayouterDrivesTheWholeElementSide() {
        LayoutHarness harness = new LayoutHarness();
        SidebarLayouter layouter = new SidebarLayouter();
        AssembledElement element = harness.assemble(ElementSpec.from(InworldProfile.follow, "sidebar")
                .withAnchor(AnchorFacet.entity("cow"))
                .custom(layouter));
        harness.register(element);

        assertEquals(0, layouter.reserveCount, "reserve waits for the first frame's environment");
        CoordinationResult first = harness.frame(LayoutHarness.anchored(LayoutHarness.pos(200, 150)), element);

        assertEquals(1, layouter.reserveCount, "reserve runs once, lazily");
        assertTrue(layouter.proposeCount >= 1);
        assertEquals(1, layouter.arbitratedCount, "observe dispatches the committed result");
        InworldPlacement placement = first.placementOf("sidebar");
        assertNotNull(placement);
        assertEquals(200.0, placement.anchor().x(), 0.01);
        assertEquals(-70.0, placement.offsetRect().x(), 0.01, "the custom candidate centers on the anchor");
        assertEquals(0, placement.variant().level(), "variant-sized candidates grant the strongest rung");
        assertEquals(
                VisibilityTracker.Phase.appearing, layouter.feedbacks.getFirst().phase());
        assertEquals(SpaceKind.world, element.spaceKind());
        assertEquals(7, element.priority(), "the reserve declaration overrides the facet priority");
        assertTrue(element.sticky());

        // The occlusion transfer: the same spec, the layouter's own decision.
        layouter.occluded = true;
        CoordinationResult transferred = harness.frame(
                LayoutHarness.anchored(LayoutHarness.pos(200, 150)).at(1.0 / 60.0, 1.0 / 60.0), element);
        InworldPlacement sidebar = transferred.placementOf("sidebar");
        assertNotNull(sidebar);
        assertEquals(12.0, sidebar.anchor().x(), 0.01, "the proposal re-anchored to the sidebar");

        // Retraction through the layouter's own choice.
        layouter.proposeRetract = true;
        CoordinationResult retracted = harness.frame(
                LayoutHarness.anchored(LayoutHarness.pos(200, 150)).at(2.0 / 60.0, 1.0 / 60.0), element);
        assertNull(retracted.placementOf("sidebar"));
        assertEquals(3, layouter.arbitratedCount);
    }

    @Test
    void facetOnlySpecsIgnoreObserve() {
        LayoutHarness harness = new LayoutHarness();
        AssembledElement element = harness.assemble(ElementSpec.from(InworldProfile.nameplate, "plain"));
        harness.register(element);
        CoordinationResult result = harness.frame(LayoutHarness.anchored(LayoutHarness.pos(200, 150)), element);
        assertNotNull(result.placementOf("plain"), "observe is a no-op without a custom layouter");
    }

    @Test
    void customLayouterMayComposeCatalogStrategies() {
        StageCatalogs.CandidateStrategy orbit =
                StageCatalogs.requireCandidateStrategy(StageCatalogs.candidatesOrbitRing);
        InworldLayouter composing = new InworldLayouter() {

            @Override
            public SpaceReservation reserve(InworldLayoutContext ctx) {
                return SpaceReservation.arbitrated(SpaceKind.world);
            }

            @Override
            public ElementProposal propose(InworldLayoutContext ctx) {
                List<PlacementCandidate> slots = orbit.candidates(new StageCatalogs.CandidateContext(
                        ctx.environment().anchor().screen(),
                        ctx.variant().requestedSize(),
                        OrientationFacet.Mode.cameraBillboard,
                        0,
                        ctx.environment(),
                        ctx.spec().profile().algorithm().params(),
                        ctx.spec().avoidance().avoids()));
                return ElementProposal.of(
                        ctx.variant(), ctx.environment().anchor().screen(), slots);
            }

            @Override
            public void arbitrated(@Nullable InworldPlacement placement, Feedback feedback) {}
        };
        LayoutHarness harness = new LayoutHarness();
        AssembledElement element = harness.assemble(ElementSpec.from(InworldProfile.follow, "composed")
                .withAnchor(AnchorFacet.entity("cow"))
                .custom(composing));
        harness.register(element);
        CoordinationResult result = harness.frame(LayoutHarness.anchored(LayoutHarness.pos(200, 150)), element);
        InworldPlacement placement = result.placementOf("composed");
        assertNotNull(placement);
        assertEquals(10.0, placement.offsetRect().x(), 0.01, "the composed orbit strategy's ring-0 slot");
        assertEquals(-50.0, placement.offsetRect().y(), 0.01);
    }
}
