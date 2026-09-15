package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import com.mojang.blaze3d.vertex.BufferBuilder;
import dev.vfyjxf.cloudlib.api.ui.border.BorderPoint;
import dev.vfyjxf.cloudlib.api.ui.border.Corner;
import dev.vfyjxf.cloudlib.api.ui.style.Edge;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;
import org.joml.Matrix4f;

/**
 * A {@link WorldUiPanel}'s quad measured for attachment — the world-space
 * half of the border API, mirroring
 * {@link dev.vfyjxf.cloudlib.api.ui.border.RectBorder}.
 * <p>
 * Positions resolve against the quad's {@code u}/{@code v} basis, so
 * parametric edge points are expressed in panel pixels ({@code t} in
 * [0,1] along each edge) while results are world positions. Edge
 * {@link #outward(Edge)} directions lie <em>in the panel plane</em> —
 * {@code top → -v̂}, {@code right → +û}, etc. — so leads exit a panel
 * along its face, like a callout leader.
 * <p>
 * {@link #nearestPort(Vec3)} always lands on the perimeter: a point
 * projecting inside the quad snaps to its nearest edge rather than the
 * surface.
 * <p>
 * Measurement draws nothing by itself; {@link #emitLines} is the opt-in
 * wireframe for {@link WorldUiPanel.LinesEmitter} callbacks.
 */
public final class QuadBorder {

    private static final double eps = 1.0e-6;

    private final QuadBasis basis;
    private final int wPx;
    private final int hPx;

    private QuadBorder(QuadBasis basis, int wPx, int hPx) {
        this.basis = basis;
        this.wPx = wPx;
        this.hPx = hPx;
    }

    public static QuadBorder of(QuadBasis basis, int wPx, int hPx) {
        return new QuadBorder(basis, wPx, hPx);
    }

    /** The panel's live border, or null while it has no placed quad. */
    public static @Nullable QuadBorder of(WorldUiPanel panel) {
        QuadBasis q = panel.basis();
        return q == null ? null : of(q, panel.width(), panel.height());
    }

    public QuadBasis basis() {
        return basis;
    }

    public int width() {
        return wPx;
    }

    public int height() {
        return hPx;
    }

    public Vec3 center() {
        return basis.center(wPx, hPx);
    }

    // region measurement

    /** Edge length in world units — top/bottom run along {@code u}, left/right along {@code v}. */
    public double edgeLength(Edge edge) {
        return switch (edge) {
            case top, bottom -> basis.u().length() * wPx;
            case left, right -> basis.v().length() * hPx;
        };
    }

    /** The panel-space outward direction of an edge, in the quad plane — a unit vector. */
    public Vec3 outward(Edge edge) {
        return switch (edge) {
            case top -> basis.v().normalize().reverse();
            case bottom -> basis.v().normalize();
            case left -> basis.u().normalize().reverse();
            case right -> basis.u().normalize();
        };
    }

    /**
     * A parametric point on an edge — {@code t} in [0,1], measured along
     * +u on top/bottom and along +v on left/right.
     */
    public Vec3 edgePoint(Edge edge, double t) {
        Vec3 o = basis.origin(), u = basis.u(), v = basis.v();
        return switch (edge) {
            case top -> o.add(u.scale(t * wPx));
            case bottom -> o.add(u.scale(t * wPx)).add(v.scale(hPx));
            case left -> o.add(v.scale(t * hPx));
            case right -> o.add(u.scale(wPx)).add(v.scale(t * hPx));
        };
    }

    public Vec3 edgeCenter(Edge edge) {
        return edgePoint(edge, 0.5);
    }

    public Vec3 corner(Corner corner) {
        return basis.origin()
                .add(basis.u().scale(corner.u() * wPx))
                .add(basis.v().scale(corner.v() * hPx));
    }

    // endregion

    // region ports

    /** An edge port — point on the edge, outward = the edge's in-plane normal. */
    public WorldPort port(Edge edge, double t) {
        return new WorldPort(edgePoint(edge, t), outward(edge));
    }

    /** A corner port — outward is the bisector of the two meeting edges' normals. */
    public WorldPort cornerPort(Corner corner) {
        return new WorldPort(corner(corner), normalize(outward(corner.edgeA()).add(outward(corner.edgeB()))));
    }

    /** The center port — exits toward the panel's readable side (its viewer normal). */
    public WorldPort centerPort() {
        Vec3 n = basis.u().cross(basis.v());
        Vec3 out = n.lengthSqr() < eps * eps ? Vec3.ZERO : n.normalize().reverse();
        return new WorldPort(center(), out);
    }

    /**
     * The perimeter port closest to {@code toward}. The point is projected
     * into panel-pixel coordinates; inside the quad it snaps to the
     * nearest edge, outside it clamps onto the boundary (a corner hit
     * takes the bisector of the two meeting normals).
     */
    public WorldPort nearestPort(Vec3 toward) {
        Vec3 d = toward.subtract(basis.origin());
        double au = d.dot(basis.u()) / basis.u().lengthSqr();
        double av = d.dot(basis.v()) / basis.v().lengthSqr();
        if (au >= 0 && au <= wPx && av >= 0 && av <= hPx) {
            // projects inside — nearest edge in panel-px distance
            double dl = au, dr = wPx - au, dt = av, db = hPx - av;
            double m = Math.min(Math.min(dl, dr), Math.min(dt, db));
            if (m == dl) return port(Edge.left, hPx > 0 ? av / hPx : 0);
            if (m == dr) return port(Edge.right, hPx > 0 ? av / hPx : 0);
            if (m == dt) return port(Edge.top, wPx > 0 ? au / wPx : 0);
            return port(Edge.bottom, wPx > 0 ? au / wPx : 0);
        }
        double cu = Math.max(0, Math.min(wPx, au));
        double cv = Math.max(0, Math.min(hPx, av));
        Vec3 pos = basis.origin().add(basis.u().scale(cu)).add(basis.v().scale(cv));
        Vec3 out = Vec3.ZERO;
        if (cu <= eps) out = out.add(outward(Edge.left));
        if (cu >= wPx - eps) out = out.add(outward(Edge.right));
        if (cv <= eps) out = out.add(outward(Edge.top));
        if (cv >= hPx - eps) out = out.add(outward(Edge.bottom));
        return new WorldPort(pos, normalize(out));
    }

    /** Resolves a {@link BorderPoint} spec into a port. {@code toward} feeds {@link BorderPoint#nearest()}. */
    public WorldPort port(BorderPoint point, @Nullable Vec3 toward) {
        return switch (point.kind()) {
            case nearest -> toward == null ? centerPort() : nearestPort(toward);
            case edge -> port(point.edge(), point.t());
            case corner -> cornerPort(point.corner());
            case center -> centerPort();
        };
    }

    // endregion

    // region opt-in drawing

    /**
     * Emits the frame's four edges as {@code POSITION_COLOR} debug lines —
     * the wireframe a {@link WorldUiPanel.LinesEmitter} can offer.
     */
    public void emitLines(BufferBuilder buffer, Matrix4f worldToView, int argb) {
        for (Edge edge : Edge.values()) {
            WorldLines.line(buffer, worldToView, edgePoint(edge, 0), edgePoint(edge, 1), argb);
        }
    }

    // endregion

    private static Vec3 normalize(Vec3 v) {
        return v.lengthSqr() < eps * eps ? Vec3.ZERO : v.normalize();
    }
}
