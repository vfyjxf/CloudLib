package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.data.snapshot.Snapshot;
import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.network.FlowHandler;
import dev.vfyjxf.cloudlib.api.network.UnaryFlowHandler;
import org.jetbrains.annotations.Contract;

import java.util.function.Consumer;

/**
 * @param <T> the exposed type
 * @param <S> the type of the value to send to the server
 * @param <R> the type of the value received at the server
 */
public interface ReversedExpose<T, S, R> extends Expose<T>, Reversed<S, R> {

    static <T, S, R> ReversedExpose<T, S, R> create(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> supplier,
            FlowEncoder<T> encoder, FlowDecoder<T> decoder,
            FlowEncoder<S> reverseEncoder, FlowDecoder<R> reverseDecoder
    ) {
        return new StandardReversedExpose<>(
                name, id,
                snapshot, supplier,
                encoder, decoder,
                reverseEncoder, reverseDecoder
        );
    }

    static <T, S, R> ReversedExpose<T, S, R> create(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> supplier,
            UnaryFlowHandler<T> codec,
            FlowEncoder<S> reverseEncoder, FlowDecoder<R> reverseDecoder
    ) {
        return new StandardReversedExpose<>(
                name, id,
                snapshot, supplier,
                codec, codec,
                reverseEncoder, reverseDecoder
        );
    }

    static <T, S, R> ReversedExpose<T, S, R> create(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> supplier,
            FlowEncoder<T> encoder, FlowDecoder<T> decoder,
            FlowHandler<S, R> reverseCodec
    ) {
        return new StandardReversedExpose<>(
                name, id,
                snapshot, supplier,
                encoder, decoder,
                reverseCodec, reverseCodec
        );
    }

    static <T, S, R> ReversedExpose<T, S, R> create(
            String name, short id,
            Snapshot<T> snapshot, ValueSupplier<T> supplier,
            UnaryFlowHandler<T> codec,
            FlowHandler<S, R> reverseCodec
    ) {
        return new StandardReversedExpose<>(
                name, id,
                snapshot, supplier,
                codec, codec,
                reverseCodec, reverseCodec
        );
    }

    @Override
    void sendToServer(S toSend);

    @Override
    @Contract("_->this")
    ReversedExpose<T, S, R> whenReceiveFromClient(Consumer<R> consumer);
}
