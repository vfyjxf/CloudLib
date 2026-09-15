package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import net.minecraft.world.phys.Vec3;

import java.util.List;

/** A panel following a fixed offset in its source's frame. */
public record FollowPosition(Vec3 localOffset) implements PositionPolicy {

    @Override
    public List<PositionSlot> slots(IntentContext context, double panelWidth, double panelHeight) {
        return List.of(new PositionSlot("follow", context.source().frame().at(localOffset), 0.0));
    }

    @Override
    public boolean canRelocate() {
        return false;
    }
}
