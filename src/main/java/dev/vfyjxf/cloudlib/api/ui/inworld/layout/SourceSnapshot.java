package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.internal.ui.inworld.layout.Vecs;
import net.minecraft.world.phys.Vec3;

import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * One frame's view of a source: where it is, how it moves, the attachment
 * points leaders may use, hull approximations for occlusion and the world
 * obstacle ids the source itself owns (so panels never self-collide).
 */
public record SourceSnapshot(
        String id,
        String dimension,
        long generation,
        boolean present,
        Pose frame,
        Vec3 velocity,
        List<Attachment> attachments,
        List<Hull> hulls,
        Set<String> obstacleIds) {

    public SourceSnapshot {
        Objects.requireNonNull(id);
        Objects.requireNonNull(dimension);
        Objects.requireNonNull(frame);
        Vecs.finite(velocity);
        attachments = List.copyOf(attachments);
        hulls = List.copyOf(hulls);
        obstacleIds = Set.copyOf(obstacleIds);
        if (present && attachments.isEmpty()) {
            throw new IllegalArgumentException("a present source needs at least one attachment");
        }
    }

    public static SourceSnapshot absent(String id, String dimension, long generation) {
        return new SourceSnapshot(
                id,
                dimension,
                generation,
                false,
                new Pose(Vec3.ZERO, PanelBasis.identity()),
                Vec3.ZERO,
                List.of(),
                List.of(),
                Set.of());
    }
}
