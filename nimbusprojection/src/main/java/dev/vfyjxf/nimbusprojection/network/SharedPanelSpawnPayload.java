package dev.vfyjxf.nimbusprojection.network;

import dev.vfyjxf.cloudlib.api.network.payload.ClientboundPayload;
import dev.vfyjxf.cloudlib.api.ui.inworld.AnchorCodec;
import dev.vfyjxf.cloudlib.api.ui.inworld.AnchorCodecs;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.PanelKey;
import dev.vfyjxf.cloudlib.api.ui.inworld.Presentation;
import dev.vfyjxf.cloudlib.api.ui.inworld.PresentationCodecs;
import dev.vfyjxf.nimbusprojection.internal.InworldManager;
import net.minecraft.core.registries.Registries;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceKey;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.minecraft.world.level.Level;
import net.neoforged.neoforge.network.handling.IPayloadContext;
import org.jetbrains.annotations.Nullable;

/**
 * Server → client: a {@code SharedPanelSpec} materializes on this client.
 * <p>
 * Every nested field resolves through its own opt-in registry — the anchor
 * through {@link AnchorCodecs}, the presentation through
 * {@link PresentationCodecs}, the view payload through the channel type
 * registry — so the wire never trusts a type the receiver didn't declare.
 * {@link #canInteract} is the per-recipient verdict of
 * {@code SharedPanelSpec.canInteract}, baked at send time: a spectator
 * materializes the panel as read-only.
 */
public record SharedPanelSpawnPayload(
        PanelKey key,
        ResourceKey<Level> dimension,
        InworldAnchor anchor,
        ResourceLocation view,
        Presentation presentation,
        double maxDistance,
        boolean canInteract,
        @Nullable CustomPacketPayload payload
) implements ClientboundPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, SharedPanelSpawnPayload> STREAM_CODEC =
            StreamCodec.ofMember(SharedPanelSpawnPayload::encode, SharedPanelSpawnPayload::decode);

    public static final Type<SharedPanelSpawnPayload> TYPE =
            new Type<>(ResourceLocation.fromNamespaceAndPath("nimbusprojection", "shared_spawn"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return TYPE;
    }

    @SuppressWarnings({"unchecked", "rawtypes"})
    private void encode(RegistryFriendlyByteBuf buf) {
        PanelKey.STREAM_CODEC.encode(buf, key);
        ResourceKey.streamCodec(Registries.DIMENSION).encode(buf, dimension);
        //anchor: type id + registered codec
        AnchorCodec anchorCodec = AnchorCodecs.of(anchor);
        if (anchorCodec == null) {
            throw new IllegalArgumentException("Anchor " + anchor.type().id() + " has no codec — not shareable");
        }
        ResourceLocation.STREAM_CODEC.encode(buf, anchor.type().id());
        anchorCodec.codec().encode(buf, anchor);
        ResourceLocation.STREAM_CODEC.encode(buf, view);
        PresentationCodecs.write(buf, presentation);
        buf.writeDouble(maxDistance);
        buf.writeBoolean(canInteract);
        buf.writeBoolean(payload != null);
        if (payload != null) {
            PanelChannelPayload.payloadCodec().encode(buf, payload);
        }
    }

    private static SharedPanelSpawnPayload decode(RegistryFriendlyByteBuf buf) {
        PanelKey key = PanelKey.STREAM_CODEC.decode(buf);
        ResourceKey<Level> dimension = ResourceKey.streamCodec(Registries.DIMENSION).decode(buf);
        ResourceLocation anchorType = ResourceLocation.STREAM_CODEC.decode(buf);
        AnchorCodec<?> codec = AnchorCodecs.byId(anchorType);
        if (codec == null) {
            throw new IllegalArgumentException("Unknown anchor type on the wire: " + anchorType);
        }
        InworldAnchor anchor = (InworldAnchor) codec.codec().decode(buf);
        ResourceLocation view = ResourceLocation.STREAM_CODEC.decode(buf);
        Presentation presentation = PresentationCodecs.read(buf);
        double maxDistance = buf.readDouble();
        boolean canInteract = buf.readBoolean();
        CustomPacketPayload payload = buf.readBoolean()
                ? PanelChannelPayload.payloadCodec().decode(buf)
                : null;
        return new SharedPanelSpawnPayload(
                key, dimension, anchor, view, presentation, maxDistance, canInteract, payload);
    }

    @Override
    public void handle(IPayloadContext context, Player player) {
        InworldManager manager = InworldManager.instance();
        if (manager != null) manager.onSharedSpawn(this);
    }
}
