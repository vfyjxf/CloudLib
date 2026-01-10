package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.appliedenergistics.yoga.YogaFlexDirection;
import org.appliedenergistics.yoga.YogaNode;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Layout property for flex direction.
 * <p>
 * Flex direction determines the main axis along which flex items are placed.
 *
 * @see YogaFlexDirection
 * @see Styles#flexRow()
 * @see Styles#flexColumn()
 */
@ApiStatus.Experimental
public final class FlexDirectionProperty implements LayoutProperty {

    public static final String NAME = "flex-direction";

    private final YogaFlexDirection direction;

    public FlexDirectionProperty(YogaFlexDirection direction) {
        this.direction = Objects.requireNonNull(direction, "direction");
    }

    @Override
    public void applyToNode(YogaNode node) {
        node.setFlexDirection(direction);
    }

    @Override
    public String name() {
        return NAME;
    }

    public YogaFlexDirection getDirection() {
        return direction;
    }

    @Override
    public String valueToString() {
        return direction.name().toLowerCase().replace("_", "-");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlexDirectionProperty that)) return false;
        return direction == that.direction;
    }

    @Override
    public int hashCode() {
        return Objects.hash(direction);
    }
}
