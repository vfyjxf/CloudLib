package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.TaffyDirection;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Layout property that maps to taffy {@link TaffyStyle#direction}.
 * <p>
 * The direction property defines the text direction (LTR or RTL) for this element.
 * Default is INHERIT which inherits from parent, or LTR if root.
 * <p>
 * The direction values supported by taffy include:
 * <ul>
 *   <li>{@link TaffyDirection#INHERIT} - inherit from parent (default)</li>
 *   <li>{@link TaffyDirection#LTR} - left to right</li>
 *   <li>{@link TaffyDirection#RTL} - right to left</li>
 * </ul>
 *
 * @see TaffyStyle#direction
 * @see TaffyDirection
 */
public record DirectionProperty(TaffyDirection direction) implements LayoutProperty {

    public static final StyleType<TaffyDirection> type = StyleType.of("direction", () -> TaffyDirection.INHERIT);

    public DirectionProperty(TaffyDirection direction) {
        this.direction = Objects.requireNonNull(direction, "direction");
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.direction = direction;
    }

    @Override
    public String toString() {
        return direction.toString();
    }

}
