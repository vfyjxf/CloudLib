package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderRouterTest {

    private static final LeaderRouter.Config config = LeaderRouter.Config.of(1.0, 2);

    private static LeaderRouter.Leader leader(String id, double ax, double ay, double lx, double ly) {
        return LeaderRouter.Leader.toPoint(id, ax, ay, lx, ly);
    }

    private static List<LeaderRouter.Leader> crossingPair() {
        return List.of(leader("a", 0, 0, 100, 100), leader("b", 100, 0, 0, 100));
    }

    private static List<LeaderRouter.Leader> parallelPair() {
        return List.of(leader("a", 0, 0, 0, 100), leader("b", 100, 0, 100, 100));
    }

    @Test
    void straightLeaderIsTheDefault() {
        LeaderRouter router = new LeaderRouter(config);

        LeaderRouter.Route route = router.route(parallelPair(), Map.of()).get(0);

        assertEquals(LeaderRouter.Style.sLeader, route.style());
        assertEquals(2, route.points().size());
        assertEquals(0, route.crossings());
        assertNull(route.clusterId());
    }

    @Test
    void crossingUpgradesToOrthogonalAfterTheDwell() {
        LeaderRouter router = new LeaderRouter(config);

        assertEquals(
                LeaderRouter.Style.sLeader,
                router.route(crossingPair(), Map.of()).get(0).style());
        assertEquals(
                LeaderRouter.Style.poLeader,
                router.route(crossingPair(), Map.of()).get(0).style());
        assertEquals(
                LeaderRouter.Style.poLeader,
                router.route(crossingPair(), Map.of()).get(0).style());
    }

    @Test
    void poRoutesFreezeTheStubAndTheArrivalDirection() {
        LeaderRouter router = new LeaderRouter(config);
        router.route(crossingPair(), Map.of());
        LeaderRouter.Route route = router.route(crossingPair(), Map.of()).get(0);

        assertEquals(LeaderRouter.Style.poLeader, route.style());
        List<FloatPos> points = route.points();
        assertTrue(points.size() >= 3, "an orthogonal route bends at least once");
        // the first segment leaves the anchor along the frozen stub axis
        assertEquals(0, points.get(0).x(), 1.0e-9);
        assertEquals(0, points.get(0).y(), 1.0e-9);
        assertEquals(points.get(0).x(), points.get(1).x(), 1.0e-9, "vertical stub off (0,0)");
        // the final segment arrives along the port normal (−1,0) → moving +x
        FloatPos approach = points.get(points.size() - 2);
        FloatPos end = points.get(points.size() - 1);
        assertEquals(approach.y(), end.y(), 1.0e-9);
        assertTrue(end.x() - approach.x() >= 20.0 - 1.0e-9, "the final segment keeps its 20 px minimum");
        assertEquals(94, end.x(), 1.0e-9, "the line stops 6 px short of the label point");
        assertEquals(100, end.y(), 1.0e-9);
    }

    @Test
    void sustainedAbsenceDowngradesBackToStraight() {
        LeaderRouter router = new LeaderRouter(config);

        router.route(crossingPair(), Map.of());
        router.route(crossingPair(), Map.of());
        assertEquals(
                LeaderRouter.Style.poLeader,
                router.route(crossingPair(), Map.of()).get(0).style());

        assertEquals(
                LeaderRouter.Style.poLeader,
                router.route(parallelPair(), Map.of()).get(0).style());
        assertEquals(
                LeaderRouter.Style.sLeader,
                router.route(parallelPair(), Map.of()).get(0).style());
    }

    @Test
    void boundaryJitterNeverFlipsTheStyle() {
        LeaderRouter router = new LeaderRouter(config);

        // alternating crossing / free: the dwell never accumulates, no upgrade
        for (int i = 0; i < 30; i++) {
            List<LeaderRouter.Leader> leaders = (i % 2 == 0) ? crossingPair() : parallelPair();
            assertEquals(
                    LeaderRouter.Style.sLeader,
                    router.route(leaders, Map.of()).get(0).style());
        }

        // now commit the upgrade...
        router.route(crossingPair(), Map.of());
        assertEquals(
                LeaderRouter.Style.poLeader,
                router.route(crossingPair(), Map.of()).get(0).style());

        // ...and the same jitter cannot downgrade it either
        for (int i = 0; i < 30; i++) {
            List<LeaderRouter.Leader> leaders = (i % 2 == 0) ? parallelPair() : crossingPair();
            assertEquals(
                    LeaderRouter.Style.poLeader,
                    router.route(leaders, Map.of()).get(0).style());
        }
    }

    @Test
    void clustersShareATrunkHyperleader() {
        LeaderRouter router = new LeaderRouter(config);
        List<LeaderRouter.Leader> leaders =
                List.of(leader("a", 0, 0, 200, -50), leader("b", 10, 10, 200, 0), leader("c", 20, 0, 200, 50));
        Map<String, String> clusters = Map.of("a", "g1", "b", "g1", "c", "g1");

        List<LeaderRouter.Route> routes = router.route(leaders, clusters);

        assertEquals(3, routes.size());
        for (LeaderRouter.Route route : routes) {
            assertEquals(LeaderRouter.Style.hyperLeader, route.style());
            assertEquals("g1", route.clusterId());
            // trunk is the anchor centroid (10, 10/3)
            assertEquals(10, route.points().get(1).x(), 1.0e-9);
            assertEquals(10.0 / 3.0, route.points().get(1).y(), 1.0e-9);
        }
    }

    @Test
    void sameClusterPairsAreExemptFromCrossingDetection() {
        LeaderRouter router = new LeaderRouter(LeaderRouter.Config.of(1.0, 1));
        // a and b cross each other but share a cluster; c crosses a and is alone
        List<LeaderRouter.Leader> leaders =
                List.of(leader("a", 0, 0, 100, 100), leader("b", 100, 0, 0, 100), leader("c", 50, -50, 50, 150));
        Map<String, String> clusters = Map.of("a", "g1", "b", "g1");

        List<LeaderRouter.Route> routes = router.route(leaders, clusters);
        Map<String, LeaderRouter.Route> byId = new HashMap<>();
        routes.forEach(route -> byId.put(route.id(), route));

        assertEquals(LeaderRouter.Style.hyperLeader, byId.get("a").style());
        assertEquals(LeaderRouter.Style.hyperLeader, byId.get("b").style());
        // c crosses a, so c upgrades immediately (dwell 1); a is hyper regardless
        assertEquals(LeaderRouter.Style.poLeader, byId.get("c").style());
    }

    @Test
    void singletonClusterIdsDoNotBecomeHyperleaders() {
        LeaderRouter router = new LeaderRouter(config);

        LeaderRouter.Route route =
                router.route(parallelPair(), Map.of("a", "lonely")).get(0);

        assertEquals(LeaderRouter.Style.sLeader, route.style());
        assertNull(route.clusterId());
    }

    @Test
    void resetDropsGateState() {
        LeaderRouter router = new LeaderRouter(config);
        router.route(crossingPair(), Map.of());
        router.route(crossingPair(), Map.of());
        router.reset();

        assertEquals(
                LeaderRouter.Style.sLeader,
                router.route(crossingPair(), Map.of()).get(0).style());
    }

    @Test
    void leavingLeadersForgetTheirGates() {
        LeaderRouter router = new LeaderRouter(config);
        router.route(crossingPair(), Map.of());
        router.route(crossingPair(), Map.of());

        // a alone crosses nothing; its po commit still holds for the dwell
        assertEquals(
                LeaderRouter.Style.poLeader,
                router.route(List.of(leader("a", 0, 0, 100, 100)), Map.of())
                        .get(0)
                        .style());

        // b re-enters with no memory of its old po commit
        assertEquals(
                LeaderRouter.Style.sLeader,
                router.route(crossingPair(), Map.of()).get(1).style());
    }

    @Test
    void closeParallelBaselinesUpgradeAfterTheDwell() {
        LeaderRouter router = new LeaderRouter(config);
        // two vertical straights 4 px apart — dense parallels read as chaotic
        List<LeaderRouter.Leader> pair = List.of(leader("a", 0, 0, 0, 200), leader("b", 4, 0, 4, 200));

        assertEquals(
                LeaderRouter.Style.sLeader, router.route(pair, Map.of()).get(0).style());
        assertEquals(
                LeaderRouter.Style.poLeader, router.route(pair, Map.of()).get(0).style());
        assertEquals(
                LeaderRouter.Style.poLeader, router.route(pair, Map.of()).get(0).style());
    }

    @Test
    void aBaselineThroughAnObstacleRoutesOrthogonalImmediately() {
        LeaderRouter router = new LeaderRouter(config);
        // the wall sits right on the straight baseline
        List<FloatRect> obstacles = List.of(new FloatRect(90, 40, 20, 20));

        LeaderRouter.Route route = router.route(List.of(leader("a", 0, 0, 200, 100)), Map.of(), obstacles)
                .get(0);

        assertEquals(LeaderRouter.Style.poLeader, route.style(), "a leader may not cross a panel — hard override");
        assertTrue(route.points().size() >= 3);
        assertFalse(
                LeaderGridRouter.polylineBlocked(
                        route.points(),
                        new FloatPos(200, 100),
                        obstacles,
                        config.routing().clearancePx()),
                "the drawn polyline clears the wall");
    }

    @Test
    void nearTargetWithinTheOldToleranceNowFolds() {
        LeaderRouter router = new LeaderRouter(config);

        LeaderRouter.Route route =
                router.route(List.of(leader("a", 0, 0, 50, 30)), Map.of()).get(0);

        // 58 px used to be inside the 80 px blank tolerance; the ladder keeps
        // the pairing: the fold band's one direct segment, both ends touching
        assertEquals(LeaderRouter.Tier.fold, route.tier());
        assertEquals(2, route.points().size());
        assertEquals(new FloatPos(0, 0), route.points().get(0), "the anchor end, exactly");
        assertEquals(new FloatPos(50, 30), route.points().get(1), "the panel end on the port, no arrival gap");
    }

    @Test
    void theTierLadderStepsBothBoundariesWithAnEightPixelSchmittBand() {
        LeaderRouter router = new LeaderRouter(config);

        assertEquals(LeaderRouter.Tier.full, tierAt(router, 100), "far out: the full routing");
        assertEquals(LeaderRouter.Tier.full, tierAt(router, 80), "above the falling edge (76) full holds");
        assertEquals(LeaderRouter.Tier.fold, tierAt(router, 75), "below 76 the fold segment takes over");
        assertEquals(LeaderRouter.Tier.fold, tierAt(router, 83.9), "below the rising edge (84) fold holds");
        assertEquals(LeaderRouter.Tier.full, tierAt(router, 84), "at 84 the full routing returns");
        assertEquals(LeaderRouter.Tier.full, tierAt(router, 76.1), "above 76 full holds again");
        assertEquals(LeaderRouter.Tier.fold, tierAt(router, 42.5), "down through the fold band");
        assertEquals(LeaderRouter.Tier.attach, tierAt(router, 41.9), "below 42 the line gives way to the marks");
        assertEquals(LeaderRouter.Tier.attach, tierAt(router, 49.9), "inside the band (below 50) attach holds");
        assertEquals(LeaderRouter.Tier.fold, tierAt(router, 50), "at 50 the fold segment returns");
        assertEquals(LeaderRouter.Tier.attach, tierAt(router, 41.9), "and below 42 it detaches again");
        assertEquals(
                LeaderRouter.Tier.attach, tierAt(router, 45), "a boundary jittering inside the band stays attached");

        // a distance oscillating entirely inside the 76–84 hold band never
        // flips the tier, whichever side it entered from
        LeaderRouter steady = new LeaderRouter(config);
        tierAt(steady, 90);
        for (int i = 0; i < 20; i++) {
            assertEquals(LeaderRouter.Tier.full, tierAt(steady, i % 2 == 0 ? 77 : 83), "epoch " + i);
        }
        LeaderRouter folding = new LeaderRouter(config);
        tierAt(folding, 60);
        for (int i = 0; i < 20; i++) {
            assertEquals(LeaderRouter.Tier.fold, tierAt(folding, i % 2 == 0 ? 77 : 83), "epoch " + i);
        }
    }

    @Test
    void aFreshLeaderEntersAtThePlainTierBoundaries() {
        assertEquals(LeaderRouter.Tier.attach, tierAt(new LeaderRouter(config), 41));
        assertEquals(LeaderRouter.Tier.fold, tierAt(new LeaderRouter(config), 42));
        assertEquals(LeaderRouter.Tier.fold, tierAt(new LeaderRouter(config), 83.9));
        assertEquals(LeaderRouter.Tier.full, tierAt(new LeaderRouter(config), 84));
    }

    @Test
    void theFoldSegmentLandsOnTheNearestOfTheEightBorderCandidates() {
        // the port is a decoy on the left edge; the fold still picks the
        // nearest border candidate — the top midpoint — and touches both ends
        FloatRect rect = new FloatRect(100, 100, 40, 20);
        LeaderRouter.Leader near = new LeaderRouter.Leader(
                "a",
                120,
                55,
                new AttachPointResolver.Port(AttachPointResolver.Face.left, new FloatPos(100, 110), -1, 0),
                rect);

        LeaderRouter.Route route =
                new LeaderRouter(config).route(List.of(near), Map.of()).get(0);

        assertEquals(LeaderRouter.Tier.fold, route.tier());
        assertEquals(List.of(new FloatPos(120, 55), new FloatPos(120, 100)), route.points());
    }

    @Test
    void aShallowFoldLeavesTheAnchorVerticallyBeforeCuttingDiagonal() {
        // 78.7° off vertical — past the 60° branch: the anchor end leaves
        // along a short vertical run (half the vertical gap), then the
        // diagonal lands on the nearest candidate corner
        FloatRect rect = new FloatRect(100, 100, 40, 20);
        LeaderRouter.Leader shallow = new LeaderRouter.Leader(
                "a",
                190,
                90,
                new AttachPointResolver.Port(AttachPointResolver.Face.right, new FloatPos(140, 110), 1, 0),
                rect);

        LeaderRouter.Route route =
                new LeaderRouter(config).route(List.of(shallow), Map.of()).get(0);

        assertEquals(LeaderRouter.Tier.fold, route.tier());
        List<FloatPos> points = route.points();
        assertEquals(3, points.size(), "vertical run + diagonal: " + points);
        assertEquals(points.get(0).x(), points.get(1).x(), 1.0e-9, "the first segment off the anchor is vertical");
        assertEquals(95, points.get(1).y(), 1.0e-9, "half the 10 px vertical gap");
        assertEquals(new FloatPos(140, 100), points.get(2), "the diagonal lands on the nearest corner");
    }

    @Test
    void theFoldAlphaRampsFromHalfAtTheFoldEdgeToFullAtTheFullEdge() {
        LeaderRouter router = new LeaderRouter(config);

        // half ink at the fold boundary, full at the full one — the near end
        // stays legible (dimmer than the full routing, never invisible)
        assertEquals(0.5, alphaAt(router, 42), 1.0e-9, "the fold segment materialises at half ink at 42 px");
        assertEquals(0.75, alphaAt(router, 63), 1.0e-9, "halfway across the band");
        assertEquals(0.5 + 0.5 * ((83.9 - 42) / 42), alphaAt(router, 83.9), 1.0e-9, "approaching full");
        assertEquals(1.0, alphaAt(router, 100), 1.0e-9, "the full tier draws at full alpha");
        // a fresh leader at 30 px is attach (the shared ladder above would
        // still be one epoch from full — its steps are single-rung)
        assertEquals(0.0, alphaAt(new LeaderRouter(config), 30), 1.0e-9, "the attach tier has no stroke to fade");
    }

    @Test
    void theAttachTierFlagsTheRouteAndDrawsNoPolyline() {
        LeaderRouter router = new LeaderRouter(config);

        LeaderRouter.Route route =
                router.route(List.of(leader("a", 0, 0, 30, 0)), Map.of()).get(0);

        assertEquals(LeaderRouter.Tier.attach, route.tier());
        assertTrue(route.points().isEmpty(), "the attach tier draws no line — only the caller's marks");
        assertEquals(0.0, route.alpha(), 1.0e-9);
    }

    @Test
    void aBareLabelPointFoldsOntoThePortItself() {
        LeaderRouter.Route route = new LeaderRouter(config)
                .route(List.of(leader("a", 0, 0, 60, 0)), Map.of())
                .get(0);

        assertEquals(LeaderRouter.Tier.fold, route.tier());
        assertEquals(List.of(new FloatPos(0, 0), new FloatPos(60, 0)), route.points());
    }

    @Test
    void aTierFlipBumpsTheShapeEpoch() {
        LeaderRouter router = new LeaderRouter(config);

        long fold =
                router.route(List.of(leader("a", 0, 0, 60, 0)), Map.of()).get(0).shapeEpoch();
        long full = router.route(List.of(leader("a", 0, 0, 100, 0)), Map.of())
                .get(0)
                .shapeEpoch();

        assertTrue(fold != full, "the tier change reads as a topology change");
    }

    @Test
    void aFoldCandidateFlipBumpsTheShapeEpochWhileSlidesKeepIt() {
        FloatRect rect = new FloatRect(100, 100, 40, 20);
        AttachPointResolver.Port port =
                new AttachPointResolver.Port(AttachPointResolver.Face.top, new FloatPos(120, 100), 0, -1);
        LeaderRouter router = new LeaderRouter(config);

        long first = router.route(List.of(new LeaderRouter.Leader("a", 120, 55, port, rect)), Map.of())
                .get(0)
                .shapeEpoch();
        long slide = router.route(List.of(new LeaderRouter.Leader("a", 121, 56, port, rect)), Map.of())
                .get(0)
                .shapeEpoch();
        assertEquals(first, slide, "endpoints sliding under the same candidate is not a topology change");

        long flipped = router.route(List.of(new LeaderRouter.Leader("a", 120, 155, port, rect)), Map.of())
                .get(0)
                .shapeEpoch();
        assertTrue(flipped != first, "crossing to another border candidate re-tokens the shape");
    }

    private static LeaderRouter.Tier tierAt(LeaderRouter router, double d) {
        return router.route(List.of(leader("a", 0, 0, d, 0)), Map.of()).get(0).tier();
    }

    private static double alphaAt(LeaderRouter router, double d) {
        return router.route(List.of(leader("a", 0, 0, d, 0)), Map.of()).get(0).alpha();
    }

    @Test
    void anchorMicroJitterReusesTheCommittedInterior() {
        // the lane blocker sits on the 1-fold corridor (making the 2-fold jog
        // cheaper) and the far blocker cuts the straight baseline — the po
        // route is the two-bend jog whose interior lane pins to x=92
        List<FloatRect> obstacles = List.of(new FloatRect(-10, 60, 20, 20), new FloatRect(60, 20, 20, 20));
        LeaderRouter router = new LeaderRouter(config);
        LeaderRouter.Route first = router.route(List.of(leader("a", 0, 0, 200, 100)), Map.of(), obstacles)
                .get(0);
        assertTrue(first.points().contains(new FloatPos(92, 0)));
        assertTrue(first.points().contains(new FloatPos(92, 100)));

        // a 5 px anchor move stays inside the 12 px quantization cell: the
        // committed lane x=92 stands still — the head joint rides it to the
        // exact anchor instead of the old frozen vertex (92, 0)
        LeaderRouter.Route second = router.route(List.of(leader("a", 3, 4, 200, 100)), Map.of(), obstacles)
                .get(0);

        assertEquals(new FloatPos(3, 4), second.points().get(0), "the exact anchor leads the line");
        assertTrue(second.points().contains(new FloatPos(92, 100)), "the committed lane's far bend stands still");
        assertEquals(
                92, second.points().get(1).x(), 1.0e-9, "the head joint rides the committed lane: " + second.points());
        assertEquals(4, second.points().get(1).y(), 1.0e-9, "…at the exact anchor's height");
    }

    @Test
    void aMarginalShorterRouteIsNotAdopted() {
        List<FloatRect> obstacles = List.of(new FloatRect(-10, 60, 20, 20), new FloatRect(60, 20, 20, 20));
        LeaderRouter router = new LeaderRouter(config);
        router.route(List.of(leader("a", 0, 0, 200, 100)), Map.of(), obstacles);

        // 10 px up crosses the quantization cell — the fresh route saves
        // ~10 px of jog, under the 20 px switch cost, and the stretched lane
        // still clears the walls: the committed lane holds
        LeaderRouter.Route held = router.route(List.of(leader("a", 0, -10, 200, 100)), Map.of(), obstacles)
                .get(0);
        long laneVertices = held.points().stream()
                .filter(p -> Math.abs(p.x() - 92) < 1.0e-9)
                .count();
        assertTrue(
                laneVertices >= 2,
                "a 10 px saving does not buy a re-route: the committed lane survives — " + held.points());
    }

    @Test
    void aClearShorterRouteIsAdopted() {
        List<FloatRect> obstacles = List.of(new FloatRect(-10, 60, 20, 20), new FloatRect(60, 20, 20, 20));
        LeaderRouter router = new LeaderRouter(config);
        router.route(List.of(leader("a", 0, 0, 200, 100)), Map.of(), obstacles);

        // 25 px down: the fresh route saves ~45 px — past the 20 px switch cost
        LeaderRouter.Route adopted = router.route(List.of(leader("a", 0, 25, 200, 100)), Map.of(), obstacles)
                .get(0);
        assertFalse(adopted.points().contains(new FloatPos(92, 0)), "the jog re-routed to the new anchor height");
        assertTrue(adopted.points().contains(new FloatPos(48, 100)));
    }

    @Test
    void anUnroutableAnchorStillDrawsTheElbowFallback() {
        // the ceiling blocks the vertical stub, the wide seal blocks every
        // horizontal jog back to the port line — no grid route exists, the
        // fallback elbow must still draw
        List<FloatRect> obstacles = List.of(new FloatRect(-30, -45, 60, 26), new FloatRect(4, 84, 190, 60));
        LeaderRouter router = new LeaderRouter(config);

        LeaderRouter.Route route = router.route(List.of(leader("a", 0, 0, 200, 100)), Map.of(), obstacles)
                .get(0);

        assertEquals(LeaderRouter.Style.poLeader, route.style());
        assertTrue(route.points().size() >= 2, "never a blank frame");
        assertFalse(route.points().isEmpty());
    }

    @Test
    void crossingLeadersRerouteUntilTheyStopCrossing() {
        // a's orthogonal route spans the screen at y=100; b's straight-down
        // route crosses it mid-span — the zero-crossing pass must move one
        // of the pair until the crossing is gone
        LeaderRouter router = new LeaderRouter(config);
        List<LeaderRouter.Leader> pair = List.of(leader("a", 0, 100, 400, 0), leader("b", 200, 200, 200, -400));

        router.route(pair, Map.of());
        List<LeaderRouter.Route> routes = router.route(pair, Map.of());

        int total = routes.stream().mapToInt(LeaderRouter.Route::crossings).sum();
        assertEquals(0, total, "the pass reroutes one of the pair until the crossing is gone");
        assertTrue(
                routes.get(0).points().size() >= 5 || routes.get(1).points().size() >= 5,
                "one of the pair detoured around the other's lane");
    }

    @Test
    void unavoidableCrossingsAreReportedNotHidden() {
        // both labels sit on the same arrival line: the two orthogonal routes
        // overlap collinearly and no reroute can separate them — the routes
        // report their crossings for the caller's budget to trim
        LeaderRouter router = new LeaderRouter(config);
        List<LeaderRouter.Leader> pair = List.of(leader("a", 0, 0, 300, 300), leader("b", 300, 0, 0, 300));

        router.route(pair, Map.of());
        List<LeaderRouter.Route> routes = router.route(pair, Map.of());

        assertTrue(routes.stream().mapToInt(LeaderRouter.Route::crossings).sum() > 0, "the stuck crossing is reported");
    }

    @Test
    void replayingTheSameEpochsReproducesTheSameRoutes() {
        List<LeaderRouter.Leader> pair = List.of(leader("a", 0, 100, 400, 0), leader("b", 200, 200, 200, -400));
        List<FloatRect> obstacles = List.of(new FloatRect(150, 40, 40, 20));

        List<List<LeaderRouter.Route>> runOne = replay(pair, obstacles);
        List<List<LeaderRouter.Route>> runTwo = replay(pair, obstacles);
        assertEquals(runOne, runTwo);
    }

    @Test
    void aHundredReplaysOfTheSameEpochsAreByteIdentical() {
        List<LeaderRouter.Leader> pair = List.of(leader("a", 0, 100, 400, 0), leader("b", 200, 200, 200, -400));
        List<FloatRect> obstacles = List.of(new FloatRect(150, 40, 40, 20), new FloatRect(250, 120, 30, 60));

        List<List<LeaderRouter.Route>> expected = replay(pair, obstacles);
        for (int run = 0; run < 100; run++) {
            assertEquals(expected, replay(pair, obstacles), "replay " + run + " diverged");
        }
    }

    @Test
    void aFreshOrthogonalRouteNeverFoldsMoreThanTwice() {
        // four blocked baselines, each passable in one Z: a wall between an
        // above-baseline anchor and a below-baseline port, a taller one in a
        // longer run, a wall right off the anchor, and a wall near the port
        List<Object[]> geometries = List.of(
                new Object[] {leader("a", 0, 0, 200, 100), new FloatRect(90, 40, 20, 20)},
                new Object[] {leader("b", 0, 0, 300, 100), new FloatRect(140, 20, 20, 40)},
                new Object[] {leader("c", 0, 0, 200, 100), new FloatRect(40, 20, 20, 20)},
                new Object[] {leader("d", 0, 0, 300, 60), new FloatRect(150, 30, 20, 20)});

        for (Object[] geometry : geometries) {
            LeaderRouter.Leader one = (LeaderRouter.Leader) geometry[0];
            List<FloatRect> walls = List.of((FloatRect) geometry[1]);
            LeaderRouter.Route route = new LeaderRouter(config)
                    .route(List.of(one), Map.of(), walls)
                    .get(0);

            assertEquals(LeaderRouter.Style.poLeader, route.style(), one.id() + " upgrades off the blocked baseline");
            assertTrue(
                    route.points().size() - 2 <= 2,
                    one.id() + " folds at most twice: " + (route.points().size() - 2));
            assertFalse(
                    LeaderGridRouter.polylineBlocked(
                            route.points(),
                            one.port().point(),
                            walls,
                            config.routing().clearancePx()),
                    one.id() + " clears the wall");
        }
    }

    @Test
    void aRoutedEpochLeavesNoCrossings() {
        LeaderRouter router = new LeaderRouter(config);
        List<LeaderRouter.Leader> leaders = List.of(
                leader("a", 0, 100, 400, 0), // inverts against b — their baselines cross
                leader("b", 200, 200, 200, -400),
                leader("c", 0, 500, 400, 700)); // far from both, shares no lane

        router.route(leaders, Map.of());
        List<LeaderRouter.Route> routes = router.route(leaders, Map.of());

        assertEquals(
                0,
                routes.stream().mapToInt(LeaderRouter.Route::crossings).sum(),
                "every crossing is routed away — the constraint is hard, not a preference");
    }

    @Test
    void aPortThatSwitchesFaceRebuildsTheApproach() {
        // the same leader, its port first on the panel's left border then on
        // its bottom border: the committed approach must follow the new face,
        // never re-join a stale one across the panel
        List<FloatRect> walls = List.of(new FloatRect(48, 12, 24, 88));
        LeaderRouter.Leader left = new LeaderRouter.Leader(
                "a", 0, 0, new AttachPointResolver.Port(AttachPointResolver.Face.left, new FloatPos(200, 50), -1, 0));
        LeaderRouter.Leader bottom = new LeaderRouter.Leader(
                "a",
                0,
                0,
                new AttachPointResolver.Port(AttachPointResolver.Face.bottom, new FloatPos(150, 200), 0, -1));

        LeaderRouter router = new LeaderRouter(config);
        router.route(List.of(left), Map.of(), walls);
        List<FloatPos> path =
                router.route(List.of(bottom), Map.of(), walls).get(0).points();

        FloatPos end = path.get(path.size() - 1);
        FloatPos approach = path.get(path.size() - 2);
        assertEquals(approach.x(), end.x(), 1.0e-9, "the tail now arrives along the bottom face's normal");
        assertTrue(approach.y() < end.y(), "…descending onto the border: " + approach.y() + " → " + end.y());
        assertPoint(end, 150, 194, "the drawn end keeps the arrival gap");
    }

    private static void assertPoint(FloatPos actual, double x, double y, String what) {
        assertEquals(x, actual.x(), 1.0e-9, what + " x");
        assertEquals(y, actual.y(), 1.0e-9, what + " y");
    }

    private static List<List<LeaderRouter.Route>> replay(List<LeaderRouter.Leader> pair, List<FloatRect> obstacles) {
        LeaderRouter router = new LeaderRouter(config);
        List<List<LeaderRouter.Route>> epochs = new ArrayList<>();
        for (int i = 0; i < 5; i++) {
            epochs.add(router.route(pair, Map.of(), obstacles));
        }
        return epochs;
    }

    @Test
    void aViewportBoundKeepsEveryRoutedPointOnScreen() {
        // anchors hugging each screen edge plus walls mid-field: every
        // emitted point must stay inside the 400×300 gui rect — po routes,
        // straights and fallback elbows alike
        FloatRect viewport = new FloatRect(0, 0, 400, 300);
        List<FloatRect> obstacles = List.of(new FloatRect(150, 150, 100, 170), new FloatRect(60, 60, 60, 60));
        LeaderRouter router = new LeaderRouter(LeaderRouter.Config.of(1.0, 1));
        double[] xs = {2, 40, 200, 396};
        double[] ys = {2, 60, 150, 296};
        for (double ax : xs) {
            for (double ay : ys) {
                LeaderRouter.Route route = router.route(
                                List.of(leader("a", ax, ay, 30, 30)), Map.of(), obstacles, viewport)
                        .get(0);
                assertInside(route.points(), viewport, "anchor (" + ax + ", " + ay + ")");
            }
        }
    }

    @Test
    void aCommittedRouteLeavingTheViewportIsRerouted() {
        // epoch one unbounded: the commit dips below the wall onto the y=332
        // lane — off a 300 px-tall screen; the same epoch bounded must
        // re-route instead of re-emitting the committed shape
        LeaderRouter router = new LeaderRouter(LeaderRouter.Config.of(1.0, 1));
        List<FloatRect> obstacles = List.of(new FloatRect(150, 150, 100, 170));
        List<LeaderRouter.Leader> leaders = List.of(leader("a", 40, 280, 360, 280));
        FloatRect viewport = new FloatRect(0, 0, 400, 300);

        LeaderRouter.Route unbounded =
                router.route(leaders, Map.of(), obstacles).get(0);
        assertEquals(LeaderRouter.Style.poLeader, unbounded.style());
        assertTrue(
                unbounded.points().stream().anyMatch(p -> p.y() > 300),
                "the unbounded commit dips off-screen: " + unbounded.points());

        LeaderRouter.Route bounded =
                router.route(leaders, Map.of(), obstacles, viewport).get(0);
        assertEquals(LeaderRouter.Style.poLeader, bounded.style());
        assertInside(bounded.points(), viewport, "re-routed");
    }

    @Test
    void theElbowFallbackStaysInsideTheViewport() {
        // the same sealed geometry as anUnroutableAnchorStillDrawsTheElbowFallback,
        // under a 150×200 viewport the elbow's tail would overshoot — the
        // ends clamp onto the edge, never emitting an off-screen point
        FloatRect viewport = new FloatRect(0, 0, 150, 200);
        List<FloatRect> obstacles = List.of(new FloatRect(-30, -45, 60, 26), new FloatRect(4, 84, 190, 60));
        LeaderRouter router = new LeaderRouter(config);

        LeaderRouter.Route route = router.route(List.of(leader("a", 0, 0, 200, 100)), Map.of(), obstacles, viewport)
                .get(0);

        assertEquals(LeaderRouter.Style.poLeader, route.style());
        assertTrue(route.points().size() >= 2, "never a blank frame");
        assertInside(route.points(), viewport, "elbow");
    }

    @Test
    void aSlidingAnchorKeepsTheStretchedRouteOrthogonal() {
        // camera-pan regression: the committed two-bend jog reuses while the
        // anchor slides — every joint the stretch re-emits must meet the
        // frozen interior at a right angle, never as a per-frame diagonal
        List<FloatRect> obstacles = List.of(new FloatRect(-10, 60, 20, 20), new FloatRect(60, 20, 20, 20));
        LeaderRouter router = new LeaderRouter(config);
        router.route(List.of(leader("a", 0, 0, 200, 100)), Map.of(), obstacles);

        for (int frame = 1; frame <= 120; frame++) {
            double ax = frame * 0.5;
            double ay = frame * 0.25;
            List<FloatPos> points = router.route(List.of(leader("a", ax, ay, 200, 100)), Map.of(), obstacles)
                    .get(0)
                    .points();
            assertOrthogonal(points, "frame " + frame);
        }
    }

    @Test
    void aWithinCellAnchorSlideMovesNoVertexFartherThanTheAnchor() {
        // 0.5 px/frame for 20 frames stays inside the 12 px reuse cell: the
        // interior lanes stand still and the joints ride them — no vertex may
        // move farther per frame than the anchor itself does (0.71 px)
        List<FloatRect> obstacles = List.of(new FloatRect(-10, 60, 20, 20), new FloatRect(60, 20, 20, 20));
        LeaderRouter router = new LeaderRouter(config);
        router.route(List.of(leader("a", 0, 0, 200, 100)), Map.of(), obstacles);

        List<FloatPos> previous = null;
        for (int frame = 1; frame <= 20; frame++) {
            List<FloatPos> points = router.route(
                            List.of(leader("a", frame * 0.5, frame * 0.25, 200, 100)), Map.of(), obstacles)
                    .get(0)
                    .points();
            assertOrthogonal(points, "frame " + frame);
            if (previous != null && previous.size() == points.size()) {
                for (int i = 0; i < points.size(); i++) {
                    double move = Math.hypot(
                            points.get(i).x() - previous.get(i).x(),
                            points.get(i).y() - previous.get(i).y());
                    assertTrue(move <= 0.8, "vertex " + i + " jumped " + move + " px at frame " + frame);
                }
            }
            previous = points;
        }
    }

    @Test
    void aSlidingPortKeepsTheStretchedTailOrthogonal() {
        // the mirrored end: the port point slides along the panel border and
        // the stretch re-joins the tail — the approach must meet the frozen
        // interior at a right angle, not as a per-frame diagonal
        List<FloatRect> obstacles = List.of(new FloatRect(-10, 60, 20, 20), new FloatRect(60, 20, 20, 20));
        LeaderRouter router = new LeaderRouter(config);
        router.route(List.of(leader("a", 0, 0, 200, 100)), Map.of(), obstacles);

        for (int frame = 1; frame <= 20; frame++) {
            double labelY = 100 + frame * 0.4;
            List<FloatPos> points = router.route(List.of(leader("a", 0, 0, 200, labelY)), Map.of(), obstacles)
                    .get(0)
                    .points();
            assertOrthogonal(points, "frame " + frame);
        }
    }

    @Test
    void aFlickeringCrossingNeverReroutesTheDetour() {
        // crossing-regression: b's straight line crosses a's committed po
        // route, but only on the epochs b is present — one epoch on, one off.
        // A crossing that flickers at that cadence never survives the dwell,
        // so the zero-crossing pass must not adopt a detour on every return:
        // a's committed shape stays put through the whole flicker (the wall
        // holds a in po, so the style gate never enters the picture)
        List<FloatRect> obstacles = List.of(new FloatRect(190, 30, 20, 40));
        List<LeaderRouter.Leader> pair = List.of(leader("a", 0, 100, 400, 0), leader("b", 300, 200, 300, -400));
        List<LeaderRouter.Leader> alone = List.of(leader("a", 0, 100, 400, 0));
        LeaderRouter router = new LeaderRouter(config);

        router.route(pair, Map.of(), obstacles);
        List<FloatPos> committed =
                router.route(alone, Map.of(), obstacles).get(0).points();
        assertEquals(
                LeaderRouter.Style.poLeader,
                router.route(alone, Map.of(), obstacles).get(0).style());

        boolean flickered = false;
        for (int epoch = 0; epoch < 12; epoch++) {
            List<LeaderRouter.Route> routes = router.route(epoch % 2 == 0 ? pair : alone, Map.of(), obstacles);
            flickered |= routes.get(0).crossings() > 0;
            assertEquals(committed, routes.get(0).points(), "epoch " + epoch + " kept the committed shape");
        }
        assertTrue(flickered, "the sequence genuinely flickered through crossings");
    }

    @Test
    void aStretchedSlideKeepsTheShapeEpochWhileAnAdoptionBumpsIt() {
        // the settle's trigger: within-cell endpoint slides reuse the commit
        // and keep its epoch; a clearly shorter fresh route is adopted and
        // moves it — the caller keys its morph off exactly this distinction
        List<FloatRect> obstacles = List.of(new FloatRect(-10, 60, 20, 20), new FloatRect(60, 20, 20, 20));
        LeaderRouter router = new LeaderRouter(config);
        long committed = router.route(List.of(leader("a", 0, 0, 200, 100)), Map.of(), obstacles)
                .get(0)
                .shapeEpoch();
        assertTrue(committed > 0, "the po commit carries a real epoch");

        for (int frame = 1; frame <= 10; frame++) {
            assertEquals(
                    committed,
                    router.route(List.of(leader("a", frame * 0.4, frame * 0.2, 200, 100)), Map.of(), obstacles)
                            .get(0)
                            .shapeEpoch(),
                    "frame " + frame + ": a stretch is not a topology change");
        }

        long adopted = router.route(List.of(leader("a", 0, 25, 200, 100)), Map.of(), obstacles)
                .get(0)
                .shapeEpoch();
        assertTrue(adopted != committed, "a fresh adoption is a topology change");
    }

    @Test
    void aStyleFlipBumpsTheShapeEpoch() {
        // the s→po upgrade (and the crossing re-route that follows it in the
        // same epoch) changes the drawn topology wholesale — the epoch must
        // move with it, or the settle would never play for the upgrade
        LeaderRouter router = new LeaderRouter(config);
        long straight = router.route(crossingPair(), Map.of()).get(0).shapeEpoch();
        long orthogonal = router.route(crossingPair(), Map.of()).get(0).shapeEpoch();
        assertTrue(straight != orthogonal, "the style flip reads as a topology change");
    }

    /** Every segment of the polyline runs along a screen axis — no diagonal joint anywhere. */
    private static void assertOrthogonal(List<FloatPos> points, String what) {
        assertTrue(points.size() >= 2, what + ": no line drawn");
        for (int i = 0; i + 1 < points.size(); i++) {
            FloatPos a = points.get(i);
            FloatPos b = points.get(i + 1);
            assertTrue(
                    Math.abs(a.x() - b.x()) < 1.0e-9 || Math.abs(a.y() - b.y()) < 1.0e-9,
                    what + ": diagonal segment " + a + " → " + b);
        }
    }

    /** Every vertex of the polyline lies inside the viewport, boundary included. */
    private static void assertInside(List<FloatPos> points, FloatRect viewport, String what) {
        for (FloatPos p : points) {
            assertTrue(
                    p.x() >= viewport.x() - 1.0e-9
                            && p.x() <= viewport.right() + 1.0e-9
                            && p.y() >= viewport.y() - 1.0e-9
                            && p.y() <= viewport.bottom() + 1.0e-9,
                    what + " leaves the viewport at " + p);
        }
    }

    @Test
    void rejectsInvalidUse() {
        LeaderRouter router = new LeaderRouter(config);

        assertThrows(IllegalArgumentException.class, () -> LeaderRouter.Config.of(0, 1));
        assertThrows(IllegalArgumentException.class, () -> LeaderRouter.Config.of(1, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> router.route(List.of(leader("a", 0, 0, 1, 1), leader("a", 1, 1, 2, 2)), Map.of()));
    }
}
