package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import net.minecraft.world.phys.Vec3;

import java.util.List;

/** A panel pinned to a fixed world position — it can never be relocated. */
public record FixedPosition(Vec3 position) implements PositionPolicy {

    @Override
    public List<PositionSlot> slots(IntentContext context, double panelWidth, double panelHeight) {
        return List.of(new PositionSlot("fixed", position, 0.0));
    }

    @Override
    public boolean canRelocate() {
        return false;
    }
}
