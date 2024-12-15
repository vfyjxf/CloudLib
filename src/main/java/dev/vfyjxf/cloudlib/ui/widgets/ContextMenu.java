package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.layout.ColumnResizer;
import dev.vfyjxf.cloudlib.api.ui.layout.modifier.Modifier;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup;

public class ContextMenu<T extends Widget> extends WidgetGroup<T> {

    public ContextMenu() {
        this(10);
    }

    public ContextMenu(int height) {
        this.withModifier(
                Modifier.builder()
                        .layoutWith(ColumnResizer::new, l -> l.vertical().height(height))
        );
    }

}
