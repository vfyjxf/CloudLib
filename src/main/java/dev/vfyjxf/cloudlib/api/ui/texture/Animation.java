package dev.vfyjxf.cloudlib.api.ui.texture;

import net.minecraft.client.gui.GuiGraphics;

/**
 * Animation interface that produces time-varying values.
 * <p>
 * Animations are independent of textures and can produce any type of value (Texture, Float, Color, etc.).
 *
 * @param <T> the type of value this animation produces
 */
public interface Animation<T> {

    /**
     * Returns the current progress (0.0 to 1.0).
     * <p>
     * This is the raw linear progress, unaffected by easing.
     */
    float progress();

    /**
     * Sets the progress.
     *
     * @param progress the progress value, will be clamped to [0, 1]
     */
    void setProgress(float progress);

    /**
     * Returns the current value.
     */
    T value();

    /**
     * Returns the value with frame interpolation.
     * <p>
     * Used for smooth rendering between ticks.
     *
     * @param partialTick interpolation factor (0.0 to 1.0)
     * @return the interpolated value
     */
    default T value(float partialTick) {
        return value();
    }

    /**
     * Returns the easing function.
     */
    default Easing easing() {
        return Easing.LINEAR;
    }

    /**
     * Returns whether this animation has completed.
     */
    default boolean isComplete() {
        return progress() >= 1.0f;
    }

    /**
     * Returns the progress with easing applied.
     */
    default float easedProgress() {
        return easing().apply(progress());
    }

    /**
     * Returns the progress with easing and frame interpolation applied.
     */
    default float easedProgress(float partialTick, float prevProgress) {
        float interpolatedProgress = prevProgress + (progress() - prevProgress) * partialTick;
        return easing().apply(interpolatedProgress);
    }

    /**
     * Renders the animation if the value is a Texture.
     */
    default void render(GuiGraphics graphics, int x, int y, int width, int height, float partialTick) {
        T val = value(partialTick);
        if (val instanceof UITexture texture) {
            texture.render(graphics, x, y, width, height);
        }
    }

    /**
     * Renders without interpolation.
     */
    default void render(GuiGraphics graphics, int x, int y, int width, int height) {
        render(graphics, x, y, width, height, 1.0f);
    }
}
