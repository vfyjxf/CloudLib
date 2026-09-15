package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Config;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.FixedOrientation;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.FixedPosition;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.FollowPosition;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiRect;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Interaction;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutFrame;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutResult;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutState;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LeaderMode;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Material;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Metrics;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelBasis;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelRequest;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Pose;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PositionPolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PreparedLayout;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SampleContext;
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
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Directed cross-space yielding — a world panel may push aside only the
 * screen panels it names; cycles are errors; the target never moves.
 * Ported from the source DirectedYieldTests main() assertions.
 */
class DirectedYieldTest {

    private static final ViewCamera cam = ViewCamera.minecraft(new Vec3(0, 0, 0), 0, 0, 640, 400, 62);

    private static LayoutFrame frame(List<PanelRequest> requests, double eyeX, int tick) {
        ViewCamera camera = ViewCamera.minecraft(new Vec3(eyeX, 0, 0), 0, 0, 640, 400, 62);
        var source = Sources.point("source", "test", new Pose(new Vec3(0, 0, 6), PanelBasis.identity()));
        return new LayoutFrame(
                new SampleContext(tick, tick / 60., 1. / 60, .5, "test"),
                camera,
                LayoutMath.viewBasis(camera),
                List.of(),
                new WorldObstacles(List.of()),
                Map.of("source", clock -> source),
                requests,
                Interaction.none(),
                Config.defaults());
    }

    private static PanelRequest ui(String id, Space space, GuiRect rect, int priority) {
        double w = cam.unproject(new GuiVec(180, 0), 6).distanceTo(cam.unproject(new GuiVec(0, 0), 6));
        double h = cam.unproject(new GuiVec(0, 110), 6).distanceTo(cam.unproject(new GuiVec(0, 0), 6));
        Metrics metrics = new Metrics(w, h, 180, 110, h / 2, 100, 45, 80, 40);
        PositionPolicy position = space == Space.world
                ? new FixedPosition(cam.unproject(rect.center(), 6))
                : new FollowPosition(new Vec3(0, 0, 0));
        return new PanelRequest(
                id,
                "source",
                id,
                "test",
                null,
                "items",
                priority,
                space,
                position,
                new FixedOrientation(LayoutMath.viewBasis(cam)),
                Material.virtual(),
                metrics,
                false,
                false,
                false,
                LeaderMode.none,
                space == Space.screen ? rect : null);
    }

    private static PanelPlacement only(LayoutResult r, String id) {
        return r.panels().stream()
                .filter(c -> c.visualId().equals(id))
                .findFirst()
                .orElseThrow();
    }

    private static LayoutResult solve(List<PanelRequest> requests) {
        LayoutFrame f = frame(requests, 0, 0);
        LayoutResult result = new LayoutEngine().solve(f, new LayoutState());
        assertTrue(LayoutEngine.validate(f, result).isEmpty(), "result audit");
        return result;
    }

    @Test
    void direction() {
        PanelRequest b = ui("B", Space.screen, null, 1);
        LayoutResult alone = solve(List.of(b));
        GuiRect spot = only(alone, "B").screenRect();
        PanelRequest a = ui("A", Space.world, spot, 100);
        assertEquals(2, solve(List.of(a, b)).panels().size(), "no implicit cross-space relation");
        PanelRequest yielding = a.withYieldTo(b.id());
        LayoutResult result = solve(List.of(yielding, b));
        assertEquals(1, result.panels().size(), "high-priority immovable A folds instead of pushing B");
        assertEquals(Tier.folded, result.addresses().get("A").tier());
        assertEquals(spot, only(result, "B").screenRect(), "movable B keeps its independently chosen plane");
        LayoutResult reversed = solve(List.of(b, yielding));
        assertEquals(spot, only(reversed, "B").screenRect(), "input order cannot reverse yielding");
        LayoutResult otherWay = solve(List.of(b.withYieldTo(a.id()), a));
        assertEquals(2, otherWay.panels().size(), "screen B can move around world A");
        assertEquals(
                cam.unproject(spot.center(), 6),
                only(otherWay, "A").pose().origin(),
                "reverse direction leaves A fixed");
        assertFalse(
                LayoutSearch.overlap(only(otherWay, "A"), only(otherWay, "B"), 10), "yielding screen plane separated");
    }

