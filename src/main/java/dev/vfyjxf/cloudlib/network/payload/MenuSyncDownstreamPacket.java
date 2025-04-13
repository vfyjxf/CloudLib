package dev.vfyjxf.cloudlib.network.payload;

import dev.vfyjxf.cloudlib.api.network.payload.ClientPayloadInfo;
import dev.vfyjxf.cloudlib.api.network.payload.ClientboundPayload;
import dev.vfyjxf.cloudlib.network.CloudlibNetworkPayloads;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Consumer;

public record MenuSyncDownstreamPacket(int containerId, byte[] syncData) implements ClientboundPayload {

    public static final ClientPayloadInfo<MenuSyncDownstreamPacket> INFO = CloudlibNetworkPayloads.createClientInfo(
            StreamCodec.ofMember(
                    MenuSyncDownstreamPacket::write,
                    MenuSyncDownstreamPacket::decode
            ),
            "menu_sync_downstream"
    );

    public MenuSyncDownstreamPacket(int containerId, Consumer<RegistryFriendlyByteBuf> writer, RegistryAccess registryAccess) {
        this(containerId, writeData(writer, registryAccess));
    }

    private static byte[] writeData(Consumer<RegistryFriendlyByteBuf> writer, RegistryAccess registryAccess) {
        var buffer = new RegistryFriendlyByteBuf(Unpooled.buffer(), registryAccess, ConnectionType.OTHER);
        writer.accept(buffer);
        byte[] result = new byte[buffer.readableBytes()];
        buffer.readBytes(result);
        return result;
    }


    @Override
    public Type<? extends CustomPacketPayload> type() {
        return INFO.type();
    }

    private void write(RegistryFriendlyByteBuf buf) {
        buf.writeInt(containerId);
        buf.writeBytes(syncData);
    }

    private static MenuSyncDownstreamPacket decode(RegistryFriendlyByteBuf buf) {
        return new MenuSyncDownstreamPacket(buf.readInt(), buf.readByteArray());
    }

    @Override
    public void handle(IPayloadContext context, Player player) {

    }
}
