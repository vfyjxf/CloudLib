package dev.vfyjxf.nimbusprojection.network;

import dev.vfyjxf.cloudlib.api.network.payload.ClientPayloadInfo;
import dev.vfyjxf.cloudlib.api.network.payload.ClientboundPayload;
import dev.vfyjxf.nimbusprojection.api.section.SectionData;
import dev.vfyjxf.nimbusprojection.api.section.SectionInstance;
import dev.vfyjxf.nimbusprojection.api.section.SectionTarget;
import dev.vfyjxf.nimbusprojection.internal.section.SectionContents;
import dev.vfyjxf.nimbusprojection.internal.section.SectionProviders;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.ByteBufCodecs;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

import java.util.ArrayList;
import java.util.List;

/**
 * Server→client snapshot of every section at a container position,
 * answering a {@link SectionQueryPayload}. Each entry is a
 * server-addressable {@code id} plus its typed data; the data codec
 * resolves through the section registry — an unregistered type fails to
 * encode loudly rather than dropping bytes.
 */
public record SectionSnapshotPayload(SectionTarget target, List<Entry> entries) implements ClientboundPayload {

    /** One section on the wire — {@code "type/index"} plus its decoded data. */
    public record Entry(String id, SectionData data) {}

    /** Packs resolved instances into wire entries. */
    public static SectionSnapshotPayload of(SectionTarget target, List<SectionInstance<?>> sections) {
        List<Entry> entries = new ArrayList<>(sections.size());
        for (SectionInstance<?> section : sections) {
            entries.add(new Entry(section.id(), section.data()));
        }
        return new SectionSnapshotPayload(target, entries);
    }

    private static final StreamCodec<RegistryFriendlyByteBuf, Entry> entryCodec = StreamCodec.of(
            (buf, entry) -> {
                buf.writeUtf(entry.id());
                ResourceLocation.STREAM_CODEC.encode(buf, entry.data().type().id());
                encodeData(buf, entry.data());
            },
            buf -> {
                String id = buf.readUtf();
                ResourceLocation typeId = ResourceLocation.STREAM_CODEC.decode(buf);
                return new Entry(id, decodeData(buf, typeId));
            });

    @SuppressWarnings({"unchecked", "rawtypes"})
    private static void encodeData(RegistryFriendlyByteBuf buf, SectionData data) {
        StreamCodec codec = SectionProviders.codecOf(data.type().id());
        if (codec == null) {
            throw new IllegalArgumentException(
                    "No section codec for type: " + data.type().id());
        }
        codec.encode(buf, data);
    }

    private static SectionData decodeData(RegistryFriendlyByteBuf buf, ResourceLocation typeId) {
        StreamCodec<RegistryFriendlyByteBuf, ? extends SectionData> codec = SectionProviders.codecOf(typeId);
        if (codec == null) {
            throw new IllegalArgumentException("No section codec for type: " + typeId);
        }
        return codec.decode(buf);
    }

    public static final ClientPayloadInfo<SectionSnapshotPayload> info = NimbusPayloads.createClientInfo(
            StreamCodec.composite(
                    SectionTarget.streamCodec,
                    SectionSnapshotPayload::target,
                    entryCodec.apply(ByteBufCodecs.list(64)),
                    SectionSnapshotPayload::entries,
                    SectionSnapshotPayload::new),
            "section_snapshot");

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return info.type();
    }

    @Override
    public void handle(IPayloadContext context, Player player) {
        SectionContents.receive(target, entries);
    }
}
