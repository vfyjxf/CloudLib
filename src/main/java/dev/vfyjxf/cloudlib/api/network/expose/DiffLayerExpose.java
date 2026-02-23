package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.data.snapshot.DiffObservable;
import dev.vfyjxf.cloudlib.api.data.snapshot.Snapshot;
import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.network.FlowHandler;
import dev.vfyjxf.cloudlib.api.network.UnaryFlowHandler;

import java.util.function.Consumer;

public interface DiffLayerExpose<E, D> extends LayerExpose<E>, Differential<D> {


    static <T extends DiffObservable<D>, E, D> DiffLayerExpose<E, D> create(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> valueSupplier,
            FlowEncoder<T> encoder, FlowDecoder<E> decoder,
            FlowEncoder<D> diffEncoder, FlowDecoder<D> diffDecoder
    ) {
        return new StandardDiffLayerExpose<>(
                name, id,
                snapshot, valueSupplier,
                encoder, decoder,
                diffEncoder, diffDecoder
        );
    }

    static <T extends DiffObservable<D>, E, D> DiffLayerExpose<E, D> create(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> valueSupplier,
            FlowHandler<T, E> codec,
            FlowEncoder<D> diffEncoder, FlowDecoder<D> diffDecoder
    ) {
        return new StandardDiffLayerExpose<>(
                name, id,
                snapshot, valueSupplier,
                codec, codec,
                diffEncoder, diffDecoder
        );
    }

    static <T extends DiffObservable<D>, E, D> DiffLayerExpose<E, D> create(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> valueSupplier,
            FlowEncoder<T> encoder, FlowDecoder<E> decoder,
            UnaryFlowHandler<D> diffCodec
    ) {
        return new StandardDiffLayerExpose<>(
                name, id,
                snapshot, valueSupplier,
                encoder, decoder,
                diffCodec, diffCodec
        );
    }

    static <T extends DiffObservable<D>, E, D> DiffLayerExpose<E, D> create(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> valueSupplier,
            FlowHandler<T, E> codec,
            UnaryFlowHandler<D> diffCodec
    ) {
        return new StandardDiffLayerExpose<>(
                name, id,
                snapshot, valueSupplier,
                codec, codec,
                diffCodec, diffCodec
        );
    }


    @Override
    void whenDiffReceive(Consumer<D> diffConsumer);

    @Override
    void whenReceive(Consumer<E> consumer);


}
