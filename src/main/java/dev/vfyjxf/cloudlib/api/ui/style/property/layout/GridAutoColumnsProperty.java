package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout property that maps to taffy {@link TaffyStyle#gridAutoColumns}.
 * <p>
 * Specifies the size of implicitly-created grid columns.
 *
 * @see TaffyStyle#gridAutoColumns
 * @see TrackSizingFunction
 */
public record GridAutoColumnsProperty(List<TrackSizingFunction> columns) implements LayoutProperty {

    public static final StyleType<List<TrackSizingFunction>> type = StyleType.of("grid-auto-columns", () -> null);

    public GridAutoColumnsProperty(List<TrackSizingFunction> columns) {
        java.util.Objects.requireNonNull(columns, "columns");
        this.columns = List.copyOf(columns);
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.gridAutoColumns = new ArrayList<>(columns);
    }

    @Override
    public String toString() {
        return columns.toString();
    }

}
