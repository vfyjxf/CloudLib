package dev.vfyjxf.cloudlib.api.plugin;

import com.google.common.graph.GraphBuilder;
import com.google.common.graph.ImmutableGraph;
import com.google.common.graph.MutableGraph;
import dev.vfyjxf.cloudlib.api.util.MutableLists;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.Checks;
import it.unimi.dsi.fastutil.objects.Object2IntOpenHashMap;
import net.neoforged.fml.loading.toposort.CyclePresentException;
import net.neoforged.fml.loading.toposort.TopologicalSort;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Dependency graph with topological ordering and level decomposition.
 * <p>
 * Plugins at the same level have no dependency relationships and can be dispatched concurrently.
 *
 * @param <T> the plugin type
 */
@SuppressWarnings("UnstableApiUsage")
public final class DependencyGraph<T extends ModPlugin> {

    private final ImmutableGraph<Namespace> graph;
    private final ImmutableList<T> sorted;
    private final ImmutableList<LoadingLevel<T>> levels;
    private final Map<Namespace, T> pluginById;

    private DependencyGraph(
            ImmutableGraph<Namespace> graph,
            ImmutableList<T> sorted,
            ImmutableList<LoadingLevel<T>> levels,
            Map<Namespace, T> pluginById
    ) {
        this.graph = graph;
        this.sorted = sorted;
        this.levels = levels;
        this.pluginById = pluginById;
    }

    //region factory

    /**
     * Builds from an unsorted collection. Performs topological sorting and level decomposition.
     */
    public static <T extends ModPlugin> DependencyGraph<T> build(Collection<T> plugins) throws CyclePresentException {
        Checks.checkNotNull(plugins, "plugins");
        var id2Plugin = collectPlugins(plugins);
        var mutableGraph = buildGraph(id2Plugin);
        var sortedIds = TopologicalSort.topologicalSort(mutableGraph, null);

        MutableList<T> sorted = MutableLists.empty();
        for (var id : sortedIds) {
            T plugin = id2Plugin.get(id);
            if (plugin != null) sorted.add(plugin);
        }

        return new DependencyGraph<>(
                ImmutableGraph.copyOf(mutableGraph),
                sorted.toImmutable(),
                computeLevels(sortedIds, mutableGraph, id2Plugin).toImmutable(),
                Collections.unmodifiableMap(id2Plugin)
        );
    }

    /**
     * Builds from a pre-sorted list. Skips topological sorting, only computes levels.
     */
    public static <T extends ModPlugin> DependencyGraph<T> buildFromSorted(Collection<T> sortedPlugins) {
        Checks.checkNotNull(sortedPlugins, "sortedPlugins");
        var id2Plugin = collectPlugins(sortedPlugins);
        var mutableGraph = buildGraph(id2Plugin);
        var sortedIds = new ArrayList<>(id2Plugin.keySet());

        return new DependencyGraph<>(
                ImmutableGraph.copyOf(mutableGraph),
                MutableLists.withAll(id2Plugin.values()).toImmutable(),
                computeLevels(sortedIds, mutableGraph, id2Plugin).toImmutable(),
                Collections.unmodifiableMap(id2Plugin)
        );
    }

    //endregion

    //region accessors

    public ImmutableList<T> sorted() {
        return sorted;
    }

    /**
     * Plugins grouped by dependency level. Same-level plugins are independent.
     */
    public ImmutableList<LoadingLevel<T>> levels() {
        return levels;
    }

    public int depth() {
        return levels.size();
    }

    public @Nullable T pluginById(Namespace pluginId) {
        return pluginById.get(pluginId);
    }

    //endregion

    //region queries

    public Set<Namespace> directDependenciesOf(Namespace pluginId) {
        Checks.checkNotNull(pluginId, "pluginId");
        if (!graph.nodes().contains(pluginId)) return Collections.emptySet();
        return graph.predecessors(pluginId);
    }

