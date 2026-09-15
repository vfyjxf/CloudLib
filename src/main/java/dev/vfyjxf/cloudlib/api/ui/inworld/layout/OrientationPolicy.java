package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import net.minecraft.world.phys.Vec3;

/** Picks the panel's world-space orientation at {@code panelCenter}. */
@FunctionalInterface
public interface OrientationPolicy {

    PanelBasis basis(IntentContext context, Vec3 panelCenter);
}
