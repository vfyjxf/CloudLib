package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.data.snapshot.Snapshot;
import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
sealed abstract class BasicExpose<T> implements Expose<T>
        permits BasicDownstreamExpose,
                StandardReversed {

    private final String name;
    private final short id;
    private final Snapshot<T> snapshot;
    private final ValueSupplier<T> supplier;
    protected final FlowEncoder<T> encoder;
    protected final FlowDecoder<T> decoder;

    protected BasicExpose(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> supplier,
            FlowEncoder<T> encoder, FlowDecoder<T> decoder
    ) {
        this.name = name;
        this.id = id;
        this.snapshot = snapshot;
        this.supplier = supplier;
        this.encoder = encoder;
        this.decoder = decoder;
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
    public void updateSnapshot() {
        if (snapshot.mutable()) {
            snapshot.updateState(supplier.get());
        }
    }

    @Override
    public void forceUpdateSnapshot() {
        if (snapshot.mutable()) {
            snapshot.forceUpdateState(supplier.get());
        }
    }

    @Override
    public String toString() {
        return "BasicExpose{" +
                "name='" + name + '\'' +
                ", id=" + id +
                ", snapshot=" + snapshot +
                '}';
    }
}
