package dev.vfyjxf.cloudlib.api.ui.reactive.widget.layer;

import dev.vfyjxf.cloudlib.api.ui.reactive.widget.RGroup;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.RPopup;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.RScrollArea;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.RSlider;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.RWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

/**
 * A wrapper that allows a widget to float within its parent without affecting layout.
 * <p>
 * Floating widgets:
 * <ul>
 *   <li>Can be positioned freely within their parent bounds</li>
 *   <li>Don't take up space in the parent's layout calculation</li>
 *   <li>Can optionally be constrained to parent bounds</li>
 *   <li>Support dragging with callbacks for position changes</li>
 * </ul>
 * 
 * <h2>Layout Modes</h2>
 * <ul>
 *   <li>{@code FLOATING} - Position is managed by this wrapper, doesn't affect siblings</li>
 *   <li>{@code INLINE} - Acts like a normal widget, affects layout</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * // Create a floating panel
 * RFloating floating = new RFloating(myPanel)
 *     .setPosition(100, 50)
 *     .setDraggable(true)
 *     .setConstrainToParent(true);
 * 
 * // Add to parent - won't affect other children's layout
 * parent.addChild(floating);
 * }</pre>
 */
public class RFloating extends RWidget {
    
    /**
     * Position mode for the floating widget.
     */
    public enum PositionMode {
        /** Relative to parent's top-left corner. */
        RELATIVE,
        /** Absolute screen coordinates. */
        ABSOLUTE
    }
    
    private final RWidget content;
    private LayoutMode layoutMode = LayoutMode.FLOATING;
    private PositionMode positionMode = PositionMode.RELATIVE;
    
    // Position (for FLOATING/ABSOLUTE modes)
    private int floatX = 0;
    private int floatY = 0;
    
    // Constraints
    private boolean constrainToParent = false;
    private int minX = Integer.MIN_VALUE;
    private int minY = Integer.MIN_VALUE;
    private int maxX = Integer.MAX_VALUE;
    private int maxY = Integer.MAX_VALUE;
    
    // Dragging
    private boolean draggable = false;
    private boolean dragging = false;
    private int dragOffsetX = 0;
    private int dragOffsetY = 0;
    private @Nullable DragCallback dragCallback;
    
    /**
     * Callback for drag events.
     */
    @FunctionalInterface
    public interface DragCallback {
        /**
         * Called when the widget is dragged.
         * 
         * @param floating the floating wrapper
         * @param newX     new X position
         * @param newY     new Y position
         * @param deltaX   change in X
         * @param deltaY   change in Y
         */
        void onDrag(RFloating floating, int newX, int newY, int deltaX, int deltaY);
    }
    
    public RFloating(RWidget content) {
        this.content = content;
        content.setParent(this);
    }
    
    // ===== Configuration =====
    
    public RWidget content() { return content; }
    
    public LayoutMode layoutMode() { return layoutMode; }
    public RFloating setLayoutMode(LayoutMode mode) {
        this.layoutMode = mode;
        return this;
    }
    
    public PositionMode positionMode() { return positionMode; }
    public RFloating setPositionMode(PositionMode mode) {
        this.positionMode = mode;
        return this;
    }
    
    /**
     * Sets the floating position.
     */
    public RFloating setFloatPosition(int x, int y) {
        this.floatX = x;
        this.floatY = y;
        return this;
    }
    
    public int floatX() { return floatX; }
    public int floatY() { return floatY; }
    
    /**
     * Whether the widget can be dragged by the user.
     */
    public boolean isDraggable() { return draggable; }
    public RFloating setDraggable(boolean draggable) {
        this.draggable = draggable;
        return this;
    }
    
    /**
     * Whether the widget is constrained to parent bounds.
     */
    public boolean isConstrainToParent() { return constrainToParent; }
    public RFloating setConstrainToParent(boolean constrain) {
        this.constrainToParent = constrain;
        return this;
    }
    
    /**
     * Sets explicit bounds constraints.
     */
    public RFloating setConstraintBounds(int minX, int minY, int maxX, int maxY) {
        this.minX = minX;
        this.minY = minY;
        this.maxX = maxX;
        this.maxY = maxY;
        return this;
    }
    
    /**
     * Sets the drag callback.
     */
    public RFloating onDrag(@Nullable DragCallback callback) {
        this.dragCallback = callback;
        return this;
    }
    
    /**
     * Returns true if currently being dragged.
     */
    public boolean isDragging() { return dragging; }
    
    // ===== Layout =====
    
