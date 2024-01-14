package dev.vfyjxf.cloudlib.api.ui.serializer;

import dev.vfyjxf.cloudlib.api.registry.ui.IUIRegistry;
import dev.vfyjxf.cloudlib.api.ui.IRenderable;
import net.minecraft.nbt.CompoundTag;
import org.jetbrains.annotations.Nullable;

public interface IRenderableSerializer<T extends IRenderable> {

    @Nullable
    static <T extends IRenderable> IRenderableSerializer<T> getSerializer(Class<T> clazz) {
        return IUIRegistry.getInstance().getSerializer(clazz);
    }

    T deserialize(CompoundTag serialized);

    CompoundTag serialize(T widget);

}
