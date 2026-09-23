package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.algorithm.AlgorithmProfile;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.PlacementCandidate;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpaceMask;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Objects;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/** The named-strategy catalogs: registration discipline and built-in geometry. */
class StageCatalogsTest {

    // region registration

    @Test
    void registrationRejectsDuplicatesAndMalformedNames() {
        StageCatalogs.registerCandidates("candidates.probeA", context -> List.of());
        assertThrows(
            IllegalArgumentException.class,
            () -> StageCatalogs.registerCandidates("candidates.probeA", context -> List.of())
        );
        StageCatalogs.unregisterCandidates("candidates.probeA");

        assertThrows(IllegalArgumentException.class, () -> StageCatalogs.registerRank("weightedLinear", (c, x) -> c));
        assertThrows(IllegalArgumentException.class, () -> StageCatalogs.registerAvoid("avoid.", (c, x) -> c));
        assertThrows(
            IllegalArgumentException.class,
            () -> StageCatalogs.registerCandidates("candidates.UpperCase", context -> List.of())
        );
    }

    @Test
    void builtinsCannotBeUnregisteredButCanBeReplaced() {
        assertThrows(
            IllegalArgumentException.class,
            () -> StageCatalogs.unregisterCandidates(StageCatalogs.candidatesSingle)
        );
        assertThrows(IllegalArgumentException.class, () -> StageCatalogs.unregisterAvoid(StageCatalogs.avoidNone));
        assertThrows(
            IllegalArgumentException.class,
            () -> StageCatalogs.unregisterRank(StageCatalogs.rankWeightedLinear)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> StageCatalogs.replaceCandidates("candidates.unknown", c -> List.of())
        );

        StageCatalogs.CandidateStrategy original = StageCatalogs
                .requireCandidateStrategy(StageCatalogs.candidatesSingle);
        try {
            StageCatalogs.replaceCandidates(StageCatalogs.candidatesSingle, context -> List.of());
            assertTrue(
                StageCatalogs.requireCandidateStrategy(StageCatalogs.candidatesSingle)
                        .candidates(context(0.0, OrientationFacet.Mode.cameraBillboard)).isEmpty()
            );
        } finally {
            StageCatalogs.replaceCandidates(StageCatalogs.candidatesSingle, original);
        }
    }

    // endregion

    // region built-in candidate strategies

    @Test
    void singleCandidateCentersOnTheAnchorAndShrinksByTheInset() {
        List<PlacementCandidate> plain = StageCatalogs.requireCandidateStrategy(StageCatalogs.candidatesSingle)
                .candidates(context(0.0, OrientationFacet.Mode.cameraBillboard));
        assertEquals(1, plain.size());
        assertEquals(FloatRect.around(LayoutHarness.pos(200, 150), 100, 26), plain.getFirst().screenRect());
        assertNotNull(plain.getFirst().world(), "a world-anchored context carries the dual representation");

        List<PlacementCandidate> inset = StageCatalogs.requireCandidateStrategy(StageCatalogs.candidatesSingle)
                .candidates(context(5.0, OrientationFacet.Mode.blockFace));
        assertEquals(FloatRect.around(LayoutHarness.pos(200, 150), 90, 16), inset.getFirst().screenRect());
    }

    @Test
    void rayFanSpreadsCandidatesAtLabelDistance() {
        List<PlacementCandidate> spread = StageCatalogs.requireCandidateStrategy(StageCatalogs.candidatesRayFan)
                .candidates(context(0.0, OrientationFacet.Mode.cameraBillboard));
        assertEquals(8, spread.size());
        for (PlacementCandidate candidate : spread) {
            FloatRect rect = Objects.requireNonNull(candidate.screenRect());
            double distance = Math.hypot(rect.centerX() - 200.0, rect.centerY() - 150.0);
            assertEquals(108.0, distance, 0.01, "radius = max(width, height) + 8");
        }
    }

    @Test
    void rayFanFallsBackToTheAnchorWhenEverythingIsBlocked() {
        LayoutEnvironment environment = LayoutHarness.unanchored().withExclusions(List.of(new Rect(100, 50, 200, 200)))
                .withAnchor(AnchorFrame.screen(LayoutHarness.pos(200, 150)));
        List<PlacementCandidate> blocked = StageCatalogs.requireCandidateStrategy(StageCatalogs.candidatesRayFan)
                .candidates(
                    new StageCatalogs.CandidateContext(
                        LayoutHarness.pos(200, 150),
                        new Size(100, 26),
                        OrientationFacet.Mode.cameraBillboard,
                        0.0,
                        environment,
                        AlgorithmProfile.nameplate.params(),
                        Set.of(SpaceMask.screenPanel)
                    )
                );
        assertEquals(1, blocked.size());
        assertEquals(FloatRect.around(LayoutHarness.pos(200, 150), 100, 26), blocked.getFirst().screenRect());
    }

