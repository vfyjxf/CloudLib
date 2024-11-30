package dev.vfyjxf.cloudlib.utils;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.RichTooltip;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;

public final class ScreenUtil {

    public static final Rect POINT = new Rect(0, 0, 0, 0);

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

    public static void renderTooltip(Screen screen, GuiGraphics graphics, RichTooltip richTooltip, int mouseX, int mouseY) {
//        graphics.pose().pushPose();
//        {
//            TooltipStack<?> stack = richTooltip.stack();
//            ItemStack itemStack = stack != null && stack.type() == ItemStack.class ? stack.castValue() : ItemStack.EMPTY;
//            List<Component> texts = CollectionUtils.filterAndMap(richTooltip.tooltips(), Tooltip::isText, Tooltip::asText);
//            List<ClientTooltipComponent> components = ClientHooks.gatherTooltipComponents(itemStack, texts, Optional.empty(), mouseX, screen.width, screen.height, screen.getMinecraft().font);
//            components = new ArrayList<>(components);
//            for (Tooltip entry : richTooltip.tooltips()) {
//                if (!entry.isText()) {
//                    TooltipComponent component = entry.asComponent();
//
//                    if (component instanceof ClientTooltipComponent client) {
//                        components.add(client);
//                        continue;
//                    }
//
//                    components.add(1, ClientTooltipComponent.create(component));
//                }
//            }
//            Font font = Minecraft.getInstance().font;
//            if (!itemStack.isEmpty()) {
//                font = ClientHooks.getTooltipFont(itemStack, font);
//            }
//            //TODO:Rewrite this
//        }
//        graphics.pose().popPose();
    }

}
