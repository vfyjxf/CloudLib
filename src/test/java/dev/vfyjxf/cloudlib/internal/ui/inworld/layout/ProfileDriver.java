package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.*;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;

/**
 * Profiling driver — loops the solver over the migrated scene fixtures
 * plus an obstacle-dense scenario so JProfiler sees a representative
 * mix: joint placement search, AUTO/REQUIRED routing, visibility SAT
 * and obstacle checks. Run it as a plain main; stop with a duration
 * budget printed to stdout.
 */
public final class ProfileDriver {

    private ProfileDriver() {}

    public static void main(String[] args) {
        long millis = args.length > 0 ? Long.parseLong(args[0]) : 120_000;
        long deadline = System.nanoTime() + millis * 1_000_000L;
        LayoutEngine engine = new LayoutEngine();
        LayoutState state = new LayoutState();
        // Prebuild a pool of frames so solve-only time is measured; the
        // clock still advances so debounce/caches see a real timeline.
        List<LayoutFrame> pool = new ArrayList<>();
        for (int variant = 0; variant < 12; variant++) {
            pool.add(PreviewScenes.scene("audit", variant, 0, 0.0, 0).frame());
        }
        WorldObstacles world = denseObstacles();
        List<LayoutFrame> dense = new ArrayList<>();
        for (int i = 0; i < 4; i++) dense.add(denseWorld(world, i));
        if (args.length > 1 && args[1].equals("split")) {
            List<LayoutFrame> all = new ArrayList<>(pool);
            all.addAll(dense);
            // JIT warmup across all code paths before measuring.
            for (LayoutFrame t : all) {
                for (int s = 0; s < 300; s++) engine.solve(reclock(t, s), state);
            }
            for (int i = 0; i < all.size(); i++) {
                LayoutFrame template = all.get(i);
                long s = 0, n = 0;
                // Fresh state → first solve pays the full search; later
                // solves ride temporal memory. Both numbers matter: the
                // first is a transition frame, the rest are steady state.
                LayoutState fresh = new LayoutState();
                long t0 = System.nanoTime();
                engine.solve(reclock(template, 0), fresh);
                double first = (System.nanoTime() - t0) / 1.0e6;
                long end = System.nanoTime() + millis * 200_000L;
                while (System.nanoTime() < end) {
                    long t = System.nanoTime();
                    engine.solve(reclock(template, ++s), fresh);
                    n += System.nanoTime() - t;
                }
                System.out.printf(
                        "scene %-2d panels=%-2d  first %7.2f ms  steady %.2f ms%n",
                        i, template.requests().size(), first, n / 1.0e6 / s);
            }
            return;
        }
        if (args.length > 1 && args[1].equals("dynamic")) {
            List<LayoutFrame> all = new ArrayList<>();
            if (args.length > 2 && args[2].equals("dense")) {
                all.addAll(dense);
            } else {
                all.addAll(pool);
                all.addAll(dense);
            }
            for (LayoutFrame t : all) {
                for (int s = 0; s < 300; s++) engine.solve(reframe(t, s), state);
            }
            for (int i = 0; i < all.size(); i++) {
                LayoutFrame template = all.get(i);
                long s = 0, n = 0;
                LayoutState fresh = new LayoutState();
                long end = System.nanoTime() + millis * 300_000L;
                while (System.nanoTime() < end) {
                    long t = System.nanoTime();
                    engine.solve(reframe(template, ++s), fresh);
                    n += System.nanoTime() - t;
                }
                System.out.printf(
                        "scene %-2d panels=%-2d  dynamic %.2f ms/solve (%d solves)%n",
                        i, template.requests().size(), n / 1.0e6 / s, s);
            }
            return;
        }
        long solves = 0;
        long solveNanos = 0;
        long started = System.nanoTime();
        int phase = 0;
        while (System.nanoTime() < deadline) {
            for (LayoutFrame template : pool) {
                LayoutFrame frame = reclock(template, phase);
                long t0 = System.nanoTime();
                engine.solve(frame, state);
                solveNanos += System.nanoTime() - t0;
                solves++;
            }
            for (LayoutFrame template : dense) {
                LayoutFrame frame = reclock(template, phase);
                long t0 = System.nanoTime();
                engine.solve(frame, state);
                solveNanos += System.nanoTime() - t0;
                solves++;
            }
            phase++;
        }
        double seconds = (System.nanoTime() - started) / 1.0e9;
        System.out.printf(
                "%,d solves in %.1fs (%.2f ms/solve, engine only %.2f ms)%n",
                solves, seconds, seconds * 1000 / solves, solveNanos / 1.0e6 / solves);
    }

