package dev.vfyjxf.cloudlib.api.ui.property;

import org.jetbrains.annotations.Nullable;

import java.util.Map;

public interface IPropertyHolder {


    <T> T putProperty(IProperty<T> property, T value);

    @Nullable <T> T getProperty(IProperty<T> property);

    boolean hasProperty(IProperty<?> property);

    Map<IProperty<?>, Object> getProperties();

}
