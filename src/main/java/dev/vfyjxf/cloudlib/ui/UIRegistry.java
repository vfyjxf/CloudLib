package dev.vfyjxf.cloudlib.ui;

import dev.vfyjxf.cloudlib.api.registry.ui.IUIRegistry;
import dev.vfyjxf.cloudlib.api.ui.GlobalExtraAreas;
import dev.vfyjxf.cloudlib.api.ui.GuiAreas;
import dev.vfyjxf.cloudlib.api.ui.GuiExtraAreas;
import dev.vfyjxf.cloudlib.api.ui.Renderable;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayProvider;
import dev.vfyjxf.cloudlib.api.ui.serializer.IRenderableSerializer;
import net.minecraft.client.gui.screens.Screen;
import org.eclipse.collections.api.collection.MutableCollection;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.factory.Maps;
import org.eclipse.collections.api.list.MutableList;
import org.eclipse.collections.api.map.MutableMap;
import org.eclipse.collections.impl.collector.Collectors2;
import org.jetbrains.annotations.Nullable;

import java.util.Collection;
import java.util.List;

public class UIRegistry implements IUIRegistry {

    private final MutableMap<Class<?>, IRenderableSerializer<?>> serializerMap = Maps.mutable.empty();
    private final MutableList<OverlayProvider> overlayProviders = Lists.mutable.empty();
    private final MutableMap<Class<?>, GuiAreas<?>> guiAreas = Maps.mutable.empty();
    private final MutableMap<Class<? extends Screen>, MutableCollection<GuiExtraAreas<?>>> guiExtraAreas = Maps.mutable.empty();
    private final MutableList<GlobalExtraAreas> globalExtraAreas = Lists.mutable.empty();

    @Override
    public <T extends Renderable> void registerWidgetSerializer(Class<T> clazz, IRenderableSerializer<T> serializer) {
        if (serializerMap.containsKey(clazz)) {
            throw new IllegalArgumentException("Serializer for " + clazz.getName() + " already exists!");
        }
        serializerMap.put(clazz, serializer);
    }

    @Override
    public <T extends Renderable> IRenderableSerializer<T> getSerializer(Class<T> clazz) {
        return (IRenderableSerializer<T>) serializerMap.get(clazz);
    }

    @Override
    public void registerOverlayProvider(OverlayProvider provider) {
        if (overlayProviders.contains(provider)) {
            throw new IllegalArgumentException("Overlay handler " + provider.getClass().getName() + " already exists!");
        }
        overlayProviders.add(provider);
    }

    @Override
    public List<OverlayProvider> getOverlayProviders() {
        return overlayProviders;
    }

    @Override
    public <T extends Screen> void registerGuiAreas(Class<T> type, GuiAreas<T> areas) {
        if (guiAreas.containsKey(type)) {
            throw new IllegalArgumentException("Gui areas for " + type.getName() + " already exists!");
        }
        guiAreas.put(type, areas);
    }

    @Override
    public void registerGuiExtraAreas(Class<? extends Screen> type, GuiExtraAreas<?> extraAreas) {
        guiExtraAreas.computeIfAbsent(type, k -> Lists.mutable.empty()).add(extraAreas);
    }


    @Override
    @SuppressWarnings("unchecked")
    public @Nullable <T extends Screen> GuiAreas<T> getGuiAreas(Class<T> type) {
        return (GuiAreas<T>) guiAreas.get(type);
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Screen> Collection<GuiExtraAreas<? extends T>> getGuiExtraAreas(T screen) {
        return (Collection<GuiExtraAreas<? extends T>>) (Object) guiExtraAreas.entrySet()
                .stream()
                .filter(entry -> entry.getKey().isAssignableFrom(screen.getClass()))
                .flatMap(entry -> entry.getValue().stream())
                .collect(Collectors2.toSet());
    }

    @Override
    @SuppressWarnings("unchecked")
    public <T extends Screen> Collection<GuiExtraAreas<T>> getGuiExtraAreas(Class<T> type) {
        return ((Collection<GuiExtraAreas<T>>) (Object) guiExtraAreas.get(type));
    }


    @Override
    public void registerGlobalExtraAreas(GlobalExtraAreas globalGuiAreas) {
        if (globalExtraAreas.contains(globalGuiAreas)) {
            throw new IllegalArgumentException("Global extra areas " + globalGuiAreas.getClass().getName() + " already exists!");
        }
        globalExtraAreas.add(globalGuiAreas);
    }

    @Override
    public Collection<GlobalExtraAreas> getGlobalExtraAreas() {
        return globalExtraAreas;
    }


}