    /**
     * A dynamic frame: the camera orbits a pivot 8 blocks ahead of its
     * eye — every solve sees moved geometry, so the stable-frame reuse
     * key never hits. Models continuous head motion at ~60fps.
     */
    private static LayoutFrame reframe(LayoutFrame f, long tick) {
        Projector camera = f.camera();
        if (camera instanceof ViewCamera view) {
            double angle = tick * 0.06;
            Vec3 pivot = view.eye().add(view.forward().scale(8.0));
            Vec3 eye = view.eye()
                    .add(view.right().scale(Math.cos(angle) * 0.35))
                    .add(view.up().scale(Math.sin(angle) * 0.35));
            camera = ViewCamera.lookAt(eye, pivot, view.width(), view.height(), view.verticalFovDegrees());
        }
        return new LayoutFrame(
                new SampleContext(tick, tick * 0.05, 0.05, 0, f.clock().dimension()),
                camera,
                camera instanceof ViewCamera view ? LayoutMath.viewBasis(view) : f.viewBasis(),
                f.hud(),
                f.world(),
                f.sources(),
                f.requests(),
                f.interaction(),
                f.config());
    }

    private static LayoutFrame reclock(LayoutFrame f, long tick) {
        return new LayoutFrame(
                new SampleContext(tick, tick * 0.05, 0.05, 0, f.clock().dimension()),
                f.camera(),
                f.viewBasis(),
                f.hud(),
                f.world(),
                f.sources(),
                f.requests(),
                f.interaction(),
                f.config());
    }

    private static WorldObstacles denseObstacles() {
        List<Obstacle> obstacles = new ArrayList<>();
        for (int x = -8; x <= 8; x++) {
            for (int y = 0; y <= 4; y++) {
                for (int z = 0; z <= 4; z++) {
                    if ((x + y + z) % 3 == 0) continue;
                    obstacles.add(new Obstacle(
                            "b" + x + "," + y + "," + z,
                            new BoxObstacle(new AABB(new Vec3(x, y, z), new Vec3(x + 1, y + 1, z + 1))),
                            true,
                            true,
                            null));
                }
            }
        }
        return new WorldObstacles(obstacles);
    }

    /** An obstacle-dense frame: ~400 solid boxes plus mixed tags. */
    private static LayoutFrame denseWorld(WorldObstacles world, int tick) {
        Vec3 eye = new Vec3(5.5, 3.0, 11.5);
        Vec3 anchor = new Vec3(0.5, 2.6, 5.5);
        List<PanelRequest> requests = new ArrayList<>();
        Map<String, SourceProvider> sources = new java.util.LinkedHashMap<>();
        for (int i = 0; i < 6; i++) {
            String id = "t" + i;
            Vec3 a = anchor.add(i * 1.1, (i % 3) * 0.4, (i % 2) * 0.7);
            sources.put(id, c -> Sources.point(id, "minecraft:overworld", new Pose(a, PanelBasis.identity())));
            requests.add(new PanelRequest(
                    id,
                    id,
                    id,
                    "demo",
                    null,
                    "items",
                    70,
                    Space.world,
                    new NearbyPosition(Vec3.ZERO, false, 0.55 + i * 0.1),
                    new FaceCamera(false),
                    Material.physical(),
                    new Metrics(1.0, 0.5, 90, 45, 0.25, 63, 20, 24, 16),
                    false,
                    false,
                    false,
                    i % 2 == 0 ? LeaderMode.auto : LeaderMode.required,
                    null));
        }
        ViewCamera view = ViewCamera.lookAt(eye, anchor, 640, 480, 62);
        return new LayoutFrame(
                new SampleContext(tick, tick * 0.05, 0.05, 0, "minecraft:overworld"),
                view,
                LayoutMath.viewBasis(view),
                List.of(),
                world,
                sources,
                requests,
                Interaction.none(),
                Config.defaults());
    }
}
