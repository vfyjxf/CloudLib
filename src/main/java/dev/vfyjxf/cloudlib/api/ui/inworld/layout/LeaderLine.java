package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import net.minecraft.world.phys.Vec3;

import java.util.List;

/**
 * A routed leader line from a panel back to its source. {@code screen} is
 * the gui-px polyline used when the owning panel is a screen panel;
 * {@code world} is the world-space polyline used when it is a world panel.
 * {@code status} is {@code "ok"} for a routed line or {@code "hidden"}
 * when routing was declined/failed.
 */
public record LeaderLine(
        String visualId,
        String sourceId,
        List<String> representedIds,
        List<GuiVec> screen,
        List<Vec3> world,
        String status,
        Depth depth) {

    public LeaderLine {
        screen = List.copyOf(screen);
        world = List.copyOf(world);
        representedIds = List.copyOf(representedIds);
    }
}
