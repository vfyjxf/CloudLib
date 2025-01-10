package dev.vfyjxf.cloudlib.ui.drag;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.drag.DragContext;
import dev.vfyjxf.cloudlib.api.ui.drag.DraggableElement;
import dev.vfyjxf.cloudlib.utils.ScreenUtil;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

@ApiStatus.Internal
public class DragContextImpl implements DragContext {
    private final DraggableManager draggableManager;
    private final FloatPos start;
    private Object content;
    private DraggableElement<?> draggableElement;

    DragContextImpl(DraggableManager draggableManager) {
        this.draggableManager = draggableManager;
        this.start = ScreenUtil.getMousePos();
    }

    @Override
    public <T> @Nullable T getContent(Class<T> type) {
        return type.isInstance(content) ? type.cast(content) : null;
    }

    @Override
    public DragContext setContent(Object content) {
        this.content = content;
        return this;
    }

    @Override
    public void addAcceptableArea(Rect rect) {

    }

    @Override
    public FloatPos getStart() {
        return start;
    }

    @Override
    public FloatPos getCurrent() {
        return ScreenUtil.getMousePos();
    }

    @Override
    public @Nullable DraggableElement<?> draggingElement() {
        return draggableElement;
    }

    @Override
    public DragContext setDrag(DraggableElement<?> draggableElement) {
        this.draggableElement = draggableElement;
        return this;
    }


}
