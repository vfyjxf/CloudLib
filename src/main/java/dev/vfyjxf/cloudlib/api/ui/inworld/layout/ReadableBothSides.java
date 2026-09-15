package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import net.minecraft.world.phys.Vec3;

/**
 * Wraps another policy so the panel is always readable from the camera —
 * when the base orientation faces away, right and normal are flipped so
 * the surface fronts the viewer.
 */
public record ReadableBothSides(OrientationPolicy base) implements OrientationPolicy {

    @Override
    public PanelBasis basis(IntentContext context, Vec3 panelCenter) {
        PanelBasis basis = base.basis(context, panelCenter);
        return basis.normal().dot(context.frame().camera().eye().subtract(panelCenter)) >= 0.0
                ? basis
                : new PanelBasis(
                        basis.right().scale(-1.0), basis.up(), basis.normal().scale(-1.0));
    }
}
