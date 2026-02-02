package dev.vfyjxf.cloudlib.api.ui.canvas;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.MeshData;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.texture.BatchableTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.SizedTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.network.chat.Component;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;
import org.joml.Vector4f;
import org.lwjgl.opengl.GL11;

import java.util.ArrayDeque;
import java.util.ArrayList;
import java.util.Deque;
import java.util.List;
import java.util.function.Consumer;

/**
 * Rendering canvas with transform, clip and automatic batch rendering.
 */
public final class SceneCanvas {

    public static SceneCanvas create(GuiGraphics graphics) {
        return new SceneCanvas(graphics);
    }

    private final GuiGraphics graphics;
    private final ClipStack clipStack = new ClipStack();

    // Transform stack
    private final Deque<Matrix4f> transformStack = new ArrayDeque<>();
    private Matrix4f currentTransform = new Matrix4f();

    private final Vector4f transformTemp = new Vector4f();

    // Color state
    private int currentColor = 0xFFFFFFFF;

    private float zOffset = 0f;

    // Batch state (always enabled globally)
    private final BatchState batchState = new BatchState();

    private SceneCanvas(GuiGraphics graphics) {
        this.graphics = graphics;
    }

    //region batch

    private static final class BatchState {
        private record TexturedQuad(
            float x, float y, float width, float height,
            float u0, float v0, float u1, float v1, int color
        ) {}

        private record ColoredQuad(float x, float y, float width, float height, int color) {}

        // Current textured batch
        private @Nullable ResourceLocation currentTexture = null;
        private final List<TexturedQuad> texturedQuads = new ArrayList<>();

        // Current colored batch (no texture)
        private final List<ColoredQuad> coloredQuads = new ArrayList<>();

        // Whether we have pending colored quads (they break textured batches)
        private boolean hasColoredPending = false;

        void addTextured(ResourceLocation texture, float x, float y, float width, float height,
                         float u0, float v0, float u1, float v1, int color) {
            // If texture changed, signal that previous batch should be flushed
            if (currentTexture != null && !currentTexture.equals(texture)) {
                // Caller should flush before adding
                return;
            }

            currentTexture = texture;
            texturedQuads.add(new TexturedQuad(x, y, width, height, u0, v0, u1, v1, color));
        }

        void addColored(float x, float y, float width, float height, int color) {
            coloredQuads.add(new ColoredQuad(x, y, width, height, color));
            hasColoredPending = true;
        }

        boolean needsFlushForTexture(ResourceLocation texture) {
            // Need to flush if we have colored quads pending
            if (hasColoredPending && !coloredQuads.isEmpty()) {
                return true;
            }
            // Need to flush if texture changed
            return currentTexture != null && !currentTexture.equals(texture);
        }

        boolean needsFlushForColored() {
            // Need to flush textured batch before drawing colored
            return !texturedQuads.isEmpty();
        }

        boolean isEmpty() {
            return texturedQuads.isEmpty() && coloredQuads.isEmpty();
        }

        void clear() {
            currentTexture = null;
            texturedQuads.clear();
            coloredQuads.clear();
            hasColoredPending = false;
        }
    }

    private final BatchableTexture.VertexEmitter batchEmitter = new BatchableTexture.VertexEmitter() {
        @Override
        public void textured(ResourceLocation texture, float x, float y, float width, float height,
                             float u0, float v0, float u1, float v1, int color) {
            // Auto-flush if texture changed
            if (batchState.needsFlushForTexture(texture)) {
                flushBatch();
            }
            batchState.addTextured(texture, x, y, width, height, u0, v0, u1, v1, color);
        }

        @Override
        public void colored(float x, float y, float width, float height, int color) {
            // Auto-flush textured batch if needed
            if (batchState.needsFlushForColored()) {
                flushBatch();
            }
            batchState.addColored(x, y, width, height, color);
        }
    };

    public BatchableTexture.VertexEmitter emitter() {
        return batchEmitter;
    }

