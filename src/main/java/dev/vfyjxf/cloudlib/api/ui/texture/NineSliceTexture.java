package dev.vfyjxf.cloudlib.api.ui.texture;

import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.BufferBuilder;
import com.mojang.blaze3d.vertex.BufferUploader;
import com.mojang.blaze3d.vertex.DefaultVertexFormat;
import com.mojang.blaze3d.vertex.Tesselator;
import com.mojang.blaze3d.vertex.VertexFormat;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.renderer.GameRenderer;
import net.minecraft.client.renderer.texture.TextureAtlasSprite;
import net.minecraft.resources.ResourceLocation;
import org.joml.Matrix4f;

/**
 * Nine-slice texture that scales while preserving corner regions.
 * <p>
 * The texture is divided into 9 regions:
 * <pre>
 * ┌─────┬───────────┬─────┐
 * │ TL  │    Top    │ TR  │
 * ├─────┼───────────┼─────┤
 * │Left │  Center   │Right│
 * ├─────┼───────────┼─────┤
 * │ BL  │  Bottom   │ BR  │
 * └─────┴───────────┴─────┘
 * </pre>
 * Corners maintain their original size, edges stretch in one direction,
 * and the center stretches in both directions.
 * <p>
 * Can operate in two modes:
 * <ul>
 *   <li>Standard mode - renders from a standalone texture file</li>
 *   <li>Atlas sprite mode - renders from GUI sprite atlas using blitSprite</li>
 * </ul>
 */
