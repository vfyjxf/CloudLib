package dev.vfyjxf.cloudlib.api.performer;

import dev.vfyjxf.cloudlib.utils.Checks;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.jetbrains.annotations.NotNull;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.function.Function;

public abstract class MergeablePerformer<T> implements MutablePerformer<T> {

    protected final Function<SequencedCollection<T>, T> merger;
    protected final FastList<PerformerEntry<T>> performers = FastList.newList();
    protected T performer;
    private int weakCount = 0;

    protected MergeablePerformer(Function<SequencedCollection<T>, T> merger) {
        this.merger = merger;
    }

    @Override
    public T performer() {
        if (cleaned() || performer == null) {
            updatePerformer();
        }
        return performer;
    }

    @Override
    public void put(T actor) {
        put(actor, PerformerPriorities.DEFAULT);
    }

    @Override
    public void remove(@NotNull T actor) {
        Checks.checkNotNull(actor, "actor");
        performers.removeIf(entry -> entry.performer().equals(actor));
        this.performer = null;
    }

    public void putWeak(Object key, T actor) {
        putWeak(key, actor, PerformerPriorities.DEFAULT);
    }

    public void put(T actor, int priority) {
        Checks.checkNotNull(actor, "actor");
        performers.add(new DirectPerformerEntry<>(actor, priority));
        performers.sort(PerformerEntry::compareTo);

        this.performer = null;
    }

    public void putWeak(Object key, T actor, int priority) {
        Checks.checkNotNull(key, "key");
        Checks.checkNotNull(actor, "actor");
        Checks.checkArgument(!Objects.equals(key, actor), "key and actor cannot be the same object!");
        performers.add(new WeakReferencePerformerEntry<>(key, actor, priority));
        weakCount++;

        this.performer = null;
    }

    protected void updatePerformer() {
        if (performers.size() == 1) {
            performer = performers.getFirst().performer();
        } else {
            performer = merger.apply(performers.collect(PerformerEntry::performer));
        }
    }

    private boolean cleaned() {
        if (weakCount == 0) {
            return false;
        }
        boolean cleaned = false;
        for (Iterator<PerformerEntry<T>> iterator = performers.iterator(); iterator.hasNext(); ) {
            PerformerEntry<T> entry = iterator.next();
            if (entry.performer() == null) {
                iterator.remove();
                weakCount--;
                cleaned = true;
            }
        }
        return cleaned;
    }

    protected static abstract sealed class PerformerEntry<T> implements Comparable<PerformerEntry<T>> permits DirectPerformerEntry, WeakReferencePerformerEntry {
        private final int priority;

        protected PerformerEntry(int priority) {
            this.priority = priority;
        }

        @Override
        public int compareTo(@NotNull MergeablePerformer.PerformerEntry<T> o) {
            return Integer.compare(o.priority, priority);
        }

        public abstract T performer();
    }

    protected static final class DirectPerformerEntry<T> extends PerformerEntry<T> {
        private final T performer;

        public DirectPerformerEntry(T performer, int priority) {
            super(priority);
            this.performer = performer;
        }


        @Override
        public T performer() {
            return performer;
        }
    }

    protected static final class WeakReferencePerformerEntry<T> extends PerformerEntry<T> {
        private final WeakReference<Object> key;
        private final T performer;

        public WeakReferencePerformerEntry(Object key, T performer, int priority) {
            super(priority);
            this.key = new WeakReference<>(key);
            this.performer = performer;
        }

        @Override
        public T performer() {
            if (key.get() == null) {
                return null;
            }
            return performer;
        }
    }
}
