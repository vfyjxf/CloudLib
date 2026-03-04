package dev.vfyjxf.cloudlib.api.plugin;

import dev.vfyjxf.cloudlib.util.Checks;

import java.util.Collection;
import java.util.ServiceLoader;

/**
 * A {@link PluginLookup} that uses Java SPI ({@link ServiceLoader})
 * to discover plugin implementations.
 *
 * @param <T> the plugin type
 */
public final class SpiPluginLookup<T extends ModPlugin> implements PluginLookup<T> {

    private final Class<T> pluginClass;
    private final ClassLoader classLoader;

    private SpiPluginLookup(Class<T> pluginClass, ClassLoader classLoader) {
        this.pluginClass = pluginClass;
        this.classLoader = classLoader;
    }

    public static <T extends ModPlugin> SpiPluginLookup<T> of(Class<T> pluginClass) {
        return of(pluginClass, SpiPluginLookup.class.getClassLoader());
    }

    public static <T extends ModPlugin> SpiPluginLookup<T> of(Class<T> pluginClass, ClassLoader classLoader) {
        Checks.checkNotNull(pluginClass, "pluginClass");
        Checks.checkNotNull(classLoader, "classLoader");
        return new SpiPluginLookup<>(pluginClass, classLoader);
    }

    @Override
    public Collection<T> findPlugins() {
        return ServiceLoader.load(pluginClass, classLoader)
                .stream()
                .map(ServiceLoader.Provider::get)
                .toList();
    }
}
