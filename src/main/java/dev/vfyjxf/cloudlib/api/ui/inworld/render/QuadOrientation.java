package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Per-panel orientation memory for the degenerate-prone {@link QuadBasis}
 * orientation constructors. Holds the last right axis a non-degenerate frame
 * produced and feeds it back as the fallback the next time the observer sits
 * exactly overhead (yaw billboards) or looks straight down/up at a
 * ground-parallel quad — the frames where the freshly computable right axis
 * is a coin flip on floating-point noise. Keeping the previous frame's axis
 * there is what stops the quad from jittering through a 180° flip.
 * <p>
 * One instance per panel, driven once per frame:
 * <pre>{@code
 * QuadOrientation orientation = new QuadOrientation();
 * QuadBasis basis = orientation.groundParallel(center, cameraFacing, ppb, w, h);
 * }</pre>
 */
public final class QuadOrientation {

    private @Nullable Vec3 lastRight;

    /** Creates the memory empty — the first degenerate frame uses the deterministic default axis. */
    public QuadOrientation() {}

    /** Creates the memory seeded with an initial right axis (any non-zero vector). */
    public QuadOrientation(Vec3 seedRight) {
        this.lastRight = seedRight.normalize();
    }

    /** {@link QuadBasis#groundParallel(Vec3, Vec3, double, int, int)} with this memory's fallback. */
    public QuadBasis groundParallel(Vec3 center, Vec3 observerFacing, double pixelsPerBlock, int wPx, int hPx) {
        return remember(QuadBasis.groundParallel(center, observerFacing, lastRight, pixelsPerBlock, wPx, hPx));
    }

    /** {@link QuadBasis#groundParallelOnPlane} with this memory's fallback. */
    public QuadBasis groundParallelOnPlane(
        Vec3 center,
        Vec3 observerFacing,
        Vec3 planeNormal,
        double pixelsPerBlock,
        int wPx,
        int hPx
    ) {
        return remember(
            QuadBasis.groundParallelOnPlane(center, observerFacing, planeNormal, lastRight, pixelsPerBlock, wPx, hPx)
        );
    }

    /** {@link QuadBasis#yawBillboard(Vec3, Vec3, double, int, int)} with this memory's fallback. */
    public QuadBasis yawBillboard(Vec3 center, Vec3 observerPos, double pixelsPerBlock, int wPx, int hPx) {
        return remember(QuadBasis.yawBillboard(center, observerPos, lastRight, pixelsPerBlock, wPx, hPx));
    }

    /** The right axis the last orientation produced, or null before the first frame. */
    public @Nullable Vec3 lastRight() {
        return lastRight;
    }

    private QuadBasis remember(QuadBasis basis) {
        this.lastRight = basis.u().normalize();
        return basis;
    }
}
