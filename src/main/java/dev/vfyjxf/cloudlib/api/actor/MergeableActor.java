package dev.vfyjxf.cloudlib.api.actor;

import dev.vfyjxf.cloudlib.utils.Checks;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.List;
import java.util.Objects;
import java.util.function.Function;

public abstract class MergeableActor<T> implements MutableActor<T> {

    protected final Function<List<T>, T> merger;
    protected final FastList<ActorEntry<T>> actors = FastList.newList();
    protected T actor;
    private int weakCount = 0;

    protected MergeableActor(Function<List<T>, T> merger) {
        this.merger = merger;
    }

    @Override
    public T actor() {
        if (cleaned() || actor == null) {
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
        actors.removeIf(entry -> entry.actor().equals(actor));
        this.actor = null;
    }

    public void putWeak(Object key, T actor) {
        putWeak(key, actor, ActorPriority.DEFAULT);
    }

    public void put(T actor, int priority) {
        Checks.checkNotNull(actor, "actor");
        actors.add(new DirectActorEntry<>(actor, priority));
        actors.sort(ActorEntry::compareTo);

        this.actor = null;
    }

    public void putWeak(Object key, T actor, int priority) {
        Checks.checkNotNull(key, "key");
        Checks.checkNotNull(actor, "actor");
        Checks.checkArgument(!Objects.equals(key, actor), "key and actor cannot be the same object!");
        actors.add(new WeakReferenceActorEntry<>(key, actor, priority));
        weakCount++;

        this.actor = null;
    }

    protected void updateActor() {
        if (actors.size() == 1) {
            actor = actors.getFirst().actor();
        } else {
            actor = merger.apply(actors.collect(ActorEntry::actor));
        }
    }

    private boolean cleaned() {
        if (weakCount == 0) {
            return false;
        }
        boolean cleaned = false;
        for (Iterator<ActorEntry<T>> iterator = actors.iterator(); iterator.hasNext(); ) {
            ActorEntry<T> entry = iterator.next();
            if (entry.actor() == null) {
                iterator.remove();
                weakCount--;
                cleaned = true;
            }
        }
        return cleaned;
    }

    protected static abstract sealed class ActorEntry<T> implements Comparable<ActorEntry<T>> permits DirectActorEntry, WeakReferenceActorEntry {
        private final int priority;

        protected ActorEntry(int priority) {
            this.priority = priority;
        }

        @Override
        public int compareTo(@NotNull MergeableActor.ActorEntry<T> o) {
            return Integer.compare(o.priority, priority);
        }

        public abstract T actor();
    }

    protected static final class DirectActorEntry<T> extends ActorEntry<T> {
        private final T actor;

        public DirectActorEntry(T actor, int priority) {
            super(priority);
            this.actor = actor;
        }


        @Override
        public T actor() {
            return actor;
        }
    }

    protected static final class WeakReferenceActorEntry<T> extends ActorEntry<T> {
        private final WeakReference<Object> key;
        private final T actor;

        public WeakReferenceActorEntry(Object key, T actor, int priority) {
            super(priority);
            this.key = new WeakReference<>(key);
            this.actor = actor;
        }

        @Override
        public T actor() {
            if (key.get() == null) {
                return null;
            }
            return actor;
        }
    }

//    protected record ActorEntry<T>(T actor, int priority) implements Comparable<ActorEntry<T>> {
//        @SuppressWarnings("ConstantConditions")
//        public ActorEntry {
//        }
//
//        @Override
//        public int compareTo(@NotNull MergeableActor.ActorEntry<T> o) {
//            return Integer.compare(o.priority, priority);
//        }
//    }
}
