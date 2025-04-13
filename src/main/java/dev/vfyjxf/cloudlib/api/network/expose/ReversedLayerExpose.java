package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.network.FlowHandler;
import dev.vfyjxf.cloudlib.api.snapshot.Snapshot;

import java.util.function.Consumer;

/**
 * @param <E> the exposed type
 * @param <S> the type of the value to send to the server
 * @param <R> the type of the value received at the server
 */
public interface ReversedLayerExpose<E, S, R> extends LayerExpose<E>, Reversed<S, R> {


    static <T, E, S, R> ReversedLayerExpose<E, S, R> create(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> valueSupplier,
            FlowEncoder<T> encoder, FlowDecoder<E> decoder,
            FlowEncoder<S> reverseEncoder, FlowDecoder<R> reverseDecoder
    ) {
        return new StandardReversedLayerExpose<>(
                name, id,
                snapshot, valueSupplier,
                encoder, decoder,
                reverseEncoder, reverseDecoder
        );
    }

    static <T, E, S, R> ReversedLayerExpose<E, S, R> create(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> valueSupplier,
            FlowHandler<T, E> codec,
            FlowEncoder<S> reverseEncoder, FlowDecoder<R> reverseDecoder
    ) {
        return new StandardReversedLayerExpose<>(
                name, id,
                snapshot, valueSupplier,
                codec, codec,
                reverseEncoder, reverseDecoder
        );
    }

    static <T, E, S, R> ReversedLayerExpose<E, S, R> create(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> valueSupplier,
            FlowEncoder<T> encoder, FlowDecoder<E> decoder,
            FlowHandler<S, R> reverseCodec
    ) {
        return new StandardReversedLayerExpose<>(
                name, id,
                snapshot, valueSupplier,
                encoder, decoder,
                reverseCodec, reverseCodec
        );
    }

    static <T, E, S, R> ReversedLayerExpose<E, S, R> create(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> valueSupplier,
            FlowHandler<T, E> codec,
            FlowHandler<S, R> reverseCodec
    ) {
        return new StandardReversedLayerExpose<>(
                name, id,
                snapshot, valueSupplier,
                codec, codec,
                reverseCodec, reverseCodec
        );
    }

    @Override
    ReversedLayerExpose<E, S, R> whenReceiveFromClient(Consumer<R> consumer);

    @Override
    void sendToServer(S toSend);
}
