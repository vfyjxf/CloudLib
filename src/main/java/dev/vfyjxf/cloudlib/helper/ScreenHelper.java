package dev.vfyjxf.cloudlib.helper;

import dev.vfyjxf.cloudlib.api.registry.ui.IUIRegistry;
import dev.vfyjxf.cloudlib.api.ui.Area;
import dev.vfyjxf.cloudlib.api.ui.GlobalExtraAreas;
import dev.vfyjxf.cloudlib.api.ui.GuiAreas;
import dev.vfyjxf.cloudlib.api.ui.GuiExtraAreas;
import net.minecraft.client.gui.screens.Screen;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.util.Collection;

public final class ScreenHelper {

    private ScreenHelper() {
    }

    @SuppressWarnings("unchecked")
    public static Collection<Area> getScreenExtraAreas(Screen screen) {
        IUIRegistry registry = IUIRegistry.getInstance();
        GuiAreas<Screen> guiAreas = registry.getGuiAreas(screen);
        MutableList<Area> areas = Lists.mutable.empty();
        if (guiAreas != null) {
            areas.addAll(guiAreas.extraAreas(screen));
        }
        for (GuiExtraAreas<?> guiExtraArea : registry.getGuiExtraAreas(screen)) {
            GuiExtraAreas<Screen> area = (GuiExtraAreas<Screen>) guiExtraArea;
            areas.addAll(area.getAreas(screen));
        }
        for (GlobalExtraAreas globalExtraArea : registry.getGlobalExtraAreas()) {
            areas.addAll(globalExtraArea.extraAreas());
        }
        return areas.distinct();
    }

}
