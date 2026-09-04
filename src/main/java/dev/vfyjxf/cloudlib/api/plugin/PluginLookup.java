package dev.vfyjxf.cloudlib.api.plugin;

import java.util.ArrayList;
import java.util.Collection;

/**
 * Discovers and instantiates plugin instances of a given type.
 * <p>
 * Does not handle deduplication or dependency resolution — that is
 * the responsibility of {@link PluginLoader}.
 *
 * @param <T> the plugin type
 */
@FunctionalInterface
public interface PluginLookup<T extends ModPlugin> {

    /**
     * Discovers all plugin instances.
     *
     * @return a collection of discovered plugin instances
     */
    Collection<T> findPlugins();

    /**
     * Combines this lookup with another, merging their results.
     */
    default PluginLookup<T> combine(PluginLookup<T> other) {
        PluginLookup<T> self = this;
        return () -> {
            var result = new ArrayList<T>();
            result.addAll(self.findPlugins());
            result.addAll(other.findPlugins());
            return result;
        };
    }
}
