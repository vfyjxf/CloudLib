package dev.vfyjxf.cloudlib.api.text.render;

import dev.vfyjxf.cloudlib.api.text.ThemeColorResolver;
import org.jetbrains.annotations.Contract;
import org.jspecify.annotations.Nullable;

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
 * @param themeColors  resolution of theme color slots at render time;
 *                     {@code null} = no theme, slots fall back to their literal
 *                     color. See {@link ThemeColorResolver}
 */
public record RenderOptions(
    int defaultColor,
    boolean shadow,
    float mouseX,
    float mouseY,
    float partialTicks,
    @Nullable ThemeColorResolver themeColors
) {

    public static final RenderOptions defaults = new RenderOptions(0xFFFFFFFF, false, 0, 0, 1.0f, null);

    @Contract("_ -> new")
    public RenderOptions withDefaultColor(int defaultColor) {
        return new RenderOptions(defaultColor, shadow, mouseX, mouseY, partialTicks, themeColors);
    }

    @Contract("_ -> new")
    public RenderOptions withShadow(boolean shadow) {
        return new RenderOptions(defaultColor, shadow, mouseX, mouseY, partialTicks, themeColors);
    }

    @Contract("_, _ -> new")
    public RenderOptions withMouse(float mouseX, float mouseY) {
        return new RenderOptions(defaultColor, shadow, mouseX, mouseY, partialTicks, themeColors);
    }

    @Contract("_ -> new")
    public RenderOptions withPartialTicks(float partialTicks) {
        return new RenderOptions(defaultColor, shadow, mouseX, mouseY, partialTicks, themeColors);
    }

    /**
     * Binds the theme resolution used for style color slots. Widgets pass their
     * own style context ({@link ThemeColorResolver#of}); detached rendering such
     * as tooltips passes the active theme
     * ({@link ThemeColorResolver#ofActiveTheme()}).
     */
    @Contract("_ -> new")
    public RenderOptions withThemeColors(@Nullable ThemeColorResolver themeColors) {
        return new RenderOptions(defaultColor, shadow, mouseX, mouseY, partialTicks, themeColors);
    }
}
