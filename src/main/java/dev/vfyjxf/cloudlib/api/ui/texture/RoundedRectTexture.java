package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Rounded rectangle texture with corner approximation.
 */
public record RoundedRectTexture(
    int fillColor,
    int radiusTopLeft, int radiusTopRight,
    int radiusBottomLeft, int radiusBottomRight,
    int borderColor, int borderThickness,
    int segments
) implements BatchableTexture {

    public RoundedRectTexture {
        segments = Math.max(4, segments);
    }

    //region factory

    public static RoundedRectTexture of(int fillColor, int radius) {
        return new RoundedRectTexture(fillColor, radius, radius, radius, radius, 0, 0, 8);
    }

    public static RoundedRectTexture of(int fillColor, int radius, int segments) {
        return new RoundedRectTexture(fillColor, radius, radius, radius, radius, 0, 0, segments);
    }

    public static RoundedRectTexture of(int fillColor, int topLeft, int topRight, int bottomLeft, int bottomRight) {
        return new RoundedRectTexture(fillColor, topLeft, topRight, bottomLeft, bottomRight, 0, 0, 8);
    }

    public static RoundedRectTexture withBorder(int fillColor, int radius, int borderColor, int borderThickness) {
        return new RoundedRectTexture(fillColor, radius, radius, radius, radius, borderColor, borderThickness, 8);
    }

    //endregion

    //region modification

    public RoundedRectTexture withBorder(int color, int thickness) {
        return new RoundedRectTexture(fillColor, radiusTopLeft, radiusTopRight,
            radiusBottomLeft, radiusBottomRight, color, thickness, segments);
    }

    public RoundedRectTexture withColor(int color) {
        return new RoundedRectTexture(color, radiusTopLeft, radiusTopRight,
            radiusBottomLeft, radiusBottomRight, borderColor, borderThickness, segments);
    }

    public RoundedRectTexture withSegments(int segments) {
        return new RoundedRectTexture(fillColor, radiusTopLeft, radiusTopRight,
            radiusBottomLeft, radiusBottomRight, borderColor, borderThickness, segments);
    }

    //endregion

    //region batchable texture

    @Override
    public void emit(VertexEmitter emitter, float x, float y, float width, float height, int tint) {
        int color = tint != -1 ? tint : fillColor;
        float maxRadius = Math.min(width, height) / 2;
        float rTL = Math.min(radiusTopLeft, maxRadius);
        float rTR = Math.min(radiusTopRight, maxRadius);
        float rBL = Math.min(radiusBottomLeft, maxRadius);
        float rBR = Math.min(radiusBottomRight, maxRadius);

        // Main body
        float topInset = Math.max(rTL, rTR);
        float bottomInset = Math.max(rBL, rBR);
        float middleHeight = height - topInset - bottomInset;
        if (middleHeight > 0) {
            emitter.colored(x, y + topInset, width, middleHeight, color);
        }

        // Top bar
        float topBarWidth = width - rTL - rTR;
        if (topBarWidth > 0 && topInset > 0) {
            emitter.colored(x + rTL, y, topBarWidth, topInset, color);
        }

        // Bottom bar
        float bottomBarWidth = width - rBL - rBR;
        if (bottomBarWidth > 0 && bottomInset > 0) {
            emitter.colored(x + rBL, y + height - bottomInset, bottomBarWidth, bottomInset, color);
        }

        // Corners
        if (rTL > 0) emitCorner(emitter, x + rTL, y + rTL, rTL, color, Corner.TOP_LEFT);
        if (rTR > 0) emitCorner(emitter, x + width - rTR, y + rTR, rTR, color, Corner.TOP_RIGHT);
        if (rBL > 0) emitCorner(emitter, x + rBL, y + height - rBL, rBL, color, Corner.BOTTOM_LEFT);
        if (rBR > 0) emitCorner(emitter, x + width - rBR, y + height - rBR, rBR, color, Corner.BOTTOM_RIGHT);

        // Border
        if (borderThickness > 0 && borderColor != 0) {
            emitBorder(emitter, x, y, width, height, rTL, rTR, rBL, rBR);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        float maxRadius = Math.min(width, height) / 2f;
        int r = (int) Math.min(Math.max(radiusTopLeft, Math.max(radiusTopRight,
            Math.max(radiusBottomLeft, radiusBottomRight))), maxRadius);

        graphics.fill(x + r, y, x + width - r, y + height, fillColor);
        graphics.fill(x, y + r, x + width, y + height - r, fillColor);

        fillCornerFallback(graphics, x, y, r, fillColor, true, true);
        fillCornerFallback(graphics, x + width - r, y, r, fillColor, false, true);
        fillCornerFallback(graphics, x, y + height - r, r, fillColor, true, false);
        fillCornerFallback(graphics, x + width - r, y + height - r, r, fillColor, false, false);

        if (borderThickness > 0 && borderColor != 0) {
            graphics.fill(x + r, y, x + width - r, y + borderThickness, borderColor);
            graphics.fill(x + r, y + height - borderThickness, x + width - r, y + height, borderColor);
            graphics.fill(x, y + r, x + borderThickness, y + height - r, borderColor);
            graphics.fill(x + width - borderThickness, y + r, x + width, y + height - r, borderColor);
        }
    }

    //endregion

    //region internal

    private enum Corner {TOP_LEFT, TOP_RIGHT, BOTTOM_LEFT, BOTTOM_RIGHT}

    private void emitCorner(VertexEmitter emitter, float cx, float cy, float radius, int color, Corner corner) {
        float startAngle = switch (corner) {
            case TOP_LEFT -> (float) Math.PI;
            case TOP_RIGHT -> (float) (Math.PI * 1.5);
            case BOTTOM_LEFT -> (float) (Math.PI * 0.5);
            case BOTTOM_RIGHT -> 0;
        };
        float angleStep = (float) (Math.PI / 2) / segments;

        for (int i = 0; i < segments; i++) {
            float angle1 = startAngle + i * angleStep;
            float angle2 = startAngle + (i + 1) * angleStep;

            float x1 = cx + (float) Math.cos(angle1) * radius;
            float y1 = cy + (float) Math.sin(angle1) * radius;
            float x2 = cx + (float) Math.cos(angle2) * radius;
            float y2 = cy + (float) Math.sin(angle2) * radius;

            float minX = Math.min(Math.min(x1, x2), cx);
            float maxX = Math.max(Math.max(x1, x2), cx);
            float minY = Math.min(Math.min(y1, y2), cy);
            float maxY = Math.max(Math.max(y1, y2), cy);

            float segWidth = maxX - minX;
            float segHeight = maxY - minY;
            if (segWidth > 0.1f && segHeight > 0.1f) {
                emitter.colored(minX, minY, segWidth, segHeight, color);
            }
        }
    }

    private void emitBorder(VertexEmitter emitter, float x, float y, float width, float height,
                            float rTL, float rTR, float rBL, float rBR) {
        float t = borderThickness;
        if (x + width - rTR > x + rTL) {
            emitter.colored(x + rTL, y, width - rTL - rTR, t, borderColor);
        }
        if (x + width - rBR > x + rBL) {
            emitter.colored(x + rBL, y + height - t, width - rBL - rBR, t, borderColor);
        }
        if (y + height - rBL > y + rTL) {
            emitter.colored(x, y + rTL, t, height - rTL - rBL, borderColor);
        }
        if (y + height - rBR > y + rTR) {
            emitter.colored(x + width - t, y + rTR, t, height - rTR - rBR, borderColor);
        }
    }

    private void fillCornerFallback(GuiGraphics graphics, int x, int y, int r, int color, boolean left, boolean top) {
        for (int i = 0; i < r; i++) {
            int dy = top ? i : (r - 1 - i);
            int arcWidth = (int) Math.sqrt(r * r - (r - i - 0.5) * (r - i - 0.5));
            int startX = left ? (x + r - arcWidth) : x;
            int endX = left ? (x + r) : (x + arcWidth);
            graphics.fill(startX, y + dy, endX, y + dy + 1, color);
        }
    }

    //endregion
}
