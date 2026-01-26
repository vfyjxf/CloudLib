package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.geometry.TaffyLine;
import dev.vfyjxf.taffy.style.GridPlacement;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.Objects;

/**
 * Layout property that maps to taffy {@link TaffyStyle#gridRow}.
 */
public record GridRowProperty(GridPlacement start, GridPlacement end) implements LayoutProperty {

    public static final StyleType<GridRowProperty> type = StyleType.of("grid-row", () -> null);

    public GridRowProperty {
        Objects.requireNonNull(start, "start");
        Objects.requireNonNull(end, "end");
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.gridRow = new TaffyLine<>(start, end);
    }

    @Override
    public String toString() {
        return start + " / " + end;
    }
}
