package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Attachment;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.BoxObstacle;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Config;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Depth;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiRect;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Interaction;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutFrame;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutResult;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutState;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LeaderLine;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LeaderMode;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Obstacle;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelBasis;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelRequest;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Pose;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SampleContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SourceSnapshot;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Sources;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.ViewCamera;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.WorldObstacles;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;

import static dev.vfyjxf.cloudlib.internal.ui.inworld.layout.LayoutTestKit.request;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Leader visibility and occlusion — camera-ray triangles vs world boxes,
 * attachment-depth geometry, prepared SAT agreement, route clutter and
 * the audit-scene depth invariant. Ported from the source VisibilityTests.
 */
class VisibilityTest {

    static LayoutFrame scene(boolean wall, double eyeX) {
        var camera = ViewCamera.lookAt(new Vec3(eyeX, 0, 0), new Vec3(0, 0, 6), 640, 400, 62);
        var source = Sources.point("s", "minecraft:overworld", new Pose(new Vec3(-3, 0, 6), PanelBasis.identity()));
        var request = request("visible", new GuiRect(110, 160, 120, 80), false, LeaderMode.required);
        List<Obstacle> terrain = wall
                ? List.of(new Obstacle(
                        "thin-wall",
                        new BoxObstacle(new AABB(new Vec3(-.12, -1.5, 2.8), new Vec3(.12, 1.5, 3.2))),
                        true,
                        true,
                        "wall"))
                : List.of();
        return new LayoutFrame(
                new SampleContext(0, 0, 1. / 60, .5, "minecraft:overworld"),
                camera,
                LayoutMath.viewBasis(camera),
                List.of(),
                new WorldObstacles(terrain),
                Map.of("s", clock -> source),
                List.of(request),
                Interaction.none(),
                Config.defaults());
    }

    @Test
    void cameraAndWalls() {
        var wall = new AABB(new Vec3(-.0001, -.001, 2.99), new Vec3(.0001, .001, 3.01));
        Vec3 a = new Vec3(-3, 0, 6), b = new Vec3(3, 0, 6);
        assertFalse(SegmentCollision.hits(a, b, wall, 0), "wall does not intersect the world segment itself");
        assertFalse(SegmentCollision.hits(new Vec3(0, 0, 0), a, wall, 0), "endpoints visible");
        assertFalse(SegmentCollision.hits(new Vec3(0, 0, 0), b, wall, 0));
        assertTrue(
                LeaderVisibility.triangleBox(new Vec3(0, 0, 0), a, b, wall),
                "interior of the line is occluded between visible endpoints");
        assertFalse(LeaderVisibility.triangleBox(new Vec3(4, 0, 0), a, b, wall), "camera orbit removes occlusion");
        assertFalse(
                LeaderVisibility.triangleBox(new Vec3(0, 0, 0), a, b, new AABB(new Vec3(-1, -1, 8), new Vec3(1, 1, 9))),
                "background wall does not block foreground line");
        LayoutFrame frame = scene(true, 0);
        SourceSnapshot source = frame.sources().get("s").sample(frame.clock());
        PanelRequest request = frame.requests().get(0);
        PanelPlacement candidate = LayoutEngine.candidates(
                        frame,
                        new LayoutEngine.Unit(request.id(), List.of(request), request, source, false),
                        null,
                        List.of())
                .get(0);
        List<GuiVec> screen = List.of(
                frame.camera().project(a).point(), frame.camera().project(b).point());
        assertFalse(
                LeaderVisibility.clear(frame, candidate, screen, List.of()),
                "screen overlay does not bypass foreground wall");
        LayoutFrame open = scene(false, 0);
        LayoutResult result = new LayoutEngine().solve(open, new LayoutState());
        assertEquals(1, result.panels().size(), "unoccluded virtual panel retains required line");
        assertTrue(result.leaders().stream().anyMatch(l -> !l.screen().isEmpty()));
        for (LeaderLine line : result.leaders()) {
            assertEquals(Depth.test, line.depth(), "virtual UI must never force XRAY leaders");
            if (!line.screen().isEmpty()) {
                assertEquals(
                        line.screen().size(),
                        line.world().size(),
                        "screen leaders retain camera-correct depth geometry");
                for (int i = 0; i < line.screen().size(); i++) {
                    assertTrue(
                            open.camera()
                                            .project(line.world().get(i))
                                            .point()
                                            .distance(line.screen().get(i))
                                    < 1e-7,
                            "depth geometry reprojects to screen line");
                }
            }
        }
    }

    @Test
    void randomOcclusion() {
        Random random = new Random(440319);
        for (int i = 0; i < 1500; i++) {
            Vec3 eye = new Vec3(random.nextDouble() * 2 - 1, random.nextDouble() * 2 - 1, 0);
            Vec3 a = new Vec3(random.nextDouble() * 8 - 4, random.nextDouble() * 4 - 2, 2 + random.nextDouble() * 8);
            Vec3 b = new Vec3(random.nextDouble() * 8 - 4, random.nextDouble() * 4 - 2, 2 + random.nextDouble() * 8);
            Vec3 min =
                    new Vec3(random.nextDouble() * 6 - 3, random.nextDouble() * 3 - 1.5, 1 + random.nextDouble() * 6);
            var box = new AABB(
                    min,
                    min.add(new Vec3(.1 + random.nextDouble(), .1 + random.nextDouble(), .1 + random.nextDouble())));
            boolean exact = LeaderVisibility.triangleBox(eye, a, b, box);
            boolean sampled = false;
            for (int j = 0; j <= 80; j++) {
                if (SegmentCollision.hits(eye, a.scale(1 - j / 80.).add(b.scale(j / 80.)), box, -1e-5)) {
                    sampled = true;
                    break;
                }
            }
            assertFalse(sampled && !exact, "continuous occlusion cannot miss sampled camera rays " + i);
        }
    }

