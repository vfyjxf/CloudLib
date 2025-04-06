package dev.vfyjxf.cloudlib.api.event;

import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.util.function.Consumer;

/**
 * A simple event channel that allows single event registration and invocation.
 *
 * @param <T> the type of the listener
 */
public class SingleEventChannel<T> {

    private final MutableList<T> listeners = Lists.mutable.empty();

    public void register(T listener) {
        listeners.add(listener);
    }

    public T unregister(T listener) {
        if (listeners.remove(listener)) {
            return listener;
        } else {
            throw new IllegalStateException("Listener not found");
        }
    }

    public void clearAllListeners() {
        listeners.clear();
    }

    public boolean isRegistered(T listener) {
        return listeners.contains(listener);
    }

    public void invoke(Consumer<T> consumer) {
        for (T listener : listeners) {
            consumer.accept(listener);
        }
    }

}
