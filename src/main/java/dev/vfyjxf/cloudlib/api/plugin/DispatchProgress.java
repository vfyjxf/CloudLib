package dev.vfyjxf.cloudlib.api.plugin;

import dev.vfyjxf.cloudlib.api.util.Namespace;

/**
 * Callback for tracking dispatch progress. Implementations must be thread-safe.
 *
 * @see ProgressTracker
 */
public interface DispatchProgress {

    /**
     * Called before dispatch begins.
     *
     * @param totalSteps total number of plugins to dispatch
     */
    void begin(int totalSteps);

    /**
     * Called after a single plugin completes its event.
     *
     * @param pluginId  the plugin that completed
     * @param completed number of plugins completed so far (1-based)
     * @param total     total number of plugins in this dispatch
     */
    void advance(Namespace pluginId, int completed, int total);

    /**
     * Called when the dispatch finishes (all plugins completed or on error).
     */
    void complete();

    /**
     * Returns a no-op progress that discards all callbacks.
     */
    static DispatchProgress empty() {
        return Empty.instance;
    }

    enum Empty implements DispatchProgress {
        instance;

        @Override
        public void begin(int totalSteps) {
        }

        @Override
        public void advance(Namespace pluginId, int completed, int total) {
        }

        @Override
        public void complete() {
        }
    }

}
