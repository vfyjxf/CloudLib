package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Config;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Depth;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.FaceCamera;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.FollowPosition;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiRect;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Interaction;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutFrame;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutResult;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutState;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LeaderLine;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LeaderMode;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Material;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Metrics;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.OverflowDock;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelBasis;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelMemory;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelRequest;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Pose;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SampleContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SearchBudget;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SourceSnapshot;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Sources;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Space;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Tier;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.ViewCamera;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.WorldObstacles;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Temporal debounce regressions — appearance gates, route-switch dwell,
 * movement dwell, skipped-frame behaviour and the integrated solve.
 * Ported from the source DebounceTests main() assertions.
 */
class DebounceTest {

    private static LayoutFrame frame(int index) {
        ViewCamera camera = ViewCamera.minecraft(new Vec3(0, 0, 0), 0, 0, 640, 400, 62);
        SourceSnapshot source = Sources.point("s", "test", new Pose(new Vec3(2, 0, 6), PanelBasis.identity()));
        PanelRequest request = new PanelRequest(
                "p",
                "s",
                "p",
                "test",
                null,
                "items",
                70,
                Space.screen,
                new FollowPosition(new Vec3(0, 0, 0)),
                new FaceCamera(false),
                Material.virtual(),
                Metrics.standard(),
                false,
                false,
                false,
                LeaderMode.auto,
                null);
        return new LayoutFrame(
                new SampleContext(index, index / 60., 1. / 60, .5, "test"),
                camera,
                LayoutMath.viewBasis(camera),
                List.of(),
                new WorldObstacles(List.of()),
                Map.of("s", clock -> source),
                List.of(request),
                Interaction.none(),
                Config.defaults());
    }

    private static PanelPlacement candidate(LayoutFrame f, GuiRect rect, String slot) {
        PanelRequest r = f.requests().get(0);
        SourceSnapshot s = f.sources().get("s").sample(f.clock());
        return new PanelPlacement(
                r.id(),
                List.of(r),
                r,
                s,
                slot,
                Tier.full,
                Space.screen,
                s.frame(),
                1,
                1,
                rect,
                rect.polygon(),
                0,
                false);
    }

    private static LeaderSolver.Option option(PanelPlacement c, String key, double cost) {
        LeaderLine line = new LeaderLine(
                c.visualId(),
                c.source().id(),
                List.of(c.visualId()),
                List.of(new GuiVec(20, 20), new GuiVec(40, 20)),
                List.of(),
                "VISIBLE",
                Depth.test);
        return new LeaderSolver.Option(line, key, cost);
    }

    @Test
    void lines() {
        TemporalDebounce d = new TemporalDebounce();
        PanelPlacement c = candidate(frame(0), new GuiRect(14, 14, 270, 180), "old");
        var a = option(c, "a", 1);
        var b = option(c, "b", 0);
        var hidden = LeaderSolver.hidden(c, List.of("p"), "OCCLUDED");
        for (int i = 0; i <= 15; i++) {
            LayoutFrame f = frame(i);
            d.begin(f);
            LeaderLine l = d.finishAuto(f, c, List.of(a), hidden);
            assertEquals(i < 15, l.screen().isEmpty(), "appearance interval " + i);
        }
        LayoutFrame same = frame(15);
        d.begin(same);
        assertFalse(
                d.finishAuto(same, c, List.of(a), hidden).screen().isEmpty(),
                "duplicate frame does not hide visible line");
        for (int i = 16; i < 22; i++) {
            LayoutFrame f = frame(i);
            d.begin(f);
            d.finishAuto(f, c, i % 2 == 0 ? List.of(b, a) : List.of(a, b), hidden);
            assertEquals("a", d.read(c).route, "alternating route does not switch");
        }
        for (int i = 22; i <= 37; i++) {
            LayoutFrame f = frame(i);
            d.begin(f);
            d.finishAuto(f, c, List.of(b, a), hidden);
            assertEquals(i < 37 ? "a" : "b", d.read(c).route, "continuous route challenge " + i);
        }
        d.begin(frame(38));
        assertTrue(d.finishAuto(frame(38), c, List.of(), hidden).screen().isEmpty(), "invalid route hides immediately");
        d.begin(frame(39));
        assertTrue(d.finishAuto(frame(39), c, List.of(a), hidden).screen().isEmpty(), "return requires fresh dwell");
        d.begin(frame(50));
        assertTrue(
                d.finishAuto(frame(50), c, List.of(a), hidden).screen().isEmpty(),
                "skipped frames do not accumulate time");
        d.reset();
        assertTrue(d.lines.isEmpty() && !d.pending(), "reset clears timers and ownership");
        TemporalDebounce.Window window = new TemporalDebounce.Window();
        assertFalse(window.observe(frame(0), "x") || window.observe(frame(0), "x"), "same frame cannot advance clock");
    }

