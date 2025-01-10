package dev.vfyjxf.cloudlib.api.registry.ui;

import dev.vfyjxf.cloudlib.api.ui.Renderable;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayProvider;
import dev.vfyjxf.cloudlib.api.ui.serializer.IRenderableSerializer;
import dev.vfyjxf.cloudlib.utils.Singletons;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.List;

//TODO:Refactor and cleanup
public interface IUIRegistry {

    static IUIRegistry getInstance() {
        return Singletons.get(IUIRegistry.class);
    }

    <T extends Renderable> void registerWidgetSerializer(Class<T> clazz, IRenderableSerializer<T> serializer);

    @Nullable <T extends Renderable> IRenderableSerializer<T> getSerializer(Class<T> clazz);

    void registerOverlayProvider(OverlayProvider provider);

    @Unmodifiable
    List<OverlayProvider> getOverlayProviders();

}
