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
 * Tiled texture that repeats to fill the given area.
 * <p>
 * Can operate in two modes:
 * <ul>
 *   <li>Standard mode - renders from a standalone texture file</li>
 *   <li>Atlas sprite mode - renders from GUI sprite atlas using blitSprite</li>
 * </ul>
 */
public record TiledTexture(
    ResourceLocation texture,
    int tileWidth, int tileHeight,
    int textureWidth, int textureHeight,
    TileMode horizontalMode, TileMode verticalMode,
    boolean atlasSprite
) implements BatchableTexture {

    public enum TileMode {
        REPEAT,
        STRETCH,
        CLAMP
    }

    //region factory

    /**
     * Creates a standard tiled texture.
     */
    public static TiledTexture of(ResourceLocation texture, int tileWidth, int tileHeight) {
        return new TiledTexture(texture, tileWidth, tileHeight, tileWidth, tileHeight,
            TileMode.REPEAT, TileMode.REPEAT, false);
    }

    /**
     * Creates a standard tiled texture with custom texture size.
     */
    public static TiledTexture of(ResourceLocation texture, int tileWidth, int tileHeight,
                                  int textureWidth, int textureHeight) {
        return new TiledTexture(texture, tileWidth, tileHeight, textureWidth, textureHeight,
            TileMode.REPEAT, TileMode.REPEAT, false);
    }

    /**
     * Creates a standard horizontally tiled texture.
     */
    public static TiledTexture horizontal(ResourceLocation texture, int tileWidth, int tileHeight) {
        return new TiledTexture(texture, tileWidth, tileHeight, tileWidth, tileHeight,
            TileMode.REPEAT, TileMode.STRETCH, false);
    }

    /**
     * Creates a standard vertically tiled texture.
     */
    public static TiledTexture vertical(ResourceLocation texture, int tileWidth, int tileHeight) {
        return new TiledTexture(texture, tileWidth, tileHeight, tileWidth, tileHeight,
            TileMode.STRETCH, TileMode.REPEAT, false);
    }

    /**
     * Creates an atlas sprite tiled texture.
     * The texture should be a sprite location registered in the GUI atlas.
     */
    public static TiledTexture sprite(ResourceLocation spriteLocation, int tileWidth, int tileHeight) {
        return new TiledTexture(spriteLocation, tileWidth, tileHeight, tileWidth, tileHeight,
            TileMode.REPEAT, TileMode.REPEAT, true);
    }

    /**
     * Creates an atlas sprite horizontally tiled texture.
     */
    public static TiledTexture spriteHorizontal(ResourceLocation spriteLocation, int tileWidth, int tileHeight) {
        return new TiledTexture(spriteLocation, tileWidth, tileHeight, tileWidth, tileHeight,
            TileMode.REPEAT, TileMode.STRETCH, true);
    }

    /**
     * Creates an atlas sprite vertically tiled texture.
     */
    public static TiledTexture spriteVertical(ResourceLocation spriteLocation, int tileWidth, int tileHeight) {
        return new TiledTexture(spriteLocation, tileWidth, tileHeight, tileWidth, tileHeight,
            TileMode.STRETCH, TileMode.REPEAT, true);
    }

    //endregion

    //region modification

    public TiledTexture withModes(TileMode horizontal, TileMode vertical) {
        return new TiledTexture(texture, tileWidth, tileHeight, textureWidth, textureHeight,
            horizontal, vertical, atlasSprite);
    }

    public TiledTexture withTileSize(int width, int height) {
        return new TiledTexture(texture, width, height, textureWidth, textureHeight,
            horizontalMode, verticalMode, atlasSprite);
    }

    /**
     * Returns whether this texture is an atlas sprite.
     */
    public boolean isAtlasSprite() {
        return atlasSprite;
    }

    /**
     * Converts this texture to an atlas sprite texture.
     */
    public TiledTexture asAtlasSprite() {
        return atlasSprite ? this : new TiledTexture(texture, tileWidth, tileHeight, textureWidth, textureHeight,
            horizontalMode, verticalMode, true);
    }

    /**
     * Converts this texture to a standard texture.
     */
    public TiledTexture asStandardTexture() {
        return atlasSprite ? new TiledTexture(texture, tileWidth, tileHeight, textureWidth, textureHeight,
            horizontalMode, verticalMode, false) : this;
    }

    //endregion

    //region batchable texture

    @Override
    public void emit(VertexEmitter emitter, float x, float y, float width, float height, int tint) {
        if (atlasSprite) {
            // For atlas sprites, get the actual sprite and calculate UV coordinates
            var minecraft = Minecraft.getInstance();
            var guiSprites = minecraft.getGuiSprites();
            TextureAtlasSprite sprite = guiSprites.getSprite(texture);
            ResourceLocation atlasLocation = sprite.atlasLocation();

            float spriteU0 = sprite.getU0();
            float spriteV0 = sprite.getV0();
            float spriteU1 = sprite.getU1();
            float spriteV1 = sprite.getV1();

            if (horizontalMode == TileMode.STRETCH && verticalMode == TileMode.STRETCH) {
                emitter.textured(atlasLocation, x, y, width, height, spriteU0, spriteV0, spriteU1, spriteV1, tint);
                return;
            }

            int tilesX = horizontalMode == TileMode.REPEAT ? (int) Math.ceil(width / tileWidth) : 1;
            int tilesY = verticalMode == TileMode.REPEAT ? (int) Math.ceil(height / tileHeight) : 1;
            float effW = horizontalMode == TileMode.STRETCH ? width : tileWidth;
            float effH = verticalMode == TileMode.STRETCH ? height : tileHeight;

            for (int ty = 0; ty < tilesY; ty++) {
                for (int tx = 0; tx < tilesX; tx++) {
                    float tileX = x + tx * effW;
                    float tileY = y + ty * effH;
                    float actualW = Math.min(effW, x + width - tileX);
                    float actualH = Math.min(effH, y + height - tileY);
                    if (actualW <= 0 || actualH <= 0) continue;

                    // Calculate UV for partial tiles
                    float tileU1 = spriteU0 + (spriteU1 - spriteU0) * (actualW / effW);
                    float tileV1 = spriteV0 + (spriteV1 - spriteV0) * (actualH / effH);
                    emitter.textured(atlasLocation, tileX, tileY, actualW, actualH, spriteU0, spriteV0, tileU1, tileV1, tint);
                }
            }
        } else {
            float u1 = (float) tileWidth / textureWidth;
            float v1 = (float) tileHeight / textureHeight;

            if (horizontalMode == TileMode.STRETCH && verticalMode == TileMode.STRETCH) {
                emitter.textured(texture, x, y, width, height, 0, 0, u1, v1, tint);
                return;
            }

            int tilesX = horizontalMode == TileMode.REPEAT ? (int) Math.ceil(width / tileWidth) : 1;
            int tilesY = verticalMode == TileMode.REPEAT ? (int) Math.ceil(height / tileHeight) : 1;
            float effW = horizontalMode == TileMode.STRETCH ? width : tileWidth;
            float effH = verticalMode == TileMode.STRETCH ? height : tileHeight;

            for (int ty = 0; ty < tilesY; ty++) {
                for (int tx = 0; tx < tilesX; tx++) {
                    float tileX = x + tx * effW;
                    float tileY = y + ty * effH;
                    float actualW = Math.min(effW, x + width - tileX);
                    float actualH = Math.min(effH, y + height - tileY);
                    if (actualW <= 0 || actualH <= 0) continue;

                    float tileU1 = u1 * (actualW / effW);
                    float tileV1 = v1 * (actualH / effH);
                    emitter.textured(texture, tileX, tileY, actualW, actualH, 0, 0, tileU1, tileV1, tint);
                }
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        if (atlasSprite) {
            // For atlas sprites, get sprite and render manually with UV coordinates
            var minecraft = Minecraft.getInstance();
            var guiSprites = minecraft.getGuiSprites();
            TextureAtlasSprite sprite = guiSprites.getSprite(texture);

            if (horizontalMode == TileMode.STRETCH && verticalMode == TileMode.STRETCH) {
                graphics.blit(x, y, 0, width, height, sprite);
                return;
            }

            float spriteU0 = sprite.getU0();
            float spriteV0 = sprite.getV0();
            float spriteU1 = sprite.getU1();
            float spriteV1 = sprite.getV1();
            float spriteWidth = spriteU1 - spriteU0;
            float spriteHeight = spriteV1 - spriteV0;

            int tilesX = horizontalMode == TileMode.REPEAT ? (int) Math.ceil((float) width / tileWidth) : 1;
            int tilesY = verticalMode == TileMode.REPEAT ? (int) Math.ceil((float) height / tileHeight) : 1;
            int effW = horizontalMode == TileMode.STRETCH ? width : tileWidth;
            int effH = verticalMode == TileMode.STRETCH ? height : tileHeight;

            RenderSystem.setShaderTexture(0, sprite.atlasLocation());
            RenderSystem.setShader(GameRenderer::getPositionTexShader);
            Matrix4f matrix = graphics.pose().last().pose();
            BufferBuilder buffer = Tesselator.getInstance().begin(VertexFormat.Mode.QUADS, DefaultVertexFormat.POSITION_TEX);

            for (int ty = 0; ty < tilesY; ty++) {
                for (int tx = 0; tx < tilesX; tx++) {
                    int tileXPos = x + tx * effW;
                    int tileYPos = y + ty * effH;
                    int actualW = Math.min(effW, x + width - tileXPos);
                    int actualH = Math.min(effH, y + height - tileYPos);
                    if (actualW <= 0 || actualH <= 0) continue;

                    // Calculate UV for partial tiles
                    float tileU1 = spriteU0 + spriteWidth * ((float) actualW / tileWidth);
                    float tileV1 = spriteV0 + spriteHeight * ((float) actualH / tileHeight);

                    buffer.addVertex(matrix, tileXPos, tileYPos, 0).setUv(spriteU0, spriteV0);
                    buffer.addVertex(matrix, tileXPos, tileYPos + actualH, 0).setUv(spriteU0, tileV1);
                    buffer.addVertex(matrix, tileXPos + actualW, tileYPos + actualH, 0).setUv(tileU1, tileV1);
                    buffer.addVertex(matrix, tileXPos + actualW, tileYPos, 0).setUv(tileU1, spriteV0);
                }
            }

            BufferUploader.drawWithShader(buffer.buildOrThrow());
        } else {
            if (horizontalMode == TileMode.STRETCH && verticalMode == TileMode.STRETCH) {
                graphics.blit(texture, x, y, 0, 0, width, height, textureWidth, textureHeight);
                return;
            }

            int tilesX = horizontalMode == TileMode.REPEAT ? (int) Math.ceil((float) width / tileWidth) : 1;
            int tilesY = verticalMode == TileMode.REPEAT ? (int) Math.ceil((float) height / tileHeight) : 1;
            int effW = horizontalMode == TileMode.STRETCH ? width : tileWidth;
            int effH = verticalMode == TileMode.STRETCH ? height : tileHeight;

            for (int ty = 0; ty < tilesY; ty++) {
                for (int tx = 0; tx < tilesX; tx++) {
                    int tileX = x + tx * effW;
                    int tileY = y + ty * effH;
                    int actualW = Math.min(effW, x + width - tileX);
                    int actualH = Math.min(effH, y + height - tileY);
                    if (actualW <= 0 || actualH <= 0) continue;
                    graphics.blit(texture, tileX, tileY, 0, 0, actualW, actualH, textureWidth, textureHeight);
                }
            }
        }
    }

    //endregion
}
