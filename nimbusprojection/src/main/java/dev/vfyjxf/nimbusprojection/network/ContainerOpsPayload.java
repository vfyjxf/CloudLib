package dev.vfyjxf.nimbusprojection.network;

import dev.vfyjxf.cloudlib.api.network.payload.ServerboundPayload;
import dev.vfyjxf.nimbusprojection.api.section.SectionTarget;
import dev.vfyjxf.nimbusprojection.feature.container.section.SectionTypes;
import dev.vfyjxf.nimbusprojection.internal.section.SectionProviders;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
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
 *   <li>{@link #extract} — pull {@code count} (or the whole stack when -1)
 *       out of {@code container}[{@code slot}] into the player inventory</li>
 *   <li>{@link #insert} — push the player's inventory stack at
 *       {@code slot} (an {@link PlayerMainInvWrapper} index, -1 = held item)
 *       into {@code container}</li>
 *   <li>{@link #extractAll} — drain the whole handler into the player</li>
 *   <li>{@link #insertAll} — dump the player's main inventory into the handler</li>
 * </ul>
 */
public record ContainerOpsPayload(String section, int op, SectionTarget target, int slot, int count)
        implements ServerboundPayload {

    public static final int extract = 0;
    public static final int insert = 1;
    public static final int extractAll = 2;
    public static final int insertAll = 3;

    /** Generous bound — a legit client only sends ops while inside its focus reach. */
    private static final double reach = 12.0;

    public static final StreamCodec<RegistryFriendlyByteBuf, ContainerOpsPayload> streamCodec =
            StreamCodec.ofMember(ContainerOpsPayload::encode, ContainerOpsPayload::decode);

    public static final Type<ContainerOpsPayload> type =
            new Type<>(ResourceLocation.fromNamespaceAndPath("nimbusprojection", "container_ops"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return type;
    }

    private void encode(RegistryFriendlyByteBuf buf) {
        buf.writeUtf(section);
        buf.writeByte(op);
        SectionTarget.streamCodec.encode(buf, target);
        buf.writeVarInt(slot);
        buf.writeVarInt(count);
    }

    private static ContainerOpsPayload decode(RegistryFriendlyByteBuf buf) {
        return new ContainerOpsPayload(
                buf.readUtf(),
                buf.readByte(),
                SectionTarget.streamCodec.decode(buf),
                buf.readVarInt(),
                buf.readVarInt());
    }

    @Override
    public void handle(IPayloadContext context, ServerPlayer player) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition();
        Vec3 center = target.center(level);
        if (center == null || !center.closerThan(eye, reach)) return;
        // ops address a section, not the client's layout: the builtin item
        // ops only ever target the unsided handler's first section
        if (!SectionProviders.idOf(SectionTypes.item, 0).equals(section)) return;
        IItemHandler handler = target.itemHandler(level);
        if (handler == null) return;

        switch (op) {
            case extract -> extract(player, handler);
            case insert -> insert(player, handler);
            case extractAll -> {
                for (int s = 0; s < handler.getSlots(); s++) {
                    moveToPlayer(player, handler, s, -1);
                }
            }
            case insertAll -> {
                IItemHandler inv = new PlayerMainInvWrapper(player.getInventory());
                for (int s = 0; s < inv.getSlots(); s++) {
                    ItemStack moved = ItemHandlerHelper.insertItem(
                            handler, inv.getStackInSlot(s).copy(), false);
                    int took = inv.getStackInSlot(s).getCount() - moved.getCount();
                    if (took > 0) inv.extractItem(s, took, false);
                }
            }
            default ->
                NimbusPayloads.log.warn(
                        "Bad container op {} from {}", op, player.getName().getString());
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
        // simulate the extraction first — the handler owns the permission
        ItemStack pulled = handler.extractItem(s, take, true);
        if (pulled.isEmpty()) return;
        ItemStack real = handler.extractItem(s, pulled.getCount(), false);
        player.getInventory().placeItemBackInInventory(real);
    }

    private void insert(ServerPlayer player, IItemHandler handler) {
        ItemStack held = slot >= 0 ? player.getInventory().getItem(slot) : player.getMainHandItem();
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
