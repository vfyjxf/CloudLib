package dev.vfyjxf.cloudlib.api.plugin;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.CloudNamespaces;
import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Set;
import java.util.concurrent.CopyOnWriteArrayList;

import static org.junit.jupiter.api.Assertions.*;

@NotNullByDefault
public class ProgressTrackerTest {

    //region basic percentage

    @Test
    void testSinglePhasePercentage() {
        var a = plugin("a");
        var b = plugin("b");
        var c = plugin("c");

        var graph = DependencyGraph.build(List.of(a, b, c));
        var dispatcher = PluginDispatcher.fromGraph(graph);
        var tracker = new ProgressTracker();

        dispatcher.dispatch(plugin -> {}, tracker.phase(1));

        assertEquals(100.0f, tracker.percentage(), 0.01f);
        assertTrue(tracker.isComplete());
    }

    @Test
    void testEmptyTrackerPercentage() {
        var tracker = new ProgressTracker();
        assertEquals(0.0f, tracker.percentage());
        assertFalse(tracker.isComplete());
        assertEquals(0, tracker.phaseCount());
    }

    @Test
    void testWeightedMultiPhasePercentage() {
        var a = plugin("a");

        var graph = DependencyGraph.build(List.of(a));
        var dispatcher = PluginDispatcher.fromGraph(graph);
        var tracker = new ProgressTracker();

        // Phase 1: weight 30
        dispatcher.dispatch(plugin -> {}, tracker.phase(30));
        // Phase 2 and 3 not yet dispatched
        var phase2 = tracker.phase(50);
        var phase3 = tracker.phase(20);

        // Only phase 1 (weight 30 out of 100) is complete
        assertEquals(30.0f, tracker.percentage(), 0.01f);
        assertFalse(tracker.isComplete());
        assertEquals(3, tracker.phaseCount());
    }

    @Test
    void testAllPhasesComplete() {
        var a = plugin("a");

        var graph = DependencyGraph.build(List.of(a));
        var dispatcher = PluginDispatcher.fromGraph(graph);
        var tracker = new ProgressTracker();

        dispatcher.dispatch(plugin -> {}, tracker.phase(30));
        dispatcher.dispatch(plugin -> {}, tracker.phase(50));
        dispatcher.dispatch(plugin -> {}, tracker.phase(20));

        assertEquals(100.0f, tracker.percentage(), 0.01f);
        assertTrue(tracker.isComplete());
    }

    @Test
    void testEqualWeightPhases() {
        var a = plugin("a");

        var graph = DependencyGraph.build(List.of(a));
        var dispatcher = PluginDispatcher.fromGraph(graph);
        var tracker = new ProgressTracker();

        dispatcher.dispatch(plugin -> {}, tracker.phase(1));
        tracker.phase(1); // registered but not dispatched
        tracker.phase(1); // registered but not dispatched

        // 1 out of 3 equal phases complete → 33.3%
        assertEquals(33.33f, tracker.percentage(), 0.5f);
    }

    //endregion

    //region listener

    @Test
    void testListenerCalledOnAdvance() {
        var a = plugin("a");
        var b = plugin("b");
        var c = plugin("c");

        var graph = DependencyGraph.build(List.of(a, b, c));
        var dispatcher = PluginDispatcher.fromGraph(graph);
        var percentages = new CopyOnWriteArrayList<Float>();
        var tracker = new ProgressTracker().withListener(t -> percentages.add(t.percentage()));

        dispatcher.dispatch(plugin -> {}, tracker.phase(1));

        // Should have received 3 advance callbacks + 1 complete = at least 3 percentage updates
        assertFalse(percentages.isEmpty());
        // Last percentage should be 100%
        assertEquals(100.0f, percentages.getLast(), 0.01f);
        // Percentages should be monotonically non-decreasing
        for (int i = 1; i < percentages.size(); i++) {
            assertTrue(percentages.get(i) >= percentages.get(i - 1),
                "Percentages should be non-decreasing: " + percentages);
        }
    }

    @Test
    void testListenerNotCalledWithoutListener() {
        // Ensure no NPE when no listener is set
        var a = plugin("a");
        var graph = DependencyGraph.build(List.of(a));
        var dispatcher = PluginDispatcher.fromGraph(graph);
        var tracker = new ProgressTracker();

        assertDoesNotThrow(() -> dispatcher.dispatch(plugin -> {}, tracker.phase(1)));
    }

