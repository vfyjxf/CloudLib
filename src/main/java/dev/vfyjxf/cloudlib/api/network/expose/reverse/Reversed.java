package dev.vfyjxf.cloudlib.api.network.expose.reverse;

import org.jetbrains.annotations.ApiStatus;

/**
 * @param <S> the type of the value to send to the server
 * @param <R> the type of the value received at the server
 */
@ApiStatus.Internal
public interface Reversed<S, R> {

    /**
     * Write the value to the send list,all the values in the send list will be sent to the server in the end of current ui tick.
     *
     * @param toSend the value to send
     */
    void sendToServer(S toSend);
}
