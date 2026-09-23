package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.algorithm.AlgorithmProfile;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.CoordinationResult;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.PlacementCandidate;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpaceMask;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpacePolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.GaussianAttention;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.LodTier;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.VisibilityPolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.ZoneCandidates;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.ZoneCost;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.ZoneWeights;
import org.jspecify.annotations.Nullable;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The zone strategies: candidates.zoneGrid's lattice and rank.zoneCost's ordering. */
class ZoneStrategiesTest {

    private static final Rect screen = new Rect(0, 0, 400, 300);
    private static final FloatPos anchor = new FloatPos(200, 150);
    private static final GaussianAttention attention = GaussianAttention.atScreenCenter(400, 300, 100.0);

    // region candidates.zoneGrid

    @Test
    void theLatticeProducesTheCanonical25Candidates() {
        List<PlacementCandidate> candidates = zoneGrid(new Size(60, 20), ZoneFacet.of());
        assertEquals(25, candidates.size(), "in-place + 8 directions × 3 tiers, nothing clamped away");
        assertEquals(
            FloatRect.around(anchor, 60, 20),
            candidates.getFirst().screenRect(),
            "the in-place candidate leads"
        );
        Set<FloatRect> unique = new LinkedHashSet<>();
        for (PlacementCandidate candidate : candidates) {
            assertNull(candidate.world(), "zone candidates are screen-only");
            assertTrue(
                contains(screen, Objects.requireNonNull(candidate.screenRect())),
                "every candidate lies inside the safe rect"
            );
            unique.add(candidate.screenRect());
        }
        assertEquals(candidates.size(), unique.size(), "the lattice is deduplicated");
    }

    @Test
    void aCornerAnchorDeduplicatesClampedCandidates() {
        List<PlacementCandidate> centered = zoneGrid(new FloatPos(200, 150), new Size(60, 20), ZoneFacet.of());
        List<PlacementCandidate> corner = zoneGrid(new FloatPos(4, 4), new Size(60, 20), ZoneFacet.of());
        assertEquals(25, centered.size());
        assertTrue(corner.size() < 25, "clamping near the edge collapses direction/tier combinations");
        Set<FloatRect> unique = new LinkedHashSet<>();
        for (PlacementCandidate candidate : corner) {
            assertTrue(contains(screen, Objects.requireNonNull(candidate.screenRect())));
            unique.add(candidate.screenRect());
        }
        assertEquals(corner.size(), unique.size(), "still deduplicated");
    }

    @Test
    void anUnfittablePanelFallsBackToTheAnchorCenteredFootprint() {
        List<PlacementCandidate> candidates = zoneGrid(new Size(500, 400), ZoneFacet.of());
        assertEquals(1, candidates.size());
        assertEquals(FloatRect.around(anchor, 500, 400), candidates.getFirst().screenRect());
    }

    @Test
    void theFacetConfigDrivesTheTierClearances() {
        // near-left: the panel's right edge sits nearPx left of the anchor
        List<PlacementCandidate> defaults = zoneGrid(new Size(60, 20), ZoneFacet.of());
        // with the default 8px near clearance the left-near candidate's right edge is anchor.x - 8
        assertTrue(defaults.stream().anyMatch(c -> {
            FloatRect rect = Objects.requireNonNull(c.screenRect());
            return rect.right() == 192 && rect.centerY() == 150;
        }), "left-near docks at 8px clearance");
        @SuppressWarnings("NullAway")
        ZoneFacet configured = new ZoneFacet(
            null,
            null,
            ZoneCandidates.Config.of(10.0, 30.0, 60.0),
            null,
            VisibilityPolicy.fade,
            LodTier.full
        );
        List<PlacementCandidate> cleared = zoneGrid(new Size(60, 20), configured);
        assertTrue(cleared.stream().anyMatch(c -> {
            FloatRect rect = Objects.requireNonNull(c.screenRect());
            return rect.right() == 190 && rect.centerY() == 150;
        }), "left-near docks at the facet's 10px clearance");
    }

    private static List<PlacementCandidate> zoneGrid(Size panel, ZoneFacet facet) {
        return zoneGrid(anchor, panel, facet);
    }

    private static List<PlacementCandidate> zoneGrid(FloatPos anchorPos, Size panel, ZoneFacet facet) {
        return StageCatalogs.requireCandidateStrategy(StageCatalogs.candidatesZoneGrid).candidates(
            new StageCatalogs.CandidateContext(
                anchorPos,
                panel,
                OrientationFacet.Mode.screen,
                0.0,
                LayoutHarness.unanchored(),
                AlgorithmProfile.facePanel.params(),
                Set.of(),
                facet
            )
        );
    }

