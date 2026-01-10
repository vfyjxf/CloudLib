package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.appliedenergistics.yoga.style.StyleLength;
import org.appliedenergistics.yoga.YogaEdge;
import org.appliedenergistics.yoga.YogaNode;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Built-in padding layout property.
 * <p>
 * Padding is applied directly to the {@link YogaNode} for layout calculation.
 *
 * @see Styles#padding(double)
 */
@ApiStatus.Experimental
public final class PaddingProperty implements LayoutProperty {

    public static final String NAME = "padding";

    private final double top;
    private final double right;
    private final double bottom;
    private final double left;

    public PaddingProperty(double all) {
        this(all, all, all, all);
    }

    public PaddingProperty(double vertical, double horizontal) {
        this(vertical, horizontal, vertical, horizontal);
    }

    public PaddingProperty(double top, double right, double bottom, double left) {
        this.top = top;
        this.right = right;
        this.bottom = bottom;
        this.left = left;
    }

    @Override
    public void applyToNode(YogaNode node) {
        node.setPadding(YogaEdge.TOP, StyleLength.points((float) top));
        node.setPadding(YogaEdge.RIGHT, StyleLength.points((float) right));
        node.setPadding(YogaEdge.BOTTOM, StyleLength.points((float) bottom));
        node.setPadding(YogaEdge.LEFT, StyleLength.points((float) left));
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
        if (!(o instanceof PaddingProperty that)) return false;
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
