package dev.vfyjxf.nimbusprojection.network;

import dev.vfyjxf.cloudlib.api.network.payload.ClientboundPayload;
import dev.vfyjxf.cloudlib.api.ui.inworld.PanelKey;
import dev.vfyjxf.nimbusprojection.internal.InworldManager;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;
import net.minecraft.network.protocol.common.custom.CustomPacketPayload;
import net.minecraft.resources.ResourceLocation;
import net.minecraft.world.entity.player.Player;
import net.neoforged.neoforge.network.handling.IPayloadContext;

/** Server → client: a shared panel was unshared — tear the local copy down. */
public record SharedPanelRemovePayload(PanelKey key) implements ClientboundPayload {

    public static final StreamCodec<RegistryFriendlyByteBuf, SharedPanelRemovePayload> streamCodec =
            PanelKey.streamCodec
                    .map(SharedPanelRemovePayload::new, SharedPanelRemovePayload::key)
                    .cast();

    public static final Type<SharedPanelRemovePayload> type =
            new Type<>(ResourceLocation.fromNamespaceAndPath("nimbusprojection", "shared_remove"));

    @Override
    public Type<? extends CustomPacketPayload> type() {
        return type;
    }

    @Override
    public void handle(IPayloadContext context, Player player) {
        InworldManager manager = InworldManager.instance();
        if (manager != null) manager.onSharedRemove(key);
    }
}