    @Test
    void attachmentDepth() {
        LayoutFrame base = scene(false, 0);
        SourceSnapshot original = base.sources().get("s").sample(base.clock());
        Vec3 attachment = new Vec3(-1, 0, 4);
        SourceSnapshot source = new SourceSnapshot(
                original.id(),
                original.dimension(),
                0,
                true,
                original.frame(),
                new Vec3(0, 0, 0),
                List.of(new Attachment("front", attachment, null, 0)),
                List.of(),
                Set.of());
        LayoutFrame frame = new LayoutFrame(
                base.clock(),
                base.camera(),
                base.viewBasis(),
                base.hud(),
                base.world(),
                Map.of("s", clock -> source),
                base.requests(),
                base.interaction(),
                base.config());
        LayoutResult result = new LayoutEngine().solve(frame, new LayoutState());
        LeaderLine line = result.leaders().stream()
                .filter(l -> !l.screen().isEmpty())
                .findFirst()
                .orElseThrow();
        assertEquals(
                attachment, line.world().get(0), "screen depth path starts at actual attachment, not source origin");
        for (Vec3 point : line.world()) {
            assertEquals(4, frame.camera().project(point).depth(), 1e-7, "screen occlusion uses attachment depth");
        }
        SourceSnapshot composite = new SourceSnapshot(
                source.id(),
                source.dimension(),
                0,
                true,
                new Pose(new Vec3(0, 0, -6), PanelBasis.identity()),
                source.velocity(),
                source.attachments(),
                List.of(),
                Set.of());
        LayoutFrame offsetRoot = new LayoutFrame(
                frame.clock(),
                frame.camera(),
                frame.viewBasis(),
                frame.hud(),
                frame.world(),
                Map.of("s", clock -> composite),
                frame.requests(),
                frame.interaction(),
                frame.config());
        LayoutResult offsetResult = new LayoutEngine().solve(offsetRoot, new LayoutState());
        assertTrue(
                offsetResult.leaders().stream().anyMatch(l -> !l.screen().isEmpty()),
                "visible attachment works even when Source frame origin is behind camera");
    }

    @Test
    void preparedSAT() {
        Random random = new Random(600431);
        for (int i = 0; i < 5000; i++) {
            double x = random.nextDouble() * 400,
                    y = random.nextDouble() * 300,
                    w = 10 + random.nextDouble() * 100,
                    h = 10 + random.nextDouble() * 100,
                    gap = random.nextDouble() * 15;
            GuiRect rect = new GuiRect(x, y, w, h);
            double bx = random.nextDouble() * 400, by = random.nextDouble() * 300;
            List<GuiVec> polygon = i % 2 == 0
                    ? new GuiRect(bx, by, 80, 60).polygon()
                    : List.of(new GuiVec(bx, by), new GuiVec(bx + 110, by + 20), new GuiVec(bx + 30, by + 95));
            assertEquals(
                    LayoutMath.overlap(rect.polygon(), polygon, gap),
                    new ScreenObstacle(polygon).overlaps(x, y, w, h, gap),
                    "prepared SAT agrees with reference " + i);
        }
    }

    @Test
    void clutter() {
        List<GuiVec> a = List.of(new GuiVec(20, 20), new GuiVec(220, 20));
        LeaderLine parallel = new LeaderLine(
                "b",
                "b",
                List.of("b"),
                List.of(new GuiVec(50, 22), new GuiVec(180, 22)),
                List.of(),
                "VISIBLE",
                Depth.test);
        assertTrue(LeaderSolver.crowded(a, List.of(parallel)), "near-parallel overlapping leaders need separation");
        LeaderLine collinear = new LeaderLine(
                "b",
                "b",
                List.of("b"),
                List.of(new GuiVec(50, 20), new GuiVec(180, 20)),
                List.of(),
                "VISIBLE",
                Depth.test);
        assertTrue(LeaderSolver.crowded(a, List.of(collinear)), "collinear overlap is not a clean route");
        for (int group = 0; group < 12; group++) {
            LayoutFrame frame = PreviewScenes.scene("audit", group, 0, 0, 0).frame();
            LayoutResult result = new LayoutEngine().solve(frame, new LayoutState());
            Set<String> optionalSources = new HashSet<>();
            for (LeaderLine leader : result.leaders()) {
                if (!leader.screen().isEmpty()) {
                    PanelPlacement owner = result.panels().stream()
                            .filter(p -> p.visualId().equals(leader.visualId()))
                            .findFirst()
                            .orElseThrow();
                    assertEquals(Depth.test, leader.depth(), "all visible lines are depth-safe");
                    assertTrue(
                            LeaderVisibility.clear(frame, owner, leader.screen(), leader.world()),
                            "all visible lines are depth-safe");
                    if (owner.active().leaderMode() == LeaderMode.auto) {
                        assertTrue(optionalSources.add(owner.source().id()), "one optional line per source");
                        assertTrue(LeaderSolver.bends(leader.screen()) <= 2, "optional line bend limit");
                    }
                }
            }
        }
    }
}
