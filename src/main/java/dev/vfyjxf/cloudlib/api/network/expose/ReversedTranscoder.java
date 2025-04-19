package dev.vfyjxf.cloudlib.api.network.expose;

import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
interface ReversedTranscoder {

    void writeToServer(RegistryFriendlyByteBuf byteBuf);

    void readFromClient(RegistryFriendlyByteBuf byteBuf);
}
