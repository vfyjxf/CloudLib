package dev.vfyjxf.cloudlib.ui;

import dev.vfyjxf.cloudlib.api.registry.ui.IUIRegistry;
import dev.vfyjxf.cloudlib.api.ui.IRenderable;
import dev.vfyjxf.cloudlib.api.ui.overlay.IOverlayPlugin;
import dev.vfyjxf.cloudlib.api.ui.serializer.IRenderableSerializer;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.util.HashMap;
import java.util.List;
import java.util.Map;

public class UIRegistry implements IUIRegistry {

    private final Map<Class<?>, IRenderableSerializer<?>> serializerMap = new HashMap<>();
    private final MutableList<IOverlayPlugin> plugins = Lists.mutable.empty();

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
    public void registerOverlayPlugin(IOverlayPlugin plugin) {
        boolean found = plugins.anySatisfy(p -> p.getPluginUid().equals(plugin.getPluginUid()));
        if (found) {
            throw new IllegalArgumentException("Plugin " + plugin.getPluginUid() + " already register!");
        }
        plugins.add(plugin);
    }

    @Override
    public List<IOverlayPlugin> getPlugins() {
        return Lists.mutable.ofAll(plugins);
    }


}
