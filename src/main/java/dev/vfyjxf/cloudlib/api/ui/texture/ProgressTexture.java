package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.gui.GuiGraphics;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Progress texture that clips display based on progress value.
 * <p>
 * The background is optional and may be {@code null}; the foreground is required and is
 * validated on construction.
 * <p>
 * Progress can be sourced from:
 * <ul>
 *   <li>Supplier&lt;Float&gt; - external value</li>
 *   <li>Animation&lt;?&gt; - progress from animation</li>
 * </ul>
 */
public class ProgressTexture implements SizedTexture {

    // region factory

    /**
     * Creates a texture whose foreground fills from left to right.
     *
     * @param bg the background, may be {@code null} for no background
     * @param fg the foreground, must be non-null
     * @param progress the progress source
     */
    public static ProgressTexture horizontal(
        @Nullable VisualTexture bg,
        VisualTexture fg,
        int w,
        int h,
        Supplier<Float> progress
    ) {
        return new ProgressTexture(bg, fg, w, h, Direction.leftToRight, progress);
    }

    /**
     * Creates a texture whose foreground fills from bottom to top.
     *
     * @param bg the background, may be {@code null} for no background
     * @param fg the foreground, must be non-null
     * @param progress the progress source
     */
    public static ProgressTexture vertical(
        @Nullable VisualTexture bg,
        VisualTexture fg,
        int w,
        int h,
        Supplier<Float> progress
    ) {
        return new ProgressTexture(bg, fg, w, h, Direction.bottomToTop, progress);
    }

    /**
     * Creates a texture whose foreground fills from left to right, driven by an animation.
     *
     * @param bg the background, may be {@code null} for no background
     * @param fg the foreground, must be non-null
     */
    public static ProgressTexture horizontal(
        @Nullable VisualTexture bg,
        VisualTexture fg,
        int w,
        int h,
        Animation<?> anim
    ) {
        return new ProgressTexture(bg, fg, w, h, Direction.leftToRight, anim);
    }

    /**
     * Creates a texture whose foreground fills from bottom to top, driven by an animation.
     *
     * @param bg the background, may be {@code null} for no background
     * @param fg the foreground, must be non-null
     */
    public static ProgressTexture vertical(
        @Nullable VisualTexture bg,
        VisualTexture fg,
        int w,
        int h,
        Animation<?> anim
    ) {
        return new ProgressTexture(bg, fg, w, h, Direction.bottomToTop, anim);
    }

    // endregion

    // region types

    public enum Direction {
        leftToRight, rightToLeft, bottomToTop, topToBottom
    }

    // endregion

    // region state

    private final @Nullable VisualTexture background;
    private final VisualTexture foreground;
    private final int width, height;
    private final Direction direction;
    private final Supplier<Float> progressSupplier;

    /**
     * @param background the background, may be {@code null} for no background
     * @param foreground the foreground, must be non-null
     * @param direction the direction the foreground fills in
     * @param progressSupplier the progress source
     */
    public ProgressTexture(
        @Nullable VisualTexture background,
        VisualTexture foreground,
        int width,
        int height,
        Direction direction,
        Supplier<Float> progressSupplier
    ) {
        this.background = background;
        this.foreground = Objects.requireNonNull(foreground, "foreground");
        this.width = width;
        this.height = height;
        this.direction = direction;
        this.progressSupplier = progressSupplier;
    }

    /**
     * Creates with progress from an Animation.
     *
     * @param background the background, may be {@code null} for no background
     * @param foreground the foreground, must be non-null
     * @param direction the direction the foreground fills in
     * @param animation the animation supplying the progress
     */
    public ProgressTexture(
        @Nullable VisualTexture background,
        VisualTexture foreground,
        int width,
        int height,
        Direction direction,
        Animation<?> animation
    ) {
        this(background, foreground, width, height, direction, animation::progress);
    }

    /**
     * Creates with horizontal direction (left to right).
     *
     * @param background the background, may be {@code null} for no background
     * @param foreground the foreground, must be non-null
     * @param progressSupplier the progress source
     */
    public ProgressTexture(
        @Nullable VisualTexture background,
        VisualTexture foreground,
        int width,
        int height,
        Supplier<Float> progressSupplier
    ) {
        this(background, foreground, width, height, Direction.leftToRight, progressSupplier);
    }

    // endregion

    // region query

    @Override
    public int width() {
        return width;
    }

    @Override
    public int height() {
        return height;
    }

    // endregion

    // region rendering

    @Override
    public void render(GuiGraphics graphics, int x, int y, int width, int height) {
        if (background != null) {
            background.render(graphics, x, y, width, height);
        }

        float progress = Math.max(0, Math.min(1, progressSupplier.get()));
        if (progress <= 0) return;

        int clipLeft = 0, clipRight = 0, clipTop = 0, clipBottom = 0;

        switch (direction) {
            case leftToRight -> clipRight = (int) ((1 - progress) * width);
            case rightToLeft -> clipLeft = (int) ((1 - progress) * width);
            case bottomToTop -> clipTop = (int) ((1 - progress) * height);
            case topToBottom -> clipBottom = (int) ((1 - progress) * height);
        }

        if (clipLeft + clipRight < width && clipTop + clipBottom < height) {
            graphics.enableScissor(x + clipLeft, y + clipTop, x + width - clipRight, y + height - clipBottom);
            foreground.render(graphics, x, y, width, height);
            graphics.disableScissor();
        }
    }

    // endregion
}
