package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import net.minecraft.world.phys.Vec3;

/**
 * A resolved attach point on a world-space frame: the position plus the
 * frame's outward direction there (a unit vector, or {@link Vec3#ZERO}
 * when the point has no preferred exit — e.g. a panel's center).
 * <p>
 * The world-space counterpart of
 * {@link dev.vfyjxf.cloudlib.api.ui.border.ScreenPort}; routers read
 * {@link #outward()} to leave a panel along its edge normals.
 */
public record WorldPort(Vec3 pos, Vec3 outward) {

    /** Whether this port has a preferred exit direction. */
    public boolean hasOutward() {
        return outward.lengthSqr() > 1.0e-12;
    }
}
