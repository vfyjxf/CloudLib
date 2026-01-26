package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.NamedGridLine;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Layout property that maps to taffy {@link TaffyStyle#gridTemplateColumnNames}.
 * <p>
 * Defines named lines for grid columns.
 * Each entry maps a line name to a line index.
 * Multiple entries with the same name create multiple named lines.
 *
 * @see NamedGridLine
 */
public record GridTemplateColumnNamesProperty(List<NamedGridLine> columnNames) implements LayoutProperty {

    public static final StyleType<List<NamedGridLine>> type = StyleType.of("grid-template-column-names", ArrayList::new);

    public GridTemplateColumnNamesProperty(List<NamedGridLine> columnNames) {
        this.columnNames = Objects.requireNonNull(columnNames, "columnNames");
    }

    public GridTemplateColumnNamesProperty(NamedGridLine... columnNames) {
        this(List.of(columnNames));
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.gridTemplateColumnNames.clear();
        style.gridTemplateColumnNames.addAll(columnNames);
    }

    @Override
    public String toString() {
        return columnNames.toString();
    }

}
