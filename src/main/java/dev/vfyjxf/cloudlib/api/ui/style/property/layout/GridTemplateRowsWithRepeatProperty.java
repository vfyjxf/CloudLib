package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.GridTemplateComponent;
import dev.vfyjxf.taffy.style.TaffyStyle;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout property that maps to taffy {@link TaffyStyle#gridTemplateRowsWithRepeat}.
 */
public record GridTemplateRowsWithRepeatProperty(@Nullable List<GridTemplateComponent> rows) implements LayoutProperty {

    public static final StyleType<List<GridTemplateComponent>> type = StyleType.of("grid-template-rows-with-repeat", () -> null);

    public GridTemplateRowsWithRepeatProperty {
        rows = (rows == null) ? null : List.copyOf(rows);
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.gridTemplateRowsWithRepeat = rows == null ? null : new ArrayList<>(rows);
    }

    @Override
    public String toString() {
        return rows == null ? "null" : rows.toString();
    }
}
