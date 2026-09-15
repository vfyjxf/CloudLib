package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Circle/ellipse texture with horizontal strip approximation.
 */
public record CircleTexture(
        int fillColor, int borderColor, int borderThickness,
        int segments, boolean filled
) implements BatchableTexture {

    public CircleTexture {
        segments = Math.max(8, segments);
    }

    //region factory

    public static CircleTexture of(int fillColor) {
        return new CircleTexture(fillColor, 0, 0, 32, true);
    }

    public static CircleTexture of(int fillColor, int segments) {
        return new CircleTexture(fillColor, 0, 0, segments, true);
    }

    public static CircleTexture withBorder(int fillColor, int borderColor, int borderThickness) {
        return new CircleTexture(fillColor, borderColor, borderThickness, 32, true);
    }

    public static CircleTexture ring(int color, int thickness) {
        return new CircleTexture(0, color, thickness, 32, false);
    }

    public static CircleTexture ring(int color, int thickness, int segments) {
        return new CircleTexture(0, color, thickness, segments, false);
    }

    //endregion

    //region modification

    public CircleTexture withBorder(int color, int thickness) {
        return new CircleTexture(fillColor, color, thickness, segments, filled);
    }

    public CircleTexture withColor(int color) {
        return new CircleTexture(color, borderColor, borderThickness, segments, filled);
    }

    public CircleTexture withSegments(int segments) {
        return new CircleTexture(fillColor, borderColor, borderThickness, segments, filled);
    }

    //endregion

    //region batchable texture

    @Override
    public void emit(VertexEmitter emitter, float x, float y, float width, float height, int tint) {
        float cx = x + width / 2;
        float cy = y + height / 2;
        float rx = width / 2;
        float ry = height / 2;
        int color = tint != -1 ? tint : fillColor;

        if (filled && color != 0) {
            emitFilledEllipse(emitter, cx, cy, rx, ry, color);
        }
        if (borderThickness > 0 && borderColor != 0) {
            emitEllipseRing(emitter, cx, cy, rx, ry);
        }
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        int cx = x + width / 2;
        int cy = y + height / 2;
        int rx = width / 2;
        int ry = height / 2;

        if (filled && fillColor != 0) {
            for (int i = -ry; i <= ry; i++) {
                float normalizedY = (float) i / ry;
                float xExtent = (float) Math.sqrt(Math.max(0, 1 - normalizedY * normalizedY));
                int halfWidth = (int) (xExtent * rx);
                if (halfWidth > 0) {
                    graphics.fill(cx - halfWidth, cy + i, cx + halfWidth, cy + i + 1, fillColor);
                }
            }
        }
        if (borderThickness > 0 && borderColor != 0) {
            for (int i = 0; i < segments * 2; i++) {
                float angle = (float) (i * Math.PI * 2 / (segments * 2));
                int px = (int) (cx + Math.cos(angle) * rx);
                int py = (int) (cy + Math.sin(angle) * ry);
                graphics.fill(px, py, px + borderThickness, py + borderThickness, borderColor);
            }
        }
    }

    //endregion

    //region internal

    private void emitFilledEllipse(VertexEmitter emitter, float cx, float cy, float rx, float ry, int color) {
        for (int i = 0; i < segments; i++) {
            float t0 = (float) i / segments;
            float t1 = (float) (i + 1) / segments;
            float y0 = -1 + 2 * t0;
            float y1 = -1 + 2 * t1;
            float stripY = cy + (y0 + y1) / 2 * ry;
            float normalizedY = (y0 + y1) / 2;
            float xExtent = (float) Math.sqrt(Math.max(0, 1 - normalizedY * normalizedY));
            float stripWidth = xExtent * rx * 2;
            float stripHeight = (t1 - t0) * ry * 2;
            if (stripWidth > 0.1f) {
                emitter.colored(cx - xExtent * rx, stripY - stripHeight / 2, stripWidth, stripHeight, color);
            }
        }
    }

    private void emitEllipseRing(VertexEmitter emitter, float cx, float cy, float rx, float ry) {
        float angleStep = (float) (Math.PI * 2) / (segments * 2);
        for (int i = 0; i < segments * 2; i++) {
            float angle = i * angleStep;
            float nextAngle = (i + 1) * angleStep;
            float ox1 = cx + (float) Math.cos(angle) * rx;
            float oy1 = cy + (float) Math.sin(angle) * ry;
            float ox2 = cx + (float) Math.cos(nextAngle) * rx;
            float oy2 = cy + (float) Math.sin(nextAngle) * ry;

            float minX = Math.min(ox1, ox2);
            float maxX = Math.max(ox1, ox2);
            float minY = Math.min(oy1, oy2);
            float maxY = Math.max(oy1, oy2);
            float w = Math.max(maxX - minX, borderThickness);
            float h = Math.max(maxY - minY, borderThickness);
            emitter.colored(minX, minY, w, h, borderColor);
        }
    }

    //endregion
}
