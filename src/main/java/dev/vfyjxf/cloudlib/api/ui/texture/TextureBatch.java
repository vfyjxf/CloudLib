package dev.vfyjxf.cloudlib.api.ui.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import it.unimi.dsi.fastutil.objects.Object2ObjectLinkedOpenHashMap;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.function.Consumer;

/**
 * Batch renderer for textures - combines multiple draw calls for better performance.
 * <p>
 * Supports all textures implementing {@link BatchableTexture}, and provides
 * specialized methods for sprite batch rendering.
 * <p>
 * Usage:
 * <pre>{@code
 * TextureBatch batch = new TextureBatch();
 * batch.draw(imageTexture, x1, y1, w1, h1);
 * batch.drawSprite(spriteLocation, x2, y2, w2, h2);
 * batch.draw(imageTexture, x3, y3, w3, h3); // same texture will be batched
 * batch.end(graphics);
 * }</pre>
 */
public class TextureBatch implements BatchableTexture.BatchCollector {

    public static TextureBatch create() {
        return new TextureBatch();
    }

    private record Quad(int x, int y, int width, int height, float u0, float v0, float u1, float v1, int color) {}

    private final Map<ResourceLocation, List<Quad>> batches = new Object2ObjectLinkedOpenHashMap<>();
    private final List<Consumer<GuiGraphics>> customDraws = new ArrayList<>();
    private boolean building = true;
    private int currentColor = 0xFFFFFFFF;

    private TextureBatch() {}

    /**
     * Sets the color for subsequent draws.
     */
    public TextureBatch color(int argb) {
        this.currentColor = argb;
        return this;
    }

    /**
     * Resets the color to white.
     */
    public TextureBatch resetColor() {
        this.currentColor = 0xFFFFFFFF;
        return this;
    }

    @Override
    public void addQuad(ResourceLocation texture, int x, int y, int width, int height,
                        float u0, float v0, float u1, float v1, int color) {
        batches.computeIfAbsent(texture, k -> new ArrayList<>())
               .add(new Quad(x, y, width, height, u0, v0, u1, v1, color));
    }

    /**
     * Adds a batchable texture draw.
     */
    public TextureBatch draw(BatchableTexture texture, int x, int y, int width, int height) {
        checkBuilding();
        if (texture.supportsBatching()) {
            texture.addToBatch(this, x, y, width, height, currentColor);
        } else {
            // Draw non-batchable textures separately
            final int fx = x, fy = y, fw = width, fh = height;
            customDraws.add(graphics -> texture.render(graphics, fx, fy, fw, fh));
        }
        return this;
    }

    /**
     * Adds a batchable SizedTexture draw using its intrinsic dimensions.
     */
    public <T extends BatchableTexture & SizedTexture> TextureBatch draw(T texture, int x, int y) {
        return draw(texture, x, y, texture.width(), texture.height());
    }

    /**
     * Adds a generic texture draw.
     * <p>
     * BatchableTexture instances are batched, others are drawn separately.
     */
    public TextureBatch draw(UITexture texture, int x, int y, int width, int height) {
        checkBuilding();
        if (texture instanceof BatchableTexture batchable && batchable.supportsBatching()) {
            batchable.addToBatch(this, x, y, width, height, currentColor);
        } else {
            final int fx = x, fy = y, fw = width, fh = height;
            final int color = currentColor;
            customDraws.add(graphics -> {
                if (color != 0xFFFFFFFF) {
                    float a = ((color >> 24) & 0xFF) / 255f;
                    float r = ((color >> 16) & 0xFF) / 255f;
                    float g = ((color >> 8) & 0xFF) / 255f;
                    float b = (color & 0xFF) / 255f;
                    RenderSystem.enableBlend();
                    RenderSystem.setShaderColor(r, g, b, a);
                    texture.render(graphics, fx, fy, fw, fh);
                    RenderSystem.setShaderColor(1f, 1f, 1f, 1f);
                } else {
                    texture.render(graphics, fx, fy, fw, fh);
                }
            });
        }
        return this;
    }

