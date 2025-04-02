package dev.vfyjxf.cloudlib.api.performer;

import dev.vfyjxf.cloudlib.utils.Checks;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.jetbrains.annotations.NotNull;

public class PerformerContainer {

    private final MutableMap<Scenario<?>, @NotNull Performer<?>> performers = Maps.mutable.empty();
    private final MutableMap<CompositeScenario<?>, @NotNull MergeablePerformer<?>> mergeablePerformers = Maps.mutable.empty();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> void add(@NotNull Scenario<T> scenario, @NotNull Performer<T> performer) {
        var existing = performers.get(scenario);
        if (existing == null) {
            performers.put(scenario, performer);
        } else if (existing instanceof MutablePerformer mutableperformer) {
            mutableperformer.put(performer);
        } else {
            throw new IllegalArgumentException("performer already exists for scenario: " + scenario);
        }
    }

    public boolean has(@NotNull Scenario<?> scenario) {
        return performers.get(scenario) != null;
    }

    public <T> void remove(@NotNull Scenario<T> scenario) {
        performers.remove(scenario);
    }

    @SuppressWarnings({"unchecked"})
    public <T> T get(@NotNull Scenario<T> scenario) {
        return (T) performers.get(scenario).performer();
    }

    @SuppressWarnings({"unchecked"})
    public <T> void add(@NotNull CompositeScenario<T> scenario, @NotNull T performer) {
        MergeablePerformer<T> mergeableperformer = (MergeablePerformer<T>) mergeablePerformers.computeIfAbsent(scenario, k -> new SimpleMergeablePerformer<>(scenario));
        mergeableperformer.put(performer);
    }

    @SuppressWarnings({"unchecked"})
    public <T> void add(@NotNull CompositeScenario<T> scenario, @NotNull T performer, int priority) {
        MergeablePerformer<T> mergeableperformer = (MergeablePerformer<T>) mergeablePerformers.computeIfAbsent(scenario, k -> new SimpleMergeablePerformer<>(scenario));
        mergeableperformer.put(performer, priority);
    }

    public <T> void addWeak(@NotNull CompositeScenario<T> scenario, @NotNull T performer, @NotNull Object reference) {
        addWeak(scenario, performer, PerformerPriorities.DEFAULT, reference);
    }

    @SuppressWarnings({"unchecked"})
    public <T> void addWeak(@NotNull CompositeScenario<T> scenario, @NotNull T performer, int priority, @NotNull Object reference) {
        Checks.checkNotNull(scenario, "scenario");
        Checks.checkNotNull(performer, "performer");
        Checks.checkNotNull(reference, "reference");
        MergeablePerformer<T> mergeableperformer = (MergeablePerformer<T>) mergeablePerformers.computeIfAbsent(scenario, k -> new SimpleMergeablePerformer<>(scenario));
        mergeableperformer.putWeak(reference, performer, priority);
    }

    public boolean has(@NotNull CompositeScenario<?> scenario) {
        return mergeablePerformers.get(scenario) != null;
    }

    @SuppressWarnings({"unchecked"})
    public <T> void remove(@NotNull CompositeScenario<T> scenario, @NotNull T performer) {
        MergeablePerformer<T> mergeableperformer = (MergeablePerformer<T>) mergeablePerformers.get(scenario);
        if (mergeableperformer != null) {
            mergeableperformer.remove(performer);
        }
    }

    public <T> void remove(@NotNull CompositeScenario<T> scenario) {
        mergeablePerformers.remove(scenario);
    }

    @NotNull
    @SuppressWarnings({"unchecked"})
    public <T> T get(@NotNull CompositeScenario<T> scenario) {
        return (T) mergeablePerformers.getIfAbsentPut(scenario, new SimpleMergeablePerformer<>(scenario)).performer();
    }

    private static class SimpleMergeablePerformer<T> extends MergeablePerformer<T> {
        private SimpleMergeablePerformer(CompositeScenario<T> scenario) {
            super(scenario.merger());
        }
    }
}
