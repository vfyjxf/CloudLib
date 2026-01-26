package dev.vfyjxf.cloudlib.api.ui.drag;

import dev.vfyjxf.cloudlib.api.performer.CompositeScenario;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.base.Scene;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.util.Locations;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.UnknownNullability;

public interface DragProvider {

    CompositeScenario<DragProvider> scenario = new CompositeScenario<>(
            Locations.ofMod("drag_provider"),
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
                public @Nullable DraggableElement<?> getDraggableElement(@UnknownNullability Scene scene, InputContext input, DragContext dragContext) {
                    for (DragProvider listener : listeners) {
                        DraggableElement<?> element = listener.getDraggableElement(scene, input, dragContext);
                        if (element != null) return element;
                    }
                    return null;
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
            public @Nullable DraggableElement<?> getDraggableElement(@UnknownNullability Scene scene, InputContext input, DragContext dragContext) {
                if (widget.parent() != null && widget.isMouseOver(input)) return DraggableElement.draggable(widget);
                else return null;
            }
        };
    }

    boolean draggable(@UnknownNullability Scene scene, InputContext input, DragContext dragContext);

    @Nullable
    DraggableElement<?> getDraggableElement(@UnknownNullability Scene scene, InputContext input, DragContext dragContext);


}
