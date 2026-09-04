package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Layout property that maps to taffy {@link TaffyStyle#gridColumn}.
 * <p>
 * Specifies a grid item's column start and end position.
 *
 * @see TaffyStyle#gridColumn
 * @see GridPlacement
 */
public record GridColumnProperty(GridPlacement start, GridPlacement end) implements LayoutProperty {

    //region types

    public static final StyleType<GridColumnProperty> type = StyleType.of("grid-column", () -> null);

    //endregion

    //region constructors

    public GridColumnProperty {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
    }

    //endregion

    //region factory methods

    /**
     * Creates a grid column property that spans a number of columns.
     */
    public static GridColumnProperty span(int span) {
        return new GridColumnProperty(GridPlacement.auto(), GridPlacement.span(span));
    }

    /**
     * Creates a grid column property starting at a specific line.
     */
    public static GridColumnProperty line(int line) {
        return new GridColumnProperty(GridPlacement.line(line), GridPlacement.auto());
    }

    /**
     * Creates a grid column property with auto placement.
     */
    public static GridColumnProperty auto() {
        return new GridColumnProperty(GridPlacement.auto(), GridPlacement.auto());
    }

    /**
     * Creates a grid column property from start line to end line.
     */
    public static GridColumnProperty fromTo(int start, int end) {
        return new GridColumnProperty(GridPlacement.line(start), GridPlacement.line(end));
    }

    /**
     * Creates a grid column property starting at a specific line and spanning a number of columns.
     */
    public static GridColumnProperty lineSpan(int line, int span) {
        return new GridColumnProperty(GridPlacement.line(line), GridPlacement.span(span));
    }

    //endregion

    //region LayoutProperty implementation

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.gridColumn = new TaffyLine<>(start, end);
    }

    //endregion

    @Override
    public String toString() {
        return start + " / " + end;
    }
}
