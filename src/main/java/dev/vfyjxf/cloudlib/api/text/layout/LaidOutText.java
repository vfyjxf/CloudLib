package dev.vfyjxf.cloudlib.api.text.layout;

import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * The immutable result of laying out a rich text document: a sequence of positioned
 * lines with positioned fragments, ready for rendering and hit testing.
 *
 * @param width  the width of the widest line
 * @param height the total height (sum of line heights plus line spacing)
 * @param lines  the laid-out lines, top to bottom
 */
public record LaidOutText(float width, float height, List<TextLine> lines) {

    public static final LaidOutText empty = new LaidOutText(0, 0, List.of());

    public LaidOutText {
        lines = List.copyOf(lines);
    }

    public boolean isEmpty() {
        return lines.isEmpty();
    }

    /**
     * Finds the fragment containing the given point (laid-out text local
     * coordinates), or {@code null}.
     */
    public @Nullable TextFragment fragmentAt(float x, float y) {
        for (TextLine line : lines) {
            if (y < line.y() || y >= line.y() + line.height()) continue;
            for (TextFragment fragment : line.fragments()) {
                if (fragment.contains(x, y)) return fragment;
            }
            return null;
        }
        return null;
    }

    /**
     * Finds the first interactive fragment containing the given point, or
     * {@code null}.
     */
    public @Nullable TextFragment interactiveFragmentAt(float x, float y) {
        TextFragment fragment = fragmentAt(x, y);
        return fragment != null && fragment.interactive() ? fragment : null;
    }
}
