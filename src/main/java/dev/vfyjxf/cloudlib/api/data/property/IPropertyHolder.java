package dev.vfyjxf.cloudlib.api.data.property;

import dev.vfyjxf.cloudlib.api.data.serialize.ISerializer;
import net.minecraft.nbt.CompoundTag;
import net.minecraft.nbt.Tag;
import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface IPropertyHolder {

    <T> T put(IProperty<T> property, T value);

    @Nullable <T> T get(IProperty<T> property);

    boolean hasProperty(IProperty<?> property);

    Map<IProperty<?>, Object> getProperties();

    default void transform(Map<IProperty<?>, Object> cache) {
        cache.putAll(this.getProperties());
    }

    default CompoundTag serialize() {
        CompoundTag tag = new CompoundTag();
        for (Map.Entry<IProperty<?>, Object> entry : getProperties().entrySet()) {
            Tag maybe = ISerializer.to(entry.getValue());
            if (maybe == null) continue;
            tag.put(entry.getKey().identifier(), maybe);
        }
        return tag;
    }

}
