package dev.vfyjxf.cloudlib.api.performer;

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

    default <T> void addPerformer(Scenario<T> scenario, Performer<T> performer) {
        performers().add(scenario, performer);
    }

    default boolean has(Scenario<?> scenario) {
        return performers().has(scenario);
    }

    default <T> void remove(Scenario<T> scenario) {
        performers().remove(scenario);
    }

    default <T> T getPerformer(Scenario<T> scenario) {
        return performers().get(scenario);
    }

    default <T> void addPerformer(CompositeScenario<T> scenario, T performer) {
        performers().add(scenario, performer);
    }

    default <T> void addPerformer(CompositeScenario<T> scenario, T performer, int priority) {
        performers().add(scenario, performer, priority);
    }

    default <T> void addWeakPerformer(CompositeScenario<T> scenario, T performer, Object reference) {
        performers().addWeak(scenario, performer, reference);
    }

    default <T> void addWeakPerformer(CompositeScenario<T> scenario, T performer, int priority, Object reference) {
        performers().addWeak(scenario, performer, priority, reference);
    }

    default boolean has(CompositeScenario<?> scenario) {
        return performers().has(scenario);
    }

    default <T> void remove(CompositeScenario<T> scenario, T performer) {
        performers().remove(scenario, performer);
    }

    default <T> void remove(CompositeScenario<T> scenario) {
        performers().remove(scenario);
    }

    default <T> T getPerformer(CompositeScenario<T> scenario) {
        return performers().get(scenario);
    }

    // TODO:Add GatheringScenario.

}