    @Test
    void selective() {
        PanelRequest a =
                ui("A", Space.world, new GuiRect(230, 145, 180, 110), 70).withYieldTo("B");
        PanelRequest b = ui("B", Space.screen, new GuiRect(14, 14, 180, 110), 70);
        PanelRequest c = ui("C", Space.screen, new GuiRect(230, 145, 180, 110), 70);
        LayoutResult result = solve(List.of(a, b, c));
        assertEquals(3, result.panels().size(), "A yields to B only, not every screen UI on same Source");
        assertFalse(PanelRelations.mustSeparate(a, c), "relation is addressed by UI ID");
        assertTrue(PanelRelations.mustSeparate(a, b));
        LayoutFrame f = frame(List.of(a, b, c), 0, 0);
        assertEquals(
                "C",
                new PreparedLayout(f, result)
                        .hitTest(new GuiVec(320, 200))
                        .orElseThrow()
                        .visualId(),
                "overlap hit order remains screen first");
        assertTrue(a.withYieldTo().yieldTo().isEmpty(), "empty target list removes rule");
        assertEquals(1, solve(List.of(a)).panels().size(), "missing target does not reserve a phantom plane");
        assertThrows(
                IllegalArgumentException.class, () -> solve(List.of(a, b.withYieldTo("A"))), "cycle is explicit error");
    }

    @Test
    void yieldToAll() {
        PanelRequest a =
                ui("A", Space.world, new GuiRect(230, 145, 180, 110), 70).withYieldToAll();
        PanelRequest b = ui("B", Space.screen, new GuiRect(14, 14, 180, 110), 70);
        PanelRequest c = ui("C", Space.screen, new GuiRect(230, 145, 180, 110), 70);
        PanelRequest w = ui("W", Space.world, new GuiRect(400, 145, 180, 110), 70);
        assertTrue(PanelRelations.yields(a, b), "yields to any screen panel");
        assertTrue(PanelRelations.yields(a, c), "yields to unnamed screen panels too");
        assertTrue(PanelRelations.yields(a, w), "yields to other world panels");
        assertFalse(PanelRelations.yields(a, a), "never yields to itself");
        LayoutResult result = solve(List.of(a, b, c));
        assertEquals(b.fixedScreen(), only(result, "B").screenRect(), "B keeps its plane");
        assertEquals(c.fixedScreen(), only(result, "C").screenRect(), "unnamed C keeps its plane");
        assertThrows(
                IllegalArgumentException.class,
                () -> solve(List.of(a, b.withYieldToAll())),
                "two yield-all panels form a cycle");
    }

    @Test
    void yieldScopes() {
        PanelRequest world = ui("A", Space.world, new GuiRect(230, 145, 180, 110), 70);
        PanelRequest screen = ui("B", Space.screen, new GuiRect(14, 14, 180, 110), 70);
        PanelRequest screen2 = ui("C", Space.screen, new GuiRect(230, 145, 180, 110), 70);

        PanelRequest toScreen = world.withYieldToAll(Space.screen);
        assertTrue(PanelRelations.yields(toScreen, screen));
        assertFalse(PanelRelations.yields(toScreen, world), "SCREEN scope skips world panels");

        PanelRequest toWorld = screen.withYieldToAll(Space.world);
        assertTrue(PanelRelations.yields(toWorld, world));
        assertFalse(PanelRelations.yields(toWorld, screen2), "WORLD scope skips screen panels");
        assertEquals(Space.screen, toWorld.space(), "yieldToAll(world) must not relocate the request into world space");

        PanelRequest toOther = world.withYieldToOtherSpace();
        assertTrue(PanelRelations.yields(toOther, screen));
        assertFalse(PanelRelations.yields(toOther, world), "OTHER_SPACE skips same-space");

        PanelRequest toAll = screen.withYieldToAll();
        assertTrue(PanelRelations.yields(toAll, screen2));
        assertTrue(PanelRelations.yields(toAll, world));
    }

    @Test
    void camera() {
        PanelRequest b = ui("B", Space.screen, new GuiRect(230, 145, 180, 110), 1);
        PanelRequest a =
                ui("A", Space.world, new GuiRect(230, 145, 180, 110), 100).withYieldTo("B");
        LayoutEngine engine = new LayoutEngine();
        LayoutState state = new LayoutState();
        assertEquals(1, engine.solve(frame(List.of(a, b), 0, 0), state).panels().size(), "projected conflict");
        LayoutResult result = null;
        for (int tick = 1; tick <= 17; tick++) {
            LayoutFrame f = frame(List.of(a, b), 3.5, tick);
            result = engine.solve(f, state);
            assertTrue(LayoutEngine.validate(f, result).isEmpty(), "current camera audit");
            assertEquals(b.fixedScreen(), only(result, "B").screenRect(), "camera never pushes target B");
        }
        assertEquals(2, result.panels().size(), "yielding A recovers after camera clears B");
        result = engine.solve(frame(List.of(a, b), 0, 18), state);
        assertEquals(1, result.panels().size(), "A retreats immediately when projection intersects B again");
        assertEquals("B", result.panels().get(0).visualId());
    }
}
