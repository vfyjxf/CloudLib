package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.FlexDirection;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Layout property for flex direction.
 * <p>
 * Flex direction determines the main axis along which flex items are placed.
 *
 * @see FlexDirection
 * @see UIStyles#flexRow()
 * @see UIStyles#flexColumn()
 */
public record FlexDirectionProperty(FlexDirection direction) implements LayoutProperty {

    public static final StyleType<FlexDirection> type = StyleType.of("flex-direction", () -> FlexDirection.COLUMN);

    public FlexDirectionProperty(FlexDirection direction) {
        this.direction = Objects.requireNonNull(direction, "direction");
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.flexDirection = direction;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlexDirectionProperty(FlexDirection direction1))) return false;
        return direction == direction1;
    }

    @Override
    public String toString() {
        return direction.name().toLowerCase().replace("_", "-");
    }

}
