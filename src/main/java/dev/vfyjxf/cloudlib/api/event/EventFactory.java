package dev.vfyjxf.cloudlib.api.event;

import org.eclipse.collections.impl.list.mutable.FastList;

import java.util.List;
import java.util.function.Function;

/**
 * Based Architectury event system.
 * <p>
 * Some basic promises:
 * <p>
 * 1. The naming of events follows the lowercase hump naming
 * <p>
 * 2. Pre-phase events do not need to add the Pre suffix, but Post-phase events need to add the Post suffix.
 * <p>
 * 3. Usually, an event is defined in an interface with its listener.
 */
public final class EventFactory {


    private EventFactory() {
    }

    public static <T> IEventDefinition<T> define(Class<T> type, Function<List<T>, T> invokerFactory) {
        return new EventDefinition<>(type, invokerFactory);
    }

    private record EventDefinition<T>(Class<T> type, Function<List<T>, T> function) implements IEventDefinition<T> {

        @Override
        public IEvent<T> create() {
            return new Event<>(function);
        }
    }

    private static class Event<T> implements IEvent<T> {
        private final Function<List<T>, T> function;
        private T invoker = null;
        private final FastList<T> listeners;

        private Event(Function<List<T>, T> function) {
            this.function = function;
            listeners = FastList.newList();
        }

        @Override
        public T invoker() {
            if (invoker == null) {
                update();
            }
            return invoker;
        }

        @Override
        public T register(T listener) {
            listeners.add(listener);
            invoker = null;
            return listener;
        }

        @Override
        public void unregister(T listener) {
            listeners.remove(listener);
            listeners.trimToSize();
            invoker = null;
        }

        @Override
        public boolean isRegistered(T listener) {
            return listeners.contains(listener);
        }

        @Override
        public void clearListeners() {
            listeners.clear();
            invoker = null;
        }

        public void update() {
            if (listeners.size() == 1) {
                invoker = listeners.get(0);
            } else {
                invoker = function.apply(listeners);
            }
        }
    }
}
