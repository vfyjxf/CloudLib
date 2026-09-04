package dev.vfyjxf.cloudlib.api.performer;

import org.jetbrains.annotations.NotNull;

/**
 * Backstage is a container for performers.
 * <p>
 * It represents the "performers" needed for a "play"
 * <p>
 * The "directors" 'call' the performers how to “perform” and when to "perform".
 * <p>
 * The "play" defines how many "performers" are needed and what they should do.
 */
public interface Backstage {

    PerformerContainer performers();

    default <T> void addPerformer(@NotNull Scenario<T> scenario, @NotNull Performer<T> performer) {
        performers().add(scenario, performer);
    }

    default boolean has(@NotNull Scenario<?> scenario) {
        return performers().has(scenario);
    }

    default <T> void remove(@NotNull Scenario<T> scenario) {
        performers().remove(scenario);
    }

    default <T> T getPerformer(@NotNull Scenario<T> scenario) {
        return performers().get(scenario);
    }

    default <T> void addPerformer(@NotNull CompositeScenario<T> scenario, @NotNull T performer) {
        performers().add(scenario, performer);
    }

    default <T> void addPerformer(@NotNull CompositeScenario<T> scenario, @NotNull T performer, int priority) {
        performers().add(scenario, performer, priority);
    }

    default <T> void addWeakPerformer(@NotNull CompositeScenario<T> scenario, @NotNull T performer, @NotNull Object reference) {
        performers().addWeak(scenario, performer, reference);
    }

    default <T> void addWeakPerformer(@NotNull CompositeScenario<T> scenario, @NotNull T performer, int priority, @NotNull Object reference) {
        performers().addWeak(scenario, performer, priority, reference);
    }

    default boolean has(@NotNull CompositeScenario<?> scenario) {
        return performers().has(scenario);
    }

    default <T> void remove(@NotNull CompositeScenario<T> scenario, @NotNull T performer) {
        performers().remove(scenario, performer);
    }

    default <T> void remove(@NotNull CompositeScenario<T> scenario) {
        performers().remove(scenario);
    }

    @NotNull
    default <T> T getPerformer(@NotNull CompositeScenario<T> scenario) {
        return performers().get(scenario);
    }

    //TODO:Add GatheringScenario.

}
