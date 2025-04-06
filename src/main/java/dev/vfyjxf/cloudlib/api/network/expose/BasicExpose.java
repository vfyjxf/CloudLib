package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.event.SingleEventChannel;
import dev.vfyjxf.cloudlib.api.network.expose.common.Expose;
import dev.vfyjxf.cloudlib.api.snapshot.Snapshot;
import org.jetbrains.annotations.ApiStatus;

import java.util.function.Consumer;

@ApiStatus.Internal
public abstract class BasicExpose<T> implements Expose<T> {

    private final SingleEventChannel<Consumer<T>> eventChannel = new SingleEventChannel<>();
    private final String name;
    private final short id;
    private final Snapshot<T> snapshot;
    private final ValueSupplier<T> supplier;

    protected BasicExpose(String name, short id, Snapshot<T> snapshot, ValueSupplier<T> supplier) {
        this.name = name;
        this.id = id;
        this.snapshot = snapshot;
        this.supplier = supplier;
    }

    @Override
    public short id() {
        return id;
    }

    @Override
    public String name() {
        return name;
    }

    @Override
    public Snapshot<T> snapshot() {
        return snapshot;
    }

    @Override
    public T current() {
        return supplier.get();
    }

    @Override
    public ValueSupplier<T> supplier() {
        return supplier;
    }

    @Override
    public void whenReceive(Consumer<T> consumer) {
        eventChannel.register(consumer);
    }

}
