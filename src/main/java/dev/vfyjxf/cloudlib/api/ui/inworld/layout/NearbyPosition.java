package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import net.minecraft.world.phys.Vec3;

import java.util.ArrayList;
import java.util.List;

/**
 * Candidate positions scattered around a local origin of the source
 * frame — six horizontal/vertical rings at {@code radius} plus panel
 * clearance, each tried at two depth offsets. When
 * {@code cameraRelative} is set the offsets are taken in view space
 * instead of source space.
 */
public record NearbyPosition(Vec3 localOrigin, boolean cameraRelative, double radius) implements PositionPolicy {

    @Override
    public List<PositionSlot> slots(IntentContext context, double panelWidth, double panelHeight) {
        Vec3 origin = context.source().frame().at(localOrigin);
        PanelBasis basis = cameraRelative
                ? context.frame().viewBasis()
                : context.source().frame().basis();
        List<PositionSlot> slots = new ArrayList<>();
        Vec3[] offsets = {
            new Vec3(radius + panelWidth * 0.5, 0.25, 0.0),
            new Vec3(-radius - panelWidth * 0.5, 0.25, 0.0),
            new Vec3(0.0, radius + panelHeight * 0.5, 0.0),
            new Vec3(0.0, -radius - panelHeight * 0.5, 0.0),
            new Vec3(radius + panelWidth * 0.5, radius + panelHeight * 0.5, 0.0),
            new Vec3(-radius - panelWidth * 0.5, radius + panelHeight * 0.5, 0.0)
        };
        for (int ring = 0; ring < offsets.length; ring++) {
            for (int depth = 0; depth < 2; depth++) {
                slots.add(new PositionSlot(
                        "near:" + ring + ":" + depth,
                        origin.add(basis.apply(offsets[ring].add(new Vec3(0.0, 0.0, depth * 0.35)))),
                        ring * 7 + depth * 12));
            }
        }
        return slots;
    }
}
