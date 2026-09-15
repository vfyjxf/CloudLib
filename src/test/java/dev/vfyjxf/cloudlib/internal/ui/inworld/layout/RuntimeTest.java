package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.BoxObstacle;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.FixedOrientation;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutFrame;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutResult;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutState;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LeaderLine;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LeaderMode;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Material;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Obstacle;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelRequest;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PositionPolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PositionSlot;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Projector;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SampleContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SourceProvider;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SourceSnapshot;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Space;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.ViewCamera;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.WorldObstacles;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.concurrent.atomic.AtomicInteger;

import static dev.vfyjxf.cloudlib.internal.ui.inworld.layout.LayoutTestKit.frame;
import static dev.vfyjxf.cloudlib.internal.ui.inworld.layout.LayoutTestKit.request;
import static dev.vfyjxf.cloudlib.internal.ui.inworld.layout.LayoutTestKit.small;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Runtime/cache behaviour — unchanged frames reuse whole results, custom
 * policies are never silently cached, camera motion reuses the stable
 * frame, and reused screens still reroute around new obstructions.
 * Ported from the source RuntimeTests main() assertions.
 */
class RuntimeTest {

    private static LayoutFrame at(
            LayoutFrame frame,
            long index,
            Projector camera,
            WorldObstacles world,
            Map<String, SourceProvider> sources) {
        return new LayoutFrame(
                new SampleContext(index, index / 60., 1. / 60, .5, frame.clock().dimension()),
                camera,
                LayoutMath.viewBasis(camera),
                frame.hud(),
                world,
                sources,
                frame.requests(),
                frame.interaction(),
                frame.config());
    }

    @Test
    void unchanged() {
        PanelRequest request = request("steady", null, false, LeaderMode.none);
        LayoutFrame base = frame(0, 640, 400, List.of(request), List.of());
        AtomicInteger samples = new AtomicInteger();
        SourceSnapshot snapshot = base.sources().get("s").sample(base.clock());
        Map<String, SourceProvider> providers = Map.of("s", clock -> {
            samples.incrementAndGet();
            return snapshot;
        });
        LayoutState state = new LayoutState();
        LayoutEngine engine = new LayoutEngine();
        LayoutResult first = engine.solve(at(base, 0, base.camera(), base.world(), providers), state);
        assertTrue(engine.lastStats().expansions() > 0, "first layout searches");
        long total = 0;
        for (int i = 1; i <= 300; i++) {
            long start = System.nanoTime();
            LayoutResult next = engine.solve(at(base, i, base.camera(), base.world(), providers), state);
            total += System.nanoTime() - start;
            assertEquals(0, engine.lastStats().expansions(), "unchanged immutable frame is reused");
            assertEquals(i, next.frameIndex(), "reuse stamps current frame");
            assertEquals(first.panels(), next.panels());
            assertTrue(next.transitions().isEmpty(), "reuse stamps current frame without replaying transitions");
        }
        assertEquals(301, samples.get(), "source providers still sampled once per render frame");
        var camera = ViewCamera.minecraft(new Vec3(.1, 0, 0), 0, 0, 640, 400, 62);
        engine.solve(at(base, 301, camera, base.world(), providers), state);
        assertTrue(engine.lastStats().nanos() > 0, "camera change refreshes frame inputs");
        assertEquals(camera, state.cachedInput.camera());
        var wall = new WorldObstacles(List.of(new Obstacle(
                "new-wall", new BoxObstacle(new AABB(new Vec3(4, 4, 4), new Vec3(5, 5, 5))), true, true, "wall")));
        engine.solve(at(base, 302, camera, wall, providers), state);
        assertEquals(wall, state.cachedInput.world(), "world collision/opacity changes refresh visibility inputs");
        state.reset();
        engine.solve(at(base, 303, camera, wall, providers), state);
        assertTrue(engine.lastStats().expansions() > 0, "view reset clears cached result");
    }

    @Test
    void mutablePolicy() {
        AtomicInteger calls = new AtomicInteger();
        LayoutFrame base = frame(0, 640, 400, List.of(), List.of());
        PositionPolicy custom = (context, w, h) -> {
            calls.incrementAndGet();
            return List.of(new PositionSlot("custom", new Vec3(0, 0, 6), 0));
        };
        PanelRequest request = new PanelRequest(
                "custom",
                "s",
                "custom",
                "test",
                null,
                "items",
                70,
                Space.world,
                custom,
                new FixedOrientation(base.viewBasis()),
                Material.virtual(),
                small,
                false,
                false,
                false,
                LeaderMode.none,
                null);
        LayoutFrame frame = new LayoutFrame(
                base.clock(),
                base.camera(),
                base.viewBasis(),
                List.of(),
                base.world(),
                base.sources(),
                List.of(request),
                base.interaction(),
                base.config());
        LayoutState state = new LayoutState();
        LayoutEngine engine = new LayoutEngine();
        engine.solve(frame, state);
        int previous = calls.get();
        engine.solve(at(frame, 1, frame.camera(), frame.world(), frame.sources()), state);
        assertTrue(calls.get() > previous, "custom policies are never silently cached");
    }

    @Test
    void cameraAndBlockedRoute() {
        LayoutFrame open = VisibilityTest.scene(false, 0);
        LayoutState state = new LayoutState();
        LayoutEngine engine = new LayoutEngine();
        LayoutResult first = engine.solve(open, state);
        assertEquals(1, first.panels().size(), "unblocked screen relation visible");
        LayoutFrame blocked = VisibilityTest.scene(true, 0);
        LayoutResult second = engine.solve(at(blocked, 1, blocked.camera(), blocked.world(), blocked.sources()), state);
        for (LeaderLine line : second.leaders()) {
            if (!line.screen().isEmpty()) {
                PanelPlacement owner = second.panels().stream()
                        .filter(p -> p.visualId().equals(line.visualId()))
                        .findFirst()
                        .orElseThrow();
                assertTrue(
                        LeaderVisibility.clear(blocked, owner, line.screen(), line.world()),
                        "reused GUI must reroute or fold new wall obstruction");
            }
        }
        LayoutFrame base = PreviewScenes.scene("dense", 5, 1, .7, 0).frame();
        state = new LayoutState();
        engine = new LayoutEngine();
        engine.solve(base, state);
        long total = 0;
        int reused = 0;
        for (int i = 1; i <= 90; i++) {
            var camera = ViewCamera.lookAt(
                    new Vec3(2.4 + Math.sin(i * .03) * .5, 3.1, -1.1), new Vec3(0, 1.4, 6), 1000, 680, 62);
            LayoutFrame frame = at(base, i, camera, base.world(), base.sources());
            long start = System.nanoTime();
            LayoutResult result = engine.solve(frame, state);
            total += System.nanoTime() - start;
            assertTrue(
                    LayoutEngine.validate(frame, result).isEmpty(),
                    "camera-motion reuse maintains all hard constraints");
            if (engine.lastStats().expansions() == 0) reused++;
        }
        assertTrue(reused >= 75, "dense GUI does not repack every camera movement: " + reused);
    }
}