    @Test
    void orbitRingStopsAtTheBaseRadius() {
        List<PlacementCandidate> slots = StageCatalogs.requireCandidateStrategy(StageCatalogs.candidatesOrbitRing)
                .candidates(context(0.0, OrientationFacet.Mode.cameraBillboard));
        assertFalse(slots.isEmpty());
        FloatRect firstSlot = Objects.requireNonNull(slots.getFirst().screenRect());
        assertEquals(60.0, firstSlot.centerX() - 200.0, 0.01, "base = max/2 + 10");
        assertEquals(0.0, firstSlot.centerY() - 150.0, 0.01);
    }

    @Test
    void dockCursorScansTheNearestEdgeAndSkipsBlockedStarts() {
        // Anchor (388, 15) on 400×300 with the dock profile's margin 8 /
        // spacing 6: nearest edge is the right one, 12 px away.
        List<PlacementCandidate> free = StageCatalogs.requireCandidateStrategy(StageCatalogs.candidatesDockCursor)
                .candidates(
                    dockContext(LayoutHarness.unanchored().withAnchor(AnchorFrame.screen(LayoutHarness.pos(388, 15))))
                );
        assertEquals(2, free.size(), "first-fit slot plus the tail alternative");
        FloatRect firstSlot = Objects.requireNonNull(free.getFirst().screenRect());
        assertEquals(292.0, firstSlot.x(), 0.01, "margin 8 from the right edge: 400 - 8 - 100");
        assertEquals(8.0, firstSlot.y(), 0.01, "first-fit at the scanline origin");

        LayoutEnvironment blockedEnv = LayoutHarness.unanchored().withExclusions(List.of(new Rect(0, 0, 400, 40)))
                .withAnchor(AnchorFrame.screen(LayoutHarness.pos(388, 15)));
        List<PlacementCandidate> blocked = StageCatalogs.requireCandidateStrategy(StageCatalogs.candidatesDockCursor)
                .candidates(dockContext(blockedEnv));
        FloatRect blockedSlot = Objects.requireNonNull(blocked.getFirst().screenRect());
        assertEquals(46.0, blockedSlot.y(), 0.01, "40 + spacing 6 pushes past the strip");
    }

    private static StageCatalogs.CandidateContext dockContext(LayoutEnvironment environment) {
        return new StageCatalogs.CandidateContext(
            LayoutHarness.pos(388, 15),
            new Size(100, 26),
            OrientationFacet.Mode.screen,
            0.0,
            environment,
            AlgorithmProfile.dock.params(),
            Set.of(SpaceMask.hudBase, SpaceMask.hudOverlay)
        );
    }

    @Test
    void excentricColumnGrowsOnTheRoomierSide() {
        StageCatalogs.CandidateContext leftFocus = new StageCatalogs.CandidateContext(
            LayoutHarness.pos(100, 150),
            new Size(120, 22),
            OrientationFacet.Mode.cameraBillboard,
            0.0,
            LayoutHarness.unanchored().withAnchor(AnchorFrame.screen(LayoutHarness.pos(100, 150))),
            AlgorithmProfile.excentric.params(),
            Set.of(SpaceMask.screenPanel)
        );
        List<PlacementCandidate> right = StageCatalogs.requireCandidateStrategy(StageCatalogs.candidatesExcentricColumn)
                .candidates(leftFocus);
        FloatRect head = Objects.requireNonNull(right.getFirst().screenRect());
        assertTrue(head.centerX() > 100.0, "focus on the left half → column right");
        assertEquals(168.0, head.centerX(), 0.01, "anchor 100 + gap 8 + half of 120");
        assertEquals(150.0, head.centerY(), 0.01);
        FloatRect second = Objects.requireNonNull(right.get(1).screenRect());
        assertEquals(176.0, second.centerY(), 0.01, "rows step by height + 4");
    }

    // endregion

    // region built-in avoid and rank strategies

