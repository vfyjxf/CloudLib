package dev.vfyjxf.cloudlib.api.ui.reactive.widget.layer;

import dev.vfyjxf.cloudlib.api.ui.reactive.widget.RGroup;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.RPopup;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.RScrollArea;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.RSlider;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.RWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

/**
 * A rendering layer that manages a collection of widgets at a specific z-level.
 * <p>
 * Layers handle:
 * <ul>
 *   <li>Widget lifecycle (add, remove, reorder)</li>
 *   <li>Rendering in correct z-order</li>
 *   <li>Input event dispatch (top-down)</li>
 *   <li>Optional modal blocking</li>
 * </ul>
 */
public class RenderLayer {
    
    private final int priority;
    private final String name;
    private final List<LayerEntry> entries = new ArrayList<>();
    private boolean modal = false;
    private boolean visible = true;
    private boolean blockInputWhenInside = true; // Block input propagation when click is inside widget bounds
    
    /**
     * Entry in the layer, wrapping a widget with additional metadata.
     */
    public static class LayerEntry {
        private final RWidget widget;
        private final Object key;
        private int zIndex;
        private boolean pinned; // Pinned entries stay even when not refreshed
        private boolean passThrough = false; // Allow input to pass through this entry
        
        public LayerEntry(RWidget widget, Object key, int zIndex) {
            this.widget = widget;
            this.key = key;
            this.zIndex = zIndex;
            this.pinned = false;
        }
        
        public RWidget widget() { return widget; }
        public Object key() { return key; }
        public int zIndex() { return zIndex; }
        public boolean isPinned() { return pinned; }
        public boolean isPassThrough() { return passThrough; }
        
        public LayerEntry setZIndex(int z) { this.zIndex = z; return this; }
        public LayerEntry setPinned(boolean pinned) { this.pinned = pinned; return this; }
        /** Sets whether input events pass through this entry to layers below. */
        public LayerEntry setPassThrough(boolean passThrough) { this.passThrough = passThrough; return this; }
    }
    
    public RenderLayer(int priority, String name) {
        this.priority = priority;
        this.name = name;
    }
    
    // ===== Configuration =====
    
    public int priority() { return priority; }
    public String name() { return name; }
    
    public boolean isModal() { return modal; }
    public RenderLayer setModal(boolean modal) { 
        this.modal = modal; 
        return this; 
    }
    
    public boolean isVisible() { return visible; }
    public RenderLayer setVisible(boolean visible) { 
        this.visible = visible; 
        return this; 
    }
    
    /**
     * Whether this layer blocks input propagation when click is inside widget bounds.
     * <p>
     * When true (default for overlay layers), clicking inside a widget in this layer
     * will NOT pass through to lower layers, even if the widget doesn't handle the event.
     * <p>
     * When false, clicks will pass through if the widget doesn't handle them.
     */
    public boolean isBlockInputWhenInside() { return blockInputWhenInside; }
    public RenderLayer setBlockInputWhenInside(boolean block) {
        this.blockInputWhenInside = block;
        return this;
    }
    
    // ===== Widget Management =====
    
    /**
     * Adds a widget to this layer.
     * 
     * @param widget the widget to add
     * @param key    unique key for caching/identification
     * @return the created entry
     */
    public LayerEntry add(RWidget widget, Object key) {
        return add(widget, key, 0);
    }
    
    /**
     * Adds a widget to this layer with specific z-index.
     * 
     * @param widget the widget to add
     * @param key    unique key for caching/identification
     * @param zIndex z-order within this layer (higher = on top)
     * @return the created entry
     */
    public LayerEntry add(RWidget widget, Object key, int zIndex) {
        // Check if already exists
        LayerEntry existing = findByKey(key);
        if (existing != null) {
            return existing;
        }
        
        LayerEntry entry = new LayerEntry(widget, key, zIndex);
        entries.add(entry);
        sortEntries();
        return entry;
    }
    
    /**
     * Removes a widget by key.
     */
    public boolean remove(Object key) {
        return entries.removeIf(e -> e.key.equals(key));
    }
    
