package dev.vfyjxf.cloudlib.api.ui.floating;

import org.jetbrains.annotations.Nullable;

/**
 * Defines where to place the floating element relative to the reference element.
 * <p>
 * <ul>
 *   <li>{@link Side} — the side of the reference element: top, right, bottom, left</li>
 *   <li>{@link Alignment} — optional alignment along the cross axis: start, end</li>
 * </ul>
 * <p>
 * This gives 12 possible placements (4 sides × 3 alignments including center).
 */
public enum FloatingPlacement {

    //region placements

    top(Side.top, null),
    topStart(Side.top, Alignment.start),
    topEnd(Side.top, Alignment.end),

    right(Side.right, null),
    rightStart(Side.right, Alignment.start),
    rightEnd(Side.right, Alignment.end),

    bottom(Side.bottom, null),
    bottomStart(Side.bottom, Alignment.start),
    bottomEnd(Side.bottom, Alignment.end),

    left(Side.left, null),
    leftStart(Side.left, Alignment.start),
    leftEnd(Side.left, Alignment.end);

    //endregion

    private static final FloatingPlacement[] VALUES = values();

    private final Side side;
    private final @Nullable Alignment alignment;

    FloatingPlacement(Side side, @Nullable Alignment alignment) {
        this.side = side;
        this.alignment = alignment;
    }

    /**
     * @return the side of the reference element this placement is on
     */
    public Side side() {
        return side;
    }

    /**
     * @return the alignment along the cross axis, or null for centered
     */
    public @Nullable Alignment alignment() {
        return alignment;
    }

    //region axis utilities

    /**
     * @return the axis that runs along the side (the "side axis")
     */
    public Axis sideAxis() {
        return side.axis();
    }

    /**
     * @return the axis that runs along the alignment (the "alignment axis")
     */
    public Axis alignmentAxis() {
        return side.axis().opposite();
    }

    //endregion

    //region opposite & expanded placements

    /**
     * Returns the placement on the opposite side with the same alignment.
     * <p>
     * E.g. {@code top → bottom}, {@code top-start → bottom-start}.
     */
    public FloatingPlacement opposite() {
        return of(side.opposite(), alignment);
    }

    /**
     * Returns the placement with the opposite alignment on the same side.
     * <p>
     * E.g. {@code top-start → top-end}, {@code top → top} (no change).
     */
    public FloatingPlacement oppositeAlignment() {
        if (alignment == null) return this;
        return of(side, alignment.opposite());
    }

    /**
     * Returns all 12 placements as an array.
     */
    public static FloatingPlacement[] all() {
        return VALUES.clone();
    }

    //endregion

    //region factory

    /**
     * Finds the placement with the given side and alignment.
     *
     * @param side      the side
     * @param alignment the alignment, or null for centered
     * @return the matching placement
     */
    public static FloatingPlacement of(Side side, @Nullable Alignment alignment) {
        for (FloatingPlacement p : VALUES) {
            if (p.side == side && p.alignment == alignment) {
                return p;
            }
        }
        throw new IllegalArgumentException("No placement for side=" + side + " alignment=" + alignment);
    }

    //endregion

    //region inner types

    /**
     * The four sides of a rectangle.
     */
    public enum Side {
        top, right, bottom, left;

        /**
         * @return the axis this side lies on
         */
        public Axis axis() {
            return (this == top || this == bottom) ? Axis.y : Axis.x;
        }

        /**
         * @return the opposite side
         */
        public Side opposite() {
            return switch (this) {
                case top -> bottom;
                case bottom -> top;
                case left -> right;
                case right -> left;
            };
        }

        /**
         * @return true if this is a "start" side ({@link #top} or {@link #left})
         */
        public boolean isOrigin() {
            return this == top || this == left;
        }
    }

    /**
     * Alignment along the cross axis.
     */
    public enum Alignment {
        start, end;

        public Alignment opposite() {
            return this == start ? end : start;
        }
    }

    /**
     * A spatial axis.
     */
    public enum Axis {
        x, y;

        public Axis opposite() {
            return this == x ? y : x;
        }
    }

    //endregion
}
