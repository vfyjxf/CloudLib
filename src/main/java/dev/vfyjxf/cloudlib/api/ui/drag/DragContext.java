package dev.vfyjxf.cloudlib.api.ui.drag;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import org.jetbrains.annotations.Nullable;

public interface DragContext {

    @Nullable
    <T> T getContent(Class<T> type);

    DragContext setContent(Object content);

    void addAcceptableArea(Rect rect);

    default void addAcceptableArea(int x, int y, int width, int height) {
        addAcceptableArea(new Rect(x, y, width, height));
    }

    default void addAcceptableArea(Widget consumer) {
        addAcceptableArea(new Rect(consumer.getAbsolute(), consumer.getSize()));
    }

    /**
     * @return the start position of mouse when dragging starts.
     */
    FloatPos getStart();

    /**
     * @return the current absolute position of the mouse.
     */
    FloatPos getCurrent();

    default Pos relativePos(Widget coordinate) {
        DraggableElement<?> element = draggingElement();
        if (element == null) return Pos.ZERO;
        Rect original = element.originalBounds();
        FloatPos start = getStart();
        FloatPos current = getCurrent();
        int absX = (int) (current.x() - start.x() + original.x);
        int absY = (int) (current.y() - start.y() + original.y);
        return new Pos(absX - coordinate.getAbsolute().x, absY - coordinate.getAbsolute().y);
    }

    default Rect draggingBounds() {
        DraggableElement<?> element = draggingElement();
        if (element == null) return new Rect(0, 0, 0, 0);
        FloatPos start = getStart();
        FloatPos current = getCurrent();
        Rect original = element.originalBounds();
        int x = (int) (current.x() - start.x() + original.x);
        int y = (int) (current.y() - start.y() + original.y);
        return new Rect(x, y, original.width, original.height);
    }


    @Nullable
    DraggableElement<?> draggingElement();

    /**
     * @param draggableElement the widget that is being dragged,it must support render without position and its position is mutable
     * @return this
     */
    DragContext setDrag(DraggableElement<?> draggableElement);

}