    /**
     * Removes all widgets matching a predicate.
     */
    public void removeIf(Predicate<LayerEntry> predicate) {
        entries.removeIf(predicate);
    }
    
    /**
     * Clears all non-pinned entries.
     */
    public void clearUnpinned() {
        entries.removeIf(e -> !e.pinned);
    }
    
    /**
     * Clears all entries.
     */
    public void clear() {
        entries.clear();
    }
    
    /**
     * Finds an entry by key.
     */
    public @Nullable LayerEntry findByKey(Object key) {
        for (LayerEntry entry : entries) {
            if (entry.key.equals(key)) {
                return entry;
            }
        }
        return null;
    }
    
    /**
     * Brings a widget to the front (highest z-index in this layer).
     */
    public void bringToFront(Object key) {
        LayerEntry entry = findByKey(key);
        if (entry != null) {
            int maxZ = entries.stream().mapToInt(LayerEntry::zIndex).max().orElse(0);
            entry.setZIndex(maxZ + 1);
            sortEntries();
        }
    }
    
    /**
     * Sends a widget to the back (lowest z-index in this layer).
     */
    public void sendToBack(Object key) {
        LayerEntry entry = findByKey(key);
        if (entry != null) {
            int minZ = entries.stream().mapToInt(LayerEntry::zIndex).min().orElse(0);
            entry.setZIndex(minZ - 1);
            sortEntries();
        }
    }
    
    private void sortEntries() {
        entries.sort(Comparator.comparingInt(LayerEntry::zIndex));
    }
    
    // ===== Rendering =====
    
