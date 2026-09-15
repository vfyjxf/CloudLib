package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.BoxObstacle;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Config;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.FaceCamera;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.FixedOrientation;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.FixedPosition;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.FollowPosition;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiRect;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.HudRegion;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Interaction;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutFrame;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LeaderMode;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Material;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Metrics;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.MountedPosition;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.NearbyPosition;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Obstacle;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.OrientationPolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelBasis;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelRequest;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Pose;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PositionPolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SampleContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SourceOrientation;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SourceProvider;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SourceSnapshot;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Sources;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Space;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Tracking;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.ViewCamera;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.WorldObstacles;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Deterministic scene fixtures for solver tests — a faithful port of the
 * source project's preview scene factory. {@link #scene} builds one of
 * twelve scenario variants (mixed spaces, source shapes, fixed/mounted
 * placement, density, merging, overflow, off-screen sources, tracking,
 * infeasible HUD) against a fixed camera orbit.
 */
final class PreviewScenes {

    private PreviewScenes() {}

    static final String dim = "minecraft:overworld";
    static final Metrics metrics = new Metrics(1.85, 1.18, 248.0, 164.0, 0.29, 174.0, 42.0, 78.0, 46.0);

    record Scene(String id, int variant, LayoutFrame frame) {}

    static Scene scene(String id, int variant, int phase, double angle, long tick) {
        Build build = new Build();
        double t = angle;
        ViewCamera camera = ViewCamera.lookAt(
                new Vec3(2.4 + phase * 0.55 + Math.sin(angle * 0.45) * 0.38, 3.1, -1.1),
                new Vec3(0.0, 1.4, 6.0),
                1000.0,
                680.0,
                62.0);
        build.block("floor", new Vec3(-9.0, -0.7, -2.0), new Vec3(9.0, 0.0, 15.0), "stone");
        build.block("back", new Vec3(-6.0, 0.0, 10.0), new Vec3(6.0, 4.0, 10.5), "brick");
        build.block("left-column", new Vec3(4.4, 0.0, 5.3), new Vec3(5.2, 3.6, 6.1), "stone");
        build.block("stairs0", new Vec3(-4.0, 0.0, 7.2), new Vec3(-2.7, 0.55, 9.9), "stone");
        build.block("stairs1", new Vec3(-3.8, 0.55, 8.0), new Vec3(-2.7, 1.1, 9.9), "stone");

        List<HudRegion> hud = new ArrayList<>();
        hud.add(HudRegion.rectangle("hotbar", new GuiRect(335.0, 602.0, 330.0, 64.0)));
        hud.add(HudRegion.rectangle("crosshair", new GuiRect(490.0, 330.0, 20.0, 20.0)));
        if (variant != 3 && variant != 4 && variant != 9) {
            hud.add(HudRegion.rectangle("quest", new GuiRect(804.0, 84.0, 182.0, 154.0)));
        }

        Interaction interaction = Interaction.none();
        if (variant == 0) {
            build.block("divider", new Vec3(-0.55, 0.0, 6.6), new Vec3(0.05, 2.7, 7.2), "stone");
            String[] titles = {"熔炉状态", "储能单元", "流体控制", "合成配方"};
            for (int i = 0; i < 4; i++) {
                String machine = "machine-" + i;
                build.box(machine, new Vec3((i - 1.5) * 1.7, 0.7, 5.8 + i % 2 * 1.3), new Vec3(0.45, 0.7, 0.45));
                build.ui(
                        "ui-" + i,
                        machine,
                        titles[i],
                        i == 3 ? Space.screen : Space.world,
                        new NearbyPosition(new Vec3(0.0, 0.6, 0.0), true, 0.6),
                        new FaceCamera(false),
                        i == 2 ? Material.virtual() : Material.physical(),
                        90 - i * 10,
                        true,
                        null,
                        "工坊设备",
                        LeaderMode.auto);
            }
        } else if (variant == 1) {
            String[] titles = {"空间点", "线段监测", "球形区域", "旋转盒体", "表面锚点", "复合来源"};
            for (int i = 0; i < 6; i++) {
                String sourceId = "source-" + i;
                Pose pose = new Pose(
                        new Vec3((i % 3 - 1) * 2.1, 1.1 + i / 3 * 0.8, 5.2 + i / 3 * 2.3),
                        PanelBasis.axisAngle(new Vec3(0.0, 1.0, 0.0), Math.PI));
                switch (i) {
                    case 0 -> build.source(Sources.point(sourceId, dim, pose));
                    case 1 ->
                        build.source(Sources.segment(
                                sourceId, dim, pose, new Vec3(-0.6, 0.0, 0.0), new Vec3(0.6, 0.5, 0.0)));
                    case 2 -> build.source(Sources.sphere(sourceId, dim, pose, 0.45));
                    case 3 -> build.box(sourceId, pose.origin(), new Vec3(0.4, 0.4, 0.4));
                    case 4 -> build.source(Sources.surface(sourceId, dim, pose, 1.0, 0.7));
                    default -> {
                        SourceSnapshot partA = Sources.point("part-a", dim, pose);
                        SourceSnapshot partB = Sources.sphere(
                                "part-b", dim, new Pose(pose.at(new Vec3(0.5, 0.0, 0.0)), pose.basis()), 0.25);
                        build.source(Sources.composite(sourceId, dim, pose, List.of(partA, partB)));
                    }
                }
                build.ui(
                        "type-" + i,
                        sourceId,
                        titles[i],
                        i % 3 == 0 ? Space.world : Space.screen,
                        new NearbyPosition(new Vec3(0.0, 0.5, 0.0), true, 0.4),
                        new FaceCamera(false),
                        Material.virtual(),
                        90 - i * 6,
                        true,
                        null,
                        "Source 类型",
                        LeaderMode.auto);
            }
        } else if (variant == 2) {
            String[] titles = {"固定位置 + 固定朝向", "固定位置 + 仅转偏航", "固定位置 + 面向相机"};
            for (int i = 0; i < 3; i++) {
                String fixed = "fixed-" + i;
                build.box(fixed, new Vec3((i - 1) * 2, 0.65, 6.5), new Vec3(0.4, 0.65, 0.4));
                Vec3 point = new Vec3((i - 1) * 2.45, 2.15, 5.7);
                OrientationPolicy orientation = i == 0
                        ? new FixedOrientation(PanelBasis.axisAngle(new Vec3(0.0, 1.0, 0.0), Math.PI))
                        : (i == 1 ? new FaceCamera(true) : new FaceCamera(false));
                build.ui(
                        "fixed-ui-" + i,
                        fixed,
                        titles[i],
                        Space.world,
                        new FixedPosition(point),
                        orientation,
                        Material.virtual(),
                        90 - i * 5,
                        true,
                        null,
                        "固定面板",
                        LeaderMode.auto);
            }
        } else if (variant == 3 || variant == 9) {
            camera = ViewCamera.lookAt(
                    new Vec3(2.8 + phase * 0.5, 4.3, -0.3), new Vec3(0.0, 1.2, 6.0), 1000.0, 680.0, 62.0);
            build.terrain.removeIf(o -> o.id().equals("back"));
            build.block("back", new Vec3(-7.0, 0.0, 8.5), new Vec3(6.0, 4.0, 9.0), "brick");
            PanelBasis wall = PanelBasis.axisAngle(new Vec3(0.0, 1.0, 0.0), Math.PI);
            PanelBasis ground = PanelBasis.of(new Vec3(-1.0, 0.0, 0.0), new Vec3(0.0, 0.0, 1.0));
            String[] titles = {"贴墙控制面", "倾斜工作面", "贴地投影面"};
            for (int i = 0; i < 3; i++) {
                PanelBasis basis = i == 0
                        ? wall
                        : (i == 1
                                ? wall.compose(PanelBasis.axisAngle(
                                        new Vec3(1.0, 0.0, 1.0), variant == 9 ? 0.55 + phase * 0.45 : 0.55))
                                : ground);
                Pose pose = new Pose(
                        new Vec3(
                                i == 0 ? -4.5 : (i == 1 ? 0.0 : 2.8),
                                i == 2 ? 0.025 : (i == 1 ? 2.7 : 1.85),
                                i == 0 ? 8.46 : (i == 1 ? 5.8 : 4.8)),
                        basis);
                String surface = "surface-" + i;
                build.source(Sources.surface(surface, dim, pose, 2.35, 1.8));
                build.ui(
                        "mounted-" + i,
                        surface,
                        titles[i],
                        Space.world,
                        new MountedPosition(new Vec3(0.0, 0.0, 0.0), 2.35, 1.8, 0.025),
                        new SourceOrientation(PanelBasis.identity()),
                        Material.physical(),
                        90 - i * 5,
                        true,
                        null,
                        "表面 UI",
                        LeaderMode.auto);
            }
        } else if (variant == 4) {
            camera = ViewCamera.lookAt(new Vec3(0.0, 2.8, -0.3), new Vec3(0.0, 1.5, 6.0), 1000.0, 680.0, 62.0);
            build.block("solid-wall", new Vec3(-3.6, 0.0, 5.8), new Vec3(3.6, 3.3, 6.35), "brick");
            for (int i = 0; i < 2; i++) {
                String device = "wall-device-" + i;
                double x = i == 0 ? 1.7 : -1.7;
                build.box(device, new Vec3(x, 0.5, 4.5), new Vec3(0.4, 0.5, 0.4));
                build.ui(
                        "wall-ui-" + i,
                        device,
                        i == 0 ? "实体：碰撞后收纳" : "虚体：允许穿透建筑",
                        Space.world,
                        new FixedPosition(new Vec3(x, 2.0, 6.05)),
                        new FixedOrientation(PanelBasis.axisAngle(new Vec3(0.0, 1.0, 0.0), Math.PI)),
                        i == 0 ? Material.physical() : Material.virtual(),
                        85,
                        true,
                        null,
                        "墙体冲突",
                        LeaderMode.auto);
            }
        } else if (variant == 5 || variant == 7 || variant == 11) {
            int count = variant == 5 ? 24 + phase * 8 : (variant == 7 ? 64 + phase * 16 : 36);
            hud.add(HudRegion.rectangle("chat", new GuiRect(14.0, 420.0, 300.0, 170.0)));
            for (int i = 0; i < 6; i++) {
                build.box("rack-" + i, new Vec3((i % 3 - 1) * 2.0, 0.7, 5.5 + i / 3 * 2.1), new Vec3(0.44, 0.7, 0.44));
            }
            for (int i = 0; i < count; i++) {
                build.ui(
                        "task-" + String.format("%03d", i),
                        "rack-" + i % 6,
                        "节点 " + String.format("%02d", i + 1),
                        Space.screen,
                        new NearbyPosition(new Vec3(0.0, 0.7, 0.0), true, 0.5),
                        new FaceCamera(false),
                        Material.virtual(),
                        i == 0 ? 98 : 30 + i % 55,
                        true,
                        null,
                        i % 3 == 0 ? "能源网络" : (i % 3 == 1 ? "物流网络" : "诊断信息"),
                        LeaderMode.auto);
            }
            if (variant == 7) {
                interaction =
                        new Interaction(phase == 2 ? "task-060" : null, null, true, phase == 1 ? "物流网络" : null, phase);
            }
            if (variant == 11) {
                if (phase == 0) {
                    hud.add(HudRegion.rectangle("expanded-map", new GuiRect(14.0, 14.0, 750.0, 564.0)));
                } else if (phase == 1) {
                    hud.add(HudRegion.rectangle("modal", new GuiRect(40.0, 14.0, 946.0, 570.0)));
                } else {
                    hud = List.of(HudRegion.rectangle("full-screen-HUD", new GuiRect(0.0, 0.0, 1000.0, 680.0)));
                }
            }
        } else if (variant == 6) {
            for (int i = 0; i < 3; i++) {
                String network = "network-" + i;
                build.box(network, new Vec3((i - 1) * 2.1, 0.8, 6.0), new Vec3(0.48, 0.8, 0.48));
                String[] tabs = {"储能", "吞吐", "历史", "配置"};
                for (int tab = 0; tab < 4; tab++) {
                    build.ui(
                            "network-" + i + "-tab-" + tab,
                            network,
                            tabs[tab] + " / " + (i + 1),
                            phase == 1 ? Space.world : Space.screen,
                            new NearbyPosition(new Vec3(0.0, 0.5, 0.0), true, 0.55),
                            new FaceCamera(false),
                            Material.virtual(),
                            85 - i * 5,
                            true,
                            "network",
                            "能源网络",
                            LeaderMode.auto);
                }
            }
            interaction = new Interaction(phase == 2 ? "network-1-tab-2" : null, "network-0-tab-2", false, null, 0);
        } else if (variant == 8) {
            String[] titles = {"东侧目标", "西侧目标", "身后目标"};
            for (int i = 0; i < 3; i++) {
                String remote = "remote-" + i;
                Pose pose = new Pose(
                        new Vec3(i == 0 ? 12.0 : (i == 1 ? -14.0 : 0.0), 1.4, i == 2 ? -8.0 : 6.0),
                        PanelBasis.identity());
                build.source(Sources.point(remote, dim, pose));
                build.ui(
                        "remote-ui-" + i,
                        remote,
                        titles[i],
                        Space.screen,
                        new FollowPosition(new Vec3(0.0, 0.0, 0.0)),
                        new FaceCamera(false),
                        Material.virtual(),
                        90 - i * 10,
                        true,
                        null,
                        "远端目标",
                        LeaderMode.auto);
            }
        } else if (variant == 10) {
            String[] titles = {"跟随位置 + 面向相机", "跟随局部坐标", "实体 UI → 屏幕", "智能附近布局"};
            for (int i = 0; i < 4; i++) {
                String entity = "entity-" + i;
                int index = i;
                Pose pose = new Pose(
                        new Vec3((i - 1.5) * 1.7 + Math.sin(t + i) * 0.4, 0.7, 5.3 + Math.cos(t + i) * 0.5),
                        PanelBasis.axisAngle(new Vec3(0.0, 1.0, 0.0), t * 0.4 + i * 0.2));
                build.source(Sources.box(entity, dim, pose, new Vec3(0.3, 0.7, 0.3)));
                SourceProvider provider = Tracking.interpolated(
                        () -> new Tracking.TickState(
                                entity,
                                dim,
                                0L,
                                true,
                                new Pose(
                                        pose.origin().subtract(new Vec3(0.035, 0.0, 0.0)),
                                        PanelBasis.axisAngle(new Vec3(0.0, 1.0, 0.0), t * 0.4 + index * 0.2 - 0.02)),
                                pose,
                                new Vec3(0.7, 0.0, 0.0),
                                false),
                        (snapshotId, dimension, at) -> Sources.box(snapshotId, dimension, at, new Vec3(0.3, 0.7, 0.3)),
                        8.0);
                build.sources.put(entity, provider);
                build.ui(
                        "entity-ui-" + i,
                        entity,
                        titles[i],
                        i == 2 ? Space.screen : Space.world,
                        i == 3
                                ? new NearbyPosition(new Vec3(0.0, 0.5, 0.0), true, 0.4)
                                : new FollowPosition(new Vec3(0.0, 1.5, 0.0)),
                        i == 1
                                ? new SourceOrientation(PanelBasis.axisAngle(new Vec3(0.0, 1.0, 0.0), Math.PI))
                                : new FaceCamera(false),
                        Material.virtual(),
                        95 - i * 10,
                        true,
                        null,
                        "实体追踪",
                        LeaderMode.auto);
            }
        }

        LayoutFrame frame = new LayoutFrame(
                new SampleContext(tick, tick / 30.0, 1.0 / 30.0, 0.65, dim),
                camera,
                LayoutMath.viewBasis(camera),
                hud,
                new WorldObstacles(build.terrain),
                build.sources,
                build.requests,
                interaction,
                Config.defaults());
        return new Scene(id, variant, frame);
    }

