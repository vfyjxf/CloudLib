package dev.vfyjxf.cloudlib.api.ui.inworld.trace.cursor;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.inworld.Projection;
import dev.vfyjxf.cloudlib.testutil.GeometryAsserts;
import dev.vfyjxf.cloudlib.testutil.ProjectionSimulator;
import net.minecraft.core.BlockPos;
import net.minecraft.world.phys.Vec3;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The screen-side corner pick ({@code BlockOriginCursor.screenAnchorCorner})
 * against real projections: a screen leader attaches to what the eye sees of
 * the block on screen, so every corner the projection can place is a
 * candidate — from an oblique camera the corner nearest the panel can sit on
 * the camera-back half, and the camera-facing rule that keeps world-side pins
 * from bleeding through faces would detach the leader from the block's
 * projected outline. The dead-zone hysteresis keeps a near-tie between corners
 * from flipping with camera micro-motion.
 */
class BlockOriginCursorScreenCornerTest {

    private static final double inflate = 0.003;
    private static final double hysteresisPx = 4.0;

    private static FloatPos[] projections(Projection proj, BlockPos pos) {
        FloatPos[] out = new FloatPos[8];
        for (int i = 0; i < 8; i++) {
            out[i] = proj.worldToScreen(BlockOriginCursor.cornerPos(pos, i, inflate));
        }
        return out;
    }

    private static double distance(FloatPos p, double x, double y) {
        return Math.hypot(p.x() - x, p.y() - y);
    }

    /** The brute-force specification: the nearest projectable corner's screen position. */
    private static FloatPos nearestProjection(Projection proj, BlockPos pos, double x, double y) {
        FloatPos best = null;
        double bestD = Double.MAX_VALUE;
        for (FloatPos s : projections(proj, pos)) {
            if (s == null) continue;
            double d = distance(s, x, y);
            if (d < bestD) {
                bestD = d;
                best = s;
            }
        }
        return best;
    }

    @Test
    void obliqueCameraPanelBelowAttachesToTheProjectedLowerCorner() {
        // the camera looks down at the block steeply from above and the side;
        // the panel hangs below the block's projection
        Projection proj = ProjectionSimulator.at(4, 76, 4)
                .lookAt(0.5, 64.5, 0.5)
                .screen(480, 270)
                .build();
        BlockPos pos = new BlockPos(0, 64, 0);
        double tx = 240, ty = 250;

        BlockOriginCursor.ScreenCorner pick = BlockOriginCursor.screenAnchorCorner(pos, inflate, proj, tx, ty, null);
        assertNotNull(pick, "the block is fully on screen — a corner must resolve");
        GeometryAsserts.assertPosEquals(nearestProjection(proj, pos, tx, ty), pick.screen(), 1.0e-4);

        // the pick is the box's bottom edge — the projected lower corner the
        // camera-facing rule excludes when the camera looks down from above
        Vec3 picked = BlockOriginCursor.cornerPos(pos, pick.index(), inflate);
        assertEquals(pos.getY() - inflate, picked.y, 1.0e-9, "a bottom-face corner");

        // pin the replaced behavior: for this same geometry the camera-facing
        // rule picks a top-face corner — visually the block's middle, higher
        // on screen and farther from the panel — the detached leader origin
        // that strayed as the view steepened
        FloatPos cameraFacing = BlockOriginCursor.anchorCorner(pos, inflate, proj, proj.cameraPos(), tx, ty);
        assertNotNull(cameraFacing);
        assertTrue(cameraFacing.y() < pick.screen().y(), "the camera-facing pick sits higher on screen");
        assertTrue(
                distance(cameraFacing, tx, ty) > distance(pick.screen(), tx, ty),
                "the camera-facing pick is farther from the panel than the nearest corner");
    }

