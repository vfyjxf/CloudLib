package dev.vfyjxf.nimbusprojection.api.presentation;

import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Size;
import net.minecraft.world.phys.Vec3;

/**
 * Where a panel surface sits this frame — the driver's uniform output.
 * The runtime reads geometry from here for rendering, occlusion checks
 * and chrome avoidance regardless of which driver produced it.
 */
public sealed interface PanelGeometry {

    /**
     * A world-space quad (face-mounted or holographic surfaces).
     *
     * @param center        quad center in world space
     * @param axisU         unit vector along the quad's horizontal axis
     * @param axisV         unit vector along the quad's vertical axis
     * @param pixelsPerBlock gui pixels per block — the quad's world size is
     *                       the panel's pixel size divided by this
     */
    record WorldQuad(Vec3 center, Vec3 axisU, Vec3 axisV, double pixelsPerBlock) implements PanelGeometry {
    }

    /**
     * A screen-space rect in gui-scaled pixels (floating, follow, docked
     * and flattened surfaces).
     */
    record ScreenRect(Pos pos, Size size) implements PanelGeometry {
    }

}
