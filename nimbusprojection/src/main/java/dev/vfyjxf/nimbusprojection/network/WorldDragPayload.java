package dev.vfyjxf.nimbusprojection.network;

import dev.vfyjxf.cloudlib.api.network.payload.ServerPayloadInfo;
import dev.vfyjxf.cloudlib.api.network.payload.ServerboundPayload;
import dev.vfyjxf.cloudlib.api.ui.inworld.SplitPlan;
import dev.vfyjxf.nimbusprojection.api.section.SectionTarget;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.entity.player.Inventory;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.level.Level;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * Commit request for a {@code WorldDrag}: the client dragged items out of an
 * in-world inventory panel and released them over world positions.
 * <p>
 * The server is authoritative for everything: it re-reads the real stack in
 * the source, re-validates reach and target capabilities, computes the
 * shares itself via {@link SplitPlan} and shrinks the source by what was
 * actually inserted (partial insertions leave the remainder behind).
 * The client only ever sends slot + mode + positions — never an ItemStack —
 * so a forged packet can at worst ask for a legal-looking transfer.
 * <p>
 * {@link #source} selects where the stack comes from: {@code null} = the
 * player's own inventory ({@link #slot} is a vanilla inventory index);
 * non-null = the item-handler block at that position ({@link #slot} is an
 * {@code IItemHandler} slot) — e.g. dragging out of the container panel.
 */
public record WorldDragPayload(int slot, int mode, List<BlockPos> targets, Vec3 look, @Nullable SectionTarget source)
        implements ServerboundPayload {

    /** Split the whole source stack evenly across {@link #targets}. */
    public static final int insertEven = 0;
    /** One item per target while supply lasts. */
    public static final int insertOne = 1;
    /** Released over air — toss the whole stack along the look vector. */
    public static final int throwStack = 2;
    /** Released over air — toss a single item. */
    public static final int throwOne = 3;

    /** Max distance from the player to a commit target or source — generous reach bound. */
    private static final double reach = 12.0;

    private static final int maxTargets = DragTrailCap.capacity;

    /** Bare codec — also registered as a channel type so the payload can
     *  travel nested inside a {@link PanelChannelPayload}. */
    public static final StreamCodec<RegistryFriendlyByteBuf, WorldDragPayload> streamCodec =
            StreamCodec.ofMember(WorldDragPayload::encode, WorldDragPayload::decode);

    public static final ServerPayloadInfo<WorldDragPayload> info =
            NimbusPayloads.createServerInfo(streamCodec, "world_drag");

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return info.type();
    }

    private void encode(RegistryFriendlyByteBuf buf) {
        buf.writeVarInt(slot);
        buf.writeByte(mode);
        buf.writeVarInt(targets.size());
        for (BlockPos pos : targets) buf.writeBlockPos(pos);
        buf.writeVec3(look);
        buf.writeBoolean(source != null);
        if (source != null) SectionTarget.streamCodec.encode(buf, source);
    }

    private static WorldDragPayload decode(RegistryFriendlyByteBuf buf) {
        int slot = buf.readVarInt();
        int mode = buf.readByte();
        int n = Math.min(buf.readVarInt(), maxTargets * 4);
        List<BlockPos> targets = new ArrayList<>(n);
        for (int i = 0; i < n; i++) targets.add(buf.readBlockPos());
        Vec3 look = buf.readVec3();
        SectionTarget source = buf.readBoolean() ? SectionTarget.streamCodec.decode(buf) : null;
        return new WorldDragPayload(slot, mode, targets, look, source);
    }

    @Override
    public void handle(IPayloadContext context, ServerPlayer player) {
        Level level = player.level();
        Vec3 eye = player.getEyePosition();

        // resolve the source: player's inventory or a container's item handler
        ItemSource src = resolveSource(player, level, eye);
        if (src == null) return;
        ItemStack stack = src.read();
        if (stack.isEmpty()) return;

        switch (mode) {
            case insertEven, insertOne -> insert(player, level, eye, src, stack);
            case throwStack -> toss(player, src, stack.getCount());
            case throwOne -> toss(player, src, 1);
            default ->
                NimbusPayloads.log.warn(
                        "Bad world-drag mode {} from {}", mode, player.getName().getString());
        }
    }

    /**
     * Binds the payload's source to a live read/remove pair — the player's
     * inventory when {@link #source} is null, otherwise the item handler at
     * that block position. Null when the source isn't reachable/usable.
     */
    private @Nullable ItemSource resolveSource(ServerPlayer player, Level level, Vec3 eye) {
        if (source == null) {
            Inventory inv = player.getInventory();
            if (slot < 0 || slot >= inv.getContainerSize()) return null;
            return new ItemSource() {
                @Override
                public ItemStack read() {
                    return inv.getItem(slot);
                }

                @Override
                public ItemStack remove(int n) {
                    return inv.removeItem(slot, n);
                }
            };
        }
        Vec3 center = source.center(level);
        if (center == null || !center.closerThan(eye, reach)) return null;
        IItemHandler handler = source.itemHandler(level);
        if (handler == null || slot < 0 || slot >= handler.getSlots()) return null;
        return new ItemSource() {
            @Override
            public ItemStack read() {
                return handler.getStackInSlot(slot);
            }

            @Override
            public ItemStack remove(int n) {
                return handler.extractItem(slot, n, false);
            }
        };
    }

    private interface ItemSource {
        ItemStack read();

        ItemStack remove(int n);
    }

    private void insert(ServerPlayer player, Level level, Vec3 eye, ItemSource src, ItemStack stack) {
        List<BlockPos> valid = new ArrayList<>(targets.size());
        for (BlockPos pos : targets) {
            if (!pos.closerToCenterThan(eye, reach)) continue;
            if (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null) {
                valid.add(pos);
            }
        }
        if (valid.isEmpty()) return;

        int[] shares = mode == insertOne
                ? SplitPlan.oneEach(stack.getCount(), valid.size())
                : SplitPlan.evenly(stack.getCount(), valid.size());

        int removed = 0;
        for (int i = 0; i < valid.size(); i++) {
            int share = shares[i];
            if (share <= 0) continue;
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, valid.get(i), null);
            if (handler == null) continue;
            // never feed a slot back into itself — dragging out of a container
            // and dropping it on the same container would be a no-op anyway
            if (source != null && valid.get(i).equals(source.pos())) continue;
            ItemStack remainder = ItemHandlerHelper.insertItem(handler, stack.copyWithCount(share), false);
            removed += share - remainder.getCount();
        }
        if (removed > 0) src.remove(removed);
    }

    private void toss(ServerPlayer player, ItemSource src, int count) {
        ItemStack thrown = src.remove(count);
        if (thrown.isEmpty()) return;
        Vec3 eye = player.getEyePosition();
        ItemEntity entity = new ItemEntity(player.level(), eye.x, eye.y - 0.25, eye.z, thrown);
        entity.setDeltaMovement(look.normalize().scale(0.42).add(0, 0.1, 0));
        entity.setDefaultPickUpDelay();
        player.level().addFreshEntity(entity);
    }

    /** Trail size cap mirrored for decode — kept separate so this record stays codec-local. */
    private static final class DragTrailCap {
        static final int capacity = 8;
    }
}
