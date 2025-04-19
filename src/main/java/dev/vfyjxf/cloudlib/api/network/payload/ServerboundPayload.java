package dev.vfyjxf.cloudlib.api.network.payload;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public interface ServerboundPayload extends CustomPacketPayload {
    void handle(IPayloadContext context, ServerPlayer player);
}
