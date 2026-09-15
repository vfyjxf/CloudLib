package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.IntentContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutFrame;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.MountedPosition;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelRequest;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Pose;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PositionSlot;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SourceSnapshot;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Space;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Tier;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

/**
 * World-space pose proposals: the request's position slots filtered by
 * facing, mounted-patch fit and world collision, at the requested tier's
 * extent.
 */
final class WorldPlacement {

    private WorldPlacement() {}

    record Placement(String key, Pose pose, double width, double height, double preference) {}

    static List<Placement> propose(
            LayoutFrame frame, PanelRequest request, SourceSnapshot source, @Nullable Pose previous, Tier tier) {
        if (!source.present() || !source.dimension().equals(frame.clock().dimension())) {
            return List.of();
        }
        double width = request.metrics().worldWidth();
        double height = tier == Tier.compact
                ? request.metrics().compactWorldHeight()
                : request.metrics().worldHeight();
        IntentContext context = new IntentContext(frame, source, previous);
        List<Placement> placements = new ArrayList<>();
        for (PositionSlot slot : NearbyCandidates.expand(
                frame,
                request,
                source,
                previous,
                width,
                height,
                request.position().slots(context, width, height))) {
            Pose pose = new Pose(slot.center(), request.orientation().basis(context, slot.center()));
            if ((!request.position().mounted() || mountedFits(request, context, pose, width, height))
                    && (request.space() != Space.world
                            || !(pose.basis().normal().dot(frame.camera().eye().subtract(pose.origin())) <= 1.0E-7)
                                    && (!request.material().requiresFreeWorldSpace()
                                            || frame.world()
                                                    .free(
                                                            pose,
                                                            width,
                                                            height,
                                                            frame.config().worldClearance())))) {
                placements.add(new Placement(slot.key(), pose, width, height, slot.preference()));
            }
        }
        return List.copyOf(placements);
    }

    /**
     * The world-visibility gate for depth-tested panels: the sampled
     * coverage fraction plus three sight-line probes across the quad.
     */
    static boolean visiblePanel(LayoutFrame frame, Pose pose, double width, double height) {
        if (LayoutMath.visibility(frame, pose, width, height, Set.of())
                < frame.config().minPanelVisibility()) {
            return false;
        }
        for (double t : new double[] {-0.45, 0.0, 0.45}) {
            if (!frame.world()
                    .visible(frame.camera().eye(), pose.at(new Vec3(t * width, height * 0.4, 0.0)), Set.of())) {
                return false;
            }
        }
        return true;
    }

    /** Does a mounted panel at {@code pose} still fit inside its declared surface patch? */
    static boolean mountedFits(PanelRequest request, IntentContext context, Pose pose, double width, double height) {
        if (!(request.position() instanceof MountedPosition mounted)) {
            return true;
        }
        for (Vec3 corner : LayoutMath.corners(pose, width, height)) {
            Vec3 local = context.source()
                    .frame()
                    .basis()
                    .inverse(corner.subtract(context.source().frame().origin()))
                    .subtract(mounted.localCenter());
            if (Math.abs(local.x) > mounted.patchWidth() * 0.5 + 1.0E-6
                    || Math.abs(local.y) > mounted.patchHeight() * 0.5 + 1.0E-6
                    || Math.abs(local.z - mounted.offset()) > 1.0E-6) {
                return false;
            }
        }
        return true;
    }
}
