package dev.vfyjxf.nimbusprojection.network;

import dev.vfyjxf.cloudlib.api.network.payload.ServerPayloadInfo;
import dev.vfyjxf.cloudlib.api.network.payload.ServerboundPayload;
import dev.vfyjxf.nimbusprojection.api.section.SectionInstance;
import dev.vfyjxf.nimbusprojection.api.section.SectionTarget;
import dev.vfyjxf.nimbusprojection.internal.section.SectionProviders;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.server.level.ServerPlayer;
import net.minecraft.world.phys.Vec3;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.List;

/**
 * Client→server request for a target's full section set — the multi-kind
 * counterpart of CloudLib's {@code ContainerQueryPayload}, addressing a
 * block position or an entity. The server runs every registered provider
 * and answers with {@link SectionSnapshotPayload}; section data is
 * display-only, ops stay server-validated.
 */
public record SectionQueryPayload(SectionTarget target) implements ServerboundPayload {

    /** Reach bound for a query — same as the item-contents channel. */
    private static final double reach = 16.0;

    public static final ServerPayloadInfo<SectionQueryPayload> info = NimbusPayloads.createServerInfo(
            StreamCodec.composite(SectionTarget.streamCodec, SectionQueryPayload::target, SectionQueryPayload::new),
            "section_query");

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return info.type();
    }

    @Override
    public void handle(IPayloadContext context, ServerPlayer player) {
        Vec3 center = target.center(player.level());
        if (center == null || !center.closerThan(player.getEyePosition(), reach)) return;
        List<SectionInstance<?>> sections = SectionProviders.collectAll(player.level(), target);
        if (sections.isEmpty()) return;
        context.reply(SectionSnapshotPayload.of(target, sections));
    }
}
