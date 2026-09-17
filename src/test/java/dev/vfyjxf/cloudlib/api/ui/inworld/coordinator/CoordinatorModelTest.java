package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.OccupancyBitmap;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpacePolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.SwitchGate;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.VisibilityTracker;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** Validation and derived-value behavior of the negotiation data records. */
class CoordinatorModelTest {

    @Test
    void worldAabbValidatesAndCenters() {
        WorldAabb box = WorldAabb.around(10, 20, 30, 6, 8, 4);
        assertEquals(7, box.minX(), 0);
        assertEquals(13, box.maxX(), 0);
        assertEquals(16, box.minY(), 0);
        assertEquals(24, box.maxY(), 0);
        assertEquals(28, box.minZ(), 0);
        assertEquals(32, box.maxZ(), 0);
        assertEquals(10, box.centerX(), 0);
        assertEquals(20, box.centerY(), 0);
        assertEquals(30, box.centerZ(), 0);
        assertThrows(IllegalArgumentException.class, () -> new WorldAabb(2, 0, 0, 1, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> WorldAabb.around(0, 0, 0, -1, 2, 2));
        assertThrows(IllegalArgumentException.class, () -> new WorldAabb(Double.NaN, 0, 0, 1, 0, 0));
    }

    @Test
    void placementCandidateFactoriesAndValidation() {
        FloatRect rect = new FloatRect(10, 10, 40, 20);
        WorldAabb box = WorldAabb.around(0, 0, 0, 1, 1, 1);

        assertNull(PlacementCandidate.screen(rect).world());
        assertEquals(rect, PlacementCandidate.screen(rect).screenRect());
        assertEquals(box, PlacementCandidate.dual(box, rect).world());

        assertThrows(NullPointerException.class, () -> PlacementCandidate.screen(null));
        assertThrows(IllegalArgumentException.class, () -> PlacementCandidate.screen(new FloatRect(0, 0, 0, 10)));
        assertThrows(IllegalArgumentException.class, () -> PlacementCandidate.screen(new FloatRect(0, 0, 10, 0)));
        assertThrows(NullPointerException.class, () -> PlacementCandidate.dual(null, rect));
    }

    @Test
    void inworldVariantValidates() {
        Size size = new Size(100, 40);
        InworldVariant variant = new InworldVariant(0, size, ContentTier.full, SpacePolicy.active, true, false, 2000);
        assertEquals(4000, variant.requestedArea(), 0);
        assertThrows(
                IllegalArgumentException.class,
                () -> new InworldVariant(-1, size, ContentTier.full, SpacePolicy.active, true, true, 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new InworldVariant(0, new Size(0, 10), ContentTier.full, SpacePolicy.active, true, true, 1));
        assertThrows(
                NullPointerException.class,
                () -> new InworldVariant(0, null, ContentTier.full, SpacePolicy.active, true, true, 1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new InworldVariant(0, size, ContentTier.full, SpacePolicy.active, true, true, -1));
        assertThrows(
                IllegalArgumentException.class,
                () -> new InworldVariant(0, size, ContentTier.full, SpacePolicy.active, true, true, Double.NaN));
    }

    @Test
    void elementProposalContract() {
        InworldVariant variant = TestElement.ladder(new Size(100, 40)).strongest();
        PlacementCandidate candidate = PlacementCandidate.screen(new FloatRect(0, 0, 100, 40));

        ElementProposal proposal = ElementProposal.of(variant, new FloatPos(50, 50), candidate);
        assertEquals(variant, proposal.variant());
        assertEquals(1, proposal.candidates().size());
        assertEquals(50, proposal.anchorScreen().x(), 0);
        assertTrue(!proposal.retracted());

        // the anchor position is copied: mutating the source does not leak in
        FloatPos anchor = new FloatPos(50, 50);
        ElementProposal copied = ElementProposal.of(variant, anchor, candidate);
        anchor.set(999, 999);
        assertEquals(50, copied.anchorScreen().x(), 0);

        ElementProposal retracted = ElementProposal.retract(variant);
        assertTrue(retracted.retracted());
        assertNull(retracted.anchorScreen());
        assertTrue(retracted.candidates().isEmpty());

        assertThrows(
                NullPointerException.class, () -> new ElementProposal(variant, null, List.of(candidate), false, false));
        assertThrows(
                IllegalArgumentException.class,
                () -> new ElementProposal(variant, new FloatPos(0, 0), List.of(candidate), true, false));
        assertThrows(
                NullPointerException.class,
                () -> new ElementProposal(null, new FloatPos(0, 0), List.of(), false, false));
    }

    @Test
    void spaceBudgetDerivesFromBitmapAndAnswersCapacity() {
        Rect workArea = new Rect(0, 0, 100, 100);
        OccupancyBitmap bitmap = new OccupancyBitmap(100, 100, 10);
        SpaceBudget empty = SpaceBudget.of(workArea, bitmap);
        assertEquals(10000, empty.workAreaArea(), 0);
        assertEquals(1.0, empty.freeFraction(), 1.0e-9);
        assertEquals(10, empty.capacityFor(1000));
        assertEquals(0, empty.capacityFor(10001));

        bitmap.mark(new Rect(0, 0, 100, 50));
        SpaceBudget half = SpaceBudget.of(workArea, bitmap);
        assertEquals(0.5, half.freeFraction(), 1.0e-9);
        assertEquals(5000, half.freeArea(), 1.0e-9);
        assertEquals(5, half.capacityFor(1000));

        assertThrows(IllegalArgumentException.class, () -> empty.capacityFor(0));
        assertThrows(IllegalArgumentException.class, () -> empty.capacityFor(-10));
        assertThrows(IllegalArgumentException.class, () -> new SpaceBudget(workArea, -1, 0.5));
        assertThrows(IllegalArgumentException.class, () -> new SpaceBudget(workArea, 100, 1.5));
        assertThrows(IllegalArgumentException.class, () -> new SpaceBudget(workArea, 10001, 1.0));
    }

    @Test
    void placementStoresOffsetRelativeToAnchor() {
        InworldVariant variant = TestElement.ladder(new Size(100, 40)).strongest();
        InworldPlacement placement = new InworldPlacement(
                "e", variant, new FloatPos(200, 150), new FloatRect(-50, -20, 100, 40), null, 3, 7);

        assertEquals(150, placement.screenRect().x(), 0);
        assertEquals(130, placement.screenRect().y(), 0);
        assertEquals(100, placement.screenRect().width(), 0);
        assertEquals(40, placement.screenRect().height(), 0);
        assertEquals(3, placement.arbitrationIndex());
        assertEquals(7, placement.epoch());
        assertNull(placement.world());

        // the anchor is copied at construction
        FloatPos anchor = new FloatPos(200, 150);
        InworldPlacement copied =
                new InworldPlacement("e", variant, anchor, new FloatRect(-50, -20, 100, 40), null, 0, 0);
        anchor.set(0, 0);
        assertEquals(200, copied.anchor().x(), 0);
        assertThrows(
                IllegalArgumentException.class,
                () -> new InworldPlacement("e", variant, new FloatPos(0, 0), new FloatRect(0, 0, 10, 10), null, -1, 0));
    }

    @Test
    void proposeContextAndRejectionValidate() {
        SpaceBudget budget = new SpaceBudget(new Rect(0, 0, 100, 100), 5000, 0.5);
        InworldVariant variant = TestElement.ladder(new Size(100, 40)).strongest();

        ElementRejection rejection = ElementRejection.of(RejectionReason.overlap);
        assertNull(rejection.blockerId());
        assertNull(rejection.suggestedRect());

        ProposeContext context = new ProposeContext(3, 1, budget, null, rejection, variant);
        assertEquals(1, context.round());
        assertEquals(rejection, context.lastRejection());

        assertThrows(NullPointerException.class, () -> new ProposeContext(0, 0, null, null, null, variant));
        assertThrows(IllegalArgumentException.class, () -> new ProposeContext(0, 2, budget, null, null, variant));
        assertThrows(NullPointerException.class, () -> new ProposeContext(0, 0, budget, null, null, null));
        assertThrows(NullPointerException.class, () -> ElementRejection.of(null));
    }

    @Test
    void coordinationResultCopiesItsLists() {
        InworldVariant variant = TestElement.ladder(new Size(100, 40)).strongest();
        InworldPlacement placement =
                new InworldPlacement("e", variant, new FloatPos(0, 0), new FloatRect(0, 0, 100, 40), null, 0, 0);
        List<InworldPlacement> placements = new ArrayList<>(List.of(placement));
        SpaceBudget budget = new SpaceBudget(new Rect(0, 0, 100, 100), 5000, 0.5);

        CoordinationResult result = new CoordinationResult(
                1,
                1,
                true,
                CoordinationResult.RenegotiationCause.membershipChanged,
                placements,
                List.of(new CoordinationResult.ElementState(
                        "e", VisibilityTracker.Phase.visible, 1.0, placement, null, null)),
                budget);

        placements.clear();
        assertEquals(1, result.placements().size());
        assertEquals(placement, result.placementOf("e"));
        assertNull(result.placementOf("missing"));
        assertEquals("e", result.elementState("e").elementId());
        assertNull(result.elementState("missing"));
        assertEquals(CoordinationResult.RenegotiationCause.membershipChanged, result.cause());
        assertThrows(
                IllegalArgumentException.class,
                () -> new CoordinationResult.ElementState("e", VisibilityTracker.Phase.visible, 1.5, null, null, null));
    }

    @Test
    void configAndFrameInputValidate() {
        InworldCoordinator.Config.defaults();
        assertThrows(
                NullPointerException.class,
                () -> new InworldCoordinator.Config(
                        0.2, 2, 12, 48, 24, 3, 8, 0.3, 900, 30, null, new SwitchGate.Config(16, 1, 2, 0)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new InworldCoordinator.Config(
                        0.2,
                        3,
                        12,
                        48,
                        24,
                        3,
                        8,
                        0.3,
                        900,
                        30,
                        VisibilityTracker.Config.of(0.1, 0.1, 0.1),
                        new SwitchGate.Config(16, 1, 2, 0)));
        assertThrows(
                IllegalArgumentException.class,
                () -> new InworldCoordinator.Config(
                        -1,
                        2,
                        12,
                        48,
                        24,
                        3,
                        8,
                        0.3,
                        900,
                        30,
                        VisibilityTracker.Config.of(0.1, 0.1, 0.1),
                        new SwitchGate.Config(16, 1, 2, 0)));

        InworldCoordinator.FrameInput.of(100, 100, 0, 0.016, new Rect(0, 0, 10, 10));
        assertThrows(IllegalArgumentException.class, () -> InworldCoordinator.FrameInput.of(0, 100, 0, 0.016));
        assertThrows(
                IllegalArgumentException.class, () -> InworldCoordinator.FrameInput.of(100, 100, Double.NaN, 0.016));
        assertEquals(0.0, InworldCoordinator.FrameInput.of(100, 100, 0, -1).dtSeconds(), 0.0);
        assertThrows(IllegalArgumentException.class, () -> InworldCoordinator.FrameInput.of(100, 100, 0, Double.NaN));
    }
}
