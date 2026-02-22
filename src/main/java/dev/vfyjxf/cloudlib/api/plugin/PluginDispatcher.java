package dev.vfyjxf.cloudlib.api.plugin;

import dev.vfyjxf.cloudlib.api.util.MutableLists;
import dev.vfyjxf.cloudlib.util.Checks;
import org.eclipse.collections.api.list.ImmutableList;
import org.eclipse.collections.api.list.MutableList;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.concurrent.ForkJoinPool;
import java.util.concurrent.atomic.AtomicInteger;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Dispatches plugin events in parallel while respecting dependency ordering.
 * <p>
 * Same-level plugins receive the event concurrently;
 * a level must fully complete before the next level begins.
 * <p>
 * Thread-safety of the event action and shared state is the caller's responsibility.
 *
 * @param <T> the plugin type
 * @see PluginLoader#loadAndDispatcher
 */
public final class PluginDispatcher<T extends ModPlugin> {

    private final ImmutableList<T> plugins;
    private final DependencyGraph<T> graph;
    private final Executor executor;

    private PluginDispatcher(ImmutableList<T> plugins, DependencyGraph<T> graph, Executor executor) {
        this.plugins = plugins;
        this.graph = graph;
        this.executor = executor;
    }

    //region factory

    public static <T extends ModPlugin> PluginDispatcher<T> create(MutableList<T> sortedPlugins) {
        return create(sortedPlugins, ForkJoinPool.commonPool());
    }

    public static <T extends ModPlugin> PluginDispatcher<T> create(MutableList<T> sortedPlugins, Executor executor) {
        Checks.checkNotNull(sortedPlugins, "sortedPlugins");
        Checks.checkNotNull(executor, "executor");
        var graph = DependencyGraph.buildFromSorted(sortedPlugins);
        return new PluginDispatcher<>(sortedPlugins.toImmutable(), graph, executor);
    }

    public static <T extends ModPlugin> PluginDispatcher<T> fromGraph(DependencyGraph<T> graph) {
        return fromGraph(graph, ForkJoinPool.commonPool());
    }

    public static <T extends ModPlugin> PluginDispatcher<T> fromGraph(DependencyGraph<T> graph, Executor executor) {
        Checks.checkNotNull(graph, "graph");
        Checks.checkNotNull(executor, "executor");
        return new PluginDispatcher<>(graph.sorted(), graph, executor);
    }

    //endregion

    //region parallel dispatch

    /**
     * Dispatches a single event. Same-level plugins run concurrently.
     */
    public void dispatch(Consumer<T> event) throws PluginLoadingException {
        dispatch(event, DispatchProgress.empty());
    }

    public void dispatch(Consumer<T> event, DispatchProgress progress) throws PluginLoadingException {
        Checks.checkNotNull(event, "event");
        Checks.checkNotNull(progress, "progress");
        progress.begin(plugins.size());
        var completed = new AtomicInteger(0);
        int total = plugins.size();
        for (var level : graph.levels()) {
            dispatchLevel(level, event, progress, completed, total);
        }
        progress.complete();
    }

    /**
     * Dispatches multiple events in order. Each event fully completes before the next starts.
     */
    @SafeVarargs
    public final void dispatchAll(Consumer<T>... events) throws PluginLoadingException {
        Checks.checkNotNull(events, "events");
        for (var event : events) {
            dispatch(event);
        }
    }

    @SafeVarargs
    public final void dispatchAll(ProgressTracker tracker, Consumer<T>... events) throws PluginLoadingException {
        Checks.checkNotNull(tracker, "tracker");
        Checks.checkNotNull(events, "events");
        float weight = events.length > 0 ? 1.0f / events.length : 0;
        for (var event : events) {
            dispatch(event, tracker.phase(weight));
        }
    }

    /**
     * Dispatches sequentially (no parallelism). Use when thread-safety cannot be guaranteed.
     */
    public void dispatchSequential(Consumer<T> event) {
        Checks.checkNotNull(event, "event");
        for (T plugin : plugins) {
            event.accept(plugin);
        }
    }

    //endregion

    //region async dispatch