    public void flushBatch() {
        if (batchState.isEmpty()) {
            return;
        }

        applyScissor();
        graphics.pose().pushPose();
        graphics.pose().mulPose(currentTransform);
        graphics.pose().translate(0, 0, zOffset);

        // Flush textured quads
        if (!batchState.texturedQuads.isEmpty() && batchState.currentTexture != null) {
            RenderSystem.setShaderTexture(0, batchState.currentTexture);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.enableBlend();

            BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);
            Matrix4f matrix = graphics.pose().last().pose();

            for (var quad : batchState.texturedQuads) {
                float x0 = quad.x, y0 = quad.y;
                float x1 = quad.x + quad.width, y1 = quad.y + quad.height;
                int c = quad.color;
                float a = ((c >> 24) & 0xFF) / 255f;
                float r = ((c >> 16) & 0xFF) / 255f;
                float g = ((c >> 8) & 0xFF) / 255f;
                float b = (c & 0xFF) / 255f;

                buffer.addVertex(matrix, x0, y1, 0).setUv(quad.u0, quad.v1).setColor(r, g, b, a);
                buffer.addVertex(matrix, x1, y1, 0).setUv(quad.u1, quad.v1).setColor(r, g, b, a);
                buffer.addVertex(matrix, x1, y0, 0).setUv(quad.u1, quad.v0).setColor(r, g, b, a);
                buffer.addVertex(matrix, x0, y0, 0).setUv(quad.u0, quad.v0).setColor(r, g, b, a);
            }

            MeshData meshData = buffer.build();
            if (meshData != null) {
                BufferUploader.drawWithShader(meshData);
            }
        }

        // Flush colored quads
        if (!batchState.coloredQuads.isEmpty()) {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.enableBlend();

            BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            Matrix4f matrix = graphics.pose().last().pose();

            for (var quad : batchState.coloredQuads) {
                float x0 = quad.x, y0 = quad.y;
                float x1 = quad.x + quad.width, y1 = quad.y + quad.height;
                int c = quad.color;
                float a = ((c >> 24) & 0xFF) / 255f;
                float r = ((c >> 16) & 0xFF) / 255f;
                float g = ((c >> 8) & 0xFF) / 255f;
                float b = (c & 0xFF) / 255f;

                buffer.addVertex(matrix, x0, y1, 0).setColor(r, g, b, a);
                buffer.addVertex(matrix, x1, y1, 0).setColor(r, g, b, a);
                buffer.addVertex(matrix, x1, y0, 0).setColor(r, g, b, a);
                buffer.addVertex(matrix, x0, y0, 0).setColor(r, g, b, a);
            }

            MeshData meshData = buffer.build();
            if (meshData != null) {
                BufferUploader.drawWithShader(meshData);
            }
        }

        graphics.pose().popPose();
        restoreScissor();

