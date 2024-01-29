package dev.vfyjxf.cloudlib.ui.property;

import dev.vfyjxf.cloudlib.api.data.property.IProperty;
import net.minecraft.resources.ResourceLocation;
import org.jetbrains.annotations.Nullable;

public class Property<T> implements IProperty<T> {

    private final String identifier;
    @Nullable
    private final T defaultValue;

    public Property(String identifier, @Nullable T defaultValue) {
        this.identifier = identifier;
        this.defaultValue = defaultValue;
    }

    @Override
    public String identifier() {
        return identifier;
    }

    @Override
    public @Nullable T defaultValue() {
        return defaultValue;
    }
}
