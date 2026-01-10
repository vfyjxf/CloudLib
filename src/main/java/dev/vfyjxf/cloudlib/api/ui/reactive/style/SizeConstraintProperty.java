package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.appliedenergistics.yoga.style.StyleSizeLength;
import org.appliedenergistics.yoga.YogaNode;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Built-in min/max size constraint layout property.
 * <p>
 * Size constraints are applied directly to the {@link YogaNode} for layout calculation.
 *
 * @see Styles#minWidth(double)
 * @see Styles#maxWidth(double)
 */
@ApiStatus.Experimental
public final class SizeConstraintProperty implements LayoutProperty {

    public static final String NAME = "sizeConstraint";

    private final double minWidth;
    private final double minHeight;
    private final double maxWidth;
    private final double maxHeight;

    public SizeConstraintProperty(double minWidth, double minHeight, double maxWidth, double maxHeight) {
        this.minWidth = minWidth;
        this.minHeight = minHeight;
        this.maxWidth = maxWidth;
        this.maxHeight = maxHeight;
    }

    public static SizeConstraintProperty minWidth(double value) {
        return new SizeConstraintProperty(value, 0, Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public static SizeConstraintProperty minHeight(double value) {
        return new SizeConstraintProperty(0, value, Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public static SizeConstraintProperty maxWidth(double value) {
        return new SizeConstraintProperty(0, 0, value, Double.MAX_VALUE);
    }

    public static SizeConstraintProperty maxHeight(double value) {
        return new SizeConstraintProperty(0, 0, Double.MAX_VALUE, value);
    }

    public static SizeConstraintProperty minSize(double width, double height) {
        return new SizeConstraintProperty(width, height, Double.MAX_VALUE, Double.MAX_VALUE);
    }

    public static SizeConstraintProperty maxSize(double width, double height) {
        return new SizeConstraintProperty(0, 0, width, height);
    }

    @Override
    public void applyToNode(YogaNode node) {
        if (minWidth > 0) {
            node.setMinWidth(StyleSizeLength.points((float) minWidth));
        }
        if (minHeight > 0) {
            node.setMinHeight(StyleSizeLength.points((float) minHeight));
        }
        if (maxWidth < Double.MAX_VALUE) {
            node.setMaxWidth(StyleSizeLength.points((float) maxWidth));
        }
        if (maxHeight < Double.MAX_VALUE) {
            node.setMaxHeight(StyleSizeLength.points((float) maxHeight));
        }
    }

    @Override
    public String name() {
        return NAME;
    }

    public double getMinWidth() { return minWidth; }
    public double getMinHeight() { return minHeight; }
    public double getMaxWidth() { return maxWidth; }
    public double getMaxHeight() { return maxHeight; }

    @Override
    public String valueToString() {
        StringBuilder sb = new StringBuilder();
        if (minWidth > 0) sb.append("minW=").append(minWidth).append(" ");
        if (minHeight > 0) sb.append("minH=").append(minHeight).append(" ");
        if (maxWidth < Double.MAX_VALUE) sb.append("maxW=").append(maxWidth).append(" ");
        if (maxHeight < Double.MAX_VALUE) sb.append("maxH=").append(maxHeight);
        return sb.toString().trim();
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SizeConstraintProperty that)) return false;
        return Double.compare(minWidth, that.minWidth) == 0 &&
               Double.compare(minHeight, that.minHeight) == 0 &&
               Double.compare(maxWidth, that.maxWidth) == 0 &&
               Double.compare(maxHeight, that.maxHeight) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(minWidth, minHeight, maxWidth, maxHeight);
    }
}
