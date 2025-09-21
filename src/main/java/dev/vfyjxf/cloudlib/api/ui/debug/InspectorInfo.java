package dev.vfyjxf.cloudlib.api.ui.debug;

import dev.vfyjxf.cloudlib.api.util.MutableLists;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;

import java.util.Arrays;

public class InspectorInfo {

    @Nullable
    private String name = null;
    @Nullable
    private Object value = null;
    private MutableList<InspectorInfoElement> properties = MutableLists.empty();

    public @Nullable String getName() {
        return name;
    }

    public void setName(@Nullable String name) {
        this.name = name;
    }

    public @Nullable <T> T getValueUnsafe() {
        return (T) value;
    }

    public @Nullable Object getValue() {
        return value;
    }

    public void setValue(@Nullable Object value) {
        this.value = value;
    }

    public void addProperties(InspectorInfoElement... properties) {
        this.properties.addAll(Arrays.asList(properties));
    }

    public InspectorInfo addProperty(String name, Object value) {
        this.properties.add(new InspectorInfoElement(name, value));
        return this;
    }

    public record InspectorInfoElement(@Nullable String name, @Nullable Object value) {
    }
}
