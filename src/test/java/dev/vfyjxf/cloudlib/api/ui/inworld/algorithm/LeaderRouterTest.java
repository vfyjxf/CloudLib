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
    void nearTargetWithinToleranceDrawsNoLine() {
        LeaderRouter router = new LeaderRouter(config);

        LeaderRouter.Route route =
                router.route(List.of(leader("a", 0, 0, 50, 30)), Map.of()).get(0);

        assertTrue(route.points().isEmpty(), "within the 80 px leader tolerance there is no line");
    }

    @Test
    void anchorMicroJitterReusesTheCommittedInterior() {
        // the lane blocker sits on the 1-fold corridor (making the 2-fold jog
        // cheaper) and the far blocker cuts the straight baseline — the po
        // route is the two-bend jog whose interior pins to (92, 0) and (92, 100)
        List<FloatRect> obstacles = List.of(new FloatRect(-10, 60, 20, 20), new FloatRect(60, 20, 20, 20));
        LeaderRouter router = new LeaderRouter(config);
        LeaderRouter.Route first = router.route(List.of(leader("a", 0, 0, 200, 100)), Map.of(), obstacles)
                .get(0);
        assertTrue(first.points().contains(new FloatPos(92, 0)));
        assertTrue(first.points().contains(new FloatPos(92, 100)));

        // a 5 px anchor move stays inside the 12 px quantization cell
        LeaderRouter.Route second = router.route(List.of(leader("a", 3, 4, 200, 100)), Map.of(), obstacles)
                .get(0);

        assertEquals(new FloatPos(3, 4), second.points().get(0), "the exact anchor leads the line");
        assertTrue(second.points().contains(new FloatPos(92, 0)), "the committed interior vertex stands still");
        assertTrue(second.points().contains(new FloatPos(92, 100)), "the committed interior vertex stands still");
    }

    @Test
    void aMarginalShorterRouteIsNotAdopted() {
        List<FloatRect> obstacles = List.of(new FloatRect(-10, 60, 20, 20), new FloatRect(60, 20, 20, 20));
        LeaderRouter router = new LeaderRouter(config);
        router.route(List.of(leader("a", 0, 0, 200, 100)), Map.of(), obstacles);

        // 10 px down crosses the quantization cell — the fresh route saves
        // ~16 px of jog, under the 20 px switch cost: the committed interior holds
        LeaderRouter.Route held = router.route(List.of(leader("a", 0, 10, 200, 100)), Map.of(), obstacles)
                .get(0);
        assertTrue(held.points().contains(new FloatPos(92, 0)), "a 16 px saving does not buy a re-route");
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
