package dev.vfyjxf.cloudlib.internal.ui.inworld;

import com.mojang.blaze3d.pipeline.TextureTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import com.mojang.blaze3d.vertex.VertexSorting;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.inworld.render.RenderStats;
import dev.vfyjxf.cloudlib.api.ui.inworld.render.Supersampling;
import dev.vfyjxf.cloudlib.api.ui.inworld.render.WorldUiPanel;
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

    private final Minecraft mc = Minecraft.getInstance();
    private final SurfaceBufferSource buffers = new SurfaceBufferSource();
    private final MipmapChain mips = new MipmapChain();

    private @Nullable TextureTarget target;
    private int widthPx;
    private int heightPx;
    private int supersample;

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

    /** The supersample factor the surface was last rendered with (0 while never rendered). */
    public int supersample() {
        return supersample;
    }

    /**
     * Repaints the surface: binds the target with a GUI ortho setup, runs the
     * painter, flushes every buffered draw, then restores the caller's full
     * render state (framebuffer, viewport, projection, model-view stack,
     * vertex sorting). Safe to call mid level-stage — it owns nothing ambient.
     *
     * @param projectedW {@code projectedH} the quad's projected size in
     *     framebuffer pixels — mip levels are only regenerated while the world
     *     quad minifies the surface (see {@link Supersampling#needsMipmap});
     *     pass 0 when unknown
     */
    public void render(
        int wPx,
        int hPx,
        int supersample,
        double projectedW,
        double projectedH,
        WorldUiPanel.Painter painter,
        float partialTick
    ) {
        RenderSystem.assertOnRenderThread();
        // Capture the caller's render state BEFORE the size pass: a resize
        // inside ensure() ends with RenderTarget's unconditional
        // glBindFramebuffer(GL_FRAMEBUFFER, 0), so a capture taken after it
        // would read 0 on resize frames and the finally block below would
        // restore the default framebuffer over the caller's target — that
        // frame's whole world pass then draws into the wrong buffer and the
        // end-of-frame blit erases every panel for one frame.
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

        if (!ensure(wPx, hPx, supersample)) return;
        RenderStats.surfaceRendered((long) widthPx * supersample * heightPx * supersample);

        var mv = RenderSystem.getModelViewStack();
        mv.pushMatrix();
        try {
            target.clear(Minecraft.ON_OSX);
            target.bindWrite(true);
            RenderStats.fboBound();
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
                new Matrix4f().setOrtho(0, wPx, hPx, 0, 1000, 21000),
                VertexSorting.ORTHOGRAPHIC_Z
            );

            GuiGraphics graphics = new GuiGraphics(mc, new PoseStack(), buffers);
            SceneCanvas canvas = SceneCanvas.create(graphics);
            // the surface rasterizes at ss texels per logical pixel — SDF
            // smoothing must follow that density, not the window's gui scale
            canvas.targetSupersample(supersample);
            painter.paint(canvas, wPx, hPx, partialTick);
            canvas.flushBatch();
            buffers.endBatch();
            // mip levels only filter the world quad's minification of the
            // surface; while the quad magnifies, level 0 is all the LINEAR
            // magnifier samples and regenerating the chain is pure waste —
            // unless the chain is stale after a (re)allocation, in which
            // case it is rebuilt this frame whatever the magnification
            // (see MipmapChain)
            boolean quadMinifies = Supersampling
                    .needsMipmap(widthPx * supersample, heightPx * supersample, projectedW, projectedH);
            if (mips.shouldGenerate(quadMinifies)) {
                RenderSystem.bindTexture(target.getColorTextureId());
                GL30.glGenerateMipmap(GL11.GL_TEXTURE_2D);
                RenderStats.mipmapGenerated();
                mips.generated();
                // the chain is complete again — put mipmap filtering back so
                // the world pass samples a filtered chain, not bare level 0
                GlStateManager
                        ._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR_MIPMAP_LINEAR);
            }
        } finally {
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFbo);
            RenderStats.fboBound();
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
            mips.reallocated();
            RenderStats.surfaceResized();
        } else if (target.width != w || target.height != h) {
            target.resize(w, h, Minecraft.ON_OSX);
            mips.reallocated();
            RenderStats.surfaceResized();
        }
        // RenderTarget defaults its color texture to NEAREST and createBuffers
        // force-resets it — the world quad magnifies/minifies the surface, so
        // NEAREST stairsteps text and SDF edges. MIN follows the mip chain's
        // validity: LINEAR while the chain is gone (a mipmap filter over a
        // level-0-only texture is incomplete and samples opaque black), back
        // to LINEAR_MIPMAP_LINEAR once a frame has rebuilt the chain. MAG
        // can't take a mipmapped enum — set the filters directly.
        GlStateManager._bindTexture(target.getColorTextureId());
        GlStateManager._texParameter(
            GL11.GL_TEXTURE_2D,
            GL11.GL_TEXTURE_MIN_FILTER,
            mips.minFilter() == MipmapChain.MinFilter.linear ? GL11.GL_LINEAR : GL11.GL_LINEAR_MIPMAP_LINEAR
        );
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
