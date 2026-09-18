package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.vfyjxf.cloudlib.internal.ui.inworld.RepaintGate;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;

/**
 * The in-world UI render pipeline (v2, sorted-translucency variant).
 * <p>
 * Every {@link WorldUiPanel} follows the same three-stage flow:
 * <ol>
 *   <li><b>place</b> — each panel's {@code Placer} resolves its world quad
 *       from the live camera;</li>
 *   <li><b>surface</b> — each visible panel repaints its offscreen FBO
 *       (premultiplied RGBA, supersampled); the pass is fully
 *       state-isolated;</li>
 *   <li><b>world pass</b> — panel quads and companion line geometry draw
 *       straight into the scene target, sorted far → near with the
 *       premultiplied blend func.</li>
 * </ol>
 * There is deliberately <b>no OIT accumulation pass</b>: the WBOIT approach
 * borrowed the scene depth texture and needed per-attachment blend funcs that
 * are not portable across the supported GL floor — this variant instead owns
 * its translucency outright and runs once at
 * {@link RenderLevelStageEvent.Stage#AFTER_LEVEL}, after the whole level —
 * including clouds, weather and the Fabulous! per-layer merge — has resolved
 * into the main target. The UI simply draws over it: per-pixel interop with
 * the world's translucent objects is not attempted. {@code AFTER_WEATHER}
 * is not an option — under Fabulous! it fires while {@code weatherTarget}
 * is bound, which is cleared per frame without a scene-depth copy, so the
 * pass would lose all world occlusion there.
 * <p>
 * Translucency <em>inside</em> the UI is still correct: panels sort far →
 * near by quad center and composite premultiplied, so overlapping
 * translucent surfaces stack like ordinary GUI layers. One caveat vs the
 * old OIT pass: sorting is per-panel — two quads that physically
 * <em>interpenetrate</em> (a fixed quad slicing through a billboard) share
 * one layer of the overlap, and the seam shifts with viewpoint; the
 * hysteresis below keeps it stable instead of flickering frame to frame.
 * The shared scene depth handles world-vs-UI occlusion — an opaque wall in
 * front still hides the panel; nothing in the pass writes depth.
 * <p>
 * Panels are added/removed through {@link #panels()}; untethered line
 * geometry (scan frames, drag trails) registers on {@link #lineEmitters()}.
 * The renderer registers itself on the NeoForge event bus on first
 * {@link #get()}.
 */
public final class WorldUiRenderer {

    private static final Logger logger = LoggerFactory.getLogger("CloudLib WorldUiRenderer");

    private static @Nullable WorldUiRenderer instance;

    /** The shared renderer — registers its level-stage hook on first access. */
    public static WorldUiRenderer get() {
        if (instance == null) {
            instance = new WorldUiRenderer();
            // LOW: host passes that resolve per-frame panel geometry (a host's
            // scan frames + runtime state, e.g. an inworld manager driving the
            // layout engine) run first
            NeoForge.EVENT_BUS.addListener(EventPriority.LOW, instance::onLevelStage);
        }
        return instance;
    }

    private final Minecraft mc = Minecraft.getInstance();
    private final List<WorldUiPanel> panels = new ArrayList<>();
    private final List<WorldUiPanel> visible = new ArrayList<>();
    /** Frame-level line emitters — not tied to a panel quad's visibility. */
    private final List<WorldUiPanel.LinesEmitter> lineEmitters = new ArrayList<>();

    private Matrix4f worldToView = new Matrix4f();

    /** The quad each panel's placer resolved this frame — renderer-side state, null/absent when hidden. */
    private final Map<WorldUiPanel, QuadBasis> bases = new IdentityHashMap<>();

    private WorldUiRenderer() {}

    /** The live panel list — panels are depth-sorted against each other per frame. */
    public List<WorldUiPanel> panels() {
        return panels;
    }

    /**
     * Emitters for line geometry that belongs to no single panel — called once
     * per frame with a shared DEBUG_LINES buffer while the world pass runs.
     */
    public List<WorldUiPanel.LinesEmitter> lineEmitters() {
        return lineEmitters;
    }

    public void addPanel(WorldUiPanel panel) {
        panels.add(panel);
        registration.put(panel, registrationSeq++);
    }

    public void removePanel(WorldUiPanel panel) {
        supersampleControllers.remove(panel);
        grantStabilizers.remove(panel);
        repaintGates.remove(panel);
        lastOrder.remove(panel);
        registration.remove(panel);
        bases.remove(panel);
        if (panels.remove(panel)) panel.close();
    }

