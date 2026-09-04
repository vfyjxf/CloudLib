package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Layout property that maps to taffy {@link TaffyStyle#boxSizing}.
 * <p>
 * Box sizing controls how the total width and height of an element is calculated.
 * <p>
 * The box sizing values supported by taffy include:
 * <ul>
 *   <li>{@link BoxSizing#BORDER_BOX} - width/height includes padding and border</li>
 *   <li>{@link BoxSizing#CONTENT_BOX} - width/height is content only (padding/border added)</li>
 * </ul>
 *
 * @see TaffyStyle#boxSizing
 * @see BoxSizing
 */
public record BoxSizingProperty(BoxSizing boxSizing) implements LayoutProperty {

    public static final StyleType<BoxSizing> type = StyleType.of("box-sizing", () -> BoxSizing.BORDER_BOX);

    public BoxSizingProperty(BoxSizing boxSizing) {
        this.boxSizing = Objects.requireNonNull(boxSizing, "boxSizing");
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.boxSizing = boxSizing;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BoxSizingProperty(BoxSizing sizing))) return false;
        return boxSizing == sizing;
    }

    @Override
    public String toString() {
        return boxSizing.toString();
    }

}
