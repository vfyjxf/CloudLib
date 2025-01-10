package dev.vfyjxf.cloudlib.api.actor;

import org.jetbrains.annotations.NotNull;

public interface ActorHolder {

    ActorContainer actors();

    default <T> void addActor(@NotNull ActorKey<T> key, @NotNull Actor<T> actor) {
        actors().add(key, actor);
    }

    default boolean has(@NotNull ActorKey<?> key) {
        return actors().has(key);
    }

    default <T> void remove(@NotNull ActorKey<T> key) {
        actors().remove(key);
    }

    default <T> T getActor(@NotNull ActorKey<T> key) {
        return actors().get(key);
    }

    default <T> void addActor(@NotNull MergeableActorKey<T> key, @NotNull T actor) {
        actors().add(key, actor);
    }

    default <T> void addActor(@NotNull MergeableActorKey<T> key, @NotNull T actor, int priority) {
        actors().add(key, actor, priority);
    }

    default boolean has(@NotNull MergeableActorKey<?> key) {
        return actors().has(key);
    }

    default <T> void remove(@NotNull MergeableActorKey<T> key, @NotNull T actor) {
        actors().remove(key, actor);
    }

    default <T> void remove(@NotNull MergeableActorKey<T> key) {
        actors().remove(key);
    }

    @NotNull
    default <T> T getActor(@NotNull MergeableActorKey<T> key) {
        return actors().get(key);
    }

}
