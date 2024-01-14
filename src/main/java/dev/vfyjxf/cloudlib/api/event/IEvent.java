package dev.vfyjxf.cloudlib.api.event;

import com.google.errorprone.annotations.CanIgnoreReturnValue;

/**
 * Architectury like event system.
 */
public interface IEvent<T> {

    T invoker();

    @CanIgnoreReturnValue
    T register(T listener);

    void unregister(T listener);

    boolean isRegistered(T listener);

    void clearListeners();

}
