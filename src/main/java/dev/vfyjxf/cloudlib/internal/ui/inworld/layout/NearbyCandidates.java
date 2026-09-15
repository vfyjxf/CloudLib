package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiRect;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.IntentContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutFrame;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.NearbyPosition;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelRequest;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Pose;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PositionSlot;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Projection;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SourceSnapshot;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Space;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Expands {@link NearbyPosition} seeds with extra candidates: the
 * previous pose when still in reach, and free screen spots found around
 * the best seeds, unprojected back to world space.
 */
final class NearbyCandidates {

    private NearbyCandidates() {}

    static List<PositionSlot> expand(
            LayoutFrame frame,
            PanelRequest request,
            SourceSnapshot source,
            Pose previous,
            double width,
            double height,
            List<PositionSlot> seeds) {
        if (request.space() != Space.world || !(request.position() instanceof NearbyPosition nearby)) {
            return seeds;
        }
        List<PositionSlot> result = new ArrayList<>(seeds);
        Vec3 origin = source.frame().at(nearby.localOrigin());
        double reach = nearby.radius() + Math.hypot(width, height) * 2;
        if (previous != null && previous.origin().distanceTo(origin) <= reach) {
            result.add(new PositionSlot("near:previous", previous.origin(), 0));
        }
        List<List<GuiVec>> obstacles = FrameSilhouettes.of(frame);
        IntentContext context = new IntentContext(frame, source, previous);
        for (int i = 0; i < Math.min(2, seeds.size()); i++) {
            Vec3 center = seeds.get(i).center();
            Projection projection = frame.camera().project(center);
            Pose pose = new Pose(center, request.orientation().basis(context, center));
            List<GuiVec> polygon = LayoutMath.projected(frame.camera(), pose, width, height);
            if (!projection.valid() || polygon.size() != 4) continue;
            GuiRect box = LayoutMath.bounds(polygon);
            for (var entry : FreeSpace.slots(frame, box.width(), box.height(), projection.point(), obstacles, 24)) {
                Vec3 point = frame.camera().unproject(entry.getValue().center(), projection.depth());
                if (point.distanceTo(origin) <= reach) {
                    result.add(
                            new PositionSlot("near:" + i + ":" + entry.getKey(), point, point.distanceTo(center) * 12));
                }
            }
        }
        return result;
    }
}
