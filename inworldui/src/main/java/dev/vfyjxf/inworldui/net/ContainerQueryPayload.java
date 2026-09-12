package dev.vfyjxf.inworldui.net;

import dev.vfyjxf.cloudlib.api.network.payload.ServerPayloadInfo;
import dev.vfyjxf.cloudlib.api.network.payload.ServerboundPayload;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.capabilities.Capabilities;
import net.neoforged.neoforge.items.IItemHandler;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Client→server request for a container's live contents. Vanilla blocks never
 * replicate their inventory to clients — the client-side block entity has the
 * capability but it is always empty — so the in-world inventory panel asks the
 * server for a snapshot, and the server answers with
 * {@link ContainerContentsPayload}.
 * <p>
 * The reply carries plain display data; reads stay server-authoritative —
 * drags re-resolve the real handler at commit time via {@code WorldDragPayload}.
 */
public record ContainerQueryPayload(BlockPos pos) implements ServerboundPayload {

    /** Reach bound for a query — same scale as drag commits. */
    private static final double REACH = 16.0;
    /** Never ship more slots than this — a malformed block shouldn't spam the wire. */
    private static final int MAX_SLOTS = 512;

    public static final ServerPayloadInfo<ContainerQueryPayload> info = InworldPayloads.createServerInfo(
            StreamCodec.ofMember(ContainerQueryPayload::encode, ContainerQueryPayload::decode),
            "container_query"
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return info.type();
    }

    private void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    private static ContainerQueryPayload decode(RegistryFriendlyByteBuf buf) {
        return new ContainerQueryPayload(buf.readBlockPos());
    }

    @Override
    public void handle(IPayloadContext context, ServerPlayer player) {
        if (!pos.closerToCenterThan(player.getEyePosition(), REACH)) return;
        IItemHandler handler = player.level().getCapability(Capabilities.ItemHandler.BLOCK, pos, null);
        if (handler == null) return;
        int slots = Math.min(handler.getSlots(), MAX_SLOTS);
        List<ItemStack> stacks = new ArrayList<>(slots);
        for (int i = 0; i < slots; i++) {
            stacks.add(handler.getStackInSlot(i).copy());
        }
        context.reply(new ContainerContentsPayload(pos, stacks));
    }
}
