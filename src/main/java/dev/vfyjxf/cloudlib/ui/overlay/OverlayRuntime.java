package dev.vfyjxf.cloudlib.ui.overlay;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayContext;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayEntry;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayExclusion;
import net.minecraft.client.renderer.Rect2i;

import java.util.ArrayList;
import java.util.List;

/**
 * Runtime state for an attached overlay.
 * <p>
 * Captures the entry–widget pair so that exclusion areas can be computed
 * in a type-safe way despite generic erasure.
 */
public final class OverlayRuntime<T extends Widget> {

    private final OverlayEntry<T> entry;
    private final T widget;

    public OverlayRuntime(OverlayEntry<T> entry, T widget) {
        this.entry = entry;
        this.widget = widget;
    }

    public OverlayEntry<T> entry() {
        return entry;
    }

    public Widget widget() {
        return widget;
    }

    /**
     * The screen rectangles this overlay currently occupies — only what is
     * really presented this frame. A widget the renderer would skip (hidden
     * or being dragged) contributes nothing, and each reported rectangle is
     * clipped to the context screen, so a panel parked off-screen never
     * leaks its parked coordinates into the output.
     */
    public List<Rect2i> exclusionAreas(OverlayContext context) {
        if (!widget.shouldRender()) {
            return List.of();
        }
        OverlayExclusion<T> exclusion = entry.exclusion();
        if (exclusion == null) {
            return List.of();
        }
        List<Rect2i> areas = exclusion.areas(widget, context);
        if (areas == null || areas.isEmpty()) {
            return List.of();
        }
        int screenWidth = context.width();
        int screenHeight = context.height();
        List<Rect2i> presented = new ArrayList<>(areas.size());
        for (Rect2i area : areas) {
            if (area == null) {
                continue;
            }
            int left = Math.max(area.getX(), 0);
            int top = Math.max(area.getY(), 0);
            int right = Math.min(area.getX() + area.getWidth(), screenWidth);
            int bottom = Math.min(area.getY() + area.getHeight(), screenHeight);
            if (right <= left || bottom <= top) {
                continue;
            }
            presented.add(new Rect2i(left, top, right - left, bottom - top));
        }
        return List.copyOf(presented);
    }
}
