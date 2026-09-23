package dev.vfyjxf.cloudlib.api.ui.base;

import java.util.IdentityHashMap;
import java.util.Map;

/**
 * A centralized cache for widget paths, managed by {@link Scene}.
 * <p>
 * Uses an epoch-based invalidation strategy: when the widget tree structure changes,
 * a new epoch is created. Cached paths are lazily rebuilt when accessed if their
 * epoch doesn't match the current one.
 * <p>
 * This design keeps caching logic in one place and removes per-widget overhead.
 */
final class PathCache {

    private Object epoch = new Object();
    private final Map<Widget, Entry> entries = new IdentityHashMap<>();

    /**
     * Gets the cached path for the given widget, rebuilding if stale.
     *
     * @param widget the widget to get path for
     * @return the widget's path from root to leaf (immutable)
     */
    WidgetPath get(Widget widget) {
        Entry entry = entries.get(widget);

        if (entry == null || entry.epoch != this.epoch) {
            entry = new Entry(this.epoch, WidgetTree.pathToRoot(widget));
            entries.put(widget, entry);
        }

        return entry.path;
    }

    /**
     * Marks all cached paths as stale by creating a new epoch.
     * <p>
     * Paths are not immediately cleared; they're lazily rebuilt on next access.
     */
    void invalidate() {
        epoch = new Object();
        entries.clear();
    }

    private static final class Entry {
        final Object epoch;
        final WidgetPath path;

        Entry(Object epoch, WidgetPath path) {
            this.epoch = epoch;
            this.path = path;
        }
    }
}
