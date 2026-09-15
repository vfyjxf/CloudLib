package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Depth;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LeaderLine;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelBasis;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Pose;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PreparedLayout;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Space;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Renderer bridge: keeps one {@link WorldUiPanel} per world-space
 * {@link PanelPlacement} in a {@link PreparedLayout}, synced per frame.
 * <p>
 * For each world panel the adapter produces:
 * <ul>
 *   <li>a {@link Placer} returning the solver's world pose as an exact
 *       {@link QuadBasis} ({@code worldWidth}×{@code worldHeight} world
 *       units, panel-local px mapped along the pose basis),</li>
 *   <li>a {@link Painter} from the host's {@link PainterFactory},</li>
 *   <li>a {@link LinesEmitter} drawing the leader's world polyline as
 *       {@code POSITION_COLOR} segments via {@link WorldLines},</li>
 *   <li>{@code depthTested} from the request material
 *       ({@link Depth.test} → occluded by terrain, {@link Depth.xray} →
 *       always on top) and {@code decal} for surface-mounted panels.</li>
 * </ul>
 * Leaders for visuals without a world panel (e.g. an omitted panel still
 * carrying a world path) render through one shared lines-only panel.
 * Panels whose visuals disappear are removed and closed.
 * <p>
 * Per-emitter depth override is not supported by the shared lines pass —
 * world leaders always take the OIT/depth path; {@link Depth.xray}
 * semantics are honoured exactly for screen-space strokes instead.
 */
public final class LayoutPanels implements AutoCloseable {

    /** Surface resolution: gui px per world unit of panel extent. */
    public static final int pixelsPerWorldUnit = 256;

    private static final int defaultLeaderColor = 0xFF4CC9F0;

    /** Host callback: the {@link Painter} for a world panel's surface. */
    @FunctionalInterface
    public interface PainterFactory {
        WorldUiPanel.@Nullable Painter create(PanelPlacement panel);
    }

    private final List<WorldUiPanel> renderPanels;
    private final Map<String, Entry> entries = new LinkedHashMap<>();
    private final List<LeaderLine> orphanLines = new ArrayList<>();
    private @Nullable PainterFactory painterFactory;
    private int leaderColor = defaultLeaderColor;
    /**
     * Exponential pose-smoothing rate per second; 0 = snap to the solver
     * pose every frame.
     */
    private double poseLerp;

    private @Nullable WorldUiPanel orphanPanel;

    /** Live per-visual state — placers and line emitters read this. */
    private static final class Entry {
        final WorldUiPanel panel;
        PanelPlacement placement;

        @Nullable
        LeaderLine leader;

        /** Smoothed pose for {@link LayoutPanels#poseLerp} — tracks {@code placement.pose()}. */
        @Nullable
        Pose smoothed;

        long lastNanos;

        Entry(WorldUiPanel panel, PanelPlacement placement) {
            this.panel = panel;
            this.placement = placement;
        }
    }

    public LayoutPanels() {
        this(WorldUiRenderer.get().panels());
    }

    /** Test seam — drive against an explicit panel list instead of the renderer. */
    public LayoutPanels(List<WorldUiPanel> renderPanels) {
        this.renderPanels = renderPanels;
    }

    /** Sets the painter factory applied to newly created world panels. */
    public LayoutPanels painterFactory(@Nullable PainterFactory factory) {
        this.painterFactory = factory;
        return this;
    }

    /** ARGB for world leader polylines (alpha honoured). */
    public LayoutPanels leaderColor(int argb) {
        this.leaderColor = argb;
        return this;
    }

    /**
     * Eases world panels between solver placements instead of snapping:
     * {@code perSecond} is the exponential smoothing rate (~14 settles in
     * a quarter second), 0 disables smoothing. Jumps over 2.5 world units
     * always snap — a re-seat that far reads as a teleport, not a slide.
     */
    public LayoutPanels poseSmoothing(double perSecond) {
        this.poseLerp = Math.max(0.0, perSecond);
        return this;
    }

