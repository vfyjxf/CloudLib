package dev.vfyjxf.cloudlib.api.ui;

import net.minecraft.client.gui.GuiGraphics;

public interface IRenderable {

    /**
     * @param graphics     The matrix stack
     * @param mouseX       the relative mouse x
     * @param mouseY       the relative mouse y
     * @param partialTicks the partial ticks
     */
    void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks);

    default void render(GuiGraphics graphics) {
        render(graphics, 0, 0, 0);
    }

}
