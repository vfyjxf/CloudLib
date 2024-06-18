package dev.vfyjxf.cloudlib.api.ui.event;

import dev.vfyjxf.cloudlib.api.annotations.NotNullByDefault;
import dev.vfyjxf.cloudlib.api.event.EventFactory;
import dev.vfyjxf.cloudlib.api.event.IEvent;

import java.util.List;
import java.util.function.Function;

@NotNullByDefault
public final class UIEventFactory {

    private UIEventFactory() {
    }

    public static <T> IUIEventDefinition<T> define(Class<T> type, Function<List<T>, T> listener) {
        return new UIEventDefinition<>(type, listener);
    }

    private static class UIEventDefinition<T> implements IUIEventDefinition<T> {

        private final Class<T> type;
        private final Function<List<T>, T> function;
        private final IEvent<T> global;

        private UIEventDefinition(Class<T> type, Function<List<T>, T> function) {
            this.type = type;
            this.function = function;
            this.global = EventFactory.createEvent(function);
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
            return EventFactory.createEvent(function);
        }

        @Override
        public IEvent<T> global() {
            return global;
        }

    }

}
