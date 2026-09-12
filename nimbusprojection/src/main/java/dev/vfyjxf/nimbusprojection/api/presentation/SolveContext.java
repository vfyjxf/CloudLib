package dev.vfyjxf.nimbusprojection.api.presentation;

import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanel;
import dev.vfyjxf.cloudlib.api.ui.inworld.Presentation;
import dev.vfyjxf.cloudlib.api.ui.inworld.Projection;
import net.minecraft.client.Camera;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.renderer.Rect2i;
import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * Per-frame input handed to {@link PresentationDriver#resolve}.
 *
 * @param presentation  the panel's presentation descriptor
 * @param panel         the live panel (size, engagement and widget state)
 * @param level         the client level — for free-space scans etc.
 * @param camera        the frame camera
 * @param projection    world ↔ screen conversion for this frame
 * @param anchorWorld   the anchor's resolved world position (may be stale
 *                      mid-frame — anchors resolve before drivers)
 * @param occupiedChrome screen rects already claimed by committed UI
 *                       chrome — drivers should avoid them where their
 *                       semantics allow
 * @param partialTick   frame interpolation factor
 */
public record SolveContext<P extends Presentation>(
        P presentation,
        InworldPanel panel,
        ClientLevel level,
        Camera camera,
        Projection projection,
        Vec3 anchorWorld,
        List<Rect2i> occupiedChrome,
        float partialTick
) {
}
