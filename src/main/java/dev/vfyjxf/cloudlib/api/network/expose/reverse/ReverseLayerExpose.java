package dev.vfyjxf.cloudlib.api.network.expose.reverse;

import dev.vfyjxf.cloudlib.api.network.FlowDecoder;
import dev.vfyjxf.cloudlib.api.network.FlowEncoder;
import dev.vfyjxf.cloudlib.api.network.expose.common.LayerExpose;
import org.apache.commons.lang3.NotImplementedException;

import java.util.function.Function;

/**
 * @param <E> the exposed type
 * @param <S> the type of the value to send to the server
 * @param <R> the type of the value received at the server
 */
public interface ReverseLayerExpose<E, S, R> extends LayerExpose<E>, Reversed<S, R> {

    static <S_UP, S_EXPORT, S_HOLDER, C_SEND, S_RECEIVE> ReverseLayerExpose<S_EXPORT, C_SEND, S_RECEIVE> create(
            Function<S_HOLDER, S_UP> getter,
            FlowEncoder<S_UP> encoder,
            FlowDecoder<S_EXPORT> decoder,
            FlowEncoder<C_SEND> sendEncoder,
            FlowDecoder<S_RECEIVE> receiveDecoder
    ) {
        throw new NotImplementedException();
    }


}
