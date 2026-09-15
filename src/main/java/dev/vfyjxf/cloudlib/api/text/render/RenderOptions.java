package dev.vfyjxf.cloudlib.api.text.render;

import org.jetbrains.annotations.Contract;

/**
 * Per-frame rendering context for rich text: defaults that apply when a fragment
 * does not specify its own styling, plus the mouse position (in the same local
 * space the laid-out text is rendered at) for interactive effects like
 * mouse-following entities.
 *
 * @param defaultColor the fallback text color (ARGB) for fragments without an
 *                     explicit vanilla style color
 * @param shadow       the fallback drop-shadow flag for text fragments
 * @param mouseX       local mouse X
 * @param mouseY       local mouse Y
 * @param partialTicks frame partial ticks (entity rendering)
 */
public record RenderOptions(
        int defaultColor,
        boolean shadow,
        float mouseX,
        float mouseY,
        float partialTicks
) {

    public static final RenderOptions DEFAULT = new RenderOptions(0xFFFFFFFF, false, 0, 0, 1.0f);

    @Contract("_ -> new")
    public RenderOptions withDefaultColor(int defaultColor) {
        return new RenderOptions(defaultColor, shadow, mouseX, mouseY, partialTicks);
    }

    @Contract("_ -> new")
    public RenderOptions withShadow(boolean shadow) {
        return new RenderOptions(defaultColor, shadow, mouseX, mouseY, partialTicks);
    }

    @Contract("_, _ -> new")
    public RenderOptions withMouse(float mouseX, float mouseY) {
        return new RenderOptions(defaultColor, shadow, mouseX, mouseY, partialTicks);
    }

    @Contract("_ -> new")
    public RenderOptions withPartialTicks(float partialTicks) {
        return new RenderOptions(defaultColor, shadow, mouseX, mouseY, partialTicks);
    }
}
