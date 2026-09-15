package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.BoxObstacle;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Config;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.FaceCamera;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.FixedOrientation;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.FixedPosition;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.FollowPosition;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.HudRegion;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Interaction;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutFrame;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.MatrixCamera;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.MountedPosition;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.NearbyPosition;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Obstacle;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.OrientationPolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelBasis;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelRequest;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PositionPolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.ReadableBothSides;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SourceOrientation;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SourceSnapshot;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SphereObstacle;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.ViewCamera;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.WorldObstacles;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.Map;

/**
 * The frame cache key — only frames built entirely of immutable built-in
 * parts are cacheable. Custom policies or projectors are always
 * evaluated, never silently reused.
 */
public record StableFrame(
        Object camera,
        double width,
        double height,
        PanelBasis viewBasis,
        List<HudRegion> hud,
        WorldObstacles world,
        List<PanelRequest> requests,
        Interaction interaction,
        Config config,
        Map<String, SourceSnapshot> sources) {

    /** The cache key for {@code frame}, or null when any input isn't provably immutable. */
    public static @Nullable StableFrame of(LayoutFrame frame, Map<String, SourceSnapshot> sources) {
        Object camera;
        if (frame.camera() instanceof ViewCamera pinhole) {
            camera = pinhole;
        } else if (frame.camera() instanceof MatrixCamera matrix) {
            camera = matrix.cacheKey();
        } else {
            return null;
        }
        for (PanelRequest request : frame.requests()) {
            PositionPolicy position = request.position();
            if (!(position instanceof FixedPosition
                    || position instanceof FollowPosition
                    || position instanceof NearbyPosition
                    || position instanceof MountedPosition)) {
                return null;
            }
            if (!immutable(request.orientation())) return null;
        }
        for (Obstacle obstacle : frame.world().obstacles()) {
            if (!(obstacle.shape() instanceof BoxObstacle || obstacle.shape() instanceof SphereObstacle)) {
                return null;
            }
        }
        return new StableFrame(
                camera,
                frame.camera().width(),
                frame.camera().height(),
                frame.viewBasis(),
                frame.hud(),
                frame.world(),
                frame.requests(),
                frame.interaction(),
                frame.config(),
                Map.copyOf(sources));
    }

    private static boolean immutable(OrientationPolicy orientation) {
        return orientation instanceof FixedOrientation
                || orientation instanceof FaceCamera
                || orientation instanceof SourceOrientation
                || orientation instanceof ReadableBothSides both && immutable(both.base());
    }
}
