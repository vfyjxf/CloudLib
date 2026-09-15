package dev.vfyjxf.cloudlib.api.plugin;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.CloudNamespaces;
import dev.vfyjxf.cloudlib.api.util.MutableLists;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CompletableFuture;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@NotNullByDefault
public class PluginDispatcherTest {

    @Test
    void testDispatchParallel() {
        var a = plugin("a");
        var b = plugin("b", dep("a", PluginDependency.Order.after));
        var c = plugin("c", dep("a", PluginDependency.Order.after));

        var graph = DependencyGraph.build(List.of(a, b, c));
        var dispatcher = PluginDispatcher.fromGraph(graph);
        var counter = new AtomicInteger(0);

        dispatcher.dispatch(plugin -> counter.incrementAndGet());

        assertEquals(3, counter.get());
    }

    @Test
    void testDispatchRespectsOrder() {
        // A → B → C (strict chain)
        var a = plugin("a");
        var b = plugin("b", dep("a", PluginDependency.Order.after));
        var c = plugin("c", dep("b", PluginDependency.Order.after));

        var graph = DependencyGraph.build(List.of(a, b, c));
        var dispatcher = PluginDispatcher.fromGraph(graph);
        var order = Collections.synchronizedList(new ArrayList<String>());

        dispatcher.dispatch(plugin -> order.add(plugin.pluginId().path()));

        assertEquals(List.of("a", "b", "c"), order);
    }

    @Test
    void testDispatchAll() {
        var a = plugin("a");
        var b = plugin("b");

        var graph = DependencyGraph.build(List.of(a, b));
        var dispatcher = PluginDispatcher.fromGraph(graph);
        var log = Collections.synchronizedList(new ArrayList<String>());

        dispatcher.dispatchAll(
            plugin -> log.add("phase1:" + plugin.pluginId().path()),
            plugin -> log.add("phase2:" + plugin.pluginId().path())
        );

        // Phase 1 must complete before phase 2
        int phase1LastIndex = Math.max(log.indexOf("phase1:a"), log.indexOf("phase1:b"));
        int phase2FirstIndex = Math.min(log.indexOf("phase2:a"), log.indexOf("phase2:b"));
        assertTrue(phase1LastIndex < phase2FirstIndex,
            "Phase 1 should complete before phase 2 starts. Log: " + log);
    }

    @Test
    void testDispatchSequential() {
        var a = plugin("a");
        var b = plugin("b", dep("a", PluginDependency.Order.after));
        var c = plugin("c", dep("b", PluginDependency.Order.after));

        var graph = DependencyGraph.build(List.of(a, b, c));
        var dispatcher = PluginDispatcher.fromGraph(graph);
        var order = new ArrayList<String>();

        dispatcher.dispatchSequential(plugin -> order.add(plugin.pluginId().path()));

        assertEquals(List.of("a", "b", "c"), order);
    }

    @Test
    void testDispatchAsync() {
        var a = plugin("a");
        var b = plugin("b", dep("a", PluginDependency.Order.after));

        var graph = DependencyGraph.build(List.of(a, b));
        var dispatcher = PluginDispatcher.fromGraph(graph);
        var counter = new AtomicInteger(0);

        dispatcher.dispatchAsync(plugin ->
            CompletableFuture.runAsync(() -> counter.incrementAndGet())
        ).join();

        assertEquals(2, counter.get());
    }

