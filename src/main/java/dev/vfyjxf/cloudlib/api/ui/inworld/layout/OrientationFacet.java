package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import java.util.Objects;

/**
 * The orientation facet: how the element's surface faces the world (G8) —
 * the declaration the render side realizes through {@code QuadBasis}
 * orientation constructors (billboard/groundParallel/blockFace families);
 * the layout pipeline consumes it to shape the arbitration footprint and to
 * validate it against the {@link AnchorFacet}.
 *
 * @param mode the orientation mode; screen orientations belong to screen
 *        anchors, world orientations to world anchors (see
 *        {@link ElementSpec}'s validation table)
 */
public record OrientationFacet(Mode mode) {

    public OrientationFacet {
        Objects.requireNonNull(mode, "mode");
    }

    /** The orientation modes. */
    public enum Mode {
        /** Faces the camera every frame (the generalization of vanilla billboards). */
        cameraBillboard,
        /** Billboards around the world up axis only — rotates with the viewer, never tilts. */
        yawBillboard,
        /** Lies parallel to the ground, hugging slopes (ground decals). */
        groundParallel,
        /** Fixed to a block face's quad. */
        blockFace,
        /** Screen-space orientation (camera-tracked and panel elements). */
        screen
    }

    /** Whether this orientation renders in the world pass. */
    public boolean worldFacing() {
        return mode == Mode.cameraBillboard
                || mode == Mode.yawBillboard
                || mode == Mode.groundParallel
                || mode == Mode.blockFace;
    }

    /** {@link #cameraBillboard}. */
    public static OrientationFacet cameraBillboard() {
        return new OrientationFacet(Mode.cameraBillboard);
    }

    /** {@link #yawBillboard}. */
    public static OrientationFacet yawBillboard() {
        return new OrientationFacet(Mode.yawBillboard);
    }

    /** {@link #groundParallel}. */
    public static OrientationFacet groundParallel() {
        return new OrientationFacet(Mode.groundParallel);
    }

    /** {@link #blockFace}. */
    public static OrientationFacet blockFace() {
        return new OrientationFacet(Mode.blockFace);
    }

    /** {@link #screen}. */
    public static OrientationFacet screen() {
        return new OrientationFacet(Mode.screen);
    }
}
