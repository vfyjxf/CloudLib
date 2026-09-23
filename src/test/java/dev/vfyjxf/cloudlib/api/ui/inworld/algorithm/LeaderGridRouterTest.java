package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class LeaderGridRouterTest {

    private static final double eps = 1.0e-9;

    private static final LeaderGridRouter.Config config = LeaderGridRouter.Config.ofDefaults();

    private static void assertPoint(FloatPos actual, double x, double y, String what) {
        assertEquals(x, actual.x(), eps, what + " x");
        assertEquals(y, actual.y(), eps, what + " y");
    }

    @Test
    void aClearAxisAlignedShotIsTheStraightLine() {
        List<FloatPos> path = LeaderGridRouter
                .route(new FloatPos(0, 100), 1, 0, new FloatPos(200, 100), -1, 0, List.of(), config);

        assertNotNull(path);
        assertEquals(2, path.size(), "collinear stub and approach merge away");
        assertPoint(path.get(0), 0, 100, "start");
        assertPoint(path.get(1), 194, 100, "the drawn end stops the arrival gap (6 px) short of the port");
    }

    @Test
    void frozenEndsHoldThroughTheWholeRoute() {
        // first segment must leave along dir (0,+1), last must arrive along
        // −normal = (+1,0) — the perpendicular stub makes the 1-fold L
        List<FloatPos> path = LeaderGridRouter
                .route(new FloatPos(0, 0), 0, 1, new FloatPos(100, 100), -1, 0, List.of(), config);

        assertNotNull(path);
        FloatPos start = path.get(0);
        FloatPos first = path.get(1);
        assertEquals(0.0, first.x() - start.x(), eps, "the first segment runs along the frozen dir");
        assertTrue(Math.abs(first.y() - start.y()) >= config.stubPx() - eps, "the stub length is guaranteed");

        FloatPos approach = path.get(path.size() - 2);
        FloatPos end = path.get(path.size() - 1);
        assertEquals(approach.y(), end.y(), eps, "the final segment runs along −normal");
        assertTrue(Math.abs(end.x() - approach.x()) >= config.lastSegmentPx() - eps, "the final segment length holds");
        assertPoint(end, 94, 100, "the drawn end keeps the arrival gap");
    }

    @Test
    void aWallDetoursViaTheShorterSide() {
        // the wall spans y 20..140, the anchor sits at y 50: the inflated top
        // lane is 42 px away, the bottom lane 102 — the route must go over
        FloatRect wall = new FloatRect(140, 20, 20, 120);
        List<FloatPos> path = LeaderGridRouter
                .route(new FloatPos(0, 50), 1, 0, new FloatPos(300, 50), -1, 0, List.of(wall), config);

        assertNotNull(path);
        double minY = path.stream().mapToDouble(FloatPos::y).min().orElseThrow();
        double maxY = path.stream().mapToDouble(FloatPos::y).max().orElseThrow();
        assertEquals(8.0, minY, eps, "crosses on the inflated top edge lane (20−12)");
        assertTrue(maxY <= 50 + eps, "never dips below the anchor line");
        assertTrue(clearsInflated(path, List.of(wall)), "no segment crosses the inflated wall");
    }

    @Test
    void aSealedAnchorHasNoRoute() {
        // the inflation around this rect swallows the anchor's stub
        FloatRect seal = new FloatRect(8, 0, 20, 100);
        List<FloatPos> path = LeaderGridRouter
                .route(new FloatPos(0, 50), 1, 0, new FloatPos(400, 50), -1, 0, List.of(seal), config);

        assertNull(path, "unsolvable geometry returns null — the caller falls back");
    }

    @Test
    void theBendPenaltyPrefersFewerFoldsAtEqualLength() {
        // from (0,0) to a left-facing port at (100,100): the dominant-axis
        // stub makes a 2-fold route, the perpendicular stub a 1-fold — both
        // exactly manhattan-length; only the bend penalty separates them
        assertEquals(1, bendsUnderPenalty(config), "the 1-fold route wins under the 24 px penalty");

        LeaderGridRouter.Config noPenalty = new LeaderGridRouter.Config(0, 6, 12, 16, 20, 6);
        assertEquals(2, bendsUnderPenalty(noPenalty), "without the penalty the tie keeps the dominant stub's 2 folds");
    }

    /** The po-route fold count the router draws for (0,0) → (100,100), forced po by a baseline-blocking obstacle. */
    private static int bendsUnderPenalty(LeaderGridRouter.Config routing) {
        LeaderRouter router = new LeaderRouter(
            new LeaderRouter.Config(1.0, 1, 42.0, 84.0, 8.0, 12.0, 20.0, 6.0, routing)
        );
        FloatRect blocker = new FloatRect(50, 30, 8, 8);
        List<LeaderRouter.Route> routes = router
                .route(List.of(LeaderRouter.Leader.toPoint("a", 0, 0, 100, 100)), Map.of(), List.of(blocker));
        return routes.get(0).points().size() - 2;
    }

    @Test
    void theCorridorRunCentersInTheAisle() {
        // two stacked obstacles leave a corridor between inflated y=72 and
        // y=88; the shortest grid lane hugs one wall — lane centering must
        // slide it to the aisle's midpoint
        FloatRect upper = new FloatRect(150, 0, 20, 60);
        FloatRect lower = new FloatRect(150, 100, 20, 60);
        List<FloatPos> path = LeaderGridRouter
                .route(new FloatPos(0, 50), 1, 0, new FloatPos(400, 50), -1, 0, List.of(upper, lower), config);

        assertNotNull(path);
        double runY = Double.NaN;
        for (int i = 0; i + 1 < path.size(); i++) {
            FloatPos a = path.get(i);
            FloatPos b = path.get(i + 1);
            if (Math.abs(a.y() - b.y()) < eps && a.x() < 172 && b.x() > 138) {
                runY = a.y();
            }
        }
        assertEquals(80.0, runY, eps, "the run slides to (72+88)/2 — the corridor's midpoint");
        assertTrue(clearsInflated(path, List.of(upper, lower)), "still clear of both obstacles");
    }

    @Test
    void aSingleBoundLaneKeepsHuggingIt() {
        // one wall: the near hug lane (22 px of jog) versus the far hug lane
        // (62 px) — both hug, the 6 px penalty is a tie-breaker not an
        // override, and with no second bound there is nothing to center into
        FloatRect wall = new FloatRect(150, 0, 20, 60);
        List<FloatPos> path = LeaderGridRouter
                .route(new FloatPos(0, 50), 1, 0, new FloatPos(400, 50), -1, 0, List.of(wall), config);

        assertNotNull(path);
        double minY = path.stream().mapToDouble(FloatPos::y).min().orElseThrow();
        double maxY = path.stream().mapToDouble(FloatPos::y).max().orElseThrow();
        assertEquals(50.0, minY, eps, "the anchor line stays the highest point");
        assertEquals(72.0, maxY, eps, "the crossing dips onto the inflated bottom edge (60+12)");
    }

    @Test
    void thePortPanelIsExemptAlongItsOwnApproach() {
        // the port sits on this panel's left border; the approach reaches
        // through the panel's own inflated margin without failing
        FloatRect panel = new FloatRect(200, 100, 160, 100);
        List<FloatPos> path = LeaderGridRouter
                .route(new FloatPos(0, 150), 1, 0, new FloatPos(200, 150), -1, 0, List.of(panel), config);

        assertNotNull(path, "the own-panel margin never blocks the final approach");
        assertPoint(path.get(path.size() - 1), 194, 150, "still stops at the arrival gap");
    }

    @Test
    void aDetourBeyondTheScreenEdgeStaysInsideTheViewport() {
        // the wall fills the lower lane: its inflated bottom edge (332) is
        // off-screen in a 400×300 gui, so the only legal detour crosses its
        // top — the unbounded plane would take the shorter dip below and
        // leave the screen
        FloatRect viewport = new FloatRect(0, 0, 400, 300);
        FloatRect wall = new FloatRect(150, 150, 100, 170);
        List<FloatPos> unbounded = LeaderGridRouter
                .route(new FloatPos(40, 280), 1, 0, new FloatPos(360, 280), -1, 0, List.of(wall), config);
        assertNotNull(unbounded);
        assertTrue(
            unbounded.stream().anyMatch(p -> p.y() > 300),
            "the unbounded route dips onto the y=332 lane below the wall — the off-screen regression"
        );

        List<FloatPos> path = LeaderGridRouter
                .route(new FloatPos(40, 280), 1, 0, new FloatPos(360, 280), -1, 0, List.of(wall), config, viewport);

        assertNotNull(path);
        assertInside(path, viewport);
        assertTrue(clearsInflated(path, List.of(wall)), "still clear of the wall");
        assertEquals(
            280.0,
            path.stream().mapToDouble(FloatPos::y).max().orElseThrow(),
            eps,
            "the route never dips below the anchor line"
        );
    }

    @Test
    void anEdgeAnchorRoutesWithTheStubClampedToTheEdge() {
        // the stub wants (408,100): the viewport clips it to the x=400 edge
        // and the route turns there instead of failing; the port's outward
        // normal pushes both tail ends past the same edge — they clamp too
        FloatRect viewport = new FloatRect(0, 0, 400, 300);
        List<FloatPos> path = LeaderGridRouter
                .route(new FloatPos(392, 100), 1, 0, new FloatPos(398, 150), 1, 0, List.of(), config, viewport);

        assertNotNull(path, "the clipped stub still routes");
        assertEquals(3, path.size());
        assertPoint(path.get(0), 392, 100, "start");
        assertPoint(path.get(1), 400, 100, "the stub end clamps onto the right edge");
        assertPoint(path.get(2), 400, 150, "the drawn end clamps onto the edge too");
        assertInside(path, viewport);
    }

    @Test
    void anAnchorExactlyOnTheEdgeStillRoutes() {
        FloatRect viewport = new FloatRect(0, 0, 400, 300);
        List<FloatPos> path = LeaderGridRouter
                .route(new FloatPos(400, 100), -1, 0, new FloatPos(200, 150), -1, 0, List.of(), config, viewport);

        assertNotNull(path, "a boundary anchor is inside the closed viewport");
        assertInside(path, viewport);
    }

    @Test
    void aSegmentLeavingTheViewportCountsAsBlocked() {
        FloatRect viewport = new FloatRect(0, 0, 400, 300);
        FloatPos port = new FloatPos(360, 150);
        assertFalse(
            LeaderGridRouter.polylineBlocked(
                List.of(new FloatPos(10, 150), port),
                port,
                List.of(),
                config.clearancePx(),
                viewport
            )
        );
        assertTrue(
            LeaderGridRouter.polylineBlocked(
                List.of(new FloatPos(10, 150), new FloatPos(410, 150)),
                port,
                List.of(),
                config.clearancePx(),
                viewport
            ),
            "a polyline exiting the right edge is blocked"
        );
        assertTrue(
            LeaderGridRouter.polylineBlocked(
                List.of(new FloatPos(10, -5), new FloatPos(200, 150)),
                port,
                List.of(),
                config.clearancePx(),
                viewport
            ),
            "a polyline entering from above the top edge is blocked"
        );
    }

    @Test
    void theSameInputRoutesToTheSameOutput() {
        FloatRect wall = new FloatRect(140, 20, 20, 120);
        List<FloatPos> one = LeaderGridRouter
                .route(new FloatPos(0, 50), 1, 0, new FloatPos(300, 50), -1, 0, List.of(wall), config);
        List<FloatPos> two = LeaderGridRouter
                .route(new FloatPos(0, 50), 1, 0, new FloatPos(300, 50), -1, 0, List.of(wall), config);
        assertEquals(one, two);
    }

    @Test
    void rejectsInvalidUse() {
        assertThrows(
            IllegalArgumentException.class,
            () -> LeaderGridRouter.route(new FloatPos(0, 0), 1, 1, new FloatPos(10, 10), -1, 0, List.of(), config)
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> LeaderGridRouter.route(new FloatPos(0, 0), 1, 0, new FloatPos(10, 10), 0, 0, List.of(), config)
        );
        assertThrows(IllegalArgumentException.class, () -> new LeaderGridRouter.Config(-1, 6, 12, 16, 20, 6));
        assertThrows(IllegalArgumentException.class, () -> new LeaderGridRouter.Config(24, 6, 12, 0, 20, 6));
        assertThrows(IllegalArgumentException.class, () -> new LeaderGridRouter.Config(24, 6, 12, 16, 20, -1));
    }

    /** No segment of the path crosses any obstacle's clearance-inflated interior. */
    private static boolean clearsInflated(List<FloatPos> path, List<FloatRect> obstacles) {
        return !LeaderGridRouter.polylineBlocked(path, new FloatPos(-1.0e9, -1.0e9), obstacles, config.clearancePx());
    }

    /** Every vertex of the path lies inside the viewport, boundary included. */
    private static void assertInside(List<FloatPos> path, FloatRect viewport) {
        for (FloatPos p : path) {
            assertTrue(
                p.x() >= viewport.x() - eps
                        && p.x() <= viewport.right() + eps
                        && p.y() >= viewport.y() - eps
                        && p.y() <= viewport.bottom() + eps,
                "point leaves the viewport: " + p
            );
        }
    }
}
