package dev.vfyjxf.cloudlib.api.ui.overlay;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import net.minecraft.client.renderer.Rect2i;

import java.util.List;

/**
 * Describes areas occupied by an overlay for UI exclusion and avoidance.
 *
 * @param <T> the overlay widget type
 */
@FunctionalInterface
public interface OverlayExclusion<T extends Widget> {

    List<Rect2i> areas(T widget, OverlayContext context);

    /**
     * Static exclusion area in GUI-scaled pixels (ignores widget).
     */
    static <T extends Widget> OverlayExclusion<T> fixed(int x, int y, int width, int height) {
        return (widget, context) -> List.of(new Rect2i(x, y, width, height));
    }
}
