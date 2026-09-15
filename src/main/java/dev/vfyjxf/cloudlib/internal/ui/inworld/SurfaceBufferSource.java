package dev.vfyjxf.cloudlib.internal.ui.inworld;

import com.mojang.blaze3d.pipeline.RenderTarget;
import com.mojang.blaze3d.platform.GlStateManager;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.ByteBufferBuilder;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.VertexConsumer;
import net.minecraft.client.renderer.MultiBufferSource;
import net.minecraft.client.renderer.RenderType;
import org.lwjgl.opengl.GL30;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;

/**
 * A {@link MultiBufferSource.BufferSource} that rasterizes every render type
 * into the currently bound UI surface FBO instead of wherever the type's
 * {@code OutputStateShard} points.
 * <p>
 * Vanilla end-of-batch drawing runs {@code RenderType.draw}, whose setup binds
 * an output target (main / translucent / item-entity …) — inside an offscreen
 * UI pass that binding is an escape hatch: 3D item models, enchant glints and
 * every other buffer-served draw would land on the scene framebuffer instead
 * of the panel texture. This source re-implements the batch lifecycle: meshes
 * are captured through the ordinary {@link #getBuffer} path and drawn with the
 * type's full render state, but the surface FBO is rebound after the setup and
 * after the clear step, so the shard's rebind never wins.
 * <p>
 * Submission order is preserved across render types: switching type flushes
 * the open builder, so painter order survives for text-over-item and friends.
 */
public final class SurfaceBufferSource extends MultiBufferSource.BufferSource {

    private final ByteBufferBuilder sortScratch = new ByteBufferBuilder(1 << 18);
    /** Rendertypes in first-submission order — the flush order of {@link #endBatch()}. */
    private final List<RenderType> submissionOrder = new ArrayList<>();

    private int boundFbo = -1;
    private int boundW;
    private int boundH;

    public SurfaceBufferSource() {
        super(new ByteBufferBuilder(786432), fixedAllocators());
    }

    /**
     * Render types that must own a private allocator — the glint family is
     * always requested as the <em>first</em> delegate of a
     * {@code VertexMultiConsumer.Double} (foil item rendering), and a
     * shared-allocator builder would be flushed — and closed — the moment the
     * second delegate's type arrives, leaving the pair writing into a dead
     * builder. {@code RenderBuffers} fixes the same set plus the BER sheets;
     * the sheets only ever appear as the <em>second</em> delegate, so the
     * shared path stays safe for them.
     */
    private static LinkedHashMap<RenderType, ByteBufferBuilder> fixedAllocators() {
        var map = new LinkedHashMap<RenderType, ByteBufferBuilder>();
        for (RenderType type : List.of(
                RenderType.glint(),
                RenderType.glintTranslucent(),
                RenderType.entityGlint(),
                RenderType.entityGlintDirect(),
                RenderType.armorEntityGlint(),
                RenderType.waterMask())) {
            map.put(type, new ByteBufferBuilder(Math.max(type.bufferSize(), 1536)));
        }
        return map;
    }

    /**
     * Points {@link #endBatch} draws at the given target. Called by the owning
     * surface right after {@code bindWrite}; must be re-asserted per surface
     * pass since a single source instance outlives binds.
     */
    public void bindTo(RenderTarget target) {
        boundFbo = target.frameBufferId;
        boundW = target.width;
        boundH = target.height;
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
        drawIntoSurface(renderType, mesh);
    }

    /**
     * Draws the captured mesh with the render type's own state — the surface
     * FBO is rebound after setup (the output shard just stole the binding to
     * its preferred target) and again after clear (clearState may rebind too).
     */
    private void drawIntoSurface(RenderType renderType, MeshData mesh) {
        renderType.setupRenderState();
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, boundFbo);
        RenderSystem.viewport(0, 0, boundW, boundH);
        BufferUploader.drawWithShader(mesh);
        renderType.clearRenderState();
        GlStateManager._glBindFramebuffer(GL30.GL_FRAMEBUFFER, boundFbo);
        RenderSystem.viewport(0, 0, boundW, boundH);
    }

    /** Releases the backing allocators — call on the render thread. */
    public void close() {
        sharedBuffer.close();
        sortScratch.close();
        fixedBuffers.values().forEach(ByteBufferBuilder::close);
        startedBuilders.clear();
        submissionOrder.clear();
    }
}
