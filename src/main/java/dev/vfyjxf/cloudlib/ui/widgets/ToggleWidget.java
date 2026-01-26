package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;

/**
 * A toggle/switch widget for boolean states.
 * <p>
 * Provides visual feedback for on/off states with customizable
 * textures and optional callbacks.
 */
public class ToggleWidget extends Widget {

    private boolean toggled = false;
    private @Nullable Consumer<Boolean> onToggle;

    private VisualTexture offTexture = new ColorTexture(0xFF666666);
    private VisualTexture onTexture = new ColorTexture(0xFF00AA00);
    private @Nullable VisualTexture hoverTexture = null;

    public static ToggleWidget create() {
        return new ToggleWidget();
    }

    public static ToggleWidget create(boolean initial) {
        return new ToggleWidget().setToggled(initial);
    }

    private ToggleWidget() {
        onMouseClick((input, clickCount, context) -> {
            toggle();
            return EventDispatch.consumed;
        });
    }

    public boolean toggled() {
        return toggled;
    }

    public ToggleWidget setToggled(boolean toggled) {
        this.toggled = toggled;
        return this;
    }

    public ToggleWidget toggle() {
        this.toggled = !this.toggled;
        if (onToggle != null) {
            onToggle.accept(toggled);
        }
        return this;
    }

    public ToggleWidget onToggle(@Nullable Consumer<Boolean> onToggle) {
        this.onToggle = onToggle;
        return this;
    }

    public ToggleWidget setTextures(VisualTexture offTexture, VisualTexture onTexture) {
        this.offTexture = offTexture;
        this.onTexture = onTexture;
        return this;
    }

    public ToggleWidget setColors(int offColor, int onColor) {
        this.offTexture = new ColorTexture(offColor);
        this.onTexture = new ColorTexture(onColor);
        return this;
    }

    public ToggleWidget setHoverTexture(@Nullable VisualTexture hoverTexture) {
        this.hoverTexture = hoverTexture;
        return this;
    }

    @Override
    protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        super.renderInternal(graphics, mouseX, mouseY, partialTicks);

        VisualTexture texture;
        if (hovered() && hoverTexture != null) {
            texture = hoverTexture;
        } else {
            texture = toggled ? onTexture : offTexture;
        }

        texture.render(graphics, 0, 0, width(), height());
    }
}
