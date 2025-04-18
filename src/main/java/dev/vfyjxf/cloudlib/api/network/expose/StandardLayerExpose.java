package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.data.snapshot.Snapshot;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
final class StandardLayerExpose<E> extends BasicLayerExpose<E> {
    <T> StandardLayerExpose(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> valueSupplier,
            FlowEncoder<T> encoder, FlowDecoder<E> decoder
    ) {
        super(name, id, snapshot, valueSupplier, encoder, decoder);
    }
}
