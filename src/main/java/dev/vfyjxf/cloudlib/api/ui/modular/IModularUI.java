package dev.vfyjxf.cloudlib.api.ui.modular;

import dev.vfyjxf.cloudlib.api.ui.widgets.IWidget;
import dev.vfyjxf.cloudlib.api.ui.widgets.IWidgetGroup;
import dev.vfyjxf.cloudlib.math.Dimension;
import dev.vfyjxf.cloudlib.math.Rectangle;
import net.minecraft.world.entity.player.Player;

public interface IModularUI {

    default IModularUI fullScreen() {
        setSize(getScreenWidth(), getScreenHeight());
        return this;
    }

    int getScreenWidth();

    int getScreenHeight();

    void updateScreen(int width, int height);

    default int getWidth() {
        return getMainGroup().getSize().width;
    }

    default int getHeight() {
        return getMainGroup().getSize().height;
    }

    int guiLeft();

    int guiTop();

    double getMouseX();

    double getMouseY();

    default void setSize(int width, int height) {
        if (width < 0 || height < 0) {
            throw new IllegalArgumentException("Width and height must be greater than 0");
        }
        Dimension current = getMainGroup().getSize();
        if (current.width != width && current.height != height) {
            getMainGroup().setSize(width, height);
            init();
        }
    }

    default void setSize(Dimension size) {
        if (size.width < 0 || size.height < 0) {
            throw new IllegalArgumentException("Width and height must be greater than 0");
        }
        Dimension current = getMainGroup().getSize();
        if (current.width != size.width && current.height != size.height) {
            getMainGroup().setSize(size);
            init();
        }
    }

    /**
     * @return the player who is viewing this UI.
     */
    Player getPlayer();


    default void init() {
        getMainGroup().setUI(this);
        getMainGroup().init();
    }

    IWidgetGroup<IWidget> getMainGroup();

    default void update() {
        getMainGroup().update();
    }

    default Rectangle getBounds() {
        return new Rectangle(0, getWidth(), 0, getHeight());
    }

}
