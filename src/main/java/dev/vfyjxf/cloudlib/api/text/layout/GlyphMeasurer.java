package dev.vfyjxf.cloudlib.api.text.layout;

import net.minecraft.network.chat.Style;

/**
 * Measures text extents for the layouter.
 * <p>
 * Abstracted behind an interface so the layout engine is unit-testable without a
 * real Minecraft {@code Font}; the runtime implementation delegates to
 * {@code Font.width(FormattedText)}.
 */
public interface GlyphMeasurer {

    /**
     * Width in pixels of the given text rendered with the given style
     * (custom fonts are resolved through the style's font, like vanilla).
     */
    int width(String text, Style style);

    /**
     * Line height in pixels (9 for the vanilla font).
     */
    int lineHeight();
}
