package dev.vfyjxf.cloudlib.api.ui.inworld;

import dev.vfyjxf.cloudlib.api.network.expose.ExposeManagement;
import dev.vfyjxf.cloudlib.blockentity.SyncedBlockEntity;
import net.minecraft.client.multiplayer.ClientLevel;
import net.minecraft.client.player.LocalPlayer;
import net.minecraft.world.level.block.entity.BlockEntity;
import org.jetbrains.annotations.Nullable;

/**
 * The context handed to a panel's content factory when the panel is created.
 * <p>
 * For block-anchored panels this is the place to grab the backing
 * {@link BlockEntity} — and through {@link #sync()} its live
 * {@link ExposeManagement} when the block entity implements
 * {@link SyncedBlockEntity} (see {@code BasicSyncedBlockEntity}).
 * Expose values received from the server can be pushed straight into widget
 * state via {@code expose.whenReceive(...)} or read each tick via the backing
 * {@code Handle}.
 */
public record InworldPanelContext(
        ClientLevel level,
        LocalPlayer player,
        InworldPanel panel,
        @Nullable PanelChannel channel
) {

    public InworldPanelContext(ClientLevel level, LocalPlayer player, InworldPanel panel) {
        this(level, player, panel, null);
    }

    /** The block entity at the panel's anchor, if block-bound. */
    public @Nullable BlockEntity blockEntity() {
        return panel.blockEntity(level);
    }

    /** Typed block entity access. */
    public <T extends BlockEntity> @Nullable T blockEntity(Class<T> type) {
        return panel.blockEntity(level, type);
    }

    /**
     * The block entity's live sync channel when it implements
     * {@link SyncedBlockEntity}, else null.
     */
    public @Nullable dev.vfyjxf.cloudlib.blockentity.BlockEntitySync sync() {
        return blockEntity() instanceof SyncedBlockEntity synced ? synced.sync() : null;
    }
}
