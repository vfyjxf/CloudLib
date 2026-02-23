package dev.vfyjxf.cloudlib.api.ui.canvas;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.*;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.base.Viewport;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.texture.BatchableTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.SizedTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.ShaderInstance;
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
@SuppressWarnings({"unused", "UnusedReturnValue"})
public final class SceneCanvas {

    public static SceneCanvas create(GuiGraphics graphics) {
        return new SceneCanvas(graphics);
    }

    private final GuiGraphics graphics;
    private final ClipStack clipStack = new ClipStack();

    // Render transform stack (render-only effects)
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
        /**
         * Stores a textured quad with pre-transformed vertex positions.
         * Vertices are stored in order: bottom-left, bottom-right, top-right, top-left.
         */
        private record TexturedQuad(
                float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                float u0, float v0, float u1, float v1, int color
        ) {
        }

        /**
         * Stores a colored quad with pre-transformed vertex positions.
         * Vertices are stored in order: bottom-left, bottom-right, top-right, top-left.
         */
        private record ColoredQuad(
                float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                int color
        ) {
        }

        // Current textured batch
        private @Nullable ResourceLocation currentTexture = null;
        private final List<TexturedQuad> texturedQuads = new ArrayList<>();

        // Current colored batch (no texture)
        private final List<ColoredQuad> coloredQuads = new ArrayList<>();

        // Whether we have pending colored quads (they break textured batches)
        private boolean hasColoredPending = false;

        /**
         * Adds a textured quad with pre-transformed vertices.
         *
         * @param x0,y0 bottom-left vertex
         * @param x1,y1 bottom-right vertex
         * @param x2,y2 top-right vertex
         * @param x3,y3 top-left vertex
         */
        void addTextured(ResourceLocation texture,
                         float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3,
                         float u0, float v0, float u1, float v1, int color) {
            // If texture changed, signal that previous batch should be flushed
            if (currentTexture != null && !currentTexture.equals(texture)) {
                // Caller should flush before adding
                return;
            }

            currentTexture = texture;
            texturedQuads.add(new TexturedQuad(x0, y0, x1, y1, x2, y2, x3, y3, u0, v0, u1, v1, color));
        }

        /**
         * Adds a colored quad with pre-transformed vertices.
         *
         * @param x0,y0 bottom-left vertex
         * @param x1,y1 bottom-right vertex
         * @param x2,y2 top-right vertex
         * @param x3,y3 top-left vertex
         */
        void addColored(float x0, float y0, float x1, float y1, float x2, float y2, float x3, float y3, int color) {
            coloredQuads.add(new ColoredQuad(x0, y0, x1, y1, x2, y2, x3, y3, color));
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
            // Transform all 4 corners using current transform
            // Order: bottom-left, bottom-right, top-right, top-left
            float[] bl = transformPointLocal(x, y + height);
            float[] br = transformPointLocal(x + width, y + height);
            float[] tr = transformPointLocal(x + width, y);
            float[] tl = transformPointLocal(x, y);
            batchState.addTextured(texture, bl[0], bl[1], br[0], br[1], tr[0], tr[1], tl[0], tl[1],
                    u0, v0, u1, v1, color);
        }

