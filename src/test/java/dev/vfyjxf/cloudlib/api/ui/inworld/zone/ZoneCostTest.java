package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import org.jetbrains.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ZoneCostTest {

    private static final Rect screen = new Rect(0, 0, 480, 270);
    private static final double diagonalSq = 480.0 * 480.0 + 270.0 * 270.0;
    private static final double diagonal = Math.sqrt(diagonalSq);
    private static final FloatPos anchor = new FloatPos(240, 135);
    private static final GaussianAttention attention = GaussianAttention.atScreenCenter(480, 270, 100.0);

    /** A deterministic flat field for isolating the attention term. */
    private static final class ConstantField implements AttentionField {

        private final double value;

        ConstantField(double value) {
            this.value = value;
        }

        @Override
        public double valueAt(double x, double y) {
            return value;
        }

        @Override
        public int samplingStep() {
            return 4;
        }
    }

    private static ZoneCost.Context ctx(
        Map<String, Rect> placed,
        List<Rect> exclusions,
        @Nullable Rect previous,
        List<ZoneCost.Segment> leaders,
        Set<ZoneCost.Adjacency> leftOf,
        Set<ZoneCost.Adjacency> above
    ) {
        return new ZoneCost.Context(
            "a",
            anchor,
            screen,
            attention,
            placed,
            exclusions,
            previous,
            leaders,
            leftOf,
            above
        );
    }

    private static ZoneCost.Context empty() {
        return ctx(Map.of(), List.of(), null, List.of(), Set.of(), Set.of());
    }

    private static ZoneCost singleWeight(ZoneCost.Term term) {
        ZoneWeights zero = new ZoneWeights(0, 0, 0, 0, 0, 0, 0, 0, 0);
        return new ZoneCost(switch (term) {
            case anchor -> zero.withAnchor(1.0);
            case overlap -> zero.withOverlap(1.0);
            case hud -> zero.withHud(1.0);
            case attention -> zero.withAttention(1.0);
            case edge -> zero.withEdge(1.0);
            case leader -> zero.withLeader(1.0);
            case temporal -> zero.withTemporal(1.0);
            case crossing -> zero.withCrossing(1.0);
            case topology -> zero.withTopology(1.0);
        });
    }

    // region single-term semantics

    @Test
    void anchorTermIsSquaredCenterDistanceOverSquaredDiagonal() {
        ZoneCost cost = singleWeight(ZoneCost.Term.anchor);
        ZoneCost.Context context = empty();

        // centered on the anchor: zero
        assertEquals(0.0, cost.term(ZoneCost.Term.anchor, new Rect(210, 123, 60, 24), context), 1.0e-12);
        // exact fractions
        assertEquals(
            40402.0 / diagonalSq,
            cost.term(ZoneCost.Term.anchor, new Rect(421, 116, 40, 40), context),
            1.0e-12
        );
        assertEquals(75077.0 / diagonalSq, cost.term(ZoneCost.Term.anchor, new Rect(0, 0, 2, 2), context), 1.0e-12);
        // far off-screen: clamped at 1
        assertEquals(1.0, cost.term(ZoneCost.Term.anchor, new Rect(-500, -500, 2, 2), context), 1.0e-12);
    }

    @Test
    void overlapTermIsCoveredFractionClampedAtOne() {
        ZoneCost cost = singleWeight(ZoneCost.Term.overlap);
        Rect candidate = new Rect(100, 100, 100, 100);

        assertEquals(
            0.5,
            cost.term(
                ZoneCost.Term.overlap,
                candidate,
                ctx(Map.of("b", new Rect(100, 100, 50, 100)), List.of(), null, List.of(), Set.of(), Set.of())
            ),
            1.0e-12
        );
        assertEquals(
            0.0,
            cost.term(
                ZoneCost.Term.overlap,
                candidate,
                ctx(Map.of("b", new Rect(300, 100, 50, 50)), List.of(), null, List.of(), Set.of(), Set.of())
            ),
            1.0e-12
        );
        // full coverage by one placement
        assertEquals(
            1.0,
            cost.term(
                ZoneCost.Term.overlap,
                candidate,
                ctx(Map.of("b", new Rect(90, 90, 120, 120)), List.of(), null, List.of(), Set.of(), Set.of())
            ),
            1.0e-12
        );
        // two overlapping placements: double-counted area clamps at 1
        assertEquals(
            1.0,
            cost.term(
                ZoneCost.Term.overlap,
                candidate,
                ctx(
                    Map.of("b1", new Rect(100, 100, 60, 100), "b2", new Rect(140, 100, 60, 100)),
                    List.of(),
                    null,
                    List.of(),
                    Set.of(),
                    Set.of()
                )
            ),
            1.0e-12
        );
    }

    @Test
    void hudTermGradesExclusionCoverage() {
        ZoneCost cost = singleWeight(ZoneCost.Term.hud);
        Rect candidate = new Rect(100, 100, 100, 100);

        assertEquals(0.0, cost.term(ZoneCost.Term.hud, candidate, empty()), 1.0e-12);
        assertEquals(
            0.5,
            cost.term(
                ZoneCost.Term.hud,
                candidate,
                ctx(Map.of(), List.of(new Rect(100, 100, 50, 100)), null, List.of(), Set.of(), Set.of())
            ),
            1.0e-12
        );
    }

    @Test
    void attentionTermIsTheFieldCost() {
        ZoneCost cost = singleWeight(ZoneCost.Term.attention);
        ZoneCost.Context context = empty();

        assertEquals(
            attention.cost(new Rect(220, 115, 40, 40)),
            cost.term(ZoneCost.Term.attention, new Rect(220, 115, 40, 40), context),
            0.0
        );
        assertEquals(
            attention.cost(new Rect(410, 5, 40, 40)),
            cost.term(ZoneCost.Term.attention, new Rect(410, 5, 40, 40), context),
            0.0
        );
    }

    @Test
    void edgeTermIsTheOutsideFraction() {
        ZoneCost cost = singleWeight(ZoneCost.Term.edge);
        ZoneCost.Context context = empty();

        assertEquals(0.0, cost.term(ZoneCost.Term.edge, new Rect(100, 100, 60, 24), context), 1.0e-12);
        // half the candidate sticks out on the right
        assertEquals(0.5, cost.term(ZoneCost.Term.edge, new Rect(460, 10, 40, 40), context), 1.0e-12);
        // fully outside
        assertEquals(1.0, cost.term(ZoneCost.Term.edge, new Rect(500, 0, 40, 40), context), 1.0e-12);
    }

    @Test
    void leaderTermMeasuresFromTheFacingEdgeMidpoint() {
        ZoneCost cost = singleWeight(ZoneCost.Term.leader);
        ZoneCost.Context context = empty();

        // panel above the anchor: facing edge is its bottom, midpoint (240, 84)
        assertEquals(51.0 / diagonal, cost.term(ZoneCost.Term.leader, new Rect(210, 60, 60, 24), context), 1.0e-12);
        // panel left of the anchor: facing edge is its right, midpoint (210, 135)
        assertEquals(30.0 / diagonal, cost.term(ZoneCost.Term.leader, new Rect(150, 123, 60, 24), context), 1.0e-12);
        // anchor inside the panel: no leader needed
        assertEquals(0.0, cost.term(ZoneCost.Term.leader, new Rect(220, 120, 60, 40), context), 1.0e-12);
        // far away: clamped at 1
        assertEquals(1.0, cost.term(ZoneCost.Term.leader, new Rect(-1000, 123, 60, 24), context), 1.0e-12);
    }

    @Test
    void temporalTermIsSquaredFrameDistance() {
        ZoneCost cost = singleWeight(ZoneCost.Term.temporal);
        Rect candidate = new Rect(100, 135, 60, 24);

        // no previous frame: zero
        assertEquals(0.0, cost.term(ZoneCost.Term.temporal, candidate, empty()), 1.0e-12);
        // previous rect at the same center: zero
        assertEquals(
            0.0,
            cost.term(
                ZoneCost.Term.temporal,
                candidate,
                ctx(Map.of(), List.of(), new Rect(100, 135, 60, 24), List.of(), Set.of(), Set.of())
            ),
            1.0e-12
        );
        // centers 240px apart: 240² / diag²
        assertEquals(
            57600.0 / diagonalSq,
            cost.term(
                ZoneCost.Term.temporal,
                candidate,
                ctx(Map.of(), List.of(), new Rect(340, 135, 60, 24), List.of(), Set.of(), Set.of())
            ),
            1.0e-12
        );
    }

    @Test
    void crossingTermCountsProperSegmentCrossings() {
        ZoneCost cost = singleWeight(ZoneCost.Term.crossing);
        // candidate far left of the anchor: its leader runs (240,135) → (120,135)
        Rect candidate = new Rect(60, 123, 60, 24);

        // no placed leaders: zero
        assertEquals(0.0, cost.term(ZoneCost.Term.crossing, candidate, empty()), 1.0e-12);
        // one crossing vertical: full count
        assertEquals(
            1.0,
            cost.term(
                ZoneCost.Term.crossing,
                candidate,
                ctx(Map.of(), List.of(), null, List.of(new ZoneCost.Segment(180, 60, 180, 200)), Set.of(), Set.of())
            ),
            1.0e-12
        );
        // parallel segment: no crossing
        assertEquals(
            0.0,
            cost.term(
                ZoneCost.Term.crossing,
                candidate,
                ctx(Map.of(), List.of(), null, List.of(new ZoneCost.Segment(100, 100, 200, 100)), Set.of(), Set.of())
            ),
            1.0e-12
        );
        // two placed, one crossed: half
        assertEquals(
            0.5,
            cost.term(
                ZoneCost.Term.crossing,
                candidate,
                ctx(
                    Map.of(),
                    List.of(),
                    null,
                    List.of(new ZoneCost.Segment(180, 60, 180, 200), new ZoneCost.Segment(0, 50, 100, 50)),
                    Set.of(),
                    Set.of()
                )
            ),
            1.0e-12
        );
        // collinear overlap counts
        assertEquals(
            1.0,
            cost.term(
                ZoneCost.Term.crossing,
                candidate,
                ctx(Map.of(), List.of(), null, List.of(new ZoneCost.Segment(150, 135, 230, 135)), Set.of(), Set.of())
            ),
            1.0e-12
        );
        // collinear disjoint does not
        assertEquals(
            0.0,
            cost.term(
                ZoneCost.Term.crossing,
                candidate,
                ctx(Map.of(), List.of(), null, List.of(new ZoneCost.Segment(0, 135, 100, 135)), Set.of(), Set.of())
            ),
            1.0e-12
        );
        // a shared endpoint is not a crossing
        assertEquals(
            0.0,
            cost.term(
                ZoneCost.Term.crossing,
                candidate,
                ctx(Map.of(), List.of(), null, List.of(new ZoneCost.Segment(120, 135, 120, 60)), Set.of(), Set.of())
            ),
            1.0e-12
        );
    }

    @Test
    void degenerateLeadersNeverCross() {
        ZoneCost cost = singleWeight(ZoneCost.Term.crossing);
        // the anchor lies inside this candidate: its leader is a point
        Rect containing = new Rect(220, 120, 60, 40);
        ZoneCost.Context context = ctx(
            Map.of(),
            List.of(),
            null,
            List.of(new ZoneCost.Segment(240, 60, 240, 200)),
            Set.of(),
            Set.of()
        );

        assertEquals(0.0, cost.term(ZoneCost.Term.crossing, containing, context), 1.0e-12);
    }

    @Test
    void topologyTermPunishesBrokenAdjacency() {
        ZoneCost cost = singleWeight(ZoneCost.Term.topology);
        Map<String, Rect> placed = Map.of("b", new Rect(300, 100, 60, 24));

        // a was left of b: a candidate still left of b keeps the relation
        ZoneCost.Context context = ctx(
            placed,
            List.of(),
            null,
            List.of(),
            Set.of(new ZoneCost.Adjacency("a", "b")),
            Set.of()
        );
        assertEquals(0.0, cost.term(ZoneCost.Term.topology, new Rect(100, 100, 60, 24), context), 1.0e-12);
        // touching counts as keeping (right edge flush with b's left edge)
        assertEquals(0.0, cost.term(ZoneCost.Term.topology, new Rect(240, 100, 60, 24), context), 1.0e-12);
        // a candidate to the right of b breaks it
        assertEquals(1.0, cost.term(ZoneCost.Term.topology, new Rect(280, 100, 60, 24), context), 1.0e-12);
    }

    @Test
    void topologyTermHandlesAboveRelationsAndReversedPairs() {
        ZoneCost cost = singleWeight(ZoneCost.Term.topology);

        // a was above b
        ZoneCost.Context above = ctx(
            Map.of("b", new Rect(210, 200, 60, 24)),
            List.of(),
            null,
            List.of(),
            Set.of(),
            Set.of(new ZoneCost.Adjacency("a", "b"))
        );
        assertEquals(0.0, cost.term(ZoneCost.Term.topology, new Rect(210, 168, 60, 24), above), 1.0e-12);
        assertEquals(1.0, cost.term(ZoneCost.Term.topology, new Rect(210, 190, 60, 24), above), 1.0e-12);

        // b was left of a: the candidate must sit right of b
        ZoneCost.Context reversed = ctx(
            Map.of("b", new Rect(60, 100, 60, 24)),
            List.of(),
            null,
            List.of(),
            Set.of(new ZoneCost.Adjacency("b", "a")),
            Set.of()
        );
        assertEquals(0.0, cost.term(ZoneCost.Term.topology, new Rect(130, 100, 60, 24), reversed), 1.0e-12);
        assertEquals(1.0, cost.term(ZoneCost.Term.topology, new Rect(0, 100, 60, 24), reversed), 1.0e-12);
    }

    @Test
    void topologyTermIgnoresPairsNotInvolvingTheCandidate() {
        ZoneCost cost = singleWeight(ZoneCost.Term.topology);
        ZoneCost.Context othersPair = ctx(
            Map.of("b", new Rect(300, 100, 60, 24), "c", new Rect(100, 100, 60, 24)),
            List.of(),
            null,
            List.of(),
            Set.of(new ZoneCost.Adjacency("b", "c")),
            Set.of()
        );

        assertEquals(0.0, cost.term(ZoneCost.Term.topology, new Rect(380, 100, 60, 24), othersPair), 1.0e-12);

        // the other element missing from the placements is not the candidate's business
        ZoneCost.Context missingOther = ctx(
            Map.of(),
            List.of(),
            null,
            List.of(),
            Set.of(new ZoneCost.Adjacency("a", "c")),
            Set.of()
        );
        assertEquals(0.0, cost.term(ZoneCost.Term.topology, new Rect(0, 0, 60, 24), missingOther), 1.0e-12);
    }

    @Test
    void topologyTermAveragesOverMixedPairs() {
        ZoneCost cost = singleWeight(ZoneCost.Term.topology);
        // keep (a,b), break (a,c): half the pairs broken
        ZoneCost.Context context = ctx(
            Map.of("b", new Rect(400, 100, 60, 24), "c", new Rect(350, 200, 60, 24)),
            List.of(),
            null,
            List.of(),
            Set.of(new ZoneCost.Adjacency("a", "b"), new ZoneCost.Adjacency("a", "c")),
            Set.of()
        );

        assertEquals(0.5, cost.term(ZoneCost.Term.topology, new Rect(320, 200, 60, 24), context), 1.0e-12);
    }

    // endregion

    // region weighting

    @Test
    void everyTermIsWeightSensitive() {
        // anchor: near beats far under the same context
        assertSensitive(ZoneCost.Term.anchor, empty(), new Rect(210, 123, 60, 24), empty(), new Rect(420, 116, 60, 24));
        // overlap: a free spot beats a covered one
        assertSensitive(
            ZoneCost.Term.overlap,
            empty(),
            new Rect(200, 100, 60, 24),
            ctx(Map.of("b", new Rect(200, 100, 60, 24)), List.of(), null, List.of(), Set.of(), Set.of()),
            new Rect(200, 100, 60, 24)
        );
        // hud: clear beats over an exclusion
        assertSensitive(
            ZoneCost.Term.hud,
            empty(),
            new Rect(200, 100, 60, 24),
            ctx(Map.of(), List.of(new Rect(200, 100, 60, 24)), null, List.of(), Set.of(), Set.of()),
            new Rect(200, 100, 60, 24)
        );
        // attention: a cheap field beats an expensive one
        assertSensitive(
            ZoneCost.Term.attention,
            new ZoneCost.Context(
                "a",
                anchor,
                screen,
                new ConstantField(0.0),
                Map.of(),
                List.of(),
                null,
                List.of(),
                Set.of(),
                Set.of()
            ),
            new Rect(220, 115, 40, 40),
            new ZoneCost.Context(
                "a",
                anchor,
                screen,
                attention,
                Map.of(),
                List.of(),
                null,
                List.of(),
                Set.of(),
                Set.of()
            ),
            new Rect(220, 115, 40, 40)
        );
        // edge: inside beats sticking out
        assertSensitive(ZoneCost.Term.edge, empty(), new Rect(100, 100, 60, 24), empty(), new Rect(460, 10, 40, 40));
        // leader: no leader beats a long one
        assertSensitive(ZoneCost.Term.leader, empty(), new Rect(220, 120, 60, 40), empty(), new Rect(60, 123, 60, 24));
        // temporal: staying put beats moving
        assertSensitive(
            ZoneCost.Term.temporal,
            ctx(Map.of(), List.of(), new Rect(210, 123, 60, 24), List.of(), Set.of(), Set.of()),
            new Rect(210, 123, 60, 24),
            ctx(Map.of(), List.of(), new Rect(340, 135, 60, 24), List.of(), Set.of(), Set.of()),
            new Rect(210, 123, 60, 24)
        );
        // crossing: a clear leader beats a crossing one
        assertSensitive(
            ZoneCost.Term.crossing,
            empty(),
            new Rect(60, 123, 60, 24),
            ctx(Map.of(), List.of(), null, List.of(new ZoneCost.Segment(180, 60, 180, 200)), Set.of(), Set.of()),
            new Rect(60, 123, 60, 24)
        );
        // topology: keeping the neighbor order beats breaking it
        assertSensitive(
            ZoneCost.Term.topology,
            ctx(
                Map.of("b", new Rect(300, 100, 60, 24)),
                List.of(),
                null,
                List.of(),
                Set.of(new ZoneCost.Adjacency("a", "b")),
                Set.of()
            ),
            new Rect(100, 100, 60, 24),
            ctx(
                Map.of("b", new Rect(300, 100, 60, 24)),
                List.of(),
                null,
                List.of(),
                Set.of(new ZoneCost.Adjacency("a", "b")),
                Set.of()
            ),
            new Rect(280, 100, 60, 24)
        );
    }

    private static void assertSensitive(
        ZoneCost.Term term,
        ZoneCost.Context goodContext,
        Rect goodRect,
        ZoneCost.Context badContext,
        Rect badRect
    ) {
        double good = singleWeight(term).cost(goodRect, goodContext);
        double bad = singleWeight(term).cost(badRect, badContext);
        assertTrue(bad > good, term + ": bad " + bad + " !> good " + good);
        // zeroing the weight disables the term entirely
        ZoneCost disabled = new ZoneCost(new ZoneWeights(0, 0, 0, 0, 0, 0, 0, 0, 0));
        assertEquals(disabled.cost(goodRect, goodContext), disabled.cost(badRect, badContext), 0.0);
    }

    @Test
    void zeroWeightsScoreZeroEverywhere() {
        ZoneCost cost = new ZoneCost(new ZoneWeights(0, 0, 0, 0, 0, 0, 0, 0, 0));
        Rect candidate = new Rect(0, 0, 60, 24);
        ZoneCost.Context worst = ctx(
            Map.of("b", candidate),
            List.of(candidate),
            new Rect(400, 250, 60, 24),
            List.of(new ZoneCost.Segment(0, 0, 480, 270)),
            Set.of(new ZoneCost.Adjacency("a", "b")),
            Set.of(new ZoneCost.Adjacency("a", "b"))
        );

        assertEquals(0.0, cost.cost(candidate, worst), 0.0);
    }

    @Test
    void everyTermSaturatesAtOne() {
        ZoneCost cost = new ZoneCost(ZoneWeights.defaults());
        ConstantField hot = new ConstantField(1.0);

        assertEquals(1.0, cost.term(ZoneCost.Term.anchor, new Rect(-500, -500, 2, 2), empty()), 1.0e-12);
        assertEquals(
            1.0,
            cost.term(
                ZoneCost.Term.overlap,
                new Rect(100, 100, 100, 100),
                ctx(Map.of("b", new Rect(90, 90, 120, 120)), List.of(), null, List.of(), Set.of(), Set.of())
            ),
            1.0e-12
        );
        assertEquals(
            1.0,
            cost.term(
                ZoneCost.Term.hud,
                new Rect(100, 100, 100, 100),
                ctx(Map.of(), List.of(new Rect(90, 90, 120, 120)), null, List.of(), Set.of(), Set.of())
            ),
            1.0e-12
        );
        assertEquals(
            1.0,
            cost.term(
                ZoneCost.Term.attention,
                new Rect(0, 0, 40, 40),
                new ZoneCost.Context("a", anchor, screen, hot, Map.of(), List.of(), null, List.of(), Set.of(), Set.of())
            ),
            1.0e-12
        );
        assertEquals(1.0, cost.term(ZoneCost.Term.edge, new Rect(500, 0, 40, 40), empty()), 1.0e-12);
        assertEquals(1.0, cost.term(ZoneCost.Term.leader, new Rect(-1000, 123, 60, 24), empty()), 1.0e-12);
        assertEquals(
            1.0,
            cost.term(
                ZoneCost.Term.temporal,
                new Rect(210, 123, 60, 24),
                ctx(Map.of(), List.of(), new Rect(-1000, 135, 2, 2), List.of(), Set.of(), Set.of())
            ),
            1.0e-12
        );
        assertEquals(
            1.0,
            cost.term(
                ZoneCost.Term.crossing,
                new Rect(60, 123, 60, 24),
                ctx(Map.of(), List.of(), null, List.of(new ZoneCost.Segment(180, 60, 180, 200)), Set.of(), Set.of())
            ),
            1.0e-12
        );
        assertEquals(
            1.0,
            cost.term(
                ZoneCost.Term.topology,
                new Rect(280, 100, 60, 24),
                ctx(
                    Map.of("b", new Rect(300, 100, 60, 24)),
                    List.of(),
                    null,
                    List.of(),
                    Set.of(new ZoneCost.Adjacency("a", "b")),
                    Set.of()
                )
            ),
            1.0e-12
        );
    }

    @Test
    void topologyBreakingScoresWorseUnderDefaultWeights() {
        ZoneCost cost = ZoneCost.withDefaults();
        ZoneCost.Context context = ctx(
            Map.of("b", new Rect(300, 100, 60, 24)),
            List.of(),
            null,
            List.of(),
            Set.of(new ZoneCost.Adjacency("a", "b")),
            Set.of()
        );

        double keeping = cost.cost(new Rect(100, 100, 60, 24), context);
        double breaking = cost.cost(new Rect(280, 100, 60, 24), context);

        assertTrue(breaking > keeping, "breaking " + breaking + " !> keeping " + keeping);
        // the gap is at least the topology term's own contribution
        assertTrue(breaking - keeping > 0.5);
    }

    @Test
    void totalIsTheWeightedSum() {
        ZoneCost cost = ZoneCost.withDefaults();
        ZoneWeights weights = cost.weights();
        Rect candidate = new Rect(280, 100, 60, 24);
        ZoneCost.Context context = ctx(
            Map.of("b", new Rect(300, 100, 60, 24)),
            List.of(),
            null,
            List.of(),
            Set.of(new ZoneCost.Adjacency("a", "b")),
            Set.of()
        );

        double expected = 0.0;
        for (ZoneCost.Term term : ZoneCost.Term.values()) {
            expected += switch (term) {
                case anchor -> weights.anchor();
                case overlap -> weights.overlap();
                case hud -> weights.hud();
                case attention -> weights.attention();
                case edge -> weights.edge();
                case leader -> weights.leader();
                case temporal -> weights.temporal();
                case crossing -> weights.crossing();
                case topology -> weights.topology();
            } * cost.term(term, candidate, context);
        }
        assertEquals(expected, cost.cost(candidate, context), 1.0e-9);
        // and bounded by the weight sum
        assertTrue(
            cost.cost(candidate, context)
                    <= weights.anchor()
                            + weights.overlap()
                            + weights.hud()
                            + weights.attention()
                            + weights.edge()
                            + weights.leader()
                            + weights.temporal()
                            + weights.crossing()
                            + weights.topology()
        );
    }

    // endregion

    // region weights and context plumbing

    @Test
    void defaultWeightsFollowTheDocumentedTable() {
        ZoneWeights weights = ZoneWeights.defaults();

        assertEquals(1.0, weights.anchor(), 0.0);
        assertEquals(2.0, weights.overlap(), 0.0);
        assertEquals(1.5, weights.hud(), 0.0);
        assertEquals(0.6, weights.attention(), 0.0);
        assertEquals(4.0, weights.edge(), 0.0);
        assertEquals(0.4, weights.leader(), 0.0);
        assertEquals(0.8, weights.temporal(), 0.0);
        assertEquals(0.5, weights.crossing(), 0.0);
        assertEquals(0.7, weights.topology(), 0.0);
    }

    @Test
    void withersTouchExactlyOneTerm() {
        ZoneWeights weights = ZoneWeights.defaults().withOverlap(3.0);

        assertEquals(3.0, weights.overlap(), 0.0);
        assertEquals(1.0, weights.anchor(), 0.0);
        assertEquals(1.5, weights.hud(), 0.0);
        assertEquals(0.6, weights.attention(), 0.0);
        assertEquals(4.0, weights.edge(), 0.0);
        assertEquals(0.4, weights.leader(), 0.0);
        assertEquals(0.8, weights.temporal(), 0.0);
        assertEquals(0.5, weights.crossing(), 0.0);
        assertEquals(0.7, weights.topology(), 0.0);
    }

    @Test
    void weightsRejectInvalidValues() {
        assertThrows(IllegalArgumentException.class, () -> ZoneWeights.defaults().withAnchor(-0.1));
        assertThrows(IllegalArgumentException.class, () -> ZoneWeights.defaults().withOverlap(Double.NaN));
        assertThrows(
            IllegalArgumentException.class,
            () -> ZoneWeights.defaults().withTopology(Double.POSITIVE_INFINITY)
        );
    }

    @Test
    void emptyContextScoresZeroOnEveryEmptyDrivenTerm() {
        ZoneCost cost = ZoneCost.withDefaults();
        ZoneCost.Context context = ZoneCost.Context.of("a", anchor, screen, attention);
        Rect candidate = new Rect(210, 123, 60, 24);

        assertEquals(0.0, cost.term(ZoneCost.Term.overlap, candidate, context), 0.0);
        assertEquals(0.0, cost.term(ZoneCost.Term.hud, candidate, context), 0.0);
        assertEquals(0.0, cost.term(ZoneCost.Term.temporal, candidate, context), 0.0);
        assertEquals(0.0, cost.term(ZoneCost.Term.crossing, candidate, context), 0.0);
        assertEquals(0.0, cost.term(ZoneCost.Term.topology, candidate, context), 0.0);
        assertEquals(0.0, cost.term(ZoneCost.Term.edge, candidate, context), 0.0);
        assertEquals(0.0, cost.term(ZoneCost.Term.anchor, candidate, context), 0.0);
    }

    @Test
    void degenerateSafeRectLeavesDiagonalTermsInertNotNaN() {
        ZoneCost cost = ZoneCost.withDefaults();
        ZoneCost.Context context = new ZoneCost.Context(
            "a",
            anchor,
            new Rect(10, 10, 0, 0),
            attention,
            Map.of(),
            List.of(),
            new Rect(0, 0, 10, 10),
            List.of(),
            Set.of(),
            Set.of()
        );

        for (ZoneCost.Term term : new ZoneCost.Term[]{ZoneCost.Term.anchor, ZoneCost.Term.temporal,
                ZoneCost.Term.leader}) {
            double value = cost.term(term, new Rect(210, 123, 60, 24), context);
            assertTrue(Double.isFinite(value), term + " is not finite: " + value);
            assertEquals(0.0, value, 0.0);
        }
        assertTrue(Double.isFinite(cost.cost(new Rect(210, 123, 60, 24), context)));
    }

    @Test
    void contextAndSegmentsValidateTheirInputs() {
        assertThrows(NullPointerException.class, () -> ZoneCost.Context.of(null, anchor, screen, attention));
        assertThrows(
            NullPointerException.class,
            () -> new ZoneCost.Context(
                "a",
                null,
                screen,
                attention,
                Map.of(),
                List.of(),
                null,
                List.of(),
                Set.of(),
                Set.of()
            )
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> new ZoneCost.Context(
                "a",
                new FloatPos(Double.NaN, 0),
                screen,
                attention,
                Map.of(),
                List.of(),
                null,
                List.of(),
                Set.of(),
                Set.of()
            )
        );
        assertThrows(IllegalArgumentException.class, () -> new ZoneCost.Segment(0, 0, Double.NaN, 1));
        assertThrows(NullPointerException.class, () -> new ZoneCost.Adjacency(null, "b"));
    }

    // endregion
}
