package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Config;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.FaceCamera;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiRect;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.HudRegion;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Interaction;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutFrame;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutResult;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutState;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LeaderMode;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Material;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Metrics;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.NearbyPosition;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.OrientationPolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelBasis;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelRequest;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Pose;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PositionPolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SampleContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SourceProvider;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Sources;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Space;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.ViewCamera;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.WorldObstacles;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Shared builders for solver tests — the frame/request factories the
 * source suite used, ported to the standalone API types.
 */
final class LayoutTestKit {

    private LayoutTestKit() {}

    static final String dim = "minecraft:overworld";
    static final Metrics metrics = Metrics.standard();
    static final Metrics small = new Metrics(1.2, .8, 120, 80, .3, 90, 35, 40, 25);
    static final Pose sourcePose = new Pose(new Vec3(0.0, 1.4, 6.0), PanelBasis.identity());

    /** LayoutTests-style request — standard metrics, machine family. */
    static PanelRequest request(
            String id,
            String sourceId,
            Space space,
            PositionPolicy position,
            OrientationPolicy orientation,
            Material material) {
        return new PanelRequest(
                id,
                sourceId,
                id,
                "machine",
                null,
                "devices",
                70,
                space,
                position,
                orientation,
                material,
                metrics,
                true,
                false,
                false,
                LeaderMode.auto,
                null);
    }

    /** SolverRegressionTests-style request — small metrics, test family. */
    static PanelRequest request(String id, GuiRect fixed, boolean compact, LeaderMode mode) {
        return new PanelRequest(
                id,
                "s",
                id,
                "test",
                null,
                "items",
                70,
                Space.screen,
                new NearbyPosition(new Vec3(0, 0, 0), true, .4),
                new FaceCamera(false),
                Material.virtual(),
                small,
                compact,
                false,
                false,
                mode,
                fixed);
    }

    /** Rebuilds a request with different flags — compact/merge/omit/leader. */
    static PanelRequest flags(
            PanelRequest request,
            boolean compactAllowed,
            boolean mergeAllowed,
            boolean omissionAllowed,
            String mergeKey,
            int priority,
            LeaderMode leaderMode) {
        return new PanelRequest(
                request.id(),
                request.sourceId(),
                request.title(),
                request.family(),
                mergeKey,
                request.overflowGroup(),
                priority,
                request.space(),
                request.position(),
                request.orientation(),
                request.material(),
                request.metrics(),
                compactAllowed,
                mergeAllowed,
                omissionAllowed,
                leaderMode,
                request.fixedScreen());
    }

    static LayoutFrame frame(
            long index,
            ViewCamera camera,
            List<PanelRequest> requests,
            Map<String, SourceProvider> sources,
            List<HudRegion> hud,
            WorldObstacles world,
            Interaction interaction) {
        return new LayoutFrame(
                new SampleContext(index, index / 60.0, 1.0 / 60.0, 0.5, "minecraft:overworld"),
                camera,
                LayoutMath.viewBasis(camera),
                hud,
                world,
                sources,
                requests,
                interaction,
                Config.defaults());
    }

    /** SolverRegressionTests-style frame — 60fps clock, custom config. */
    static LayoutFrame frame(
            long index, double width, double height, List<PanelRequest> requests, List<HudRegion> hud) {
        ViewCamera camera = ViewCamera.minecraft(new Vec3(0, 0, 0), 0, 0, width, height, 62);
        var source = Sources.point("s", dim, new Pose(new Vec3(0, 0, 6), PanelBasis.identity()));
        return new LayoutFrame(
                new SampleContext(index, index / 60., 1. / 60, .5, dim),
                camera,
                LayoutMath.viewBasis(camera),
                hud,
                new WorldObstacles(List.of()),
                Map.of("s", clock -> source),
                requests,
                Interaction.none(),
                new Config(10, 10, .93, 24, 12, 65, .25, 5, .015, 12, .95));
    }

    static ViewCamera camera() {
        return ViewCamera.minecraft(new Vec3(0.0, 1.7, 0.0), 0.0, 0.0, 1000.0, 700.0, 62.0);
    }

    static LayoutFrame plain(long index, List<PanelRequest> requests) {
        return frame(
                index,
                camera(),
                requests,
                Map.of("s", clock -> Sources.point("s", "minecraft:overworld", sourcePose)),
                List.of(),
                new WorldObstacles(List.of()),
                Interaction.none());
    }

    /** Solves and audits — every solve in the suite asserts postconditions. */
    static LayoutResult solve(LayoutFrame frame, LayoutState state) {
        LayoutResult result = new LayoutEngine().solve(frame, state);
        assertTrue(
                LayoutEngine.validate(frame, result).isEmpty(),
                "postconditions: " + LayoutEngine.validate(frame, result));
        return result;
    }

    static PanelPlacement sole(LayoutResult result) {
        assertEquals(1, result.panels().size(), "one panel expected: " + result.addresses());
        return result.panels().get(0);
    }

    static LayoutFrame movedWith(LayoutFrame frame, List<PanelRequest> requests) {
        return new LayoutFrame(
                frame.clock(),
                frame.camera(),
                frame.viewBasis(),
                frame.hud(),
                frame.world(),
                frame.sources(),
                requests,
                frame.interaction(),
                frame.config());
    }
}
