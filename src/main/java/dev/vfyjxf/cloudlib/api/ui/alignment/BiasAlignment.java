package dev.vfyjxf.cloudlib.api.ui.alignment;

import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Size;

/**
 * An [Alignment] specified by bias: for example, a bias of -1 represents alignment to the
 * start/top, a bias of 0 will represent centering, and a bias of 1 will represent end/bottom.
 * Any value can be specified to obtain an alignment. Inside the [-1, 1] range, the obtained
 * alignment will position the aligned size fully inside the available space, while outside the
 * range it will the aligned size will be positioned partially or completely outside.
 *
 * @see Alignment
 */
public record BiasAlignment(
        float horizontalBias,
        float verticalBias
) implements Alignment {
    @Override
    public Pos align(Size size, Size space) {
        // Convert to Px first and only round at the end, to avoid rounding twice while calculating
        // the new positions
        var centerX = (space.width - size.width) / 2f;
        var centerY = (space.height - size.height) / 2f;
        var x = centerX * (1 + horizontalBias);
        var y = centerY * (1 + verticalBias);
        return new Pos(Math.round(x), Math.round(y));
    }

    public record Horizontal(float bias) implements Alignment.Horizontal {

        @Override
        public int align(int size, int space) {
            // Convert to Px first and only round at the end, to avoid rounding twice while
            // calculating the new positions
            var center = (space - size) / 2f;
            return Math.round(center * (1 + bias));
        }
    }

    public record Vertical(float bias) implements Alignment.Vertical {
        @Override
        public int align(int size, int space) {
            // Convert to Px first and only round at the end, to avoid rounding twice while
            // calculating the new positions
            var center = (space - size) / 2f;
            return Math.round(center * (1 + bias));
        }
    }
}
