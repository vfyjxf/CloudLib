package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.TaffyStyle;

/**
 * Layout property that maps to taffy {@link TaffyStyle#scrollbarWidth}.
 * <p>
 * Specifies the width of scrollbars for scrollable containers.
 * A value of 0 indicates no scrollbars.
 *
 * @see TaffyStyle#scrollbarWidth
 */
public record ScrollbarWidthProperty(float width) implements LayoutProperty {

    public static final StyleType<Float> type = StyleType.of("scrollbar-width", () -> 0.0f);

    public ScrollbarWidthProperty(float width) {
        this.width = Math.max(0.0f, width);
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.scrollbarWidth = width;
    }

    @Override
    public String toString() {
        return String.valueOf(width);
    }
}
