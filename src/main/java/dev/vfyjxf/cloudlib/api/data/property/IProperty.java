package dev.vfyjxf.cloudlib.api.data.property;

import dev.vfyjxf.cloudlib.ui.property.Property;
import org.jetbrains.annotations.Nullable;

import java.util.UUID;

public interface IProperty<T> {

    static <T> IProperty<T> define() {
        return new Property<>(UUID.randomUUID().toString(), null);
    }

    static <T> IProperty<T> define(@Nullable T defaultValue) {
        return new Property<>(UUID.randomUUID().toString(), defaultValue);
    }

    static <T> IProperty<T> define(String identifier) {
        return new Property<>(identifier, null);
    }

    static <T> IProperty<T> define(String identifier, @Nullable T defaultValue) {
        return new Property<>(identifier, defaultValue);
    }


    String identifier();

    @Nullable
    T defaultValue();

}
