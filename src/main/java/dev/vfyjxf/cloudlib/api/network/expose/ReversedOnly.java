package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.network.FlowHandler;

import java.util.function.Consumer;

public sealed interface ReversedOnly<S, R>
        extends Reversed<S, R>
        permits StandardReversed, UnaryReversed {

    static <S, R> ReversedOnly<S, R> create(
            String name,
            short id,
            FlowEncoder<S> reversedEncoder,
            FlowDecoder<R> reversedDecoder
    ) {
        return new StandardReversed<>(
                name,
                id,
                reversedEncoder,
                reversedDecoder
        );
    }

    static <S, R> ReversedOnly<S, R> create(
            String name,
            short id,
            FlowHandler<S, R> reverseCodec
    ) {
        return new StandardReversed<>(
                name,
                id,
                reverseCodec,
                reverseCodec
        );
    }

    @Override
    void sendToServer(S toSend);

    @Override
    ReversedOnly<S, R> whenReceiveFromClient(Consumer<R> consumer);
}
