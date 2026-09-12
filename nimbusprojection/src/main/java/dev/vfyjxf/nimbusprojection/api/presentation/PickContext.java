package dev.vfyjxf.nimbusprojection.api.presentation;

import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanel;
import dev.vfyjxf.cloudlib.api.ui.inworld.Presentation;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Per-frame input handed to {@link PresentationDriver#pick}.
 *
 * @param presentation the panel's presentation descriptor
 * @param panel        the live panel
 * @param geometry     this frame's resolved {@link PanelGeometry}
 * @param rayOrigin    view-ray origin (eye position) — world-mode picking
 * @param rayDirection normalized view-ray direction — world-mode picking
 * @param screenPoint  pointer position in gui pixels — inspect-mode picking;
 *                     null in world mode
 * @param partialTick  frame interpolation factor
 */
public record PickContext<P extends Presentation>(
        P presentation,
        InworldPanel panel,
        PanelGeometry geometry,
        Vec3 rayOrigin,
        Vec3 rayDirection,
        @Nullable Pos screenPoint,
        float partialTick) {}
