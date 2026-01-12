package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.ui.InputContext;
import dev.vfyjxf.cloudlib.api.ui.texture.UITexture;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import net.minecraft.client.gui.GuiGraphics;

import java.util.function.Consumer;

/**
 * C simple button support for selecting and hovering.
 */
public class ButtonWidget extends Widget {

    private boolean selected;
    private UITexture selectedBackground;
    private UITexture selectedIcon;
    private UITexture hoverIcon;
    private boolean clicked;

    public ButtonWidget(
            UITexture background,
            UITexture icon,
            UITexture selectedBackground,
            UITexture selectedIcon,
            UITexture hoverIcon,
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
