package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.BoxSizing;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Layout property that maps to taffy {@link TaffyStyle#boxSizing}.
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
