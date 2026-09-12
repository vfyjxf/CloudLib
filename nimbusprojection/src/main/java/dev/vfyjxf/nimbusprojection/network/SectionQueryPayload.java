package dev.vfyjxf.nimbusprojection.network;

import dev.vfyjxf.cloudlib.api.network.payload.ServerPayloadInfo;
import dev.vfyjxf.cloudlib.api.network.payload.ServerboundPayload;
import dev.vfyjxf.nimbusprojection.api.section.SectionInstance;
import dev.vfyjxf.nimbusprojection.internal.section.SectionProviders;
import net.minecraft.core.BlockPos;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Client→server request for a container's full section set — the
 * multi-kind counterpart of CloudLib's {@code ContainerQueryPayload}.
 * The server runs every registered {@code SectionProvider} at the
 * position and answers with {@link SectionSnapshotPayload}; section data
 * is display-only, ops stay server-validated.
 */
public record SectionQueryPayload(BlockPos pos) implements ServerboundPayload {

    /** Reach bound for a query — same as the item-contents channel. */
    private static final double reach = 16.0;

    public static final ServerPayloadInfo<SectionQueryPayload> info = NimbusPayloads.createServerInfo(
            StreamCodec.ofMember(SectionQueryPayload::encode, SectionQueryPayload::decode), "section_query");

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return info.type();
    }

    private void encode(RegistryFriendlyByteBuf buf) {
        buf.writeBlockPos(pos);
    }

    private static SectionQueryPayload decode(RegistryFriendlyByteBuf buf) {
        return new SectionQueryPayload(buf.readBlockPos());
    }

    @Override
    public void handle(IPayloadContext context, ServerPlayer player) {
        if (!pos.closerToCenterThan(player.getEyePosition(), reach)) return;
        List<SectionInstance<?>> sections = SectionProviders.collectAll(player.level(), pos);
        if (sections.isEmpty()) return;
        context.reply(SectionSnapshotPayload.of(pos, sections));
    }
}
