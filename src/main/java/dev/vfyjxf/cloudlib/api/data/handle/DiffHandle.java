package dev.vfyjxf.cloudlib.api.data.handle;

import dev.vfyjxf.cloudlib.api.data.CheckStrategy;
import dev.vfyjxf.cloudlib.api.data.snapshot.DiffObservable;
import dev.vfyjxf.cloudlib.util.Checks;
import org.jetbrains.annotations.Contract;

/**
 * A {@link Handle} whose value is a {@link DiffObservable}, so incremental changes can be synced as
 * differences. {@code changed()} is the union of the handle's dirty flag and the value's own
 * {@link DiffObservable#changed()}: an in-place mutation surfaces even without {@link #set}.
 * <p>
 * Unlike a plain {@link Handle}, which may start out as an empty cell, a {@code DiffHandle} is
 * always created with a value, so {@link #get()} never returns null.
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

    /** A {@code DiffHandle} is never an empty cell, so the value read is non-null. */
    @Override
    @Contract(pure = true)
    T get();

    @Override
    default boolean changed() {
        return dirty() || get().changed();
    }

    @Override
    default D difference() {
        return get().difference();
    }
}
