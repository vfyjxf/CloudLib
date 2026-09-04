package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Linear gradient texture using strip-based rendering.
 */
public record GradientTexture(
        int colorTopLeft, int colorTopRight,
        int colorBottomLeft, int colorBottomRight,
        int segments
) implements BatchableTexture {

    public GradientTexture {
        segments = Math.max(1, segments);
    }

    //region factory

    public static GradientTexture horizontal(int colorLeft, int colorRight) {
        return new GradientTexture(colorLeft, colorRight, colorLeft, colorRight, 16);
    }

    public static GradientTexture horizontal(int colorLeft, int colorRight, int segments) {
        return new GradientTexture(colorLeft, colorRight, colorLeft, colorRight, segments);
    }

    public static GradientTexture vertical(int colorTop, int colorBottom) {
        return new GradientTexture(colorTop, colorTop, colorBottom, colorBottom, 16);
    }

    public static GradientTexture vertical(int colorTop, int colorBottom, int segments) {
        return new GradientTexture(colorTop, colorTop, colorBottom, colorBottom, segments);
    }

    public static GradientTexture diagonal(int topLeft, int topRight, int bottomLeft, int bottomRight) {
        return new GradientTexture(topLeft, topRight, bottomLeft, bottomRight, 16);
    }

    public static GradientTexture diagonal(int topLeft, int topRight, int bottomLeft, int bottomRight, int segments) {
        return new GradientTexture(topLeft, topRight, bottomLeft, bottomRight, segments);
    }

    //endregion

    //region batchable texture

    @Override
    public void emit(VertexEmitter emitter, float x, float y, float width, float height, int tint) {
        boolean isHorizontal = colorTopLeft == colorBottomLeft && colorTopRight == colorBottomRight;
        boolean isVertical = colorTopLeft == colorTopRight && colorBottomLeft == colorBottomRight;

        if (isHorizontal) {
            emitHorizontal(emitter, x, y, width, height);
        } else if (isVertical) {
            emitVertical(emitter, x, y, width, height);
        } else {
            emitDiagonal(emitter, x, y, width, height);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        boolean isVertical = colorTopLeft == colorTopRight && colorBottomLeft == colorBottomRight;
        if (isVertical) {
            graphics.fillGradient(x, y, x + width, y + height, colorTopLeft, colorBottomLeft);
        } else {
            renderStrips(graphics, x, y, width, height);
        }
    }

    //endregion

    //region internal

    private void emitHorizontal(VertexEmitter emitter, float x, float y, float width, float height) {
        float segmentWidth = width / segments;
        for (int i = 0; i < segments; i++) {
            float t = (i + 0.5f) / segments;
            int color = lerpColor(colorTopLeft, colorTopRight, t);
            emitter.colored(x + i * segmentWidth, y, segmentWidth, height, color);
        }
    }

    private void emitVertical(VertexEmitter emitter, float x, float y, float width, float height) {
        float segmentHeight = height / segments;
        for (int i = 0; i < segments; i++) {
            float t = (i + 0.5f) / segments;
            int color = lerpColor(colorTopLeft, colorBottomLeft, t);
            emitter.colored(x, y + i * segmentHeight, width, segmentHeight, color);
        }
    }

    private void emitDiagonal(VertexEmitter emitter, float x, float y, float width, float height) {
        float segmentHeight = height / segments;
        float segmentWidth = width / segments;
        for (int i = 0; i < segments; i++) {
            float ty = (i + 0.5f) / segments;
            int leftColor = lerpColor(colorTopLeft, colorBottomLeft, ty);
            int rightColor = lerpColor(colorTopRight, colorBottomRight, ty);
            for (int j = 0; j < segments; j++) {
                float tx = (j + 0.5f) / segments;
                int color = lerpColor(leftColor, rightColor, tx);
                emitter.colored(x + j * segmentWidth, y + i * segmentHeight, segmentWidth, segmentHeight, color);
            }
        }
    }

    private void renderStrips(GuiGraphics graphics, int x, int y, int width, int height) {
        boolean isHorizontal = colorTopLeft == colorBottomLeft && colorTopRight == colorBottomRight;
        if (isHorizontal) {
            for (int i = 0; i < segments; i++) {
                float t = (i + 0.5f) / segments;
                int color = lerpColor(colorTopLeft, colorTopRight, t);
                int sx = x + (i * width / segments);
                int ex = x + ((i + 1) * width / segments);
                graphics.fill(sx, y, ex, y + height, color);
            }
        } else {
            for (int i = 0; i < segments; i++) {
                float ty = (i + 0.5f) / segments;
                int leftColor = lerpColor(colorTopLeft, colorBottomLeft, ty);
                int rightColor = lerpColor(colorTopRight, colorBottomRight, ty);
                for (int j = 0; j < segments; j++) {
                    float tx = (j + 0.5f) / segments;
                    int color = lerpColor(leftColor, rightColor, tx);
                    graphics.fill(
                            x + (j * width / segments), y + (i * height / segments),
                            x + ((j + 1) * width / segments), y + ((i + 1) * height / segments),
                            color
                    );
                }
            }
        }
    }

    private static int lerpColor(int c1, int c2, float t) {
        int a = (int) (((c1 >> 24) & 0xFF) + (((c2 >> 24) & 0xFF) - ((c1 >> 24) & 0xFF)) * t);
        int r = (int) (((c1 >> 16) & 0xFF) + (((c2 >> 16) & 0xFF) - ((c1 >> 16) & 0xFF)) * t);
        int g = (int) (((c1 >> 8) & 0xFF) + (((c2 >> 8) & 0xFF) - ((c1 >> 8) & 0xFF)) * t);
        int b = (int) ((c1 & 0xFF) + ((c2 & 0xFF) - (c1 & 0xFF)) * t);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    //endregion
}
