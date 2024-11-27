package dev.vfyjxf.cloudlib.api.ui.overlay;

import dev.vfyjxf.cloudlib.api.annotations.Singleton;
import dev.vfyjxf.cloudlib.api.event.EventHandler;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.Renderable;
import dev.vfyjxf.cloudlib.api.ui.event.OverlayEvent;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.api.ui.widgets.WidgetGroup;
import dev.vfyjxf.cloudlib.utils.Singletons;
import org.eclipse.collections.api.list.MutableList;
import org.jetbrains.annotations.Nullable;

/**
 *
 */
@Singleton
public interface UIOverlay extends Renderable, EventHandler<OverlayEvent> {

    static UIOverlay getInstance() {
        return Singletons.get(UIOverlay.class);
    }

    void init();

    boolean initialized();

    void update();

    void tick();

    default WidgetGroup<Widget> getMainGroup() {
        //TODO: Implement
        return null;
    }

    @Nullable
    default <T extends Widget> T getWidgetOfType(Class<T> type) {
        return getMainGroup().getWidgetOfType(type);
    }

    default <T extends Widget> void getWidgetsOfType(Class<T> type, MutableList<T> widgets) {
        getMainGroup().getWidgetsOfType(type, widgets);
    }

    Rect getBounds();

    void setBounds(Rect bounds);

    boolean containsMouse(int mouseX, int mouseY);

}
