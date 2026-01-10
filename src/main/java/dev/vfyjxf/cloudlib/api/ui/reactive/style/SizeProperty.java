package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.appliedenergistics.yoga.style.StyleSizeLength;
import org.appliedenergistics.yoga.YogaNode;
import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Built-in size layout property.
 * <p>
 * Size is applied directly to the {@link YogaNode} for layout calculation.
 *
 * @see Styles#size(double, double)
 */
@ApiStatus.Experimental
public final class SizeProperty implements LayoutProperty {

    public static final String NAME = "size";

    private final double width;
    private final double height;

    public SizeProperty(double size) {
        this(size, size);
    }

    public SizeProperty(double width, double height) {
        this.width = width;
        this.height = height;
    }

    @Override
    public void applyToNode(YogaNode node) {
        if (width >= 0) {
            node.setWidth(StyleSizeLength.points((float) width));
        }
        if (height >= 0) {
            node.setHeight(StyleSizeLength.points((float) height));
        }
    }

    @Override
    public String name() {
        return NAME;
    }

    public double getWidth() { return width; }
    public double getHeight() { return height; }

    @Override
    public String valueToString() {
        if (width == height) {
            return String.valueOf(width);
        }
        return width + " x " + height;
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof SizeProperty that)) return false;
        return Double.compare(width, that.width) == 0 &&
               Double.compare(height, that.height) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, height);
    }
}
