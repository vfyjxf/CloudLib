package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.appliedenergistics.yoga.YogaJustify;
import org.appliedenergistics.yoga.YogaNode;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Layout property for justify content.
 * <p>
 * Justify content defines the alignment along the main axis.
 * It helps distribute extra free space leftover when either all the flex items
 * on a line are inflexible, or are flexible but have reached their maximum size.
 *
 * @see YogaJustify
 * @see Styles#justifyCenter()
 * @see Styles#justifySpaceBetween()
 */
@ApiStatus.Experimental
public final class JustifyContentProperty implements LayoutProperty {

    public static final String NAME = "justify-content";

    private final YogaJustify justify;

    public JustifyContentProperty(YogaJustify justify) {
        this.justify = Objects.requireNonNull(justify, "justify");
    }

    @Override
    public void applyToNode(YogaNode node) {
        node.setJustifyContent(justify);
    }

    @Override
    public String name() {
        return NAME;
    }

    public YogaJustify getJustify() {
        return justify;
    }

    @Override
    public String valueToString() {
        return justify.name().toLowerCase().replace("_", "-");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof JustifyContentProperty that)) return false;
        return justify == that.justify;
    }

    @Override
    public int hashCode() {
        return Objects.hash(justify);
    }
}
