package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TextAlign;

import java.util.Objects;

/**
 * Layout property that maps to taffy {@link TaffyStyle#textAlign}.
 */
public record TextAlignProperty(TextAlign textAlign) implements LayoutProperty {

    public static final StyleType<TextAlign> type = StyleType.of("text-align", () -> TextAlign.AUTO);

    public TextAlignProperty(TextAlign textAlign) {
        this.textAlign = Objects.requireNonNull(textAlign, "textAlign");
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.textAlign = textAlign;
    }

    @Override
    public String toString() {
        return textAlign.toString();
    }
}