public record NineSliceTexture(
    ResourceLocation location, int width, int height,
    int left, int right, int top, int bottom,
    boolean atlasSprite
) implements SizedTexture, BatchableTexture {

    /**
     * Creates a nine-slice texture with uniform border (standard mode).
     */
    public NineSliceTexture(ResourceLocation location, int width, int height, int border) {
        this(location, width, height, border, border, border, border, false);
    }

    /**
     * Creates a nine-slice texture with custom border sizes (standard mode).
     */
    public NineSliceTexture(ResourceLocation location, int width, int height, int left, int right, int top, int bottom) {
        this(location, width, height, left, right, top, bottom, false);
    }

    //region factory

    /**
     * Creates a standard nine-slice texture with uniform border.
     */
    public static NineSliceTexture of(ResourceLocation location, int width, int height, int border) {
        return new NineSliceTexture(location, width, height, border, border, border, border, false);
    }

    /**
     * Creates a standard nine-slice texture with custom borders.
     */
    public static NineSliceTexture of(ResourceLocation location, int width, int height, int left, int right, int top, int bottom) {
        return new NineSliceTexture(location, width, height, left, right, top, bottom, false);
    }

    /**
     * Creates an atlas sprite nine-slice texture with uniform border.
     * The location should be a sprite location registered in the GUI atlas.
     */
    public static NineSliceTexture sprite(ResourceLocation spriteLocation, int width, int height, int border) {
        return new NineSliceTexture(spriteLocation, width, height, border, border, border, border, true);
    }

    /**
     * Creates an atlas sprite nine-slice texture with custom borders.
     * The location should be a sprite location registered in the GUI atlas.
     */
    public static NineSliceTexture sprite(ResourceLocation spriteLocation, int width, int height, int left, int right, int top, int bottom) {
        return new NineSliceTexture(spriteLocation, width, height, left, right, top, bottom, true);
    }

    //endregion

    /**
     * Creates a nine-slice texture with custom border sizes.
     *
     * @param location    texture resource location
     * @param width       texture width in pixels
     * @param height      texture height in pixels
     * @param left        left border width
     * @param right       right border width
     * @param top         top border height
     * @param bottom      bottom border height
     * @param atlasSprite whether this is an atlas sprite
     */
    public NineSliceTexture {
    }

    //endregion

    //region rendering

    @Override
    public void render(GuiGraphics graphics, int x, int y, int w, int h) {
        if (atlasSprite) {
            // For atlas sprites, get the actual sprite and manually render nine-slice
            var minecraft = Minecraft.getInstance();
            var guiSprites = minecraft.getGuiSprites();
            TextureAtlasSprite sprite = guiSprites.getSprite(location);

            int centerWidth = w - left - right;
            int centerHeight = h - top - bottom;

            // Calculate UV coordinates from sprite
            float spriteU0 = sprite.getU0();
            float spriteV0 = sprite.getV0();
            float spriteU1 = sprite.getU1();
            float spriteV1 = sprite.getV1();
            float spriteWidth = spriteU1 - spriteU0;
            float spriteHeight = spriteV1 - spriteV0;

            // UV coordinates for nine-slice regions
            float uLeft = spriteU0 + spriteWidth * ((float) left / width);
            float uRight = spriteU0 + spriteWidth * ((float) (width - right) / width);
            float vTop = spriteV0 + spriteHeight * ((float) top / height);
            float vBottom = spriteV0 + spriteHeight * ((float) (height - bottom) / height);

            // Top row
            innerBlit(graphics, sprite.atlasLocation(), x, y, left, top, spriteU0, spriteV0, uLeft, vTop);
            innerBlit(graphics, sprite.atlasLocation(), x + left, y, centerWidth, top, uLeft, spriteV0, uRight, vTop);
            innerBlit(graphics, sprite.atlasLocation(), x + w - right, y, right, top, uRight, spriteV0, spriteU1, vTop);

            // Middle row
            innerBlit(graphics, sprite.atlasLocation(), x, y + top, left, centerHeight, spriteU0, vTop, uLeft, vBottom);
            innerBlit(graphics, sprite.atlasLocation(), x + left, y + top, centerWidth, centerHeight, uLeft, vTop, uRight, vBottom);
            innerBlit(graphics, sprite.atlasLocation(), x + w - right, y + top, right, centerHeight, uRight, vTop, spriteU1, vBottom);

            // Bottom row
            innerBlit(graphics, sprite.atlasLocation(), x, y + h - bottom, left, bottom, spriteU0, vBottom, uLeft, spriteV1);
            innerBlit(graphics, sprite.atlasLocation(), x + left, y + h - bottom, centerWidth, bottom, uLeft, vBottom, uRight, spriteV1);
            innerBlit(graphics, sprite.atlasLocation(), x + w - right, y + h - bottom, right, bottom, uRight, vBottom, spriteU1, spriteV1);
        } else {
            // Fallback to individual blit calls for standard textures
            int centerWidth = w - left - right;
            int centerHeight = h - top - bottom;
            int texCenterW = width - left - right;
            int texCenterH = height - top - bottom;

            // Top row
            graphics.blit(location, x, y, left, top, 0, 0, left, top, width, height);
            graphics.blit(location, x + left, y, centerWidth, top, left, 0, texCenterW, top, width, height);
            graphics.blit(location, x + w - right, y, right, top, width - right, 0, right, top, width, height);

            // Middle row
            graphics.blit(location, x, y + top, left, centerHeight, 0, top, left, texCenterH, width, height);
            graphics.blit(location, x + left, y + top, centerWidth, centerHeight, left, top, texCenterW, texCenterH, width, height);
            graphics.blit(location, x + w - right, y + top, right, centerHeight, width - right, top, right, texCenterH, width, height);

            // Bottom row
            graphics.blit(location, x, y + h - bottom, left, bottom, 0, height - bottom, left, bottom, width, height);
            graphics.blit(location, x + left, y + h - bottom, centerWidth, bottom, left, height - bottom, texCenterW, bottom, width, height);
            graphics.blit(location, x + w - right, y + h - bottom, right, bottom, width - right, height - bottom, right, bottom, width, height);
        }
    }

    private static void innerBlit(GuiGraphics graphics, ResourceLocation texture, int x, int y, int width, int height, float u0, float v0, float u1, float v1) {
        RenderSystem.setShaderTexture(0, texture);
        RenderSystem.setShader(GameRenderer::getPositionTexShader);
        Matrix4f matrix = graphics.pose().last().pose();
        BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);
        buffer.addVertex(matrix, x, y, 0).setUv(u0, v0);
        buffer.addVertex(matrix, x, y + height, 0).setUv(u0, v1);
        buffer.addVertex(matrix, x + width, y + height, 0).setUv(u1, v1);
        buffer.addVertex(matrix, x + width, y, 0).setUv(u1, v0);
        BufferUploader.drawWithShader(buffer.buildOrThrow());
    }

    //endregion

    //region batchable texture

    @Override
    public void emit(VertexEmitter emitter, float x, float y, float w, float h, int color) {
        if (atlasSprite) {
            // For atlas sprites, get the actual sprite and calculate UV coordinates
            var minecraft = Minecraft.getInstance();
            var guiSprites = minecraft.getGuiSprites();
            TextureAtlasSprite sprite = guiSprites.getSprite(location);
            ResourceLocation atlasLocation = sprite.atlasLocation();

            float centerWidth = w - left - right;
            float centerHeight = h - top - bottom;

            // Calculate UV coordinates from sprite
            float spriteU0 = sprite.getU0();
            float spriteV0 = sprite.getV0();
            float spriteU1 = sprite.getU1();
            float spriteV1 = sprite.getV1();
            float spriteWidth = spriteU1 - spriteU0;
            float spriteHeight = spriteV1 - spriteV0;

            // UV coordinates for nine-slice regions
            float uLeft = spriteU0 + spriteWidth * ((float) left / width);
            float uRight = spriteU0 + spriteWidth * ((float) (width - right) / width);
            float vTop = spriteV0 + spriteHeight * ((float) top / height);
            float vBottom = spriteV0 + spriteHeight * ((float) (height - bottom) / height);

            // Top row
            emitter.textured(atlasLocation, x, y, left, top, spriteU0, spriteV0, uLeft, vTop, color);
            emitter.textured(atlasLocation, x + left, y, centerWidth, top, uLeft, spriteV0, uRight, vTop, color);
            emitter.textured(atlasLocation, x + w - right, y, right, top, uRight, spriteV0, spriteU1, vTop, color);

            // Middle row
            emitter.textured(atlasLocation, x, y + top, left, centerHeight, spriteU0, vTop, uLeft, vBottom, color);
            emitter.textured(atlasLocation, x + left, y + top, centerWidth, centerHeight, uLeft, vTop, uRight, vBottom, color);
            emitter.textured(atlasLocation, x + w - right, y + top, right, centerHeight, uRight, vTop, spriteU1, vBottom, color);

            // Bottom row
            emitter.textured(atlasLocation, x, y + h - bottom, left, bottom, spriteU0, vBottom, uLeft, spriteV1, color);
            emitter.textured(atlasLocation, x + left, y + h - bottom, centerWidth, bottom, uLeft, vBottom, uRight, spriteV1, color);
            emitter.textured(atlasLocation, x + w - right, y + h - bottom, right, bottom, uRight, vBottom, spriteU1, spriteV1, color);
        } else {
            float centerWidth = w - left - right;
            float centerHeight = h - top - bottom;

            // UV coordinates
            float uLeft = (float) left / width;
            float uRight = (float) (width - right) / width;
            float vTop = (float) top / height;
            float vBottom = (float) (height - bottom) / height;

            // Top row
            emitter.textured(location, x, y, left, top, 0, 0, uLeft, vTop, color);
            emitter.textured(location, x + left, y, centerWidth, top, uLeft, 0, uRight, vTop, color);
            emitter.textured(location, x + w - right, y, right, top, uRight, 0, 1, vTop, color);

            // Middle row
            emitter.textured(location, x, y + top, left, centerHeight, 0, vTop, uLeft, vBottom, color);
            emitter.textured(location, x + left, y + top, centerWidth, centerHeight, uLeft, vTop, uRight, vBottom, color);
            emitter.textured(location, x + w - right, y + top, right, centerHeight, uRight, vTop, 1, vBottom, color);

            // Bottom row
            emitter.textured(location, x, y + h - bottom, left, bottom, 0, vBottom, uLeft, 1, color);
            emitter.textured(location, x + left, y + h - bottom, centerWidth, bottom, uLeft, vBottom, uRight, 1, color);
            emitter.textured(location, x + w - right, y + h - bottom, right, bottom, uRight, vBottom, 1, 1, color);
        }
    }

    //endregion

    //region modification

    /**
     * Converts this texture to an atlas sprite texture.
     */
    public NineSliceTexture asAtlasSprite() {
        return atlasSprite ? this : new NineSliceTexture(location, width, height, left, right, top, bottom, true);
    }

    /**
     * Converts this texture to a standard texture.
     */
    public NineSliceTexture asStandardTexture() {
        return atlasSprite ? new NineSliceTexture(location, width, height, left, right, top, bottom, false) : this;
    }

    //endregion
}