    //endregion

    //region dispatchAll with tracker

    @Test
    void testDispatchAllWithTracker() {
        var a = plugin("a");
        var b = plugin("b");

        var graph = DependencyGraph.build(List.of(a, b));
        var dispatcher = PluginDispatcher.fromGraph(graph);
        var tracker = new ProgressTracker();

        dispatcher.dispatchAll(tracker,
            plugin -> {},
            plugin -> {},
            plugin -> {}
        );

        assertEquals(100.0f, tracker.percentage(), 0.01f);
        assertTrue(tracker.isComplete());
        assertEquals(3, tracker.phaseCount());
    }

    //endregion

    //region DispatchQueue with tracker

    @Test
    void testQueueExecuteWithTracker() {
        var a = plugin("a");
        var b = plugin("b");

        var graph = DependencyGraph.build(List.of(a, b));
        var dispatcher = PluginDispatcher.fromGraph(graph);
        var tracker = new ProgressTracker();

        var queue = dispatcher.createQueue();
        queue.enqueue(plugin -> {});
        queue.enqueue(plugin -> {});
        queue.execute(tracker);

        assertEquals(100.0f, tracker.percentage(), 0.01f);
        assertTrue(tracker.isComplete());
        assertEquals(2, tracker.phaseCount());
    }

    @Test
    void testQueueExecuteEmptyWithTracker() {
        var a = plugin("a");

        var graph = DependencyGraph.build(List.of(a));
        var dispatcher = PluginDispatcher.fromGraph(graph);
        var tracker = new ProgressTracker();

        var queue = dispatcher.createQueue();
        queue.execute(tracker); // empty queue, no phases registered

        assertEquals(0, tracker.phaseCount());
        assertEquals(0.0f, tracker.percentage());
    }

    //endregion

    //region percentage granularity

    @Test
    void testPercentageWithMultiplePlugins() {
        // 4 plugins, single phase: each advance should be ~25%
        var a = plugin("a");
        var b = plugin("b", dep("a", PluginDependency.Order.after));
        var c = plugin("c", dep("b", PluginDependency.Order.after));
        var d = plugin("d", dep("c", PluginDependency.Order.after));

        var graph = DependencyGraph.build(List.of(a, b, c, d));
        var dispatcher = PluginDispatcher.fromGraph(graph);
        var percentages = new CopyOnWriteArrayList<Float>();
        var tracker = new ProgressTracker().withListener(t -> percentages.add(t.percentage()));

        dispatcher.dispatch(plugin -> {}, tracker.phase(1));

        // Should see roughly 25%, 50%, 75%, 100%
        assertEquals(4, percentages.size() - 1); // -1 because complete() also fires
        // Last should be 100%
        assertEquals(100.0f, percentages.getLast(), 0.01f);
    }

    @Test
    void testPhaseCountMatchesRegistrations() {
        var tracker = new ProgressTracker();
        tracker.phase(10);
        tracker.phase(20);
        tracker.phase(30);
        assertEquals(3, tracker.phaseCount());
    }

    //endregion

    //region error scenarios

    @Test
    void testInvalidWeightThrows() {
        var tracker = new ProgressTracker();
        assertThrows(IllegalArgumentException.class, () -> tracker.phase(0));
        assertThrows(IllegalArgumentException.class, () -> tracker.phase(-1));
    }

    @Test
    void testProgressDoesNotReach100OnError() {
        var a = plugin("a");
        var b = plugin("b");

        var graph = DependencyGraph.build(List.of(a, b));
        var dispatcher = PluginDispatcher.fromGraph(graph);
        var tracker = new ProgressTracker();
        var phase = tracker.phase(1);

        assertThrows(PluginLoadingException.class, () ->
            dispatcher.dispatch(plugin -> {
                if (plugin.pluginId().path().equals("a")) {
                    throw new RuntimeException("fail");
                }
            }, phase)
        );

        // Should not be 100% because dispatch was aborted
        assertTrue(tracker.percentage() < 100.0f);
    }

    //endregion

    //region DispatchProgress.empty

    @Test
    void testEmptyProgressIsNoOp() {
        var empty = DispatchProgress.empty();
        assertDoesNotThrow(() -> {
            empty.begin(10);
            empty.advance(CloudNamespaces.ofMod("test"), 1, 10);
            empty.complete();
        });
    }

    //endregion

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
