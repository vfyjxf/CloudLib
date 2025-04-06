package dev.vfyjxf.cloudlib.api.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamDecoder;

public interface FlowDecoder<T> {

    static <T> FlowDecoder<T> of(StreamDecoder<? super FriendlyByteBuf, T> decoder) {
        return decoder::decode;
    }

    T decode(RegistryFriendlyByteBuf byteBuf);

}