    public void clearPanels() {
        for (WorldUiPanel panel : panels) panel.close();
        panels.clear();
        supersampleControllers.clear();
        grantStabilizers.clear();
        repaintGates.clear();
        lastOrder.clear();
        registration.clear();
        bases.clear();
    }

    // region level stage

    private void onLevelStage(RenderLevelStageEvent event) {
        // last stage — the whole level (including the Fabulous! translucent
        // merge) is already in the main target, so sorted translucent draws
        // composite correctly over everything behind them
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (mc.level == null || mc.player == null || (panels.isEmpty() && lineEmitters.isEmpty())) return;
        if (mc.options.hideGui) return;

        Vec3 cameraPos = event.getCamera().getPosition();
        float pt = event.getPartialTick().getGameTimeDeltaPartialTick(true);
        // the level modelView is camera ROTATION only — compose in the
        // −cameraPos translation to get world → view
        worldToView = new Matrix4f(event.getModelViewMatrix())
                .translate((float) -cameraPos.x, (float) -cameraPos.y, (float) -cameraPos.z);
        Matrix4f viewToClip = new Matrix4f(event.getProjectionMatrix());

        var frame = new WorldUiPanel.Frame(
                event.getCamera(),
                cameraPos,
                worldToView,
                viewToClip,
                pt,
                mc.level,
                mc.getWindow().getWidth(),
                mc.getWindow().getHeight());

        // 1. place — hidden panels (null basis) skip everything downstream
        visible.clear();
        for (WorldUiPanel panel : panels) {
            if (!panel.visible()) continue;
            QuadBasis basis = panel.placer().place(frame);
            if (basis != null) {
                bases.put(panel, basis);
                visible.add(panel);
            } else {
                bases.remove(panel);
            }
        }
        RenderStats.panelCount(visible.size());
        if (visible.isEmpty() && lineEmitters.isEmpty()) return;

        // 2. surfaces — every panel repaints its FBO up-front; no surface
        //    work interleaves with the world pass below. Supersampling adapts
        //    to the quad's projected size (≈1 texel per framebuffer pixel at
        //    reading distance instead of a fixed factor the world pass then
        //    LINEAR-magnifies 2–14×), quantized through per-panel hysteresis
        //    so the FBO doesn't resize every frame, then capped by a global
        //    texel budget that keeps large panels sharp and steps smaller ones
        //    down when the frame overspends
        renderSurfaces(frame, pt);

        // 3. world pass — sorted translucency: far → near, premult blend,
        //    depth-tested against the completed scene, no depth writes.
        //    The sort carries last-frame hysteresis so panels whose centers
        //    are near-equidistant don't flip order every other frame.
        var mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.identity();
        RenderSystem.applyModelViewMatrix();
        try {
            sortPanels(cameraPos);
            for (WorldUiPanel panel : visible) {
                drawPanelQuad(panel);
            }
            drawLines();
            RenderSystem.depthMask(true);
        } finally {
            mv.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    private double centerDist(WorldUiPanel panel, Vec3 cameraPos) {
        QuadBasis q = bases.get(panel);
        if (q == null) return 0;
        return q.center(panel.width(), panel.height()).distanceTo(cameraPos);
    }

    // region adaptive supersampling

    /**
     * Per-panel hysteresis for the adaptive supersample factor — a controller
     * only exists for panels that have been visible at least once and is
     * dropped with the panel (see {@link #removePanel}/{@link #clearPanels}).
     */
    private final Map<WorldUiPanel, SupersampleController> supersampleControllers = new IdentityHashMap<>();

    /**
     * Second-stage hysteresis on the budget's grant: {@link Supersampling#allocate}
     * can step a panel's grant up and down in long segments as other panels
     * toggle visibility or screen areas reorder — the same stability window
     * on the grant keeps those segment flips from resizing the texture.
     */
    private final Map<WorldUiPanel, SupersampleController> grantStabilizers = new IdentityHashMap<>();

    /**
     * Per-panel dirty-check cache (the {@link RepaintGate} protocol): a panel
     * whose {@link WorldUiPanel#contentVersion(LongSupplier) content version},
     * size and granted supersample are all unchanged skips its surface
     * repaint — the world quad keeps sampling the existing texture. Panels
     * without a version stay always-dirty; the gate is dropped with the panel
     * (see {@link #removePanel}/{@link #clearPanels}).
     */
    private final Map<WorldUiPanel, RepaintGate> repaintGates = new IdentityHashMap<>();

    /**
     * Repaints every visible panel's surface at this frame's supersample
     * factor: project the resolved quad, derive the desired factor (the
     * profile's {@code supersample} is the floor, an explicit user value a
     * lower bound), stabilize it through the panel's controller, then fit all
     * panels under the global texel budget, then stabilize the budget's
     * grant the same way. Panels whose projection is degenerate this frame
     * (edge-on, corner behind the camera) produce no observation —
     * {@link Supersampling#desired} answers 0 and both controllers hold,
     * so a transiently degenerate projection never resizes anything.
     * <p>
     * The actual repaint goes through the panel's {@link RepaintGate}: a
     * panel whose content version, size and granted factor are all unchanged
     * keeps last frame's texture (no painter run, no FBO bind, no mips — the
     * world quad samples the existing surface). Panels without a version
     * repaint every frame, exactly as before.
     */
    private void renderSurfaces(WorldUiPanel.Frame frame, float pt) {
        int count = visible.size();
        double[] projW = new double[count];
        double[] projH = new double[count];
        List<Supersampling.Request> requests = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            WorldUiPanel panel = visible.get(i);
            Supersampling.ProjectedSize proj = Supersampling.projectQuad(
                    worldToView,
                    frame.viewToClip(),
                    bases.get(panel),
                    panel.width(),
                    panel.height(),
                    frame.viewportW(),
                    frame.viewportH());
            int desired =
                    Supersampling.desired(proj == null ? 0 : proj.heightPx(), panel.height(), panel.supersample());
            int stable = supersampleControllers
                    .computeIfAbsent(panel, p -> new SupersampleController())
                    .observe(desired);
            projW[i] = proj == null ? 0 : proj.widthPx();
            projH[i] = proj == null ? 0 : proj.heightPx();
            requests.add(new Supersampling.Request(
                    panel.width(), panel.height(), stable, panel.supersample(), proj == null ? 0 : proj.areaPx()));
        }
        int[] granted = Supersampling.allocate(requests, Supersampling.defaultTexelBudget);
        for (int i = 0; i < count; i++) {
            WorldUiPanel panel = visible.get(i);
            int grantedStable = grantStabilizers
                    .computeIfAbsent(panel, p -> new SupersampleController())
                    .observe(granted[i]);
            RepaintGate gate = repaintGates.computeIfAbsent(panel, p -> new RepaintGate());
            int ss = grantedStable;
            double projectedW = projW[i];
            double projectedH = projH[i];
            boolean painted = gate.render(panel, ss, () -> panel.renderSurface(ss, projectedW, projectedH, pt));
            if (!painted) {
                // the surface texture stays active from the last paint — the
                // texel budget in use still counts it
                RenderStats.surfaceCached((long) panel.width() * ss * panel.height() * ss);
            }
        }
    }

    // endregion

    /** Last frame's draw order — the hysteresis tie-break for near-ties. */
    private final Map<WorldUiPanel, Integer> lastOrder = new IdentityHashMap<>();

    /** Stable registration order — the final tie-break that keeps the sort transitive. */
    private final Map<WorldUiPanel, Integer> registration = new IdentityHashMap<>();

    private int registrationSeq;

    /** Within this distance (blocks), two panels count as tied. */
    private static final double tieEps = 0.05;

    /**
     * Sorts {@link #visible} far → near by quad-center distance. Distance
     * alone would flip near-equidistant panels every time the camera drifts
     * past the tie boundary, but a comparator that breaks near-ties by last
     * frame's order is not transitive (a~b and b~c by stickiness, a~c by
     * distance) and TimSort throws a comparison-contract violation once
     * enough panels are up. So near-ties are first chained into clusters —
     * after the distance sort, each panel within {@link #tieEps} of its
     * sorted neighbor joins its cluster — and the final order is the fully
     * transitive chain (cluster, last frame's order, registration): panels
     * inside a cluster keep their previous relative order across frames,
     * and every level of the chain is a total order.
     */
    private void sortPanels(Vec3 cameraPos) {
        Map<WorldUiPanel, Double> dist = new IdentityHashMap<>(visible.size() * 2);
        for (WorldUiPanel panel : visible) dist.put(panel, centerDist(panel, cameraPos));
        visible.sort(Comparator.comparingDouble(dist::get).thenComparingInt(registration::get));
        // chain neighbors within tieEps (cluster ids stay in distance order)
        Map<WorldUiPanel, Integer> cluster = new IdentityHashMap<>(visible.size() * 2);
        int clusterId = 0;
        double previous = Double.NEGATIVE_INFINITY;
        for (WorldUiPanel panel : visible) {
            double d = dist.get(panel);
            if (d - previous >= tieEps) clusterId++;
            cluster.put(panel, clusterId);
            previous = d;
        }
        // within a cluster: last frame's order first (newcomers last), then registration
        visible.sort(Comparator.comparingInt(cluster::get)
                .thenComparingInt(p -> lastOrder.getOrDefault(p, Integer.MAX_VALUE))
                .thenComparingInt(registration::get));
        lastOrder.clear();
        for (int i = 0; i < visible.size(); i++) lastOrder.put(visible.get(i), i);
    }

    // endregion

    // region quad draws

    /**
     * Blits a panel's surface texture onto its world quad. The surface stores
     * premultiplied texels, so the composite uses the premult func —
     * {@code SRC_ALPHA} would square alpha and wash it out.
     */
    private void drawPanelQuad(WorldUiPanel panel) {
        int tex = panel.surfaceTextureId();
        QuadBasis q = bases.get(panel);
        if (tex == 0 || q == null) return;

        BufferBuilder buffer =
                Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        emitQuad(buffer, q, panel.width(), panel.height(), panel.opacity());
        MeshData mesh = buffer.build();
        if (mesh == null) return;
        RenderStats.quadDrawn();

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        // no depth writes — translucent quads would cull whatever blends
        // behind them, and near-coplanar quads would z-fight per pixel
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        if (!panel.depthTested()) RenderSystem.disableDepthTest();
        if (panel.decal()) {
            RenderSystem.enablePolygonOffset();
            RenderSystem.polygonOffset(-1f, -4f);
        }
        RenderSystem.setShaderTexture(0, tex);
        BufferUploader.drawWithShader(mesh);
        if (panel.decal()) {
            RenderSystem.polygonOffset(0f, 0f);
            RenderSystem.disablePolygonOffset();
        }
        if (!panel.depthTested()) RenderSystem.enableDepthTest();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableCull();
    }

    /** Emits the four quad vertices (positions baked through worldToView). */
    private void emitQuad(BufferBuilder buffer, QuadBasis q, int wPx, int hPx, float alpha) {
        Matrix4f mat = worldToView;
        Vec3 o = q.origin();
        Vec3 u = q.u();
        Vec3 v = q.v();
        Vec3 p01 = o.add(v.scale(hPx));
        Vec3 p11 = o.add(u.scale(wPx)).add(v.scale(hPx));
        Vec3 p10 = o.add(u.scale(wPx));

        // the surface texture is premultiplied — a global-opacity tint must
        // scale rgb by the same factor it applies to alpha, so the vertex
        // color carries `alpha` on all four channels (tex * vColor stays
        // premult-consistent)
        buffer.addVertex(mat, (float) o.x, (float) o.y, (float) o.z).setUv(0, 1).setColor(alpha, alpha, alpha, alpha);
        buffer.addVertex(mat, (float) p01.x, (float) p01.y, (float) p01.z)
                .setUv(0, 0)
                .setColor(alpha, alpha, alpha, alpha);
        buffer.addVertex(mat, (float) p11.x, (float) p11.y, (float) p11.z)
                .setUv(1, 0)
                .setColor(alpha, alpha, alpha, alpha);
        buffer.addVertex(mat, (float) p10.x, (float) p10.y, (float) p10.z)
                .setUv(1, 1)
                .setColor(alpha, alpha, alpha, alpha);
    }

    /** Companion line geometry — one shared DEBUG_LINES mesh for all panels and global emitters. */
    private void drawLines() {
        BufferBuilder buffer =
                Tesselator.getInstance().begin(VertexFormat.Mode.DEBUG_LINES, DefaultVertexFormat.POSITION_COLOR);
        boolean any = false;
        for (WorldUiPanel panel : visible) {
            var lines = panel.lines();
            if (lines != null) {
                lines.emit(buffer, worldToView);
                any = true;
            }
        }
        for (WorldUiPanel.LinesEmitter emitter : lineEmitters) {
            emitter.emit(buffer, worldToView);
            any = true;
        }
        if (!any) return;
        MeshData mesh = buffer.build();
        if (mesh == null) return;
        RenderStats.lineDrawn();
        RenderSystem.setShader(GameRenderer::getPositionColorShader);
        RenderSystem.enableBlend();
        RenderSystem.defaultBlendFunc();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.disableCull();
        BufferUploader.drawWithShader(mesh);
        RenderSystem.depthMask(true);
        RenderSystem.enableCull();
    }

    // endregion
}
