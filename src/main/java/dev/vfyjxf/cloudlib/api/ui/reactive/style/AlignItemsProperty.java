package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.appliedenergistics.yoga.YogaAlign;
import org.appliedenergistics.yoga.YogaNode;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Layout property for align items.
 * <p>
 * Align items defines the default behavior for how flex items are laid out
 * along the cross axis on the current line.
 *
 * @see YogaAlign
 * @see Styles#alignItemsCenter()
 * @see Styles#alignItemsFlexStart()
 */
@ApiStatus.Experimental
public final class AlignItemsProperty implements LayoutProperty {

    public static final String NAME = "align-items";

    private final YogaAlign align;

    public AlignItemsProperty(YogaAlign align) {
        this.align = Objects.requireNonNull(align, "align");
    }

    @Override
    public void applyToNode(YogaNode node) {
        node.setAlignItems(align);
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
        if (!(o instanceof AlignItemsProperty that)) return false;
        return align == that.align;
    }

    @Override
    public int hashCode() {
        return Objects.hash(align);
    }
}