    /**
     * Same-level futures run concurrently, levels are chained.
     */
    public CompletableFuture<Void> dispatchAsync(Function<T, CompletableFuture<Void>> asyncEvent) {
        Checks.checkNotNull(asyncEvent, "asyncEvent");
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (var level : graph.levels()) {
            var plugins = level.plugins();
            chain = chain.thenCompose(ignored -> {
                CompletableFuture<?>[] futures = new CompletableFuture[plugins.size()];
                plugins.forEachWithIndex((plugin, index) -> futures[index] = asyncEvent.apply(plugin));
                return CompletableFuture.allOf(futures);
            });
        }
        return chain;
    }

    /**
     * Like {@link #dispatchAsync}, but level transitions run on this dispatcher's executor.
     */
    public CompletableFuture<Void> dispatchAsyncOn(Function<T, CompletableFuture<Void>> asyncEvent) {
        Checks.checkNotNull(asyncEvent, "asyncEvent");
        CompletableFuture<Void> chain = CompletableFuture.completedFuture(null);
        for (var level : graph.levels()) {
            var plugins = level.plugins();
            chain = chain.thenComposeAsync(ignored -> {
                CompletableFuture<?>[] futures = new CompletableFuture[plugins.size()];
                plugins.forEachWithIndex((plugin, index) -> futures[index] = asyncEvent.apply(plugin));
                return CompletableFuture.allOf(futures);
            }, executor);
        }
        return chain;
    }

    //endregion

    //region logged dispatch

    public void dispatchLogged(Consumer<T> event, String eventName, Logger logger) throws PluginLoadingException {
        dispatchLogged(event, eventName, logger, DispatchProgress.empty());
    }

    public void dispatchLogged(Consumer<T> event, String eventName, Logger logger, DispatchProgress progress) throws PluginLoadingException {
        Checks.checkNotNull(event, "event");
        Checks.checkNotNull(eventName, "eventName");

        logger.debug("{}: dispatching to {} plugins ({} levels)",
            eventName, plugins.size(), graph.depth());

        progress.begin(plugins.size());
        var completed = new AtomicInteger(0);
        int total = plugins.size();

        for (var level : graph.levels()) {
            logger.debug("{}: level {} ({} plugins)",
                eventName, level.depth(), level.plugins().size());
            dispatchLevel(level, event, progress, completed, total);
        }

        progress.complete();
        logger.debug("{}: dispatch completed", eventName);
    }

    //endregion

    //region task queue

    public DispatchQueue<T> createQueue() {
        return new DispatchQueue<>(this, executor);
    }

    public DispatchQueue<T> createQueue(Executor queueExecutor) {
        Checks.checkNotNull(queueExecutor, "queueExecutor");
        return new DispatchQueue<>(this, queueExecutor);
    }

    //endregion

    //region accessors

    public ImmutableList<T> plugins() {
        return plugins;
    }

    public DependencyGraph<T> graph() {
        return graph;
    }

    //endregion

    //region internal

    private void dispatchLevel(
        DependencyGraph.LoadingLevel<T> level,
        Consumer<T> event,
        DispatchProgress progress,
        AtomicInteger completed,
        int total
    ) throws PluginLoadingException {
        var plugins = level.plugins();

        if (plugins.size() == 1) {
            T plugin = plugins.getFirst();
            try {
                event.accept(plugin);
                progress.advance(plugin.pluginId(), completed.incrementAndGet(), total);
            } catch (Exception e) {
                throw new PluginLoadingException(
                    MutableLists.of(new PluginLoadingException.Failure(plugin.pluginId(), e))
                );
            }
            return;
        }

        MutableList<PluginLoadingException.Failure> failures = MutableLists.empty();
        CompletableFuture<?>[] futures = new CompletableFuture[plugins.size()];
        for (int index = 0; index < plugins.size(); index++) {
            T plugin = plugins.get(index);
            futures[index] = CompletableFuture.runAsync(() -> {
                try {
                    event.accept(plugin);
                    progress.advance(plugin.pluginId(), completed.incrementAndGet(), total);
                } catch (Exception e) {
                    synchronized (failures) {
                        failures.add(new PluginLoadingException.Failure(plugin.pluginId(), e));
                    }
                }
            }, executor);
        }

        CompletableFuture.allOf(futures).join();

        if (failures.notEmpty()) {
            throw new PluginLoadingException(failures);
        }
    }

    //endregion

}
