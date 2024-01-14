package dev.vfyjxf.cloudlib.utils;

import dev.vfyjxf.cloudlib.api.ui.widgets.ITooltip;
import dev.vfyjxf.cloudlib.api.ui.widgets.ITooltipStack;
import dev.vfyjxf.cloudlib.math.Point;
import dev.vfyjxf.cloudlib.math.Rectangle;
import dev.vfyjxf.cloudlib.mixin.GuiGraphicsAccessor;
import net.minecraft.client.Minecraft;
import net.minecraft.client.MouseHandler;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.Screen;
import net.minecraft.client.gui.screens.inventory.tooltip.ClientTooltipComponent;
import net.minecraft.client.gui.screens.inventory.tooltip.DefaultTooltipPositioner;
import net.minecraft.network.chat.Component;
import net.minecraft.world.inventory.tooltip.TooltipComponent;
import net.minecraft.world.item.ItemStack;
import net.minecraftforge.client.ForgeHooksClient;

import java.util.ArrayList;
import java.util.List;
import java.util.Optional;

public final class ScreenUtil {

    public static final Rectangle POINT = new Rectangle(0, 0, 0, 0);

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

    public static Point ofMouse() {
        return new Point((int) getMouseX(), (int) getMouseY());
    }

    public static void renderTooltip(Screen screen, GuiGraphics graphics, ITooltip tooltip, int mouseX, int mouseY) {
        graphics.pose().pushPose();
        {
            ITooltipStack<?> stack = tooltip.stack();
            ItemStack itemStack = stack != null && stack.type() == ItemStack.class ? stack.castValue() : ItemStack.EMPTY;
            List<Component> texts = CollectionUtils.filterAndMap(tooltip.entries(), ITooltip.Entry::isText, ITooltip.Entry::asText);
            List<ClientTooltipComponent> components = ForgeHooksClient.gatherTooltipComponents(itemStack, texts, Optional.empty(), mouseX, screen.width, screen.height, screen.getMinecraft().font);
            components = new ArrayList<>(components);
            for (ITooltip.Entry entry : tooltip.entries()) {
                if (!entry.isText()) {
                    TooltipComponent component = entry.asComponent();

                    if (component instanceof ClientTooltipComponent client) {
                        components.add(client);
                        continue;
                    }

                    components.add(1, ClientTooltipComponent.create(component));
                }
            }
            Font font = Minecraft.getInstance().font;
            if (!itemStack.isEmpty()) {
                font = ForgeHooksClient.getTooltipFont(itemStack, font);
            }
            //because some problems with arch loom,subproject can apply at,so we use mixin to access it.
            GuiGraphicsAccessor accessor = (GuiGraphicsAccessor) graphics;
            accessor.setTooltipStack(itemStack);
            accessor.callRenderTooltipInternal(font, components, mouseX, mouseY, DefaultTooltipPositioner.INSTANCE);
            accessor.setTooltipStack(null);
        }
        graphics.pose().popPose();
    }

}
