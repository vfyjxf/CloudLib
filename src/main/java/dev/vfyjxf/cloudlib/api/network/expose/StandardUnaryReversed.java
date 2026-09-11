package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;

import java.util.function.Consumer;

final class StandardUnaryReversed<T> extends StandardReversed<T, T> implements UnaryReversed<T> {

    StandardUnaryReversed(
            String name,
            short id,
            FlowEncoder<T> reversedEncoder,
            FlowDecoder<T> reversedDecoder
    ) {
        super(name, id, reversedEncoder, reversedDecoder);
    }

    @Override
    public StandardUnaryReversed<T> whenReceiveFromClient(Consumer<T> consumer) {
        super.whenReceiveFromClient(consumer);
        return this;
    }
}
