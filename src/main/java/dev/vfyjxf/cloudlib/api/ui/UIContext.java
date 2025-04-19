package dev.vfyjxf.cloudlib.api.ui;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import dev.vfyjxf.cloudlib.utils.ScreenUtil;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.AbstractContainerScreen;
import net.minecraft.world.inventory.AbstractContainerMenu;
import net.minecraft.world.inventory.Slot;
import net.minecraft.world.item.ItemStack;
import org.jetbrains.annotations.Nullable;

public class UIContext {

    /**
     * The font used to render text.
     */
    private Font font;
    /**
     * The width of current {@link net.minecraft.client.Minecraft#screen}
     */
    private int width;
    /**
     * The height of current {@link net.minecraft.client.Minecraft#screen}
     */
    private int height;
    private double mouseX;
    private double mouseY;

    /**
     * @return if {@link net.minecraft.client.Minecraft#screen} is null,return null
     */
    public static UIContext current() {
        return new UIContext();
    }

    private UIContext() {
        var minecraft = Minecraft.getInstance();
        this.font = minecraft.font;
        this.height = minecraft.getWindow().getGuiScaledWidth();
        this.width = minecraft.getWindow().getGuiScaledHeight();
    }

    public @Nullable Screen currentScreen() {
        return Minecraft.getInstance().screen;
    }

    @Nullable
    public <T extends Screen> T typedScreen(Class<T> type) {
        var screen = currentScreen();
        return type.isInstance(screen) ? type.cast(screen) : null;
    }

    /**
     * @param coordinate the widget to get the relative mouse position
     * @return the relative position of the mouse
     */
    public FloatPos mouseRelative(Widget coordinate) {
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

    @Nullable
    public ItemStack getStackUnderMouse() {
        AbstractContainerScreen<?> screen = typedScreen(AbstractContainerScreen.class);
        AbstractContainerMenu menu = screen != null ? screen.getMenu() : null;
        if (menu == null) return null;
        Slot slot = screen.getSlotUnderMouse();
        return slot == null ? null : slot.getItem();
    }

    public void tick() {
        var pos = ScreenUtil.getMousePos();
        this.mouseX = pos.x;
        this.mouseY = pos.y;
    }

}
