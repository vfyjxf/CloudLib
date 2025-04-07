package dev.vfyjxf.cloudlib.api.event;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

import java.util.function.Consumer;

/**
 * A simple event which only has basic features of {@link Event}
 *
 * @param <T> the type of the listener
 */
public interface SimpleEvent<T> {

    void invoke(Consumer<T> invoker);

    /**
     * Register a listener.
     *
     * @param listener register the listener
     * @return the registered listener
     */
    @CanIgnoreReturnValue
    T register(T listener);

    /**
     * Unregister a listener.
     *
     * @param listener the listener to unregister.
     */
    void unregister(T listener);

    /**
     * @param listener the listener to check
     * @return if the listener is registered
     */
    boolean isRegistered(T listener);

    /**
     * Unregister all listener.
     */
    void clearListeners();

}
