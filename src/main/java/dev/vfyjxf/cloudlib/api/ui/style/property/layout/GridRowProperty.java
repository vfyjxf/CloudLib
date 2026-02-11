package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Layout property that maps to taffy {@link TaffyStyle#gridRow}.
 * <p>
 * Specifies a grid item's row start and end position.
 *
 * @see TaffyStyle#gridRow
 * @see GridPlacement
 */
public record GridRowProperty(GridPlacement start, GridPlacement end) implements LayoutProperty {

    //region types

    public static final StyleType<GridRowProperty> type = StyleType.of("grid-row", () -> null);

    //endregion

    //region constructors

    public GridRowProperty {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
    }

    //endregion

    //region factory methods

    /**
     * Creates a grid row property that spans a number of rows.
     */
    public static GridRowProperty span(int span) {
        return new GridRowProperty(GridPlacement.auto(), GridPlacement.span(span));
    }

    /**
     * Creates a grid row property starting at a specific line.
     */
    public static GridRowProperty line(int line) {
        return new GridRowProperty(GridPlacement.line(line), GridPlacement.auto());
    }

    /**
     * Creates a grid row property with auto placement.
     */
    public static GridRowProperty auto() {
        return new GridRowProperty(GridPlacement.auto(), GridPlacement.auto());
    }

    /**
     * Creates a grid row property from start line to end line.
     */
    public static GridRowProperty fromTo(int start, int end) {
        return new GridRowProperty(GridPlacement.line(start), GridPlacement.line(end));
    }

    /**
     * Creates a grid row property starting at a specific line and spanning a number of rows.
     */
    public static GridRowProperty lineSpan(int line, int span) {
        return new GridRowProperty(GridPlacement.line(line), GridPlacement.span(span));
    }

    //endregion

    //region LayoutProperty implementation

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.gridRow = new TaffyLine<>(start, end);
    }

    //endregion

    @Override
    public String toString() {
        return start + " / " + end;
    }
}
