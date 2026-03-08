package dev.vfyjxf.cloudlib.api.ui.test;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.ui.widget.*;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * Extracts readable information from widgets for finders and assertions.
 */
final class WidgetInspector {

    private WidgetInspector() {}

    /**
     * Extracts the primary display text from a widget, or null if not a text-bearing widget.
     */
    static @Nullable String textOf(Widget widget) {
        if (widget instanceof ButtonWidget btn) {
            return componentToString(btn.label());
        }
        if (widget instanceof LabelWidget lbl) {
            return componentToString(lbl.text());
        }
        if (widget instanceof TextWidget txt) {
            return componentToString(txt.text());
        }
        if (widget instanceof TextFieldWidget field) {
            return field.text();
        }
        return null;
    }

    static @Nullable String componentToString(@Nullable Component component) {
        return component == null ? null : component.getString();
    }
}
