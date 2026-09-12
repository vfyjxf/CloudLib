package dev.vfyjxf.cloudlib.api.ui.inworld;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

/**
 * A drag operation that leaves an in-world panel and enters the world: the
 * player is holding {@link #carried} (a client-side preview copy — the real
 * stack still lives in the source and is only ever removed by the server)
 * and may aim it at world objects such as containers.
 * <p>
 * Created by {@link WorldDraggable#beginWorldDrag}. While a drag is active the
 * runtime raycasts the player's view every frame, collects the crossed
 * container positions into a trail and, on release, asks the server to commit
 * the transfer. Overriding {@link #commitTargets} lets a provider narrow or
 * re-order the trail (e.g. only the last target); {@link #button} follows
 * vanilla pickup semantics: left = whole stack, right = single items.
 * <p>
 * {@link #sourceContainer} identifies where the real stack lives:
 * {@code null} = the player's own inventory ({@link #sourceSlot} is a vanilla
 * inventory index); non-null = the item-handler block at that position
 * ({@link #sourceSlot} is an {@code IItemHandler} slot index). The server
 * re-reads and re-validates the source on commit — the preview never moves
 * items by itself.
 */
public record WorldDrag(ItemStack carried, int sourceSlot, int button, @Nullable BlockPos sourceContainer) {

    /** Player-inventory source — the common case. */
    public WorldDrag(ItemStack carried, int sourceSlot, int button) {
        this(carried, sourceSlot, button, null);
    }

    /** Vanilla semantics: left button drags the whole stack, right drags singles. */
    public boolean wholeStack() {
        return button == 0;
    }

    /**
     * Narrows/reorders the collected trail before commit. Default: the trail
     * as gathered (every crossed container, earliest first, deduplicated).
     */
    public java.util.List<BlockPos> commitTargets(java.util.List<BlockPos> trail, InworldPanelContext ctx) {
        return trail;
    }
}
