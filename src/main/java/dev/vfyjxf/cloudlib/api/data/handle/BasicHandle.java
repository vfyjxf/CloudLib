package dev.vfyjxf.cloudlib.api.data.handle;

import dev.vfyjxf.cloudlib.api.data.CheckStrategy;
import dev.vfyjxf.cloudlib.api.event.SimpleEvent;
import dev.vfyjxf.cloudlib.util.Checks;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Contract;
import org.jetbrains.annotations.Nullable;

import java.util.function.BiConsumer;
import java.util.function.Consumer;

/**
 * Default {@link Handle} implementation.
 */
@ApiStatus.Internal
final class BasicHandle<T> implements Handle<T> {

    private final CheckStrategy<T> strategy;
    private final SimpleEvent<Consumer<T>> changeEvent = SimpleEvent.create();
    private final SimpleEvent<BiConsumer<T, T>> pairChangeEvent = SimpleEvent.create();

    private T value;
    private boolean dirty;

    BasicHandle(@Nullable T initial, CheckStrategy<T> strategy) {
        this.strategy = strategy;
        this.value = initial;
    }

    @Override
    @Contract(pure = true)
    public T get() {
        return value;
    }

    @Override
    public void set(T value) {
        if (strategy.matches(this.value, value)) return;
        T previous = this.value;
        this.value = value;
        dirty = true;
        fire(previous, value);
    }

    @Override
    public void apply(T value) {
        if (strategy.matches(this.value, value)) return;
        T previous = this.value;
        this.value = value;
        fire(previous, value);
    }

    @Override
    public void load(T value) {
        //silent: store only, no listeners, dirty flag untouched
        this.value = value;
    }

    private void fire(T previous, T value) {
        changeEvent.invoke(listener -> listener.accept(value));
        pairChangeEvent.invoke(listener -> listener.accept(previous, value));
    }

    @Override
    @Contract(pure = true)
    public boolean dirty() {
        return dirty;
    }

    @Override
    public void clearDirty() {
        dirty = false;
    }

    @Override
    @Contract(pure = true)
    public CheckStrategy<T> strategy() {
        return strategy;
    }

    @Override
    public Subscription onChange(Consumer<? super T> listener) {
        Checks.checkNotNull(listener, "listener");
        Consumer<T> adapted = listener::accept;
        changeEvent.register(adapted);
        return () -> changeEvent.unregister(adapted);
    }

    @Override
    public Subscription onChange(BiConsumer<? super T, ? super T> listener) {
        Checks.checkNotNull(listener, "listener");
        BiConsumer<T, T> adapted = listener::accept;
        pairChangeEvent.register(adapted);
        return () -> pairChangeEvent.unregister(adapted);
    }

    @Override
    public String toString() {
        return "Handle{" + value + (dirty ? " (dirty)" : "") + '}';
    }
}
