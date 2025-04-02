package dev.vfyjxf.cloudlib.api.ui.drag;

import dev.vfyjxf.cloudlib.api.performer.CompositeScenario;
import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.UIContext;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.utils.Locations;
import org.jetbrains.annotations.Nullable;

public interface DragProvider {

    CompositeScenario<DragProvider> SCENARIO = new CompositeScenario<>(
            Locations.of("drag_provider"),
            DragProvider.class,
            listeners -> new DragProvider() {
                @Override
                public boolean draggable(UIContext uiContext, InputContext input, DragContext dragContext) {
                    for (DragProvider listener : listeners) {
                        if (listener.draggable(uiContext, input, dragContext)) {
                            return true;
                        }
                    }
                    return false;
                }

                @Override
                public @Nullable DraggableElement<?> getDraggableElement(UIContext uiContext, InputContext input, DragContext dragContext) {
                    for (DragProvider listener : listeners) {
                        DraggableElement<?> element = listener.getDraggableElement(uiContext, input, dragContext);
                        if (element != null) return element;
                    }
                    return null;
                }
            }
    );

    static DragProvider fromWidget(Widget widget) {
        return new DragProvider() {
            @Override
            public boolean draggable(UIContext uiContext, InputContext input, DragContext dragContext) {
                return widget.parent() != null && widget.isMouseOver(input);
            }

            @Override
            public @Nullable DraggableElement<?> getDraggableElement(UIContext uiContext, InputContext input, DragContext dragContext) {
                if (widget.parent() != null && widget.isMouseOver(input)) return DraggableElement.draggable(widget);
                else return null;
            }
        };
    }

    boolean draggable(UIContext uiContext, InputContext input, DragContext dragContext);

    @Nullable
    DraggableElement<?> getDraggableElement(UIContext uiContext, InputContext input, DragContext dragContext);


}
