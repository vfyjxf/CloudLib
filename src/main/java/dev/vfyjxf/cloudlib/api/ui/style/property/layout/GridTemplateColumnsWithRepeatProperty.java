package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.GridTemplateComponent;
import dev.vfyjxf.taffy.style.TaffyStyle;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Layout property that maps to taffy {@link TaffyStyle#gridTemplateColumnsWithRepeat}.
 * <p>
 * Defines grid column templates with support for repeat() notation.
 * When set, this overrides {@link TaffyStyle#gridTemplateColumns}.
 *
 * @see TaffyStyle#gridTemplateColumnsWithRepeat
 * @see GridTemplateComponent
 */
public record GridTemplateColumnsWithRepeatProperty(
        @Nullable List<GridTemplateComponent> columns) implements LayoutProperty {

    public static final StyleType<List<GridTemplateComponent>> type = StyleType.of("grid-template-columns-with-repeat", () -> null);

    public GridTemplateColumnsWithRepeatProperty {
        columns = (columns == null) ? null : List.copyOf(columns);
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.gridTemplateColumnsWithRepeat = columns == null ? null : new ArrayList<>(columns);
    }

    @Override
    public String toString() {
        return columns == null ? "null" : columns.toString();
    }
}
