package dev.vfyjxf.cloudlib.api.network.sync;

import org.apache.commons.lang3.NotImplementedException;

import java.util.function.Function;

public interface ReverseExposeLayerValue<EXPORT, SEND, RECEIVE> extends ExposeLayerValue<EXPORT> {

    static <S_UP, S_EXPORT, S_HOLDER, C_SEND, S_RECEIVE> ReverseExposeLayerValue<S_EXPORT, C_SEND, S_RECEIVE> create(
            Function<S_HOLDER, S_UP> getter,
            FlowEncoder<S_UP> encoder,
            FlowDecoder<S_EXPORT> decoder,
            FlowEncoder<C_SEND> sendEncoder,
            FlowDecoder<S_RECEIVE> receiveDecoder
    ) {
        throw new NotImplementedException();
    }


    void send(SEND element);

}
