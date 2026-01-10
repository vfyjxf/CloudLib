package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaNode;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Layout property for align self.
 * <p>
 * Align self allows the default alignment (or the one specified by align-items)
 * to be overridden for individual flex items.
 *
 * @see YogaAlign
 * @see Styles#alignSelfCenter()
 * @see Styles#alignSelfFlexStart()
 */
@ApiStatus.Experimental
public final class AlignSelfProperty implements LayoutProperty {

    public static final String NAME = "align-self";

    private final YogaAlign align;

    public AlignSelfProperty(YogaAlign align) {
        this.align = Objects.requireNonNull(align, "align");
    }

    @Override
    public void applyToNode(YogaNode node) {
        node.setAlignSelf(align);
    }

    @Override
    public String name() {
        return NAME;
    }

    public YogaAlign getAlign() {
        return align;
    }

    @Override
    public String valueToString() {
        return align.name().toLowerCase().replace("_", "-");
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof AlignSelfProperty that)) return false;
        return align == that.align;
    }

    @Override
    public int hashCode() {
        return Objects.hash(align);
    }
}
