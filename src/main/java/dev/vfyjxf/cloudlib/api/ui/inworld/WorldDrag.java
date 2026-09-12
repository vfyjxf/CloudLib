package dev.vfyjxf.cloudlib.api.ui.inworld;

import net.minecraft.core.BlockPos;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

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
 * The source identifies where the real stack lives: {@code sourceContainer} =
 * the item-handler block at that position, {@code sourceEntity} = an entity's
 * item handler, both {@code null} = the player's own inventory
 * ({@link #sourceSlot} is then a vanilla inventory index). The server
 * re-reads and re-validates the source on commit — the preview never moves
 * items by itself.
 */
public record WorldDrag(
        ItemStack carried,
        int sourceSlot,
        int button,
        @Nullable BlockPos sourceContainer,
        @Nullable UUID sourceEntity) {

    /** Player-inventory source — the common case. */
    public WorldDrag(ItemStack carried, int sourceSlot, int button) {
        this(carried, sourceSlot, button, null, null);
    }

    /** Container-block source. */
    public WorldDrag(ItemStack carried, int sourceSlot, int button, @Nullable BlockPos sourceContainer) {
        this(carried, sourceSlot, button, sourceContainer, null);
    }

    /** Entity-inventory source — the stack lives in an entity's item handler. */
    public static WorldDrag fromEntity(ItemStack carried, int sourceSlot, int button, UUID entity) {
        return new WorldDrag(carried, sourceSlot, button, null, entity);
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
