package dev.vfyjxf.cloudlib.api.text;

import dev.vfyjxf.cloudlib.api.text.RichNode;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;

/**
 * An embedded fully-interactive widget. The widget is added as a child of the
 * enclosing rich text widget, positioned at the laid-out fragment rect with the
 * declared size, and participates in the normal event/focus/render pipeline.
 * <p>
 * The size is explicit (replaced-element semantics): the text flow reserves exactly
 * this box.
 */
public record WidgetNode(Widget widget, int width, int height) implements RichNode {

    public WidgetNode {
        if (widget == null) throw new NullPointerException("widget");
    }
}
