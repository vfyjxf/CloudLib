package dev.vfyjxf.cloudlib.api.event;

import dev.vfyjxf.cloudlib.api.ui.event.IUIEventDefinition;
import org.eclipse.collections.impl.list.mutable.FastList;

import java.util.List;
import java.util.function.Function;

/**
 * Based Architectury event system.
 * <p>
 * Some basic promises:
 * <p>
 * 1. The naming of channel follows the lowercase hump naming
 * <p>
 * 2. Pre-phase channel do not need to add the Pre suffix, but Post-phase channel need to add the Post suffix.
 * <p>
 * 3. Usually, an event is defined in an interface with its listeners.
 */
public final class EventFactory {


    private EventFactory() {
    }

    public static <T> IEventDefinition<T> define(Class<T> type, Function<List<T>, T> invokerFactory) {
        return new EventDefinition<>(type, invokerFactory);
    }

    public static <T> IEvent<T> createEvent(Function<List<T>, T> invokerFactory) {
        return new Event<>(invokerFactory);
    }

    private static class EventDefinition<T> implements IEventDefinition<T> {

        private final Class<T> type;
        private final Function<List<T>, T> function;
        private final IEvent<T> global;

        private EventDefinition(Class<T> type, Function<List<T>, T> function) {
            this.type = type;
            this.function = function;
            this.global = new Event<>(function);
        }

        public Function<List<T>, T> function() {
            return function;
        }

        @Override
        public Class<T> type() {
            return type;
        }

        @Override
        public IEvent<T> create() {
            return new Event<>(function);
        }

        @Override
        public IEvent<T> global() {
            return global;
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