    /** The live {@link WorldUiPanel} for a solver visualId, if placed. */
    public @Nullable WorldUiPanel panel(String visualId) {
        Entry entry = entries.get(visualId);
        return entry == null ? null : entry.panel;
    }

    /** visualId → panel for every currently-placed world visual. */
    public Map<String, WorldUiPanel> panels() {
        Map<String, WorldUiPanel> out = new LinkedHashMap<>();
        for (Map.Entry<String, Entry> e : entries.entrySet()) {
            out.put(e.getKey(), e.getValue().panel);
        }
        return out;
    }

    /**
     * Reconciles the panel pool with {@code prepared}: creates panels for
     * new world visuals, updates poses/leaders/material flags on existing
     * ones, removes and closes panels for vanished visuals.
     */
    public void sync(PreparedLayout prepared) {
        Map<String, PanelPlacement> desired = new LinkedHashMap<>();
        for (PanelPlacement panel : prepared.result().panels()) {
            if (panel.space() == Space.world && panel.pose() != null) {
                desired.put(panel.visualId(), panel);
            }
        }
        Map<String, LeaderLine> leaders = new HashMap<>();
        orphanLines.clear();
        for (LeaderLine leader : prepared.worldLeaders()) {
            if (desired.containsKey(leader.visualId())) {
                leaders.put(leader.visualId(), leader);
            } else {
                orphanLines.add(leader);
            }
        }

        var it = entries.entrySet().iterator();
        while (it.hasNext()) {
            Map.Entry<String, Entry> e = it.next();
            if (!desired.containsKey(e.getKey())) {
                remove(e.getValue().panel);
                it.remove();
            }
        }

        for (PanelPlacement placement : desired.values()) {
            Entry entry = entries.get(placement.visualId());
            if (entry == null) {
                entry = createEntry(placement);
                entries.put(placement.visualId(), entry);
                renderPanels.add(entry.panel);
            }
            entry.placement = placement;
            entry.leader = leaders.get(placement.visualId());
            applyMaterial(entry);
        }

        syncOrphanPanel();
    }

    /**
     * The exact world quad for a solver pose: {@code u} maps +px along
     * {@code basis.right}, {@code v} maps +px along {@code -basis.up}
     * (panel down, as the reader sees it — so {@code u×v} points away
     * from the viewer side, matching {@link QuadBasis}'s contract).
     */
    public static QuadBasis basisFor(Pose pose, double worldWidth, double worldHeight, int wPx, int hPx) {
        Vec3 u = pose.basis().right().scale(worldWidth / wPx);
        Vec3 v = pose.basis().up().scale(-worldHeight / hPx);
        Vec3 origin = pose.origin().subtract(u.scale(wPx * 0.5)).subtract(v.scale(hPx * 0.5));
        return QuadBasis.of(origin, u, v);
    }

    /** Surface pixel size for a world panel — proportional to world extent. */
    public static int[] surfaceSize(double worldWidth, double worldHeight) {
        return new int[] {
            Math.max(8, (int) Math.round(worldWidth * pixelsPerWorldUnit)),
            Math.max(8, (int) Math.round(worldHeight * pixelsPerWorldUnit)),
        };
    }

    @Override
    public void close() {
        for (Entry entry : entries.values()) {
            remove(entry.panel);
        }
        entries.clear();
        if (orphanPanel != null) {
            remove(orphanPanel);
            orphanPanel = null;
        }
    }

