package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.event.SimpleEvent;
import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.snapshot.Snapshot;
import net.minecraft.network.RegistryFriendlyByteBuf;

import java.util.function.Consumer;

sealed abstract class BasicLayerExpose<E>
        implements LayerExpose<E>, Transcoder
        permits StandardLayerExpose,
                StandardReversedLayerExpose,
                StandardDiffLayerExpose,
                StandardDiffReverseLayerExpose {

    private final SimpleEvent<Consumer<E>> receiveEvent = SimpleEvent.create();
    private final String name;
    private final short id;
    private final LayerSnapshot<?> snapshot;
    private final FlowDecoder<E> decoder;

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
        return name;
    }

    @Override
    public short id() {
        return id;
    }

    @SuppressWarnings("unchecked")
    @Override
    public <T> LayerSnapshot<T> layerSnapshot() {
        return (LayerSnapshot<T>) this.snapshot;
    }

    @Override
    public void whenReceive(Consumer<E> consumer) {
        receiveEvent.register(consumer);
    }

    @Override
    public void writeToClient(RegistryFriendlyByteBuf byteBuf) {
        layerSnapshot().encoder().encode(byteBuf, layerSnapshot().current());
    }

    @Override
    public void readFromServer(RegistryFriendlyByteBuf byteBuf) {
        E received = decoder.decode(byteBuf);
        receiveEvent.invoke(c -> c.accept(received));
    }
}