    @Test
    void movements() {
        LayoutState state = new LayoutState();
        LayoutFrame f = frame(0);
        PanelPlacement old = candidate(f, new GuiRect(14, 14, 270, 180), "rail:0:0");
        PanelPlacement target = candidate(f, new GuiRect(356, 14, 270, 180), "rail:1:0");
        // NONE avoids involving routing in this isolated position-gate test.
        PanelRequest r = old.active();
        r = new PanelRequest(
                r.id(),
                r.sourceId(),
                r.title(),
                r.family(),
                null,
                r.overflowGroup(),
                70,
                r.space(),
                r.position(),
                r.orientation(),
                r.material(),
                r.metrics(),
                false,
                false,
                false,
                LeaderMode.none,
                null);
        old = new PanelPlacement(
                old.visualId(),
                List.of(r),
                r,
                old.source(),
                old.slot(),
                old.tier(),
                old.space(),
                old.pose(),
                1,
                1,
                old.screenRect(),
                old.polygon(),
                0,
                false);
        target = new PanelPlacement(
                target.visualId(),
                List.of(r),
                r,
                target.source(),
                target.slot(),
                target.tier(),
                target.space(),
                target.pose(),
                1,
                1,
                target.screenRect(),
                target.polygon(),
                0,
                false);
        state.panels.put(
                "p", new PanelMemory(old.slot(), Tier.full, old.pose(), old.screenRect(), Space.screen, 0, 0, 0));
        state.cachedResult = new LayoutResult(
                0,
                List.of(old),
                List.of(),
                new OverflowDock(null, false, List.of(), 0, 0, 1, List.of()),
                null,
                Map.of(),
                List.of(),
                List.of(),
                Map.of());
        var unit = new LayoutEngine.Unit("p", List.of(r), r, old.source(), false);
        var allocation = new LayoutSearch.Allocation(
                new LayoutSearch.Plan(List.of(target), List.of(), 0), null, null, List.of());
        LeaderSolver solver = new LeaderSolver();
        for (int i = 0; i <= 15; i++) {
            LayoutFrame next = frame(i);
            state.debounce.begin(next);
            solver.bind(next, state.debounce);
            var result = state.debounce.panels(
                    next, state, List.of(unit), allocation, solver, new LayoutSearch.Work(SearchBudget.defaults()));
            GuiRect expected = i < 15 ? old.screenRect() : target.screenRect();
            assertEquals(expected, result.plan().panels().get(0).screenRect(), "position dwell " + i);
        }
        state.debounce.reset();
        var blocked = new LayoutSearch.Allocation(allocation.plan(), null, null, List.of(old.polygon()));
        assertEquals(
                blocked,
                state.debounce.panels(
                        frame(0),
                        state,
                        List.of(unit),
                        blocked,
                        solver,
                        new LayoutSearch.Work(SearchBudget.defaults())),
                "invalid old placement never held");
    }

    @Test
    void integration() {
        LayoutEngine engine = new LayoutEngine();
        LayoutState state = new LayoutState();
        for (int i = 0; i <= 17; i++) {
            LayoutFrame f = frame(i);
            LayoutResult result = engine.solve(f, state);
            assertTrue(LayoutEngine.validate(f, result).isEmpty(), "integrated constraints");
            if (i < 15) {
                assertTrue(
                        result.leaders().stream().allMatch(l -> l.screen().isEmpty()),
                        "cache cannot bypass appearance gate");
            }
        }
        // A joint required route is never hidden by an AUTO appearance timer.
        LayoutFrame f = frame(0);
        PanelRequest r = f.requests().get(0);
        r = new PanelRequest(
                r.id(),
                r.sourceId(),
                r.title(),
                r.family(),
                null,
                r.overflowGroup(),
                70,
                r.space(),
                r.position(),
                r.orientation(),
                r.material(),
                r.metrics(),
                false,
                false,
                false,
                LeaderMode.required,
                new GuiRect(350, 100, 244, 164));
        f = new LayoutFrame(
                f.clock(),
                f.camera(),
                f.viewBasis(),
                f.hud(),
                f.world(),
                f.sources(),
                List.of(r),
                f.interaction(),
                f.config());
        LayoutResult result = new LayoutEngine().solve(f, new LayoutState());
        assertFalse(result.panels().isEmpty(), "required route stays visible with panel");
        assertTrue(result.leaders().stream().anyMatch(l -> !l.screen().isEmpty()));
    }
}
