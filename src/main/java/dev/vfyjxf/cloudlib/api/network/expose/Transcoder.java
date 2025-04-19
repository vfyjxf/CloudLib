package dev.vfyjxf.cloudlib.api.network.expose;

import net.minecraft.network.RegistryFriendlyByteBuf;
import org.jetbrains.annotations.ApiStatus;

@ApiStatus.Internal
interface Transcoder {

    void writeToClient(RegistryFriendlyByteBuf byteBuf);

    void readFromServer(RegistryFriendlyByteBuf byteBuf);

}
