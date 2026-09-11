package dev.vfyjxf.cloudlib.api.data.handle;

import dev.vfyjxf.cloudlib.api.data.CheckStrategy;
import dev.vfyjxf.cloudlib.api.data.snapshot.DiffObservable;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Default {@link DiffHandle} implementation. Composes a {@link BasicHandle} for value/dirty/listener
 * behavior; diff state lives on the underlying {@link DiffObservable} value.
 */
@ApiStatus.Internal
final class BasicDiffHandle<T extends DiffObservable<D>, D> implements DiffHandle<T, D> {

    private final BasicHandle<T> delegate;

    BasicDiffHandle(T initial, CheckStrategy<T> strategy) {
        this.delegate = new BasicHandle<>(initial, strategy);
    }

    @Override
    @Contract(pure = true)
    public T get() {
        return delegate.get();
    }

    @Override
    public void set(T value) {
        delegate.set(value);
    }

    @Override
    public void apply(T value) {
        delegate.apply(value);
    }

    @Override
    public void load(T value) {
        delegate.load(value);
    }

    @Override
    @Contract(pure = true)
    public boolean dirty() {
        return delegate.dirty();
    }

    @Override
    public void clearDirty() {
        delegate.clearDirty();
    }

    @Override
    @Contract(pure = true)
    public CheckStrategy<T> strategy() {
        return delegate.strategy();
    }

    @Override
    public Subscription onChange(Consumer<? super T> listener) {
        return delegate.onChange(listener);
    }

    @Override
    public Subscription onChange(BiConsumer<? super T, ? super T> listener) {
        return delegate.onChange(listener);
    }

    @Override
    public boolean changed() {
        //union of the handle's own dirty flag and the value's reported change; null-safe on the value half
        T current = delegate.get();
        return delegate.dirty() || (current != null && current.changed());
    }

    @Override
    public D difference() {
        return delegate.get().difference();
    }

    @Override
    public String toString() {
        return "DiffHandle{" + delegate.get() + (delegate.dirty() ? " (dirty)" : "") + '}';
    }
}
