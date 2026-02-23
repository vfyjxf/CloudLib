package dev.vfyjxf.cloudlib.api.network.expose;

import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.network.FlowHandler;
import org.jetbrains.annotations.Contract;

import java.util.function.Consumer;

/**
 * The ability to send a value to the server.
 * <p>
 * We call this reversed because the value is sent to the server, not received from it.
 * Normally, the server sends a value to the client, but in this case, the client sends a value to the server.
 *
 * @param <S> the type of the value to send to the server
 * @param <R> the type of the value received at the server
 */
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
    @Contract("_->this")
    ReversedOnly<S, R> whenReceiveFromClient(Consumer<R> consumer);
}
