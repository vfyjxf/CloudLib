package dev.vfyjxf.cloudlib.api.ui;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.utils.ScreenUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import org.jetbrains.annotations.Nullable;

public class UIContext {

    /**
     * Nullable when use overlay.
     */
    @Nullable
    private Screen screen;

    /**
     * The font used to render text.
     */
    private Font font;
    /**
     * The width of current {@link net.minecraft.client.Minecraft#screen}
     */
    private int widget;
    /**
     * The height of current {@link net.minecraft.client.Minecraft#screen}
     */
    private int height;
    private double mouseX;
    private double mouseY;

    /**
     * @return if {@link net.minecraft.client.Minecraft#screen} is null,return null
     */
    @Nullable
    public static UIContext current() {
        var screen = Minecraft.getInstance().screen;
        if (screen == null) return null;
        return new UIContext(screen);
    }


    private UIContext(Screen screen) {
        this.font = Minecraft.getInstance().font;
        this.height = screen.height;
        this.widget = screen.width;
    }

    /**
     * @param coordinate the widget to get the relative mouse position
     * @return the relative position of the mouse
     */
    public FloatPos relativeMousePos(Widget coordinate) {
        var parent = coordinate.parent();
        var pos = mousePos();
        while (parent != null) {
            pos.translate(-parent.posX(), -parent.posY());
            parent = parent.parent();
        }
        return pos;
    }

    /**
     * @return the absolute position of the mouse
     */
    public FloatPos mousePos() {
        var pos = ScreenUtil.getMousePos();
        this.mouseX = pos.x;
        this.mouseY = pos.y;
        return pos;
    }

    public Font getFont() {
        return font;
    }

    public void tick() {
        var pos = ScreenUtil.getMousePos();
        this.mouseX = pos.x;
        this.mouseY = pos.y;
    }

}
