package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Shadow texture using multi-layer alpha falloff.
 */
public record ShadowTexture(
        int color, int blur,
        int offsetX, int offsetY,
        int layers
) implements BatchableTexture {

    public ShadowTexture {
        blur = Math.max(1, blur);
        layers = Math.max(1, layers);
    }

    //region factory

    public static ShadowTexture of(int color, int blur) {
        return new ShadowTexture(color, blur, blur / 2, blur / 2, blur);
    }

    public static ShadowTexture of(int color, int blur, int offsetX, int offsetY) {
        return new ShadowTexture(color, blur, offsetX, offsetY, blur);
    }

    public static ShadowTexture glow(int color, int radius) {
        return new ShadowTexture(color, radius, 0, 0, radius);
    }

    public static ShadowTexture inner(int color, int blur) {
        return new ShadowTexture(color, blur, 0, 0, blur);
    }

    //endregion

    //region modification

    public ShadowTexture withOffset(int x, int y) {
        return new ShadowTexture(color, blur, x, y, layers);
    }

    public ShadowTexture withBlur(int blur) {
        return new ShadowTexture(color, blur, offsetX, offsetY, blur);
    }

    public ShadowTexture withColor(int color) {
        return new ShadowTexture(color, blur, offsetX, offsetY, layers);
    }

    //endregion

    //region batchable texture

    @Override
    public void emit(VertexEmitter emitter, float x, float y, float width, float height, int tint) {
        int baseAlpha = (color >> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;

        for (int i = layers - 1; i >= 0; i--) {
            float t = (float) i / layers;
            int layerAlpha = (int) (baseAlpha * (1 - t * t));
            int layerColor = (layerAlpha << 24) | rgb;

            float expand = i;
            emitter.colored(
                    x + offsetX - expand, y + offsetY - expand,
                    width + expand * 2, height + expand * 2,
                    layerColor
            );
        }
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        int baseAlpha = (color >> 24) & 0xFF;
        int rgb = color & 0x00FFFFFF;

        for (int i = layers - 1; i >= 0; i--) {
            float t = (float) i / layers;
            int layerAlpha = (int) (baseAlpha * (1 - t * t));
            int layerColor = (layerAlpha << 24) | rgb;

            int expand = i;
            graphics.fill(
                    x + offsetX - expand, y + offsetY - expand,
                    x + offsetX + width + expand, y + offsetY + height + expand,
                    layerColor
            );
        }
    }

    //endregion
}
