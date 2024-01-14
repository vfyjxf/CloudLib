package dev.vfyjxf.cloudlib.ui.property;

import dev.vfyjxf.cloudlib.api.ui.property.IProperty;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class Property<T> implements IProperty<T> {

    private final ResourceLocation identifier;
    @Nullable
    private final T defaultValue;

    public Property(ResourceLocation identifier, @Nullable T defaultValue) {
        this.identifier = identifier;
        this.defaultValue = defaultValue;
    }

    @Override
    public ResourceLocation identifier() {
        return identifier;
    }

    @Override
    public @Nullable T defaultValue() {
        return defaultValue;
    }
}
