package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

/**
 * Convenience {@link SourceSnapshot} builders for the common source
 * shapes: a point, a segment, a sphere, a box, a flat surface, or a
 * composite of other snapshots.
 */
public final class Sources {

    private Sources() {}

    public static SourceSnapshot point(String id, String dimension, Pose frame) {
        return snapshot(id, dimension, frame, List.of(new Attachment("point", frame.origin(), null, 0.0)), List.of());
    }

    public static SourceSnapshot segment(String id, String dimension, Pose frame, Vec3 fromLocal, Vec3 toLocal) {
        List<Attachment> attachments = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            attachments.add(new Attachment(
                    "segment:" + i,
                    frame.at(fromLocal.scale(1.0 - i * 0.5).add(toLocal.scale(i * 0.5))),
                    null,
                    i == 1 ? 0.0 : 5.0));
        }
        return snapshot(
                id, dimension, frame, attachments, List.of(new Hull(List.of(frame.at(fromLocal), frame.at(toLocal)))));
    }

    public static SourceSnapshot sphere(String id, String dimension, Pose frame, double radius) {
        if (radius <= 0.0) {
            throw new IllegalArgumentException();
        }
        List<Attachment> attachments = new ArrayList<>();
        List<Vec3> hull = new ArrayList<>();
        for (int ring = 0; ring < 3; ring++) {
            for (int sector = 0; sector < 8; sector++) {
                double latitude = (ring - 1) * Math.PI / 4.0;
                double longitude = sector * Math.PI / 4.0;
                Vec3 normal = frame.basis()
                        .apply(new Vec3(
                                Math.cos(latitude) * Math.cos(longitude),
                                Math.sin(latitude),
                                Math.cos(latitude) * Math.sin(longitude)));
                Vec3 point = frame.origin().add(normal.scale(radius));
                hull.add(frame.origin().add(normal.scale(radius * 1.18)));
                attachments.add(new Attachment("sphere:" + ring + ":" + sector, point, normal, 0.0));
            }
        }
        hull.add(frame.origin().add(frame.basis().up().scale(radius * 1.18)));
        hull.add(frame.origin().subtract(frame.basis().up().scale(radius * 1.18)));
        return snapshot(id, dimension, frame, attachments, List.of(new Hull(hull)));
    }

    /** A box source — {@code halfExtents} in source-frame units. */
    public static SourceSnapshot box(String id, String dimension, Pose frame, Vec3 halfExtents) {
        if (!(halfExtents.x <= 0.0) && !(halfExtents.y <= 0.0) && !(halfExtents.z <= 0.0)) {
            List<Attachment> attachments = new ArrayList<>();
            List<Vec3> hull = new ArrayList<>();
            Vec3[] axes = {
                new Vec3(1.0, 0.0, 0.0),
                new Vec3(-1.0, 0.0, 0.0),
                new Vec3(0.0, 1.0, 0.0),
                new Vec3(0.0, -1.0, 0.0),
                new Vec3(0.0, 0.0, 1.0),
                new Vec3(0.0, 0.0, -1.0)
            };
            for (int face = 0; face < 6; face++) {
                Vec3 center = new Vec3(
                        axes[face].x * halfExtents.x, axes[face].y * halfExtents.y, axes[face].z * halfExtents.z);
                attachments.add(new Attachment(
                        "face:" + face, frame.at(center), frame.basis().apply(axes[face]), 0.0));
            }
            for (int corner = 0; corner < 8; corner++) {
                hull.add(frame.at(new Vec3(
                        (corner & 1) == 0 ? -halfExtents.x : halfExtents.x,
                        (corner & 2) == 0 ? -halfExtents.y : halfExtents.y,
                        (corner & 4) == 0 ? -halfExtents.z : halfExtents.z)));
            }
            return snapshot(id, dimension, frame, attachments, List.of(new Hull(hull)));
        }
        throw new IllegalArgumentException();
    }

    /** A flat rectangular surface source — {@code width} × {@code height} in source-frame units. */
    public static SourceSnapshot surface(String id, String dimension, Pose frame, double width, double height) {
        List<Vec3> hull = new ArrayList<>();
        for (double[] corner : new double[][] {{-0.5, -0.5}, {0.5, -0.5}, {0.5, 0.5}, {-0.5, 0.5}}) {
            hull.add(frame.at(new Vec3(corner[0] * width, corner[1] * height, 0.0)));
        }
        List<Attachment> attachments = List.of(
                new Attachment("surface:center", frame.origin(), frame.basis().normal(), 0.0));
        return snapshot(id, dimension, frame, attachments, List.of(new Hull(hull)));
    }

    /**
     * Merges several snapshots under one id — attachments are re-keyed
     * {@code childId/key}, hulls and obstacle ids are unioned.
     */
    public static SourceSnapshot composite(String id, String dimension, Pose frame, List<SourceSnapshot> children) {
        List<Attachment> attachments = new ArrayList<>();
        List<Hull> hulls = new ArrayList<>();
        Set<String> obstacleIds = new HashSet<>();
        for (SourceSnapshot child : children) {
            for (Attachment attachment : child.attachments()) {
                attachments.add(new Attachment(
                        child.id() + "/" + attachment.key(),
                        attachment.point(),
                        attachment.normal(),
                        attachment.preference()));
            }
            hulls.addAll(child.hulls());
            obstacleIds.addAll(child.obstacleIds());
        }
        return new SourceSnapshot(id, dimension, 0L, true, frame, Vec3.ZERO, attachments, hulls, obstacleIds);
    }

    public static SourceSnapshot snapshot(
            String id, String dimension, Pose frame, List<Attachment> attachments, List<Hull> hulls) {
        return new SourceSnapshot(id, dimension, 0L, true, frame, Vec3.ZERO, attachments, hulls, Set.of());
    }
}
