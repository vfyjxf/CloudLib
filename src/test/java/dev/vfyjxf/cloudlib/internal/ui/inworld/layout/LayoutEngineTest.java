package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.BoxObstacle;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Config;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.FaceCamera;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.FixedOrientation;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.FixedPosition;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.FollowPosition;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiRect;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.HudRegion;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Interaction;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutFrame;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutHit;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutResult;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutState;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LeaderLine;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LeaderMode;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Material;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.MountedPosition;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.NearbyPosition;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Obstacle;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelBasis;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelMemory;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelRequest;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Pose;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PreparedLayout;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Projection;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.ReadableBothSides;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SampleContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SourceOrientation;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SourceProvider;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SourceSnapshot;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Sources;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Space;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SphereObstacle;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Tier;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Tracking;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.ViewCamera;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.WorldObstacles;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;
import java.util.Set;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.vfyjxf.cloudlib.internal.ui.inworld.layout.LayoutTestKit.camera;
import static dev.vfyjxf.cloudlib.internal.ui.inworld.layout.LayoutTestKit.dim;
import static dev.vfyjxf.cloudlib.internal.ui.inworld.layout.LayoutTestKit.flags;
import static dev.vfyjxf.cloudlib.internal.ui.inworld.layout.LayoutTestKit.frame;
import static dev.vfyjxf.cloudlib.internal.ui.inworld.layout.LayoutTestKit.movedWith;
import static dev.vfyjxf.cloudlib.internal.ui.inworld.layout.LayoutTestKit.plain;
import static dev.vfyjxf.cloudlib.internal.ui.inworld.layout.LayoutTestKit.request;
import static dev.vfyjxf.cloudlib.internal.ui.inworld.layout.LayoutTestKit.sole;
import static dev.vfyjxf.cloudlib.internal.ui.inworld.layout.LayoutTestKit.solve;
import static dev.vfyjxf.cloudlib.internal.ui.inworld.layout.LayoutTestKit.sourcePose;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end solver suite — camera math, source shapes and tracking,
 * position/orientation policies, capacity (compact/merge/overflow/omit),
 * temporal stability and input-order determinism, edge cases
 * (off-screen proxies, hit testing, world leader geometry, obstacle
 * exemptions, dimension/generation resets, backface rejection,
 * required-leader budget failure) and a 140-frame randomized audit.
 * Ported from the source LayoutTests main() assertions.
 */
class LayoutEngineTest {

    @Test
    void coordinates() {
        ViewCamera camera = camera();
        assertTrue(camera.project(new Vec3(-1.0, 1.7, 6.0)).point().x() > 500.0, "Minecraft yaw 0 right=-X");
        assertTrue(camera.project(new Vec3(0.0, 2.7, 6.0)).point().y() < 350.0, "screen Y down");
        assertTrue(
                ViewCamera.minecraft(new Vec3(0.0, 0.0, 0.0), 90.0, 0.0, 800.0, 600.0, 70.0)
                                .forward()
                                .distanceTo(new Vec3(-1.0, 0.0, 0.0))
                        < 1.0E-9,
                "yaw 90 -X");
        Random random = new Random(12L);
        for (int i = 0; i < 300; i++) {
            Vec3 point = new Vec3(
                    random.nextDouble() * 6.0 - 3.0, random.nextDouble() * 5.0, 2.0 + random.nextDouble() * 10.0);
            Projection projection = camera.project(point);
            assertTrue(
                    camera.unproject(projection.point(), projection.depth()).distanceTo(point) < 1.0E-8,
                    "projection round trip");
        }

        BoxObstacle box = new BoxObstacle(new AABB(new Vec3(-1.0, -1.0, -1.0), new Vec3(1.0, 1.0, 1.0)));
        assertTrue(
                box.intersects(
                        new Pose(new Vec3(0.0, 0.0, 0.0), PanelBasis.axisAngle(new Vec3(1.0, 1.0, 0.0), 0.78)),
                        3.0,
                        2.0,
                        0.0),
                "tilted rectangle hits box");
        assertFalse(
                box.intersects(new Pose(new Vec3(0.0, 0.0, 1.0), PanelBasis.identity()), 1.0, 1.0, 0.0),
                "zero-thickness touching allowed");
        assertTrue(
                box.intersects(new Pose(new Vec3(0.0, 0.0, 1.0), PanelBasis.identity()), 1.0, 1.0, 0.01),
                "clearance required");
        SphereObstacle sphere = new SphereObstacle(new Vec3(0.0, 0.0, 0.0), 1.0);
        assertTrue(
                sphere.intersects(new Pose(new Vec3(0.0, 0.0, 0.5), PanelBasis.identity()), 1.0, 1.0, 0.0),
                "sphere intersects plane");
    }

