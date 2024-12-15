package dev.vfyjxf.cloudlib.api.event;

import org.jetbrains.annotations.Contract;

import java.util.function.BooleanSupplier;

/**
 * @param <T> the invoker type,it must be a functional interface.
 */
//TODO:priority support
public interface Event<T> {

    /**
     * @return the invoker
     */
    T invoker();

    /**
     * @param listener register the listener
     * @return the listener
     */
    T register(T listener);

    /**
     * Register a listener which managed by the lifetime.
     *
     * @param listener the listener to register
     * @param lifetime the lifetime of the listener in invoke calls times
     * @return the wrapped listener,which can count the invoke times
     */
    @Contract("_, _ -> new")
    T registerManaged(T listener, int lifetime);

    /**
     * Register a listener which lifetime managed by the condition.
     *
     * @param listener  the listener to register
     * @param condition the condition to check if the listener should be removed before next invoke
     * @return the listener
     */
    T registerManaged(T listener, BooleanSupplier condition);

    /**
     * Unregister a listener.
     *
     * @param listener the listener to unregister.
     */
    void unregister(T listener);

    boolean isRegistered(T listener);

    /**
     * Unregister all listener.
     */
    void clearListeners();

}
