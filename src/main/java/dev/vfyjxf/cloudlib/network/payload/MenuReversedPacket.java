package dev.vfyjxf.cloudlib.network.payload;

import dev.vfyjxf.cloudlib.api.network.payload.ServerPayloadInfo;
import dev.vfyjxf.cloudlib.api.network.payload.ServerboundPayload;
import dev.vfyjxf.cloudlib.network.CloudlibNetworkPayloads;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public record MenuReversedPacket(int containerId, byte[] syncData) implements ServerboundPayload {

    public static final ServerPayloadInfo<MenuReversedPacket> INFO = CloudlibNetworkPayloads.createServerInfo(
            StreamCodec.ofMember(
                    MenuReversedPacket::encode,
                    MenuReversedPacket::decode
            ),
            "menu_sync_reversed"
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return INFO.type();
    }

    @Override
    public void handle(IPayloadContext context, ServerPlayer player) {

    }

    private void encode(RegistryFriendlyByteBuf buf) {
        buf.writeInt(containerId);
        buf.writeBytes(syncData);
    }

    private static MenuReversedPacket decode(RegistryFriendlyByteBuf buf) {
        return new MenuReversedPacket(buf.readInt(), buf.readByteArray());
    }

}
