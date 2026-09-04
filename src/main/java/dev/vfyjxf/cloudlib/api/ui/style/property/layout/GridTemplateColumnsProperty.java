package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Layout property that maps to taffy {@link TaffyStyle#gridTemplateColumns}.
 * <p>
 * Defines the column track sizing functions for the grid container.
 * Applying this will also clear {@link TaffyStyle#gridTemplateColumnsWithRepeat} so the plain template takes effect.
 *
 * @see TaffyStyle#gridTemplateColumns
 * @see TrackSizingFunction
 */
public record GridTemplateColumnsProperty(List<TrackSizingFunction> columns) implements LayoutProperty {

    public static final StyleType<List<TrackSizingFunction>> type = StyleType.of("grid-template-columns", () -> null);

    public GridTemplateColumnsProperty {
        Objects.requireNonNull(columns, "columns");
        columns = List.copyOf(columns);
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.gridTemplateColumnsWithRepeat = null;
        style.gridTemplateColumns = new ArrayList<>(columns);
    }

    @Override
    public String toString() {
        return columns.toString();
    }
}
