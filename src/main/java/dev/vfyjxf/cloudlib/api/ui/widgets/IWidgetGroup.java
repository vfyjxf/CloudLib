package dev.vfyjxf.cloudlib.api.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.drag.IDragConsumer;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.regex.Pattern;

public interface IWidgetGroup<T extends IWidget> extends IWidget, IDragConsumer<T> {

    List<T> children();

    IWidgetGroup<T> add(T widget);

    IWidgetGroup<T> add(int index, T widget);

    default IWidgetGroup<T> widget(T widget) {
        widget.asChild(this);
        return this;
    }

    @SuppressWarnings("unchecked")
    default IWidgetGroup<T> widgets(T... widgets) {
        for (T widget : widgets) {
            widget.asChild(this);
        }
        return this;
    }

    boolean remove(T widget);

    boolean remove(int index);

    void clear();

    boolean contains(T widget);

    @Nullable
    default IWidget getById(String id) {
        for (T child : children()) {
            if (child.getId().equals(id)) {
                return child;
            }
        }
        return null;
    }

    List<IWidget> getWidgetById(Pattern regex);

    @Nullable
    @Override
    default IWidget getHoveredWidget(double mouseX, double mouseY) {
        for (T child : children()) {
            if (child.visible() && child.isMouseOver(mouseX, mouseY)) {
                return child;
            }
        }
        return IWidget.super.getHoveredWidget(mouseX, mouseY);
    }

    default List<IWidget> getContainedWidgets(boolean withInvisible) {
        List<IWidget> result = new ArrayList<>();
        for (T child : children()) {
            if (child.visible() || withInvisible) {
                result.add(child);
                if (child instanceof IWidgetGroup<?> group)
                    result.addAll((group).getContainedWidgets(withInvisible));
            }
        }
        return result;
    }

    List<IWidget> getContainedWidgets(boolean withInvisible, int maxDepth);
}
