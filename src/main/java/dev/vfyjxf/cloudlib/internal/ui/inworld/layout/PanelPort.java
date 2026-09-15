package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * One attach port on a panel edge: {@code side} 0..3 (left, right, top,
 * bottom), {@code slot} 0..2 along the edge, the screen-space attach
 * {@code point} and outward {@code gate}, and for world panels their
 * world-space counterparts plus the projection depths.
 */
public record PanelPort(
        int side,
        int slot,
        GuiVec point,
        GuiVec gate,
        @Nullable Vec3 worldPoint,
        @Nullable Vec3 worldGate,
        double depth,
        double gateDepth) {

    public String key() {
        return side + ":" + slot;
    }
}
