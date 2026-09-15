package dev.vfyjxf.cloudlib.api.lifecycle;

import java.util.List;

public interface LifecycleReceiver {

    default void context(LifecycleState<Void> state, String reason) {
        context(state, null, reason);
    }

    <T> void context(LifecycleState<T> state, T value, String reason);

    default void event(LifecycleState<Void> state, String reason) {
        event(state, null, reason);
    }

    <T> void event(LifecycleState<T> state, T value, String reason);

    void clear(LifecycleState<?> state, String reason);

    void resetEvents(String reason);

    boolean reload(String reason);

    boolean loadNow(String reason);

    boolean isLoaded();

    List<LifecycleState<?>> missing();
}