    @Test
    void deadZoneHoldsTheIncumbentAgainstASlightlyCloserCorner() {
        Projection proj = ProjectionSimulator.at(0.5, 0.5, 0)
                .lookAt(0.5, 0.5, -1)
                .screen(400, 240)
                .build();
        BlockPos pos = new BlockPos(0, 0, -6);
        // the straight-on camera mirrors one corner pair around the screen
        // center: start the target exactly between them, then slide it less
        // than the dead zone toward one side
        FloatPos[] all = projections(proj, pos);
        BlockOriginCursor.ScreenCorner first =
                BlockOriginCursor.screenAnchorCorner(pos, inflate, proj, 200, all[0].y(), null);
        int other = first.index() ^ 1; // the mirrored pair differs in the x bit
        double px = all[other].x() - all[first.index()].x();
        double py = all[other].y() - all[first.index()].y();
        double len = Math.hypot(px, py);
        double slide = (hysteresisPx - 2.0) / len; // the challenger wins by 2 px

        BlockOriginCursor.ScreenCorner pick = BlockOriginCursor.screenAnchorCorner(
                pos, inflate, proj, 200 + px * slide, all[0].y() + py * slide, first);
        assertEquals(first.index(), pick.index(), "a corner 2 px closer than the incumbent must not take over");
    }

    @Test
    void beyondTheDeadZoneTheCloserCornerTakesOver() {
        Projection proj = ProjectionSimulator.at(0.5, 0.5, 0)
                .lookAt(0.5, 0.5, -1)
                .screen(400, 240)
                .build();
        BlockPos pos = new BlockPos(0, 0, -6);
        FloatPos[] all = projections(proj, pos);
        BlockOriginCursor.ScreenCorner first =
                BlockOriginCursor.screenAnchorCorner(pos, inflate, proj, 200, all[0].y(), null);
        int other = first.index() ^ 1;
        double px = all[other].x() - all[first.index()].x();
        double py = all[other].y() - all[first.index()].y();
        double len = Math.hypot(px, py);
        double slide = (hysteresisPx + 2.0) / len; // the challenger wins by 6 px

        BlockOriginCursor.ScreenCorner pick = BlockOriginCursor.screenAnchorCorner(
                pos, inflate, proj, 200 + px * slide, all[0].y() + py * slide, first);
        assertEquals(other, pick.index(), "a corner 6 px closer clears the dead zone");
        GeometryAsserts.assertPosEquals(all[other], pick.screen(), 1.0e-4);
    }

    @Test
    void incumbentBehindTheCameraFallsBackToTheBestVisibleCorner() {
        // the camera sits inside the block's near face's half-space: the near
        // corners are behind it, only the far face projects — an incumbent
        // that went behind cannot be held
        Projection proj = ProjectionSimulator.at(0.5, 0.5, -5.6)
                .lookAt(0.5, 0.5, -6.6)
                .screen(400, 240)
                .build();
        BlockPos pos = new BlockPos(0, 0, -6);
        BlockOriginCursor.ScreenCorner stale = new BlockOriginCursor.ScreenCorner(5, new FloatPos(30, 30));

        BlockOriginCursor.ScreenCorner pick = BlockOriginCursor.screenAnchorCorner(pos, inflate, proj, 200, 120, stale);
        assertNotNull(pick);
        assertTrue(pick.index() < 4, "the pick must come from the projectable far face");
        GeometryAsserts.assertPosEquals(nearestProjection(proj, pos, 200, 120), pick.screen(), 1.0e-4);
    }

    @Test
    void noProjectableCornerReturnsNull() {
        // the camera looks away from the block: every corner is behind it
        Projection proj =
                ProjectionSimulator.at(0, 0, 0).lookAt(0, 0, 1).screen(400, 240).build();
        BlockPos pos = new BlockPos(0, 0, -6);
        assertNull(BlockOriginCursor.screenAnchorCorner(pos, inflate, proj, 200, 120, null));
        assertNull(BlockOriginCursor.screenAnchorCorner(
                pos, inflate, proj, 200, 120, new BlockOriginCursor.ScreenCorner(0, new FloatPos(30, 30))));
    }
}
