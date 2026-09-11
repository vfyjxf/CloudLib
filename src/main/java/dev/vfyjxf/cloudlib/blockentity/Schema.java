package dev.vfyjxf.cloudlib.blockentity;

import com.mojang.serialization.Codec;
import dev.vfyjxf.cloudlib.api.network.UnaryFlowHandler;
import dev.vfyjxf.cloudlib.util.Checks;

/**
 * A reusable, instance-independent description of one networked+serialized field. Declared as
 * {@code static final} (often in a static inner container); a {@link BasicSyncedBlockEntity}
 * materializes an instance-bound handle from it via {@link BasicSyncedBlockEntity#useSynced}.
 *
 * @param <T> the value type
 */
public record Schema<T>(
        String name,
        T initial,
        Codec<T> nbtCodec,
        UnaryFlowHandler<T> netCodec
) {

    public static <T> Schema<T> of(String name, T initial, Codec<T> nbtCodec, UnaryFlowHandler<T> netCodec) {
        Checks.checkNotNull(name, "name");
        Checks.checkNotNull(nbtCodec, "nbtCodec");
        Checks.checkNotNull(netCodec, "netCodec");
        return new Schema<>(name, initial, nbtCodec, netCodec);
    }
}
