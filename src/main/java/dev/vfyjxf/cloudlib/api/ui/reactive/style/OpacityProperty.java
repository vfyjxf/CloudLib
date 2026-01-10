package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.jetbrains.annotations.ApiStatus;

import java.util.Objects;

/**
 * Built-in opacity visual property.
 * <p>
 * Opacity is applied to the {@link VisualContext} for rendering.
 *
 * @see Styles#opacity(double)
 */
@ApiStatus.Experimental
public final class OpacityProperty implements VisualProperty {

    public static final String NAME = "opacity";

    private final double opacity;

    public OpacityProperty(double opacity) {
        this.opacity = Math.max(0, Math.min(1, opacity));
    }

    @Override
    public void applyToWidget(VisualContext context) {
        context.setOpacity(opacity);
    }

    @Override
    public String name() {
        return NAME;
    }

    public double getOpacity() {
        return opacity;
    }

    @Override
    public String valueToString() {
        return String.valueOf(opacity);
    }

    @Override
    public boolean equals(Object o) {
        if (this == o) return true;
        if (!(o instanceof OpacityProperty that)) return false;
        return Double.compare(opacity, that.opacity) == 0;
    }

    @Override
    public int hashCode() {
        return Objects.hash(opacity);
    }
}
