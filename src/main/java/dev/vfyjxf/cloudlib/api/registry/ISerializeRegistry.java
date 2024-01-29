package dev.vfyjxf.cloudlib.api.registry;

import dev.vfyjxf.cloudlib.api.annotations.Singleton;
import dev.vfyjxf.cloudlib.api.data.serialize.ISerializer;
import dev.vfyjxf.cloudlib.utils.Singletons;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;

@Singleton
public interface ISerializeRegistry {

    static ISerializeRegistry getInstance() {
        return Singletons.get(ISerializeRegistry.class);
    }

    <T> void register(Class<T> type, ISerializer<T> serializer);

    @Nullable <T> ISerializer<T> getSerializer(Class<T> type);

    Collection<ISerializer<?>> getSerializers();

}
