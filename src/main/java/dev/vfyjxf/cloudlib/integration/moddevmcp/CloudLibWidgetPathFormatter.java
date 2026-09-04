package dev.vfyjxf.cloudlib.integration.moddevmcp;

import dev.vfyjxf.cloudlib.api.ui.base.CompositeWidget;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.base.WidgetPath;

public final class CloudLibWidgetPathFormatter {
    private static final char SEPARATOR = '/';
    private static final char INDEX_SEPARATOR = '#';

    private CloudLibWidgetPathFormatter() {
    }

    public static String format(WidgetPath path) {
        if (path == null || path.isEmpty()) {
            return "";
        }
        StringBuilder builder = new StringBuilder();
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                builder.append(SEPARATOR);
            }
            builder.append(formatSegment(path.get(i), i));
        }
        return builder.toString();
    }

    private static String formatSegment(Widget widget, int fallbackIndex) {
        String key = keyText(widget);
        if (key != null) {
            return key;
        }

        String typeName = typeNameOf(widget);
        return typeName + INDEX_SEPARATOR + siblingOrdinal(widget, typeName, fallbackIndex);
    }

    private static int siblingOrdinal(Widget widget, String typeName, int fallbackIndex) {
        if (widget == null) {
            return fallbackIndex;
        }
        Widget parent = widget.parent();
        if (parent == null) {
            return fallbackIndex;
        }
        if (!(parent instanceof CompositeWidget<?> composite)) {
            return fallbackIndex;
        }

        int ordinal = 0;
        for (Widget sibling : composite.children()) {
            if (sibling == widget) {
                return ordinal;
            }
            if (keyText(sibling) == null && typeName.equals(typeNameOf(sibling))) {
                ordinal++;
            }
        }
        return fallbackIndex;
    }

    private static String keyText(Widget widget) {
        if (widget == null || widget.key() == null) {
            return null;
        }
        String key = widget.key().toString();
        return key == null || key.isEmpty() ? null : key;
    }

    private static String typeNameOf(Widget widget) {
        String typeName = null;
        if (widget != null) {
            typeName = widget.inspectionTypeName();
            if (typeName == null || typeName.trim().isEmpty()) {
                typeName = widget.getClass().getSimpleName();
            }
        }
        if (typeName == null || typeName.trim().isEmpty()) {
            return "Widget";
        }
        return typeName;
    }
}
