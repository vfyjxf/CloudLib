package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Built-in border visual property.
 * <p>
 * Border appearance is applied to the {@link VisualContext} for rendering.
 *
 * @see Styles#border(double, int)
 */
@ApiStatus.Experimental
public final class BorderProperty implements VisualProperty {

    public static final String NAME = "border";

    private final double width;
    private final int color;

    public BorderProperty(double width, int color) {
        this.width = width;
        this.color = color;
    }

    @Override
    public void applyToWidget(VisualContext context) {
        context.setBorder(width, color);
    }

    @Override
    public String name() {
        return NAME;
    }

    public double getWidth() {
        return width;
    }

    public int getColor() {
        return color;
    }

    @Override
    public String valueToString() {
        return width + "px " + String.format("0x%08X", color);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof BorderProperty that)) return false;
        return Double.compare(width, that.width) == 0 && color == that.color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(width, color);
    }
}
