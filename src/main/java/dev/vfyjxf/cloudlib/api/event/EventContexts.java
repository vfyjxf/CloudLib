package dev.vfyjxf.cloudlib.api.event;

import dev.vfyjxf.cloudlib.api.event.context.BubbleContext;
import dev.vfyjxf.cloudlib.api.event.context.CancelableContext;
import dev.vfyjxf.cloudlib.api.event.context.CommonContext;
import dev.vfyjxf.cloudlib.api.event.context.IntentContext;
import dev.vfyjxf.cloudlib.api.event.context.InterruptibleContext;

/**
 * The EventContext interface is used to provide context information to event listeners.
 * It passes the poster of the event and provides the ability to cancel and interrupt the current event.
 * <p>
 * {@link CancelableContext}:Affects the execution logic of the poster, with the exact cancellation depending on the semantics of the event.
 * <p>
 * {@link InterruptibleContext}:Interrupts the current event, preventing further event listeners from being called.
 * <p>
 * {@link CommonContext}:Provides both cancelable and interruptible functionality.But when canceling, it also interrupts the event.
 * <p>
 * {@link BubbleContext}:Event context supporting hierarchical propagation, enabling events to flow through Capturing, Target, and Bubbling phases.
 */
public final class EventContexts {

    public static CommonContext createCommon(EventChannel<?> channel) {
        return new CommonContext(channel);
    }

    public static CancelableContext createCancelable(EventChannel<?> channel) {
        return new CancelableContext(channel);
    }

    public static InterruptibleContext createInterruptible(EventChannel<?> channel) {
        return new InterruptibleContext(channel);
    }

    public static BubbleContext createBubble(EventChannel<?> source) {
        return new BubbleContext(source);
    }

    public static IntentContext createIntent(EventChannel<?> source) {
        return new IntentContext(source);
    }

    private EventContexts() {throw new UnsupportedOperationException();}

    static CommonContext emptyCommon = new CommonContext(null);
    static CancelableContext emptyCancelable = new CancelableContext(null);
    static InterruptibleContext emptyInterruptible = new InterruptibleContext(null);
    static BubbleContext emptyBubble = new BubbleContext(null);
    static IntentContext emptyIntent = new IntentContext(null);

}
