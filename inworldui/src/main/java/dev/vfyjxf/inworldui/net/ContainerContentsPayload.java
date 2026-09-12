package dev.vfyjxf.inworldui.net;

import dev.vfyjxf.cloudlib.api.network.payload.ClientPayloadInfo;
import dev.vfyjxf.cloudlib.api.network.payload.ClientboundPayload;
import dev.vfyjxf.inworldui.internal.ContainerContents;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.item.ItemStack;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Server→client snapshot of one container's slots, answering a
 * {@link ContainerQueryPayload}. Cached client-side in
 * {@link ContainerContents}; the grid widget renders the latest snapshot and
 * re-queries on a slow poll while its panel is up.
 */
public record ContainerContentsPayload(BlockPos pos, List<ItemStack> stacks)
        implements ClientboundPayload {

    public static final ClientPayloadInfo<ContainerContentsPayload> info = ClientPayloadInfo.create(
            StreamCodec.composite(
                    BlockPos.STREAM_CODEC, ContainerContentsPayload::pos,
                    ItemStack.OPTIONAL_STREAM_CODEC.apply(ByteBufCodecs.list(512)), ContainerContentsPayload::stacks,
                    ContainerContentsPayload::new),
            ResourceLocation.fromNamespaceAndPath(
                    dev.vfyjxf.inworldui.Constants.namespace, "container_contents")
    );

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return info.type();
    }

    @Override
    public void handle(IPayloadContext context, Player player) {
        ContainerContents.receive(pos, stacks);
    }
}
