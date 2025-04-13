package dev.vfyjxf.cloudlib.api.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public interface FlowHandler<T, R> extends FlowEncoder<T>, FlowDecoder<R> {

    static <T> FlowHandler<T, T> codecOf(StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        return new FlowHandler<>() {
            @Override
            public void encode(RegistryFriendlyByteBuf buffer, T value) {
                codec.encode(buffer, value);
            }

            @Override
            public T decode(RegistryFriendlyByteBuf buffer) {
                return codec.decode(buffer);
            }
        };
    }

}
