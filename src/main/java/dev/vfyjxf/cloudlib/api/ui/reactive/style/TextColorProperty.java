package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Built-in text color visual property.
 * <p>
 * Text color is applied to the {@link VisualContext} for rendering.
 *
 * @see Styles#textColor(int)
 */
@ApiStatus.Experimental
public final class TextColorProperty implements VisualProperty {

    public static final String NAME = "textColor";

    private final int color;

    public TextColorProperty(int color) {
        this.color = color;
    }

    @Override
    public void applyToWidget(VisualContext context) {
        context.setTextColor(color);
    }

    @Override
    public String name() {
        return NAME;
    }

    public int getColor() {
        return color;
    }

    @Override
    public String valueToString() {
        return String.format("0x%08X", color);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof TextColorProperty that)) return false;
        return color == that.color;
    }

    @Override
    public int hashCode() {
        return Objects.hash(color);
    }
}
