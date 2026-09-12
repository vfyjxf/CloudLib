package dev.vfyjxf.inworldui.net;

import dev.vfyjxf.cloudlib.api.network.payload.ServerPayloadInfo;
import dev.vfyjxf.cloudlib.api.network.payload.ServerboundPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.entity.item.ItemEntity;
import net.minecraft.world.item.ItemStack;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.items.ItemHandlerHelper;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Commit request for a {@code WorldDrag}: the client dragged items out of an
 * in-world inventory panel and released them over world positions.
 * <p>
 * The server is authoritative for everything: it re-reads the real stack in
 * {@link #slot}, re-validates reach and target capabilities, computes the
 * shares itself via {@link SplitPlan} and shrinks the source by what was
 * actually inserted (partial insertions leave the remainder in the slot).
 * The client only ever sends slot + mode + positions — never an ItemStack —
 * so a forged packet can at worst ask for a legal-looking transfer.
 */
public record WorldDragPayload(
        int slot,
        int mode,
        List<BlockPos> targets,
        Vec3 look
) implements ServerboundPayload {

    /** Split the whole source stack evenly across {@link #targets}. */
    public static final int INSERT_EVEN = 0;
    /** One item per target while supply lasts. */
    public static final int INSERT_ONE = 1;
    /** Released over air — toss the whole stack along the look vector. */
    public static final int THROW_STACK = 2;
    /** Released over air — toss a single item. */
    public static final int THROW_ONE = 3;

    /** Max distance from the player to a commit target — generous reach bound. */
    private static final double REACH = 12.0;
    private static final int MAX_TARGETS = DragTrailCap.CAPACITY;

    public static final ServerPayloadInfo<WorldDragPayload> info = InworldPayloads.createServerInfo(
            StreamCodec.ofMember(WorldDragPayload::encode, WorldDragPayload::decode),
            "world_drag"
    );

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
    }

    private static WorldDragPayload decode(RegistryFriendlyByteBuf buf) {
        int slot = buf.readVarInt();
        int mode = buf.readByte();
        int n = Math.min(buf.readVarInt(), MAX_TARGETS * 4);
        List<BlockPos> targets = new ArrayList<>(n);
        for (int i = 0; i < n; i++) targets.add(buf.readBlockPos());
        return new WorldDragPayload(slot, mode, targets, buf.readVec3());
    }

    @Override
    public void handle(IPayloadContext context, ServerPlayer player) {
        var inv = player.getInventory();
        if (slot < 0 || slot >= inv.getContainerSize()) return;
        ItemStack source = inv.getItem(slot);
        if (source.isEmpty()) return;

        switch (mode) {
            case INSERT_EVEN, INSERT_ONE -> insert(player, source);
            case THROW_STACK -> toss(player, inv, source.getCount());
            case THROW_ONE -> toss(player, inv, 1);
            default -> InworldPayloads.log.warn("Bad world-drag mode {} from {}", mode, player.getName().getString());
        }
    }

    private void insert(ServerPlayer player, ItemStack source) {
        var level = player.level();
        Vec3 eye = player.getEyePosition();
        List<BlockPos> valid = new ArrayList<>(targets.size());
        for (BlockPos pos : targets) {
            if (!pos.closerToCenterThan(eye, REACH)) continue;
            if (level.getCapability(Capabilities.ItemHandler.BLOCK, pos, null) != null) {
                valid.add(pos);
            }
        }
        if (valid.isEmpty()) return;

        int[] shares = mode == INSERT_ONE
                ? SplitPlan.oneEach(source.getCount(), valid.size())
                : SplitPlan.evenly(source.getCount(), valid.size());

        int removed = 0;
        for (int i = 0; i < valid.size(); i++) {
            int share = shares[i];
            if (share <= 0) continue;
            IItemHandler handler = level.getCapability(Capabilities.ItemHandler.BLOCK, valid.get(i), null);
            if (handler == null) continue;
            ItemStack remainder = ItemHandlerHelper.insertItem(handler, source.copyWithCount(share), false);
            removed += share - remainder.getCount();
        }
        source.shrink(removed);
    }

    private void toss(ServerPlayer player, net.minecraft.world.entity.player.Inventory inv, int count) {
        ItemStack thrown = inv.removeItem(slot, count);
        if (thrown.isEmpty()) return;
        Vec3 eye = player.getEyePosition();
        ItemEntity entity = new ItemEntity(player.level(), eye.x, eye.y - 0.25, eye.z, thrown);
        entity.setDeltaMovement(look.normalize().scale(0.42).add(0, 0.1, 0));
        entity.setDefaultPickUpDelay();
        player.level().addFreshEntity(entity);
    }

    /** Trail size cap mirrored for decode — kept separate so this record stays codec-local. */
    private static final class DragTrailCap {
        static final int CAPACITY = 8;
    }
}
