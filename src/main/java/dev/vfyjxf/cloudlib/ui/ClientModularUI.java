package dev.vfyjxf.cloudlib.ui;

import dev.vfyjxf.cloudlib.api.ui.modular.IModularUI;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidgetGroup;
import dev.vfyjxf.cloudlib.event.EventListeners;
import dev.vfyjxf.cloudlib.math.Point;
import dev.vfyjxf.cloudlib.ui.inputs.KeyMappings;
import dev.vfyjxf.cloudlib.ui.widgets.WidgetGroup;
import dev.vfyjxf.cloudlib.utils.ScreenUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import net.minecraftforge.client.event.ScreenEvent;

import java.util.Objects;

public class ClientModularUI implements IModularUI {

    private IWidgetGroup<IWidget> mainGroup;
    private Screen screen;

    public ClientModularUI() {
        this.mainGroup = createMainGroup();
        EventListeners.register(ScreenEvent.Opening.class, event -> screen = event.getScreen());
    }

    private IWidgetGroup<IWidget> createMainGroup() {
        return new WidgetGroup<>()
                .setUI(this)
                .setPos(Point.ZERO)
                .setSize(this.getScreenWidth(), this.getScreenHeight())
                .fixedPosition()
                .autoSize(this::getScreenWidth, this::getScreenHeight)
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
        return screen == null ? 0 : screen.width;
    }

    @Override
    public int getScreenHeight() {
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
        if (screen instanceof AbstractContainerScreen<?> container) {
            return container.getGuiLeft();
        }
        return 0;
    }

    @Override
    public int guiTop() {
        if (screen instanceof AbstractContainerScreen<?> container) {
            return container.getGuiTop();
        }
        return 0;
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

}