    @Test
    void shapesAndTracking() {
        List<SourceSnapshot> shapes = List.of(
                Sources.point("s", dim, sourcePose),
                Sources.segment("s", dim, sourcePose, new Vec3(-0.8, 0.0, 0.0), new Vec3(0.8, 0.5, 0.0)),
                Sources.sphere("s", dim, sourcePose, 0.4),
                Sources.box("s", dim, sourcePose, new Vec3(0.4, 0.4, 0.4)),
                Sources.surface(
                        "s",
                        dim,
                        new Pose(sourcePose.origin(), PanelBasis.axisAngle(new Vec3(0.0, 1.0, 0.0), Math.PI)),
                        1.0,
                        1.0));
        List<SourceSnapshot> all = new ArrayList<>(shapes);
        all.add(Sources.composite("s", dim, sourcePose, List.of(shapes.get(0), shapes.get(1))));
        PanelRequest request = request(
                "a",
                "s",
                Space.screen,
                new NearbyPosition(new Vec3(0.0, 0.0, 0.0), true, 0.6),
                new FaceCamera(false),
                Material.virtual());

        for (SourceSnapshot shape : all) {
            AtomicInteger samples = new AtomicInteger();
            LayoutFrame frame = frame(
                    0L,
                    camera(),
                    List.of(
                            request,
                            request(
                                    "b",
                                    "s",
                                    Space.screen,
                                    request.position(),
                                    request.orientation(),
                                    request.material())),
                    Map.of("s", clock -> {
                        samples.incrementAndGet();
                        return shape;
                    }),
                    List.of(),
                    new WorldObstacles(List.of()),
                    Interaction.none());
            LayoutResult result = solve(frame, new LayoutState());
            assertEquals(1, samples.get(), "sample source once per frame");
            assertEquals(2, result.addresses().size(), "shape-independent accounting");
        }

        assertTrue(shapes.get(0).hulls().isEmpty(), "point needs no synthetic AABB");
        Pose from = new Pose(new Vec3(0.0, 0.0, 0.0), PanelBasis.identity());
        Pose to = new Pose(new Vec3(2.0, 0.0, 0.0), PanelBasis.axisAngle(new Vec3(0.0, 1.0, 0.0), Math.PI / 2));
        Pose mid = Tracking.interpolate(from, to, 0.5);
        assertTrue(mid.origin().distanceTo(new Vec3(1.0, 0.0, 0.0)) < 1.0E-9, "position interpolation");
        assertTrue(
                mid.basis().normal().distanceTo(new Vec3(Math.sqrt(0.5), 0.0, Math.sqrt(0.5))) < 1.0E-8,
                "rotation SLERP");
        Pose cw = new Pose(from.origin(), PanelBasis.axisAngle(new Vec3(0.0, 1.0, 0.0), Math.toRadians(179.0)));
        Pose ccw = new Pose(from.origin(), PanelBasis.axisAngle(new Vec3(0.0, 1.0, 0.0), Math.toRadians(-179.0)));
        assertTrue(Tracking.interpolate(cw, ccw, 0.5).basis().normal().z() < -0.9999, "short arc over 180");
        SourceProvider provider = Tracking.interpolated(
                () -> new Tracking.TickState(
                        "s",
                        dim,
                        3L,
                        true,
                        from,
                        new Pose(new Vec3(30.0, 0.0, 0.0), to.basis()),
                        new Vec3(0.0, 0.0, 0.0),
                        false),
                Sources::point,
                8.0);
        assertEquals(
                30.0,
                provider.sample(new SampleContext(0L, 0.0, 0.01, 0.5, dim))
                        .frame()
                        .origin()
                        .x(),
                "teleport snaps");
        SourceProvider absent = clock -> SourceSnapshot.absent("s", "minecraft:overworld", 4L);
        LayoutResult missing = solve(
                frame(
                        0L,
                        camera(),
                        List.of(request),
                        Map.of("s", absent),
                        List.of(),
                        new WorldObstacles(List.of()),
                        Interaction.none()),
                new LayoutState());
        assertEquals(Tier.unavailable, missing.addresses().get("a").tier(), "missing remains recoverable");
        assertEquals(1, missing.dock().total());
    }

