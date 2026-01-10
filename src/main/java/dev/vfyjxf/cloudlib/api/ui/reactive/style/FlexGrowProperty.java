package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.appliedenergistics.yoga.YogaNode;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Layout property for flex grow.
 * <p>
 * Flex grow determines how much the item should grow relative to the rest
 * of the flexible items when there is remaining space in the container.
 *
 * @see Styles#flexGrow(float)
 */
@ApiStatus.Experimental
public final class FlexGrowProperty implements LayoutProperty {

    public static final String NAME = "flex-grow";

    private final float grow;

    public FlexGrowProperty(float grow) {
        this.grow = grow;
    }

    @Override
    public void applyToNode(YogaNode node) {
        node.setFlexGrow(grow);
    }

    @Override
    public String name() {
        return NAME;
    }

    public float getGrow() {
        return grow;
    }

    @Override
    public String valueToString() {
        return String.valueOf(grow);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlexGrowProperty that)) return false;
        return Float.compare(grow, that.grow) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(grow);
    }
}
