package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Layout property that maps to taffy {@link TaffyStyle#gridTemplateRows}.
 * <p>
 * Defines the row track sizing functions for the grid container.
 * Applying this will also clear {@link TaffyStyle#gridTemplateRowsWithRepeat} so the plain template takes effect.
 *
 * @see TaffyStyle#gridTemplateRows
 * @see TrackSizingFunction
 */
public record GridTemplateRowsProperty(List<TrackSizingFunction> rows) implements LayoutProperty {

    public static final StyleType<List<TrackSizingFunction>> type = StyleType.of("grid-template-rows", () -> null);

    public GridTemplateRowsProperty {
        Objects.requireNonNull(rows, "rows");
        rows = List.copyOf(rows);
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.gridTemplateRowsWithRepeat = null;
        style.gridTemplateRows = new ArrayList<>(rows);
    }

    @Override
    public String toString() {
        return rows.toString();
    }
}
