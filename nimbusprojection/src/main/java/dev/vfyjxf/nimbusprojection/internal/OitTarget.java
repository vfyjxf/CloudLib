package dev.vfyjxf.nimbusprojection.internal;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.platform.TextureUtil;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.renderer.ShaderInstance;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL14;
import org.lwjgl.opengl.GL20;
import org.lwjgl.opengl.GL30;
import org.lwjgl.opengl.GL40C;
import org.slf4j.Logger;
import org.slf4j.LoggerFactory;

/**
 * Weighted-blended order-independent transparency (McGuire &amp; Bavoil 2013)
 * accumulation target for the world-space in-world UI.
 * <p>
 * Blaze3D's {@code RenderTarget} only supports a single color attachment, so
 * this is a hand-rolled FBO with two draw buffers:
 * <ul>
 *   <li>{@code GL_COLOR_ATTACHMENT0} — RGBA16F "accum", cleared to
 *       {@code (0,0,0,0)}, blended with {@code (ONE, ONE)};</li>
 *   <li>{@code GL_COLOR_ATTACHMENT1} — R16F "reveal", cleared to
 *       {@code (1,1,1,1)}, blended with {@code (ZERO, ONE_MINUS_SRC_COLOR)}.</li>
 * </ul>
 * The scene's own depth texture is borrowed per frame so UI geometry is
 * occluded by the opaque world; nothing inside the pass writes depth
 * ({@code depthMask(false)}), which is what makes the blend order-independent.
 * A fullscreen resolve then composites {@code accum/reveal} back into the
 * scene target.
 * <p>
 * GL ceiling here is macOS OpenGL 4.1 — {@code glBlendFunci} (4.0),
 * {@code glDrawBuffers}/{@code glClearBufferfv} (3.0) are the newest features
 * used; nothing from 4.2+.
 */
final class OitTarget {

    private static final Logger LOGGER = LoggerFactory.getLogger("NimbusProjection OIT");

    private int fbo = -1;
    private int accumTex = -1;
    private int revealTex = -1;
    private int width = -1;
    private int height = -1;

    /** FBO that was bound when {@link #beginAccum} ran — the pass resolves into it. */
    private int prevFbo;
    private final int[] prevViewport = new int[4];

