package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.NamedGridLine;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Layout property that maps to taffy {@link TaffyStyle#gridTemplateRowNames}.
 * <p>
 * Defines named lines for grid rows.
 * Each entry maps a line name to a line index.
 * Multiple entries with the same name create multiple named lines.
 *
 * @see NamedGridLine
 */
public record GridTemplateRowNamesProperty(List<NamedGridLine> rowNames) implements LayoutProperty {

    public static final StyleType<List<NamedGridLine>> type = StyleType.of("grid-template-row-names", ArrayList::new);

    public GridTemplateRowNamesProperty(List<NamedGridLine> rowNames) {
        this.rowNames = Objects.requireNonNull(rowNames, "rowNames");
    }

    public GridTemplateRowNamesProperty(NamedGridLine... rowNames) {
        this(List.of(rowNames));
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.gridTemplateRowNames.clear();
        style.gridTemplateRowNames.addAll(rowNames);
    }

    @Override
    public String toString() {
        return rowNames.toString();
    }

}
