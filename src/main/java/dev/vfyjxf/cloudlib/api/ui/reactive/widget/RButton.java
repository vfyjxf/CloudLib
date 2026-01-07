package dev.vfyjxf.cloudlib.api.ui.reactive.widget;

import dev.vfyjxf.cloudlib.api.ui.reactive.Style;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.event.EventDef;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.event.RUIContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

/**
 * A reactive button widget with hover effects and click handling.
 * <p>
 * Supports the new event system for type-safe callbacks:
 * <pre>{@code
 * button.onAction().register((ctx) -> {
 *     System.out.println("Button clicked!");
 * });
 * }</pre>
 */
public class RButton extends RWidget {

    // ===== Custom Button Events =====
    
    @FunctionalInterface
    public interface OnAction {
        void onAction(RUIContext ctx);
    }
    
    private static final EventDef<OnAction> ON_ACTION = EventDef.direct(
        OnAction.class,
        listeners -> (ctx) -> {
            for (var listener : listeners) {
                if (ctx.isCancelled()) break;
                listener.onAction(ctx);
            }
        }
    );
    
    private final EventDef.REvent<OnAction> onAction = ON_ACTION.create();

    private String label;
    private Runnable onClick; // Legacy callback support

    // Colors
    private int normalColor = 0xFF404040;
    private int hoverColor = 0xFF505050;
    private int pressedColor = 0xFF303030;
    private int textColor = 0xFFFFFFFF;
    private int borderColor = 0xFF606060;

    private boolean hovered = false;
    private boolean pressed = false;

    public RButton(String label) {
        this.label = label;
    }

    public RButton(String label, Runnable onClick) {
        this.label = label;
        this.onClick = onClick;
    }
    
    /**
     * Get the action event for this button.
     * This is a direct event that fires when the button is clicked.
     */
    public EventDef.REvent<OnAction> onAction() {
        return onAction;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public String getLabel() {
        return label;
    }

    public void setOnClick(Runnable onClick) {
        this.onClick = onClick;
    }

    public void setColors(int normal, int hover, int pressed) {
        this.normalColor = normal;
        this.hoverColor = hover;
        this.pressedColor = pressed;
    }

    public void setTextColor(int color) {
        this.textColor = color;
    }

    public void setBorderColor(int color) {
        this.borderColor = color;
    }

    @Override
    public void setStyle(Style style) {
        super.setStyle(style);
        if (style != null) {
            var bg = style.get(Style.Background.class);
            if (bg != null) {
                normalColor = bg.color();
                hoverColor = brighten(normalColor, 0.2f);
                pressedColor = darken(normalColor, 0.2f);
            }
            var color = style.get(Style.Color.class);
            if (color != null) {
                textColor = color.value();
            }
        }
    }

    private int brighten(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, (int)(((color >> 16) & 0xFF) * (1 + factor)));
        int g = Math.min(255, (int)(((color >> 8) & 0xFF) * (1 + factor)));
        int b = Math.min(255, (int)((color & 0xFF) * (1 + factor)));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    private int darken(int color, float factor) {
        int a = (color >> 24) & 0xFF;
        int r = (int)(((color >> 16) & 0xFF) * (1 - factor));
        int g = (int)(((color >> 8) & 0xFF) * (1 - factor));
        int b = (int)((color & 0xFF) * (1 - factor));
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        // Update hover state
        hovered = contains(mouseX, mouseY);

        // Determine background color
        int bgColor;
        if (pressed && hovered) {
            bgColor = pressedColor;
        } else if (hovered) {
            bgColor = hoverColor;
        } else {
            bgColor = normalColor;
        }

        // Draw background
        graphics.fill(x, y, x + width, y + height, bgColor);

        // Draw border
        graphics.fill(x, y, x + width, y + 1, borderColor); // top
        graphics.fill(x, y + height - 1, x + width, y + height, borderColor); // bottom
        graphics.fill(x, y, x + 1, y + height, borderColor); // left
        graphics.fill(x + width - 1, y, x + width, y + height, borderColor); // right

        // Draw label centered
        int textX = x + (width - font.width(label)) / 2;
        int textY = y + (height - font.lineHeight) / 2;
        graphics.drawString(font, label, textX, textY, textColor);
    }

    @Override
    public int[] measure(Font font) {
        int textWidth = font.width(label);
        int textHeight = font.lineHeight;
        // Add padding
        return new int[]{textWidth + 16, textHeight + 8};
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!visible || !hovered) return false;
        if (button == 0) { // Left click
            pressed = true;
            
            // Fire new event system
            RUIContext ctx = RUIContext.forEvent(this, mouseX, mouseY);
            onAction.invoker().onAction(ctx);
            
            // Legacy callback support
            if (onClick != null) {
                onClick.run();
            }
            
            // Also fire via parent's event system
            super.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseReleased(int mouseX, int mouseY, int button) {
        pressed = false;
        return super.mouseReleased(mouseX, mouseY, button);
    }
}
