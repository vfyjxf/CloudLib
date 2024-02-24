package dev.vfyjxf.cloudlib.helper;

import dev.vfyjxf.cloudlib.api.registry.ui.IUIRegistry;
import dev.vfyjxf.cloudlib.api.ui.IArea;
import dev.vfyjxf.cloudlib.api.ui.IGlobalExtraAreas;
import dev.vfyjxf.cloudlib.api.ui.IGuiAreas;
import dev.vfyjxf.cloudlib.api.ui.IGuiExtraAreas;
import net.minecraft.client.gui.screens.Screen;
import org.eclipse.collections.api.factory.Lists;
import org.eclipse.collections.api.list.MutableList;

import java.util.Collection;

public final class ScreenHelper {

    private ScreenHelper() {
    }

    @SuppressWarnings("unchecked")
    public static Collection<IArea> getScreenExtraAreas(Screen screen) {
        IUIRegistry registry = IUIRegistry.class.getInstance();
        IGuiAreas<Screen> guiAreas = registry.getGuiAreas(screen);
        MutableList<IArea> areas = Lists.mutable.empty();
        if (guiAreas != null) {
            areas.addAll(guiAreas.extraAreas(screen));
        }
        for (IGuiExtraAreas<?> guiExtraArea : registry.getGuiExtraAreas(screen)) {
            IGuiExtraAreas<Screen> area = (IGuiExtraAreas<Screen>) guiExtraArea;
            areas.addAll(area.getAreas(screen));
        }
        for (IGlobalExtraAreas globalExtraArea : registry.getGlobalExtraAreas()) {
            areas.addAll(globalExtraArea.extraAreas());
        }
        return areas.distinct();
    }

}
