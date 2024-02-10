package dev.vfyjxf.cloudlib.ui;

import dev.vfyjxf.cloudlib.api.ui.IModularUI;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidgetGroup;
import dev.vfyjxf.cloudlib.math.Point;
import dev.vfyjxf.cloudlib.ui.inputs.KeyMappings;
import dev.vfyjxf.cloudlib.ui.widgets.WidgetGroup;
import dev.vfyjxf.cloudlib.utils.ScreenUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

public class ClientModularUI implements IModularUI {

    private IWidgetGroup<IWidget> mainGroup;

    public ClientModularUI() {
        this.mainGroup = createMainGroup();
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
        if (getScreen() instanceof AbstractContainerScreen<?> container) {
            return container.getGuiLeft();
        }
        return 0;
    }

    @Override
    public int guiTop() {
        if (getScreen() instanceof AbstractContainerScreen<?> container) {
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

    @Nullable
    private static Screen getScreen() {
        return Minecraft.getInstance().screen;
    }

}