    /**
     * The pose the panel draws at this frame: the solver pose directly,
     * or an exponential-smoothed step toward it when {@link #poseLerp}
     * is on. Wall-clock dt keeps the easing frame-rate independent.
     */
    private Pose smoothedPose(Entry entry) {
        Pose target = entry.placement.pose();
        if (poseLerp <= 0.0 || target == null) {
            entry.smoothed = target;
            return target;
        }
        long now = System.nanoTime();
        Pose prev = entry.smoothed;
        if (prev == null) {
            entry.smoothed = target;
            entry.lastNanos = now;
            return target;
        }
        double dt = Math.min(0.25, (now - entry.lastNanos) / 1.0e9);
        entry.lastNanos = now;
        if (dt <= 0.0 || target.origin().distanceTo(prev.origin()) > 2.5) {
            entry.smoothed = target;
            return target;
        }
        double t = 1.0 - Math.exp(-poseLerp * dt);
        entry.smoothed = lerpPose(prev, target, t);
        return entry.smoothed;
    }

    private static Pose lerpPose(Pose a, Pose b, double t) {
        Vec3 right = a.basis().right().lerp(b.basis().right(), t);
        Vec3 up = a.basis().up().lerp(b.basis().up(), t);
        // a 180° flip lerps the axes through ~zero — snap the basis instead
        if (right.lengthSqr() < 0.01 || up.lengthSqr() < 0.01) {
            return new Pose(a.origin().lerp(b.origin(), t), b.basis());
        }
        return new Pose(a.origin().lerp(b.origin(), t), PanelBasis.of(right, up));
    }

    private void remove(WorldUiPanel panel) {
        if (renderPanels.remove(panel)) {
            panel.close();
        }
    }

    private Entry createEntry(PanelPlacement placement) {
        int[] size = surfaceSize(placement.worldWidth(), placement.worldHeight());
        Entry entry = new Entry(new WorldUiPanel(size[0], size[1]), placement);
        WorldUiPanel panel = entry.panel;
        panel.placer(frame -> {
            Pose pose = smoothedPose(entry);
            return basisFor(
                    pose, entry.placement.worldWidth(), entry.placement.worldHeight(), panel.width(), panel.height());
        });
        panel.lines((buffer, worldToView) -> emitLeader(buffer, worldToView, entry.leader));
        WorldUiPanel.Painter painter = painterFactory == null ? null : painterFactory.create(placement);
        if (painter != null) {
            panel.painter(painter);
        }
        return entry;
    }

    private void emitLeader(
            com.mojang.blaze3d.vertex.BufferBuilder buffer,
            org.joml.Matrix4f worldToView,
            @Nullable LeaderLine leader) {
        if (leader == null || leader.world().size() < 2) return;
        List<Vec3> pts = leader.world();
        for (int i = 1; i < pts.size(); i++) {
            WorldLines.line(buffer, worldToView, pts.get(i - 1), pts.get(i), leaderColor);
        }
    }

    private void applyMaterial(Entry entry) {
        PanelPlacement placement = entry.placement;
        boolean depthTest =
                placement.active() == null || placement.active().material().depth() == Depth.test;
        entry.panel.depthTested(depthTest);
        entry.panel.decal("mounted".equals(placement.slot()));
        entry.panel.tag(placement);
    }

    /**
     * Leaders whose visuals have no world quad still emit through one
     * shared panel — its quad is a sub-pixel placeholder at the camera
     * eye; only the lines draw.
     */
    private void syncOrphanPanel() {
        if (orphanLines.isEmpty()) {
            if (orphanPanel != null) {
                remove(orphanPanel);
                orphanPanel = null;
            }
            return;
        }
        if (orphanPanel == null) {
            WorldUiPanel panel = new WorldUiPanel(1, 1);
            panel.placer(frame ->
                    QuadBasis.axes(frame.camera().getPosition(), new Vec3(1, 0, 0), new Vec3(0, -1, 0), 1024, 1, 1));
            panel.lines((buffer, worldToView) -> {
                for (LeaderLine leader : orphanLines) {
                    emitLeader(buffer, worldToView, leader);
                }
            });
            orphanPanel = panel;
            renderPanels.add(panel);
        }
    }
}
