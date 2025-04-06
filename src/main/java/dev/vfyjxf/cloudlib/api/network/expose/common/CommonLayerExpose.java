package dev.vfyjxf.cloudlib.api.network.expose.common;

import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.network.expose.BasicLayerExpose;
import dev.vfyjxf.cloudlib.api.network.expose.ValueSupplier;
import dev.vfyjxf.cloudlib.api.snapshot.Snapshot;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
class CommonLayerExpose<E> extends BasicLayerExpose<E> {
    protected <T> CommonLayerExpose(String name, short id, Snapshot<T> snapshot, ValueSupplier<T> valueSupplier, FlowEncoder<T> encoder, FlowDecoder<E> decoder) {
        super(name, id, snapshot, valueSupplier, encoder, decoder);
    }
}
