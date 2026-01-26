package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.TaffyStyle;
import dev.vfyjxf.taffy.style.TrackSizingFunction;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Layout property that maps to taffy {@link TaffyStyle#gridAutoRows}.
 */
public record GridAutoRowsProperty(List<TrackSizingFunction> rows) implements LayoutProperty {

    public static final StyleType<List<TrackSizingFunction>> type = StyleType.of("grid-auto-rows", () -> null);

    public GridAutoRowsProperty {
        Objects.requireNonNull(rows, "rows");
        rows = List.copyOf(rows);
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.gridAutoRows = new ArrayList<>(rows);
    }

    @Override
    public String toString() {
        return rows.toString();
    }
}
