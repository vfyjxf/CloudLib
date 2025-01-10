package dev.vfyjxf.cloudlib.api.actor;

import dev.vfyjxf.cloudlib.utils.Checks;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.jetbrains.annotations.NotNull;

public class ActorContainer {

    private final MutableMap<ActorKey<?>, @NotNull Actor<?>> actors = Maps.mutable.empty();
    private final MutableMap<MergeableActorKey<?>, @NotNull MergeableActor<?>> mergeableActors = Maps.mutable.empty();

    @SuppressWarnings({"unchecked", "rawtypes"})
    public <T> void add(@NotNull ActorKey<T> key, @NotNull Actor<T> actor) {
        var existing = actors.get(key);
        if (existing == null) {
            actors.put(key, actor);
        } else if (existing instanceof MutableActor mutableActor) {
            mutableActor.put(actor);
        } else {
            throw new IllegalArgumentException("Actor already exists for key: " + key);
        }
    }

    public boolean has(@NotNull ActorKey<?> key) {
        return actors.get(key) != null;
    }

    public <T> void remove(@NotNull ActorKey<T> key) {
        actors.remove(key);
    }

    @SuppressWarnings({"unchecked"})
    public <T> T get(@NotNull ActorKey<T> key) {
        return (T) actors.get(key).actor();
    }

    @SuppressWarnings({"unchecked"})
    public <T> void add(@NotNull MergeableActorKey<T> key, @NotNull T actor) {
        MergeableActor<T> mergeableActor = (MergeableActor<T>) mergeableActors.computeIfAbsent(key, k -> new SimpleMergeableActor<>(key));
        mergeableActor.put(actor);
    }

    @SuppressWarnings({"unchecked"})
    public <T> void add(@NotNull MergeableActorKey<T> key, @NotNull T actor, int priority) {
        MergeableActor<T> mergeableActor = (MergeableActor<T>) mergeableActors.computeIfAbsent(key, k -> new SimpleMergeableActor<>(key));
        mergeableActor.put(actor, priority);
    }

    public <T> void addWeak(@NotNull MergeableActorKey<T> key, @NotNull T actor, @NotNull Object reference) {
        addWeak(key, actor, ActorPriority.DEFAULT, reference);
    }

    @SuppressWarnings({"unchecked"})
    public <T> void addWeak(@NotNull MergeableActorKey<T> key, @NotNull T actor, int priority, @NotNull Object reference) {
        Checks.checkNotNull(key, "key");
        Checks.checkNotNull(actor, "actor");
        Checks.checkNotNull(reference, "reference");
        MergeableActor<T> mergeableActor = (MergeableActor<T>) mergeableActors.computeIfAbsent(key, k -> new SimpleMergeableActor<>(key));
        mergeableActor.putWeak(reference, actor, priority);
    }

    public boolean has(@NotNull MergeableActorKey<?> key) {
        return mergeableActors.get(key) != null;
    }

    @SuppressWarnings({"unchecked"})
    public <T> void remove(@NotNull MergeableActorKey<T> key, @NotNull T actor) {
        MergeableActor<T> mergeableActor = (MergeableActor<T>) mergeableActors.get(key);
        if (mergeableActor != null) {
            mergeableActor.remove(actor);
        }
    }

    public <T> void remove(@NotNull MergeableActorKey<T> key) {
        mergeableActors.remove(key);
    }

    @NotNull
    @SuppressWarnings({"unchecked"})
    public <T> T get(@NotNull MergeableActorKey<T> key) {
        return (T) mergeableActors.getIfAbsentPut(key, new SimpleMergeableActor<>(key)).actor();
    }

    private static class SimpleMergeableActor<T> extends MergeableActor<T> {
        private SimpleMergeableActor(MergeableActorKey<T> key) {
            super(key.merger());
        }
    }
}
