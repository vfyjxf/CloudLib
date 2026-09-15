package dev.vfyjxf.cloudlib.api.plugin;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.CloudNamespaces;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.*;

@NotNullByDefault
public class DependencyGraphTest {

    @Test
    void testLevelDecomposition() {
        // A has no deps (level 0)
        // B depends on A (level 1)
        // C depends on A (level 1) — parallel with B
        // D depends on B and C (level 2)
        var a = plugin("a");
        var b = plugin("b", dep("a", PluginDependency.Order.after));
        var c = plugin("c", dep("a", PluginDependency.Order.after));
        var d = plugin("d", dep("b", PluginDependency.Order.after), dep("c", PluginDependency.Order.after));

        var graph = DependencyGraph.build(List.of(a, b, c, d));

        assertEquals(3, graph.depth());
        assertEquals(1, graph.levels().get(0).plugins().size()); // A
        assertEquals(2, graph.levels().get(1).plugins().size()); // B, C
        assertEquals(1, graph.levels().get(2).plugins().size()); // D
    }

    @Test
    void testIndependentPlugins() {
        var a = plugin("a");
        var b = plugin("b");
        var c = plugin("c", dep("a", PluginDependency.Order.after));

        var graph = DependencyGraph.build(List.of(a, b, c));

        assertTrue(graph.areIndependent(CloudNamespaces.ofMod("a"), CloudNamespaces.ofMod("b")));
        assertFalse(graph.areIndependent(CloudNamespaces.ofMod("a"), CloudNamespaces.ofMod("c")));
    }

    @Test
    void testTransitiveDependencies() {
        var a = plugin("a");
        var b = plugin("b", dep("a", PluginDependency.Order.after));
        var c = plugin("c", dep("b", PluginDependency.Order.after));

        var graph = DependencyGraph.build(List.of(a, b, c));

        var transitive = graph.transitiveDependenciesOf(CloudNamespaces.ofMod("c"));
        assertEquals(2, transitive.size());
        assertTrue(transitive.contains(CloudNamespaces.ofMod("a")));
        assertTrue(transitive.contains(CloudNamespaces.ofMod("b")));
    }

    @Test
    void testDirectDependants() {
        var a = plugin("a");
        var b = plugin("b", dep("a", PluginDependency.Order.after));
        var c = plugin("c", dep("a", PluginDependency.Order.after));

        var graph = DependencyGraph.build(List.of(a, b, c));

        var dependants = graph.directDependantsOf(CloudNamespaces.ofMod("a"));
        assertEquals(2, dependants.size());
        assertTrue(dependants.contains(CloudNamespaces.ofMod("b")));
        assertTrue(dependants.contains(CloudNamespaces.ofMod("c")));
    }

    @Test
    void testSinglePlugin() {
        var a = plugin("a");
        var graph = DependencyGraph.build(List.of(a));

        assertEquals(1, graph.depth());
        assertEquals(1, graph.sorted().size());
        assertEquals(CloudNamespaces.ofMod("a"), graph.sorted().getFirst().pluginId());
    }

    @Test
    void testEmptyGraph() {
        var graph = DependencyGraph.build(List.of());

        assertEquals(0, graph.depth());
        assertTrue(graph.sorted().isEmpty());
        assertTrue(graph.levels().isEmpty());
    }

    @Test
    void testDuplicatePluginId() {
        var a1 = plugin("a");
        var a2 = plugin("a");

        assertThrows(IllegalStateException.class, () -> DependencyGraph.build(List.of(a1, a2)));
    }

    @Test
    void testBuildFromSorted() {
        var a = plugin("a");
        var b = plugin("b", dep("a", PluginDependency.Order.after));
        var c = plugin("c", dep("a", PluginDependency.Order.after));

        // Already sorted: a, b, c
        var graph = DependencyGraph.buildFromSorted(List.of(a, b, c));

        assertEquals(2, graph.depth());
        assertEquals(1, graph.levels().get(0).plugins().size()); // A
        assertEquals(2, graph.levels().get(1).plugins().size()); // B, C
    }

    @Test
    void testPluginById() {
        var a = plugin("a");
        var b = plugin("b");

        var graph = DependencyGraph.build(List.of(a, b));

        assertSame(a, graph.pluginById(CloudNamespaces.ofMod("a")));
        assertSame(b, graph.pluginById(CloudNamespaces.ofMod("b")));
        assertNull(graph.pluginById(CloudNamespaces.ofMod("nonexistent")));
    }

    @Test
    void testBeforeOrder() {
        // A declares BEFORE B → A runs before B
        var a = plugin("a", new PluginDependency(CloudNamespaces.ofMod("b"), PluginDependency.Order.before, PluginDependency.Constraint.required));
        var b = plugin("b");

        var graph = DependencyGraph.build(List.of(a, b));

        assertEquals(2, graph.depth());
        assertEquals(CloudNamespaces.ofMod("a"), graph.sorted().get(0).pluginId());
        assertEquals(CloudNamespaces.ofMod("b"), graph.sorted().get(1).pluginId());
    }

    @Test
    void testMissingDependencyIgnored() {
        // B depends on C (not present) — should still work, edge just not created
        var a = plugin("a");
        var b = plugin("b", dep("c", PluginDependency.Order.after));

        var graph = DependencyGraph.build(List.of(a, b));

        // Both at level 0 since the dependency target is missing
        assertEquals(1, graph.depth());
        assertEquals(2, graph.levels().get(0).plugins().size());
    }

    @Test
    void testComplexDiamond() {
        //     A (level 0)
        //    / \
        //   B   C (level 1)
        //    \ /
        //     D (level 2)
        //     |
        //     E (level 3)
        var a = plugin("a");
        var b = plugin("b", dep("a", PluginDependency.Order.after));
        var c = plugin("c", dep("a", PluginDependency.Order.after));
        var d = plugin("d", dep("b", PluginDependency.Order.after), dep("c", PluginDependency.Order.after));
        var e = plugin("e", dep("d", PluginDependency.Order.after));

        var graph = DependencyGraph.build(List.of(a, b, c, d, e));

        assertEquals(4, graph.depth());
        assertEquals(0, graph.levels().get(0).depth());
        assertEquals(1, graph.levels().get(1).depth());
        assertEquals(2, graph.levels().get(2).depth());
        assertEquals(3, graph.levels().get(3).depth());

        var transE = graph.transitiveDependenciesOf(CloudNamespaces.ofMod("e"));
        assertEquals(4, transE.size()); // a, b, c, d
    }

    //region helpers

    private static ModPlugin plugin(String name, PluginDependency... deps) {
        return new SimplePlugin(CloudNamespaces.ofMod(name), Set.of(deps));
    }

    private static PluginDependency dep(String name, PluginDependency.Order order) {
        return new PluginDependency(CloudNamespaces.ofMod(name), order, PluginDependency.Constraint.required);
    }

    private record SimplePlugin(Namespace pluginId, Set<PluginDependency> dependencies) implements ModPlugin {}

    //endregion

}
