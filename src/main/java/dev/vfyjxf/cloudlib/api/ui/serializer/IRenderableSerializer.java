package dev.vfyjxf.cloudlib.api.ui.serializer;

import dev.vfyjxf.cloudlib.api.registry.ui.IUIRegistry;
import dev.vfyjxf.cloudlib.api.ui.Renderable;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public interface IRenderableSerializer<T extends Renderable> {

    @Nullable
    static <T extends Renderable> IRenderableSerializer<T> getSerializer(Class<T> clazz) {
        return IUIRegistry.getInstance().getSerializer(clazz);
    }

    T deserialize(CompoundTag serialized);

    CompoundTag serialize(T widget);

}
