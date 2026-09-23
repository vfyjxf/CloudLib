package dev.vfyjxf.cloudlib.api.ui.canvas;

import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.Minecraft;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.opengl.GL11;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * The forwarded channel's own batch source: vanilla's capture-and-drain lifecycle, with the one
 * thing a fade needs — a drain that owns the blend state.
 * <p>
 * A forwarded draw's opacity arrives as the shader color's alpha ({@code ColorModulator}), the only
 * per-vertex-free knob vanilla rendering offers. But the shader composes it into the fragment
 * ({@code color = texture * vertexColor * ColorModulator}), so the tint alpha lands in the
 * framebuffer as the fragment's alpha — and a fragment alpha only <em>is</em> an opacity if the
 * draw blends. The render types this channel mostly serves never do: an entity model's body is
 * drawn with {@code entityCutoutNoCull} ({@code Model.renderType}), a block's GUI icon with
 * {@code Sheets.cutoutBlockSheet}'s {@code entityCutout}, and a mob's shadow parts with
 * {@code entitySolid} — all three disable blending in their own render state, so the uniform is a
 * value nobody reads: the model keeps writing full coverage straight through the fade and then
 * vanishes with the panel. That is the preview artifact an exiting panel shows: the background is
 * gone while the model is still solid, then it pops in one frame — or, painting into a UI surface,
 * it hands the surface its own alpha and composites back as a ghost. Asserting blending around each
 * captured mesh makes the fragment alpha the fade factor for <em>every</em> type: one multiply, no
 * darkening, and a silent contribution once the tint reaches zero. A type that blends keeps the
 * blend function its own state set, so additive glints and glow layers still composite their own way.
 * <p>
 * The override runs only while the tint is not opaque — an opaque segment drains exactly like
 * vanilla's, so the steady-state picture is untouched.
 * <p>
 * The drain also pins the pass: a render type's output shard may retarget the framebuffer
 * ({@code itemEntityTranslucentCull} binds the item-entity target under Fabulous graphics), and
 * this source is not the canvas's — a UI surface's {@code SurfaceBufferSource} is. So under shader
 * transparency the binding captured before each draw is re-asserted after the type's setup, the
 * same way that sibling pins its FBO.
 * <p>
 * <b>Thread contract:</b> one instance per render thread, and a segment always drains before its
 * canvas returns, so a single shared source serves every canvas.
 */
final class ForwardedBufferSource extends MultiBufferSource.BufferSource {

    private final ByteBufferBuilder sortScratch = new ByteBufferBuilder(1 << 18);
    /** Render types in first-submission order — the flush order of {@link #endBatch()}. */
    private final List<RenderType> submissionOrder = new ArrayList<>();

    /** The tint alpha of the segment being drained; 1 means "opaque, drain like vanilla". */
    private float fadeAlpha = 1f;
    /** Whether the faded drain keeps the depth test on — see {@link #fade(float, boolean)}. */
    private boolean fadeDepthTested;

    private ForwardedBufferSource() {
        super(new ByteBufferBuilder(786432), fixedAllocators());
    }

    /**
     * The render types that must own a private allocator — the glint family is always requested as
     * the <em>first</em> delegate of a {@code VertexMultiConsumer.Double} (foil item rendering),
     * and a shared-allocator builder would be flushed — and closed — the moment the second
     * delegate's type arrives, leaving the pair writing into a dead builder. {@code RenderBuffers}
     * fixes the same set plus the BER sheets; the sheets only ever appear as the <em>second</em>
     * delegate, so the shared path stays safe for them.
     */
    private static LinkedHashMap<RenderType, ByteBufferBuilder> fixedAllocators() {
        var map = new LinkedHashMap<RenderType, ByteBufferBuilder>();
        for (RenderType type : List.of(
            RenderType.glint(),
            RenderType.glintTranslucent(),
            RenderType.entityGlint(),
            RenderType.entityGlintDirect(),
            RenderType.armorEntityGlint(),
            RenderType.waterMask()
        )) {
            map.put(type, new ByteBufferBuilder(Math.max(type.bufferSize(), 1536)));
        }
        return map;
    }

    private static final class Holder {
        private static final ForwardedBufferSource instance = new ForwardedBufferSource();
    }

    /**
     * The render thread's forwarded source. The backing allocators are native, so the instance is
     * created on first use and reused for the process's lifetime.
     */
    static ForwardedBufferSource shared() {
        return Holder.instance;
    }

