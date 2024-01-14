package dev.vfyjxf.cloudlib.api.ui.overlay;

import dev.vfyjxf.cloudlib.api.ui.IRenderable;
import dev.vfyjxf.cloudlib.api.ui.modular.IModularUI;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidgetGroup;
import dev.vfyjxf.cloudlib.math.Rectangle;
import dev.vfyjxf.cloudlib.utils.Singletons;

import javax.annotation.Nullable;
import java.util.List;

public interface IUIOverlay extends IRenderable {


    static IUIOverlay getInstance() {
        return Singletons.get(IUIOverlay.class);
    }

    void init();

    boolean initialized();

    void update();

    void tick();

    IModularUI getUI();

    List<IOverlayPlugin> getPlugins();

    @Nullable
    IOverlayPlugin getDisplayPlugin();

    IUIOverlay setDisplayPlugin(IOverlayPlugin displayPlugin);

    IUIOverlay setUI(IModularUI ui);

    default IWidgetGroup<IWidget> getMainGroup() {
        return getUI().getMainGroup();
    }

    Rectangle getBounds();

    void setBounds(Rectangle bounds);

    boolean containsMouse(int mouseX, int mouseY);

}
