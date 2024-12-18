package dev.vfyjxf.cloudlib.api.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.alignment.Alignment;
import dev.vfyjxf.cloudlib.data.lang.LangEntry;
import dev.vfyjxf.cloudlib.ui.widgets.TextWidget;
import net.minecraft.network.chat.Component;

/**
 * Standard widgets
 */
public final class Widgets {

    public static TextWidget text(Component component, Alignment.Horizontal horizontal, Alignment.Vertical vertical) {
        return TextWidget.create(component, horizontal, vertical);
    }

    public static TextWidget text(Component component) {
        return TextWidget.create(component);
    }

    public static TextWidget text(String text) {
        return TextWidget.of(text);
    }

    public static TextWidget text(LangEntry entry) {
        return TextWidget.of(entry);
    }

    private Widgets() {
    }

}
