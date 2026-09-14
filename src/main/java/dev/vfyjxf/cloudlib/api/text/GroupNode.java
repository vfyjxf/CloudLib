package dev.vfyjxf.cloudlib.api.text;

import dev.vfyjxf.cloudlib.api.text.RichNode;

import java.util.List;

/**
 * An ordered sequence of sibling nodes. The group's own styling is applied by
 * wrapping the group in a {@link StyledNode}.
 */
public record GroupNode(List<RichNode> children) implements RichNode {

    public GroupNode {
        children = List.copyOf(children);
    }

    public GroupNode(RichNode... children) {
        this(List.of(children));
    }
}
