package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.*;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Cross-space avoidance / camera / alignment assertions, ported from the
 * source suite's {@code CrossSpaceTests}. The V3 semantics are
 * directional: {@code crossSpaceAvoidance} on one request makes that
 * request yield to the other space; enabling it on both sides of a pair
 * is a cyclic rule and rejected.
 */
final class CrossSpaceTest {

    private static LayoutFrame scene(
            long tick, boolean screenAvoids, boolean worldAvoids, double eyeX, List<HudRegion> hud) {
        ViewCamera base = ViewCamera.minecraft(new Vec3(0, 0, 0), 0, 0, 640, 400, 62);
        ViewCamera camera = ViewCamera.minecraft(new Vec3(eyeX, 0, 0), 0, 0, 640, 400, 62);
        double w = base.unproject(new GuiVec(410, 200), 6).distanceTo(base.unproject(new GuiVec(230, 200), 6));
        double h = base.unproject(new GuiVec(320, 255), 6).distanceTo(base.unproject(new GuiVec(320, 145), 6));
        Metrics metrics = new Metrics(w, h, 180, 110, h / 2, 100, 45, 80, 40);
        SourceSnapshot source = Sources.point("s", "test", new Pose(new Vec3(0, 0, 6), PanelBasis.identity()));
        PanelRequest world = new PanelRequest(
                        "world",
                        "s",
                        "world",
                        "test",
                        null,
                        "items",
                        90,
                        Space.world,
                        new FixedPosition(source.frame().origin()),
                        new FixedOrientation(LayoutMath.viewBasis(base)),
                        Material.virtual(),
                        metrics,
                        false,
                        false,
                        false,
                        LeaderMode.none,
                        null)
                .withCrossSpaceAvoidance(worldAvoids);
        PanelRequest screen = new PanelRequest(
                        "screen",
                        "s",
                        "screen",
                        "test",
                        null,
                        "items",
                        70,
                        Space.screen,
                        new FollowPosition(new Vec3(0, 0, 0)),
                        new FaceCamera(false),
                        Material.virtual(),
                        metrics,
                        false,
                        false,
                        false,
                        LeaderMode.none,
                        new GuiRect(230, 145, 180, 110))
                .withCrossSpaceAvoidance(screenAvoids);
        return new LayoutFrame(
                new SampleContext(tick, tick / 60., 1. / 60, .5, "test"),
                camera,
                LayoutMath.viewBasis(camera),
                hud,
                new WorldObstacles(List.of()),
                Map.of("s", clock -> source),
                List.of(world, screen),
                Interaction.none(),
                Config.defaults());
    }

    @Test
    void policies() {
        for (boolean screen : List.of(false, true)) {
            for (boolean world : List.of(false, true)) {
                if (screen && world) {
                    LayoutFrame frame = scene(0, true, true, 0, List.of());
                    assertThrows(
                            IllegalArgumentException.class,
                            () -> new LayoutEngine().solve(frame, new LayoutState()),
                            "mutual avoidance is a cyclic yield rule");
                    continue;
                }
                LayoutFrame frame = scene(0, screen, world, 0, List.of());
                LayoutResult result = new LayoutEngine().solve(frame, new LayoutState());
                assertEquals(
                        screen || world ? 1 : 2, result.panels().size(), "directional opt-in " + screen + "/" + world);
                assertTrue(LayoutEngine.validate(frame, result).isEmpty(), "validator uses same pair policy");
                if (result.panels().size() == 2) {
                    assertEquals(
                            "screen",
                            new PreparedLayout(frame, result)
                                    .hitTest(new GuiVec(320, 200))
                                    .orElseThrow()
                                    .visualId(),
                            "screen layer receives overlap clicks");
                }
                assertTrue(
                        PanelRelations.mustSeparate(
                                frame.requests().get(0), frame.requests().get(0)),
                        "same-space avoidance stays mandatory");
            }
        }
        LayoutFrame covered =
                scene(0, false, false, 0, List.of(HudRegion.rectangle("hud", new GuiRect(200, 120, 240, 160))));
        assertTrue(
                new LayoutEngine().solve(covered, new LayoutState()).panels().isEmpty(), "opt-out does not ignore HUD");
        PanelRequest screen = scene(0, false, true, 0, List.of()).requests().get(1);
        PanelRequest enabled = screen.withCrossSpaceAvoidance(true);
        assertTrue(
                enabled.crossSpaceAvoidance() && enabled.fixedScreen().equals(screen.fixedScreen()),
                "copy API preserves fixed rectangle");
    }

    @Test
    void camera() {
        LayoutState state = new LayoutState();
        LayoutEngine engine = new LayoutEngine();
        assertEquals(
                1,
                engine.solve(scene(0, false, true, 0, List.of()), state)
                        .panels()
                        .size(),
                "initial projected conflict");
        LayoutResult last = null;
        for (int i = 1; i <= 17; i++) {
            LayoutFrame frame = scene(i, false, true, 3.5, List.of());
            last = engine.solve(frame, state);
            assertTrue(LayoutEngine.validate(frame, last).isEmpty(), "moving camera obeys constraints");
            for (PanelPlacement panel : last.panels()) {
                if (panel.space() == Space.world) {
                    assertEquals(new Vec3(0, 0, 6), panel.pose().origin(), "fixed world UI is not relocated");
                }
            }
        }
        assertEquals(2, last.panels().size(), "projection separation permits debounced recovery");
        assertEquals(
                1,
                engine.solve(scene(18, false, true, 0, List.of()), state)
                        .panels()
                        .size(),
                "renewed camera conflict retreats immediately");
    }

    @Test
    void aesthetics() {
        LayoutFrame frame = scene(0, false, false, 0, List.of());
        PanelPlacement panel = new LayoutEngine()
                .solve(frame, new LayoutState()).panels().stream()
                        .filter(p -> p.space() == Space.screen)
                        .findFirst()
                        .orElseThrow();
        GuiRect aligned = new GuiRect(420, 145, 100, 110);
        GuiRect unaligned = new GuiRect(420, 166, 100, 110);
        PanelPlacement a = new PanelPlacement(
                "a",
                panel.members(),
                panel.active(),
                panel.source(),
                "a",
                panel.tier(),
                panel.space(),
                panel.pose(),
                panel.worldWidth(),
                panel.worldHeight(),
                aligned,
                aligned.polygon(),
                0,
                false);
        PanelPlacement b = new PanelPlacement(
                "b",
                panel.members(),
                panel.active(),
                panel.source(),
                "b",
                panel.tier(),
                panel.space(),
                panel.pose(),
                panel.worldWidth(),
                panel.worldHeight(),
                unaligned,
                unaligned.polygon(),
                0,
                false);
        assertTrue(
                LayoutSearch.alignment(panel, a, frame.config()) < LayoutSearch.alignment(panel, b, frame.config()),
                "neighboring screen rows prefer aligned edges");
    }
}
