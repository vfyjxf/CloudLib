package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The previous-frame snapshot: placement map, leaders and the adjacency tables. */
class PreviousFrameLayoutTest {

    private static final Rect screen = new Rect(0, 0, 400, 300);

    private static PreviousFrameLayout.Placement placed(String id, Rect rect) {
        // the anchor sits on the rect's center by default (a degenerate leader)
        return new PreviousFrameLayout.Placement(id, new FloatPos(rect.centerX(), rect.centerY()), rect);
    }

    private static PreviousFrameLayout layout(PreviousFrameLayout.Placement... placements) {
        return PreviousFrameLayout.of(List.of(placements));
    }

    // region placement map

    @Test
    void placementsAreKeyedById() {
        PreviousFrameLayout snapshot = layout(
            placed("a", new Rect(10, 10, 40, 20)),
            placed("b", new Rect(200, 40, 60, 30))
        );
        assertEquals(Map.of("a", new Rect(10, 10, 40, 20), "b", new Rect(200, 40, 60, 30)), snapshot.placements());
        assertTrue(snapshot.leftOf().isEmpty());
        assertTrue(snapshot.above().isEmpty());
    }

    @Test
    void malformedInputIsRejected() {
        assertThrows(
            IllegalArgumentException.class,
            () -> PreviousFrameLayout.of(List.of(placed("a", new Rect(0, 0, 10, 10))), -1.0)
        );
        assertThrows(NullPointerException.class, () -> PreviousFrameLayout.of(null));
    }

    // endregion

    // region adjacency: horizontal

    @Test
    void horizontallyDisjointRectsWithOverlappingProjectionsAreLeftOf() {
        // a right edge 100, b left edge 104: gap 4 ≤ 8, vertical projection overlaps 10..30 ∩ 12..32 = 12..30 > 0
        PreviousFrameLayout snapshot = layout(
            placed("a", new Rect(60, 10, 40, 20)),
            placed("b", new Rect(104, 12, 40, 20))
        );
        assertEquals(Set.of(new ZoneCost.Adjacency("a", "b")), snapshot.leftOf());
        assertTrue(snapshot.above().isEmpty());
    }

    @Test
    void theGapThresholdIsInclusive() {
        // gap exactly 8 (a.right = 100, b.x = 108): adjacent; gap 9: not
        PreviousFrameLayout atThreshold = layout(
            placed("a", new Rect(60, 10, 40, 20)),
            placed("b", new Rect(108, 10, 40, 20))
        );
        assertEquals(Set.of(new ZoneCost.Adjacency("a", "b")), atThreshold.leftOf());

        PreviousFrameLayout beyondThreshold = layout(
            placed("a", new Rect(60, 10, 40, 20)),
            placed("b", new Rect(109, 10, 40, 20))
        );
        assertTrue(beyondThreshold.leftOf().isEmpty());
    }

    @Test
    void overlappingRectsWithDistinctCentersCountAsNeighbors() {
        // ghost-policy rects may overlap; gap is negative, the threshold holds
        PreviousFrameLayout snapshot = layout(
            placed("a", new Rect(60, 10, 40, 20)),
            placed("b", new Rect(80, 12, 40, 20))
        );
        assertEquals(Set.of(new ZoneCost.Adjacency("a", "b")), snapshot.leftOf());
    }

    @Test
    void verticalProjectionOverlapMustBePositive() {
        // vertical projections merely touch (a spans 10..30, b spans 30..50): not adjacent
        PreviousFrameLayout touching = layout(
            placed("a", new Rect(60, 10, 40, 20)),
            placed("b", new Rect(104, 30, 40, 20))
        );
        assertTrue(touching.leftOf().isEmpty());
        assertTrue(touching.above().isEmpty());

        // vertically disjoint entirely
        PreviousFrameLayout disjoint = layout(
            placed("a", new Rect(60, 10, 40, 20)),
            placed("b", new Rect(104, 80, 40, 20))
        );
        assertTrue(disjoint.leftOf().isEmpty());
    }

    @Test
    void equalCentersFormNoRelation() {
        // centers coincide on x: neither a leftOf b nor b leftOf a
        PreviousFrameLayout snapshot = layout(
            placed("a", new Rect(60, 10, 40, 20)),
            placed("b", new Rect(60, 10, 40, 20))
        );
        assertTrue(snapshot.leftOf().isEmpty());
        assertTrue(snapshot.above().isEmpty());
    }

    // endregion

    // region adjacency: vertical and diagonal

    @Test
    void verticallyDisjointRectsWithOverlappingProjectionsAreAbove() {
        // a bottom 30, b top 38: gap 8 (exactly the threshold); horizontal projection 60..100 ∩ 64..104 overlaps
        PreviousFrameLayout snapshot = layout(
            placed("a", new Rect(60, 10, 40, 20)),
            placed("b", new Rect(64, 38, 40, 20))
        );
        assertEquals(Set.of(new ZoneCost.Adjacency("a", "b")), snapshot.above());
        assertTrue(snapshot.leftOf().isEmpty());
    }

