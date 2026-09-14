package dev.vfyjxf.cloudlib.api.text.layout;

import dev.vfyjxf.cloudlib.api.text.ClickAction;
import dev.vfyjxf.cloudlib.api.text.HoverAction;
import dev.vfyjxf.cloudlib.api.text.RichNode;
import dev.vfyjxf.cloudlib.api.text.RichTextStyle;
import org.jetbrains.annotations.Nullable;

/**
 * One positioned, styled piece of a laid-out line.
 * <p>
 * Fragments keep a reference to their source node and the effective interaction
 * actions, which is what enables per-fragment hit testing (click / hover / tooltip)
 * inside a rich text widget.
 *
 * @param kind    what this fragment paints as
 * @param x       left edge, relative to the laid-out text origin
 * @param y       top edge, relative to the laid-out text origin
 * @param width   reserved width in pixels
 * @param height  reserved height in pixels
 * @param text    the characters for {@link Kind#TEXT} fragments, otherwise null
 * @param style   the effective style (vanilla style + CloudLib extras merged)
 * @param source  the innermost content {@link RichNode} this fragment came from
 * @param onClick effective click action (explicit or adapted from the vanilla style)
 * @param onHover effective hover action (explicit or adapted from the vanilla style)
 */
public record TextFragment(
        Kind kind,
        float x, float y, float width, float height,
        @Nullable String text,
        RichTextStyle style,
        RichNode source,
        @Nullable ClickAction onClick,
        @Nullable HoverAction onHover
) {

    public enum Kind {
        TEXT,
        OBJECT,
        SPACE
    }

    public boolean contains(float px, float py) {
        return px >= x && px < x + width && py >= y && py < y + height;
    }

    public boolean interactive() {
        return onClick != null || onHover != null;
    }
}
