package dev.vfyjxf.cloudlib.ui;

import dev.vfyjxf.cloudlib.api.ui.IModularUI;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidgetGroup;
import dev.vfyjxf.cloudlib.ui.widgets.WidgetGroup;
import dev.vfyjxf.cloudlib.utils.ScreenUtil;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.entity.player.Player;
import org.jetbrains.annotations.NotNull;

import java.util.Objects;

/**
 * wrapper a {@link net.minecraft.client.gui.screens.Screen} as a {@link IModularUI}
 */
public class ScreenWrapper implements IModularUI {

    @NotNull
    private final Screen screen;
    private final IWidgetGroup<IWidget> mainGroup = new WidgetGroup<>();

    public ScreenWrapper(Screen screen) {
        this.screen = screen;
    }

    @Override
    public int getScreenWidth() {
        return screen.width;
    }

    @Override
    public int getScreenHeight() {
        return screen.height;
    }

    @Override
    public void updateScreen(int width, int height) {
        //NO-OP
    }

    @Override
    public int getWidth() {
        return screen.width;
    }

    @Override
    public int getHeight() {
        return screen.height;
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
    public int guiWidth() {
        return 0;
    }

    @Override
    public int guiHeight() {
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
        return Objects.requireNonNull(screen.getMinecraft().player);
    }

    @Override
    public IWidgetGroup<IWidget> getMainGroup() {
        return mainGroup;
    }

    @Override
    public void update() {
        mainGroup.update();
    }

}
