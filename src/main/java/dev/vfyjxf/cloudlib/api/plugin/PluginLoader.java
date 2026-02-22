package dev.vfyjxf.cloudlib.api.plugin;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import dev.vfyjxf.cloudlib.api.util.MutableLists;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;
import net.neoforged.fml.loading.toposort.CyclePresentException;
import net.neoforged.fml.loading.toposort.TopologicalSort;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.collector.Collectors2;
import org.eclipse.collections.impl.factory.Multimaps;
import org.slf4j.Logger;

import java.util.Collection;
import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public final class PluginLoader {

    public static <T extends ModPlugin> LoadingResult<T> load(Class<T> pluginClass) throws CyclePresentException, IllegalStateException {
        return load(pluginClass, PluginLoader.class.getClassLoader());
    }

    public static <T extends ModPlugin> LoadingResult<T> load(Class<T> pluginClass, ClassLoader classLoader) throws CyclePresentException, IllegalStateException {
        Checks.checkNotNull(pluginClass, "pluginClass");
        Checks.checkNotNull(classLoader, "classLoader");

        ServiceLoader<T> loader = ServiceLoader.load(pluginClass, classLoader);
        var pluginsList = loader.stream()
                                .map(ServiceLoader.Provider::get)
                                .map(LoadingPlugin::of)
                                .collect(Collectors.groupingBy(LoadingPlugin::id, Collectors.toList()));
        var duplicatePlugins = pluginsList.values()
                                          .stream()
                                          .filter(plugins -> plugins.size() > 1)
                                          .toList();
        if (!duplicatePlugins.isEmpty()) {
            String errorMessage = duplicatePlugins.stream()
                                                  .flatMap(Collection::stream)
                                                  .map(LoadingPlugin::id)
                                                  .distinct()
                                                  .map(Namespace::toString)
                                                  .collect(Collectors.joining(", "));
            throw new IllegalStateException("Duplicate plugins: " + errorMessage);
        }
        var loadingPlugins = pluginsList.values()
                                        .stream()
                                        .flatMap(Collection::stream)
                                        .collect(Collectors.toSet());
        var id2Plugin = loadingPlugins.stream()
                                      .collect(Collectors.toMap(LoadingPlugin::id, plugin -> plugin));

        MutableGraph<LoadingPlugin<T>> graph = GraphBuilder.directed().build();
        loadingPlugins.forEach(graph::addNode);
        for (var loadingPlugin : loadingPlugins) {
            for (var dependency : loadingPlugin.plugin.dependencies()) {

                if (dependency.order() == PluginDependency.Order.none) continue;
                var maybePlugin = id2Plugin.get(dependency.pluginId());
                if (maybePlugin == null) continue;

                switch (dependency.order()) {
                    case before -> graph.putEdge(loadingPlugin, maybePlugin);
                    case after -> graph.putEdge(maybePlugin, loadingPlugin);
                }
            }
        }
        var sorted = TopologicalSort.topologicalSort(graph, null);

        MutableList<T> plugins = MutableLists.empty();
        MutableList<LoadingFailure<T>> failures = MutableLists.empty();

        for (var loadingPlugin : sorted) {
            T plugin = loadingPlugin.plugin;

            var missingTargetDeps =
                loadingPlugin.dependencies()
                             .stream()
                             .filter(dep -> !id2Plugin.containsKey(dep.pluginId()))
                             .collect(Collectors2.groupBy(PluginDependency::constraint, Multimaps.mutable.list::empty));

            var required = missingTargetDeps.get(PluginDependency.Constraint.required);
            var ignorable = missingTargetDeps.get(PluginDependency.Constraint.optionalRequired);
            if (required.isEmpty() && ignorable.isEmpty()) {
                plugins.add(plugin);
            } else {
                var loadingFailures = ignorable.collect(dep -> new LoadingFailure<>(plugin, FailureType.warning, "Missing optional dependency: " + dep.pluginId()));
                failures.addAll(loadingFailures);
            }

            var pluginFailures =
                required.select(dep -> !id2Plugin.containsKey(dep.pluginId()))
                        .collect(dep -> new LoadingFailure<>(plugin, FailureType.fatal, "Missing required dependency: " + dep.pluginId()));

            failures.addAll(pluginFailures);
        }

        return new LoadingResult<>(plugins, failures);
    }

    //region util

    public static <T extends ModPlugin> MutableList<T> loadPlugin(Logger logger, String pluginCategory, Class<T> pluginClass) throws CyclePresentException, IllegalStateException {
        PluginLoader.LoadingResult<T> loadingResult = PluginLoader.load(pluginClass);
        if (loadingResult.failures().notEmpty()) {
            var failureByType =
                loadingResult.failures()
                             .groupBy(PluginLoader.LoadingFailure::type);
            var warnings = failureByType.get(PluginLoader.FailureType.warning);
            for (var warning : warnings) {
                logger.warn("{}: {} is skipped because : {}", pluginCategory, warning.instance().pluginId(), warning.reason());
            }
            var fatal = failureByType.get(PluginLoader.FailureType.fatal);
            if (fatal.notEmpty()) {
                String errorString = fatal.makeString("", ",\n", "");
                throw new IllegalStateException("Fatal error when loading " + pluginCategory + "s: " + errorString);
            }
        }
        return loadingResult.plugins();
    }

    /**
     * Loads plugins synchronously and returns a {@link PluginDispatcher} for parallel event dispatch.
     */
    public static <T extends ModPlugin> PluginDispatcher<T> loadAndDispatcher(Logger logger, String pluginCategory, Class<T> pluginClass) throws CyclePresentException, IllegalStateException {
        MutableList<T> plugins = loadPlugin(logger, pluginCategory, pluginClass);
        return PluginDispatcher.create(plugins);
    }

    //endregion

    public record LoadingResult<T extends ModPlugin>(MutableList<T> plugins, MutableList<LoadingFailure<T>> failures) {}

    public record LoadingFailure<T extends ModPlugin>(T instance, FailureType type, String reason) {
        @Override
        public String toString() {
            return "plugin: %s failed to load because: %s".formatted(instance.pluginId(), reason);
        }
    }

    public enum FailureType {
        fatal,
        warning
    }

    private record LoadingPlugin<T extends ModPlugin>(T plugin, Namespace id) {

        static <T extends ModPlugin> LoadingPlugin<T> of(T plugin) {
            return new LoadingPlugin<>(plugin, plugin.pluginId());
        }

        public Set<PluginDependency> dependencies() {
            return plugin.dependencies();
        }

        @Override
        public boolean equals(Object object) {
            if (object == null || getClass() != object.getClass()) return false;

            LoadingPlugin<?> that = (LoadingPlugin<?>) object;
            return id.equals(that.id);
        }

        @Override
        public int hashCode() {
            return id.hashCode();
        }

    }


    private PluginLoader() {}

}
