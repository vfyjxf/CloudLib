package dev.vfyjxf.cloudlib.api.performer;

import dev.vfyjxf.cloudlib.util.Checks;
import org.eclipse.collections.impl.list.mutable.FastList;
import org.jspecify.annotations.Nullable;

import java.lang.ref.WeakReference;
import java.util.Iterator;
import java.util.Objects;
import java.util.SequencedCollection;
import java.util.function.Function;

public abstract class MergeablePerformer<T> implements MutablePerformer<T> {

    protected final Function<SequencedCollection<T>, T> merger;
    protected final FastList<PerformerEntry<T>> performers = FastList.newList();
    protected @Nullable T performer;
    private int weakCount = 0;

    protected MergeablePerformer(Function<SequencedCollection<T>, T> merger) {
        this.merger = merger;
    }

    @Override
    public T performer() {
        if (cleaned() || performer == null) {
            updatePerformer();
        }
        @Nullable
        T current = performer;
        if (current == null) {
            throw new IllegalStateException("no available performer for " + description());
        }
        return current;
    }

    @Override
    public void put(T performer) {
        put(performer, PerformerPriorities.normal);
    }

    @Override
    public void remove(T performer) {
        Checks.checkNotNull(performer, "performer");
        performers.removeIf(entry -> Objects.equals(entry.performer(), performer));
        this.performer = null;
    }

    public void putWeak(Object key, T performer) {
        putWeak(key, performer, PerformerPriorities.normal);
    }

    public void put(T performer, int priority) {
        Checks.checkNotNull(performer, "performer");
        performers.add(new DirectPerformerEntry<>(performer, priority));
        performers.sort(PerformerEntry::compareTo);

        this.performer = null;
    }

    public void putWeak(Object key, T performer, int priority) {
        Checks.checkNotNull(key, "key");
        Checks.checkNotNull(performer, "performer");
        Checks.checkArgument(!Objects.equals(key, performer), "key and performer cannot be the same object!");
        performers.add(new WeakReferencePerformerEntry<>(key, performer, priority));
        weakCount++;

        this.performer = null;
    }

    protected void updatePerformer() {
        if (performers.size() == 1) {
            @Nullable
            T single = performers.getFirst().performer();
            performer = single;
        } else {
            @Nullable
            T merged = merger.apply(performers.collect(PerformerEntry::performer));
            if (merged == null && !performers.isEmpty()) {
                throw new IllegalStateException("merger returned null for " + description());
            }
            performer = merged;
        }
    }

    /**
     * @return a short description of this performer reference, used in error messages
     */
    protected String description() {
        return getClass().getSimpleName();
    }

    private boolean cleaned() {
        if (weakCount == 0) {
            return false;
        }
        boolean cleaned = false;
        for (Iterator<PerformerEntry<T>> iterator = performers.iterator(); iterator.hasNext();) {
            PerformerEntry<T> entry = iterator.next();
            if (entry.performer() == null) {
                iterator.remove();
                weakCount--;
                cleaned = true;
            }
        }
        return cleaned;
    }

    protected abstract static sealed class PerformerEntry<T> implements Comparable<PerformerEntry<T>>
            permits DirectPerformerEntry, WeakReferencePerformerEntry {
        private final int priority;

        protected PerformerEntry(int priority) {
            this.priority = priority;
        }

        @Override
        public int compareTo(MergeablePerformer.PerformerEntry<T> o) {
            return Integer.compare(o.priority, priority);
        }

        public abstract @Nullable T performer();
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
        public @Nullable T performer() {
            if (key.get() == null) {
                return null;
            }
            return performer;
        }
    }
}