    @Test
    void policies() {
        Vec3 fixed = new Vec3(-1.4, 2.0, 5.0);
        PanelRequest fixedRequest = request(
                "fixed",
                "s",
                Space.world,
                new FixedPosition(fixed),
                new FixedOrientation(LayoutMath.viewBasis(camera())),
                Material.virtual());
        LayoutState state = new LayoutState();
        PanelPlacement first = sole(solve(plain(0L, List.of(fixedRequest)), state));
        LayoutFrame movedFrame = frame(
                20L,
                ViewCamera.minecraft(new Vec3(0.1, 1.7, 0.0), 2.0, 1.0, 1000.0, 700.0, 62.0),
                List.of(fixedRequest),
                plain(0L, List.of()).sources(),
                List.of(),
                new WorldObstacles(List.of()),
                Interaction.none());
        PanelPlacement second = sole(solve(movedFrame, state));
        assertTrue(
                first.pose().origin().distanceTo(fixed) < 1.0E-9
                        && second.pose().origin().distanceTo(fixed) < 1.0E-9,
                "fixed world position invariant");
        assertEquals(first.pose().basis(), second.pose().basis(), "fixed orientation invariant");
        PanelRequest billboard = request(
                "billboard", "s", Space.world, fixedRequest.position(), new FaceCamera(false), Material.virtual());
        second = sole(solve(movedWith(movedFrame, List.of(billboard)), new LayoutState()));
        assertTrue(
                second.pose()
                                .basis()
                                .normal()
                                .dot(Vecs.unit(movedFrame.camera().eye().subtract(fixed)))
                        > 0.999999999,
                "billboard faces actual camera position");
        PanelRequest follow = request(
                "follow",
                "s",
                Space.world,
                new FollowPosition(new Vec3(-1.0, 1.0, 0.0)),
                new FaceCamera(false),
                Material.virtual());
        second = sole(solve(plain(0L, List.of(follow)), new LayoutState()));
        assertTrue(
                second.pose().origin().distanceTo(sourcePose.at(new Vec3(-1.0, 1.0, 0.0))) < 1.0E-9,
                "follow source frame");
        WorldObstacles world = new WorldObstacles(List.of(new Obstacle(
                "wall",
                new BoxObstacle(new AABB(new Vec3(-3.0, 0.5, 4.8), new Vec3(1.0, 3.0, 5.2))),
                true,
                true,
                "brick")));
        LayoutFrame wallFrame = frame(
                0L,
                camera(),
                List.of(fixedRequest),
                plain(0L, List.of()).sources(),
                List.of(),
                world,
                Interaction.none());
        assertEquals(Space.world, sole(solve(wallFrame, new LayoutState())).space(), "virtual penetrates building");
        PanelRequest physical = request(
                "physical", "s", Space.world, fixedRequest.position(), fixedRequest.orientation(), Material.physical());
        assertTrue(
                solve(movedWith(wallFrame, List.of(physical)), new LayoutState())
                        .panels()
                        .isEmpty(),
                "physical collision folds, cannot displace fixed pose");
        PanelBasis flipped = PanelBasis.axisAngle(new Vec3(0.0, 1.0, 0.0), Math.PI);
        SourceSnapshot surface = Sources.surface("s", dim, new Pose(new Vec3(0.0, 1.6, 5.0), flipped), 2.0, 2.0);
        PanelRequest mount = request(
                "mount",
                "s",
                Space.world,
                new MountedPosition(new Vec3(0.0, 0.0, 0.0), 2.0, 2.0, 0.02),
                new SourceOrientation(PanelBasis.identity()),
                Material.virtual());
        LayoutFrame mountFrame = frame(
                0L,
                camera(),
                List.of(mount),
                Map.of("s", clock -> surface),
                List.of(),
                new WorldObstacles(List.of()),
                Interaction.none());
        second = sole(solve(mountFrame, new LayoutState()));
        assertTrue(
                second.pose().origin().distanceTo(surface.frame().at(new Vec3(0.0, 0.0, 0.02))) < 1.0E-9,
                "surface offset in local normal");
        assertEquals(
                "ATTACHED",
                solve(mountFrame, new LayoutState()).leaders().get(0).status(),
                "mount ownership does not need redundant leader");
        PanelRequest large = request(
                "large",
                "s",
                Space.world,
                new MountedPosition(new Vec3(0.0, 0.0, 0.0), 0.5, 0.5, 0.02),
                mount.orientation(),
                Material.virtual());
        assertTrue(
                solve(movedWith(mountFrame, List.of(large)), new LayoutState())
                        .panels()
                        .isEmpty(),
                "mounted patch is a hard bound");
        PanelRequest screenFixed = request(
                "fixed",
                "s",
                Space.screen,
                fixedRequest.position(),
                fixedRequest.orientation(),
                fixedRequest.material());
        LayoutResult screenResult = solve(movedWith(movedFrame, List.of(screenFixed)), state);
        assertTrue(
                screenResult.addresses().containsKey("fixed")
                        && sole(screenResult).space() == Space.screen,
                "same identity across presentation mode");
    }

