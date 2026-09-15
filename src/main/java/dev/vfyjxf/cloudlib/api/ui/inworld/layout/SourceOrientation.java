package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import net.minecraft.world.phys.Vec3;

/**
 * The panel rides the source surface's orientation — a local basis
 * composed onto the source frame. Required for {@link MountedPosition}.
 */
public record SourceOrientation(PanelBasis local) implements OrientationPolicy {

    @Override
    public PanelBasis basis(IntentContext context, Vec3 panelCenter) {
        return context.source().frame().basis().compose(local);
    }
}
