package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.TaffyStyle;

/**
 * Layout property for flex shrink.
 * <p>
 * Flex shrink determines how much the item should shrink relative to the rest
 * of the flexible items when there is not enough space in the container.
 * Maps to taffy {@link TaffyStyle#flexShrink}.
 * <p>
 * A value of 0 means the item will not shrink. A value greater than 0 specifies
 * the shrink factor relative to other items.
 *
 * @see TaffyStyle#flexShrink
 * @see UIStyles#flexShrink(float)
 */
public record FlexShrinkProperty(float shrink) implements LayoutProperty {

    public static final StyleType<Float> type = StyleType.of("flex-shrink", () -> 0.0f);

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.flexShrink = shrink;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlexShrinkProperty(float shrink1))) return false;
        return Float.compare(shrink, shrink1) == 0;
    }

    @Override
    public String toString() {
        return String.valueOf(shrink);
    }

}
