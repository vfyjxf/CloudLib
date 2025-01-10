package dev.vfyjxf.cloudlib.api.ui.drag;

import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.Renderable;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Predicate;

public interface DraggableElement<T> extends Renderable {

    /**
     * @param widget a widget to be draggable,it will translate widget's position when drag released
     * @return a draggable element
     */
    static DraggableElement<Widget> draggable(Widget widget) {
        return new SimpleDraggableElement(widget);
    }

    static DraggableElement<Widget> forConsumer(Widget widget) {
        return new SimpleDraggableElement(widget);
    }

    default void dragStart(InputContext input, DragContext context) {

    }

    default void onDrag(double mouseX, double mouseY, DragContext context) {

    }

    default void dragEnd(InputContext input, DragContext context, double deltaX, double deltaY, boolean consumed) {

    }

    /**
     * @return a special value for consumer to check or consume this element
     */
    T value();

    default <V> boolean whenConsume(Class<V> type, Predicate<V> consumer) {
        return type.isInstance(value()) && consumer.test(type.cast(value()));
    }

    default boolean whenConsume(Predicate<Widget> consumer) {
        return whenConsume(Widget.class, consumer);
    }

    default <V> boolean test(Class<V> type, Predicate<V> predicate) {
        return type.isInstance(value()) && predicate.test(type.cast(value()));
    }

    default boolean test(Predicate<Widget> predicate) {
        return test(Widget.class, predicate);
    }

    Rect originalBounds();

    @Override
    void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks);

}
