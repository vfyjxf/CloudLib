package dev.vfyjxf.cloudlib.api.ui.inworld.group;

import java.util.Objects;

/**
 * {@link GroupStrategy} for vertical stacks (chat bubbles above a speaker, a
 * side column of notices): members stack in the caller's canonical order
 * starting at the group anchor, each row spaced by {@link #spacing} plus the
 * member's own height. The first {@link #maxVisible} members are placed;
 * the next {@link #maxAggregated} aggregate into the <em>last visible</em>
 * member (its "+N" badge); the rest hide (and linger out through the
 * coordinator's retract path).
 *
 * @param direction which way the column grows from the anchor
 * @param spacing the gap between neighboring members in gui pixels
 * @param maxVisible how many members the column shows
 * @param maxAggregated how many surplus members the last visible member
 *        absorbs before the rest hide
 */
public record StackInColumn(Direction direction, double spacing, int maxVisible, int maxAggregated)
        implements
            GroupStrategy {

    public StackInColumn {
        Objects.requireNonNull(direction, "direction");
        if (!Double.isFinite(spacing) || spacing < 0) {
            throw new IllegalArgumentException("spacing must be finite and non-negative: " + spacing);
        }
        if (maxVisible < 1) {
            throw new IllegalArgumentException("maxVisible must be at least 1: " + maxVisible);
        }
        if (maxAggregated < 0) {
            throw new IllegalArgumentException("maxAggregated must not be negative: " + maxAggregated);
        }
    }

    /** An upward stack with the chat-bubble defaults: 4 px gap, 4 visible, 9 "+N". */
    public static StackInColumn of() {
        return new StackInColumn(Direction.up, 4.0, 4, 9);
    }

    /** Which way a {@link StackInColumn} grows from the anchor. */
    public enum Direction {
        up, down
    }
}
