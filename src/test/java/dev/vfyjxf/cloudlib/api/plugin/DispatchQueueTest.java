package dev.vfyjxf.cloudlib.api.plugin;

import dev.vfyjxf.cloudlib.api.annotation.NotNullByDefault;
import dev.vfyjxf.cloudlib.api.util.MutableLists;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import dev.vfyjxf.cloudlib.util.CloudNamespaces;
import org.junit.jupiter.api.Test;

import java.util.Collections;
import java.util.List;
import java.util.Set;
import java.util.concurrent.CountDownLatch;
import java.util.concurrent.ExecutorService;
import java.util.concurrent.Executors;
import java.util.concurrent.TimeUnit;
import java.util.concurrent.atomic.AtomicInteger;

import static org.junit.jupiter.api.Assertions.*;

@NotNullByDefault
public class DispatchQueueTest {

    @Test
    void testSingleTaskExecute() {
        var a = plugin("a");
        var b = plugin("b");

        var dispatcher = PluginDispatcher.create(MutableLists.of(a, b));
        var queue = dispatcher.createQueue();
        var counter = new AtomicInteger(0);

        queue.enqueue(plugin -> counter.incrementAndGet());
        queue.execute();

        assertEquals(2, counter.get());
    }

    @Test
    void testMultipleTasksExecuteInParallel() {
        var a = plugin("a");
        var b = plugin("b");

        var dispatcher = PluginDispatcher.create(MutableLists.of(a, b));
        ExecutorService executor = Executors.newFixedThreadPool(4);
        var queue = dispatcher.createQueue(executor);

        var threadNames = Collections.synchronizedSet(new java.util.HashSet<String>());

        try {
            queue.enqueue(plugin -> {
                threadNames.add(Thread.currentThread().getName());
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            });
            queue.enqueue(plugin -> {
                threadNames.add(Thread.currentThread().getName());
                try { Thread.sleep(50); } catch (InterruptedException ignored) {}
            });
            queue.execute();

            assertTrue(threadNames.size() >= 2,
                "Expected parallel task execution, but only " + threadNames.size() + " threads were used");
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void testQueueClearedAfterExecute() {
        var a = plugin("a");
        var dispatcher = PluginDispatcher.create(MutableLists.of(a));
        var queue = dispatcher.createQueue();

        queue.enqueue(plugin -> {});
        assertEquals(1, queue.size());

        queue.execute();
        assertTrue(queue.isEmpty());
        assertEquals(0, queue.size());
    }

    @Test
    void testQueueReusable() {
        var a = plugin("a");
        var dispatcher = PluginDispatcher.create(MutableLists.of(a));
        var queue = dispatcher.createQueue();
        var counter = new AtomicInteger(0);

        // First batch
        queue.enqueue(plugin -> counter.incrementAndGet());
        queue.execute();
        assertEquals(1, counter.get());

        // Second batch
        queue.enqueue(plugin -> counter.incrementAndGet());
        queue.execute();
        assertEquals(2, counter.get());
    }

    @Test
    void testExecuteEmptyQueue() {
        var a = plugin("a");
        var dispatcher = PluginDispatcher.create(MutableLists.of(a));
        var queue = dispatcher.createQueue();

        // Should not throw
        queue.execute();
    }

    @Test
    void testEnqueueChaining() {
        var a = plugin("a");
        var dispatcher = PluginDispatcher.create(MutableLists.of(a));
        var counter = new AtomicInteger(0);

        dispatcher.createQueue()
                  .enqueue(plugin -> counter.incrementAndGet())
                  .enqueue(plugin -> counter.incrementAndGet())
                  .execute();

        assertEquals(2, counter.get());
    }

    @Test
    void testBatchIsolation() {
        // Batch 1 completes before batch 2 starts
        var a = plugin("a");
        var dispatcher = PluginDispatcher.create(MutableLists.of(a));
        var queue = dispatcher.createQueue();
        var log = Collections.synchronizedList(new java.util.ArrayList<String>());

        queue.enqueue(plugin -> log.add("batch1-taskA"));
        queue.enqueue(plugin -> log.add("batch1-taskB"));
        queue.execute();

        queue.enqueue(plugin -> log.add("batch2-taskA"));
        queue.execute();

        // batch2 must come after all batch1 entries
        int lastBatch1 = Math.max(log.indexOf("batch1-taskA"), log.indexOf("batch1-taskB"));
        int firstBatch2 = log.indexOf("batch2-taskA");
        assertTrue(lastBatch1 < firstBatch2,
            "Batch 1 should complete before batch 2 starts. Log: " + log);
    }

    @Test
    void testFailureCollection() {
        var a = plugin("a");
        var b = plugin("b");

        var dispatcher = PluginDispatcher.create(MutableLists.of(a, b));
        ExecutorService executor = Executors.newFixedThreadPool(2);
        var queue = dispatcher.createQueue(executor);

        try {
            // Both tasks will fail for both plugins
            queue.enqueue(plugin -> { throw new RuntimeException("task1 fail"); });
            queue.enqueue(plugin -> { throw new RuntimeException("task2 fail"); });

            var exception = assertThrows(PluginLoadingException.class, queue::execute);
            assertTrue(exception.failures().size() >= 2);
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void testNamedTasks() {
        var a = plugin("a");
        var dispatcher = PluginDispatcher.create(MutableLists.of(a));
        var queue = dispatcher.createQueue();
        var counter = new AtomicInteger(0);

        queue.enqueue("registerContentType", plugin -> counter.incrementAndGet())
             .enqueue("registerRecipeSystem", plugin -> counter.incrementAndGet());

        assertEquals(2, queue.size());
        queue.execute();
        assertEquals(2, counter.get());
    }

    @Test
    void testDependencyOrderingPreservedInsideTask() {
        // A → B → C chain: even when dispatched from the queue, the level ordering is respected
        var a = plugin("a");
        var b = plugin("b", dep("a", PluginDependency.Order.after));
        var c = plugin("c", dep("b", PluginDependency.Order.after));

        var graph = DependencyGraph.build(List.of(a, b, c));
        var dispatcher = PluginDispatcher.fromGraph(graph);
        var queue = dispatcher.createQueue();
        var order = Collections.synchronizedList(new java.util.ArrayList<String>());

        queue.enqueue(plugin -> order.add(plugin.pluginId().path()));
        queue.execute();

        assertEquals(List.of("a", "b", "c"), order);
    }

    @Test
    void testQueueTasksRunConcurrently() throws InterruptedException {
        // 3 enqueued tasks must run in parallel.
        // Each task's dispatch event waits on a latch that only opens when all 3 tasks are active.
        var a = plugin("a");
        var dispatcher = PluginDispatcher.create(MutableLists.of(a));
        ExecutorService executor = Executors.newFixedThreadPool(4);
        var queue = dispatcher.createQueue(executor);

        var allTasksActive = new CountDownLatch(3);

        try {
            queue.enqueue(plugin -> {
                allTasksActive.countDown();
                try {
                    assertTrue(allTasksActive.await(5, TimeUnit.SECONDS),
                        "Task 1 timed out: not all tasks running concurrently");
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            queue.enqueue(plugin -> {
                allTasksActive.countDown();
                try {
                    assertTrue(allTasksActive.await(5, TimeUnit.SECONDS),
                        "Task 2 timed out: not all tasks running concurrently");
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            queue.enqueue(plugin -> {
                allTasksActive.countDown();
                try {
                    assertTrue(allTasksActive.await(5, TimeUnit.SECONDS),
                        "Task 3 timed out: not all tasks running concurrently");
                } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
            });
            queue.execute();
        } finally {
            executor.shutdown();
        }
    }

    @Test
    void testQueueTasksWithDependencyLevelsParallel() throws InterruptedException {
        // Plugins: A → {B, C} (diamond without D)
        // Queue has 2 tasks; within each task B and C should still run concurrently at level 1.
        var a = plugin("a");
        var b = plugin("b", dep("a", PluginDependency.Order.after));
        var c = plugin("c", dep("a", PluginDependency.Order.after));

        var graph = DependencyGraph.build(List.of(a, b, c));
        ExecutorService executor = Executors.newFixedThreadPool(8);
        var dispatcher = PluginDispatcher.fromGraph(graph, executor);
        var queue = dispatcher.createQueue(executor);

        // Per-task latch: B and C should run concurrently within each task's dispatch
        var task1BC = new CountDownLatch(2);
        var task2BC = new CountDownLatch(2);
        var order1 = Collections.synchronizedList(new java.util.ArrayList<String>());
        var order2 = Collections.synchronizedList(new java.util.ArrayList<String>());

        try {
            queue.enqueue(plugin -> {
                String name = plugin.pluginId().path();
                if (name.equals("b") || name.equals("c")) {
                    task1BC.countDown();
                    try {
                        assertTrue(task1BC.await(5, TimeUnit.SECONDS),
                            "Task1: B and C should run in parallel");
                    } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
                order1.add(name);
            });
            queue.enqueue(plugin -> {
                String name = plugin.pluginId().path();
                if (name.equals("b") || name.equals("c")) {
                    task2BC.countDown();
                    try {
                        assertTrue(task2BC.await(5, TimeUnit.SECONDS),
                            "Task2: B and C should run in parallel");
                    } catch (InterruptedException e) { Thread.currentThread().interrupt(); }
                }
                order2.add(name);
            });
            queue.execute();
        } finally {
            executor.shutdown();
        }

        // Both tasks should have dispatched A first
        assertEquals("a", order1.getFirst());
        assertEquals("a", order2.getFirst());
        assertEquals(3, order1.size());
        assertEquals(3, order2.size());
    }

    @Test
    void testSingleTaskNotParallelized() {
        // A single enqueued task should dispatch via the single-path optimization.
        var a = plugin("a");
        var b = plugin("b");
        var dispatcher = PluginDispatcher.create(MutableLists.of(a, b));
        var queue = dispatcher.createQueue();
        var counter = new AtomicInteger(0);

        queue.enqueue(plugin -> counter.incrementAndGet());
        queue.execute();

        assertEquals(2, counter.get());
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
