package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import java.util.List;

/**
 * A screen-space region the layout must not cover — an existing HUD
 * element. Given as a convex polygon in gui px.
 */
public record HudRegion(String id, List<GuiVec> polygon) {

    public HudRegion {
        polygon = List.copyOf(polygon);
        if (polygon.size() < 3) {
            throw new IllegalArgumentException();
        }
    }

    public static HudRegion rectangle(String id, GuiRect rect) {
        return new HudRegion(id, rect.polygon());
    }
}
