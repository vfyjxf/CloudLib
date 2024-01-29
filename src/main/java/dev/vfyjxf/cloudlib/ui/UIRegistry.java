package dev.vfyjxf.cloudlib.ui;

import dev.vfyjxf.cloudlib.api.registry.ui.IUIRegistry;
import dev.vfyjxf.cloudlib.api.ui.IRenderable;
import dev.vfyjxf.cloudlib.api.ui.overlay.IOverlayProvider;
import dev.vfyjxf.cloudlib.api.ui.serializer.IRenderableSerializer;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;

import java.util.List;

public class UIRegistry implements IUIRegistry {

    private final MutableMap<Class<?>, IRenderableSerializer<?>> serializerMap = Maps.mutable.empty();
    private final MutableList<IOverlayProvider> overlayProviders = Lists.mutable.empty();

    @Override
    public <T extends IRenderable> void registerWidgetSerializer(Class<T> clazz, IRenderableSerializer<T> serializer) {
        if (serializerMap.containsKey(clazz)) {
            throw new IllegalArgumentException("Serializer for " + clazz.getName() + " already exists!");
        }
        serializerMap.put(clazz, serializer);
    }

    @Override
    public <T extends IRenderable> IRenderableSerializer<T> getSerializer(Class<T> clazz) {
        return (IRenderableSerializer<T>) serializerMap.get(clazz);
    }

    @Override
    public void registerOverlayProvider(IOverlayProvider provider) {
        if (overlayProviders.contains(provider)) {
            throw new IllegalArgumentException("Overlay provider " + provider.getClass().getName() + " already exists!");
        }
        overlayProviders.add(provider);
    }

    @Override
    public List<IOverlayProvider> getOverlayProviders() {
        return overlayProviders;
    }

}
