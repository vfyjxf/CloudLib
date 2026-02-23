package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.data.snapshot.DiffObservable;
import dev.vfyjxf.cloudlib.api.data.snapshot.Snapshot;
import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.network.FlowHandler;
import dev.vfyjxf.cloudlib.api.network.UnaryFlowHandler;

import java.util.function.Consumer;

public interface DiffReverseLayerExpose<E, D, S, R> extends LayerExpose<E>, Differential<D>, Reversed<S, R> {

    static <T extends DiffObservable<D>, E, D, S, R> DiffReverseLayerExpose<E, D, S, R> create(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> valueSupplier,
            FlowEncoder<T> encoder, FlowDecoder<E> decoder,
            FlowEncoder<D> diffEncoder, FlowDecoder<D> diffDecoder,
            FlowEncoder<S> reverseEncoder, FlowDecoder<R> reverseDecoder
    ) {
        return new StandardDiffReverseLayerExpose<>(
                name, id,
                snapshot, valueSupplier,
                encoder, decoder,
                diffEncoder, diffDecoder,
                reverseEncoder, reverseDecoder
        );
    }

    static <T extends DiffObservable<D>, E, D, S, R> DiffReverseLayerExpose<E, D, S, R> create(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> valueSupplier,
            FlowHandler<T, E> codec,
            UnaryFlowHandler<D> diffCodec,
            FlowHandler<S, R> reverseCodec
    ) {
        return new StandardDiffReverseLayerExpose<>(
                name, id,
                snapshot, valueSupplier,
                codec, codec,
                diffCodec, diffCodec,
                reverseCodec, reverseCodec
        );
    }

    @Override
    void whenDiffReceive(Consumer<D> diffConsumer);

    @Override
    void sendToServer(S toSend);

}
