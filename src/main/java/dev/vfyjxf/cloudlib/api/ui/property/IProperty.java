package dev.vfyjxf.cloudlib.api.ui.property;

import dev.vfyjxf.cloudlib.ui.property.Property;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public interface IProperty<T> {

    static <T> IProperty<T> define(ResourceLocation identifier) {
        return new Property<>(identifier, null);
    }

    static <T> IProperty<T> define(ResourceLocation identifier, @Nullable T defaultValue) {
        return new Property<>(identifier, defaultValue);
    }

    ResourceLocation identifier();

    @Nullable
    T defaultValue();

}
