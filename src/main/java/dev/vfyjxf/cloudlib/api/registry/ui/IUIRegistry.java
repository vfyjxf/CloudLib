package dev.vfyjxf.cloudlib.api.registry.ui;

import dev.vfyjxf.cloudlib.api.annotations.Singleton;
import dev.vfyjxf.cloudlib.api.ui.GlobalExtraAreas;
import dev.vfyjxf.cloudlib.api.ui.GuiAreas;
import dev.vfyjxf.cloudlib.api.ui.GuiExtraAreas;
import dev.vfyjxf.cloudlib.api.ui.Renderable;
import dev.vfyjxf.cloudlib.api.ui.overlay.OverlayProvider;
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

    <T extends Renderable> void registerWidgetSerializer(Class<T> clazz, IRenderableSerializer<T> serializer);

    @Nullable <T extends Renderable> IRenderableSerializer<T> getSerializer(Class<T> clazz);

    void registerOverlayProvider(OverlayProvider provider);

    @Unmodifiable
    List<OverlayProvider> getOverlayProviders();

    <T extends Screen> void registerGuiAreas(Class<T> type, GuiAreas<T> areas);

    void registerGuiExtraAreas(Class<? extends Screen> type, GuiExtraAreas<?> extraAreas);

    void registerGlobalExtraAreas(GlobalExtraAreas globalGuiAreas);

    default @Nullable <T extends Screen> GuiAreas<T> getGuiAreas(T screen){
        return (GuiAreas<T>) getGuiAreas(screen.getClass());
    }

    @Nullable <T extends Screen> GuiAreas<T> getGuiAreas(Class<T> type);

    <T extends Screen> Collection<GuiExtraAreas<? extends T>> getGuiExtraAreas(T screen);

    <T extends Screen> Collection<GuiExtraAreas<T>> getGuiExtraAreas(Class<T> type);

    @Unmodifiable
    Collection<GlobalExtraAreas> getGlobalExtraAreas();

}