    @Test
    void exclusionsFilterDropsIntersectingCandidates() {
        LayoutEnvironment environment = LayoutHarness.unanchored().withExclusions(List.of(new Rect(240, 130, 60, 40)));
        List<PlacementCandidate> candidates = List.of(
            PlacementCandidate.screen(FloatRect.around(LayoutHarness.pos(260, 150), 40, 20)),
            PlacementCandidate.screen(FloatRect.around(LayoutHarness.pos(100, 150), 40, 20))
        );
        List<PlacementCandidate> surviving = StageCatalogs.requireAvoidStrategy(StageCatalogs.avoidExclusions)
                .filter(candidates, new StageCatalogs.AvoidContext(Set.of(), true, environment));
        assertEquals(1, surviving.size());
        assertEquals(100.0, Objects.requireNonNull(surviving.getFirst().screenRect()).centerX(), 0.01);
    }

    @Test
    void maskFilterAlsoConsidersAvoidedOccupancy() {
        LayoutEnvironment environment = LayoutHarness.unanchored().withOccupancy(
            List.of(new LayoutEnvironment.MaskedRect(SpaceMask.screenPanel, "panel-1", new Rect(240, 130, 60, 40)))
        );
        List<PlacementCandidate> candidates = List.of(
            PlacementCandidate.screen(FloatRect.around(LayoutHarness.pos(260, 150), 40, 20)),
            PlacementCandidate.screen(FloatRect.around(LayoutHarness.pos(100, 150), 40, 20))
        );
        StageCatalogs.AvoidContext avoidingPanels = new StageCatalogs.AvoidContext(
            Set.of(SpaceMask.screenPanel),
            false,
            environment
        );
        List<PlacementCandidate> surviving = StageCatalogs.requireAvoidStrategy(StageCatalogs.avoidExclusionsAndMasks)
                .filter(candidates, avoidingPanels);
        assertEquals(1, surviving.size());
        assertEquals(100.0, Objects.requireNonNull(surviving.getFirst().screenRect()).centerX(), 0.01);

        StageCatalogs.AvoidContext avoidingNothing = new StageCatalogs.AvoidContext(Set.of(), false, environment);
        assertEquals(
            2,
            StageCatalogs.requireAvoidStrategy(StageCatalogs.avoidExclusionsAndMasks)
                    .filter(candidates, avoidingNothing).size(),
            "occupancy on un-avoided layers does not block"
        );
    }

    @Test
    void weightedRankPrefersTheCandidateNearestTheAnchor() {
        List<PlacementCandidate> candidates = List.of(
            PlacementCandidate.screen(FloatRect.around(LayoutHarness.pos(260, 150), 40, 20)),
            PlacementCandidate.screen(FloatRect.around(LayoutHarness.pos(220, 150), 40, 20))
        );
        List<PlacementCandidate> ranked = StageCatalogs.requireRankStrategy(StageCatalogs.rankWeightedLinear).rank(
            candidates,
            new StageCatalogs.RankContext(LayoutHarness.pos(200, 150), null, false, AlgorithmProfile.nameplate.params())
        );
        assertEquals(220.0, Objects.requireNonNull(ranked.getFirst().screenRect()).centerX(), 0.01);
    }

    @Test
    void incumbentRankMovesTheMatchingCandidateFirst() {
        List<PlacementCandidate> candidates = List.of(
            PlacementCandidate.screen(FloatRect.around(LayoutHarness.pos(260, 150), 40, 20)),
            PlacementCandidate.screen(FloatRect.around(LayoutHarness.pos(220, 150), 40, 20))
        );
        List<PlacementCandidate> ranked = StageCatalogs.requireRankStrategy(StageCatalogs.rankIncumbentFirst).rank(
            candidates,
            new StageCatalogs.RankContext(
                LayoutHarness.pos(200, 150),
                LayoutHarness.pos(260, 150),
                true,
                AlgorithmProfile.nameplate.params()
            )
        );
        assertEquals(
            260.0,
            Objects.requireNonNull(ranked.getFirst().screenRect()).centerX(),
            0.01,
            "the incumbent slot leads"
        );
        ranked = StageCatalogs.requireRankStrategy(StageCatalogs.rankIncumbentFirst).rank(
            candidates,
            new StageCatalogs.RankContext(LayoutHarness.pos(200, 150), null, true, AlgorithmProfile.nameplate.params())
        );
        assertEquals(260.0, ranked.getFirst().screenRect().centerX(), 0.01, "without an incumbent the order stands");
    }

    // endregion

    private static StageCatalogs.CandidateContext context(double inset, OrientationFacet.Mode mode) {
        return new StageCatalogs.CandidateContext(
            LayoutHarness.pos(200, 150),
            new Size(100, 26),
            mode,
            inset,
            LayoutHarness.unanchored().withAnchor(AnchorFrame.screen(LayoutHarness.pos(200, 150))),
            AlgorithmProfile.nameplate.params(),
            Set.of(SpaceMask.screenPanel)
        );
    }
}
