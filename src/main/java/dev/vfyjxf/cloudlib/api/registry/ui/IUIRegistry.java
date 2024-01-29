package dev.vfyjxf.cloudlib.api.registry.ui;

import dev.vfyjxf.cloudlib.api.annotations.Singleton;
import dev.vfyjxf.cloudlib.api.ui.IRenderable;
import dev.vfyjxf.cloudlib.api.ui.overlay.IOverlayProvider;
import dev.vfyjxf.cloudlib.api.ui.serializer.IRenderableSerializer;
import dev.vfyjxf.cloudlib.utils.Singletons;
import org.jetbrains.annotations.Nullable;

import java.util.List;

@Singleton
public interface IUIRegistry {

    static IUIRegistry getInstance() {
        return Singletons.get(IUIRegistry.class);
    }

    <T extends IRenderable> void registerWidgetSerializer(Class<T> clazz, IRenderableSerializer<T> serializer);

    @Nullable <T extends IRenderable> IRenderableSerializer<T> getSerializer(Class<T> clazz);

    void registerOverlayProvider(IOverlayProvider provider);

    List<IOverlayProvider> getOverlayProviders();

}
