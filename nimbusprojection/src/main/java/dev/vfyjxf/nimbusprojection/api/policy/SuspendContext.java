package dev.vfyjxf.nimbusprojection.api.policy;

import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanel;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;

/**
 * Per-tick suspend-evaluation input handed to a {@link SuspendPolicy}.
 *
 * @param level            the client level
 * @param player           the local player
 * @param panel            the panel being evaluated
 * @param anchorAlive      whether the panel's anchor still resolves
 * @param screenOpen       whether a real {@code Screen} (non-overlay) is open
 * @param paused           whether the game is paused
 * @param dimensionChanged whether the player changed dimension since the
 *                         panel was created
 */
public record SuspendContext(
        ClientLevel level,
        LocalPlayer player,
        InworldPanel panel,
        boolean anchorAlive,
        boolean screenOpen,
        boolean paused,
        boolean dimensionChanged
) {
}