    @Override
    public int[] measure(Font font) {
        if (layoutMode == LayoutMode.FLOATING || layoutMode == LayoutMode.OVERLAY) {
            // Floating widgets report zero size - they don't affect parent layout
            return new int[]{0, 0};
        }
        // NORMAL mode - report actual size
        return content.measure(font);
    }
    
    /**
     * Gets the actual size of the content (even when floating).
     */
    public int[] measureActual(Font font) {
        return content.measure(font);
    }
    
    /**
     * Calculates the actual render position based on mode.
     */
    private int[] calculatePosition() {
        int px, py;
        
        if (layoutMode == LayoutMode.NORMAL) {
            // Use position set by parent layout
            px = x;
            py = y;
        } else if (positionMode == PositionMode.ABSOLUTE) {
            // Screen coordinates
            px = floatX;
            py = floatY;
        } else {
            // Relative to parent
            RWidget p = getParent();
            int parentX = p != null ? p.getX() : 0;
            int parentY = p != null ? p.getY() : 0;
            px = parentX + floatX;
            py = parentY + floatY;
        }
        
        // Apply constraints
        if (constrainToParent && getParent() != null) {
            RWidget p = getParent();
            int[] size = content.measure(null);
            px = Math.max(p.getX(), Math.min(px, p.getX() + p.getWidth() - size[0]));
            py = Math.max(p.getY(), Math.min(py, p.getY() + p.getHeight() - size[1]));
        }
        
        px = Math.max(minX, Math.min(px, maxX));
        py = Math.max(minY, Math.min(py, maxY));
        
        return new int[]{px, py};
    }
    
    // ===== Rendering =====
    
    @Override
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        
        int[] pos = calculatePosition();
        int[] size = content.measure(font);
        
        content.setPos(pos[0], pos[1]);
        content.setSize(size[0], size[1]);
        
        // Layout if content is a group
        if (content instanceof RGroup group) {
            group.layout(font);
        }
        
        content.render(graphics, font, mouseX, mouseY, delta);
    }
    
    // ===== Input Handling =====
    
    @Override
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!visible) return false;
        
        int[] pos = calculatePosition();
        int[] size = measureActual(null);
        
        // Check if click is within our bounds
        boolean inBounds = mouseX >= pos[0] && mouseX < pos[0] + size[0] &&
                          mouseY >= pos[1] && mouseY < pos[1] + size[1];
        
        if (!inBounds) return false;
        
        // Start drag?
        if (draggable && button == 0) {
            dragging = true;
            dragOffsetX = mouseX - pos[0];
            dragOffsetY = mouseY - pos[1];
        }
        
        // Forward to content
        return content.mouseClicked(mouseX, mouseY, button);
    }
    
    public boolean mouseDragged(int mouseX, int mouseY, int button, double deltaX, double deltaY) {
        if (!visible) return false;
        
        if (dragging && button == 0) {
            int oldX = floatX;
            int oldY = floatY;
            
            if (positionMode == PositionMode.ABSOLUTE) {
                floatX = mouseX - dragOffsetX;
                floatY = mouseY - dragOffsetY;
            } else {
                // Relative to parent
                RWidget p = getParent();
                int parentX = p != null ? p.getX() : 0;
                int parentY = p != null ? p.getY() : 0;
                floatX = mouseX - dragOffsetX - parentX;
                floatY = mouseY - dragOffsetY - parentY;
            }
            
            // Notify callback
            if (dragCallback != null) {
                dragCallback.onDrag(this, floatX, floatY, floatX - oldX, floatY - oldY);
            }
            
            return true;
        }
        
        // Forward drag to content if it supports dragging
        if (content instanceof RPopup popup) {
            return popup.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        } else if (content instanceof RScrollArea scrollArea) {
            return scrollArea.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        } else if (content instanceof RSlider slider) {
            return slider.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
        }
        return false;
    }
    
    @Override
    public boolean mouseReleased(int mouseX, int mouseY, int button) {
        if (button == 0) {
            dragging = false;
        }
        return content.mouseReleased(mouseX, mouseY, button);
    }
    
    @Override
    public boolean mouseScrolled(int mouseX, int mouseY, double scrollX, double scrollY) {
        if (!visible) return false;
        return content.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }
    
    @Override
    public boolean contains(int mouseX, int mouseY) {
        int[] pos = calculatePosition();
        int[] size = measureActual(null);
        return mouseX >= pos[0] && mouseX < pos[0] + size[0] &&
               mouseY >= pos[1] && mouseY < pos[1] + size[1];
    }
}