    /** Scene-builder: sources, requests and shared terrain obstacles. */
    static final class Build {
        final Map<String, SourceProvider> sources = new LinkedHashMap<>();
        final List<PanelRequest> requests = new ArrayList<>();
        final List<Obstacle> terrain = new ArrayList<>();

        void source(SourceSnapshot snapshot) {
            sources.put(snapshot.id(), clock -> snapshot);
        }

        /** A machine body — a box source plus its matching terrain obstacle. */
        void box(String id, Vec3 center, Vec3 half) {
            SourceSnapshot snapshot = Sources.box(id, dim, new Pose(center, PanelBasis.identity()), half);
            snapshot = new SourceSnapshot(
                    snapshot.id(),
                    snapshot.dimension(),
                    snapshot.generation(),
                    true,
                    snapshot.frame(),
                    snapshot.velocity(),
                    snapshot.attachments(),
                    snapshot.hulls(),
                    Set.of("body:" + id));
            source(snapshot);
            terrain.add(new Obstacle(
                    "body:" + id,
                    new BoxObstacle(new AABB(center.subtract(half), center.add(half))),
                    true,
                    true,
                    "machine"));
        }

        void block(String id, Vec3 min, Vec3 max, String material) {
            terrain.add(new Obstacle(id, new BoxObstacle(new AABB(min, max)), true, true, material));
        }

        void ui(
                String id,
                String sourceId,
                String title,
                Space space,
                PositionPolicy position,
                OrientationPolicy orientation,
                Material material,
                int priority,
                boolean compactAllowed,
                String mergeKey,
                String overflowGroup,
                LeaderMode leaderMode) {
            requests.add(new PanelRequest(
                    id,
                    sourceId,
                    title,
                    mergeKey == null ? "device" : "energy",
                    mergeKey,
                    overflowGroup,
                    priority,
                    space,
                    position,
                    orientation,
                    material,
                    id.equals("mounted-0")
                            ? new Metrics(2.25, 1.42, 248.0, 164.0, 0.29, 174.0, 42.0, 78.0, 46.0)
                            : metrics,
                    compactAllowed,
                    mergeKey != null,
                    false,
                    leaderMode,
                    null));
        }
    }
}
