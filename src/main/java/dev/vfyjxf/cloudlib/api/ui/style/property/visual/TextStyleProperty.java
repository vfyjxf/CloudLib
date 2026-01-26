package dev.vfyjxf.cloudlib.api.ui.style.property.visual;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.VisualContext;
import dev.vfyjxf.cloudlib.api.ui.style.property.VisualProperty;

/**
 * Built-in text style visual property for bold, italic, underline, strikethrough.
 * <p>
 * Text styles are applied to the {@link VisualContext} for rendering.
 *
 * @see UIStyles#bold()
 * @see UIStyles#italic()
 */
public record TextStyleProperty(boolean isBold, boolean isItalic, boolean isUnderline, boolean isStrikethrough) implements VisualProperty {

    public static final StyleType<Integer> type = StyleType.of("textStyle", () -> 0);

    @Override
    public StyleType<?> type() {
        return type;
    }

    public static TextStyleProperty bold() {
        return new TextStyleProperty(true, false, false, false);
    }

    public static TextStyleProperty italic() {
        return new TextStyleProperty(false, true, false, false);
    }

    public static TextStyleProperty underline() {
        return new TextStyleProperty(false, false, true, false);
    }

    public static TextStyleProperty strikethrough() {
        return new TextStyleProperty(false, false, false, true);
    }

    @Override
    public void applyToWidget(VisualContext context) {
        if (isBold) context.textBold(true);
        if (isItalic) context.textItalic(true);
        if (isUnderline) context.textUnderline(true);
        if (isStrikethrough) context.textStrikethrough(true);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder();
        if (isBold) sb.append("bold ");
        if (isItalic) sb.append("italic ");
        if (isUnderline) sb.append("underline ");
        if (isStrikethrough) sb.append("strikethrough ");
        return sb.toString().trim();
    }
}
