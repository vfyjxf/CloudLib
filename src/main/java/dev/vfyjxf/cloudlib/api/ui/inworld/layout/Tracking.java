package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import net.minecraft.world.phys.Vec3;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * Source tracking helpers — turns a per-tick {@link TickState} supplier
 * into an interpolated {@link SourceProvider}. Poses interpolate linearly
 * in position and slerp in orientation; a discontinuity flag or a jump
 * larger than {@code maxJump} snaps instead of smearing.
 */
public final class Tracking {

    private Tracking() {}

    /**
     * A {@link SourceProvider} sampling {@code ticks} each frame and
     * rebuilding the source shape at the interpolated pose via
     * {@code geometry}. {@code maxJump} is the discontinuity distance in
     * blocks.
     */
    public static SourceProvider interpolated(Supplier<TickState> ticks, Geometry geometry, double maxJump) {
        if (maxJump <= 0.0) {
            throw new IllegalArgumentException();
        }
        return context -> {
            TickState tick = Objects.requireNonNull(ticks.get());
            if (!tick.present()) {
                return SourceSnapshot.absent(tick.id(), tick.dimension(), tick.generation());
            }
            double t = Math.max(0.0, Math.min(1.0, context.partialTick()));
            Pose pose = !tick.discontinuity()
                            && tick.previous() != null
                            && !(tick.current()
                                            .origin()
                                            .distanceTo(tick.previous().origin())
                                    > maxJump)
                    ? interpolate(tick.previous(), tick.current(), t)
                    : tick.current();
            SourceSnapshot shape = geometry.at(tick.id(), tick.dimension(), pose);
            return new SourceSnapshot(
                    tick.id(),
                    tick.dimension(),
                    tick.generation(),
                    true,
                    pose,
                    tick.velocity(),
                    shape.attachments(),
                    shape.hulls(),
                    shape.obstacleIds());
        };
    }

    public static Pose interpolate(Pose from, Pose to, double t) {
        return new Pose(
                from.origin().scale(1.0 - t).add(to.origin().scale(t)),
                Quaternion.from(from.basis())
                        .slerp(Quaternion.from(to.basis()), t)
                        .basis());
    }

    /** Builds a source's attachments/hulls at a sampled pose. */
    @FunctionalInterface
    public interface Geometry {
        SourceSnapshot at(String id, String dimension, Pose pose);
    }

    /** The per-tick pose record the interpolated provider reads. */
    public record TickState(
            String id,
            String dimension,
            long generation,
            boolean present,
            Pose previous,
            Pose current,
            Vec3 velocity,
            boolean discontinuity) {}

    public record Quaternion(double x, double y, double z, double w) {

        Quaternion unit() {
            double len = Math.sqrt(x * x + y * y + z * z + w * w);
            return new Quaternion(x / len, y / len, z / len, w / len);
        }

        double dot(Quaternion other) {
            return x * other.x + y * other.y + z * other.z + w * other.w;
        }

        Quaternion mul(double scale) {
            return new Quaternion(x * scale, y * scale, z * scale, w * scale);
        }

        Quaternion add(Quaternion other) {
            return new Quaternion(x + other.x, y + other.y, z + other.z, w + other.w);
        }

        public Quaternion slerp(Quaternion other, double t) {
            Quaternion a = unit();
            Quaternion b = other.unit();
            double cos = a.dot(b);
            if (cos < 0.0) {
                b = b.mul(-1.0);
                cos = -cos;
            }
            if (cos > 0.9995) {
                return a.mul(1.0 - t).add(b.mul(t)).unit();
            }
            double angle = Math.acos(Math.max(-1.0, Math.min(1.0, cos)));
            double sin = Math.sin(angle);
            return a.mul(Math.sin((1.0 - t) * angle) / sin)
                    .add(b.mul(Math.sin(t * angle) / sin))
                    .unit();
        }

        public PanelBasis basis() {
            Quaternion q = unit();
            Vec3 right = q.rotate(new Vec3(1.0, 0.0, 0.0));
            Vec3 up = q.rotate(new Vec3(0.0, 1.0, 0.0));
            return PanelBasis.of(right, up);
        }

        Vec3 rotate(Vec3 v) {
            Vec3 q = new Vec3(x, y, z);
            Vec3 t = q.cross(v).scale(2.0);
            return v.add(t.scale(w)).add(q.cross(t));
        }

        public static Quaternion from(PanelBasis basis) {
            double m00 = basis.right().x, m01 = basis.up().x, m02 = basis.normal().x;
            double m10 = basis.right().y, m11 = basis.up().y, m12 = basis.normal().y;
            double m20 = basis.right().z, m21 = basis.up().z, m22 = basis.normal().z;
            double trace = m00 + m11 + m22;
            if (trace > 0.0) {
                double s = Math.sqrt(trace + 1.0) * 2.0;
                return new Quaternion((m21 - m12) / s, (m02 - m20) / s, (m10 - m01) / s, s / 4.0).unit();
            } else if (m00 > m11 && m00 > m22) {
                double s = Math.sqrt(1.0 + m00 - m11 - m22) * 2.0;
                return new Quaternion(s / 4.0, (m01 + m10) / s, (m02 + m20) / s, (m21 - m12) / s).unit();
            } else if (m11 > m22) {
                double s = Math.sqrt(1.0 + m11 - m00 - m22) * 2.0;
                return new Quaternion((m01 + m10) / s, s / 4.0, (m12 + m21) / s, (m02 - m20) / s).unit();
            } else {
                double s = Math.sqrt(1.0 + m22 - m00 - m11) * 2.0;
                return new Quaternion((m02 + m20) / s, (m12 + m21) / s, s / 4.0, (m10 - m01) / s).unit();
            }
        }
    }
}
