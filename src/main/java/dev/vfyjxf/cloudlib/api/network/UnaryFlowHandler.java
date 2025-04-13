package dev.vfyjxf.cloudlib.api.network;

import net.minecraft.network.RegistryFriendlyByteBuf;
import net.minecraft.network.codec.StreamCodec;

public interface UnaryFlowHandler<T> extends FlowHandler<T, T> {

    static <T> UnaryFlowHandler<T> codecOf(StreamCodec<? super RegistryFriendlyByteBuf, T> codec) {
        return new UnaryFlowHandler<>() {
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
