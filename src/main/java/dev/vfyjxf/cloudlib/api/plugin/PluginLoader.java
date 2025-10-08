package dev.vfyjxf.cloudlib.api.plugin;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.MutableGraph;
import dev.vfyjxf.cloudlib.api.util.MutableLists;
import dev.vfyjxf.cloudlib.util.Checks;
import net.minecraft.resources.ResourceLocation;
import net.neoforged.fml.loading.toposort.CyclePresentException;
import net.neoforged.fml.loading.toposort.TopologicalSort;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.impl.collector.Collectors2;
import org.eclipse.collections.impl.factory.Multimaps;

import java.util.ServiceLoader;
import java.util.Set;
import java.util.stream.Collectors;

@SuppressWarnings("UnstableApiUsage")
public final class PluginLoader {

    public static <T extends ModPlugin> LoadingResult<T> load(Class<T> pluginClass) throws CyclePresentException {
        return load(pluginClass, PluginLoader.class.getClassLoader());
    }

    public static <T extends ModPlugin> LoadingResult<T> load(Class<T> pluginClass, ClassLoader classLoader) throws CyclePresentException {
        Checks.checkNotNull(pluginClass, "pluginClass");

        ServiceLoader<T> loader = ServiceLoader.load(pluginClass, classLoader);
        var loadingPlugins = loader.stream()
            .map(ServiceLoader.Provider::get)
            .map(LoadingPlugin::of)
            .collect(Collectors.toSet());
        var id2Plugin = loadingPlugins.stream()
            .collect(Collectors.toMap(LoadingPlugin::id, plugin -> plugin));

        MutableGraph<LoadingPlugin<T>> graph = GraphBuilder.directed().build();
        loadingPlugins.forEach(graph::addNode);
        for (var loadingPlugin : loadingPlugins) {
            for (var dependency : loadingPlugin.plugin.dependencies()) {

                if (dependency.order() == PluginDependency.Order.NONE) continue;
                var maybePlugin = id2Plugin.get(dependency.pluginId());
                if (maybePlugin == null) continue;

                switch (dependency.order()) {
                    case BEFORE -> graph.putEdge(loadingPlugin, maybePlugin);
                    case AFTER -> graph.putEdge(maybePlugin, loadingPlugin);
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

            var required = missingTargetDeps.get(PluginDependency.Constraint.REQUIRED);
            var ignorable = missingTargetDeps.get(PluginDependency.Constraint.OPTIONAL_REQUIRED);
            if (required.isEmpty() && ignorable.isEmpty()) {
                plugins.add(plugin);
            } else {
                var loadingFailures = ignorable.collect(dep -> new LoadingFailure<>(plugin, FailureType.WARNING, "Missing optional dependency: " + dep.pluginId()));
                failures.addAll(loadingFailures);
            }

            var pluginFailures =
                required.select(dep -> !id2Plugin.containsKey(dep.pluginId()))
                    .collect(dep -> new LoadingFailure<>(plugin, FailureType.FATAL, "Missing required dependency: " + dep.pluginId()));

            failures.addAll(pluginFailures);
        }

        return new LoadingResult<>(plugins, failures);
    }

    public record LoadingResult<T extends ModPlugin>(MutableList<T> plugins, MutableList<LoadingFailure<T>> failures) {}

    public record LoadingFailure<T extends ModPlugin>(T instance, FailureType type, String reason) {
        @Override
        public String toString() {
            return "plugin: %s failed to load because: %s".formatted(instance.pluginId(), reason);
        }
    }

    public enum FailureType {
        FATAL,
        WARNING
    }

    private record LoadingPlugin<T extends ModPlugin>(T plugin, ResourceLocation id) {

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
