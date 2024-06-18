package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.IRenderableTexture;
import net.minecraft.client.gui.GuiGraphics;

public class IconButton extends Widget {

    private final IRenderableTexture background;
    private final IRenderableTexture icon;

    public IconButton(IRenderableTexture background, IRenderableTexture icon) {
        this.background = background;
        this.icon = icon;
    }

    public IRenderableTexture getBackground() {
        return background;
    }

    public IRenderableTexture getIcon() {
        return icon;
    }

    @Override
    public void render(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        if (invisible()) return;
        background.render(graphics, 0, 0);
        icon.render(graphics, 0, 0);
    }
}
