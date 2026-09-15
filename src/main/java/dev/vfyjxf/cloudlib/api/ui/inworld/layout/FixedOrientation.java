package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import net.minecraft.world.phys.Vec3;

/** A panel with a constant world-space orientation. */
public record FixedOrientation(PanelBasis value) implements OrientationPolicy {

    @Override
    public PanelBasis basis(IntentContext context, Vec3 panelCenter) {
        return value;
    }
}
