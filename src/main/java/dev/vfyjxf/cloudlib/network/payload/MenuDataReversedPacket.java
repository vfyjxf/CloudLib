package dev.vfyjxf.cloudlib.network.payload;

import dev.vfyjxf.cloudlib.api.network.payload.ServerPayloadInfo;
import dev.vfyjxf.cloudlib.api.network.payload.ServerboundPayload;
import dev.vfyjxf.cloudlib.api.ui.sync.menu.BasicMenu;
import dev.vfyjxf.cloudlib.network.CloudlibNetworkPayloads;
import io.netty.buffer.Unpooled;
import net.minecraft.core.RegistryAccess;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.connection.ConnectionType;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.function.Consumer;

public record MenuDataReversedPacket(int containerId, byte[] syncData) implements ServerboundPayload {

    public static final ServerPayloadInfo<MenuDataReversedPacket> INFO = CloudlibNetworkPayloads.createServerInfo(
            StreamCodec.ofMember(
                    MenuDataReversedPacket::encode,
                    MenuDataReversedPacket::decode
            ),
            "menu_sync_reversed"
    );

    public MenuDataReversedPacket(int containerId, Consumer<RegistryFriendlyByteBuf> writer, RegistryAccess registryAccess) {
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

    private void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(containerId);
        buf.writeByteArray(syncData);
    }

    private static MenuDataReversedPacket decode(RegistryFriendlyByteBuf buf) {
        return new MenuDataReversedPacket(buf.readInt(), buf.readByteArray());
    }

    @Override
    public void handle(IPayloadContext context, ServerPlayer player) {
        if (player.containerMenu instanceof BasicMenu<?> menu && menu.containerId == containerId) {
            var buffer = new RegistryFriendlyByteBuf(Unpooled.wrappedBuffer(syncData), player.registryAccess(), ConnectionType.OTHER);
            menu.receiveFromClient(buffer);
        } else {
            CloudlibNetworkPayloads.log.warn("Received menu data reversed packet for non-matching menu: {} != {}", containerId, player.containerMenu.containerId);
        }
    }

}
