package dev.vfyjxf.cloudlib.api.ui;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetGroup;
import dev.vfyjxf.cloudlib.api.ui.event.InputEvent;
import dev.vfyjxf.cloudlib.api.ui.style.UIStyle;
import dev.vfyjxf.taffy.style.AlignItems;
import dev.vfyjxf.taffy.style.JustifyContent;

import static dev.vfyjxf.cloudlib.api.ui.style.UIStyles.*;

public final class Widgets {

    public static <T extends Widget> WidgetGroup<T> column(
            JustifyContent horizontalArrangement,
            AlignItems verticalArrangement
    ) {
        WidgetGroup<T> widget = new WidgetGroup<>();
        UIStyle style = UIStyle.of(
                flexColumn(),
                justifyContent(horizontalArrangement),
                alignItems(verticalArrangement)

        );
        widget.useStyle(style);
        return widget;
    }

    public static <T extends Widget> WidgetGroup<T> row(
            JustifyContent horizontalArrangement,
            AlignItems verticalArrangement
    ) {
        WidgetGroup<T> widget = new WidgetGroup<>();
        UIStyle style = UIStyle.of(
                flexRow(),
                justifyContent(horizontalArrangement),
                alignItems(verticalArrangement)
        );
        widget.useStyle(style);
        return widget;
    }

    public static <T extends Widget> WidgetGroup<T> grid() {
        WidgetGroup<T> widget = new WidgetGroup<>();
        UIStyle style = UIStyle.of(
                displayGrid()
        );
        widget.useStyle(style);
        throw new UnsupportedOperationException("Not Implemented");
    }

    public static <T extends Widget> WidgetGroup<T> group(
            UIStyle style
    ) {
        WidgetGroup<T> widget = new WidgetGroup<>();
        widget.useStyle(style);
        return widget;

    }

    public static Widget button(
            UIStyle style,
            InputEvent.OnMouseClick onClick
    ) {
        Widget widget = new Widget();
        widget.useStyle(style);
        widget.onMouseClick(onClick);
        return widget;
    }


    private Widgets() {
        throw new AssertionError("This class should not be instantiated!");
    }
}
