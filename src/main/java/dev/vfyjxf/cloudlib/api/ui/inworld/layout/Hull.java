package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.internal.ui.inworld.layout.Vecs;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A world-space point cloud approximating a source's shape — used to keep
 * panels and leader lines out of the source's silhouette.
 */
public record Hull(List<Vec3> vertices) {

    public Hull {
        vertices = List.copyOf(vertices);
        for (Vec3 vertex : vertices) {
            Vecs.finite(vertex);
        }
    }
}
