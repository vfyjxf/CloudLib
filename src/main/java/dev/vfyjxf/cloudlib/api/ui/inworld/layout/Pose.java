package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.internal.ui.inworld.layout.Vecs;
import net.minecraft.world.phys.Vec3;

import java.util.Objects;

/** A rigid frame in world space: an origin plus an orthonormal {@link PanelBasis}. */
public record Pose(Vec3 origin, PanelBasis basis) {

    public Pose {
        Vecs.finite(origin);
        Objects.requireNonNull(basis);
    }

    /** The world-space point at {@code local} coordinates in this frame. */
    public Vec3 at(Vec3 local) {
        return origin.add(basis.apply(local));
    }

    /** This pose applied after {@code inner} — panel-in-panel composition. */
    public Pose compose(Pose inner) {
        return new Pose(at(inner.origin), basis.compose(inner.basis));
    }
}
