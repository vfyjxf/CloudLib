package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.RenderableTexture;
import dev.vfyjxf.cloudlib.api.ui.widgets.Widget;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Consumer;

/**
 * C simple button support for selecting and hovering.
 */
public class ButtonWidget extends Widget {

    private boolean selected;
    private RenderableTexture selectedBackground;
    private RenderableTexture selectedIcon;
    private RenderableTexture hoverIcon;
    private boolean clicked;

    public ButtonWidget(
            RenderableTexture background,
            RenderableTexture icon,
            RenderableTexture selectedBackground,
            RenderableTexture selectedIcon,
            RenderableTexture hoverIcon,
            Consumer<InputContext> onClick
    ) {
        onMouseClicked((input, context) -> {
            if (input.isLeftClick()) {
                clicked = true;
                return true;
            }
            clicked = false;
            return false;
        });
        onMouseReleased((input, context) -> {
            if (clicked) {
                onClick.accept(input);

            }
            clicked = false;
            return true;
        });

    }

    @Override
    protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {

    }
}
