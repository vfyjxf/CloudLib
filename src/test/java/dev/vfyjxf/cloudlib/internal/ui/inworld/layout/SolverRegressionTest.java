package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Config;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.FixedOrientation;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiRect;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.HudRegion;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutFrame;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutResult;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutState;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LeaderMode;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Material;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Metrics;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelBasis;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelMemory;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelRequest;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Pose;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PositionPolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PositionSlot;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SampleContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SearchBudget;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SourceSnapshot;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Sources;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Space;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Tier;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static dev.vfyjxf.cloudlib.internal.ui.inworld.layout.LayoutTestKit.dim;
import static dev.vfyjxf.cloudlib.internal.ui.inworld.layout.LayoutTestKit.frame;
import static dev.vfyjxf.cloudlib.internal.ui.inworld.layout.LayoutTestKit.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Solver regression suite — cavities, lazy overflow, recovery windows,
 * continuous motion safety, exhaustive packing oracle, required joint
 * routing, beam cooperation, recovery lifecycle and bounded search.
 * Ported from the source project's main()-based assertions to JUnit 5.
 */
class SolverRegressionTest {

    private static LayoutFrame fragmented() {
        List<PanelRequest> requests = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            requests.add(request("cavity-" + i, null, false, LeaderMode.none));
        }
        return frame(
                0,
                720,
                300,
                requests,
                List.of(
                        HudRegion.rectangle("left", new GuiRect(0, 0, 65, 300)),
                        HudRegion.rectangle("split-a", new GuiRect(205, 0, 55, 300)),
                        HudRegion.rectangle("split-b", new GuiRect(400, 0, 55, 300)),
                        HudRegion.rectangle("right", new GuiRect(595, 0, 125, 300)),
                        HudRegion.rectangle("bottom", new GuiRect(0, 200, 720, 100))));
    }

    @Test
    void cavities() {
        LayoutFrame frame = fragmented();
        LayoutResult result = new LayoutEngine().solve(frame, new LayoutState());
        assertEquals(6, result.panels().size(), "six full panels in three HUD cavities");
        assertTrue(result.panels().stream().allMatch(c -> c.tier() == Tier.full), "cavities preserve full content");
        assertNull(result.dock().rect(), "unused dock has no footprint");
        assertEquals(0, result.dock().total());
        assertTrue(LayoutEngine.validate(frame, result).isEmpty(), "cavity audit");
    }

    @Test
    void docks() {
        LayoutEngine engine = new LayoutEngine();
        LayoutResult empty = engine.solve(frame(0, 500, 300, List.of(), List.of()), new LayoutState());
        assertNull(empty.dock().rect(), "empty input is not external overflow");
        assertFalse(empty.dock().externalOnly());

        PanelRequest whole = request("whole", new GuiRect(10, 10, 480, 280), false, LeaderMode.none);
        LayoutResult fit = engine.solve(frame(0, 500, 300, List.of(whole), List.of()), new LayoutState());
        assertEquals(1, fit.panels().size(), "no speculative dock may displace a fixed panel");
        assertNull(fit.dock().rect());

        PanelRequest second = request("second", new GuiRect(10, 10, 480, 280), false, LeaderMode.none);
        LayoutResult crowded = engine.solve(frame(0, 500, 300, List.of(whole, second), List.of()), new LayoutState());
        assertTrue(crowded.dock().total() > 0, "real overflow reserves a reachable entry");
        assertNotNull(crowded.dock().rect());

        LayoutFrame covered =
                frame(0, 500, 300, List.of(second), List.of(HudRegion.rectangle("all", new GuiRect(0, 0, 500, 300))));
        LayoutResult impossible = engine.solve(covered, new LayoutState());
        assertTrue(
                impossible.dock().externalOnly() && impossible.dock().total() == 1,
                "fully covered HUD reports external registry");

        LayoutState state = new LayoutState();
        engine.solve(covered, state);
        LayoutResult cleared = engine.solve(frame(1, 500, 300, List.of(), List.of()), state);
        assertNull(cleared.dock().rect(), "removal releases dock and pending recovery");
        assertTrue(state.recovery.isEmpty());

        LayoutFrame narrow = frame(
                0,
                200,
                160,
                List.of(second),
                List.of(
                        HudRegion.rectangle("top", new GuiRect(0, 0, 200, 57)),
                        HudRegion.rectangle("bottom", new GuiRect(0, 107, 200, 53)),
                        HudRegion.rectangle("left", new GuiRect(0, 57, 71, 50)),
                        HudRegion.rectangle("right", new GuiRect(129, 57, 71, 50))));
        LayoutResult tiny = engine.solve(narrow, new LayoutState());
        assertNotNull(tiny.dock().rect(), "off-grid cavity accepts exact-fit overflow entry");
        assertEquals(new GuiRect(81, 67, 38, 30), tiny.dock().rect());
    }

    @Test
    void recovery() {
        LayoutState state = new LayoutState();
        LayoutEngine engine = new LayoutEngine();
        int switches = 0;
        boolean last = false;
        for (int i = 0; i < 180; i++) {
            // Short legal windows never accumulate across intervening failures.
            LayoutFrame frame = frame(
                    i,
                    160,
                    140,
                    List.of(request("recover", new GuiRect(10, 10, 120, 80), false, LeaderMode.none)),
                    i < 100 && i % 10 < 2
                            ? List.of(HudRegion.rectangle("blocked", new GuiRect(0, 0, 160, 140)))
                            : List.of());
            LayoutResult result = engine.solve(frame, state);
            boolean visible = !result.panels().isEmpty();
            if (i < 107) {
                assertFalse(visible, "must wait for a continuous recovery interval at frame " + i);
            }
            if (visible != last) switches++;
            last = visible;
        }
        assertEquals(1, switches, "180-frame interrupted recovery expands only once");
        assertTrue(last);
        assertTrue(state.recovery.isEmpty(), "visible stable panels release observation state");
    }

    private static GuiRect lerp(GuiRect from, GuiRect to, double t) {
        return new GuiRect(
                from.x() + (to.x() - from.x()) * t,
                from.y() + (to.y() - from.y()) * t,
                from.width() + (to.width() - from.width()) * t,
                from.height() + (to.height() - from.height()) * t);
    }

    private static GuiRect randomRect(Random random) {
        return new GuiRect(
                random.nextDouble() * 500,
                random.nextDouble() * 400,
                10 + random.nextDouble() * 90,
                10 + random.nextDouble() * 90);
    }

    @Test
    void motion() {
        Random random = new Random(720211);
        int sampledCollisions = 0;
        for (int i = 0; i < 1200; i++) {
            GuiRect a0 = randomRect(random), a1 = randomRect(random);
            GuiRect b0 = randomRect(random), b1 = randomRect(random);
            double gap = random.nextDouble() * 12;
            boolean continuous = MotionSafety.collide(a0, a1, b0, b1, gap);
            boolean sampled = false;
            for (int step = 0; step <= 200; step++) {
                double t = step / 200.;
                if (LayoutMath.overlap(
                        lerp(a0, a1, t).polygon(), lerp(b0, b1, t).polygon(), gap)) {
                    sampled = true;
                    break;
                }
            }
            if (sampled) sampledCollisions++;
            assertFalse(sampled && !continuous, "continuous SAT must not miss sampled collision " + i);
            List<GuiVec> triangle = List.of(
                    new GuiVec(b0.x(), b0.y()),
                    new GuiVec(b0.x() + b0.width(), b0.y()),
                    new GuiVec(b0.x(), b0.y() + b0.height()));
            boolean againstHud = MotionSafety.collide(a0, a1, triangle, gap);
            for (int step = 0; step <= 40; step++) {
                assertTrue(
                        againstHud
                                || !LayoutMath.overlap(lerp(a0, a1, step / 40.).polygon(), triangle, gap),
                        "convex HUD motion");
            }
        }
        assertTrue(sampledCollisions > 100, "random motion actually exercises collisions");
        assertFalse(
                MotionSafety.collide(
                        new GuiRect(0, 0, 20, 20),
                        new GuiRect(100, 0, 20, 20),
                        new GuiRect(50, 50, 20, 20),
                        new GuiRect(150, 50, 20, 20),
                        10),
                "parallel separated motion is safe");
        assertTrue(
                MotionSafety.collide(
                        new GuiRect(0, 0, 1, 1),
                        new GuiRect(10000, 0, 1, 1),
                        new GuiRect(4321, 0, 1, 1),
                        new GuiRect(4321, 0, 1, 1),
                        0),
                "thin obstacle crossed between samples");
    }

    @Test
    void budget() {
        LayoutFrame frame = fragmented();
        LayoutEngine engine = new LayoutEngine(new SearchBudget(1, 1, 0, 1));
        LayoutResult result = engine.solve(frame, new LayoutState());
        assertTrue(LayoutEngine.validate(frame, result).isEmpty(), "exhaustion preserves constraints and accounting");
        assertTrue(
                engine.lastStats().budgetReached() && engine.lastStats().expansions() <= 1,
                "deterministic expansion bound");
    }

    private static LayoutFrame finitePacking(int seed) {
        Random random = new Random(seed);
        LayoutFrame base = frame(0, 600, 400, List.of(), List.of());
        List<PanelRequest> requests = new ArrayList<>();
        double depth = 6;
        double worldWidth = base.camera()
                .unproject(new GuiVec(120, 100), depth)
                .distanceTo(base.camera().unproject(new GuiVec(20, 100), depth));
        double worldHeight = base.camera()
                .unproject(new GuiVec(100, 80), depth)
                .distanceTo(base.camera().unproject(new GuiVec(100, 20), depth));
        Metrics metrics = new Metrics(worldWidth, worldHeight, 100, 60, worldHeight / 2, 80, 30, 40, 25);
        int count = 3 + seed % 3;
        for (int i = 0; i < count; i++) {
            List<PositionSlot> slots = new ArrayList<>();
            for (int j = 0; j < 2 + seed % 2; j++) {
                GuiVec point = new GuiVec(70 + random.nextInt(4) * 120, 65 + random.nextInt(3) * 90);
                slots.add(new PositionSlot(
                        "finite:" + i + ":" + j, base.camera().unproject(point, depth), random.nextInt(80)));
            }
            PositionPolicy policy = (context, width, height) -> slots;
            requests.add(new PanelRequest(
                    "finite-" + i,
                    "s",
                    "finite",
                    "test",
                    null,
                    "items",
                    30 + random.nextInt(60),
                    Space.world,
                    policy,
                    new FixedOrientation(base.viewBasis()),
                    Material.virtual(),
                    metrics,
                    false,
                    false,
                    false,
                    LeaderMode.none,
                    null));
        }
        return frame(0, 600, 400, requests, List.of());
    }

    private static double brute(
            LayoutFrame frame,
            List<List<PanelPlacement>> domains,
            int index,
            List<PanelPlacement> chosen,
            double cost) {
        if (index == domains.size()) return cost;
        double best = brute(
                frame,
                domains,
                index + 1,
                chosen,
                cost + LayoutEngine.weight(frame, frame.requests().get(index)) * 1700);
        for (PanelPlacement candidate : domains.get(index)) {
            boolean collides = false;
            double area = Math.abs(ScreenMath.area(candidate.polygon()));
            for (PanelPlacement other : chosen) {
                area += Math.abs(ScreenMath.area(other.polygon()));
                if (LayoutMath.overlap(
                        candidate.polygon(), other.polygon(), frame.config().gap())) {
                    collides = true;
                }
            }
            if (collides
                    || chosen.size() >= frame.config().maxPanels()
                    || area
                            > frame.camera().width()
                                    * frame.camera().height()
                                    * frame.config().maxCoverage()) {
                continue;
            }
            chosen.add(candidate);
            best = Math.min(best, brute(frame, domains, index + 1, chosen, cost + candidate.score()));
            chosen.remove(chosen.size() - 1);
        }
        return best;
    }

    @Test
    void exhaustive() {
        for (int seed = 0; seed < 48; seed++) {
            LayoutFrame frame = finitePacking(seed);
            SourceSnapshot source = frame.sources().get("s").sample(frame.clock());
            List<List<PanelPlacement>> domains = new ArrayList<>();
            for (PanelRequest request : frame.requests()) {
                var unit = new LayoutEngine.Unit(request.id(), List.of(request), request, source, false);
                domains.add(LayoutEngine.candidates(frame, unit, null, List.of()));
                assertFalse(
                        domains.get(domains.size() - 1).isEmpty(), "finite-domain oracle has real legal candidates");
            }
            double expected = brute(frame, domains, 0, new ArrayList<>(), 0);
            LayoutResult result = new LayoutEngine().solve(frame, new LayoutState());
            double actual =
                    result.panels().stream().mapToDouble(PanelPlacement::score).sum();
            for (PanelRequest request : frame.requests()) {
                if (result.panels().stream().noneMatch(c -> c.visualId().equals(request.id()))) {
                    actual += LayoutEngine.weight(frame, request) * 1700;
                }
            }
            assertEquals(
                    expected,
                    actual,
                    1e-7,
                    "finite-domain exhaustive optimum " + seed + ": " + expected + " vs " + actual);
            assertTrue(LayoutEngine.validate(frame, result).isEmpty(), "exhaustive result constraints");
        }
    }

    private static LayoutFrame jointLeaders() {
        LayoutFrame base = frame(0, 640, 400, List.of(), List.of());
        SourceSnapshot sourceA = Sources.point(
                "a", dim, new Pose(base.camera().unproject(new GuiVec(120, 200), 6), PanelBasis.identity()));
        SourceSnapshot sourceB = Sources.point(
                "b", dim, new Pose(base.camera().unproject(new GuiVec(320, 330), 6), PanelBasis.identity()));
        PanelRequest a = request("a", new GuiRect(420, 150, 120, 80), false, LeaderMode.required);
        a = new PanelRequest(
                a.id(),
                "a",
                a.title(),
                a.family(),
                null,
                a.overflowGroup(),
                80,
                a.space(),
                a.position(),
                a.orientation(),
                a.material(),
                a.metrics(),
                false,
                false,
                false,
                a.leaderMode(),
                a.fixedScreen());
        PanelRequest b = request("b", new GuiRect(220, 135, 120, 80), false, LeaderMode.required);
        b = new PanelRequest(
                b.id(),
                "b",
                b.title(),
                b.family(),
                null,
                b.overflowGroup(),
                70,
                b.space(),
                b.position(),
                b.orientation(),
                b.material(),
                b.metrics(),
                false,
                false,
                false,
                b.leaderMode(),
                b.fixedScreen());
        return new LayoutFrame(
                base.clock(),
                base.camera(),
                base.viewBasis(),
                List.of(),
                base.world(),
                Map.of("a", clock -> sourceA, "b", clock -> sourceB),
                List.of(a, b),
                base.interaction(),
                base.config());
    }

    @Test
    void requiredRoutes() {
        LayoutFrame frame = jointLeaders();
        LayoutEngine engine = new LayoutEngine();
        LayoutResult result = engine.solve(frame, new LayoutState());
        assertEquals(2, result.panels().size(), "required panels must be jointly routable");
        assertEquals(
                2,
                result.leaders().stream().filter(l -> !l.screen().isEmpty()).count(),
                "every required panel has a route");
        assertTrue(LayoutEngine.validate(frame, result).isEmpty(), "joint routing audit");
        for (int i = 0; i < 10; i++) {
            LayoutResult repeated = engine.solve(frame, new LayoutState());
            assertEquals(result.panels(), repeated.panels(), "search trials do not mutate committed history");
            assertEquals(result.leaders(), repeated.leaders());
        }
        LayoutResult broken = new LayoutResult(
                result.frameIndex(),
                result.panels(),
                List.of(),
                result.dock(),
                result.drawer(),
                result.addresses(),
                result.transitions(),
                result.diagnostics(),
                result.sources());
        assertFalse(
                LayoutEngine.validate(frame, broken).isEmpty(), "independent audit catches a missing required route");
    }

    private static LayoutFrame chain() {
        LayoutFrame base = frame(0, 1200, 400, List.of(), List.of());
        List<PanelRequest> requests = new ArrayList<>();
        double w = base.camera()
                .unproject(new GuiVec(100, 100), 6)
                .distanceTo(base.camera().unproject(new GuiVec(200, 100), 6));
        double h = base.camera()
                .unproject(new GuiVec(100, 100), 6)
                .distanceTo(base.camera().unproject(new GuiVec(100, 160), 6));
        Metrics metrics = new Metrics(w, h, 100, 60, h / 2, 80, 30, 40, 25);
        for (int i = 0; i < 8; i++) {
            int first = i == 7 ? 0 : i, second = i == 7 ? 0 : i + 1;
            List<PositionSlot> slots = List.of(
                    new PositionSlot(
                            "chain:" + first, base.camera().unproject(new GuiVec(70 + first * 140, 160), 6), 0),
                    new PositionSlot(
                            "chain:" + second, base.camera().unproject(new GuiVec(70 + second * 140, 160), 6), 20));
            requests.add(new PanelRequest(
                    "chain-" + i,
                    "s",
                    "chain",
                    "test",
                    null,
                    "items",
                    70,
                    Space.world,
                    (context, width, height) -> slots,
                    new FixedOrientation(base.viewBasis()),
                    Material.virtual(),
                    metrics,
                    false,
                    false,
                    false,
                    LeaderMode.none,
                    null));
        }
        Config c = base.config();
        return new LayoutFrame(
                base.clock(),
                base.camera(),
                base.viewBasis(),
                base.hud(),
                base.world(),
                base.sources(),
                requests,
                base.interaction(),
                new Config(
                        c.margin(),
                        c.gap(),
                        c.minPanelVisibility(),
                        1,
                        12,
                        c.switchPenalty(),
                        c.recoverySeconds(),
                        5,
                        c.worldClearance(),
                        12,
                        c.maxCoverage()));
    }

    @Test
    void cooperation() {
        LayoutFrame frame = chain();
        LayoutResult result = new LayoutEngine().solve(frame, new LayoutState());
        assertEquals(8, result.panels().size(), "eight-panel reassignment chain with beam width one");
        assertEquals(0, result.dock().total(), "reassignment respects physical candidates");
        assertTrue(LayoutEngine.validate(frame, result).isEmpty());
    }

    @Test
    void recoveryLifecycle() {
        LayoutFrame base = frame(0, 300, 200, List.of(request("life", null, false, LeaderMode.none)), List.of());
        SourceSnapshot source = base.sources().get("s").sample(base.clock());
        var unit =
                new LayoutEngine.Unit("life", base.requests(), base.requests().get(0), source, false);
        PanelPlacement full =
                LayoutEngine.candidates(base, unit, null, List.of()).get(0);
        LayoutState state = new LayoutState();
        state.panels.put(
                "life", new PanelMemory("folded", Tier.folded, null, null, Space.screen, 0, 0, source.generation()));
        var feasible = new LayoutSearch.Plan(List.of(full), List.of(), 0);
        assertTrue(
                RecoveryGate.observe(base, state, List.of(unit), feasible).contains("life"), "initial recovery held");
        RecoveryGate.observe(base, state, List.of(unit), feasible);
        assertEquals(0, state.recovery.get("life").since(), "repeated extraction does not advance recovery");
        LayoutFrame skipped = new LayoutFrame(
                new SampleContext(1, 1, 1, .5, dim),
                base.camera(),
                base.viewBasis(),
                base.hud(),
                base.world(),
                base.sources(),
                base.requests(),
                base.interaction(),
                base.config());
        assertTrue(
                RecoveryGate.observe(skipped, state, List.of(unit), feasible).contains("life")
                        && state.recovery.get("life").since() == 1,
                "unobserved long interval resets recovery");
        LayoutFrame lost = new LayoutFrame(
                new SampleContext(2, 1 + 1. / 60, 1. / 60, .5, dim),
                base.camera(),
                base.viewBasis(),
                base.hud(),
                base.world(),
                base.sources(),
                base.requests(),
                base.interaction(),
                base.config());
        RecoveryGate.observe(lost, state, List.of(unit), new LayoutSearch.Plan(List.of(), List.of(), 0));
        assertTrue(state.recovery.isEmpty(), "loss of joint feasibility resets the timer");
        state.reset();
        assertTrue(state.recovery.isEmpty() && state.panels.isEmpty(), "reset clears lifecycle state");
    }
}
