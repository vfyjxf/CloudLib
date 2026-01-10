package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaNode;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Layout property for align content.
 * <p>
 * Align content aligns a flex container's lines within when there is extra
 * space in the cross-axis.
 *
 * @see YogaAlign
 * @see Styles#alignContentCenter()
 * @see Styles#alignContentSpaceBetween()
 */
@ApiStatus.Experimental
public final class AlignContentProperty implements LayoutProperty {

    public static final String NAME = "align-content";

    private final YogaAlign align;

    public AlignContentProperty(YogaAlign align) {
        this.align = Objects.requireNonNull(align, "align");
    }

    @Override
    public void applyToNode(YogaNode node) {
        node.setAlignContent(align);
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
        if (!(o instanceof AlignContentProperty that)) return false;
        return align == that.align;
    }

    @Override
    public int hashCode() {
        return Objects.hash(align);
    }
}
