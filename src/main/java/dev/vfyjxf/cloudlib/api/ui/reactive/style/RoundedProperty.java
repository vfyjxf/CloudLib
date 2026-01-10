package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Built-in border radius visual property.
 * <p>
 * Border radius is applied to the {@link VisualContext} for rendering.
 *
 * @see Styles#rounded(double)
 */
@ApiStatus.Experimental
public final class RoundedProperty implements VisualProperty {

    public static final String NAME = "borderRadius";

    private final double topLeft;
    private final double topRight;
    private final double bottomRight;
    private final double bottomLeft;

    public RoundedProperty(double all) {
        this(all, all, all, all);
    }

    public RoundedProperty(double topLeftBottomRight, double topRightBottomLeft) {
        this(topLeftBottomRight, topRightBottomLeft, topLeftBottomRight, topRightBottomLeft);
    }

    public RoundedProperty(double topLeft, double topRight, double bottomRight, double bottomLeft) {
        this.topLeft = topLeft;
        this.topRight = topRight;
        this.bottomRight = bottomRight;
        this.bottomLeft = bottomLeft;
    }

    @Override
    public void applyToWidget(VisualContext context) {
        context.setBorderRadius(topLeft, topRight, bottomRight, bottomLeft);
    }

    @Override
    public String name() {
        return NAME;
    }

    public double getTopLeft() { return topLeft; }
    public double getTopRight() { return topRight; }
    public double getBottomRight() { return bottomRight; }
    public double getBottomLeft() { return bottomLeft; }

    @Override
    public String valueToString() {
        if (topLeft == topRight && topLeft == bottomRight && topLeft == bottomLeft) {
            return String.valueOf(topLeft);
        }
        return topLeft + ", " + topRight + ", " + bottomRight + ", " + bottomLeft;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof RoundedProperty that)) return false;
        return Double.compare(topLeft, that.topLeft) == 0 &&
               Double.compare(topRight, that.topRight) == 0 &&
               Double.compare(bottomRight, that.bottomRight) == 0 &&
               Double.compare(bottomLeft, that.bottomLeft) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(topLeft, topRight, bottomRight, bottomLeft);
    }
}