        @Override
        public void colored(float x, float y, float width, float height, int color) {
            // Auto-flush textured batch if needed
            if (batchState.needsFlushForColored()) {
                flushBatch();
            }
            // Transform all 4 corners using current transform
            // Order: bottom-left, bottom-right, top-right, top-left
            float[] bl = transformPointLocal(x, y + height);
            float[] br = transformPointLocal(x + width, y + height);
            float[] tr = transformPointLocal(x + width, y);
            float[] tl = transformPointLocal(x, y);
            batchState.addColored(bl[0], bl[1], br[0], br[1], tr[0], tr[1], tl[0], tl[1], color);
        }
    };

    public BatchableTexture.VertexEmitter emitter() {
        return batchEmitter;
    }

    /**
     * Flushes all pending batch operations and submits them to the GPU.
     * <p>
     * Also flushes the underlying {@link GuiGraphics} buffer (text drawn via
     * {@code Font.drawInBatch}) and clears the depth buffer to ensure correct
     * draw ordering between layers.
     */
    public void flushBatch() {
        // Submit any buffered text from drawString calls
        graphics.flush();

        if (batchState.isEmpty()) return;

        // Clear depth so batch quads are not occluded by previous text/item depth writes
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);

        applyScissor();
        graphics.pose().pushPose();
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
                int c = quad.color;
                float a = ((c >> 24) & 0xFF) / 255f;
                float r = ((c >> 16) & 0xFF) / 255f;
                float g = ((c >> 8) & 0xFF) / 255f;
                float b = (c & 0xFF) / 255f;

                buffer.addVertex(matrix, quad.x0, quad.y0, 0).setUv(quad.u0, quad.v1).setColor(r, g, b, a); // bottom-left
                buffer.addVertex(matrix, quad.x1, quad.y1, 0).setUv(quad.u1, quad.v1).setColor(r, g, b, a); // bottom-right
                buffer.addVertex(matrix, quad.x2, quad.y2, 0).setUv(quad.u1, quad.v0).setColor(r, g, b, a); // top-right
                buffer.addVertex(matrix, quad.x3, quad.y3, 0).setUv(quad.u0, quad.v0).setColor(r, g, b, a); // top-left
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
                int c = quad.color;
                float a = ((c >> 24) & 0xFF) / 255f;
                float r = ((c >> 16) & 0xFF) / 255f;
                float g = ((c >> 8) & 0xFF) / 255f;
                float b = (c & 0xFF) / 255f;

                buffer.addVertex(matrix, quad.x0, quad.y0, 0).setColor(r, g, b, a);
                buffer.addVertex(matrix, quad.x1, quad.y1, 0).setColor(r, g, b, a);
                buffer.addVertex(matrix, quad.x2, quad.y2, 0).setColor(r, g, b, a);
                buffer.addVertex(matrix, quad.x3, quad.y3, 0).setColor(r, g, b, a);
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

    //region render - drawing API

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
     */
    public SceneCanvas batch(Runnable drawCall) {
        directDraw(drawCall);
        return this;
    }

    public Matrix4f pose() {
        return graphics.pose().last().pose();
    }

    public Matrix4f combinedPose() {
        Matrix4f combined = new Matrix4f(graphics.pose().last().pose());
        combined.mul(localTransform());
        return combined;
    }

    private Matrix4f localTransform() {
        return new Matrix4f(currentTransform);
    }

    //region texture

    public SceneCanvas texture(VisualTexture texture, int x, int y, int width, int height) {
        // Check if texture supports batching
        if (texture instanceof BatchableTexture batchable && batchable.supportsBatching()) {
            batchable.emit(batchEmitter, x, y, width, height, currentColor);
        } else {
            directDraw(() -> {
                if (currentColor != 0xFFFFFFFF) {
                    float a = ((currentColor >> 24) & 0xFF) / 255f;
                    float r = ((currentColor >> 16) & 0xFF) / 255f;
                    float g = ((currentColor >> 8) & 0xFF) / 255f;
                    float b = (currentColor & 0xFF) / 255f;
                    RenderSystem.enableBlend();
                    RenderSystem.setShaderColor(r, g, b, a);
                    texture.render(graphics, x, y, width, height);
                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                } else {
                    texture.render(graphics, x, y, width, height);
                }
            });
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

    //region fill & shape

    public SceneCanvas fill(int x, int y, int width, int height, int color) {
        batchEmitter.colored(x, y, width, height, color);
        return this;
    }

    public SceneCanvas fill(Rect rect, int color) {
        return fill(rect.x(), rect.y(), rect.width(), rect.height(), color);
    }

    /**
     * Fills a rectangle with a four-corner gradient. Not batchable.
     *
     * @param colorTL top-left color (ARGB)
     * @param colorTR top-right color (ARGB)
     * @param colorBL bottom-left color (ARGB)
     * @param colorBR bottom-right color (ARGB)
     */
    public SceneCanvas fillGradient(int x, int y, int width, int height,
                                    int colorTL, int colorTR, int colorBL, int colorBR) {
        directDraw(() -> {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.enableBlend();
            BufferBuilder buffer = Tesselator.getInstance().begin(
                    VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            Matrix4f matrix = graphics.pose().last().pose();
            buffer.addVertex(matrix, x, y + height, 0).setColor(colorBL);
            buffer.addVertex(matrix, x + width, y + height, 0).setColor(colorBR);
            buffer.addVertex(matrix, x + width, y, 0).setColor(colorTR);
            buffer.addVertex(matrix, x, y, 0).setColor(colorTL);
            MeshData meshData = buffer.build();
            if (meshData != null) BufferUploader.drawWithShader(meshData);
        });
        return this;
    }

    /**
     * Fills a vertical gradient (top to bottom). Convenience for {@link #fillGradient(int, int, int, int, int, int, int, int)}.
     */
    public SceneCanvas fillGradient(int x, int y, int width, int height, int colorTop, int colorBottom) {
        return fillGradient(x, y, width, height, colorTop, colorTop, colorBottom, colorBottom);
    }

    /**
     * Strokes a rectangle outline with the given thickness.
     */
    public SceneCanvas strokeRect(int x, int y, int width, int height, int color, int thickness) {
        fill(x, y, width, thickness, color);                                     // top
        fill(x, y + height - thickness, width, thickness, color);                 // bottom
        fill(x, y + thickness, thickness, height - 2 * thickness, color);         // left
        fill(x + width - thickness, y + thickness, thickness, height - 2 * thickness, color); // right
        return this;
    }

    public SceneCanvas strokeRect(int x, int y, int width, int height, int color) {
        return strokeRect(x, y, width, height, color, 1);
    }

    public SceneCanvas strokeRect(Rect rect, int color, int thickness) {
        return strokeRect(rect.x(), rect.y(), rect.width(), rect.height(), color, thickness);
    }

    public SceneCanvas strokeRect(Rect rect, int color) {
        return strokeRect(rect, color, 1);
    }

    /**
     * Draws a straight line between two points with the given thickness. Not batchable.
     */
    public SceneCanvas line(float x1, float y1, float x2, float y2, float thickness, int color) {
        float dx = x2 - x1;
        float dy = y2 - y1;
        float len = (float) Math.sqrt(dx * dx + dy * dy);
        if (len < 1e-6f) return this;
        float nx = -dy / len * thickness * 0.5f;
        float ny = dx / len * thickness * 0.5f;
        directDraw(() -> {
            RenderSystem.setShader(GameRenderer::getPositionColorShader);
            RenderSystem.enableBlend();
            BufferBuilder buffer = Tesselator.getInstance().begin(
                    VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_COLOR);
            Matrix4f matrix = graphics.pose().last().pose();
            buffer.addVertex(matrix, x1 + nx, y1 + ny, 0).setColor(color);
            buffer.addVertex(matrix, x2 + nx, y2 + ny, 0).setColor(color);
            buffer.addVertex(matrix, x2 - nx, y2 - ny, 0).setColor(color);
            buffer.addVertex(matrix, x1 - nx, y1 - ny, 0).setColor(color);
            MeshData meshData = buffer.build();
            if (meshData != null) BufferUploader.drawWithShader(meshData);
        });
        return this;
    }

    public SceneCanvas line(int x1, int y1, int x2, int y2, int color) {
        return line(x1, y1, x2, y2, 1f, color);
    }

    //region shader shapes

    /**
     * Emits a single quad with UV [0,1] mapping for shader-based drawing.
     * Uses {@link DefaultVertexFormat#POSITION_TEX} – all colour / shape
     * parameters are passed through uniforms, not vertex attributes.
     */
    private void drawShaderQuad(float x, float y, float width, float height) {
        RenderSystem.enableBlend();
        BufferBuilder buffer = Tesselator.getInstance().begin(
                VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        Matrix4f matrix = graphics.pose().last().pose();
        buffer.addVertex(matrix, x, y + height, 0).setUv(0, 1);
        buffer.addVertex(matrix, x + width, y + height, 0).setUv(1, 1);
        buffer.addVertex(matrix, x + width, y, 0).setUv(1, 0);
        buffer.addVertex(matrix, x, y, 0).setUv(0, 0);
        MeshData meshData = buffer.build();
        if (meshData != null) BufferUploader.drawWithShader(meshData);
    }

    /**
     * Returns the AA smoothing width in logical pixels, accounting for the
     * current GUI scale so edges remain crisp at any scale factor.
     */
    private static float getSmoothing() {
        return 1.0f / (float) Minecraft.getInstance().getWindow().getGuiScale();
    }

    /**
     * Decomposes an ARGB colour int and sets a vec4 uniform (r, g, b, a).
     */
    @SuppressWarnings("DataFlowIssue")
    private static void setColorUniform(ShaderInstance shader, String name, int argb) {
        shader.getUniform(name).set(
                ((argb >> 16) & 0xFF) / 255f,
                ((argb >> 8) & 0xFF) / 255f,
                (argb & 0xFF) / 255f,
                ((argb >> 24) & 0xFF) / 255f
        );
    }

    //region rounded rectangle

    /**
     * Draws a filled rounded rectangle with uniform corner radius.
     */
    public SceneCanvas roundedRect(int x, int y, int width, int height, float radius, int color) {
        return roundedRect(x, y, width, height, radius, radius, radius, radius, color, 0, 0);
    }

    /**
     * Draws a filled rounded rectangle with per-corner radii.
     */
    public SceneCanvas roundedRect(
            int x, int y, int width, int height,
            float radiusTL, float radiusTR, float radiusBR, float radiusBL,
            int color
    ) {
        return roundedRect(x, y, width, height, radiusTL, radiusTR, radiusBR, radiusBL, color, 0, 0);
    }

    /**
     * Draws a rounded rectangle with optional border (uniform radius).
     *
     * @param fillColor   interior fill colour (ARGB; alpha 0 for border-only)
     * @param borderWidth border thickness in pixels (0 = no border)
     * @param borderColor border colour (ARGB)
     */
    public SceneCanvas roundedRect(
            int x, int y, int width, int height, float radius,
            int fillColor, float borderWidth, int borderColor
    ) {
        return roundedRect(x, y, width, height, radius, radius, radius, radius,
                fillColor, borderWidth, borderColor);
    }

    /**
     * Draws a rounded rectangle with per-corner radii, optional border,
     * and optional texture clipping. All other rounded-rect overloads
     * delegate here. Not batchable.
     */
    public SceneCanvas roundedRect(
            int x, int y, int width, int height,
            float radiusTL, float radiusTR, float radiusBR, float radiusBL,
            int fillColor, float borderWidth, int borderColor
    ) {
        return roundedRectTextured(x, y, width, height,
                radiusTL, radiusTR, radiusBR, radiusBL,
                fillColor, borderWidth, borderColor, null);
    }

    /**
     * Draws a rounded rectangle with a texture bound to Sampler0.
     * The texture is clipped by the SDF shape and modulated by FillColor.
     *
     * @param texture texture to bind (or {@code null} for colour-only)
     */
    public SceneCanvas roundedRectTextured(
            int x, int y, int width, int height, float radius,
            int fillColor, float borderWidth, int borderColor,
            @Nullable ResourceLocation texture
    ) {
        return roundedRectTextured(x, y, width, height, radius, radius, radius, radius,
                fillColor, borderWidth, borderColor, texture);
    }

    /**
     * Most general rounded-rect overload with texture support.
     */
    @SuppressWarnings("DataFlowIssue")
    public SceneCanvas roundedRectTextured(
            int x, int y, int width, int height,
            float radiusTL, float radiusTR, float radiusBR, float radiusBL,
            int fillColor, float borderWidth, int borderColor,
            @Nullable ResourceLocation texture
    ) {
        ShaderInstance shader = CloudShaders.roundedRect();
        if (shader == null) return this;

        directDraw(() -> {
            RenderSystem.setShader(() -> shader);
            setColorUniform(shader, "FillColor", fillColor);
            shader.getUniform("Size").set((float) width, (float) height);
            shader.getUniform("Radii").set(radiusBR, radiusTR, radiusBL, radiusTL);
            shader.getUniform("BorderWidth").set(borderWidth);
            setColorUniform(shader, "BorderColor", borderColor);
            shader.getUniform("Smoothing").set(getSmoothing());
            if (texture != null) {
                shader.getUniform("HasTexture").set(1);
                RenderSystem.setShaderTexture(0, texture);
            } else {
                shader.getUniform("HasTexture").set(0);
            }
            drawShaderQuad(x, y, width, height);
        });
        return this;
    }

    //endregion

    //region circle / ellipse

    /**
     * Draws a filled circle.
     */
    public SceneCanvas circle(float centerX, float centerY, float radius, int color) {
        return ellipse(centerX - radius, centerY - radius, radius * 2, radius * 2, color, 0, 0);
    }

    /**
     * Draws a circle with optional border.
     */
    public SceneCanvas circle(float centerX, float centerY, float radius,
                              int fillColor, float borderWidth, int borderColor) {
        return ellipse(centerX - radius, centerY - radius, radius * 2, radius * 2,
                fillColor, borderWidth, borderColor);
    }

    /**
     * Draws a filled ellipse fitting the given bounding box.
     */
    public SceneCanvas ellipse(float x, float y, float width, float height, int color) {
        return ellipse(x, y, width, height, color, 0, 0);
    }

    /**
     * Draws an ellipse with optional border and optional texture.
     */
    public SceneCanvas ellipse(
            float x, float y, float width, float height,
            int fillColor, float borderWidth, int borderColor
    ) {
        return ellipseTextured(x, y, width, height, fillColor, borderWidth, borderColor, null);
    }

    /**
     * Draws an ellipse with a texture bound to Sampler0.
     */
    @SuppressWarnings("DataFlowIssue")
    public SceneCanvas ellipseTextured(
            float x, float y, float width, float height,
            int fillColor, float borderWidth, int borderColor,
            @Nullable ResourceLocation texture
    ) {
        ShaderInstance shader = CloudShaders.circle();
        if (shader == null) return this;

        directDraw(() -> {
            RenderSystem.setShader(() -> shader);
            setColorUniform(shader, "FillColor", fillColor);
            shader.getUniform("Size").set(width, height);
            shader.getUniform("BorderWidth").set(borderWidth);
            setColorUniform(shader, "BorderColor", borderColor);
            shader.getUniform("Smoothing").set(getSmoothing());
            if (texture != null) {
                shader.getUniform("HasTexture").set(1);
                RenderSystem.setShaderTexture(0, texture);
            } else {
                shader.getUniform("HasTexture").set(0);
            }
            drawShaderQuad(x, y, width, height);
        });
        return this;
    }

    //endregion

    //region bezier curves

    /**
     * Draws a quadratic Bézier curve with uniform colour. Not batchable.
     *
     * @param x0        start X
     * @param y0        start Y
     * @param cx        control point X
     * @param cy        control point Y
     * @param x1        end X
     * @param y1        end Y
     * @param lineWidth line thickness in pixels
     * @param color     stroke colour (ARGB)
     */
    public SceneCanvas bezierQuadratic(
            float x0, float y0, float cx, float cy,
            float x1, float y1, float lineWidth, int color
    ) {
        return bezierInternal(0, x0, y0, cx, cy, x1, y1, x1, y1,
                lineWidth, color, color, 0, 0);
    }

    /**
     * Draws a cubic Bézier curve with uniform colour. Not batchable.
     * <p>
     * For splines with gradient and glow, prefer
     * {@link #bezierCubic(float, float, float, float, float, float, float, float, float, int, int, float, int)}.
     */
    public SceneCanvas bezierCubic(
            float x0, float y0, float cx0, float cy0,
            float cx1, float cy1, float x1, float y1,
            float lineWidth, int color
    ) {
        return bezierInternal(1, x0, y0, cx0, cy0, cx1, cy1, x1, y1,
                lineWidth, color, color, 0, 0);
    }

    /**
     * Draws a cubic Bézier curve with start→end colour gradient and
     * an optional outer glow – useful for node-graph style connection
     * wires. Not batchable.
     *
     * @param lineWidth  line thickness in pixels
     * @param colorStart start colour (ARGB)
     * @param colorEnd   end colour (ARGB)
     * @param glowWidth  glow radius in pixels (0 = off)
     * @param glowColor  glow colour (ARGB)
     */
    public SceneCanvas bezierCubic(
            float x0, float y0, float cx0, float cy0,
            float cx1, float cy1, float x1, float y1,
            float lineWidth,
            int colorStart, int colorEnd,
            float glowWidth, int glowColor
    ) {
        return bezierInternal(1, x0, y0, cx0, cy0, cx1, cy1, x1, y1,
                lineWidth, colorStart, colorEnd, glowWidth, glowColor);
    }

    /**
     * Draws a horizontal spline between two points with automatic
     * tangent computation. Useful for node-graph connection wires.
     */
    public SceneCanvas horizontalSpline(
            float x0, float y0, float x1, float y1,
            float lineWidth,
            int colorStart, int colorEnd,
            float glowWidth, int glowColor
    ) {
        float dx = Math.abs(x1 - x0);
        float tangent = Math.max(dx * 0.5f, 30f);
        return bezierCubic(x0, y0, x0 + tangent, y0, x1 - tangent, y1, x1, y1,
                lineWidth, colorStart, colorEnd, glowWidth, glowColor);
    }

    /**
     * Internal: submits a Bézier shader quad for either quadratic or cubic
     * curves.  Computes the bounding box from control points, remaps them
     * into quad-local pixel coordinates, and sets all uniforms.
     */
    @SuppressWarnings("DataFlowIssue")
    private SceneCanvas bezierInternal(
            int curveType,
            float ax, float ay, float bx, float by,
            float cx, float cy, float dx, float dy,
            float lineWidth,
            int colorStart, int colorEnd,
            float glowWidth, int glowColor
    ) {
        ShaderInstance shader = CloudShaders.bezierCurve();
        if (shader == null) return this;

        float pad = lineWidth + glowWidth + 4f;
        float minX = Math.min(Math.min(ax, bx), Math.min(cx, dx)) - pad;
        float minY = Math.min(Math.min(ay, by), Math.min(cy, dy)) - pad;
        float maxX = Math.max(Math.max(ax, bx), Math.max(cx, dx)) + pad;
        float maxY = Math.max(Math.max(ay, by), Math.max(cy, dy)) + pad;
        float qw = maxX - minX;
        float qh = maxY - minY;

        directDraw(() -> {
            RenderSystem.setShader(() -> shader);
            shader.getUniform("Size").set(qw, qh);
            shader.getUniform("P0").set(ax - minX, ay - minY);
            shader.getUniform("P1").set(bx - minX, by - minY);
            shader.getUniform("P2").set(cx - minX, cy - minY);
            shader.getUniform("P3").set(dx - minX, dy - minY);
            shader.getUniform("CurveType").set(curveType);
            shader.getUniform("LineWidth").set(lineWidth * 0.5f);
            shader.getUniform("Smoothing").set(getSmoothing());
            setColorUniform(shader, "ColorStart", colorStart);
            setColorUniform(shader, "ColorEnd", colorEnd);
            shader.getUniform("GlowWidth").set(glowWidth);
            setColorUniform(shader, "GlowColor", glowColor);
            drawShaderQuad(minX, minY, qw, qh);
        });
        return this;
    }

    //endregion

    //region drop shadow

    /**
     * Draws a soft drop shadow behind a rounded rectangle (uniform radius).
     * Not batchable.
     *
     * @param spread      shadow expansion in pixels
     * @param softness    blur distance in pixels
     * @param shadowColor shadow colour (ARGB)
     */
    public SceneCanvas shadow(
            int x, int y, int width, int height,
            float radius, float spread, float softness, int shadowColor
    ) {
        return shadow(x, y, width, height, radius, radius, radius, radius,
                spread, softness, shadowColor);
    }

    /**
     * Draws a soft drop shadow with per-corner radii.
     */
    @SuppressWarnings("DataFlowIssue")
    public SceneCanvas shadow(
            int x, int y, int width, int height,
            float radiusTL, float radiusTR, float radiusBR, float radiusBL,
            float spread, float softness, int shadowColor
    ) {
        ShaderInstance shader = CloudShaders.shadow();
        if (shader == null) return this;

        float totalSpread = spread + softness;
        float qx = x - totalSpread;
        float qy = y - totalSpread;
        float qw = width + totalSpread * 2;
        float qh = height + totalSpread * 2;

        directDraw(() -> {
            RenderSystem.setShader(() -> shader);
            setColorUniform(shader, "FillColor", shadowColor);
            shader.getUniform("Size").set(qw, qh);
            shader.getUniform("Radii").set(radiusBR, radiusTR, radiusBL, radiusTL);
            shader.getUniform("ShadowSpread").set(totalSpread);
            shader.getUniform("ShadowSoftness").set(softness);
            drawShaderQuad(qx, qy, qw, qh);
        });
        return this;
    }

    //endregion drop shadow

    //endregion shader shapes

    //endregion fill & shape

    //region text

    public SceneCanvas drawString(String text, int x, int y, int color) {
        return drawString(text, x, y, color, false);
    }

    public SceneCanvas drawString(String text, int x, int y, int color, boolean dropShadow) {
        directDraw(() -> graphics.drawString(font(), text, x, y, color, dropShadow));
        return this;
    }

    public SceneCanvas drawString(Component text, int x, int y, int color) {
        return drawString(text, x, y, color, false);
    }

    public SceneCanvas drawString(Component text, int x, int y, int color, boolean dropShadow) {
        directDraw(() -> graphics.drawString(font(), text, x, y, color, dropShadow));
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
        layeredDraw(() -> graphics.renderItem(stack, x, y));
        return this;
    }

    public SceneCanvas renderItemDecorations(ItemStack stack, int x, int y, @Nullable String text) {
        layeredDraw(() -> graphics.renderItemDecorations(font(), stack, x, y, text));
        return this;
    }

    public SceneCanvas renderItemDecorations(ItemStack stack, int x, int y) {
        return renderItemDecorations(stack, x, y, null);
    }

    //endregion

    //region layered

    /**
     * Renders content that changes z-level (items, tooltips, etc.).
     * Clears depth buffer afterward and increments z-offset.
     */
    public SceneCanvas renderLayered(Consumer<GuiGraphics> draw) {
        layeredDraw(() -> draw.accept(graphics));
        return this;
    }

    //endregion

    /**
     * Executes a custom draw operation with transform and clip applied.
     */
    public SceneCanvas render(Consumer<GuiGraphics> draw) {
        directDraw(() -> draw.accept(graphics));
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

    //endregion render

    //region internal helpers

    public Font font() {
        return Minecraft.getInstance().font;
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

    /**
     * Flush → scissor → push pose with local transform + z-offset → action → pop.
     */
    private void directDraw(Runnable action) {
        flushBatch();
        applyScissor();
        graphics.pose().pushPose();
        graphics.pose().mulPose(localTransform());
        graphics.pose().translate(0, 0, zOffset);
        action.run();
        graphics.pose().popPose();
        restoreScissor();
    }

    /**
     * {@link #directDraw} + depth-clear + z-increment (for items / tooltips).
     */
    private void layeredDraw(Runnable action) {
        directDraw(action);
        RenderSystem.clear(GL11.GL_DEPTH_BUFFER_BIT, Minecraft.ON_OSX);
        zOffset += Z_INCREMENT;
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

    public SceneCanvas translate(Pos pos) {
        return translate(pos.x(), pos.y());
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
        return localTransform();
    }

    public float[] transformPoint(int x, int y) {
        return transformPoint((float) x, (float) y);
    }

    public float[] transformPoint(float x, float y) {
        transformTemp.set(x, y, 0, 1);
        localTransform().transform(transformTemp);
        float px = transformTemp.x;
        float py = transformTemp.y;
        Matrix4f matrix = graphics.pose().last().pose();
        transformTemp.set(px, py, 0, 1);
        matrix.transform(transformTemp);
        return new float[]{transformTemp.x, transformTemp.y};
    }

    public float[] transformPointLocal(float x, float y) {
        transformTemp.set(x, y, 0, 1);
        localTransform().transform(transformTemp);
        return new float[]{transformTemp.x, transformTemp.y};
    }

    //endregion

    //region widget rendering

    /**
     * Renders a list of widgets with proper viewport-aware transform handling.
     * <p>
     * Each widget's full viewport forward matrix (including layout position and all
     * user transforms) is applied via the canvas transform stack. The mouse coordinates
     * are transformed through the viewport's inverse matrix so that each widget
     * receives coordinates in its own local space.
     *
     * @param widgets      the widgets to render
     * @param mouseX       relative mouse X (relative to parent)
     * @param mouseY       relative mouse Y (relative to parent)
     * @param partialTicks partial ticks
     * @param <T>          widget type
     */
    public <T extends Widget> void renderWidgets(List<T> widgets, int mouseX, int mouseY, float partialTicks) {
        //noinspection ForLoopReplaceableByForEach
        for (int i = 0; i < widgets.size(); i++) {
            T widget = widgets.get(i);
            if (!widget.shouldRender()) continue;
            Viewport vp = widget.viewport();
            pushViewport(vp);
            var local = vp.parentToLocal(mouseX, mouseY);
            widget.render(this, (int) local.x, (int) local.y, partialTicks);
            popViewport();
        }
    }

    /**
     * Alias for {@link #renderWidgets(List, int, int, float)}.
     */
    public <T extends Widget> void renderChildren(List<T> widgets, int mouseX, int mouseY, float partialTicks) {
        renderWidgets(widgets, mouseX, mouseY, partialTicks);
    }

    //endregion

    //region viewport

    /**
     * Pushes the given viewport's forward matrix onto the canvas transform stack.
     *
     * @param viewport the viewport whose transform to apply
     */
    public SceneCanvas pushViewport(Viewport viewport) {
        pushTransform();
        currentTransform.mul(viewport.toMatrix4f());
        return this;
    }

    /**
     * Pushes only the <em>view</em> portion of the viewport (user transforms, indices 1…n,
     * excludes layout position). Use this when the layout translation is already applied
     * by other means and you only want scroll/zoom/rotation.
     *
     * @param viewport the viewport whose view matrix to apply
     */
    public SceneCanvas pushViewMatrix(Viewport viewport) {
        pushTransform();
        currentTransform.mul(viewport.viewMatrix4f());
        return this;
    }

    /**
     * Pops the viewport transform. This is simply an alias for {@link #popTransform()}
     * for readability.
     */
    public SceneCanvas popViewport() {
        return popTransform();
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
        applyScissor();
    }

    public void disableScissor() {
        restoreScissor();
    }

    //endregion
}
