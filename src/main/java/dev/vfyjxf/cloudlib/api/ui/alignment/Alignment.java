package dev.vfyjxf.cloudlib.api.ui.alignment;

import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Size;

//Design from Jetpack Compose

/**
 * An interface to calculate the position of a sized box inside an available space. {@link Alignment} is
 * often used to define the alignment of a layout inside a parent layout.
 * <p>
 */
public interface Alignment {

    // 2D Alignments
    Alignment TopStart = new BiasAlignment(-1f, -1f);
    Alignment TopCenter = new BiasAlignment(0f, -1f);
    Alignment TopEnd = new BiasAlignment(1f, -1f);
    Alignment CenterStart = new BiasAlignment(-1f, 0f);
    Alignment Center = new BiasAlignment(0f, 0f);
    Alignment CenterEnd = new BiasAlignment(1f, 0f);
    Alignment BottomStart = new BiasAlignment(-1f, 1f);
    Alignment BottomCenter = new BiasAlignment(0f, 1f);
    Alignment BottomEnd = new BiasAlignment(1f, 1f);

    // 1D Alignment.Verticals
    Alignment.Vertical Top = new BiasAlignment.Vertical(-1f);
    Alignment.Vertical CenterVertically = new BiasAlignment.Vertical(0f);
    Alignment.Vertical Bottom = new BiasAlignment.Vertical(1f);

    // 1D Alignment.Horizontals
    Alignment.Horizontal Start = new BiasAlignment.Horizontal(-1f);
    Alignment.Horizontal CenterHorizontally = new BiasAlignment.Horizontal(0f);
    Alignment.Horizontal End = new BiasAlignment.Horizontal(1f);

    /**
     * Calculates the position of a box of size {@code size} relative to the top left corner of an area
     * of size {@code space}. The returned offset can be negative or larger than {@code space - size},
     * meaning that the box will be positioned partially or completely outside the area.
     */
    Pos align(Size size, Size space);

    /**
     * An interface to calculate the position of box of a certain width inside an available width.
     * {@link Horizontal} is often used to define the horizontal alignment of a layout inside a
     * parent layout.
     */
    interface Horizontal {

        /**
         * Calculates the horizontal position of a box of width {@code size} relative to the left
         * side of an area of width {@code space}. The returned offset can be negative or larger than
         * {@code space - size} meaning that the box will be positioned partially or completely outside
         * the area.
         */
        int align(int size, int space);
    }

    /**
     * An interface to calculate the position of a box of a certain height inside an available
     * height. {@link Vertical} is often used to define the vertical alignment of a
     * layout inside a parent layout.
     */
    interface Vertical {

        /**
         * Calculates the vertical position of a box of height {@code size} relative to the top edge of
         * an area of height {@code  space}. The returned offset can be negative or larger than
         * {@code space - size} meaning that the box will be positioned partially or completely outside
         * the area.
         */
        int align(int size, int space);
    }
}