    private static boolean contains(Rect bounds, FloatRect rect) {
        return rect.x() >= bounds.x()
                && rect.y() >= bounds.y()
                && rect.right() <= bounds.right()
                && rect.bottom() <= bounds.bottom();
    }

    // endregion

    // region rank.zoneCost

    @Test
    void overlapDominantRankingAvoidsTheCoveredCandidate() {
        ZoneWeights weights = only(ZoneCost.Term.overlap);
        PlacementCandidate covered = candidate(60, 130, 60, 24);
        PlacementCandidate clear = candidate(60, 30, 60, 24);
        ZoneCost.Context context = context(
            weights,
            Map.of("blocker", new Rect(50, 120, 100, 40)),
            null,
            List.of(),
            Set.of(),
            Set.of()
        );
        List<PlacementCandidate> ranked = rank(List.of(covered, clear), weights, context);
        assertSame(clear, ranked.getFirst(), "the candidate covering the placed rect loses");
        assertSame(covered, ranked.get(1));
    }

    @Test
    void attentionDominantRankingAvoidsTheCrosshair() {
        ZoneWeights weights = only(ZoneCost.Term.attention);
        PlacementCandidate crosshair = candidate(180, 140, 40, 20);
        PlacementCandidate corner = candidate(10, 270, 40, 20);
        ZoneCost.Context context = context(weights, Map.of(), null, List.of(), Set.of(), Set.of());
        List<PlacementCandidate> ranked = rank(List.of(crosshair, corner), weights, context);
        assertSame(corner, ranked.getFirst(), "the crosshair is the most expensive real estate");
        assertSame(crosshair, ranked.get(1));
    }

    @Test
    void temporalDominantRankingPrefersThePreviousRect() {
        ZoneWeights weights = only(ZoneCost.Term.temporal);
        PlacementCandidate nearPrevious = candidate(260, 100, 40, 20);
        PlacementCandidate far = candidate(60, 240, 40, 20);
        ZoneCost.Context context = context(
            weights,
            Map.of(),
            new Rect(260, 100, 40, 20),
            List.of(),
            Set.of(),
            Set.of()
        );
        List<PlacementCandidate> ranked = rank(List.of(far, nearPrevious), weights, context);
        assertSame(nearPrevious, ranked.getFirst(), "staying put costs nothing");
        assertSame(far, ranked.get(1));
    }

    @Test
    void topologyPreservationBeatsBreakingThePreviousAdjacency() {
        ZoneWeights weights = only(ZoneCost.Term.topology);
        // the previous frame had "me" left of the neighbor at (60, 50, 40, 20)
        ZoneCost.Adjacency leftOfNeighbor = new ZoneCost.Adjacency("me", "neighbor");
        PlacementCandidate keeps = candidate(10, 50, 40, 20); // right edge 50 ≤ 60: holds
        PlacementCandidate breaks = candidate(50, 50, 40, 20); // right edge 90 > 60: broken
        ZoneCost.Context context = context(
            weights,
            Map.of("neighbor", new Rect(60, 50, 40, 20)),
            null,
            List.of(),
            Set.of(leftOfNeighbor),
            Set.of()
        );
        List<PlacementCandidate> ranked = rank(List.of(breaks, keeps), weights, context);
        assertSame(keeps, ranked.getFirst(), "preserving the left-of relation wins");
        assertSame(breaks, ranked.get(1));
    }

    @Test
    void crossingDominantRankingAvoidsCrossingPlacedLeaders() {
        ZoneWeights weights = only(ZoneCost.Term.crossing);
        // a placed leader running horizontally below the anchor
        ZoneCost.Segment placedLeader = new ZoneCost.Segment(150, 160, 250, 160);
        PlacementCandidate below = candidate(190, 170, 20, 10); // its leader crosses the placed one
        PlacementCandidate above = candidate(190, 120, 20, 10); // its leader points away
        ZoneCost.Context context = context(weights, Map.of(), null, List.of(placedLeader), Set.of(), Set.of());
        List<PlacementCandidate> ranked = rank(List.of(below, above), weights, context);
        assertSame(above, ranked.getFirst(), "the leader pointing away crosses nothing");
        assertSame(below, ranked.get(1));
    }

    @Test
    void withoutZoneInputsTheCanonicalOrderStands() {
        List<PlacementCandidate> candidates = List.of(candidate(60, 130, 60, 24), candidate(60, 30, 60, 24));
        List<PlacementCandidate> ranked = StageCatalogs.requireRankStrategy(StageCatalogs.rankZoneCost).rank(
            candidates,
            new StageCatalogs.RankContext(anchor, null, false, AlgorithmProfile.facePanel.params())
        );
        assertNull(new StageCatalogs.RankContext(anchor, null, false, AlgorithmProfile.facePanel.params()).zone());
        assertEquals(candidates, ranked);
    }

