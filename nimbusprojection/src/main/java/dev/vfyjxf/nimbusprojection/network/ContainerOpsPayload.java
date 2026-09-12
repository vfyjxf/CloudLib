package dev.vfyjxf.nimbusprojection.network;

import dev.vfyjxf.cloudlib.api.network.payload.ServerboundPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.items.wrapper.PlayerMainInvWrapper;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Server-authoritative container operations — the quick-action half of the
 * container feature (drags go through {@link WorldDragPayload}).
 * <p>
 * The client reports intent only (op + positions + slot indices); the server
 * re-resolves every capability, re-checks reach and lets the handler's own
 * {@code extractItem}/{@code insertItem} decide what actually moves. A forged
 * or stale request can at most attempt a legal transfer.
 * <ul>
 *   <li>{@link #EXTRACT} — pull {@code count} (or the whole stack when -1)
 *       out of {@code container}[{@code slot}] into the player inventory</li>
 *   <li>{@link #INSERT} — push the player's inventory stack at
 *       {@code slot} (an {@link PlayerMainInvWrapper} index, -1 = held item)
 *       into {@code container}</li>
 *   <li>{@link #EXTRACT_ALL} — drain the whole handler into the player</li>
 *   <li>{@link #INSERT_ALL} — dump the player's main inventory into the handler</li>
 * </ul>
 */
public record ContainerOpsPayload(
        int op,
        BlockPos container,
        int slot,
        int count
) implements ServerboundPayload {

    public static final int EXTRACT = 0;
    public static final int INSERT = 1;
    public static final int EXTRACT_ALL = 2;
    public static final int INSERT_ALL = 3;

    /** Generous bound — a legit client only sends ops while inside its focus reach. */
    private static final double REACH = 12.0;

    public static final StreamCodec<RegistryFriendlyByteBuf, ContainerOpsPayload> STREAM_CODEC =
            StreamCodec.ofMember(ContainerOpsPayload::encode, ContainerOpsPayload::decode);

    public static final Type<ContainerOpsPayload> TYPE =
            new Type<>(net.minecraft.resources.ResourceLocation.fromNamespaceAndPath("nimbusprojection", "container_ops"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    private void encode(RegistryFriendlyByteBuf buf) {
        buf.writeByte(op);
        buf.writeBlockPos(container);
        buf.writeVarInt(slot);
        buf.writeVarInt(count);
    }

    private static ContainerOpsPayload decode(RegistryFriendlyByteBuf buf) {
        return new ContainerOpsPayload(buf.readByte(), buf.readBlockPos(), buf.readVarInt(), buf.readVarInt());
    }

    @Override
    public void handle(IPayloadContext context, ServerPlayer player) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        if (!container.closerToCenterThan(eye, REACH)) return;
        IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, container, null);
        if (handler == null) return;

        switch (op) {
            case EXTRACT -> extract(player, handler);
            case INSERT -> insert(player, handler);
            case EXTRACT_ALL -> {
                for (int s = 0; s < handler.getSlots(); s++) {
                    moveToPlayer(player, handler, s, -1);
                }
            }
            case INSERT_ALL -> {
                IItemHandler inv = new PlayerMainInvWrapper(player.getInventory());
                for (int s = 0; s < inv.getSlots(); s++) {
                    ItemStack moved = ItemHandlerHelper.insertItem(handler, inv.getStackInSlot(s).copy(), false);
                    int took = inv.getStackInSlot(s).getCount() - moved.getCount();
                    if (took > 0) inv.extractItem(s, took, false);
                }
            }
            default -> NimbusPayloads.log.warn("Bad container op {} from {}", op, player.getName().getString());
        }
    }

    private void extract(ServerPlayer player, IItemHandler handler) {
        if (slot < 0 || slot >= handler.getSlots()) return;
        moveToPlayer(player, handler, slot, count);
    }

    private void moveToPlayer(ServerPlayer player, IItemHandler handler, int s, int wanted) {
        ItemStack stack = handler.getStackInSlot(s);
        if (stack.isEmpty()) return;
        int take = wanted < 0 ? stack.getCount() : Math.min(wanted, stack.getCount());
        //simulate the extraction first — the handler owns the permission
        ItemStack pulled = handler.extractItem(s, take, true);
        if (pulled.isEmpty()) return;
        ItemStack real = handler.extractItem(s, pulled.getCount(), false);
        player.getInventory().placeItemBackInInventory(real);
    }

    private void insert(ServerPlayer player, IItemHandler handler) {
        ItemStack held = slot >= 0
                ? player.getInventory().getItem(slot)
                : player.getMainHandItem();
        if (held.isEmpty()) return;
        int offer = count < 0 ? held.getCount() : Math.min(count, held.getCount());
        ItemStack remainder = ItemHandlerHelper.insertItem(handler, held.copyWithCount(offer), false);
        int moved = offer - remainder.getCount();
        if (moved <= 0) return;
        if (slot >= 0) {
            player.getInventory().removeItem(slot, moved);
        } else {
            held.shrink(moved);
        }
    }
}
