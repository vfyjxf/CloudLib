package dev.vfyjxf.cloudlib.util;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.ui.tooltip.Tooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.world.item.ItemStack;

public final class ScreenUtil {

    public static double getMouseX() {
        Minecraft minecraft = Minecraft.getInstance();
        MouseHandler mouseHelper = minecraft.mouseHandler;
        double scale = (double) minecraft.getWindow().getGuiScaledWidth() / (double) minecraft.getWindow().getScreenWidth();
        return mouseHelper.xpos() * scale;
    }

    public static double getMouseY() {
        Minecraft minecraft = Minecraft.getInstance();
        MouseHandler mouseHelper = minecraft.mouseHandler;
        double scale = (double) minecraft.getWindow().getGuiScaledHeight() / (double) minecraft.getWindow().getScreenHeight();
        return mouseHelper.ypos() * scale;
    }

    public static FloatPos getMousePos() {
        Minecraft minecraft = Minecraft.getInstance();
        MouseHandler mouseHelper = minecraft.mouseHandler;
        double scale = (double) minecraft.getWindow().getGuiScaledWidth() / (double) minecraft.getWindow().getScreenWidth();
        var mouseX = mouseHelper.xpos() * scale;
        var mouseY = mouseHelper.ypos() * scale;
        return new FloatPos(mouseX, mouseY);
    }

    public static Pos ofMouse() {
        return new Pos((int) getMouseX(), (int) getMouseY());
    }

    public static void renderTooltip(GuiGraphics graphics, Tooltip tooltip, int mouseX, int mouseY) {
        Font font = Minecraft.getInstance().font;
        var tooltipStack = tooltip.stack();
        ItemStack stackValue = ItemStack.EMPTY;
        if (tooltipStack != null) {
            stackValue = tooltipStack.asStack();
            font = tooltipStack.resolveFont(font);
        }
        graphics.renderComponentTooltipFromElements(font, tooltip.toVanilla(), mouseX, mouseY, stackValue);
    }

}
