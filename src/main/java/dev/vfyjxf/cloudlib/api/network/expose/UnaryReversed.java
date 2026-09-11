package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.network.FlowHandler;

public sealed interface UnaryReversed<T>
        extends ReversedOnly<T, T>
        permits StandardUnaryReversed {

    static <T> UnaryReversed<T> create(
            String name,
            short id,
            FlowEncoder<T> reversedEncoder,
            FlowDecoder<T> reversedDecoder
    ) {
        return new StandardUnaryReversed<>(
                name,
                id,
                reversedEncoder,
                reversedDecoder
        );
    }

    static <T> UnaryReversed<T> create(
            String name,
            short id,
            FlowHandler<T, T> reverseCodec
    ) {
        return create(
                name,
                id,
                reverseCodec,
                reverseCodec
        );
    }

}
