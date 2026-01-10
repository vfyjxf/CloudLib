package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.appliedenergistics.yoga.style.StyleLength;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaNode;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Built-in margin layout property.
 * <p>
 * Margin is applied directly to the {@link YogaNode} for layout calculation.
 *
 * @see Styles#margin(double)
 */
@ApiStatus.Experimental
public final class MarginProperty implements LayoutProperty {

    public static final String NAME = "margin";

    private final double top;
    private final double right;
    private final double bottom;
    private final double left;

    public MarginProperty(double all) {
        this(all, all, all, all);
    }

    public MarginProperty(double vertical, double horizontal) {
        this(vertical, horizontal, vertical, horizontal);
    }

    public MarginProperty(double top, double right, double bottom, double left) {
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }

    @Override
    public void applyToNode(YogaNode node) {
        node.setMargin(YogaEdge.TOP, StyleLength.points((float) top));
        node.setMargin(YogaEdge.RIGHT, StyleLength.points((float) right));
        node.setMargin(YogaEdge.BOTTOM, StyleLength.points((float) bottom));
        node.setMargin(YogaEdge.LEFT, StyleLength.points((float) left));
    }

    @Override
    public String name() {
        return NAME;
    }

    public double getTop() { return top; }
    public double getRight() { return right; }
    public double getBottom() { return bottom; }
    public double getLeft() { return left; }

    @Override
    public String valueToString() {
        if (top == right && top == bottom && top == left) {
            return String.valueOf(top);
        }
        if (top == bottom && left == right) {
            return top + ", " + right;
        }
        return top + ", " + right + ", " + bottom + ", " + left;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof MarginProperty that)) return false;
        return Double.compare(top, that.top) == 0 &&
               Double.compare(right, that.right) == 0 &&
               Double.compare(bottom, that.bottom) == 0 &&
               Double.compare(left, that.left) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(top, right, bottom, left);
    }
}
