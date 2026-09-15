package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.internal.ui.inworld.layout.Vecs;
import net.minecraft.world.phys.Vec3;

/**
 * The orientation of a panel quad: a right-handed orthonormal basis in
 * world space ({@code right × up = normal}). All components are
 * normalized on construction; {@link #of} orthogonalizes {@code up}
 * against {@code right}.
 */
public record PanelBasis(Vec3 right, Vec3 up, Vec3 normal) {

    public PanelBasis {
        right = Vecs.unit(Vecs.finite(right));
        up = Vecs.unit(Vecs.finite(up));
        normal = Vecs.unit(Vecs.finite(normal));
        if (Math.abs(right.dot(up)) > 1.0E-6 || right.cross(up).distanceTo(normal) > 1.0E-6) {
            throw new IllegalArgumentException("right-handed orthonormal basis required");
        }
    }

    public static PanelBasis identity() {
        return new PanelBasis(new Vec3(1, 0, 0), new Vec3(0, 1, 0), new Vec3(0, 0, 1));
    }

    /** An orthonormal basis from a right axis and an arbitrary up hint. */
    public static PanelBasis of(Vec3 right, Vec3 up) {
        Vec3 r = Vecs.unit(right);
        Vec3 u = Vecs.unit(up.subtract(r.scale(up.dot(r))));
        return new PanelBasis(r, u, r.cross(u));
    }

    /** World-space direction of panel-local {@code local}. */
    public Vec3 apply(Vec3 local) {
        return right.scale(local.x).add(up.scale(local.y)).add(normal.scale(local.z));
    }

    /** This basis applied after {@code inner} — panel-in-panel composition. */
    public PanelBasis compose(PanelBasis inner) {
        return new PanelBasis(apply(inner.right), apply(inner.up), apply(inner.normal));
    }

    /** Panel-local coordinates of a world-space {@code direction}. */
    public Vec3 inverse(Vec3 direction) {
        return new Vec3(direction.dot(right), direction.dot(up), direction.dot(normal));
    }

    public static PanelBasis axisAngle(Vec3 axis, double radians) {
        Vec3 n = Vecs.unit(axis);
        double cos = Math.cos(radians);
        double sin = Math.sin(radians);
        Vec3 rotatedRight = rotate(new Vec3(1, 0, 0), n, cos, sin);
        Vec3 rotatedUp = rotate(new Vec3(0, 1, 0), n, cos, sin);
        return of(rotatedRight, rotatedUp);
    }

    private static Vec3 rotate(Vec3 v, Vec3 axis, double cos, double sin) {
        return v.scale(cos).add(axis.cross(v).scale(sin)).add(axis.scale(axis.dot(v) * (1.0 - cos)));
    }
}
