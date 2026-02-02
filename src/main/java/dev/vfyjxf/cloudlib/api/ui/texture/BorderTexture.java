package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Rectangular border texture with configurable sides.
 */
public record BorderTexture(
    int colorTop, int colorRight, int colorBottom, int colorLeft,
    int thicknessTop, int thicknessRight, int thicknessBottom, int thicknessLeft
) implements BatchableTexture {

    // region Factory

    public static BorderTexture of(int color, int thickness) {
        return new BorderTexture(color, color, color, color, thickness, thickness, thickness, thickness);
    }

    public static BorderTexture of(int color, int top, int right, int bottom, int left) {
        return new BorderTexture(color, color, color, color, top, right, bottom, left);
    }

    public static BorderTexture sides(int cTop, int cRight, int cBottom, int cLeft,
                                      int tTop, int tRight, int tBottom, int tLeft) {
        return new BorderTexture(cTop, cRight, cBottom, cLeft, tTop, tRight, tBottom, tLeft);
    }

    public static BorderTexture sides(int cTop, int cRight, int cBottom, int cLeft, int thickness) {
        return new BorderTexture(cTop, cRight, cBottom, cLeft, thickness, thickness, thickness, thickness);
    }

    public static BorderTexture horizontal(int color, int thickness) {
        return new BorderTexture(color, 0, color, 0, thickness, 0, thickness, 0);
    }

    public static BorderTexture vertical(int color, int thickness) {
        return new BorderTexture(0, color, 0, color, 0, thickness, 0, thickness);
    }

    public static BorderTexture top(int color, int thickness) {
        return new BorderTexture(color, 0, 0, 0, thickness, 0, 0, 0);
    }

    public static BorderTexture bottom(int color, int thickness) {
        return new BorderTexture(0, 0, color, 0, 0, 0, thickness, 0);
    }

    public static BorderTexture left(int color, int thickness) {
        return new BorderTexture(0, 0, 0, color, 0, 0, 0, thickness);
    }

    public static BorderTexture right(int color, int thickness) {
        return new BorderTexture(0, color, 0, 0, 0, thickness, 0, 0);
    }

    // endregion

    // region Modification

    public BorderTexture withColor(int color) {
        return new BorderTexture(color, color, color, color,
            thicknessTop, thicknessRight, thicknessBottom, thicknessLeft);
    }

    public BorderTexture withThickness(int thickness) {
        return new BorderTexture(colorTop, colorRight, colorBottom, colorLeft,
            thickness, thickness, thickness, thickness);
    }

    public BorderTexture withAlpha(int alpha) {
        return new BorderTexture(
            setAlpha(colorTop, alpha), setAlpha(colorRight, alpha),
            setAlpha(colorBottom, alpha), setAlpha(colorLeft, alpha),
            thicknessTop, thicknessRight, thicknessBottom, thicknessLeft
        );
    }

    private static int setAlpha(int color, int alpha) {
        return (color & 0x00FFFFFF) | ((alpha & 0xFF) << 24);
    }

    // endregion

    // region Query

    public boolean isEmpty() {
        return thicknessTop == 0 && thicknessRight == 0 && thicknessBottom == 0 && thicknessLeft == 0;
    }

    public boolean isUniformColor() {
        return colorTop == colorRight && colorTop == colorBottom && colorTop == colorLeft;
    }

    public boolean isUniformThickness() {
        return thicknessTop == thicknessRight && thicknessTop == thicknessBottom && thicknessTop == thicknessLeft;
    }

    // endregion

    // region BatchableTexture

    @Override
    public void emit(VertexEmitter emitter, float x, float y, float width, float height, int tint) {
        if (thicknessTop > 0) {
            emitter.colored(x, y, width, thicknessTop, colorTop);
        }
        if (thicknessBottom > 0) {
            emitter.colored(x, y + height - thicknessBottom, width, thicknessBottom, colorBottom);
        }
        float innerY = y + thicknessTop;
        float innerHeight = height - thicknessTop - thicknessBottom;
        if (innerHeight > 0) {
            if (thicknessLeft > 0) {
                emitter.colored(x, innerY, thicknessLeft, innerHeight, colorLeft);
            }
            if (thicknessRight > 0) {
                emitter.colored(x + width - thicknessRight, innerY, thicknessRight, innerHeight, colorRight);
            }
        }
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        if (thicknessTop > 0) {
            graphics.fill(x, y, x + width, y + thicknessTop, colorTop);
        }
        if (thicknessBottom > 0) {
            graphics.fill(x, y + height - thicknessBottom, x + width, y + height, colorBottom);
        }
        int innerY = y + thicknessTop;
        int innerHeight = height - thicknessTop - thicknessBottom;
        if (innerHeight > 0) {
            if (thicknessLeft > 0) {
                graphics.fill(x, innerY, x + thicknessLeft, innerY + innerHeight, colorLeft);
            }
            if (thicknessRight > 0) {
                graphics.fill(x + width - thicknessRight, innerY, x + width, innerY + innerHeight, colorRight);
            }
        }
    }

    // endregion
}
