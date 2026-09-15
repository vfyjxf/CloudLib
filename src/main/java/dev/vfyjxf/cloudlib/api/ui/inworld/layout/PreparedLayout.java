package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.internal.ui.inworld.layout.LayoutMath;
import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

/**
 * A solved frame bundled with its input — the unit the render adapters
 * consume. {@code PreparedLayout} answers two questions without touching
 * the solver internals: "which visuals are world/screen/overflow?" and
 * "what is under this gui point?"
 * <p>
 * {@link #hitTest} checks in pointer order: the overflow drawer, the dock,
 * screen panels (front-to-back), then world panels along the eye ray.
 */
public final class PreparedLayout {

    private final LayoutFrame frame;
    private final LayoutResult result;

    public PreparedLayout(LayoutFrame frame, LayoutResult result) {
        this.frame = frame;
        this.result = result;
    }

    public LayoutFrame frame() {
        return frame;
    }

    public LayoutResult result() {
        return result;
    }

    public long frameIndex() {
        return result.frameIndex();
    }

    /** World-space visuals in solver order (back-to-front by score). */
    public List<PanelPlacement> worldPanels() {
        return panelsIn(Space.world);
    }

    /** Screen-space visuals in solver order. */
    public List<PanelPlacement> screenPanels() {
        return panelsIn(Space.screen);
    }

    private List<PanelPlacement> panelsIn(Space space) {
        List<PanelPlacement> panels = new ArrayList<>();
        for (PanelPlacement panel : result.panels()) {
            if (panel.space() == space) {
                panels.add(panel);
            }
        }
        return panels;
    }

    /**
     * Leaders that must render as world geometry — attached to a world
     * panel or routed in world space. Screen panels' leaders are emitted
     * as screen strokes.
     */
    public List<LeaderLine> worldLeaders() {
        List<LeaderLine> leaders = new ArrayList<>();
        for (LeaderLine leader : result.leaders()) {
            if (leader.world().size() >= 2) {
                leaders.add(leader);
            }
        }
        return leaders;
    }

    public List<LeaderLine> screenLeaders() {
        List<LeaderLine> leaders = new ArrayList<>();
        for (LeaderLine leader : result.leaders()) {
            if (leader.world().size() < 2 && leader.screen().size() >= 2) {
                leaders.add(leader);
            }
        }
        return leaders;
    }

    /**
     * What is under {@code point} (gui px)? Drawer → dock → screen panels
     * → world panels, nearest first.
     */
    public Optional<LayoutHit> hitTest(GuiVec point) {
        OverflowDrawer drawer = result.drawer();
        if (drawer != null && drawer.rect().contains(point)) {
            double[] uv = uv(point, drawer.rect());
            return Optional.of(new LayoutHit(
                    "overflow:drawer",
                    drawer.items().isEmpty() ? null : drawer.items().get(0),
                    uv[0],
                    uv[1],
                    true));
        }
        OverflowDock dock = result.dock();
        if (dock != null && dock.rect() != null && dock.rect().contains(point)) {
            double[] uv = uv(point, dock.rect());
            return Optional.of(new LayoutHit("overflow:dock", null, uv[0], uv[1], true));
        }

        for (int i = result.panels().size() - 1; i >= 0; i--) {
            PanelPlacement panel = result.panels().get(i);
            if (panel.space() == Space.screen && panel.screenRect().contains(point)) {
                return Optional.of(hit(panel, uv(point, panel.screenRect()), false));
            }
        }

        Projector camera = frame.camera();
        Vec3 eye = camera.eye();
        Vec3 through = camera.unproject(point, 1.0);
        Vec3 direction = through.subtract(eye);
        PanelPlacement nearest = null;
        double nearestDepth = Double.POSITIVE_INFINITY;
        double nearestU = 0;
        double nearestV = 0;
        for (PanelPlacement panel : result.panels()) {
            if (panel.space() != Space.world || panel.pose() == null) {
                continue;
            }
            List<Vec3> corners = LayoutMath.corners(panel.pose(), panel.worldWidth(), panel.worldHeight());
            Hit hit = rayQuad(eye, direction, corners);
            if (hit != null && hit.depth < nearestDepth) {
                nearestDepth = hit.depth;
                nearest = panel;
                nearestU = hit.u;
                nearestV = hit.v;
            }
        }
        return nearest == null ? Optional.empty() : Optional.of(hit(nearest, nearestU, nearestV, false));
    }

    private static double[] uv(GuiVec point, GuiRect rect) {
        return new double[] {(point.x() - rect.x()) / rect.width(), (point.y() - rect.y()) / rect.height()};
    }

    private static LayoutHit hit(PanelPlacement panel, double u, double v, boolean overflow) {
        return new LayoutHit(
                panel.visualId(), panel.active() == null ? null : panel.active().id(), u, v, overflow);
    }

    private static LayoutHit hit(PanelPlacement panel, double[] uv, boolean overflow) {
        return hit(panel, uv[0], uv[1], overflow);
    }

    /** Ray-quad intersection on the quad's plane, inside-test in 3D. */
    private static Hit rayQuad(Vec3 eye, Vec3 direction, List<Vec3> corners) {
        Vec3 a = corners.get(0);
        Vec3 b = corners.get(1);
        Vec3 c = corners.get(2);
        Vec3 normal = b.subtract(a).cross(c.subtract(a));
        double denom = direction.dot(normal);
        if (Math.abs(denom) < 1.0E-12) {
            return null;
        }
        double t = a.subtract(eye).dot(normal) / denom;
        if (t <= 0.0 || !Double.isFinite(t)) {
            return null;
        }
        Vec3 point = eye.add(direction.scale(t));
        Vec3 uAxis = b.subtract(a);
        Vec3 vAxis = corners.get(3).subtract(a);
        Vec3 local = point.subtract(a);
        double u = uAxis.lengthSqr() > 0 ? local.dot(uAxis) / uAxis.lengthSqr() : 0.0;
        double v = vAxis.lengthSqr() > 0 ? local.dot(vAxis) / vAxis.lengthSqr() : 0.0;
        return u >= -1.0E-6 && u <= 1.0 + 1.0E-6 && v >= -1.0E-6 && v <= 1.0 + 1.0E-6
                ? new Hit(t * direction.length(), u, v)
                : null;
    }

    private record Hit(double depth, double u, double v) {}
}
