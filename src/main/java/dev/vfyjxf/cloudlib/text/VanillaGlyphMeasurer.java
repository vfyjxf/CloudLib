package dev.vfyjxf.cloudlib.text;

import dev.vfyjxf.cloudlib.api.text.layout.GlyphMeasurer;
import net.minecraft.client.gui.Font;
import net.minecraft.network.chat.FormattedText;
import net.minecraft.network.chat.Style;

/**
 * {@link GlyphMeasurer} backed by the vanilla {@link Font}: width queries go
 * through {@link Font#width(FormattedText)} so style-driven width effects
 * (bold, custom fonts) are honored.
 */
public final class VanillaGlyphMeasurer implements GlyphMeasurer {

    private final Font font;

    public VanillaGlyphMeasurer(Font font) {
        this.font = font;
    }

    public Font font() {
        return font;
    }

    @Override
    public int width(String text, Style style) {
        if (text.isEmpty()) return 0;
        return font.width(FormattedText.of(text, style));
    }

    @Override
    public int lineHeight() {
        return font.lineHeight;
    }
}
