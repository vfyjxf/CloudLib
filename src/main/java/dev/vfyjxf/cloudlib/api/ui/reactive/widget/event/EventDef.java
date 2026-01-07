package dev.vfyjxf.cloudlib.api.ui.reactive.widget.event;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Function;

/**
 * Definition of an event type for reactive widgets.
 * <p>
 * Similar to the main EventDefinition but specialized for reactive UI with
 * capture-bubble propagation support.
 *
 * @param <T> the listener type (must be a functional interface)
 */
public final class EventDef<T> {

    /**
     * Event propagation type.
     */
    public enum PropagationType {
        /** Capture → Target → Bubble */
        BUBBLING,
        /** Target only, no propagation */
        DIRECT,
        /** Broadcast to all descendants */
        BROADCAST
    }

    private final Class<T> type;
    private final Function<List<T>, T> merger;
    private final PropagationType propagationType;

    private EventDef(Class<T> type, Function<List<T>, T> merger, PropagationType propagationType) {
        this.type = type;
        this.merger = merger;
        this.propagationType = propagationType;
    }

    /**
     * Create a bubbling event definition (capture → target → bubble).
     */
    public static <T> EventDef<T> bubbling(Class<T> type, Function<List<T>, T> merger) {
        return new EventDef<>(type, merger, PropagationType.BUBBLING);
    }

    /**
     * Create a direct event definition (target only, no propagation).
     */
    public static <T> EventDef<T> direct(Class<T> type, Function<List<T>, T> merger) {
        return new EventDef<>(type, merger, PropagationType.DIRECT);
    }

    /**
     * Create a broadcast event definition (broadcast to all descendants).
     */
    public static <T> EventDef<T> broadcast(Class<T> type, Function<List<T>, T> merger) {
        return new EventDef<>(type, merger, PropagationType.BROADCAST);
    }

    /**
     * Get the listener type.
     */
    public Class<T> type() {
        return type;
    }

    /**
     * Get the propagation type.
     */
    public PropagationType propagationType() {
        return propagationType;
    }

    /**
     * Check if this event bubbles.
     */
    public boolean bubbles() {
        return propagationType == PropagationType.BUBBLING;
    }

    /**
     * Check if this event broadcasts.
     */
    public boolean broadcasts() {
        return propagationType == PropagationType.BROADCAST;
    }

    /**
     * Create a new event instance.
     */
    public REvent<T> create() {
        return new REvent<>(this, merger);
    }

    /**
     * Event instance that holds listeners.
     */
    public static final class REvent<T> {
        private final EventDef<T> definition;
        private final Function<List<T>, T> merger;
        private final List<ListenerEntry<T>> listeners = new ArrayList<>();
        private T invokerCache = null;
        private boolean dirty = true;

        private record ListenerEntry<T>(T listener, boolean useCapture) {}

        REvent(EventDef<T> definition, Function<List<T>, T> merger) {
            this.definition = definition;
            this.merger = merger;
        }

        /**
         * Register a listener for the bubble/target phase.
         */
        public T register(T listener) {
            return register(listener, false);
        }

        /**
         * Register a listener.
         *
         * @param listener   the listener
         * @param useCapture true for capture phase, false for bubble phase
         */
        public T register(T listener, boolean useCapture) {
            listeners.add(new ListenerEntry<>(listener, useCapture));
            dirty = true;
            return listener;
        }

        /**
         * Unregister a listener.
         */
        public void unregister(T listener) {
            listeners.removeIf(entry -> entry.listener() == listener);
            dirty = true;
        }

        /**
         * Check if a listener is registered.
         */
        public boolean isRegistered(T listener) {
            return listeners.stream().anyMatch(entry -> entry.listener() == listener);
        }

        /**
         * Clear all listeners.
         */
        public void clear() {
            listeners.clear();
            dirty = true;
        }

        /**
         * Check if there are any listeners.
         */
        public boolean hasListeners() {
            return !listeners.isEmpty();
        }

        /**
         * Get the event definition.
         */
        public EventDef<T> definition() {
            return definition;
        }

        /**
         * Get listeners for the capture phase.
         */
        public List<T> captureListeners() {
            return listeners.stream()
                    .filter(ListenerEntry::useCapture)
                    .map(ListenerEntry::listener)
                    .toList();
        }

        /**
         * Get listeners for the bubble phase.
         */
        public List<T> bubbleListeners() {
            return listeners.stream()
                    .filter(entry -> !entry.useCapture())
                    .map(ListenerEntry::listener)
                    .toList();
        }

        /**
         * Get all listeners.
         */
        public List<T> allListeners() {
            return listeners.stream()
                    .map(ListenerEntry::listener)
                    .toList();
        }

        /**
         * Get the merged invoker for all listeners.
         */
        public T invoker() {
            if (dirty || invokerCache == null) {
                invokerCache = merger.apply(allListeners());
                dirty = false;
            }
            return invokerCache;
        }

        /**
         * Get the merged invoker for capture phase listeners.
         */
        public T captureInvoker() {
            return merger.apply(captureListeners());
        }

        /**
         * Get the merged invoker for bubble phase listeners.
         */
        public T bubbleInvoker() {
            return merger.apply(bubbleListeners());
        }
    }
}