    /**
     * Declares the fade the segment's drains run under: the segment's tint alpha, and whether a
     * drain may rely on a depth buffer holding only the segment's own geometry — true for a canvas
     * that clears depth when it opens a forwarded draw, which is what makes the depth test usable as
     * the per-surface resolver a translucent model needs. Declared when the segment opens and
     * cleared when it closes, never per drain: vanilla code inside a forwarded draw flushes on its
     * own, and everything it draws afterwards is still the same segment.
     */
    void fade(float tintAlpha, boolean depthTested) {
        this.fadeAlpha = tintAlpha;
        this.fadeDepthTested = depthTested;
    }

    /** Whether {@code tintAlpha} needs the drain to own the blend state — anything short of opaque. */
    static boolean fadeOwnsBlend(float tintAlpha) {
        return tintAlpha < 1f;
    }

    /** Whether any geometry is waiting to be drawn — an empty segment drains as a no-op. */
    boolean isEmpty() {
        return startedBuilders.isEmpty() && lastSharedType == null;
    }

    @Override
    public VertexConsumer getBuffer(RenderType renderType) {
        BufferBuilder builder = startedBuilders.get(renderType);
        if (builder != null && !renderType.canConsolidateConsecutiveGeometry()) {
            // non-quad modes can't merge with a follow-up batch — flush now
            endBatch(renderType);
            builder = null;
        }
        if (builder != null) {
            return builder;
        }
        ByteBufferBuilder fixed = fixedBuffers.get(renderType);
        if (fixed != null) {
            // dedicated allocator — may stay open alongside a shared-allocator
            // builder (the foil-item Double case)
            builder = new BufferBuilder(fixed, renderType.mode(), renderType.format());
        } else {
            if (lastSharedType != null) {
                // the shared allocator is single-tenant: a new shared type
                // flushes the previous one, preserving submission order
                endBatch(lastSharedType);
            }
            builder = new BufferBuilder(sharedBuffer, renderType.mode(), renderType.format());
            lastSharedType = renderType;
        }
        startedBuilders.put(renderType, builder);
        submissionOrder.add(renderType);
        return builder;
    }

    @Override
    public void endLastBatch() {
        RenderType type = lastSharedType;
        lastSharedType = null;
        if (type != null) endBatch(type);
    }

    @Override
    public void endBatch() {
        float alpha = fadeAlpha;
        boolean faded = fadeOwnsBlend(alpha);
        if (faded) {
            // the src-over function is asserted once for the whole drain: a type whose own state
            // disables blending has set none, and a faded draw must not inherit a stale function
            RenderSystem.blendFuncSeparate(
                GlStateManager.SourceFactor.SRC_ALPHA,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA,
                GlStateManager.SourceFactor.ONE,
                GlStateManager.DestFactor.ONE_MINUS_SRC_ALPHA
            );
            // a translucent draw has to resolve its own surfaces — with no depth test the front and
            // back faces of a model blend into each other in submission order
            if (fadeDepthTested) RenderSystem.enableDepthTest();
        }
        for (RenderType type : submissionOrder) {
            endBatch(type);
        }
        submissionOrder.clear();
        endLastBatch();
    }

    @Override
    public void endBatch(RenderType renderType) {
        BufferBuilder builder = startedBuilders.remove(renderType);
        if (renderType.equals(lastSharedType)) {
            lastSharedType = null;
        }
        if (builder == null) return;
        MeshData mesh = builder.build();
        if (mesh == null) return;
        if (renderType.sortOnUpload()) {
            mesh.sortQuads(sortScratch, RenderSystem.getVertexSorting());
        }
        draw(renderType, mesh, fadeAlpha);
    }

    /**
     * Draws one captured mesh with the type's own render state, plus the fade's additions: the
     * blend a type may have just turned off, and — under shader transparency — the pass binding an
     * output shard may have just stolen.
     */
    private void draw(RenderType renderType, MeshData mesh, float alpha) {
        boolean faded = fadeOwnsBlend(alpha);
        boolean pinned = Minecraft.useShaderTransparency();
        int boundFbo = pinned ? GL11.glGetInteger(GL30.GL_FRAMEBUFFER_BINDING) : -1;
        renderType.setupRenderState();
        if (faded) {
            RenderSystem.enableBlend();
        }
        if (boundFbo >= 0) {
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, boundFbo);
        }
        BufferUploader.drawWithShader(mesh);
        renderType.clearRenderState();
        if (boundFbo >= 0) {
            GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, boundFbo);
        }
    }
}