    public Set<Namespace> transitiveDependenciesOf(Namespace pluginId) {
        Checks.checkNotNull(pluginId, "pluginId");
        if (!graph.nodes().contains(pluginId)) return Collections.emptySet();

        Set<Namespace> visited = new HashSet<>();
        Deque<Namespace> queue = new ArrayDeque<>(graph.predecessors(pluginId));
        while (!queue.isEmpty()) {
            Namespace current = queue.poll();
            if (visited.add(current)) {
                queue.addAll(graph.predecessors(current));
            }
        }
        return Collections.unmodifiableSet(visited);
    }

    public Set<Namespace> directDependantsOf(Namespace pluginId) {
        Checks.checkNotNull(pluginId, "pluginId");
        if (!graph.nodes().contains(pluginId)) return Collections.emptySet();
        return graph.successors(pluginId);
    }

    public boolean areIndependent(Namespace a, Namespace b) {
        Checks.checkNotNull(a, "a");
        Checks.checkNotNull(b, "b");
        return !transitiveDependenciesOf(a).contains(b)
                && !transitiveDependenciesOf(b).contains(a);
    }

    //endregion

    //region internal

    private static <T extends ModPlugin> Map<Namespace, T> collectPlugins(Collection<T> plugins) {
        var id2Plugin = new LinkedHashMap<Namespace, T>();
        for (T plugin : plugins) {
            var previous = id2Plugin.put(plugin.pluginId(), plugin);
            if (previous != null) {
                throw new IllegalStateException("Duplicate plugin id: " + plugin.pluginId());
            }
        }
        return id2Plugin;
    }

    private static <T extends ModPlugin> MutableGraph<Namespace> buildGraph(Map<Namespace, T> id2Plugin) {
        MutableGraph<Namespace> graph = GraphBuilder.directed().build();
        for (var id : id2Plugin.keySet()) {
            graph.addNode(id);
        }
        for (var plugin : id2Plugin.values()) {
            for (var dependency : plugin.dependencies()) {
                if (dependency.order() == PluginDependency.Order.none) continue;
                if (!id2Plugin.containsKey(dependency.pluginId())) continue;

                switch (dependency.order()) {
                    case before -> graph.putEdge(plugin.pluginId(), dependency.pluginId());
                    case after -> graph.putEdge(dependency.pluginId(), plugin.pluginId());
                }
            }
        }
        return graph;
    }

    private static <T extends ModPlugin> MutableList<LoadingLevel<T>> computeLevels(
            List<Namespace> sortedIds,
            MutableGraph<Namespace> graph,
            Map<Namespace, T> id2Plugin
    ) {
        var levelMap = new Object2IntOpenHashMap<Namespace>();
        int maxLevel = -1;

        for (var id : sortedIds) {
            int predecessorLevel = -1;
            for (var pred : graph.predecessors(id)) {
                int predLevel = levelMap.getInt(pred);
                if (predLevel != -1) {
                    predecessorLevel = Math.max(predecessorLevel, predLevel);
                }
            }
            int level = predecessorLevel + 1;
            levelMap.put(id, level);
            maxLevel = Math.max(maxLevel, level);
        }

        MutableList<LoadingLevel<T>> levels = MutableLists.empty();
        for (int level = 0; level <= maxLevel; level++) {
            MutableList<T> levelPlugins = MutableLists.empty();
            for (var id : sortedIds) {
                if (levelMap.getOrDefault(id, -1) == level) {
                    T plugin = id2Plugin.get(id);
                    if (plugin != null) levelPlugins.add(plugin);
                }
            }
            if (levelPlugins.notEmpty()) {
                levels.add(new LoadingLevel<>(level, levelPlugins.toImmutable()));
            }
        }
        return levels;
    }

    //endregion

    /**
     * @param depth   the level depth (0 = root, no dependencies)
     * @param plugins the plugins at this level
     */
    public record LoadingLevel<T extends ModPlugin>(int depth, ImmutableList<T> plugins) {
    }

}
