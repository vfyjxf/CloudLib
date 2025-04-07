package dev.vfyjxf.cloudlib.api.network.expose.diff;

import dev.vfyjxf.cloudlib.api.event.EventFactory;
import dev.vfyjxf.cloudlib.api.event.SimpleEvent;
import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.network.expose.BasicLayerExpose;
import dev.vfyjxf.cloudlib.api.network.expose.ValueSupplier;
import dev.vfyjxf.cloudlib.api.snapshot.Snapshot;

public class DiffLayerExpose<E, D> extends BasicLayerExpose<E> implements Differential<D> {

    private final SimpleEvent<D> diffReceiveEvent = EventFactory.createSimpleEvent();

    protected <T> DiffLayerExpose(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> valueSupplier,
            FlowEncoder<T> encoder, FlowDecoder<E> decoder
    ) {
        super(name, id, snapshot, valueSupplier, encoder, decoder);
    }

    @Override
    public void whenDiffReceive(D difference) {
        diffReceiveEvent.register(difference);
    }
}
