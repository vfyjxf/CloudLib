package dev.vfyjxf.nimbusprojection.api.presentation;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Size;

/**
 * Where a flattened panel sits in inspect mode — a screen-space rect in
 * gui pixels.
 */
public record FlattenedGeometry(FloatPos pos, Size size) {

    /** Default flatten target: a natural-sized rect right of the anchor projection. */
    public static FlattenedGeometry beside(FloatPos anchorScreen, Size naturalSize) {
        return new FlattenedGeometry(
                new FloatPos(anchorScreen.x() + 24, anchorScreen.y() - naturalSize.height() / 2f), naturalSize);
    }
}
