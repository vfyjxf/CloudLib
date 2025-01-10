package dev.vfyjxf.cloudlib.api.actor;

import dev.vfyjxf.cloudlib.utils.Checks;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.jetbrains.annotations.NotNull;

import java.util.List;
import java.util.function.Function;

public abstract class MergeableActor<T> implements MutableActor<T> {

    protected final Function<List<T>, T> merger;
    protected final FastList<ActorEntry<T>> actors = FastList.newList();
    protected T actor;

    protected MergeableActor(Function<List<T>, T> merger) {
        this.merger = merger;
    }

    @Override
    public T actor() {
        if (actor == null) {
            updateActor();
        }
        return actor;
    }

    @Override
    public void put(T actor) {
        put(actor, ActorPriority.DEFAULT);
    }

    @Override
    public void remove(@NotNull T actor) {
        Checks.checkNotNull(actor, "actor");
        actors.removeIf(entry -> entry.actor.equals(actor));
        this.actor = null;
    }

    public void put(T actor, int priority) {
        Checks.checkNotNull(actor, "actor");
        actors.add(new ActorEntry<>(actor, priority));
        actors.sort(ActorEntry::compareTo);

        this.actor = null;
    }

    protected void updateActor() {
        if (actors.size() == 1) {
            actor = actors.getFirst().actor;
        } else {
            actor = merger.apply(actors.collect(ActorEntry::actor));
        }
    }

    protected record ActorEntry<T>(T actor, int priority) implements Comparable<ActorEntry<T>> {
        @SuppressWarnings("ConstantConditions")
        public ActorEntry {
        }

        @Override
        public int compareTo(@NotNull MergeableActor.ActorEntry<T> o) {
            return Integer.compare(o.priority, priority);
        }
    }
}
