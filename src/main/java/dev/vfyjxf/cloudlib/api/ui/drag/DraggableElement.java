package dev.vfyjxf.cloudlib.api.ui.drag;

import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.Renderable;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.util.Checks;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Predicate;

public interface DraggableElement<T> extends Renderable {

    @SuppressWarnings("unchecked")
    static <T> DraggableElement<T> empty() {
        return (DraggableElement<T>) EmptyDraggableElement.instance;
    }

    boolean isEmpty();

    default boolean notEmpty() {
        return !isEmpty();
    }

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

class EmptyDraggableElement<T> implements DraggableElement<T> {

    static final EmptyDraggableElement<?> instance = new EmptyDraggableElement<>();

    @Override
    public boolean isEmpty() {
        return true;
    }

    @Override
    public T value() {
        throw new UnsupportedOperationException("Empty draggable element does not support this operation.");
    }

    @Override
    public Rect originalBounds() {
        throw new UnsupportedOperationException("Empty draggable element does not support this operation.");
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        throw new UnsupportedOperationException("Empty draggable element does not support this operation.");
    }
}

class SimpleDraggableElement implements DraggableElement<Widget> {

    protected final Widget widget;
    private final Rect originalBounds;

    SimpleDraggableElement(Widget widget) {
        this.widget = Checks.checkNotNull(widget, "widget");
        this.originalBounds = widget.absoluteBounds().copy();
    }

    @Override
    public boolean isEmpty() {
        return false;
    }

    @Override
    public void dragStart(InputContext input, DragContext context) {
        widget.setDragging(true);
    }

    @Override
    public void dragEnd(InputContext input, DragContext context, double deltaX, double deltaY, boolean consumed) {
        if (!consumed) {
            throw new UnsupportedOperationException("Not Implemented");
            //            widget.translate((int) deltaX, (int) deltaY);
        }
        widget.setDragging(false);
    }

    @Override
    public Widget value() {
        return widget;
    }

    @Override
    public Rect originalBounds() {
        return originalBounds;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        widget.render(graphics, mouseX, mouseY, partialTicks);
    }

}
