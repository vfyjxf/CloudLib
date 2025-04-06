package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.event.SingleEventChannel;
import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.network.expose.common.LayerExpose;
import dev.vfyjxf.cloudlib.api.snapshot.Snapshot;

import java.util.function.Consumer;

public abstract class BasicLayerExpose<E> implements LayerExpose<E> {

    private final String name;
    private final short id;
    private final LayerSnapshot<?> snapshot;
    private final FlowDecoder<E> decoder;
    private final SingleEventChannel<Consumer<E>> receiveListeners = new SingleEventChannel<>();

    protected <T> BasicLayerExpose(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> valueSupplier,
            FlowEncoder<T> encoder, FlowDecoder<E> decoder
    ) {

        this.name = name;
        this.id = id;
        this.snapshot = new LayerSnapshot<>(snapshot, valueSupplier, encoder);
        this.decoder = decoder;
    }

    @Override
    public String name() {
        return "";
    }

    @Override
    public short id() {
        return 0;
    }

    @Override
    public void whenReceive(Consumer<E> consumer) {
        receiveListeners.register(consumer);
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> LayerSnapshot<T> layerSnapshot() {
        return (LayerSnapshot<T>) this.snapshot;
    }
}
