package dev.vfyjxf.cloudlib.network.payload;

import dev.vfyjxf.cloudlib.api.network.payload.ClientPayloadInfo;
import dev.vfyjxf.cloudlib.api.network.payload.ClientboundPayload;
import dev.vfyjxf.cloudlib.blockentity.SyncedBlockEntity;
import dev.vfyjxf.cloudlib.network.CloudlibPayloads;
import io.netty.buffer.Unpooled;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Merged sync updates of multiple block entities, sent once per dimension per tick by
 * {@link dev.vfyjxf.cloudlib.blockentity.BlockEntitySyncBatcher}. The client applies only entries
 * whose block entity it has loaded.
 */
public record BlockEntitySyncPacket(List<Entry> entries) implements ClientboundPayload {

    public record Entry(BlockPos pos, byte[] syncData) {
    }

    public static final ClientPayloadInfo<BlockEntitySyncPacket> info = CloudlibPayloads.createClientInfo(
            StreamCodec.ofMember(BlockEntitySyncPacket::write, BlockEntitySyncPacket::decode),
            "block_entity_sync"
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return info.type();
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeInt(entries.size());
        for (Entry entry : entries) {
            buf.writeBlockPos(entry.pos());
            buf.writeByteArray(entry.syncData());
        }
    }

    private static BlockEntitySyncPacket decode(RegistryFriendlyByteBuf buf) {
        int size = buf.readInt();
        List<Entry> entries = new ArrayList<>(size);
        for (int i = 0; i < size; i++) {
            entries.add(new Entry(buf.readBlockPos(), buf.readByteArray()));
        }
        return new BlockEntitySyncPacket(List.copyOf(entries));
    }

    @Override
    public void handle(IPayloadContext context, Player player) {
        for (Entry entry : entries) {
            if (player.level().getBlockEntity(entry.pos()) instanceof SyncedBlockEntity synced) {
                var buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(entry.syncData()), player.registryAccess(), ConnectionType.OTHER);
                synced.sync().receiveFromServer(buffer);
            }
        }
    }
}