    @Test
    void capacity() {
        List<PanelRequest> requests = new ArrayList<>();
        for (int i = 0; i < 80; i++) {
            requests.add(flags(
                    request(
                            "ui-" + String.format("%03d", i),
                            "s",
                            Space.screen,
                            new NearbyPosition(new Vec3(0.0, 0.0, 0.0), true, 0.7),
                            new FaceCamera(false),
                            Material.virtual()),
                    true,
                    false,
                    false,
                    null,
                    20 + i % 80,
                    LeaderMode.auto));
        }
        List<HudRegion> hud = List.of(
                HudRegion.rectangle("chat", new GuiRect(14.0, 410.0, 320.0, 276.0)),
                HudRegion.rectangle("hotbar", new GuiRect(350.0, 626.0, 420.0, 60.0)),
                HudRegion.rectangle("quest", new GuiRect(750.0, 70.0, 235.0, 270.0)));
        LayoutFrame frame = frame(
                0L,
                camera(),
                requests,
                plain(0L, List.of()).sources(),
                hud,
                new WorldObstacles(List.of()),
                Interaction.none());
        LayoutResult result = solve(frame, new LayoutState());
        assertTrue(result.dock().total() > 50, "overflow folds excess");
        assertTrue(result.panels().stream().anyMatch(c -> c.tier() == Tier.compact), "capacity compacts");
        assertEquals(80, result.addresses().size(), "no requests lost");
        assertEquals(1, result.dock().groups().size(), "one bounded root group");
        LayoutFrame covered = frame(
                0L,
                camera(),
                requests,
                frame.sources(),
                List.of(HudRegion.rectangle("modal", new GuiRect(0.0, 0.0, 1000.0, 700.0))),
                frame.world(),
                Interaction.none());
        result = solve(covered, new LayoutState());
        assertTrue(
                result.dock().externalOnly() && result.dock().total() == 80,
                "infeasible viewport never overlaps HUD or loses data");
        assertTrue(result.panels().isEmpty());
        String focus = "ui-000";
        LayoutFrame focused = frame(
                30L,
                camera(),
                requests,
                frame.sources(),
                hud,
                frame.world(),
                new Interaction(focus, null, true, "devices", 2));
        result = solve(focused, new LayoutState());
        assertTrue(
                result.panels().stream().anyMatch(c -> c.active().id().equals(focus)), "folded member can be promoted");
        assertNotNull(result.drawer(), "paginated drawer");
        assertEquals(2, result.drawer().page());
        List<PanelRequest> tabs = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            tabs.add(flags(
                    request(
                            "tab" + i,
                            "s",
                            Space.screen,
                            new NearbyPosition(new Vec3(0.0, 0.0, 0.0), true, 0.7),
                            new FaceCamera(false),
                            Material.virtual()),
                    true,
                    true,
                    false,
                    "energy",
                    70,
                    LeaderMode.auto));
        }
        result = solve(plain(0L, tabs), new LayoutState());
        assertEquals(1, result.panels().size(), "opt-in semantic tabs");
        assertEquals(4, result.panels().get(0).members().size());
        assertTrue(
                result.addresses().values().stream().allMatch(a -> a.tier() == Tier.merged),
                "merged addresses retain every ID");
        PanelRequest optional = flags(
                request(
                        "optional",
                        "s",
                        Space.screen,
                        new FixedPosition(sourcePose.origin()),
                        new FaceCamera(false),
                        Material.virtual()),
                true,
                false,
                true,
                null,
                2,
                LeaderMode.none);
        result = solve(movedWith(covered, List.of(optional)), new LayoutState());
        assertEquals(Tier.omitted, result.addresses().get("optional").tier(), "omission only by explicit opt-in");
    }

    @Test
    void stability() {
        PanelRequest request = request(
                "stable",
                "s",
                Space.screen,
                new NearbyPosition(new Vec3(0.0, 0.0, 0.0), true, 0.7),
                new FaceCamera(false),
                Material.virtual());
        LayoutState state = new LayoutState();
        LayoutEngine engine = new LayoutEngine();
        String last = null;
        int switches = 0;
        for (int i = 0; i < 120; i++) {
            ViewCamera jittered = ViewCamera.minecraft(
                    new Vec3(Math.sin(i * 0.3) * 0.006, 1.7, 0.0), Math.sin(i * 0.21) * 0.08, 0.0, 1000.0, 700.0, 62.0);
            LayoutFrame frame = frame(
                    i,
                    jittered,
                    List.of(request),
                    plain(0L, List.of()).sources(),
                    List.of(),
                    new WorldObstacles(List.of()),
                    Interaction.none());
            LayoutResult result = engine.solve(frame, state);
            assertTrue(LayoutEngine.validate(frame, result).isEmpty(), "jitter audit");
            PanelPlacement candidate = sole(result);
            if (last != null && !last.equals(candidate.slot())) {
                switches++;
            }
            last = candidate.slot();
        }
        assertEquals(0, switches, "subpixel camera jitter must not re-slot");
        List<PanelRequest> requests = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            requests.add(flags(
                    request("d" + i, "s", Space.screen, request.position(), request.orientation(), request.material()),
                    true,
                    false,
                    false,
                    null,
                    50,
                    LeaderMode.none));
        }
        LayoutResult ordered = solve(plain(0L, requests), new LayoutState());
        Collections.shuffle(requests, new Random(30L));
        LayoutResult shuffled = solve(plain(0L, requests), new LayoutState());
        assertEquals(ordered.addresses(), shuffled.addresses(), "input-order independent identity allocation");
        assertEquals(
                ordered.panels().stream().map(c -> c.visualId() + c.slot()).toList(),
                shuffled.panels().stream().map(c -> c.visualId() + c.slot()).toList(),
                "input-order independent slots");
    }

    @Test
    void integrationAndEdges() {
        PanelRequest request = request(
                "edge",
                "s",
                Space.screen,
                new FollowPosition(new Vec3(0.0, 0.0, 0.0)),
                new FaceCamera(false),
                Material.virtual());

        for (Vec3 origin : List.of(new Vec3(15.0, 1.5, 6.0), new Vec3(-15.0, 1.5, 6.0), new Vec3(0.0, 1.5, -5.0))) {
            LayoutFrame frame = frame(
                    0L,
                    camera(),
                    List.of(request),
                    Map.of(
                            "s",
                            clock ->
                                    Sources.point("s", "minecraft:overworld", new Pose(origin, PanelBasis.identity()))),
                    List.of(),
                    new WorldObstacles(List.of()),
                    Interaction.none());
            LayoutResult result = solve(frame, new LayoutState());
            assertTrue(
                    result.leaders().stream()
                            .anyMatch(l -> l.status().startsWith("EDGE_PROXY")
                                    && l.screen().isEmpty()),
                    "offscreen and behind preserve direction status without a fictitious world line");
            PanelPlacement candidate = sole(result);
            LayoutHit hit = new PreparedLayout(frame, result)
                    .hitTest(candidate.center())
                    .orElseThrow();
            assertEquals(0.5, hit.u(), 1.0E-8, "screen normalized UV");
            assertEquals(0.5, hit.v(), 1.0E-8);
        }

        PanelBasis tilted = LayoutMath.viewBasis(camera()).compose(PanelBasis.axisAngle(new Vec3(0.0, 0.0, 1.0), 0.37));
        PanelRequest tiltedRequest = request(
                "tilted",
                "s",
                Space.world,
                new FixedPosition(new Vec3(-1.4, 2.1, 5.0)),
                new FixedOrientation(tilted),
                Material.virtual());
        LayoutFrame tiltedFrame = plain(0L, List.of(tiltedRequest));
        LayoutResult tiltedResult = solve(tiltedFrame, new LayoutState());
        PanelPlacement panel = sole(tiltedResult);
        Vec3 local =
                panel.pose().at(new Vec3(-0.27 * panel.worldWidth(), -0.10999999999999999 * panel.worldHeight(), 0.0));
        LayoutHit hit = new PreparedLayout(tiltedFrame, tiltedResult)
                .hitTest(tiltedFrame.camera().project(local).point())
                .orElseThrow();
        assertEquals(0.23, hit.u(), 1.0E-8, "tilted world inverse projection UV");
        assertEquals(0.61, hit.v(), 1.0E-8);

        for (LeaderLine leader : tiltedResult.leaders()) {
            if (!leader.world().isEmpty()) {
                for (int i = 0; i < leader.world().size(); i++) {
                    assertTrue(
                            tiltedFrame
                                            .camera()
                                            .project(leader.world().get(i))
                                            .point()
                                            .distance(leader.screen().get(i))
                                    < 1.0E-7,
                            "leader inverse W agrees with projection");
                }
                Vec3 terminal = panel.pose()
                        .basis()
                        .inverse(leader.world()
                                .get(leader.world().size() - 1)
                                .subtract(panel.pose().origin()));
                assertEquals(0.0, terminal.z(), 1.0E-8, "world leader terminates on GUI plane");
                assertTrue(
                        Math.abs(Math.abs(terminal.x()) - panel.worldWidth() / 2.0) < 1.0E-8
                                || Math.abs(Math.abs(terminal.y()) - panel.worldHeight() / 2.0) < 1.0E-8,
                        "world leader terminates on GUI edge");
            }
        }

        WorldObstacles ownBody = new WorldObstacles(List.of(new Obstacle(
                "own",
                new BoxObstacle(new AABB(new Vec3(-1.0, -1.0, -1.0), new Vec3(1.0, 1.0, 1.0))),
                true,
                true,
                "stone")));
        assertTrue(
                ownBody.lineFree(
                        List.of(new Vec3(0.0, 0.0, 0.0), new Vec3(2.0, 0.0, 0.0), new Vec3(3.0, 0.0, 0.0)),
                        Set.of("own"),
                        0.0),
                "own body excluded on departure only");
        assertFalse(
                ownBody.lineFree(
                        List.of(new Vec3(0.0, 0.0, 0.0), new Vec3(2.0, 0.0, 0.0), new Vec3(0.0, 0.0, 0.0)),
                        Set.of("own"),
                        0.0),
                "route cannot reenter source body");
        LayoutState state = new LayoutState();
        solve(plain(0L, List.of(request)), state);
        SourceSnapshot nether = Sources.point("s", "minecraft:the_nether", sourcePose);
        LayoutResult wrongDim = solve(
                frame(
                        2L,
                        camera(),
                        List.of(request),
                        Map.of("s", clock -> nether),
                        List.of(),
                        new WorldObstacles(List.of()),
                        Interaction.none()),
                state);
        assertEquals(Tier.unavailable, wrongDim.addresses().get("edge").tier(), "dimension mismatch is unavailable");
        SourceSnapshot original = Sources.point("s", "minecraft:overworld", sourcePose);
        SourceSnapshot regenerated = new SourceSnapshot(
                "s",
                "minecraft:overworld",
                9L,
                true,
                sourcePose,
                new Vec3(0.0, 0.0, 0.0),
                original.attachments(),
                original.hulls(),
                Set.of());
        LayoutResult regen = solve(
                frame(
                        3L,
                        camera(),
                        List.of(request),
                        Map.of("s", clock -> regenerated),
                        List.of(),
                        new WorldObstacles(List.of()),
                        Interaction.none()),
                state);
        assertFalse(regen.panels().isEmpty(), "new generation invalidates stale folding dwell");
        assertEquals(9L, state.panels.get("edge").generation());
        GuiRect a0 = new GuiRect(40.0, 200.0, 40.0, 40.0);
        GuiRect a1 = new GuiRect(300.0, 200.0, 40.0, 40.0);
        GuiRect b0 = new GuiRect(190.0, 80.0, 40.0, 40.0);
        GuiRect b1 = new GuiRect(190.0, 320.0, 40.0, 40.0);
        PanelPlacement slideA = new PanelPlacement(
                "slide-a",
                List.of(request),
                request,
                original,
                "a",
                Tier.compact,
                Space.screen,
                sourcePose,
                1.0,
                1.0,
                a1,
                a1.polygon(),
                0.0,
                false);
        PanelPlacement slideB = new PanelPlacement(
                "slide-b",
                List.of(request),
                request,
                original,
                "b",
                Tier.compact,
                Space.screen,
                sourcePose,
                1.0,
                1.0,
                b1,
                b1.polygon(),
                0.0,
                false);
        Map<String, PanelMemory> previous =
                Map.of("slide-b", new PanelMemory("b", Tier.compact, sourcePose, b0, Space.screen, 0.0, 0.0, 0L));
        assertFalse(
                LayoutEngine.safeSweep(tiltedFrame, slideA, a0, a1, List.of(slideA, slideB), List.of(), previous),
                "concurrent sweeps cannot cross");
        PanelRequest backface = request(
                "back",
                "s",
                Space.world,
                new FixedPosition(new Vec3(-1.4, 2.1, 5.0)),
                new FixedOrientation(PanelBasis.identity()),
                Material.virtual());
        assertTrue(
                solve(plain(0L, List.of(backface)), new LayoutState()).panels().isEmpty(),
                "backface never mirrors text");
        PanelRequest both = request(
                "both",
                "s",
                Space.world,
                backface.position(),
                new ReadableBothSides(backface.orientation()),
                Material.virtual());
        assertFalse(
                solve(plain(0L, List.of(both)), new LayoutState()).panels().isEmpty(),
                "explicit two-sided reading keeps plane");
        assertThrows(
                IllegalArgumentException.class,
                () -> new Pose(new Vec3(Double.NaN, 0.0, 0.0), PanelBasis.identity()),
                "reject nonfinite source input");
        PanelRequest required = flags(tiltedRequest, true, false, false, null, 80, LeaderMode.required);
        LayoutFrame budgeted = new LayoutFrame(
                tiltedFrame.clock(),
                tiltedFrame.camera(),
                tiltedFrame.viewBasis(),
                tiltedFrame.hud(),
                tiltedFrame.world(),
                tiltedFrame.sources(),
                List.of(required),
                tiltedFrame.interaction(),
                new Config(14.0, 10.0, 0.93, 14, 8, 65.0, 0.25, 0, 0.015, 12, 0.48));
        LayoutResult starved = solve(budgeted, new LayoutState());
        assertTrue(starved.panels().isEmpty(), "required leader budget failure folds entire relationship");
        assertEquals(1, starved.dock().total());
    }

    @Test
    void randomized() {
        Random random = new Random(720211L);
        LayoutState state = new LayoutState();
        LayoutEngine engine = new LayoutEngine();
        for (int i = 0; i < 140; i++) {
            double width = i % 9 == 0 ? 640.0 : 1000.0;
            double height = i % 9 == 0 ? 480.0 : 700.0;
            ViewCamera camera = ViewCamera.minecraft(
                    new Vec3(Math.sin(i * 0.04) * 0.7, 1.8, 0.1),
                    Math.sin(i * 0.03) * 12.0,
                    Math.cos(i * 0.02) * 9.0,
                    width,
                    height,
                    62.0);
            List<PanelRequest> requests = new ArrayList<>();
            Map<String, SourceProvider> providers = new HashMap<>();
            List<Obstacle> obstacles = List.of(new Obstacle(
                    "pillar",
                    new BoxObstacle(new AABB(new Vec3(0.9, 0.0, 4.0), new Vec3(1.7, 3.0, 5.0))),
                    true,
                    true,
                    "stone"));

            for (int j = 0; j < 18; j++) {
                Pose pose = new Pose(
                        new Vec3((j % 6 - 2.5) * 0.8, 1.1 + j / 6 * 0.7, 5.5 + Math.sin(i * 0.04 + j)),
                        PanelBasis.axisAngle(new Vec3(0.0, 1.0, 0.0), i * 0.007));
                String sourceId = "s" + j;
                SourceSnapshot snapshot = j % 3 == 0
                        ? Sources.sphere(sourceId, "minecraft:overworld", pose, 0.2)
                        : (j % 3 == 1
                                ? Sources.point(sourceId, "minecraft:overworld", pose)
                                : Sources.segment(
                                        sourceId,
                                        "minecraft:overworld",
                                        pose,
                                        new Vec3(-0.2, 0.0, 0.0),
                                        new Vec3(0.2, 0.3, 0.0)));
                providers.put(sourceId, clock -> snapshot);
                PanelRequest request = request(
                        "r" + j,
                        sourceId,
                        j % 3 == 0 ? Space.world : Space.screen,
                        new NearbyPosition(new Vec3(0.0, 0.0, 0.0), true, 0.3),
                        j % 4 == 0
                                ? new FixedOrientation(PanelBasis.axisAngle(new Vec3(1.0, 1.0, 0.0), 0.3))
                                : new FaceCamera(false),
                        j % 2 == 0 ? Material.physical() : Material.virtual());
                requests.add(flags(
                        request, true, false, false, null, 20 + j * 4, j < 3 ? LeaderMode.required : LeaderMode.auto));
            }

            List<HudRegion> hud = new ArrayList<>();
            hud.add(HudRegion.rectangle("hotbar", new GuiRect(width * 0.28, height - 72.0, width * 0.44, 58.0)));
            if (i % 14 < 7) {
                hud.add(HudRegion.rectangle(
                        "chat", new GuiRect(14.0, height - 235.0, Math.min(260.0, width * 0.32), 145.0)));
            }
            if (i % 31 == 0) {
                hud.add(new HudRegion(
                        "diamond",
                        List.of(
                                new GuiVec(width * 0.5, 45.0),
                                new GuiVec(width * 0.5 + 70.0, 100.0),
                                new GuiVec(width * 0.5, 155.0),
                                new GuiVec(width * 0.5 - 70.0, 100.0))));
            }

            Collections.shuffle(requests, random);
            LayoutFrame frame =
                    frame(i, camera, requests, providers, hud, new WorldObstacles(obstacles), Interaction.none());
            LayoutResult result = engine.solve(frame, state);
            assertTrue(LayoutEngine.validate(frame, result).isEmpty(), "random constraints frame " + i);
            assertEquals(18, result.addresses().size(), "random accounting");

            for (PanelPlacement candidate : result.panels()) {
                assertTrue(
                        candidate
                                        .pose()
                                        .basis()
                                        .right()
                                        .cross(candidate.pose().basis().up())
                                        .distanceTo(candidate.pose().basis().normal())
                                < 1.0E-8,
                        "orthonormal arbitrary pose");
            }
        }
    }
}