    /**
     * Adds a SizedTexture draw.
     */
    public TextureBatch draw(SizedTexture texture, int x, int y) {
        return draw(texture, x, y, texture.width(), texture.height());
    }

    /**
     * Adds a raw quad with normalized UV coordinates.
     */
    public TextureBatch drawRaw(
        ResourceLocation texture, int x, int y, int width, int height,
        float u0, float v0, float u1, float v1
    ) {
        checkBuilding();
        addQuad(texture, x, y, width, height, u0, v0, u1, v1, currentColor);
        return this;
    }

    /**
     * Adds a raw quad with pixel UV coordinates.
     */
    public TextureBatch drawRaw(
        ResourceLocation texture, int x, int y, int width, int height,
        int u, int v, int regionWidth, int regionHeight,
        int textureWidth, int textureHeight
    ) {
        float u0 = (float) u / textureWidth;
        float v0 = (float) v / textureHeight;
        float u1 = (float) (u + regionWidth) / textureWidth;
        float v1 = (float) (v + regionHeight) / textureHeight;
        return drawRaw(texture, x, y, width, height, u0, v0, u1, v1);
    }

    /**
     * Adds a sprite draw using its ResourceLocation.
     * <p>
     * Note: This uses deferred rendering via GuiGraphics.blitSprite and cannot
     * be batched with other quads. For true batching, use {@link #drawSprite(TextureAtlasSprite, int, int, int, int)}.
     */
    public TextureBatch drawSprite(ResourceLocation spriteLocation, int x, int y, int width, int height) {
        checkBuilding();
        var minecraft = Minecraft.getInstance();
        var guiSprites = minecraft.getGuiSprites();
        var sprite = guiSprites.getSprite(spriteLocation);
        return drawSprite(sprite, x, y, width, height);

    }

    /**
     * Adds a sprite draw from a TextureAtlasSprite directly.
     */
    public TextureBatch drawSprite(TextureAtlasSprite sprite, int x, int y, int width, int height) {
        checkBuilding();
        addQuad(sprite.atlasLocation(), x, y, width, height,
            sprite.getU0(), sprite.getV0(), sprite.getU1(), sprite.getV1(), currentColor);
        return this;
    }

    /**
     * Ends batch collection and executes rendering.
     */
    public void end(GuiGraphics graphics) {
        checkBuilding();
        building = false;

        Matrix4f matrix = graphics.pose().last().pose();

        // Batch render quads with the same texture
        for (var entry : batches.entrySet()) {
            ResourceLocation location = entry.getKey();
            List<Quad> quads = entry.getValue();

            if (quads.isEmpty()) continue;

            RenderSystem.setShaderTexture(0, location);
            RenderSystem.setShader(GameRenderer::getPositionTexColorShader);
            RenderSystem.enableBlend();

            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX_COLOR);

            for (Quad quad : quads) {
                float x0 = quad.x;
                float y0 = quad.y;
                float x1 = quad.x + quad.width;
                float y1 = quad.y + quad.height;

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

            BufferUploader.drawWithShader(buffer.buildOrThrow());
        }

        // Execute custom draws that cannot be batched
        for (Consumer<GuiGraphics> draw : customDraws) {
            draw.accept(graphics);
        }

        batches.clear();
        customDraws.clear();
    }

    /**
     * Returns the current number of collected quads.
     */
    public int quadCount() {
        int count = 0;
        for (List<Quad> quads : batches.values()) {
            count += quads.size();
        }
        return count;
    }

    /**
     * Returns the number of different textures (i.e., draw call count).
     */
    public int batchCount() {
        return batches.size() + customDraws.size();
    }

    private void checkBuilding() {
        if (!building) {
            throw new IllegalStateException("Batch already ended");
        }
    }
}
