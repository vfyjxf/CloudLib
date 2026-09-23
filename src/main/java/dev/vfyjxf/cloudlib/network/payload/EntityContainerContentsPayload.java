package dev.vfyjxf.cloudlib.network.payload;

import dev.vfyjxf.cloudlib.api.network.payload.ClientPayloadInfo;
import dev.vfyjxf.cloudlib.api.network.payload.ClientboundPayload;
import dev.vfyjxf.cloudlib.network.CloudlibPayloads;
import dev.vfyjxf.cloudlib.ui.sync.EntityContainerContents;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server→client snapshot of one entity's inventory slots, answering an
 * {@link EntityContainerQueryPayload}. Cached client-side in
 * {@link EntityContainerContents}; grid widgets render the latest snapshot
 * and re-query on a slow poll while their panel is up.
 */
public record EntityContainerContentsPayload(int entityId, List<ItemStack> stacks) implements ClientboundPayload {

    public static final ClientPayloadInfo<EntityContainerContentsPayload> info = CloudlibPayloads.createClientInfo(
        StreamCodec.composite(
            ByteBufCodecs.VAR_INT,
            EntityContainerContentsPayload::entityId,
            ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(512)),
            EntityContainerContentsPayload::stacks,
            EntityContainerContentsPayload::new
        ),
        "entity_container_contents"
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return info.type();
    }

    @Override
    public void handle(IPayloadContext context, Player player) {
        EntityContainerContents.receive(entityId, stacks);
    }
}
