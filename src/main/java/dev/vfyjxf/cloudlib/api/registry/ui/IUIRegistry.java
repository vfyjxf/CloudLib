package dev.vfyjxf.cloudlib.api.registry.ui;

import dev.vfyjxf.cloudlib.api.annotations.Singleton;
import dev.vfyjxf.cloudlib.api.ui.IGlobalExtraAreas;
import dev.vfyjxf.cloudlib.api.ui.IGuiAreas;
import dev.vfyjxf.cloudlib.api.ui.IGuiExtraAreas;
import dev.vfyjxf.cloudlib.api.ui.IRenderable;
import dev.vfyjxf.cloudlib.api.ui.overlay.IOverlayProvider;
import dev.vfyjxf.cloudlib.api.ui.serializer.IRenderableSerializer;
import dev.vfyjxf.cloudlib.utils.Singletons;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;
import org.jetbrains.annotations.Unmodifiable;

import java.util.Collection;
import java.util.List;

@Singleton
public interface IUIRegistry {

    static IUIRegistry getInstance() {
        return Singletons.get(IUIRegistry.class);
    }

    <T extends IRenderable> void registerWidgetSerializer(Class<T> clazz, IRenderableSerializer<T> serializer);

    @Nullable <T extends IRenderable> IRenderableSerializer<T> getSerializer(Class<T> clazz);

    void registerOverlayProvider(IOverlayProvider provider);

    @Unmodifiable
    List<IOverlayProvider> getOverlayProviders();

    <T extends Screen> void registerGuiAreas(Class<T> type, IGuiAreas<T> areas);

    void registerGuiExtraAreas(Class<? extends Screen> type, IGuiExtraAreas<?> extraAreas);

    void registerGlobalExtraAreas(IGlobalExtraAreas globalGuiAreas);

    default @Nullable <T extends Screen> IGuiAreas<T> getGuiAreas(T screen){
        return (IGuiAreas<T>) getGuiAreas(screen.getClass());
    }

    @Nullable <T extends Screen> IGuiAreas<T> getGuiAreas(Class<T> type);

    <T extends Screen> Collection<IGuiExtraAreas<? extends T>> getGuiExtraAreas(T screen);

    <T extends Screen> Collection<IGuiExtraAreas<T>> getGuiExtraAreas(Class<T> type);

    @Unmodifiable
    Collection<IGlobalExtraAreas> getGlobalExtraAreas();

}