    private static List<PlacementCandidate> rank(
        List<PlacementCandidate> candidates,
        ZoneWeights weights,
        ZoneCost.Context context
    ) {
        return StageCatalogs.requireRankStrategy(StageCatalogs.rankZoneCost).rank(
            candidates,
            new StageCatalogs.RankContext(
                anchor,
                null,
                false,
                AlgorithmProfile.facePanel.params(),
                new StageCatalogs.RankContext.ZoneInputs(weights, context)
            )
        );
    }

    private static ZoneWeights only(ZoneCost.Term term) {
        ZoneWeights zero = new ZoneWeights(0, 0, 0, 0, 0, 0, 0, 0, 0);
        return switch (term) {
            case overlap -> zero.withOverlap(1.0);
            case attention -> zero.withAttention(1.0);
            case temporal -> zero.withTemporal(1.0);
            case topology -> zero.withTopology(1.0);
            case crossing -> zero.withCrossing(1.0);
            default -> throw new IllegalArgumentException("not a single-term fixture: " + term);
        };
    }

    private static ZoneCost.Context context(
        ZoneWeights weights,
        Map<String, Rect> placed,
        @Nullable Rect previous,
        List<ZoneCost.Segment> leaders,
        Set<ZoneCost.Adjacency> leftOf,
        Set<ZoneCost.Adjacency> above
    ) {
        return new ZoneCost.Context(
            "me",
            anchor,
            screen,
            attention,
            placed,
            List.of(),
            previous,
            leaders,
            leftOf,
            above
        );
    }

    private static PlacementCandidate candidate(int x, int y, int w, int h) {
        return PlacementCandidate.screen(new FloatRect(x, y, w, h));
    }

    // endregion

    // region end-to-end binding

    @Test
    void aZoneSpecDrivesTheLatticeThroughTheCoordinator() {
        LayoutHarness harness = new LayoutHarness();
        AssembledElement zone = harness.assemble(zonePanelSpec("z"));
        harness.register(zone);
        CoordinationResult result = harness.frame(LayoutHarness.unanchored(), zone);
        Rect granted = Objects.requireNonNull(Objects.requireNonNull(result.placementOf("z")).screenRect()).toRect();
        List<Rect> lattice = latticeRects(new FloatPos(32, 255), granted.width(), granted.height());
        assertTrue(lattice.contains(granted), "the grant is one of the zone lattice candidates: " + granted);
        assertEquals(LodTier.full, Objects.requireNonNull(result.elementState("z")).lodTier());
    }

    @Test
    void aZonelessFacePanelStillDrivesTheAnchoredSingleCandidate() {
        LayoutHarness harness = new LayoutHarness();
        AssembledElement plain = harness.assemble(
            ElementSpec.from(InworldProfile.facePanel, "fp").withAnchor(AnchorFacet.position(1, 2, 3))
                    .withSpaces(new SpaceFacet(Set.of(SpaceMask.worldAnchored), SpacePolicy.fixed, 5))
                    .withDegrade(DegradeFacet.of(SpacePolicy.fixed, false, true, new Size(120, 90)))
        );
        harness.register(plain);
        CoordinationResult result = harness.frame(LayoutHarness.anchored(LayoutHarness.pos(200, 150)), plain);
        assertEquals(
            FloatRect.around(LayoutHarness.pos(200, 150), 120, 90),
            Objects.requireNonNull(result.placementOf("fp")).screenRect(),
            "the default path keeps candidates.single"
        );
    }

    /** A tracked zone panel for the bottom-left corner of the 400×300 harness screen. */
    static ElementSpec zonePanelSpec(String id) {
        return ElementSpec.from(InworldProfile.facePanel, id)
                .withSpaces(new SpaceFacet(Set.of(SpaceMask.screenPanel), SpacePolicy.passive, 1))
                .withDegrade(DegradeFacet.of(SpacePolicy.passive, false, true, new Size(60, 20), new Size(40, 16)))
                .withAnchor(AnchorFacet.cameraTracked(0.08, 0.85)).withZone(ZoneFacet.of());
    }

    static List<Rect> latticeRects(FloatPos anchorPos, int w, int h) {
        List<Rect> rects = new ArrayList<>();
        for (ZoneCandidates.Candidate candidate : ZoneCandidates
                .generate(anchorPos, new Size(w, h), new Rect(0, 0, LayoutHarness.width, LayoutHarness.height))) {
            rects.add(candidate.rect());
        }
        return rects;
    }

    // endregion
}
