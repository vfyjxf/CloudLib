package dev.vfyjxf.cloudlib.api.text;

import org.jspecify.annotations.Nullable;

/**
 * Decorates a child node with a {@link RichTextStyle} and optional interaction
 * actions. The style merges over the inherited style (this node's fields win);
 * actions override inherited ones when non-null.
 *
 * @param child   the decorated node
 * @param style   the node's style, never {@code null} ({@code null} is normalized to
 *                {@link RichTextStyle#empty})
 * @param onClick the click action; {@code null} = inherit
 * @param onHover the hover action; {@code null} = inherit
 */
public record StyledNode(
    RichNode child,
    RichTextStyle style,
    @Nullable ClickAction onClick,
    @Nullable HoverAction onHover
) implements RichNode {

    public StyledNode {
        if (child == null) throw new NullPointerException("child");
        if (style == null) style = RichTextStyle.empty;
    }

    public StyledNode(RichNode child, RichTextStyle style) {
        this(child, style, null, null);
    }

    public StyledNode withClick(@Nullable ClickAction onClick) {
        return new StyledNode(child, style, onClick, onHover);
    }

    public StyledNode withHover(@Nullable HoverAction onHover) {
        return new StyledNode(child, style, onClick, onHover);
    }

    /**
     * Merges additional style over this node's current style.
     */
    public StyledNode withStyle(RichTextStyle extra) {
        return new StyledNode(child, style.merge(extra), onClick, onHover);
    }
}
