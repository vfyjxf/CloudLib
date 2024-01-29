package dev.vfyjxf.cloudlib.api.data.serialize;

import dev.vfyjxf.cloudlib.api.registry.ISerializeRegistry;
import dev.vfyjxf.cloudlib.helper.DataHelper;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

public interface ISerializer<T> {

    static <T> Tag to(T value) {
        return DataHelper.toNbt(value);
    }

    @Nullable
    static <T> T from(Tag tag) {
        for (ISerializer<?> serializer : ISerializeRegistry.getInstance().getSerializers()) {
            var maybe = serializer.deserialize(tag);
            if (maybe != null) {
                return (T) maybe;
            }
        }
        return null;
    }

    @Nullable
    T deserialize(Tag tag);

    Tag serialize(T value);

}
