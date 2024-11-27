package dev.vfyjxf.cloudlib.api.ui.drag;

import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public interface Draggable {

    Widget self();

    default void onDragStart(int button) {

    }

    default void onDragEnd(boolean successful) {

    }

    default void onDrag(int mouseButton, long timeSinceLastClick) {

    }

    default boolean accepts(int x, int y, @Nullable Widget widget) {
        return true;
    }

    boolean isMoving();

    void setMoving(boolean moving);

    default List<DragConsumer<? super Widget>> receivers() {
        return Collections.emptyList();
    }

}
