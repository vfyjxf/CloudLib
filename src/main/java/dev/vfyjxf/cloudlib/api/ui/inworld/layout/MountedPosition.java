package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A panel mounted flush on the source surface at {@code localCenter},
 * pushed {@code offset} units along the surface normal. Only offered when
 * the panel's world extent fits inside {@code patchWidth} ×
 * {@code patchHeight}. Mounted requests must use
 * {@link SourceOrientation} (directly or under {@link ReadableBothSides}).
 */
public record MountedPosition(Vec3 localCenter, double patchWidth, double patchHeight, double offset)
        implements PositionPolicy {

    @Override
    public List<PositionSlot> slots(IntentContext context, double panelWidth, double panelHeight) {
        return !(panelWidth > patchWidth + 1.0E-8) && !(panelHeight > patchHeight + 1.0E-8)
                ? List.of(new PositionSlot(
                        "mounted", context.source().frame().at(localCenter.add(new Vec3(0.0, 0.0, offset))), 0.0))
                : List.of();
    }

    @Override
    public boolean canRelocate() {
        return false;
    }

    @Override
    public boolean mounted() {
        return true;
    }
}
