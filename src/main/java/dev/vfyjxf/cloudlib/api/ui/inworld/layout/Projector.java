package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import net.minecraft.world.phys.Vec3;

/**
 * World ↔ gui-px projection for one rendered frame. Implementations are
 * {@link ViewCamera} (pinhole, built from yaw/pitch + fov) and
 * {@link MatrixCamera} (a full view-projection matrix captured from the
 * render pipeline).
 */
public interface Projector {

    Projection project(Vec3 point);

    /** The world point whose projection lands on {@code point} at view depth {@code depth}. */
    Vec3 unproject(GuiVec point, double depth);

    Vec3 eye();

    /** Viewport width in gui px. */
    double width();

    /** Viewport height in gui px. */
    double height();
}
