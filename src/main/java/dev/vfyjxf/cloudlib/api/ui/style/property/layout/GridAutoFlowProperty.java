package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.GridAutoFlow;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Layout property that maps to taffy {@link TaffyStyle#gridAutoFlow}.
 * <p>
 * Controls how auto-placed items are inserted in the grid.
 * <p>
 * The grid auto flow values supported by taffy include:
 * <ul>
 *   <li>{@link GridAutoFlow#ROW} - fill rows first</li>
 *   <li>{@link GridAutoFlow#COLUMN} - fill columns first</li>
 *   <li>{@link GridAutoFlow#ROW_DENSE} - fill rows first with dense packing</li>
 *   <li>{@link GridAutoFlow#COLUMN_DENSE} - fill columns first with dense packing</li>
 * </ul>
 *
 * @see TaffyStyle#gridAutoFlow
 * @see GridAutoFlow
 */
public record GridAutoFlowProperty(GridAutoFlow flow) implements LayoutProperty {

    public static final StyleType<GridAutoFlow> type = StyleType.of("grid-auto-flow", () -> GridAutoFlow.ROW);

    public GridAutoFlowProperty(GridAutoFlow flow) {
        this.flow = Objects.requireNonNull(flow, "flow");
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.gridAutoFlow = flow;
    }

    @Override
    public String toString() {
        return flow.toString();
    }
}
