package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.GridAutoFlow;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Layout property that maps to taffy {@link TaffyStyle#gridAutoFlow}.
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
