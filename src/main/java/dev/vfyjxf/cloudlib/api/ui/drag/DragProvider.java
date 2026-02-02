package dev.vfyjxf.cloudlib.api.ui.drag;

import dev.vfyjxf.cloudlib.Constants;
import dev.vfyjxf.cloudlib.api.performer.CompositeScenario;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.util.Namespace;
import org.jetbrains.annotations.UnknownNullability;

public interface DragProvider {

    CompositeScenario<DragProvider> scenario = new CompositeScenario<>(
        Namespace.of(Constants.MOD_ID, "drag_provider"),
        DragProvider.class,
        listeners -> new DragProvider() {
            @Override
            public boolean draggable(@UnknownNullability Scene scene, InputContext input, DragContext dragContext) {
                for (DragProvider listener : listeners) {
                    if (listener.draggable(scene, input, dragContext)) {
                        return true;
                    }
                }
                return false;
            }

            @Override
            public DraggableElement<?> getDraggableElement(@UnknownNullability Scene scene, InputContext input, DragContext dragContext) {
                for (DragProvider listener : listeners) {
                    DraggableElement<?> element = listener.getDraggableElement(scene, input, dragContext);
                    if (!element.isEmpty()) return element;
                }
                return DraggableElement.empty();
            }
        }
    );

    static DragProvider fromWidget(Widget widget) {
        return new DragProvider() {
            @Override
            public boolean draggable(@UnknownNullability Scene scene, InputContext input, DragContext dragContext) {
                return widget.parent() != null && widget.isMouseOver(input);
            }

            @Override
            public DraggableElement<?> getDraggableElement(@UnknownNullability Scene scene, InputContext input, DragContext dragContext) {
                if (widget.parent() != null && widget.isMouseOver(input)) return DraggableElement.draggable(widget);
                else return DraggableElement.empty();
            }
        };
    }

    boolean draggable(@UnknownNullability Scene scene, InputContext input, DragContext dragContext);

    DraggableElement<?> getDraggableElement(@UnknownNullability Scene scene, InputContext input, DragContext dragContext);


}