    /**
     * Renders all widgets in this layer.
     * <p>
     * Uses the layer's priority as a z-offset to ensure proper rendering order
     * in Minecraft's rendering system.
     */
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float delta) {
        if (!visible) return;
        
        // Push pose and translate Z based on priority to ensure overlay layers render on top
        graphics.pose().pushPose();
        graphics.pose().translate(0, 0, priority);
        
        for (LayerEntry entry : entries) {
            RWidget widget = entry.widget;
            if (widget.isVisible()) {
                widget.render(graphics, font, mouseX, mouseY, delta);
            }
        }
        
        graphics.pose().popPose();
    }
    
    // ===== Input Handling =====
    
    /**
     * Handles mouse click, dispatching to widgets in reverse z-order (top first).
     * <p>
     * Input blocking behavior is controlled by:
     * <ul>
     *   <li>{@link #setBlockInputWhenInside(boolean)} - layer-level setting</li>
     *   <li>{@link LayerEntry#setPassThrough(boolean)} - per-widget setting</li>
     * </ul>
     * <p>
     * By default, overlay layers (priority >= DROPDOWN) block input when mouse is inside
     * widget bounds, preventing clicks from passing through to content behind.
     * 
     * @return true if handled or if click was blocked
     */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        if (!visible) return false;
        
        // Process in reverse order (top widget first)
        for (int i = entries.size() - 1; i >= 0; i--) {
            LayerEntry entry = entries.get(i);
            RWidget widget = entry.widget;
            if (!widget.isVisible()) continue;
            
            // Check if click is inside this widget
            boolean inside = widget.contains(mouseX, mouseY);
            
            // Let the widget handle the click
            if (widget.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            
            // Block propagation if:
            // 1. Click is inside widget bounds
            // 2. Layer has blockInputWhenInside enabled (or is overlay layer by default)
            // 3. Entry doesn't have passThrough enabled
            if (inside && !entry.passThrough) {
                boolean shouldBlock = blockInputWhenInside || priority >= LayerPriority.DROPDOWN;
                if (shouldBlock) {
                    return true;
                }
            }
        }
        
        // Modal layers consume all clicks
        return modal;
    }
    
    /**
     * Handles mouse scroll.
     * <p>
     * Similar to mouseClicked, input blocking is controlled by layer and entry settings.
     */
    public boolean mouseScrolled(int mouseX, int mouseY, double scrollX, double scrollY) {
        if (!visible) return false;
        
        for (int i = entries.size() - 1; i >= 0; i--) {
            LayerEntry entry = entries.get(i);
            RWidget widget = entry.widget;
            if (!widget.isVisible()) continue;
            
            boolean inside = widget.contains(mouseX, mouseY);
            
            if (widget.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }
            
            // Block scroll propagation with same logic as mouseClicked
            if (inside && !entry.passThrough) {
                boolean shouldBlock = blockInputWhenInside || priority >= LayerPriority.DROPDOWN;
                if (shouldBlock) {
                    return true;
                }
            }
        }
        
        return modal;
    }
    
    /**
     * Propagates mouse drag to all widgets that support dragging.
     * Since RWidget base class doesn't have mouseDragged, we check for specific types.
     */
    public boolean mouseDragged(int mouseX, int mouseY, int button, double deltaX, double deltaY) {
        if (!visible) return false;
        
        for (int i = entries.size() - 1; i >= 0; i--) {
            LayerEntry entry = entries.get(i);
            if (!entry.widget.isVisible()) continue;
            
            // Check for widgets that support dragging
            RWidget widget = entry.widget;
            if (widget instanceof RPopup popup) {
                if (popup.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                    return true;
                }
            } else if (widget instanceof RScrollArea scrollArea) {
                if (scrollArea.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                    return true;
                }
            } else if (widget instanceof RSlider slider) {
                if (slider.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                    return true;
                }
            } else if (widget instanceof RFloating floating) {
                if (floating.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                    return true;
                }
            } else if (widget instanceof RGroup group) {
                // Recursively check group children
                if (propagateDragToGroup(group, mouseX, mouseY, button, deltaX, deltaY)) {
                    return true;
                }
            }
        }
        
        return false;
    }
    
    private boolean propagateDragToGroup(RGroup group, int mouseX, int mouseY, int button, double deltaX, double deltaY) {
        for (RWidget child : group.getChildren()) {
            if (!child.isVisible()) continue;
            
            if (child instanceof RPopup popup) {
                if (popup.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
            } else if (child instanceof RScrollArea scrollArea) {
                if (scrollArea.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
            } else if (child instanceof RSlider slider) {
                if (slider.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
            } else if (child instanceof RFloating floating) {
                if (floating.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) return true;
            } else if (child instanceof RGroup childGroup) {
                if (propagateDragToGroup(childGroup, mouseX, mouseY, button, deltaX, deltaY)) return true;
            }
        }
        return false;
    }
    
    /**
     * Propagates mouse release to all widgets, including children of groups.
     */
    public void mouseReleased(int mouseX, int mouseY, int button) {
        if (!visible) return;
        
        for (LayerEntry entry : entries) {
            if (entry.widget.isVisible()) {
                propagateReleaseToWidget(entry.widget, mouseX, mouseY, button);
            }
        }
    }
    
    private void propagateReleaseToWidget(RWidget widget, int mouseX, int mouseY, int button) {
        widget.mouseReleased(mouseX, mouseY, button);
        
        // Recursively release to children for group types
        if (widget instanceof RGroup group) {
            for (RWidget child : group.getChildren()) {
                if (child.isVisible()) {
                    propagateReleaseToWidget(child, mouseX, mouseY, button);
                }
            }
        } else if (widget instanceof RPopup popup) {
            RWidget content = popup.getContent();
            if (content != null && content.isVisible()) {
                propagateReleaseToWidget(content, mouseX, mouseY, button);
            }
        } else if (widget instanceof RScrollArea scrollArea) {
            RWidget content = scrollArea.getContent();
            if (content != null && content.isVisible()) {
                propagateReleaseToWidget(content, mouseX, mouseY, button);
            }
        }
    }
    
    // ===== Queries =====
    
    public int size() { return entries.size(); }
    public boolean isEmpty() { return entries.isEmpty(); }
    public List<LayerEntry> entries() { return List.copyOf(entries); }
}
