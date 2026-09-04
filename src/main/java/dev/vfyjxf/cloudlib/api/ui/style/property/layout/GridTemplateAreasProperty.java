package dev.vfyjxf.cloudlib.api.ui.style.property.layout;

import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.property.LayoutProperty;
import dev.vfyjxf.taffy.style.GridTemplateArea;
import dev.vfyjxf.taffy.style.TaffyStyle;

import java.util.ArrayList;
import java.util.List;
import java.util.Objects;

/**
 * Layout property that maps to taffy {@link TaffyStyle#gridTemplateAreas}.
 * <p>
 * Defines named grid areas in the grid template.
 * Each area specifies a name and the row/column lines that bound it.
 *
 * @see TaffyStyle#gridTemplateAreas
 * @see GridTemplateArea
 */
public record GridTemplateAreasProperty(List<GridTemplateArea> areas) implements LayoutProperty {

    public static final StyleType<List<GridTemplateArea>> type = StyleType.of("grid-template-areas", ArrayList::new);

    public GridTemplateAreasProperty(List<GridTemplateArea> areas) {
        this.areas = Objects.requireNonNull(areas, "areas");
    }

    public GridTemplateAreasProperty(GridTemplateArea... areas) {
        this(List.of(areas));
    }

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToStyle(TaffyStyle style) {
        style.gridTemplateAreas.clear();
        style.gridTemplateAreas.addAll(areas);
    }

    @Override
    public String toString() {
        return areas.toString();
    }

}
