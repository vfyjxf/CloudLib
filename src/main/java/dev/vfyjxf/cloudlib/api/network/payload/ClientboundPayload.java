package dev.vfyjxf.cloudlib.api.network.payload;

import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

public interface ClientboundPayload extends CustomPacketPayload {

    void handle(IPayloadContext context, Player player);
}

