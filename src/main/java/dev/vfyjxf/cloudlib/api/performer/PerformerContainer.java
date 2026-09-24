package dev.vfyjxf.cloudlib.api.performer;

import dev.vfyjxf.cloudlib.util.Checks;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;

public class PerformerContainer {

    private final MutableMap<Scenario<?>, Performer<?>> performers = Maps.mutable.empty();
    private final MutableMap<CompositeScenario<?>, MergeablePerformer<?>> mergeablePerformers = Maps.mutable.empty();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> void add(Scenario<T> scenario, Performer<T> performer) {
        var existing = performers.get(scenario);
        if (existing == null) {
            performers.put(scenario, performer);
        } else if (existing instanceof MutablePerformer mutableperformer) {
            mutableperformer.put(performer);
        } else {
            throw new IllegalArgumentException("performer already exists for scenario: " + scenario);
        }
    }

    public boolean has(Scenario<?> scenario) {
        return performers.get(scenario) != null;
    }

    public <T> void remove(Scenario<T> scenario) {
        performers.remove(scenario);
    }

    @SuppressWarnings({"unchecked"})
    public <T> T get(Scenario<T> scenario) {
        return (T) Checks.checkNotNull(
            performers.get(scenario),
            () -> new IllegalArgumentException("no performer for scenario: " + scenario)
        ).performer();
    }

    @SuppressWarnings({"unchecked"})
    public <T> void add(CompositeScenario<T> scenario, T performer) {
        MergeablePerformer<T> mergeableperformer = (MergeablePerformer<T>) mergeablePerformers
                .computeIfAbsent(scenario, k -> new SimpleMergeablePerformer<>(scenario));
        mergeableperformer.put(performer);
    }

    @SuppressWarnings({"unchecked"})
    public <T> void add(CompositeScenario<T> scenario, T performer, int priority) {
        MergeablePerformer<T> mergeableperformer = (MergeablePerformer<T>) mergeablePerformers
                .computeIfAbsent(scenario, k -> new SimpleMergeablePerformer<>(scenario));
        mergeableperformer.put(performer, priority);
    }

    public <T> void addWeak(CompositeScenario<T> scenario, T performer, Object reference) {
        addWeak(scenario, performer, PerformerPriorities.normal, reference);
    }

    @SuppressWarnings({"unchecked"})
    public <T> void addWeak(CompositeScenario<T> scenario, T performer, int priority, Object reference) {
        Checks.checkNotNull(scenario, "scenario");
        Checks.checkNotNull(performer, "performer");
        Checks.checkNotNull(reference, "reference");
        MergeablePerformer<T> mergeableperformer = (MergeablePerformer<T>) mergeablePerformers
                .computeIfAbsent(scenario, k -> new SimpleMergeablePerformer<>(scenario));
        mergeableperformer.putWeak(reference, performer, priority);
    }

    public boolean has(CompositeScenario<?> scenario) {
        return mergeablePerformers.get(scenario) != null;
    }

    @SuppressWarnings({"unchecked"})
    public <T> void remove(CompositeScenario<T> scenario, T performer) {
        MergeablePerformer<T> mergeableperformer = (MergeablePerformer<T>) mergeablePerformers.get(scenario);
        if (mergeableperformer != null) {
            mergeableperformer.remove(performer);
        }
    }

    public <T> void remove(CompositeScenario<T> scenario) {
        mergeablePerformers.remove(scenario);
    }

    @SuppressWarnings({"unchecked"})
    public <T> T get(CompositeScenario<T> scenario) {
        MergeablePerformer<T> mergeableperformer = (MergeablePerformer<T>) mergeablePerformers
                .getIfAbsentPut(scenario, () -> new SimpleMergeablePerformer<>(scenario));
        return Checks.checkNotNull(mergeableperformer, "mergeableperformer").performer();
    }

    private static class SimpleMergeablePerformer<T> extends MergeablePerformer<T> {
        private final CompositeScenario<T> scenario;

        private SimpleMergeablePerformer(CompositeScenario<T> scenario) {
            super(scenario.merger());
            this.scenario = scenario;
        }

        @Override
        protected String description() {
            return "scenario " + scenario;
        }
    }
}
