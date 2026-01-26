package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.TaffyStyle;

/**
 * Layout property for flex grow.
 * <p>
 * Flex grow determines how much the item should grow relative to the rest
 * of the flexible items when there is remaining space in the container.
 *
 * @see UIStyles#flexGrow(float)
 */
public record FlexGrowProperty(float grow) implements LayoutProperty {

    public static final StyleType<Float> type = StyleType.of("flex-grow", () -> 0.0f);

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.flexGrow = grow;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlexGrowProperty(float grow1))) return false;
        return Float.compare(grow, grow1) == 0;
    }

    @Override
    public String toString() {
        return String.valueOf(grow);
    }

}
