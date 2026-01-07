package dev.vfyjxf.cloudlib.api.ui.reactive.widget;

import dev.vfyjxf.cloudlib.api.ui.reactive.widget.event.EventDef;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.event.RUIContext;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;

/**
 * A horizontal slider widget for selecting values.
 * <p>
 * Supports the new event system for type-safe callbacks:
 * <pre>{@code
 * slider.onValueChange().register((ctx, oldValue, newValue) -> {
 *     System.out.println("Value changed from " + oldValue + " to " + newValue);
 * });
 * }</pre>
 */
public class RSlider extends RWidget {

    // ===== Custom Slider Events =====
    
    @FunctionalInterface
    public interface OnValueChange {
        void onValueChange(RUIContext ctx, float oldValue, float newValue);
    }
    
    private static final EventDef<OnValueChange> ON_VALUE_CHANGE = EventDef.direct(
        OnValueChange.class,
        listeners -> (ctx, oldValue, newValue) -> {
            for (var listener : listeners) {
                if (ctx.isCancelled()) break;
                listener.onValueChange(ctx, oldValue, newValue);
            }
        }
    );
    
    private final EventDef.REvent<OnValueChange> onValueChangeEvent = ON_VALUE_CHANGE.create();

    private float value = 0.5f; // 0.0 to 1.0
    private float min = 0.0f;
    private float max = 1.0f;
    private float step = 0.01f;

    private String label = "";
    private boolean showValue = true;

    // Appearance
    private int trackColor = 0xFF444444;
    private int trackFillColor = 0xFF4488FF;
    private int thumbColor = 0xFFDDDDDD;
    private int thumbHoverColor = 0xFFFFFFFF;
    private int labelColor = 0xFFFFFFFF;
    private int thumbWidth = 8;

    // State - persisted in SliderState
    private final SliderState state;

    // Transient
    private boolean hovered = false;
    private boolean thumbHovered = false;

    // Callback
    private ValueChangeListener onValueChange; // Legacy callback support

    public RSlider(int width, int height, SliderState state) {
        this.width = width;
        this.height = height;
        this.state = state;
        this.value = state.value;
    }
    
    /**
     * Get the value change event for this slider.
     * This is a direct event that fires when the value changes.
     */
    public EventDef.REvent<OnValueChange> onValueChange() {
        return onValueChangeEvent;
    }

    public void setValue(float value) {
        this.value = Math.max(0.0f, Math.min(1.0f, value));
        this.state.value = this.value;
    }

    public float getValue() {
        return value;
    }

    public float getActualValue() {
        return min + value * (max - min);
    }

    public void setRange(float min, float max) {
        this.min = min;
        this.max = max;
    }

    public void setStep(float step) {
        this.step = step;
    }

    public void setLabel(String label) {
        this.label = label;
    }

    public void setShowValue(boolean show) {
        this.showValue = show;
    }

    public void setOnValueChange(ValueChangeListener listener) {
        this.onValueChange = listener;
    }

    public void setColors(int track, int trackFill, int thumb) {
        this.trackColor = track;
        this.trackFillColor = trackFill;
        this.thumbColor = thumb;
    }

    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float delta) {
        if (!visible) return;

        hovered = contains(mouseX, mouseY);
        value = state.value; // Sync from state

        int trackY = y + (height - 4) / 2;
        int thumbX = x + (int) ((width - thumbWidth) * value);
        int thumbY = y + 2;
        int thumbHeight = height - 4;

        thumbHovered = mouseX >= thumbX && mouseX < thumbX + thumbWidth &&
                mouseY >= thumbY && mouseY < thumbY + thumbHeight;

        // Draw track background
        graphics.fill(x, trackY, x + width, trackY + 4, trackColor);

        // Draw filled portion
        graphics.fill(x, trackY, thumbX + thumbWidth / 2, trackY + 4, trackFillColor);

        // Draw thumb
        int tc = (thumbHovered || state.dragging) ? thumbHoverColor : thumbColor;
        graphics.fill(thumbX, thumbY, thumbX + thumbWidth, thumbY + thumbHeight, tc);

        // Draw label/value
        if (showValue || !label.isEmpty()) {
            String text = label.isEmpty() ? String.format("%.2f", getActualValue()) 
                    : label.replace("%v", String.format("%.2f", getActualValue()));
            graphics.drawString(font, text, x + width + 4, y + (height - 8) / 2, labelColor);
        }
    }

    @Override
    public int[] measure(Font font) {
        return new int[]{width, height};
    }

    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!visible || button != 0) return false;
        
        // Check if click is within slider bounds
        if (!contains(mouseX, mouseY)) return false;

        // Start dragging
        state.dragging = true;
        updateValueFromMouse(mouseX);
        return true;
    }

    public boolean mouseDragged(int mouseX, int mouseY, int button, double deltaX, double deltaY) {
        if (state.dragging) {
            updateValueFromMouse(mouseX);
            return true;
        }
        return false;
    }

    @Override
    public boolean mouseReleased(int mouseX, int mouseY, int button) {
        if (state.dragging) {
            state.dragging = false;
            return true;
        }
        return false;
    }
    
    @Override
    public boolean mouseScrolled(int mouseX, int mouseY, double scrollX, double scrollY) {
        if (!visible || !contains(mouseX, mouseY)) return false;
        
        // Scroll to change value - scrollY positive = increase
        float scrollAmount = (float) scrollY * step;
        if (step <= 0) {
            scrollAmount = (float) scrollY * 0.05f; // Default 5% per scroll
        }
        
        float oldValue = value;
        float newValue = value + scrollAmount / (max - min);
        newValue = Math.max(0.0f, Math.min(1.0f, newValue));
        
        if (Math.abs(newValue - value) > 0.0001f) {
            value = newValue;
            state.value = value;
            
            // Fire event
            RUIContext ctx = RUIContext.forEvent(this, mouseX, mouseY);
            onValueChangeEvent.invoker().onValueChange(ctx, getActualValue(oldValue), getActualValue());
            
            // Legacy callback
            if (onValueChange != null) {
                onValueChange.onValueChange(getActualValue());
            }
        }
        
        return true; // Consume scroll event when hovering slider
    }

    private void updateValueFromMouse(int mouseX) {
        float oldValue = value;
        float newValue = (float) (mouseX - x - thumbWidth / 2) / (width - thumbWidth);
        newValue = Math.max(0.0f, Math.min(1.0f, newValue));

        // Apply step
        if (step > 0) {
            float range = max - min;
            float actual = min + newValue * range;
            actual = Math.round(actual / step) * step;
            newValue = (actual - min) / range;
        }

        if (Math.abs(newValue - value) > 0.001f) {
            value = newValue;
            state.value = value;
            
            // Fire new event system
            RUIContext ctx = RUIContext.forEvent(this, mouseX, y);
            onValueChangeEvent.invoker().onValueChange(ctx, getActualValue(oldValue), getActualValue());
            
            // Legacy callback support
            if (onValueChange != null) {
                onValueChange.onValueChange(getActualValue());
            }
        }
    }
    
    private float getActualValue(float normalizedValue) {
        return min + normalizedValue * (max - min);
    }

    @FunctionalInterface
    public interface ValueChangeListener {
        void onValueChange(float newValue);
    }

    /**
     * State for slider that persists across widget rebuilds.
     */
    public static class SliderState {
        public float value = 0.5f;
        public boolean dragging = false;

        public SliderState() {}

        public SliderState(float initialValue) {
            this.value = initialValue;
        }
    }
}