        batchState.clear();
    }


    //endregion

    //region render

    public GuiGraphics graphics() {
        return graphics;
    }

    //region color

    public SceneCanvas color(int argb) {
        this.currentColor = argb;
        return this;
    }

    public SceneCanvas resetColor() {
        this.currentColor = 0xFFFFFFFF;
        return this;
    }

    public int currentColor() {
        return currentColor;
    }

    //endregion

    /**
     * Executes a draw call with transform and clip applied.
     * Flushes any pending batch before executing.
     */
    public SceneCanvas batch(Runnable drawCall) {
        flushBatch();
        applyScissor();
        graphics.pose().pushPose();
        graphics.pose().mulPose(currentTransform);
        graphics.pose().translate(0, 0, zOffset);
        drawCall.run();
        graphics.pose().popPose();
        restoreScissor();
        return this;
    }

    public Matrix4f pose() {
        return graphics.pose().last().pose();
    }

    public Matrix4f combinedPose() {
        Matrix4f combined = new Matrix4f(graphics.pose().last().pose());
        combined.mul(currentTransform);
        return combined;
    }

    //region texture

    public SceneCanvas texture(VisualTexture texture, int x, int y, int width, int height) {
        // Check if texture supports batching
        if (texture instanceof BatchableTexture batchable && batchable.supportsBatching()) {
            batchable.emit(batchEmitter, x, y, width, height, currentColor);
        } else {
            // Non-batchable texture - flush pending batch and draw immediately
            flushBatch();
            applyScissor();
            graphics.pose().pushPose();
            graphics.pose().mulPose(currentTransform);
            graphics.pose().translate(0, 0, zOffset);
            applyColorTint(() -> texture.render(graphics, x, y, width, height));
            graphics.pose().popPose();
            restoreScissor();
        }
        return this;
    }

    public <T extends SizedTexture> SceneCanvas texture(T texture, int x, int y) {
        return texture(texture, x, y, texture.width(), texture.height());
    }

    public SceneCanvas quad(ResourceLocation texture, int x, int y, int width, int height,
                            float u0, float v0, float u1, float v1) {
        batchEmitter.textured(texture, x, y, width, height, u0, v0, u1, v1, currentColor);
        return this;
    }

    public SceneCanvas quad(ResourceLocation texture, int x, int y, int width, int height,
                            int u, int v, int regionWidth, int regionHeight,
                            int textureWidth, int textureHeight) {
        float u0 = (float) u / textureWidth;
        float v0 = (float) v / textureHeight;
        float u1 = (float) (u + regionWidth) / textureWidth;
        float v1 = (float) (v + regionHeight) / textureHeight;
        return quad(texture, x, y, width, height, u0, v0, u1, v1);
    }

    public SceneCanvas sprite(ResourceLocation spriteLocation, int x, int y, int width, int height) {
        var minecraft = Minecraft.getInstance();
        var guiSprites = minecraft.getGuiSprites();
        var sprite = guiSprites.getSprite(spriteLocation);
        return sprite(sprite, x, y, width, height);
    }

    public SceneCanvas sprite(TextureAtlasSprite sprite, int x, int y, int width, int height) {
        batchEmitter.textured(sprite.atlasLocation(), x, y, width, height,
            sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), currentColor);
        return this;
    }

    public SceneCanvas blit(ResourceLocation texture, int x, int y, int width, int height,
                            int u, int v, int regionWidth, int regionHeight,
                            int textureWidth, int textureHeight) {
        return quad(texture, x, y, width, height, u, v, regionWidth, regionHeight, textureWidth, textureHeight);
    }

    public SceneCanvas blit(ResourceLocation texture, int x, int y, int u, int v,
                            int width, int height, int textureWidth, int textureHeight) {
        return quad(texture, x, y, width, height, u, v, width, height, textureWidth, textureHeight);
    }

    public SceneCanvas blitSprite(ResourceLocation spriteLocation, int x, int y, int width, int height) {
        return sprite(spriteLocation, x, y, width, height);
    }

    //endregion

    //region fill

    public SceneCanvas fill(int x, int y, int width, int height, int color) {
        batchEmitter.colored(x, y, width, height, color);
        return this;
    }

    public SceneCanvas fill(Rect rect, int color) {
        return fill(rect.x(), rect.y(), rect.width(), rect.height(), color);
    }

    /**
     * Fills a gradient rectangle (vertical). Not batchable.
     */
    public SceneCanvas fillGradient(int x, int y, int width, int height, int colorTop, int colorBottom) {
        flushBatch(); // Gradient cannot be batched
        applyScissor();
        graphics.pose().pushPose();
        graphics.pose().mulPose(currentTransform);
        graphics.pose().translate(0, 0, zOffset);
        graphics.fillGradient(x, y, x + width, y + height, colorTop, colorBottom);
        graphics.pose().popPose();
        restoreScissor();
        return this;
    }

    public SceneCanvas border(int x, int y, int width, int height, int color, int thickness) {
        // Top
        fill(x, y, width, thickness, color);
        // Bottom
        fill(x, y + height - thickness, width, thickness, color);
        // Left
        fill(x, y + thickness, thickness, height - 2 * thickness, color);
        // Right
        fill(x + width - thickness, y + thickness, thickness, height - 2 * thickness, color);
        return this;
    }

    public SceneCanvas border(int x, int y, int width, int height, int color) {
        return border(x, y, width, height, color, 1);
    }

    public SceneCanvas hLine(int x1, int x2, int y, int color) {
        int minX = Math.min(x1, x2);
        int maxX = Math.max(x1, x2);
        return fill(minX, y, maxX - minX + 1, 1, color);
    }

    public SceneCanvas vLine(int x, int y1, int y2, int color) {
        int minY = Math.min(y1, y2);
        int maxY = Math.max(y1, y2);
        return fill(x, minY, 1, maxY - minY + 1, color);
    }

    public SceneCanvas highlight(int x, int y, int width, int height) {
        return fill(x, y, width, height, 0x80FFFFFF);
    }

    public SceneCanvas highlight(int x, int y, int width, int height, int color) {
        return fill(x, y, width, height, color);
    }

    //endregion

    //region text

    public SceneCanvas drawString(String text, int x, int y, int color) {
        return drawString(text, x, y, color, false);
    }

    public SceneCanvas drawString(String text, int x, int y, int color, boolean dropShadow) {
        flushBatch();
        applyScissor();
        graphics.pose().pushPose();
        graphics.pose().mulPose(currentTransform);
        graphics.pose().translate(0, 0, zOffset);
        graphics.drawString(font(), text, x, y, color, dropShadow);
        graphics.pose().popPose();
        restoreScissor();
        return this;
    }

    public SceneCanvas drawString(Component text, int x, int y, int color) {
        return drawString(text, x, y, color, false);
    }

    public SceneCanvas drawString(Component text, int x, int y, int color, boolean dropShadow) {
        flushBatch();
        applyScissor();
        graphics.pose().pushPose();
        graphics.pose().mulPose(currentTransform);
        graphics.pose().translate(0, 0, zOffset);
        graphics.drawString(font(), text, x, y, color, dropShadow);
        graphics.pose().popPose();
        restoreScissor();
        return this;
    }

    public SceneCanvas drawCenteredString(String text, int x, int y, int color) {
        int textWidth = font().width(text);
        return drawString(text, x - textWidth / 2, y, color);
    }

    public SceneCanvas drawCenteredString(Component text, int x, int y, int color) {
        int textWidth = font().width(text);
        return drawString(text, x - textWidth / 2, y, color);
    }

    public SceneCanvas text(String text, int x, int y, int color, boolean dropShadow) {
        return drawString(text, x, y, color, dropShadow);
    }

    public SceneCanvas text(String text, int x, int y, int color) {
        return drawString(text, x, y, color, false);
    }

    public SceneCanvas text(Component text, int x, int y, int color, boolean dropShadow) {
        return drawString(text, x, y, color, dropShadow);
    }

    public SceneCanvas text(Component text, int x, int y, int color) {
        return drawString(text, x, y, color, false);
    }

    //endregion

    //region item

    private static final float Z_INCREMENT = 1f;

    public SceneCanvas renderItem(ItemStack stack, int x, int y) {
        flushBatch();
        applyScissor();
        graphics.pose().pushPose();
        graphics.pose().mulPose(currentTransform);
        graphics.pose().translate(0, 0, zOffset);
        graphics.renderItem(stack, x, y);
        graphics.pose().popPose();
        restoreScissor();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        zOffset += Z_INCREMENT;
        return this;
    }

    public SceneCanvas renderItemDecorations(ItemStack stack, int x, int y, @Nullable String text) {
        flushBatch();
        applyScissor();
        graphics.pose().pushPose();
        graphics.pose().mulPose(currentTransform);
        graphics.pose().translate(0, 0, zOffset);
        graphics.renderItemDecorations(font(), stack, x, y, text);
        graphics.pose().popPose();
        restoreScissor();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        zOffset += Z_INCREMENT;
        return this;
    }

    public SceneCanvas renderItemDecorations(ItemStack stack, int x, int y) {
        return renderItemDecorations(stack, x, y, null);
    }

    //endregion

    //region layered

    /**
     * Renders content that changes z-level (items, tooltips, etc.).
     * Clears depth buffer after rendering and adds z offset for subsequent draws.
     */
    public SceneCanvas renderLayered(Consumer<GuiGraphics> draw) {
        flushBatch();
        applyScissor();
        graphics.pose().pushPose();
        graphics.pose().mulPose(currentTransform);
        graphics.pose().translate(0, 0, zOffset);
        draw.accept(graphics);
        graphics.pose().popPose();
        restoreScissor();
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        zOffset += Z_INCREMENT;
        return this;
    }

    //endregion

    /**
     * Executes a custom draw operation. Not batchable.
     */
    public SceneCanvas render(Consumer<GuiGraphics> draw) {
        flushBatch();
        applyScissor();
        graphics.pose().pushPose();
        graphics.pose().mulPose(currentTransform);
        graphics.pose().translate(0, 0, zOffset);
        draw.accept(graphics);
        graphics.pose().popPose();
        restoreScissor();
        return this;
    }

    //region scoped

    public SceneCanvas withTransform(Runnable action) {
        pushTransform();
        try {
            action.run();
        } finally {
            popTransform();
        }
        return this;
    }

    public SceneCanvas withClip(int x, int y, int width, int height, Runnable action) {
        pushClip(x, y, width, height);
        try {
            action.run();
        } finally {
            popClip();
        }
        return this;
    }

    public SceneCanvas withClip(Rect clip, Runnable action) {
        return withClip(clip.x(), clip.y(), clip.width(), clip.height(), action);
    }

    public SceneCanvas translated(int x, int y, Runnable action) {
        pushTransform();
        try {
            translate(x, y);
            action.run();
        } finally {
            popTransform();
        }
        return this;
    }

    /**
     * Executes an action with translation and clipping. Flushes batch at scope exit.
     */
    public SceneCanvas scoped(int x, int y, int width, int height, Runnable action) {
        pushTransform();
        pushClip(x, y, width, height);
        try {
            translate(x, y);
            action.run();
        } finally {
            flushBatch(); // Ensure batch is flushed at scope exit
            popClip();
            popTransform();
        }
        return this;
    }

    //endregion

    public Font font() {
        return Minecraft.getInstance().font;
    }

    private void applyColorTint(Runnable action) {
        if (currentColor != 0xFFFFFFFF) {
            float a = ((currentColor >> 24) & 0xFF) / 255f;
            float r = ((currentColor >> 16) & 0xFF) / 255f;
            float g = ((currentColor >> 8) & 0xFF) / 255f;
            float b = (currentColor & 0xFF) / 255f;
            RenderSystem.enableBlend();
            RenderSystem.setShaderColor(r, g, b, a);
            action.run();
            RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
        } else {
            action.run();
        }
    }

    private void applyScissor() {
        Rect clip = clipStack.current();
        if (clip != null) {
            graphics.enableScissor(clip.x(), clip.y(), clip.right(), clip.bottom());
        }
    }

    private void restoreScissor() {
        if (clipStack.hasClip()) {
            graphics.disableScissor();
        }
    }

    //endregion

    //region transform

    public SceneCanvas pushTransform() {
        transformStack.push(new Matrix4f(currentTransform));
        return this;
    }

    public SceneCanvas popTransform() {
        if (transformStack.isEmpty()) {
            throw new IllegalStateException("Transform stack underflow");
        }
        currentTransform = transformStack.pop();
        return this;
    }

    public SceneCanvas translate(float x, float y) {
        currentTransform.translate(x, y, 0);
        return this;
    }

    public SceneCanvas translate(int x, int y) {
        return translate((float) x, (float) y);
    }

    public SceneCanvas scale(float scale) {
        currentTransform.scale(scale, scale, 1);
        return this;
    }

    public SceneCanvas scale(float scaleX, float scaleY) {
        currentTransform.scale(scaleX, scaleY, 1);
        return this;
    }

    public SceneCanvas rotate(float radians) {
        currentTransform.rotateZ(radians);
        return this;
    }

    public SceneCanvas rotateDegrees(float degrees) {
        return rotate((float) Math.toRadians(degrees));
    }

    public SceneCanvas resetTransform() {
        currentTransform.identity();
        return this;
    }

    public Matrix4f currentTransform() {
        return new Matrix4f(currentTransform);
    }

    public Matrix4f combinedTransform() {
        Matrix4f combined = new Matrix4f(graphics.pose().last().pose());
        combined.mul(currentTransform);
        return combined;
    }

    public float[] transformPoint(int x, int y) {
        return transformPoint((float) x, (float) y);
    }

    public float[] transformPoint(float x, float y) {
        transformTemp.set(x, y, 0, 1);
        currentTransform.transform(transformTemp);
        float px = transformTemp.x;
        float py = transformTemp.y;
        Matrix4f matrix = graphics.pose().last().pose();
        transformTemp.set(px, py, 0, 1);
        matrix.transform(transformTemp);
        return new float[]{transformTemp.x, transformTemp.y};
    }

    public float[] transformPointLocal(float x, float y) {
        transformTemp.set(x, y, 0, 1);
        currentTransform.transform(transformTemp);
        return new float[]{transformTemp.x, transformTemp.y};
    }

    //endregion

    //region clipping

    public SceneCanvas pushClip(int x, int y, int width, int height) {
        clipStack.push(x, y, width, height);
        return this;
    }

    public SceneCanvas pushClip(Rect rect) {
        clipStack.push(rect);
        return this;
    }

    public SceneCanvas popClip() {
        clipStack.pop();
        return this;
    }

    public @Nullable Rect currentClip() {
        return clipStack.current();
    }

    public boolean isClipped(int x, int y, int width, int height) {
        return clipStack.isClipped(x, y, width, height);
    }

    public boolean isClipped(Rect bounds) {
        return clipStack.isClipped(bounds);
    }

    public boolean hasClip() {
        return clipStack.hasClip();
    }

    public int clipDepth() {
        return clipStack.depth();
    }

    public void enableScissor() {
        Rect clip = clipStack.current();
        if (clip != null) {
            graphics.enableScissor(clip.x(), clip.y(), clip.right(), clip.bottom());
        }
    }

    public void disableScissor() {
        if (clipStack.hasClip()) {
            graphics.disableScissor();
        }
    }

    //endregion
}
