package dev.vfyjxf.cloudlib.api.event;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;


/**
 * The EventContext interface is used to provide context information to event listeners.
 * It passes the poster of the event and provides the ability to cancel and interrupt the current event.
 * <p>
 * {@link Cancelable}:Affects the execution logic of the poster, with the exact cancellation depending on the semantics of the event.
 * <p>
 * {@link Interruptible}:Interrupts the current event, preventing further event listeners from being called.
 * <p>
 * {@link Common}:Provides both cancelable and interruptible functionality.But when canceling, it also interrupts the event.
 */
//TODO:Add EmptyContext to avoid create new context for empty event listener list
public sealed interface EventContext {

    @ApiStatus.Internal
    EventChannel<?> getChannel();

    /**
     * <b>Note: This method is unsafe and should be used with caution.</b>
     *
     * @param <T> the type of the poster
     * @param <E> the type of the event
     * @return the object that posted the event
     */
    @SuppressWarnings("unchecked")
    default <T extends EventHandler<E>, E> T poster() {
        return (T) getChannel().handler();
    }

    @Nullable
    default <T extends EventHandler<E>, E> T poster(Class<T> type) {
        EventHandler<?> handler = getChannel().handler();
        if (type.isInstance(handler)) {
            return type.cast(handler);
        }
        return null;
    }

    boolean cancelled();

    boolean interrupted();

    /**
     * Cancellable/Interruptible
     */
    final class Common implements EventContext {

        private final EventChannel<?> channel;
        private boolean cancelled = false;
        private boolean interrupted = false;

        public Common(EventChannel<?> channel) {
            this.channel = channel;
        }

        @Override
        public EventChannel<?> getChannel() {
            return channel;
        }

        @Override
        public boolean cancelled() {
            return cancelled;
        }

        @Override
        public boolean interrupted() {
            return interrupted;
        }

        public void cancel() {
            cancelled = true;
            interrupted = true;
        }

        public void interrupt() {
            interrupted = true;
        }

    }

    final class Cancelable implements EventContext {

        private final EventChannel<?> channel;
        private boolean cancelled = false;

        public Cancelable(EventChannel<?> channel) {
            this.channel = channel;
        }

        @Override
        public EventChannel<?> getChannel() {
            return channel;
        }

        @Override
        public boolean cancelled() {
            return cancelled;
        }

        @Override
        public boolean interrupted() {
            return false;
        }

        public void cancel() {
            cancelled = true;
        }

    }

    final class Interruptible implements EventContext {

        private final EventChannel<?> channel;
        private boolean interrupted = false;

        public Interruptible(EventChannel<?> channel) {
            this.channel = channel;
        }

        @Override
        public EventChannel<?> getChannel() {
            return channel;
        }

        @Override
        public boolean cancelled() {
            return false;
        }

        @Override
        public boolean interrupted() {
            return interrupted;
        }

        public void interrupt() {
            interrupted = true;
        }

    }
}
