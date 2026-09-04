package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyles;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.TaffyPosition;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Layout property for position type.
 * <p>
 * Position type controls how the element is positioned within its parent.
 * Maps to taffy {@link TaffyStyle#position}.
 * <p>
 * The position types supported by taffy include:
 * <ul>
 *   <li>{@link TaffyPosition#RELATIVE} - positioned relative to normal flow</li>
 *   <li>{@link TaffyPosition#ABSOLUTE} - positioned relative to nearest positioned ancestor</li>
 * </ul>
 *
 * @see TaffyStyle#position
 * @see TaffyPosition
 * @see UIStyles#positionRelative()
 * @see UIStyles#positionAbsolute()
 */
public record PositionTypeProperty(TaffyPosition positionType) implements LayoutProperty {

    public static final StyleType<TaffyPosition> type = StyleType.of("position", () -> TaffyPosition.RELATIVE);

    public PositionTypeProperty(TaffyPosition positionType) {
        this.positionType = Objects.requireNonNull(positionType, "positionType");
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.position = positionType;
    }

    @Override
    public String toString() {
        return positionType.name().toLowerCase();
    }
}
