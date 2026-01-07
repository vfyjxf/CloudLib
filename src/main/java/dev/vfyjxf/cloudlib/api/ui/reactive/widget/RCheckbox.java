package dev.vfyjxf.cloudlib.api.ui.reactive.widget;

import dev.vfyjxf.cloudlib.api.ui.reactive.widget.event.EventDef;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.event.RUIContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

import java.util.function.Supplier;

/**
 * A checkbox widget with reactive state support.
 * <p>
 * Supports the new event system for type-safe callbacks:
 * <pre>{@code
 * checkbox.onToggle().register((ctx, checked) -> {
 *     System.out.println("Checkbox toggled: " + checked);
 * });
 * }</pre>
 */
public class RCheckbox extends RWidget {

    // ===== Custom Checkbox Events =====
    
    @FunctionalInterface
    public interface OnToggle {
        void onToggle(RUIContext ctx, boolean checked);
    }
    
    private static final EventDef<OnToggle> ON_TOGGLE = EventDef.direct(
        OnToggle.class,
        listeners -> (ctx, checked) -> {
            for (var listener : listeners) {
                if (ctx.isCancelled()) break;
                listener.onToggle(ctx, checked);
            }
        }
    );
    
    private final EventDef.REvent<OnToggle> onToggleEvent = ON_TOGGLE.create();

    private String label;
    private Supplier<Boolean> valueGetter;
    private Runnable onToggle; // Legacy callback support

    // Appearance
    private int boxSize = 12;
    private int boxColor = 0xFF404040;
    private int checkColor = 0xFF00FF00;
    private int borderColor = 0xFF666666;
    private int textColor = 0xFFFFFFFF;

    private boolean hovered = false;

    public RCheckbox(String label) {
        this.label = label;
        this.valueGetter = () -> false;
    }

    public RCheckbox(String label, Supplier<Boolean> valueGetter) {
        this.label = label;
        this.valueGetter = valueGetter;
    }

    public RCheckbox(String label, Supplier<Boolean> valueGetter, Runnable onToggle) {
        this.label = label;
        this.valueGetter = valueGetter;
        this.onToggle = onToggle;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setValueGetter(Supplier<Boolean> getter) {
        this.valueGetter = getter;
    }

    public void setOnToggle(Runnable onToggle) {
        this.onToggle = onToggle;
    }
    
    /**
     * Get the toggle event for this checkbox.
     * This is a direct event that fires when the checkbox is toggled.
     */
    public EventDef.REvent<OnToggle> onToggle() {
        return onToggleEvent;
    }

    public boolean isChecked() {
        return valueGetter != null && valueGetter.get();
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        hovered = contains(mouseX, mouseY);
        boolean checked = isChecked();

        // Draw checkbox box
        int bx = x;
        int by = y + (height - boxSize) / 2;

        // Background
        int bg = hovered ? brighten(boxColor) : boxColor;
        graphics.fill(bx, by, bx + boxSize, by + boxSize, bg);

        // Border
        graphics.fill(bx, by, bx + boxSize, by + 1, borderColor);
        graphics.fill(bx, by + boxSize - 1, bx + boxSize, by + boxSize, borderColor);
        graphics.fill(bx, by, bx + 1, by + boxSize, borderColor);
        graphics.fill(bx + boxSize - 1, by, bx + boxSize, by + boxSize, borderColor);

        // Checkmark
        if (checked) {
            int inset = 2;
            graphics.fill(bx + inset, by + inset, bx + boxSize - inset, by + boxSize - inset, checkColor);
        }

        // Label
        int textX = x + boxSize + 4;
        int textY = y + (height - font.lineHeight) / 2;
        graphics.drawString(font, label, textX, textY, textColor);
    }

    private int brighten(int color) {
        int a = (color >> 24) & 0xFF;
        int r = Math.min(255, ((color >> 16) & 0xFF) + 30);
        int g = Math.min(255, ((color >> 8) & 0xFF) + 30);
        int b = Math.min(255, (color & 0xFF) + 30);
        return (a << 24) | (r << 16) | (g << 8) | b;
    }

    @Override
    public int[] measure(Font font) {
        int textWidth = font.width(label);
        return new int[]{boxSize + 4 + textWidth, Math.max(boxSize, font.lineHeight)};
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!visible || !hovered) return false;

        if (button == 0) {
            // Fire new event system
            RUIContext ctx = RUIContext.forEvent(this, mouseX, mouseY);
            boolean newChecked = !isChecked(); // After toggle
            onToggleEvent.invoker().onToggle(ctx, newChecked);
            
            // Legacy callback support
            if (onToggle != null) {
                onToggle.run();
            }
            
            // Also fire via parent's event system
            super.mouseClicked(mouseX, mouseY, button);
            return true;
        }
        return false;
    }
}
