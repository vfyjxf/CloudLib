package dev.vfyjxf.cloudlib.ui.widgets;

import dev.vfyjxf.cloudlib.api.event.EventDispatch;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.texture.ColorTexture;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

/**
 * A clickable button widget with customizable appearance.
 * <p>
 * Features:
 * <ul>
 *   <li>Normal, hover, and pressed states</li>
 *   <li>Optional icon support</li>
 *   <li>Disabled state</li>
 * </ul>
 */
public class ClickableButtonWidget extends Widget {

    private Component label;
    private @Nullable Runnable onClick;

    private VisualTexture normalTexture = new ColorTexture(0xFF555555);
    private VisualTexture hoverTexture = new ColorTexture(0xFF777777);
    private VisualTexture pressedTexture = new ColorTexture(0xFF333333);
    private VisualTexture disabledTexture = new ColorTexture(0xFF444444);
    private @Nullable VisualTexture iconTexture = null;

    private int textColor = 0xFFFFFF;
    private int disabledTextColor = 0xA0A0A0;
    private boolean enabled = true;
    private boolean pressed = false;

    public static ClickableButtonWidget of(String label) {
        return new ClickableButtonWidget(Component.literal(label));
    }

    public static ClickableButtonWidget of(Component label) {
        return new ClickableButtonWidget(label);
    }

    public static ClickableButtonWidget of(String label, Runnable onClick) {
        return new ClickableButtonWidget(Component.literal(label)).onClick(onClick);
    }

    private ClickableButtonWidget(Component label) {
        this.label = label;

        onMouseClick((input, clickCount, context) -> {
            if (enabled && onClick != null) {
                onClick.run();
                return EventDispatch.consumed;
            }
            return EventDispatch.pass;
        });

        onMouseEnter((mouseX, mouseY, context) -> {
            if (enabled) {
                setHovered(true);
            }
        });

        onMouseLeave((mouseX, mouseY, context) -> {
            setHovered(false);
            pressed = false;
        });
    }

    public Component label() {
        return label;
    }

    public ClickableButtonWidget setLabel(Component label) {
        this.label = label;
        return this;
    }

    public ClickableButtonWidget setLabel(String label) {
        this.label = Component.literal(label);
        return this;
    }

    public ClickableButtonWidget onClick(@Nullable Runnable onClick) {
        this.onClick = onClick;
        return this;
    }

    public boolean enabled() {
        return enabled;
    }

    public ClickableButtonWidget setEnabled(boolean enabled) {
        this.enabled = enabled;
        return this;
    }

    public ClickableButtonWidget setTextures(VisualTexture normal, VisualTexture hover, VisualTexture pressed) {
        this.normalTexture = normal;
        this.hoverTexture = hover;
        this.pressedTexture = pressed;
        return this;
    }

    public ClickableButtonWidget setColors(int normal, int hover, int pressed) {
        this.normalTexture = new ColorTexture(normal);
        this.hoverTexture = new ColorTexture(hover);
        this.pressedTexture = new ColorTexture(pressed);
        return this;
    }

    public ClickableButtonWidget setDisabledTexture(VisualTexture texture) {
        this.disabledTexture = texture;
        return this;
    }

    public ClickableButtonWidget setIconTexture(@Nullable VisualTexture texture) {
        this.iconTexture = texture;
        return this;
    }

    public ClickableButtonWidget setTextColor(int textColor) {
        this.textColor = textColor;
        return this;
    }

    public ClickableButtonWidget setDisabledTextColor(int disabledTextColor) {
        this.disabledTextColor = disabledTextColor;
        return this;
    }

    @Override
    protected void renderInternal(GuiGraphics graphics, int mouseX, int mouseY, float partialTicks) {
        // Determine current texture based on state
        VisualTexture texture;
        if (!enabled) {
            texture = disabledTexture;
        } else if (pressed) {
            texture = pressedTexture;
        } else if (hovered()) {
            texture = hoverTexture;
        } else {
            texture = normalTexture;
        }

        texture.render(graphics, 0, 0, width(), height());

        // Render icon if present
        int iconOffset = 0;
        if (iconTexture != null) {
            int iconSize = Math.min(height() - 4, 16);
            int iconY = (height() - iconSize) / 2;
            iconTexture.render(graphics, 4, iconY, iconSize, iconSize);
            iconOffset = iconSize + 4;
        }

        // Render label
        var font = context().font();
        int textWidth = font.width(label);
        int textX = iconOffset + (width() - iconOffset - textWidth) / 2;
        int textY = (height() - font.lineHeight) / 2;
        int color = enabled ? textColor : disabledTextColor;

        graphics.drawString(font, label, textX, textY, color);
    }
}
