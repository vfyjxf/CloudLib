package dev.vfyjxf.cloudlib.api.plugin;

import dev.vfyjxf.cloudlib.api.util.MutableLists;
import dev.vfyjxf.cloudlib.util.Checks;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;
import org.slf4j.Logger;

import java.util.concurrent.CompletableFuture;
import java.util.concurrent.Executor;
import java.util.function.Consumer;

/**
 * Batches independent dispatch events and executes them in parallel.
 * Reusable across multiple {@link #execute()} calls.
 *
 * @see PluginDispatcher#createQueue()
 */
public final class DispatchQueue<T extends ModPlugin> {

    private final PluginDispatcher<T> dispatcher;
    private final Executor executor;
    private final MutableList<NamedTask<T>> pending;

    DispatchQueue(PluginDispatcher<T> dispatcher, Executor executor) {
        this.dispatcher = dispatcher;
        this.executor = executor;
        this.pending = MutableLists.empty();
    }

    //region enqueue

    public DispatchQueue<T> enqueue(Consumer<T> event) {
        return enqueue(null, event);
    }

    public DispatchQueue<T> enqueue(@Nullable String name, Consumer<T> event) {
        Checks.checkNotNull(event, "event");
        pending.add(new NamedTask<>(name, event));
        return this;
    }

    //endregion

    //region execute

    public void execute() throws PluginLoadingException {
        executeBatch(null, null);
    }

    public void execute(ProgressTracker tracker) throws PluginLoadingException {
        Checks.checkNotNull(tracker, "tracker");
        executeBatch(tracker, null);
    }

    public void executeLogged(Logger logger) throws PluginLoadingException {
        Checks.checkNotNull(logger, "logger");
        executeBatch(null, logger);
    }

    public void executeLogged(Logger logger, ProgressTracker tracker) throws PluginLoadingException {
        Checks.checkNotNull(logger, "logger");
        Checks.checkNotNull(tracker, "tracker");
        executeBatch(tracker, logger);
    }

    //endregion

    //region query

    public int size() {
        return pending.size();
    }

    public boolean isEmpty() {
        return pending.isEmpty();
    }

    //endregion

    //region internal

    private void executeBatch(@Nullable ProgressTracker tracker, @Nullable Logger logger) throws PluginLoadingException {
        if (pending.isEmpty()) return;

        var batch = MutableLists.withAll(pending);
        pending.clear();

        if (logger != null) {
            var taskNames = batch.collect(t -> t.name() != null ? t.name() : "<unnamed>");
            logger.debug("DispatchQueue: executing {} tasks [{}]", batch.size(), taskNames.makeString(", "));
        }

        if (batch.size() == 1) {
            dispatchSingle(batch.getFirst(), tracker, logger);
            return;
        }

        float weight = tracker != null ? 1.0f / batch.size() : 0;
        MutableList<PluginLoadingException.Failure> failures = MutableLists.empty();

        CompletableFuture<?>[] futures = new CompletableFuture[batch.size()];
        batch.forEachWithIndex((task, index) -> {
            var progress = tracker != null ? tracker.phase(weight) : DispatchProgress.empty();
            futures[index] = CompletableFuture.runAsync(() -> {
                try {
                    dispatchTask(task, index, progress, logger);
                } catch (PluginLoadingException e) {
                    synchronized (failures) {
                        failures.addAll(e.failures());
                    }
                }
            }, executor);
        });

        CompletableFuture.allOf(futures).join();

        if (logger != null) logger.debug("DispatchQueue: batch completed");
        if (failures.notEmpty()) throw new PluginLoadingException(failures);
    }

    private void dispatchSingle(NamedTask<T> task, @Nullable ProgressTracker tracker, @Nullable Logger logger) throws PluginLoadingException {
        var progress = tracker != null ? tracker.phase(1) : DispatchProgress.empty();
        if (logger != null) {
            var name = task.name() != null ? task.name() : "dispatch";
            dispatcher.dispatchLogged(task.event(), name, logger, progress);
        } else {
            dispatcher.dispatch(task.event(), progress);
        }
    }

    private void dispatchTask(NamedTask<T> task, int index, DispatchProgress progress, @Nullable Logger logger) throws PluginLoadingException {
        if (logger != null) {
            var name = task.name() != null ? task.name() : "task-" + index;
            dispatcher.dispatchLogged(task.event(), name, logger, progress);
        } else {
            dispatcher.dispatch(task.event(), progress);
        }
    }

    //endregion

    private record NamedTask<T extends ModPlugin>(@Nullable String name, Consumer<T> event) {}

}
