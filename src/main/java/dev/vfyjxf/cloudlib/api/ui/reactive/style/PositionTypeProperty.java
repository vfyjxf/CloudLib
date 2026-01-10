package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.appliedenergistics.yoga.YogaNode;
import org.appliedenergistics.yoga.YogaPositionType;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Layout property for position type.
 * <p>
 * Position type controls how the element is positioned within its parent.
 *
 * @see YogaPositionType
 * @see Styles#positionRelative()
 * @see Styles#positionAbsolute()
 */
@ApiStatus.Experimental
public final class PositionTypeProperty implements LayoutProperty {

    public static final String NAME = "position";

    private final YogaPositionType positionType;

    public PositionTypeProperty(YogaPositionType positionType) {
        this.positionType = Objects.requireNonNull(positionType, "positionType");
    }

    @Override
    public void applyToNode(YogaNode node) {
        node.setPositionType(positionType);
    }

    @Override
    public String name() {
        return NAME;
    }

    public YogaPositionType getPositionType() {
        return positionType;
    }

    @Override
    public String valueToString() {
        return positionType.name().toLowerCase();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof PositionTypeProperty that)) return false;
        return positionType == that.positionType;
    }

    @Override
    public int hashCode() {
        return Objects.hash(positionType);
    }
}
