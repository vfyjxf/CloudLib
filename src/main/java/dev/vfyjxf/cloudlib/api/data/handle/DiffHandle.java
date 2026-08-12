package dev.vfyjxf.cloudlib.api.data.handle;

import dev.vfyjxf.cloudlib.api.data.CheckStrategy;
import dev.vfyjxf.cloudlib.api.data.snapshot.DiffObservable;
import dev.vfyjxf.cloudlib.util.Checks;

/**
 * A {@link Handle} whose value is a {@link DiffObservable}, so incremental changes can be synced as
 * differences. {@code changed()} is the union of the handle's dirty flag and the value's own
 * {@link DiffObservable#changed()}: an in-place mutation surfaces even without {@link #set}.
 *
 * @param <T> the value type (a {@link DiffObservable})
 * @param <D> the difference type
 */
public interface DiffHandle<T extends DiffObservable<D>, D> extends Handle<T>, DiffObservable<D> {

    static <T extends DiffObservable<D>, D> DiffHandle<T, D> of(T initial, CheckStrategy<T> strategy) {
        Checks.checkNotNull(initial, "initial");
        Checks.checkNotNull(strategy, "strategy");
        return new BasicDiffHandle<>(initial, strategy);
    }

    static <T extends DiffObservable<D>, D> DiffHandle<T, D> of(T initial) {
        Checks.checkNotNull(initial, "initial");
        return new BasicDiffHandle<>(initial, CheckStrategy.observable());
    }

    @Override
    default boolean changed() {
        T current = get();
        return dirty() || (current != null && current.changed());
    }

    @Override
    default D difference() {
        return get().difference();
    }
}