    /**
     * Creates the FBO on first use and recreates it when the window's physical
     * size changes (the accumulation attachments must match the scene target
     * pixel-for-pixel — the shared depth attachment is window-sized too).
     *
     * @return false when the size is degenerate or the FBO failed to complete —
     *         callers should fall back to direct (non-OIT) drawing
     */
    boolean ensureSize(int w, int h) {
        RenderSystem.assertOnRenderThreadOrInit();
        if (w <= 0 || h <= 0) return false;
        if (fbo >= 0 && width == w && height == h) return true;

        int prev = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        destroy();
        width = w;
        height = h;
        fbo = GlStateManager.glGenFramebuffers();
        accumTex = TextureUtil.generateTextureId();
        revealTex = TextureUtil.generateTextureId();
        initTexture(accumTex, GL30.GL_RGBA16F, GL11.GL_RGBA, w, h);
        initTexture(revealTex, GL30.GL_R16F, GL11.GL_RED, w, h);

        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT0, GL11.GL_TEXTURE_2D, accumTex, 0);
        GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_COLOR_ATTACHMENT1, GL11.GL_TEXTURE_2D, revealTex, 0);
        GL20.glDrawBuffers(new int[]{GL30.GL_COLOR_ATTACHMENT0, GL30.GL_COLOR_ATTACHMENT1});
        //checked without the depth attachment — the scene depth texture is only
        //borrowed per-frame inside beginAccum
        int status = GlStateManager.glCheckFramebufferStatus(GL30.GL_FRAMEBUFFER);
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, prev);
        if (status != GL30.GL_FRAMEBUFFER_COMPLETE) {
            LOGGER.error("OIT accumulation framebuffer incomplete (status {:#x}) — falling back to direct draws", status);
            destroy();
            return false;
        }
        return true;
    }

    private static void initTexture(int tex, int internalFormat, int format, int w, int h) {
        GlStateManager._bindTexture(tex);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MIN_FILTER, GL11.GL_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_MAG_FILTER, GL11.GL_LINEAR);
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_S, 33071); //GL_CLAMP_TO_EDGE
        GlStateManager._texParameter(GL11.GL_TEXTURE_2D, GL11.GL_TEXTURE_WRAP_T, 33071);
        GlStateManager._texImage2D(GL11.GL_TEXTURE_2D, 0, internalFormat, w, h, 0, format, GL11.GL_UNSIGNED_BYTE, null);
    }

    /**
     * Reads the object name of the depth attachment on {@code fbo}. The
     * accumulation pass needs to borrow the scene's real depth buffer so the
     * UI stays occluded by opaque geometry. Vanilla {@code RenderTarget}s
     * always attach depth as a texture — anything else (renderbuffer, none)
     * returns 0 and the caller skips OIT for the frame.
     */
    static int querySceneDepth(int fbo) {
        if (fbo <= 0) return 0;
        int prev = GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING);
        if (prev != fbo) {
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        }
        int type = GL30.glGetFramebufferAttachmentParameteri(
                GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_TYPE);
        int name = 0;
        if (type == GL11.GL_TEXTURE) {
            name = GL30.glGetFramebufferAttachmentParameteri(
                    GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL30.GL_FRAMEBUFFER_ATTACHMENT_OBJECT_NAME);
        }
        if (prev != fbo) {
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, prev);
        }
        return name;
    }

    /**
     * Binds the accumulation target and arms all GL state for the pass.
     * Saves the currently bound FBO and viewport for {@link #endAccum}.
     */
    void beginAccum(int dstFbo, int sceneDepthTex) {
        prevFbo = dstFbo;
        GL11.glGetIntegerv(GL11.GL_VIEWPORT, prevViewport);

        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, fbo);
        //borrow the scene depth buffer — UI tests against the opaque world
        GlStateManager._glFramebufferTexture2D(GL30.GL_FRAMEBUFFER, GL30.GL_DEPTH_ATTACHMENT, GL11.GL_TEXTURE_2D, sceneDepthTex, 0);
        RenderSystem.viewport(0, 0, width, height);
        //accum → black, reveal → white
        GL30.glClearBufferfv(GL11.GL_COLOR, 0, new float[]{0f, 0f, 0f, 0f});
        GL30.glClearBufferfv(GL11.GL_COLOR, 1, new float[]{1f, 1f, 1f, 1f});
        beginDraw();
    }

    /**
     * Re-asserts everything an OIT draw relies on. Must run immediately before
     * every {@code drawWithShader} inside the pass: anything calling the
     * non-indexed {@code glBlendFuncSeparate} in between (rendertype output
     * shards inside a panel FBO fill do exactly that) silently resets the
     * per-attachment funcs on ALL draw buffers, including attachment 1's.
     */
    void beginDraw() {
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        applyAccumBlend();
        //FUNC_ADD is the ambient default and nothing in vanilla changes it,
        //but the equation is context-global — re-arm with the funcs
        GlStateManager._blendEquation(GL14.GL_FUNC_ADD);
        RenderSystem.disableCull();
        RenderSystem.colorMask(true, true, true, true);
    }

    /** The per-attachment blend functions of the accumulation pass. */
    static void applyAccumBlend() {
        //indexed calls are invisible to GlStateManager's BlendState cache —
        //endAccum() restores ambient state with a raw call to compensate
        GL40C.glBlendFunci(0, GL11.GL_ONE, GL11.GL_ONE);
        GL40C.glBlendFunci(1, GL11.GL_ZERO, GL11.GL_ONE_MINUS_SRC_COLOR);
    }

    /** Detaches back to the pre-pass framebuffer and restores ambient state. */
    void endAccum() {
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, prevFbo);
        RenderSystem.viewport(prevViewport[0], prevViewport[1], prevViewport[2], prevViewport[3]);
        restoreAmbientState();
    }

    /**
     * Fullscreen resolve: draws the WBOIT composite
     * {@code (acc.rgb / acc.a, T)} into {@code dstFbo} with
     * {@code (ONE_MINUS_SRC_ALPHA, SRC_ALPHA)} blending — the usual
     * "transparent over scene" equation with T as the surviving coverage.
     */
    void resolve(int dstFbo) {
        ShaderInstance shader = NimbusShaders.oitResolve();
        if (shader == null) return;
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, dstFbo);
        RenderSystem.viewport(0, 0, width, height);
        RenderSystem.disableDepthTest();
        RenderSystem.depthMask(false);
        RenderSystem.enableBlend();
        //raw first so the real call isn't skipped by a desynced cache — the
        //indexed blendi calls inside the pass never touched it — then the
        //RenderSystem call syncs the cache to the same values
        GlStateManager._blendFuncSeparate(
                GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_SRC_ALPHA);
        RenderSystem.blendFuncSeparate(
                GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_SRC_ALPHA,
                GL11.GL_ONE_MINUS_SRC_ALPHA, GL11.GL_SRC_ALPHA);

        RenderSystem.setShader(NimbusShaders::oitResolve);
        RenderSystem.setShaderTexture(0, accumTex);
        RenderSystem.setShaderTexture(1, revealTex);
        BufferBuilder buffer = Tesselator.getInstance()
                .begin(VertexFormat.Mode.TRIANGLES, DefaultVertexFormat.POSITION);
        buffer.addVertex(-1f, -1f, 0f);
        buffer.addVertex(3f, -1f, 0f);
        buffer.addVertex(-1f, 3f, 0f);
        BufferUploader.drawWithShader(buffer.buildOrThrow());

        restoreAmbientState();
    }

    /**
     * Puts real GL state back to the translucent-stage ambient values. The
     * per-attachment {@code glBlendFunci} calls are context-global state that
     * outlives the pass AND are invisible to Blaze3D's cache, so the raw
     * {@code glBlendFuncSeparate} forces the real func on every draw buffer
     * regardless of what the cache believes; the following RenderSystem call
     * re-syncs the cache (no-op when it already agrees).
     */
    static void restoreAmbientState() {
        GL20.glBlendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.blendFuncSeparate(
                GL11.GL_SRC_ALPHA, GL11.GL_ONE_MINUS_SRC_ALPHA,
                GL11.GL_ONE, GL11.GL_ONE_MINUS_SRC_ALPHA);
        RenderSystem.enableBlend();
        RenderSystem.enableDepthTest();
        RenderSystem.depthFunc(GL11.GL_LEQUAL);
        RenderSystem.depthMask(true);
        RenderSystem.colorMask(true, true, true, true);
        RenderSystem.enableCull();
    }

    /** Deletes all GL objects; safe to call with nothing allocated. */
    void close() {
        destroy();
    }

    private void destroy() {
        if (accumTex > -1) {
            TextureUtil.releaseTextureId(accumTex);
            accumTex = -1;
        }
        if (revealTex > -1) {
            TextureUtil.releaseTextureId(revealTex);
            revealTex = -1;
        }
        if (fbo > -1) {
            GlStateManager._glDeleteFramebuffers(fbo);
            fbo = -1;
        }
        width = -1;
        height = -1;
    }
}
