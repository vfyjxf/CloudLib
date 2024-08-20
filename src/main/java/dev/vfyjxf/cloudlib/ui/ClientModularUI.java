package dev.vfyjxf.cloudlib.ui;

import dev.vfyjxf.cloudlib.api.registry.ui.IUIRegistry;
import dev.vfyjxf.cloudlib.api.ui.IArea;
import dev.vfyjxf.cloudlib.api.ui.IGuiAreas;
import dev.vfyjxf.cloudlib.api.ui.IModularUI;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidgetGroup;
import dev.vfyjxf.cloudlib.event.EventListeners;
import dev.vfyjxf.cloudlib.math.Point;
import dev.vfyjxf.cloudlib.ui.inputs.KeyMappings;
import dev.vfyjxf.cloudlib.ui.widgets.WidgetGroup;
import dev.vfyjxf.cloudlib.utils.ScreenUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.world.entity.player.Player;
import net.neoforged.bus.api.EventPriority;
import net.neoforged.neoforge.client.event.ScreenEvent;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ClientModularUI implements IModularUI {

    private IWidgetGroup<IWidget> mainGroup;
    @Nullable
    private IGuiAreas<Screen> guiAreas;

    public ClientModularUI() {
        this.mainGroup = createMainGroup();
        EventListeners.register(ScreenEvent.Opening.class, EventPriority.LOWEST, event -> {
            Screen screen = event.getNewScreen();
            if (screen == null) {
                guiAreas = null;
                return;
            }
            guiAreas = (IGuiAreas<Screen>) IUIRegistry.getInstance().getGuiAreas(screen.getClass());
        });
    }

    private IWidgetGroup<IWidget> createMainGroup() {
        return new WidgetGroup<>()
                .setUI(this)
                .setPos(Point.ZERO)
                .setSize(this.getScreenWidth(), this.getScreenHeight())
//                .fixedPosition()
//                .autoSize(this::getScreenWidth, this::getScreenHeight)
                .onKeyPressed((context, input) -> {
                    if (input.is(KeyMappings.refreshUI)) {
                        mainGroup = null;
                        mainGroup = createMainGroup();
                        mainGroup.init();
                        mainGroup.update();
                    }
                    return false;
                })
                .cast();
    }

    @Override
    public int getScreenWidth() {
        Screen screen = getScreen();
        return screen == null ? 0 : screen.width;
    }

    @Override
    public int getScreenHeight() {
        Screen screen = getScreen();
        return screen == null ? 0 : screen.height;
    }

    @Override
    public void updateScreen(int width, int height) {
        //NOOP
    }

    @Override
    public int getWidth() {
        return mainGroup.getSize().width;
    }

    @Override
    public int getHeight() {
        return mainGroup.getSize().height;
    }

    @Override
    public int guiLeft() {
        Screen screen = getScreen();
        if (guiAreas == null) return 0;
        IArea area = guiAreas.mainArea(screen);
        return area.x();
    }

    @Override
    public int guiTop() {
        Screen screen = getScreen();
        if (guiAreas == null) return 0;
        IArea area = guiAreas.mainArea(screen);
        return area.y();
    }

    @Override
    public int guiWidth() {
        Screen screen = getScreen();
        if (guiAreas == null) return 0;
        IArea area = guiAreas.mainArea(screen);
        return area.width();
    }

    @Override
    public int guiHeight() {
        Screen screen = getScreen();
        if (guiAreas == null) return 0;
        IArea area = guiAreas.mainArea(screen);
        return area.height();
    }

    @Override
    public double getMouseX() {
        return ScreenUtil.getMouseX();
    }

    @Override
    public double getMouseY() {
        return ScreenUtil.getMouseY();
    }

    @Override
    public Player getPlayer() {
        return Objects.requireNonNull(Minecraft.getInstance().player);
    }

    @Override
    public IWidgetGroup<IWidget> getMainGroup() {
        return mainGroup;
    }

    @Nullable
    private static Screen getScreen() {
        return Minecraft.getInstance().screen;
    }

}
