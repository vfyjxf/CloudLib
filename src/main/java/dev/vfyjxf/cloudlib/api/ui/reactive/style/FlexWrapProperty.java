package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.appliedenergistics.yoga.YogaNode;
import org.appliedenergistics.yoga.YogaWrap;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Layout property for flex wrap.
 * <p>
 * Flex wrap controls whether the flex container is single-line or multi-line,
 * and the direction of the cross-axis.
 *
 * @see YogaWrap
 * @see Styles#flexWrap()
 * @see Styles#flexNoWrap()
 */
@ApiStatus.Experimental
public final class FlexWrapProperty implements LayoutProperty {

    public static final String NAME = "flex-wrap";

    private final YogaWrap wrap;

    public FlexWrapProperty(YogaWrap wrap) {
        this.wrap = Objects.requireNonNull(wrap, "wrap");
    }

    @Override
    public void applyToNode(YogaNode node) {
        node.setWrap(wrap);
    }

    @Override
    public String name() {
        return NAME;
    }

    public YogaWrap getWrap() {
        return wrap;
    }

    @Override
    public String valueToString() {
        return wrap.name().toLowerCase().replace("_", "-");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FlexWrapProperty that)) return false;
        return wrap == that.wrap;
    }

    @Override
    public int hashCode() {
        return Objects.hash(wrap);
    }
}
