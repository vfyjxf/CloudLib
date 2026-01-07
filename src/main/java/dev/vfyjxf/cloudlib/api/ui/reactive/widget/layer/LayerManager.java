package dev.vfyjxf.cloudlib.api.ui.reactive.widget.layer;

import dev.vfyjxf.cloudlib.api.ui.reactive.widget.RGroup;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.RScrollArea;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.RWidget;
import net.minecraft.client.gui.Font;
import net.minecraft.client.gui.GuiGraphics;
import org.jetbrains.annotations.Nullable;

import java.util.*;

/**
 * Manages multiple rendering layers with proper z-ordering and input dispatch.
 * <p>
 * The LayerManager acts like a simple window manager:
 * <ul>
 *   <li>Maintains a sorted list of layers by priority</li>
 *   <li>Renders layers from back to front</li>
 *   <li>Dispatches input events from front to back</li>
 *   <li>Handles modal layers that block input to lower layers</li>
 * </ul>
 * 
 * <h2>Usage</h2>
 * <pre>{@code
 * LayerManager manager = new LayerManager();
 * 
 * // Get or create layers
 * RenderLayer baseLayer = manager.getOrCreateLayer(LayerPriority.BASE, "base");
 * RenderLayer popupLayer = manager.getOrCreateLayer(LayerPriority.POPUP, "popup");
 * 
 * // Add widgets to layers
 * baseLayer.add(mainContent, "main");
 * popupLayer.add(myPopup, popupState);
 * 
 * // In render loop
 * manager.render(graphics, font, mouseX, mouseY, delta);
 * 
 * // Input handling
 * manager.mouseClicked(mouseX, mouseY, button);
 * }</pre>
 */
public class LayerManager {
    
    private final List<RenderLayer> layers = new ArrayList<>();
    private final Map<Integer, RenderLayer> layersByPriority = new HashMap<>();
    private final Map<String, RenderLayer> layersByName = new HashMap<>();
    
    /** Cache for widgets keyed by their state object across layers. */
    private final Map<Object, CachedWidget> widgetCache = new HashMap<>();
    
    /** Keys that were accessed this frame. */
    private final Set<Object> activeKeys = new HashSet<>();
    
    /** Whether we're in a frame (between beginFrame and endFrame). */
    private boolean inFrame = false;
    
    /**
     * Cached widget entry with layer info.
     */
    private record CachedWidget(RWidget widget, int layerPriority, Object key) {}
    
    // ===== Layer Management =====
    
    /**
     * Gets or creates a layer at the given priority.
     * 
     * @param priority the layer priority (use LayerPriority constants)
     * @param name     display name for debugging
     * @return the layer
     */
    public RenderLayer getOrCreateLayer(int priority, String name) {
        RenderLayer layer = layersByPriority.get(priority);
        if (layer == null) {
            layer = new RenderLayer(priority, name);
            layers.add(layer);
            layersByPriority.put(priority, layer);
            layersByName.put(name, layer);
            sortLayers();
        }
        return layer;
    }
    
    /**
     * Gets a layer by name.
     */
    public @Nullable RenderLayer getLayer(String name) {
        return layersByName.get(name);
    }
    
    /**
     * Gets a layer by priority.
     */
    public @Nullable RenderLayer getLayer(int priority) {
        return layersByPriority.get(priority);
    }
    
    /**
     * Removes a layer.
     */
    public void removeLayer(int priority) {
        RenderLayer layer = layersByPriority.remove(priority);
        if (layer != null) {
            layers.remove(layer);
            layersByName.remove(layer.name());
        }
    }
    
    private void sortLayers() {
        layers.sort(Comparator.comparingInt(RenderLayer::priority));
    }
    
    // ===== Frame Management =====
    
    /**
     * Begins a new frame. Call before building/updating widgets.
     * Clears active keys tracking.
     */
    public void beginFrame() {
        inFrame = true;
        activeKeys.clear();
        
        // Clear unpinned entries from all layers
        for (RenderLayer layer : layers) {
            layer.clearUnpinned();
        }
    }
    
    /**
     * Ends the current frame. Call after building widgets.
     * Cleans up widgets that weren't used this frame.
     */
    public void endFrame() {
        inFrame = false;
        
        // Remove cached widgets that weren't accessed
        widgetCache.keySet().removeIf(key -> !activeKeys.contains(key));
    }
    
    // ===== Widget Caching =====
    
