package dev.vfyjxf.cloudlib.network.payload;

import dev.vfyjxf.cloudlib.api.network.payload.ServerPayloadInfo;
import dev.vfyjxf.cloudlib.api.network.payload.ServerboundPayload;
import dev.vfyjxf.cloudlib.blockentity.SyncedBlockEntity;
import dev.vfyjxf.cloudlib.network.CloudlibPayloads;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/**
 * Carries queued {@code ReversedOnly} values (in-world UI actions) from a client
 * to the server-side block entity. The counterpart of
 * {@link BlockEntitySyncPacket} for the client → server direction.
 */
public record BlockEntityReversedPacket(BlockPos pos, byte[] syncData) implements ServerboundPayload {

    public static final ServerPayloadInfo<BlockEntityReversedPacket> info = CloudlibPayloads.createServerInfo(
        StreamCodec.ofMember(BlockEntityReversedPacket::encode, BlockEntityReversedPacket::decode),
        "block_entity_reversed"
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return info.type();
    }

    private void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
        buf.writeByteArray(syncData);
    }

    private static BlockEntityReversedPacket decode(RegistryFriendlyByteBuf buf) {
        return new BlockEntityReversedPacket(buf.readBlockPos(), buf.readByteArray());
    }

    @Override
    public void handle(IPayloadContext context, ServerPlayer player) {
        // basic sanity: the sender must be near the target — in-world panels can
        // only be presented within a small radius of their anchor anyway
        if (!pos.closerToCenterThan(player.position(), 64.0)) {
            CloudlibPayloads.log.warn(
                "Rejected reversed data for far-away block entity {} from {}",
                pos,
                player.getName().getString()
            );
            return;
        }
        if (player.level().getBlockEntity(pos) instanceof SyncedBlockEntity synced) {
            var buffer = new RegistryFriendlyByteBuf(
                Unpooled.wrappedBuffer(syncData),
                player.registryAccess(),
                ConnectionType.OTHER
            );
            synced.sync().receiveFromClient(buffer);
        }
    }
}
