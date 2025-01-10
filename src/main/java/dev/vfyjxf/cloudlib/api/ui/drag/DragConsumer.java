package dev.vfyjxf.cloudlib.api.ui.drag;

import dev.vfyjxf.cloudlib.api.actor.MergeableActorKey;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.utils.Locations;

import java.util.function.BiFunction;
import java.util.function.BiPredicate;

public interface DragConsumer {

    MergeableActorKey<DragConsumer> ACTOR_KEY = new MergeableActorKey<>(
            Locations.of("drag_consumer"),
            DragConsumer.class,
            listeners -> new DragConsumer() {

                @Override
                public void dragStart(DraggableElement<?> element, DragContext context) {
                    for (DragConsumer listener : listeners) {
                        listener.dragStart(element, context);
                    }
                }

                @Override
                public void onDrag(DraggableElement<?> element, DragContext context, double deltaX, double deltaY) {
                    for (DragConsumer listener : listeners) {
                        listener.onDrag(element, context, deltaX, deltaY);
                    }
                }

                @Override
                public void dragEnd(DraggableElement<?> element, DragContext context, double deltaX, double deltaY) {
                    for (DragConsumer listener : listeners) {
                        listener.dragEnd(element, context, deltaX, deltaY);
                    }
                }

                @Override
                public boolean consume(DraggableElement<?> widget, DragContext context) {
                    for (DragConsumer listener : listeners) {
                        if (listener.consume(widget, context)) {
                            return true;
                        }
                    }
                    return false;
                }
            }
    );

    static DragConsumer fromConsumer(BiFunction<DraggableElement<?>, DragContext, Boolean> consumer) {
        return new DragConsumer() {
            @Override
            public boolean consume(DraggableElement<?> element, DragContext context) {
                return consumer.apply(element, context);
            }
        };
    }

    @SuppressWarnings("unchecked")
    static DragConsumer forWidgetConsumer(
            BiPredicate<Widget, DragContext> predicate,
            BiFunction<DraggableElement<Widget>, DragContext, Boolean> consumer

    ) {
        return new DragConsumer() {
            @Override
            public boolean consume(DraggableElement<?> element, DragContext context) {
                return element.whenConsume(Widget.class, widget -> {
                    if (predicate.test(widget, context)) {
                        return consumer.apply((DraggableElement<Widget>) element, context);
                    } else return false;
                });
            }
        };
    }

    @SuppressWarnings("unchecked")
    static DragConsumer forGroupConsumer(
            BiPredicate<DraggableElement<?>, DragContext> predicate,
            BiFunction<DraggableElement<Widget>, DragContext, Boolean> consumer
    ) {
        return new DragConsumer() {
            @Override
            public boolean consume(DraggableElement<?> element, DragContext context) {
                if (predicate.test(element, context)) {
                    return element.whenConsume(Widget.class, widget -> {
                        return consumer.apply((DraggableElement<Widget>) element, context);
                    });
                } else return false;
            }
        };
    }

    default void dragStart(DraggableElement<?> element, DragContext context) {

    }

    default void onDrag(DraggableElement<?> element, DragContext context, double deltaX, double deltaY) {

    }

    default void dragEnd(DraggableElement<?> element, DragContext context, double deltaX, double deltaY) {

    }

    /**
     * @param widget the widget that is being dragged
     * @return true if the widget is consumed, false otherwise
     */
    default boolean consume(DraggableElement<?> widget, DragContext context) {
        return false;
    }

}
