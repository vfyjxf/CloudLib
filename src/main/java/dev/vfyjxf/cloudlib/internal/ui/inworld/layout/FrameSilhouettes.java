package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.HudRegion;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutFrame;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Obstacle;

import java.util.ArrayList;
import java.util.List;
import java.util.Map;
import java.util.WeakHashMap;

/**
 * Per-frame cache of screen-space obstacle silhouettes (HUD regions +
 * projected world-obstacle hulls) shared by candidate generators and
 * the leader router — the projection+hull cost is paid once per frame
 * instead of once per candidate.
 */
final class FrameSilhouettes {

    private FrameSilhouettes() {}

    private static final Map<LayoutFrame, List<List<GuiVec>>> cache = new WeakHashMap<>();

    /**
     * HUD polygons plus the screen hull of every solid-or-opaque world
     * obstacle that projects — the {@code obstacles} input shape used
     * by {@link FreeSpace#slots} callers.
     */
    static List<List<GuiVec>> of(LayoutFrame frame) {
        return cache.computeIfAbsent(frame, FrameSilhouettes::build);
    }

    private static List<List<GuiVec>> build(LayoutFrame frame) {
        List<List<GuiVec>> obstacles = new ArrayList<>();
        for (HudRegion hud : frame.hud()) obstacles.add(hud.polygon());
        for (Obstacle obstacle : frame.world().obstacles()) {
            if (!obstacle.solid() && !obstacle.opaque()) continue;
            List<GuiVec> points =
                    ScreenMath.projectAll(frame.camera(), obstacle.shape().previewVertices());
            if (points.size() >= 3) obstacles.add(ScreenMath.hull(points));
        }
        return List.copyOf(obstacles);
    }
}
