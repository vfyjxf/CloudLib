package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.resources.ResourceLocation;

/**
 * Image texture - the most commonly used texture type.
 * <p>
 * Supports batch rendering via {@link BatchableTexture}.
 */
public record ImageTexture(
    ResourceLocation location,
    int u, int v,
    int width, int height,
    int textureWidth, int textureHeight
) implements SizedTexture, BatchableTexture {

    public ImageTexture(ResourceLocation location, int width, int height) {
        this(location, 0, 0, width, height, width, height);
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        graphics.blit(location, x, y, width, height, u, v, this.width, this.height, textureWidth, textureHeight);
    }

    /**
     * Creates a sub-region texture.
     */
    public ImageTexture subTexture(int u, int v, int width, int height) {
        return new ImageTexture(location, this.u + u, this.v + v, width, height, textureWidth, textureHeight);
    }

    // ==================== BatchableTexture Implementation ====================

    @Override
    public void emit(VertexEmitter emitter, float x, float y, float w, float h, int color) {
        float u0 = (float) this.u / textureWidth;
        float v0 = (float) this.v / textureHeight;
        float u1 = (float) (this.u + width) / textureWidth;
        float v1 = (float) (this.v + height) / textureHeight;
        emitter.textured(location, x, y, w, h, u0, v0, u1, v1, color);
    }
}