    /**
     * Gets or creates a cached widget.
     * 
     * @param key           unique cache key (e.g., state object)
     * @param layerPriority which layer to add to
     * @param factory       creates the widget if not cached
     * @return the widget (cached or newly created)
     */
    public RWidget getOrCreateWidget(Object key, int layerPriority, java.util.function.Supplier<RWidget> factory) {
        activeKeys.add(key);
        
        CachedWidget cached = widgetCache.get(key);
        if (cached != null && cached.layerPriority == layerPriority) {
            // Re-add to layer (layer clears unpinned each frame)
            RenderLayer layer = getOrCreateLayer(layerPriority, "layer-" + layerPriority);
            layer.add(cached.widget, key);
            return cached.widget;
        }
        
        // Create new widget
        RWidget widget = factory.get();
        widgetCache.put(key, new CachedWidget(widget, layerPriority, key));
        
        // Add to layer
        RenderLayer layer = getOrCreateLayer(layerPriority, "layer-" + layerPriority);
        layer.add(widget, key);
        
        return widget;
    }
    
    /**
     * Checks if a widget is cached.
     */
    public boolean hasCachedWidget(Object key) {
        return widgetCache.containsKey(key);
    }
    
    /**
     * Gets a cached widget without creating.
     */
    public @Nullable RWidget getCachedWidget(Object key) {
        CachedWidget cached = widgetCache.get(key);
        if (cached != null) {
            activeKeys.add(key);
            return cached.widget;
        }
        return null;
    }
    
    /**
     * Removes a cached widget.
     */
    public void removeWidget(Object key) {
        CachedWidget cached = widgetCache.remove(key);
        if (cached != null) {
            RenderLayer layer = layersByPriority.get(cached.layerPriority);
            if (layer != null) {
                layer.remove(key);
            }
        }
    }
    
    // ===== Rendering =====
    
    /**
     * Renders all layers in priority order (back to front).
     */
    public void render(GuiGraphics graphics, Font font, int mouseX, int mouseY, float delta) {
        for (RenderLayer layer : layers) {
            layer.render(graphics, font, mouseX, mouseY, delta);
        }
    }
    
    /**
     * Lays out widgets in all layers.
     */
    public void layout(Font font) {
        for (RenderLayer layer : layers) {
            for (RenderLayer.LayerEntry entry : layer.entries()) {
                RWidget widget = entry.widget();
                if (widget instanceof RGroup group) {
                    group.layout(font);
                } else if (widget instanceof RScrollArea scrollArea) {
                    scrollArea.layout(font);
                }
            }
        }
    }
    
    // ===== Input Handling =====
    
    /**
     * Handles mouse click, dispatching to layers from front to back.
     * Modal layers block input to lower layers.
     * 
     * @return true if handled
     */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        // Process layers from highest priority (front) to lowest (back)
        for (int i = layers.size() - 1; i >= 0; i--) {
            RenderLayer layer = layers.get(i);
            if (layer.mouseClicked(mouseX, mouseY, button)) {
                return true;
            }
            // Modal layer blocks further processing
            if (layer.isModal() && layer.isVisible() && !layer.isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Handles mouse scroll.
     */
    public boolean mouseScrolled(int mouseX, int mouseY, double scrollX, double scrollY) {
        for (int i = layers.size() - 1; i >= 0; i--) {
            RenderLayer layer = layers.get(i);
            if (layer.mouseScrolled(mouseX, mouseY, scrollX, scrollY)) {
                return true;
            }
            if (layer.isModal() && layer.isVisible() && !layer.isEmpty()) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Handles mouse drag.
     */
    public boolean mouseDragged(int mouseX, int mouseY, int button, double deltaX, double deltaY) {
        for (int i = layers.size() - 1; i >= 0; i--) {
            RenderLayer layer = layers.get(i);
            if (layer.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }
        return false;
    }
    
    /**
     * Handles mouse release.
     */
    public void mouseReleased(int mouseX, int mouseY, int button) {
        // Release is broadcast to all layers
        for (RenderLayer layer : layers) {
            layer.mouseReleased(mouseX, mouseY, button);
        }
    }
    
    // ===== Queries =====
    
    public List<RenderLayer> layers() { return List.copyOf(layers); }
    public int layerCount() { return layers.size(); }
    
    /**
     * Finds which layer contains a widget at the given position.
     */
    public @Nullable RenderLayer findLayerAt(int mouseX, int mouseY) {
        for (int i = layers.size() - 1; i >= 0; i--) {
            RenderLayer layer = layers.get(i);
            if (!layer.isVisible()) continue;
            
            for (RenderLayer.LayerEntry entry : layer.entries()) {
                if (entry.widget().isVisible() && entry.widget().contains(mouseX, mouseY)) {
                    return layer;
                }
            }
        }
        return null;
    }
}
