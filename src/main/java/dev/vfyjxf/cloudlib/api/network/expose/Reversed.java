package dev.vfyjxf.cloudlib.api.network.expose;

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
interface Reversed<S, R> {

    /**
     * Write the value to the send list,all the values in the send list will be sent to the server in the end of current ui tick.
     *
     * @param toSend the value to send
     */
    void sendToServer(S toSend);

    /**
     * Register a listener to be called when a value is received from the client.
     *
     * @param consumer the listener to register
     */
    Reversed<S, R> whenReceiveFromClient(Consumer<R> consumer);

    boolean hasReversedData();

}