    @Test
    void testDispatchAsyncOn() {
        var a = plugin("a");
        var b = plugin("b");

        var graph = DependencyGraph.build(List.of(a, b));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        var dispatcher = PluginDispatcher.fromGraph(graph, executor);
        var counter = new AtomicInteger(0);

        try {
            dispatcher.dispatchAsyncOn(plugin ->
                CompletableFuture.runAsync(() -> counter.incrementAndGet(), executor)
            ).join();

            assertEquals(2, counter.get());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void testDispatchException() {
        var a = plugin("a");
        var b = plugin("b", dep("a", PluginDependency.Order.after));

        var graph = DependencyGraph.build(List.of(a, b));
        var dispatcher = PluginDispatcher.fromGraph(graph);

        var exception = assertThrows(PluginLoadingException.class, () ->
            dispatcher.dispatch(plugin -> {
                if (plugin.pluginId().path().equals("a")) {
                    throw new RuntimeException("Test failure");
                }
            })
        );

        assertEquals(1, exception.failures().size());
        assertEquals(CloudNamespaces.ofMod("a"), exception.failures().getFirst().pluginId());
    }

    @Test
    void testParallelismWithinLevel() {
        var a = plugin("a");
        var b = plugin("b");
        var c = plugin("c");
        var d = plugin("d");

        var graph = DependencyGraph.build(List.of(a, b, c, d));
        assertEquals(1, graph.depth());

        var threadNames = Collections.synchronizedSet(new java.util.HashSet<String>());
        ExecutorService executor = Executors.newFixedThreadPool(4);
        var dispatcher = PluginDispatcher.fromGraph(graph, executor);

        try {
            dispatcher.dispatch(plugin -> {
                threadNames.add(Thread.currentThread().getName());
                try {
                    Thread.sleep(50);
                } catch (InterruptedException ignored) {
                }
            });
        } finally {
            executor.shutdown();
        }

        assertTrue(threadNames.size() >= 2,
            "Expected parallel execution, but only " + threadNames.size() + " threads were used");
    }

    @Test
    void testMultipleFailuresCollected() {
        var a = plugin("a");
        var b = plugin("b");

        var graph = DependencyGraph.build(List.of(a, b));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        var dispatcher = PluginDispatcher.fromGraph(graph, executor);

        try {
            var exception = assertThrows(PluginLoadingException.class, () ->
                dispatcher.dispatch(plugin -> {
                    throw new RuntimeException("Failure in " + plugin.pluginId().path());
                })
            );

            assertEquals(2, exception.failures().size());
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void testEmptyDispatcher() {
        var graph = DependencyGraph.build(List.of());
        var dispatcher = PluginDispatcher.fromGraph(graph);
        var counter = new AtomicInteger(0);

        dispatcher.dispatch(plugin -> counter.incrementAndGet());

        assertEquals(0, counter.get());
    }

    @Test
    void testCreateFromSortedPlugins() {
        var a = plugin("a");
        var b = plugin("b", dep("a", PluginDependency.Order.after));

        var dispatcher = PluginDispatcher.create(
            MutableLists.of(a, b)
        );

        assertEquals(2, dispatcher.plugins().size());
        assertEquals(2, dispatcher.graph().depth());
    }

    @Test
    void testDispatchAllPhaseSeparation() {
        // Diamond: A → B, C → D
        var a = plugin("a");
        var b = plugin("b", dep("a", PluginDependency.Order.after));
        var c = plugin("c", dep("a", PluginDependency.Order.after));
        var d = plugin("d", dep("b", PluginDependency.Order.after), dep("c", PluginDependency.Order.after));

        var graph = DependencyGraph.build(List.of(a, b, c, d));
        var dispatcher = PluginDispatcher.fromGraph(graph);
        var log = Collections.synchronizedList(new ArrayList<String>());

        dispatcher.dispatchAll(
            plugin -> log.add("register:" + plugin.pluginId().path()),
            plugin -> log.add("init:" + plugin.pluginId().path())
        );

        // All register events must complete before any init event
        int lastRegister = -1;
        int firstInit = Integer.MAX_VALUE;
        for (int i = 0; i < log.size(); i++) {
            if (log.get(i).startsWith("register:")) lastRegister = Math.max(lastRegister, i);
            if (log.get(i).startsWith("init:")) firstInit = Math.min(firstInit, i);
        }
        assertTrue(lastRegister < firstInit,
            "All register events should complete before init starts. Log: " + log);
    }

    @Test
    void testSameLevelPluginsRunConcurrently() throws InterruptedException {
        // 4 independent plugins → all at level 0 → must run in parallel.
        // Each plugin waits on a latch that only releases when ALL 4 are running.
        // If not parallel, this deadlocks → timeout fails the test.
        var a = plugin("a");
        var b = plugin("b");
        var c = plugin("c");
        var d = plugin("d");

        var graph = DependencyGraph.build(List.of(a, b, c, d));
        assertEquals(1, graph.depth(), "All plugins should be at the same level");

        ExecutorService executor = Executors.newFixedThreadPool(4);
        var dispatcher = PluginDispatcher.fromGraph(graph, executor);
        var allArrived = new CountDownLatch(4);
        var proceed = new CountDownLatch(1);

        try {
            dispatcher.dispatch(plugin -> {
                allArrived.countDown();
                try {
                    // Wait for all 4 to arrive — proves they are running concurrently
                    assertTrue(allArrived.await(5, TimeUnit.SECONDS),
                        "Timed out waiting for parallel execution: not all plugins arrived");
                    proceed.await(5, TimeUnit.SECONDS);
                } catch (InterruptedException e) {
                    Thread.currentThread().interrupt();
                }
            });
        } finally {
            proceed.countDown();
            executor.shutdown();
        }
    }

    @Test
    void testDifferentLevelsRunSequentially() {
        // A → B → C: three levels, must run in strict order, not parallel across levels.
        var a = plugin("a");
        var b = plugin("b", dep("a", PluginDependency.Order.after));
        var c = plugin("c", dep("b", PluginDependency.Order.after));

        var graph = DependencyGraph.build(List.of(a, b, c));
        assertEquals(3, graph.depth());

        var dispatcher = PluginDispatcher.fromGraph(graph);
        var timestamps = Collections.synchronizedList(new ArrayList<long[]>());

        dispatcher.dispatch(plugin -> {
            long start = System.nanoTime();
            try { Thread.sleep(30); } catch (InterruptedException ignored) {}
            long end = System.nanoTime();
            timestamps.add(new long[]{start, end});
        });

        assertEquals(3, timestamps.size());
        // Each level's start must be after the previous level's end
        for (int i = 1; i < timestamps.size(); i++) {
            assertTrue(timestamps.get(i)[0] >= timestamps.get(i - 1)[1],
                "Level " + i + " started before level " + (i - 1) + " finished");
        }
    }

    @Test
    void testDiamondParallelismAndOrdering() throws InterruptedException {
        // Diamond: A → {B, C} → D
        // Level 0: A, Level 1: B+C (parallel), Level 2: D
        var a = plugin("a");
        var b = plugin("b", dep("a", PluginDependency.Order.after));
        var c = plugin("c", dep("a", PluginDependency.Order.after));
        var d = plugin("d", dep("b", PluginDependency.Order.after), dep("c", PluginDependency.Order.after));

        var graph = DependencyGraph.build(List.of(a, b, c, d));
        assertEquals(3, graph.depth());

        ExecutorService executor = Executors.newFixedThreadPool(4);
        var dispatcher = PluginDispatcher.fromGraph(graph, executor);

        // B and C must run concurrently (level 1)
        var bothArrived = new CountDownLatch(2);
        var order = Collections.synchronizedList(new ArrayList<String>());

        try {
            dispatcher.dispatch(plugin -> {
                String name = plugin.pluginId().path();
                if (name.equals("b") || name.equals("c")) {
                    bothArrived.countDown();
                    try {
                        assertTrue(bothArrived.await(5, TimeUnit.SECONDS),
                            "B and C should run in parallel but timed out");
                    } catch (InterruptedException e) {
                        Thread.currentThread().interrupt();
                    }
                }
                order.add(name);
            });
        } finally {
            executor.shutdown();
        }

        // A must be first, D must be last
        assertEquals("a", order.getFirst());
        assertEquals("d", order.getLast());
        assertEquals(4, order.size());
    }

    @Test
    void testAsyncDispatchParallelWithinLevel() throws Exception {
        var a = plugin("a");
        var b = plugin("b");
        var c = plugin("c");

        var graph = DependencyGraph.build(List.of(a, b, c));
        var dispatcher = PluginDispatcher.fromGraph(graph);

        var allArrived = new CountDownLatch(3);

        dispatcher.dispatchAsync(plugin -> CompletableFuture.runAsync(() -> {
            allArrived.countDown();
            try {
                assertTrue(allArrived.await(5, TimeUnit.SECONDS),
                    "Async dispatch should run same-level plugins concurrently");
            } catch (InterruptedException e) {
                Thread.currentThread().interrupt();
            }
        })).get(10, TimeUnit.SECONDS);
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