    @Test
    void diagonalNeighborsAppearInBothTables() {
        // overlapping rects separated diagonally (ghost-policy style): both
        // projections overlap and both center comparisons order, so the pair
        // lands in both tables
        PreviousFrameLayout snapshot = layout(
            placed("a", new Rect(60, 10, 40, 20)),
            placed("b", new Rect(80, 24, 40, 20))
        );
        assertEquals(Set.of(new ZoneCost.Adjacency("a", "b")), snapshot.leftOf());
        assertEquals(Set.of(new ZoneCost.Adjacency("a", "b")), snapshot.above());
    }

    @Test
    void threeRectsProduceBothDirectionsInTheSameTable() {
        // b sits between a (left of b) and c (b left of c), all on one row;
        // a and c are 48px apart — beyond the threshold, so adjacency is not
        // transitive
        PreviousFrameLayout snapshot = layout(
            placed("a", new Rect(10, 50, 40, 20)),
            placed("b", new Rect(54, 50, 40, 20)),
            placed("c", new Rect(98, 50, 40, 20))
        );
        assertEquals(Set.of(new ZoneCost.Adjacency("a", "b"), new ZoneCost.Adjacency("b", "c")), snapshot.leftOf());
        assertTrue(snapshot.above().isEmpty());
    }

    // endregion

    // region leaders

    @Test
    void leadersRunFromTheAnchorToTheFacingEdgeMidpoint() {
        // the anchor far left of the rect: the leader ends on the rect's left edge, at its vertical center
        PreviousFrameLayout snapshot = PreviousFrameLayout
                .of(List.of(new PreviousFrameLayout.Placement("a", new FloatPos(10, 60), new Rect(100, 50, 40, 20))));
        ZoneCost.Segment segment = snapshot.leaders().getFirst().segment();
        assertEquals(10.0, segment.x1(), 0.0);
        assertEquals(60.0, segment.y1(), 0.0);
        assertEquals(100.0, segment.x2(), 0.0);
        assertEquals(60.0, segment.y2(), 0.0);
        // the same definition the crossing term uses for a candidate
        assertEquals(segment, ZoneCost.leaderOf(new FloatPos(10, 60), new Rect(100, 50, 40, 20)));
    }

    @Test
    void anAnchorInsideTheRectDeclaresADegenerateLeader() {
        PreviousFrameLayout snapshot = PreviousFrameLayout
                .of(List.of(new PreviousFrameLayout.Placement("a", new FloatPos(120, 60), new Rect(100, 50, 40, 20))));
        ZoneCost.Segment segment = snapshot.leaders().getFirst().segment();
        assertEquals(segment.x1(), segment.x2(), 0.0);
        assertEquals(segment.y1(), segment.y2(), 0.0);
    }

    @Test
    void leadersExcludingDropsOnlyTheOwner() {
        PreviousFrameLayout snapshot = layout(
            new PreviousFrameLayout.Placement("a", new FloatPos(10, 60), new Rect(100, 50, 40, 20)),
            new PreviousFrameLayout.Placement("b", new FloatPos(390, 60), new Rect(300, 50, 40, 20)),
            placed("c", new Rect(60, 200, 40, 20))
        );
        assertEquals(3, snapshot.leaders().size());
        List<ZoneCost.Segment> others = snapshot.leadersExcluding("a");
        assertEquals(2, others.size());
        assertEquals(snapshot.leaders().get(1).segment(), others.get(0));
        assertEquals(snapshot.leaders().get(2).segment(), others.get(1));
    }

    // endregion

    // region zone vocabulary compatibility

    @Test
    void theSnapshotFeedsTheCostContextsTopologyTerm() {
        // a was left of b (gap 6, within the threshold); a candidate keeping
        // a right of b's left edge preserves, one crossing it breaks
        Rect a = new Rect(10, 50, 40, 20);
        Rect b = new Rect(56, 50, 40, 20);
        PreviousFrameLayout snapshot = layout(placed("a", a), placed("b", b));
        assertEquals(Set.of(new ZoneCost.Adjacency("a", "b")), snapshot.leftOf());
        ZoneCost cost = new ZoneCost(new ZoneWeights(0, 0, 0, 0, 0, 0, 0, 0, 1.0));
        ZoneCost.Context context = new ZoneCost.Context(
            "a",
            new FloatPos(a.centerX(), a.centerY()),
            screen,
            GaussianAttention.atScreenCenter(400, 300, 96.0),
            Map.of("b", b),
            List.of(),
            null,
            List.of(),
            snapshot.leftOf(),
            snapshot.above()
        );
        // a candidate at a's old spot: a.right() = 50 ≤ b.x() = 56 → holds → 0
        assertEquals(0.0, cost.term(ZoneCost.Term.topology, a, context), 1.0e-12);
        // a candidate overlapping b horizontally: a'.right() = 90 > 56 → broken → 1
        assertEquals(1.0, cost.term(ZoneCost.Term.topology, new Rect(50, 50, 40, 20), context), 1.0e-12);
    }

    // endregion
}
