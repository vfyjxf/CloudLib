package dev.vfyjxf.cloudlib.api.lifecycle;

import org.jspecify.annotations.Nullable;

import java.util.List;

public interface LifecycleReceiver {

    /**
     * Reports a {@code Void} context.
     *
     * @param state  the context state, must be required by the receiving driver
     * @param reason a short description of why the context changed
     */
    default void context(LifecycleState<Void> state, String reason) {
        context(state, null, reason);
    }

    /**
     * Reports a context value.
     *
     * @param state  the context state, must be required by the receiving driver
     * @param value  the context value, or {@code null} to clear the context; ignored for {@code Void} states
     * @param reason a short description of why the context changed
     */
    <T> void context(LifecycleState<T> state, @Nullable T value, String reason);

    /**
     * Reports an event without a payload.
     *
     * @param state  the event state, must be required by the receiving driver
     * @param reason a short description of why the event happened
     */
    default void event(LifecycleState<Void> state, String reason) {
        event(state, null, reason);
    }

    /**
     * Reports an event.
     *
     * @param state  the event state, must be required by the receiving driver
     * @param value  the event payload, or {@code null} to skip the event; ignored for {@code Void} states
     * @param reason a short description of why the event happened
     */
    <T> void event(LifecycleState<T> state, @Nullable T value, String reason);

    void clear(LifecycleState<?> state, String reason);

    void resetEvents(String reason);

    boolean reload(String reason);

    boolean loadNow(String reason);

    boolean isLoaded();

    List<LifecycleState<?>> missing();
}
