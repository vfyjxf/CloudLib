package dev.vfyjxf.cloudlib.data;

import dev.vfyjxf.cloudlib.api.data.serialize.ISerializer;
import dev.vfyjxf.cloudlib.api.registry.ISerializeRegistry;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

public class SerializeRegistry implements ISerializeRegistry {

    private final MutableMap<Class<?>, ISerializer<?>> serializers = Maps.mutable.empty();

    @Override
    public <T> void register(Class<T> type, ISerializer<T> serializer) {
        if (serializers.containsKey(type)) {
            throw new IllegalStateException("Serializer for type " + type + " already registered");
        }
        serializers.put(type, serializer);
    }

    @SuppressWarnings("unchecked")
    @Nullable
    @Override
    public <T> ISerializer<T> getSerializer(Class<T> type) {
        return (ISerializer<T>) serializers.get(type);
    }

    @Override
    public Collection<ISerializer<?>> getSerializers() {
        return serializers.values();
    }
}
