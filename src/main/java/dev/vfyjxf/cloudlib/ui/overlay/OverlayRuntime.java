package dev.vfyjxf.cloudlib.ui.overlay;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayContext;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayEntry;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayExclusion;
import net.minecraft.client.renderer.Rect2i;

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

    public List<Rect2i> exclusionAreas(OverlayContext context) {
        OverlayExclusion<T> exclusion = entry.exclusion();
        return exclusion == null ? List.of() : exclusion.areas(widget, context);
    }
}
