package dev.vfyjxf.cloudlib.api.registry.ui;

import dev.vfyjxf.cloudlib.api.ui.IRenderable;
import dev.vfyjxf.cloudlib.api.ui.overlay.IOverlayPlugin;
import dev.vfyjxf.cloudlib.api.ui.serializer.IRenderableSerializer;
import dev.vfyjxf.cloudlib.utils.Singletons;
import org.jetbrains.annotations.Nullable;

import java.util.List;

public interface IUIRegistry {

    static IUIRegistry getInstance() {
        return Singletons.get(IUIRegistry.class);
    }


    <T extends IRenderable> void registerWidgetSerializer(Class<T> clazz, IRenderableSerializer<T> serializer);

    @Nullable <T extends IRenderable> IRenderableSerializer<T> getSerializer(Class<T> clazz);

    void registerOverlayPlugin(IOverlayPlugin plugin);

    List<IOverlayPlugin> getPlugins();

}
