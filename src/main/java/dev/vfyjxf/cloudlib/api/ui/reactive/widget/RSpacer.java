package dev.vfyjxf.cloudlib.api.ui.reactive.widget;

import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

/**
 * A simple spacer widget for adding gaps in layouts.
 */
public class RSpacer extends RWidget {

    public RSpacer(int width, int height) {
        this.width = width;
        this.height = height;
    }

    public static RSpacer vertical(int height) {
        return new RSpacer(0, height);
    }

    public static RSpacer horizontal(int width) {
        return new RSpacer(width, 0);
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float delta) {
        // Spacer renders nothing
    }

    @Override
    public int[] measure(Font font) {
        return new int[]{width, height};
    }
}
