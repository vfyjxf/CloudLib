package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Tiled texture that repeats to fill the given area.
 */
public record TiledTexture(
    ResourceLocation texture,
    int tileWidth, int tileHeight,
    int textureWidth, int textureHeight,
    TileMode horizontalMode, TileMode verticalMode
) implements BatchableTexture {

    public enum TileMode {
        REPEAT,
        STRETCH,
        CLAMP
    }

    // region Factory

    public static TiledTexture of(ResourceLocation texture, int tileWidth, int tileHeight) {
        return new TiledTexture(texture, tileWidth, tileHeight, tileWidth, tileHeight,
            TileMode.REPEAT, TileMode.REPEAT);
    }

    public static TiledTexture of(ResourceLocation texture, int tileWidth, int tileHeight,
                                  int textureWidth, int textureHeight) {
        return new TiledTexture(texture, tileWidth, tileHeight, textureWidth, textureHeight,
            TileMode.REPEAT, TileMode.REPEAT);
    }

    public static TiledTexture horizontal(ResourceLocation texture, int tileWidth, int tileHeight) {
        return new TiledTexture(texture, tileWidth, tileHeight, tileWidth, tileHeight,
            TileMode.REPEAT, TileMode.STRETCH);
    }

    public static TiledTexture vertical(ResourceLocation texture, int tileWidth, int tileHeight) {
        return new TiledTexture(texture, tileWidth, tileHeight, tileWidth, tileHeight,
            TileMode.STRETCH, TileMode.REPEAT);
    }

    // endregion

    // region Modification

    public TiledTexture withModes(TileMode horizontal, TileMode vertical) {
        return new TiledTexture(texture, tileWidth, tileHeight, textureWidth, textureHeight,
            horizontal, vertical);
    }

    public TiledTexture withTileSize(int width, int height) {
        return new TiledTexture(texture, width, height, textureWidth, textureHeight,
            horizontalMode, verticalMode);
    }

    // endregion

    // region BatchableTexture

    @Override
    public void emit(VertexEmitter emitter, float x, float y, float width, float height, int tint) {
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

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
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

    // endregion
}
