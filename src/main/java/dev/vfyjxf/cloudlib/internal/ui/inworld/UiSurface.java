package dev.vfyjxf.cloudlib.internal.ui.inworld;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

/**
 * A panel's offscreen surface: a {@link TextureTarget} (color + depth) sized
 * at {@code logicalSize × supersample} that content is rasterized into with
 * the plain GUI convention — ortho projection over logical pixels, modelView
 * translated to {@code z = -11000}.
 * <p>
 * Every fill/blit inside goes through ordinary src-over blending, so the
 * resulting texture is <b>premultiplied</b>; consumers composite it with the
 * premult blend func ({@code ONE, ONE_MINUS_SRC_ALPHA}).
 * <p>
 * The {@link SurfaceBufferSource} backs the {@link GuiGraphics} handed to the
 * painter, so <em>every</em> vanilla draw path — batched quads, font text,
 * real 3D item models, enchant glints — lands inside this target. No content
 * escapes to the scene framebuffer mid-pass.
 */
public final class UiSurface implements AutoCloseable {

    /** The painter callback: draws the surface's content for this frame. */
    @FunctionalInterface
    public interface Painter {
        void paint(SceneCanvas canvas, int width, int height, float partialTick);
    }

    private final Minecraft mc = Minecraft.getInstance();
    private final SurfaceBufferSource buffers = new SurfaceBufferSource();

    private @Nullable TextureTarget target;
    private int widthPx;
    private int heightPx;
    private int supersample = 2;

    /** The surface color texture, or 0 while unallocated. */
    public int colorTextureId() {
        return target == null ? 0 : target.getColorTextureId();
    }

    /** Logical (gui-px) width of the painted area. */
    public int width() {
        return widthPx;
    }

    /** Logical (gui-px) height of the painted area. */
    public int height() {
        return heightPx;
    }

    /**
     * Repaints the surface: binds the target with a GUI ortho setup, runs the
     * painter, flushes every buffered draw, then restores the caller's full
     * render state (framebuffer, viewport, projection, model-view stack,
     * vertex sorting). Safe to call mid level-stage — it owns nothing ambient.
     */
    public void render(int wPx, int hPx, int supersample, Painter painter, float partialTick) {
        RenderSystem.assertOnRenderThread();
        if (!ensure(wPx, hPx, supersample)) return;

        int prevFbo = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        int[] prevVp = new int[4];
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, prevVp);
        Matrix4f prevProj = new Matrix4f(RenderSystem.getProjectionMatrix());
        VertexSorting prevSorting = RenderSystem.getVertexSorting();
        // entity/item shaders apply distance fog off the view-space vertex
        // distance — inside this pass everything sits ~11000 units out, so
        // without neutralizing fog items rasterize as solid fog-color blobs
        float prevFogStart = RenderSystem.getShaderFogStart();
        float prevFogEnd = RenderSystem.getShaderFogEnd();

        var mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        try {
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            buffers.bindTo(target);
            // the surface owns its depth buffer — canvas layering relies on
            // depth clears, which silently no-op while depthMask is off
            RenderSystem.depthMask(true);

            mv.identity();
            mv.translate(0, 0, -11000);
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setShaderFogStart(Float.MAX_VALUE);
            RenderSystem.setShaderFogEnd(Float.MAX_VALUE);
            // items rendered with a block model keep the ambient light state —
            // inside a level stage that is directional sun light; give them the
            // gui 3D-item lighting they would get inside an inventory surface
            Lighting.setupFor3DItems();
            RenderSystem.setProjectionMatrix(
                    new Matrix4f().setOrtho(0, wPx, hPx, 0, 1000, 21000), VertexSorting.ORTHOGRAPHIC_Z);

            GuiGraphics graphics = new GuiGraphics(mc, new PoseStack(), buffers);
            SceneCanvas canvas = SceneCanvas.create(graphics);
            painter.paint(canvas, wPx, hPx, partialTick);
            canvas.flushBatch();
            buffers.endBatch();
            // regenerate mip levels — the world quad minifies the surface at
            // distance/glancing angles and LINEAR alone shimmers badly
            RenderSystem.bindTexture(target.getColorTextureId());
            GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFbo);
            RenderSystem.viewport(prevVp[0], prevVp[1], prevVp[2], prevVp[3]);
            mv.popMatrix();
            RenderSystem.applyModelViewMatrix();
            RenderSystem.setProjectionMatrix(prevProj, prevSorting);
            RenderSystem.setShaderFogStart(prevFogStart);
            RenderSystem.setShaderFogEnd(prevFogEnd);
            if (mc.level != null && mc.level.effects().constantAmbientLight()) {
                Lighting.setupNetherLevel();
            } else {
                Lighting.setupLevel();
            }
            // canvas work leaves depth state loose — re-arm the translucent
            // stage ambient values so nothing leaks into the level pass
            RenderSystem.depthMask(true);
            RenderSystem.enableDepthTest();
            RenderSystem.enableCull();
        }
    }

    private boolean ensure(int wPx, int hPx, int ss) {
        if (wPx <= 0 || hPx <= 0) return false;
        widthPx = wPx;
        heightPx = hPx;
        supersample = ss;
        int w = wPx * ss;
        int h = hPx * ss;
        if (target == null) {
            target = new TextureTarget(w, h, true, Minecraft.ON_OSX);
            target.setClearColor(0f, 0f, 0f, 0f);
        } else if (target.width != w || target.height != h) {
            target.resize(w, h, Minecraft.ON_OSX);
        }
        // RenderTarget defaults its color texture to NEAREST and createBuffers
        // force-resets it — the world quad magnifies/minifies the surface, so
        // NEAREST stairsteps text and SDF edges; re-assert every frame, with
        // mipmaps so minification filters instead of shimmering (MAG can't
        // take a mipmapped enum — set the filters directly)
        GlStateManager._bindTexture(target.getColorTextureId());
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        return true;
    }

    /** Releases GL objects — callable from any thread (defers to the render thread). */
    @Override
    public void close() {
        TextureTarget t = target;
        target = null;
        if (RenderSystem.isOnRenderThread()) {
            if (t != null) t.destroyBuffers();
            buffers.close();
        } else {
            RenderSystem.recordRenderCall(() -> {
                if (t != null) t.destroyBuffers();
                buffers.close();
            });
        }
    }
}
