package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import org.junit.jupiter.api.Test;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;

class LeaderRouterTest {

    private static final LeaderRouter.Config config =
            LeaderRouter.Config.of(LeaderRouter.Elbow.horizontalFirst, 1.0, 2);

    private static LeaderRouter.Leader leader(String id, double ax, double ay, double lx, double ly) {
        return new LeaderRouter.Leader(id, ax, ay, lx, ly);
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
    void poLeaderUsesTheSharedElbowOrder() {
        LeaderRouter router = new LeaderRouter(config);
        router.route(crossingPair(), Map.of());
        LeaderRouter.Route route = router.route(crossingPair(), Map.of()).get(0);

        assertEquals(LeaderRouter.Style.poLeader, route.style());
        assertEquals(3, route.points().size());
        assertEquals(0, route.points().get(0).x(), 1.0e-9);
        assertEquals(0, route.points().get(0).y(), 1.0e-9);
        // horizontalFirst: the segment leaving the anchor is horizontal
        assertEquals(100, route.points().get(1).x(), 1.0e-9);
        assertEquals(0, route.points().get(1).y(), 1.0e-9);
        assertEquals(100, route.points().get(2).x(), 1.0e-9);
        assertEquals(100, route.points().get(2).y(), 1.0e-9);

        LeaderRouter vertical = new LeaderRouter(LeaderRouter.Config.of(LeaderRouter.Elbow.verticalFirst, 1.0, 1));
        LeaderRouter.Route verticalRoute =
                vertical.route(crossingPair(), Map.of()).get(0);
        assertEquals(LeaderRouter.Style.poLeader, verticalRoute.style());
        assertEquals(0, verticalRoute.points().get(1).x(), 1.0e-9);
        assertEquals(100, verticalRoute.points().get(1).y(), 1.0e-9);
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
        LeaderRouter router = new LeaderRouter(LeaderRouter.Config.of(LeaderRouter.Elbow.horizontalFirst, 1.0, 1));
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
    void rejectsInvalidUse() {
        LeaderRouter router = new LeaderRouter(config);

        assertThrows(
                IllegalArgumentException.class, () -> LeaderRouter.Config.of(LeaderRouter.Elbow.horizontalFirst, 0, 1));
        assertThrows(
                IllegalArgumentException.class, () -> LeaderRouter.Config.of(LeaderRouter.Elbow.horizontalFirst, 1, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> router.route(List.of(leader("a", 0, 0, 1, 1), leader("a", 1, 1, 2, 2)), Map.of()));
    }
}
