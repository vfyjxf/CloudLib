package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.appliedenergistics.yoga.YogaNode;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Layout property for flex shrink.
 * <p>
 * Flex shrink determines how much the item should shrink relative to the rest
 * of the flexible items when there is not enough space in the container.
 *
 * @see Styles#flexShrink(float)
 */
@ApiStatus.Experimental
public final class FlexShrinkProperty implements LayoutProperty {

    public static final String NAME = "flex-shrink";

    private final float shrink;

    public FlexShrinkProperty(float shrink) {
        this.shrink = shrink;
    }

    @Override
    public void applyToNode(YogaNode node) {
        node.setFlexShrink(shrink);
    }

    @Override
    public String name() {
        return NAME;
    }

    public float getShrink() {
        return shrink;
    }

    @Override
    public String valueToString() {
        return String.valueOf(shrink);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlexShrinkProperty that)) return false;
        return Float.compare(shrink, that.shrink) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(shrink);
    }
}
