package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.appliedenergistics.yoga.style.StyleLength;
import org.appliedenergistics.yoga.YogaGutter;
import org.appliedenergistics.yoga.YogaNode;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Layout property for gap (spacing between flex items).
 * <p>
 * Gap sets the spacing between rows and columns in flexbox layouts.
 *
 * @see YogaGutter
 * @see Styles#gap(float)
 * @see Styles#rowGap(float)
 * @see Styles#columnGap(float)
 */
@ApiStatus.Experimental
public final class GapProperty implements LayoutProperty {

    public static final String NAME = "gap";
    public static final String NAME_ROW = "row-gap";
    public static final String NAME_COLUMN = "column-gap";

    private final YogaGutter gutter;
    private final StyleLength gap;

    public GapProperty(float gap) {
        this(YogaGutter.ALL, StyleLength.points(gap));
    }

    public GapProperty(YogaGutter gutter, float gap) {
        this(gutter, StyleLength.points(gap));
    }

    public GapProperty(YogaGutter gutter, StyleLength gap) {
        this.gutter = Objects.requireNonNull(gutter, "gutter");
        this.gap = Objects.requireNonNull(gap, "gap");
    }

    @Override
    public void applyToNode(YogaNode node) {
        node.setGap(gutter, gap);
    }

    @Override
    public String name() {
        return switch (gutter) {
            case ROW -> NAME_ROW;
            case COLUMN -> NAME_COLUMN;
            default -> NAME;
        };
    }

    public YogaGutter getGutter() {
        return gutter;
    }

    public StyleLength getGap() {
        return gap;
    }

    @Override
    public String valueToString() {
        return gap.toString();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof GapProperty that)) return false;
        return gutter == that.gutter && Objects.equals(gap, that.gap);
    }

    @Override
    public int hashCode() {
        return Objects.hash(gutter, gap);
    }
}
