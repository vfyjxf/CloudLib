package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Built-in font size visual property.
 * <p>
 * Font size is applied to the {@link VisualContext} for rendering.
 *
 * @see Styles#fontSize(double)
 */
@ApiStatus.Experimental
public final class FontSizeProperty implements VisualProperty {

    public static final String NAME = "fontSize";

    private final double size;

    public FontSizeProperty(double size) {
        this.size = size;
    }

    @Override
    public void applyToWidget(VisualContext context) {
        context.setFontSize(size);
    }

    @Override
    public String name() {
        return NAME;
    }

    public double getSize() {
        return size;
    }

    @Override
    public String valueToString() {
        return size + "px";
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof FontSizeProperty that)) return false;
        return Double.compare(size, that.size) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(size);
    }
}
