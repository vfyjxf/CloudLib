package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.client.event.RenderLevelStageEvent;
import net.neoforged.neoforge.common.NeoForge;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

import java.util.ArrayList;
import java.util.List;

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
 * {@link RenderLevelStageEvent.Stage#AFTER_LEVEL}, after the level (including
 * the Fabulous! per-layer merge) has fully resolved into the main target.
 * Sorting handles UI-vs-UI overlap; the shared scene depth handles
 * world-vs-UI occlusion.
 * <p>
 * Panels are added/removed through {@link #panels()}; untethered line
 * geometry (scan frames, drag trails) registers on {@link #lineEmitters()}.
 * The renderer registers itself on the NeoForge event bus on first
 * {@link #get()}.
 */
public final class WorldUiRenderer {

    private static final Logger logger = LoggerFactory.getLogger("CloudLib WorldUiRenderer");

    private static @org.jetbrains.annotations.Nullable WorldUiRenderer instance;

    /** The shared renderer — registers its level-stage hook on first access. */
    public static WorldUiRenderer get() {
        if (instance == null) {
            instance = new WorldUiRenderer();
            NeoForge.EVENT_BUS.addListener(instance::onLevelStage);
        }
        return instance;
    }

    private final Minecraft mc = Minecraft.getInstance();
    private final List<WorldUiPanel> panels = new ArrayList<>();
    private final List<WorldUiPanel> visible = new ArrayList<>();
    /** Frame-level line emitters — not tied to a panel quad's visibility. */
    private final List<WorldUiPanel.LinesEmitter> lineEmitters = new ArrayList<>();

    private Matrix4f worldToView = new Matrix4f();

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
    }

    public void removePanel(WorldUiPanel panel) {
        if (panels.remove(panel)) panel.close();
    }

    public void clearPanels() {
        for (WorldUiPanel panel : panels) panel.close();
        panels.clear();
    }

    // region level stage

    private void onLevelStage(RenderLevelStageEvent event) {
        // last stage — the whole level (including the Fabulous! translucent
        // merge) is already in the main target, so sorted translucent draws
        // composite correctly over everything behind them
        if (event.getStage() != RenderLevelStageEvent.Stage.AFTER_LEVEL) return;
        if (mc.level == null || mc.player == null || (panels.isEmpty() && lineEmitters.isEmpty())) return;

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
            panel.basis = panel.placer().place(frame);
            if (panel.basis != null) visible.add(panel);
        }
        if (visible.isEmpty() && lineEmitters.isEmpty()) return;

        // 2. surfaces — every panel repaints its FBO up-front; no surface
        //    work interleaves with the world pass below
        for (WorldUiPanel panel : visible) {
            panel.surface().render(panel.width(), panel.height(), panel.supersample(), panel.painter()::paint, pt);
        }

        // 3. world pass — sorted translucency: far → near, premult blend,
        //    depth-tested against the completed scene, no depth writes
        var mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        mv.identity();
        RenderSystem.applyModelViewMatrix();
        try {
            visible.sort((a, b) -> Double.compare(centerDist(b, cameraPos), centerDist(a, cameraPos)));
            for (WorldUiPanel panel : visible) {
                drawPanelQuad(panel);
            }
            drawLines();
        } finally {
            mv.popMatrix();
            RenderSystem.applyModelViewMatrix();
        }
    }

    private static double centerDist(WorldUiPanel panel, Vec3 cameraPos) {
        QuadBasis q = panel.basis;
        if (q == null) return 0;
        return q.center(panel.width(), panel.height()).distanceTo(cameraPos);
    }

    // endregion

    // region quad draws

    /**
     * Blits a panel's surface texture onto its world quad. The surface stores
     * premultiplied texels, so the composite uses the premult func —
     * {@code SRC_ALPHA} would square alpha and wash it out.
     */
    private void drawPanelQuad(WorldUiPanel panel) {
        int tex = panel.surface().colorTextureId();
        QuadBasis q = panel.basis;
        if (tex == 0 || q == null) return;

        BufferBuilder buffer =
                Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
        emitQuad(buffer, q, panel.width(), panel.height(), panel.opacity());
        MeshData mesh = buffer.build();
        if (mesh == null) return;

        RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
        RenderSystem.enableBlend();
        RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
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
        RenderSystem.depthMask(true);
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
