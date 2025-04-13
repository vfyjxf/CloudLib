package dev.vfyjxf.cloudlib.api.network;

import net.minecraft.network.FriendlyByteBuf;
import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamEncoder;

public interface FlowEncoder<T> extends StreamEncoder<RegistryFriendlyByteBuf, T> {

    static <T> FlowEncoder<T> of(StreamEncoder<? super FriendlyByteBuf, T> encoder) {
        return encoder::encode;
    }

    void encode(RegistryFriendlyByteBuf byteBuf, T element);

}
