package dev.vfyjxf.cloudlib.api.ui;

import dev.vfyjxf.cloudlib.api.ui.state.IState;
import dev.vfyjxf.cloudlib.utils.Singletons;
import net.minecraft.resources.ResourceLocation;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.map.MutableMap;
import org.jetbrains.annotations.NotNull;
import org.jetbrains.annotations.Nullable;

public class UIConfig {

    private final MutableMap<ResourceLocation, Object> states = Maps.mutable.empty();

    public static UIConfig getInstance() {
        return Singletons.get(UIConfig.class);
    }

    @SuppressWarnings("unchecked")
    public <T> T put(@NotNull ResourceLocation identifier, @NotNull IState<T> state) {
        //noinspection
        return (T) states.put(identifier, state);
    }

    @Nullable
    public <T> T get(ResourceLocation identifier) {
        return (T) states.get(identifier);
    }

    public void clear() {
        states.clear();
    }

}
