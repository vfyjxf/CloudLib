package dev.vfyjxf.cloudlib.api.ui.drag;

import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.List;

public interface IDraggable {

    IWidget self();

    default void onDragStart(int button) {

    }

    default void onDragEnd(boolean successful) {

    }

    default void onDrag(int mouseButton, long timeSinceLastClick) {

    }

    default boolean accepts(int x, int y, @Nullable IWidget widget) {
        return true;
    }

    boolean isMoving();

    void setMoving(boolean moving);

    default List<IDragConsumer<? super IWidget>> receivers() {
        return Collections.emptyList();
    }

}
