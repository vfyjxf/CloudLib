package dev.vfyjxf.cloudlib.helper;

import dev.vfyjxf.cloudlib.api.data.serialize.ISerializer;
import dev.vfyjxf.cloudlib.api.registry.ISerializeRegistry;
import net.minecraft.nbt.Tag;

public final class DataHelper {

    private DataHelper() {

    }

    @SuppressWarnings("unchecked")
    public static <T> Tag toNbt(T value) {
        Class<T> type = (Class<T>) value.getClass();
        ISerializeRegistry registry = ISerializeRegistry.getInstance();
        ISerializer<T> serializer = registry.getSerializer(type);
        if (serializer == null) return null;
        return serializer.serialize(value);
    }

}
