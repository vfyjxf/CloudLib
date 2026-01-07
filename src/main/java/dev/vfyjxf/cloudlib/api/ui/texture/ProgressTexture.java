package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Supplier;

/**
 * Progress texture that clips display based on progress value.
 * <p>
 * Progress can be sourced from:
 * <ul>
 *   <li>Supplier&lt;Float&gt; - external value</li>
 *   <li>Animation&lt;?&gt; - progress from animation</li>
 * </ul>
 */
public class ProgressTexture implements SizedTexture {

    public enum Direction {
        LEFT_TO_RIGHT,
        RIGHT_TO_LEFT,
        BOTTOM_TO_TOP,
        TOP_TO_BOTTOM
    }

    private final Texture background;
    private final Texture foreground;
    private final int width, height;
    private final Direction direction;
    private final Supplier<Float> progressSupplier;

    public ProgressTexture(Texture background, Texture foreground, int width, int height,
                           Direction direction, Supplier<Float> progressSupplier) {
        this.background = background;
        this.foreground = foreground;
        this.width = width;
        this.height = height;
        this.direction = direction;
        this.progressSupplier = progressSupplier;
    }

    /**
     * Creates with progress from an Animation.
     */
    public ProgressTexture(Texture background, Texture foreground, int width, int height,
                           Direction direction, Animation<?> animation) {
        this(background, foreground, width, height, direction, animation::progress);
    }

    /**
     * Creates with horizontal direction (left to right).
     */
    public ProgressTexture(Texture background, Texture foreground, int width, int height,
                           Supplier<Float> progressSupplier) {
        this(background, foreground, width, height, Direction.LEFT_TO_RIGHT, progressSupplier);
    }

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        if (background != null) {
            background.render(graphics, x, y, width, height);
        }

        float progress = Math.max(0, Math.min(1, progressSupplier.get()));
        if (progress <= 0) return;

        int clipLeft = 0, clipRight = 0, clipTop = 0, clipBottom = 0;

        switch (direction) {
            case LEFT_TO_RIGHT -> clipRight = (int) ((1 - progress) * width);
            case RIGHT_TO_LEFT -> clipLeft = (int) ((1 - progress) * width);
            case BOTTOM_TO_TOP -> clipTop = (int) ((1 - progress) * height);
            case TOP_TO_BOTTOM -> clipBottom = (int) ((1 - progress) * height);
        }

        if (clipLeft + clipRight < width && clipTop + clipBottom < height) {
            graphics.enableScissor(x + clipLeft, y + clipTop, x + width - clipRight, y + height - clipBottom);
            foreground.render(graphics, x, y, width, height);
            graphics.disableScissor();
        }
    }

    public static ProgressTexture horizontal(Texture bg, Texture fg, int w, int h, Supplier<Float> progress) {
        return new ProgressTexture(bg, fg, w, h, Direction.LEFT_TO_RIGHT, progress);
    }

    public static ProgressTexture vertical(Texture bg, Texture fg, int w, int h, Supplier<Float> progress) {
        return new ProgressTexture(bg, fg, w, h, Direction.BOTTOM_TO_TOP, progress);
    }

    public static ProgressTexture horizontal(Texture bg, Texture fg, int w, int h, Animation<?> anim) {
        return new ProgressTexture(bg, fg, w, h, Direction.LEFT_TO_RIGHT, anim);
    }

    public static ProgressTexture vertical(Texture bg, Texture fg, int w, int h, Animation<?> anim) {
        return new ProgressTexture(bg, fg, w, h, Direction.BOTTOM_TO_TOP, anim);
    }
}
