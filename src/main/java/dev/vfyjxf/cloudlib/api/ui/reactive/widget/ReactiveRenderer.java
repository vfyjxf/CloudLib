package dev.vfyjxf.cloudlib.api.ui.reactive.widget;

import dev.vfyjxf.cloudlib.api.ui.element.ElementTree;
import dev.vfyjxf.cloudlib.api.ui.reactive.Component;
import dev.vfyjxf.cloudlib.api.ui.reactive.ComponentContext;
import dev.vfyjxf.cloudlib.api.ui.reactive.LayoutType;
import dev.vfyjxf.cloudlib.api.ui.reactive.Providers;
import dev.vfyjxf.cloudlib.api.ui.reactive.Render;
import dev.vfyjxf.cloudlib.api.ui.reactive.RenderNode;
import dev.vfyjxf.cloudlib.api.ui.reactive.Style;
import dev.vfyjxf.cloudlib.api.ui.reactive.Signal;
import dev.vfyjxf.cloudlib.api.ui.reactive.Computed;
import dev.vfyjxf.cloudlib.api.ui.reactive.ReactiveState;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.event.RUIContext;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.event.RUIContext;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.layer.LayerManager;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.layer.LayerPriority;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.layer.RenderLayer;
import dev.vfyjxf.cloudlib.api.ui.reactive.widget.layer.RFloating;
import dev.vfyjxf.cloudlib.test.ui.DSLShowcaseScreen;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.Font;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Bridges the reactive RenderNode/ElementTree system to the RWidget rendering system.
 * <p>
 * This renderer converts RenderNode trees into RWidget trees for rendering.
 * It uses a {@link LayerManager} for proper z-ordering and window management.
 * <p>
 * Features:
 * <ul>
 *   <li>Multi-layer rendering with proper z-ordering</li>
 *   <li>Widget caching for efficient updates</li>
 *   <li>Support for popups, tooltips, modals with correct input handling</li>
 *   <li>Dynamic nodes that re-evaluate on each frame</li>
 *   <li>Full ElementTree integration with Signal tracking</li>
 * </ul>
 * <p>
 * Usage modes:
 * <ol>
 *   <li><b>Direct RenderNode</b>: Simple, re-builds widgets from RenderNode each frame</li>
 *   <li><b>With ElementTree</b>: Full reactivity, only updates dirty elements</li>
 * </ol>
 */
public class ReactiveRenderer {

    private final Font font;
    private @Nullable ElementTree elementTree;
    private @Nullable RWidget rootWidget;
    private @Nullable Supplier<RenderNode> rootNodeSupplier;
    
    /**
     * Layer manager for multi-layer rendering and input handling.
     */
    private final LayerManager layerManager = new LayerManager();
    
    // Pre-created standard layers
    private final RenderLayer baseLayer;
    private final RenderLayer dropdownLayer;
    private final RenderLayer popupLayer;
    private final RenderLayer tooltipLayer;
    private final RenderLayer modalLayer;
    
    /**
     * Legacy overlay layers (for backwards compatibility).
     * @deprecated Use {@link #layerManager} instead
     */
    @Deprecated
    private final List<List<RWidget>> overlayLayers = new ArrayList<>();
    
    /**
     * Cache for overlay widgets keyed by their state object.
     * This prevents re-creating popups every frame.
     */
    private final Map<Object, RWidget> overlayWidgetCache = new HashMap<>();
    
    /**
     * Set of overlay widget keys that are active this frame.
     */
    private final Set<Object> activeOverlayKeys = new HashSet<>();
    
    /**
     * Standard overlay layer indices.
     * @deprecated Use {@link LayerPriority} constants instead
     */
    @Deprecated
    public static final int LAYER_BASE = 0;
    @Deprecated
    public static final int LAYER_DROPDOWN = 1;
    @Deprecated
    public static final int LAYER_POPUP = 2;
    @Deprecated
    public static final int LAYER_TOOLTIP = 3;
    @Deprecated
    public static final int LAYER_MODAL = 4;

    public ReactiveRenderer(Font font) {
        this.font = font;
        
        // Initialize standard layers
        this.baseLayer = layerManager.getOrCreateLayer(LayerPriority.BASE, "base");
        this.dropdownLayer = layerManager.getOrCreateLayer(LayerPriority.DROPDOWN, "dropdown");
        this.popupLayer = layerManager.getOrCreateLayer(LayerPriority.POPUP, "popup");
        this.tooltipLayer = layerManager.getOrCreateLayer(LayerPriority.TOOLTIP, "tooltip");
        this.modalLayer = layerManager.getOrCreateLayer(LayerPriority.MODAL, "modal");
    }
    
    // ===== Layer Access =====
    
    /**
     * Gets the layer manager for advanced layer control.
     */
    public LayerManager layerManager() {
        return layerManager;
    }
    
    /**
     * Gets the base layer (normal UI content).
     */
    public RenderLayer baseLayer() { return baseLayer; }
    
    /**
     * Gets the popup layer.
     */
    public RenderLayer popupLayer() { return popupLayer; }
    
    /**
     * Gets the tooltip layer.
     */
    public RenderLayer tooltipLayer() { return tooltipLayer; }
    
    /**
     * Gets the modal layer.
     */
    public RenderLayer modalLayer() { return modalLayer; }

    // ===== Setup Methods =====

    /**
     * Sets up the renderer with an ElementTree for full reactive support.
     * The ElementTree tracks Signal dependencies and enables fine-grained updates.
     *
     * @param tree           the element tree
     * @param rootNodeSupplier supplier that creates the root RenderNode
     */
    public void attachElementTree(ElementTree tree, Supplier<RenderNode> rootNodeSupplier) {
        this.elementTree = tree;
        this.rootNodeSupplier = rootNodeSupplier;

        // Attach root to element tree - this enables Signal tracking
        // Build the initial RenderNode and attach it
        tree.attachRoot(rootNodeSupplier.get());
    }

    /**
     * Sets up the renderer with a direct RenderNode supplier.
     * This is simpler but doesn't provide fine-grained reactivity.
     *
     * @param rootNodeSupplier supplier that creates the root RenderNode
     */
    public void attachDirect(Supplier<RenderNode> rootNodeSupplier) {
        this.rootNodeSupplier = rootNodeSupplier;
        this.elementTree = null;
    }

    // ===== Rendering =====

    /**
     * Renders the UI at the given position.
     *
     * @param graphics the graphics context
     * @param x        left position
     * @param y        top position
     * @param mouseX   mouse X
     * @param mouseY   mouse Y
     * @param delta    partial tick
     */
    public void render(GuiGraphics graphics, int x, int y, int mouseX, int mouseY, float delta) {
        if (rootNodeSupplier == null) return;

        // Begin frame - clears unpinned entries and active key tracking
        layerManager.beginFrame();
        activeOverlayKeys.clear();

        // Always get fresh RenderNode from supplier to handle dynamic sizing
        RenderNode rootNode = rootNodeSupplier.get();

        if (rootNode == null) {
            layerManager.endFrame();
            return;
        }

        // Convert RenderNode tree to RWidget tree
        rootWidget = buildWidget(rootNode);

        if (rootWidget == null) {
            layerManager.endFrame();
            return;
        }

        // Position and layout the root widget
        rootWidget.setPos(x, y);
        if (rootWidget instanceof RGroup group) {
            group.layout(font);
        } else if (rootWidget instanceof RScrollArea scrollArea) {
            scrollArea.layout(font);
        }

        // Add root to base layer for rendering
        baseLayer.add(rootWidget, "root");

        // Layout all layers
        layerManager.layout(font);
        
        // Render all layers (back to front)
        layerManager.render(graphics, font, mouseX, mouseY, delta);
        
        // End frame - cleanup unused cached widgets
        layerManager.endFrame();
        cleanupInactiveOverlays();
    }
    
    /**
     * Clears all overlay layers (but keeps cache intact).
     * @deprecated Legacy method, use layerManager instead
     */
    @Deprecated
    private void clearOverlayLayers() {
        overlayLayers.clear();
    }
    
    /**
     * Removes cached overlay widgets that weren't used this frame.
     */
    private void cleanupInactiveOverlays() {
        overlayWidgetCache.keySet().removeIf(key -> !activeOverlayKeys.contains(key));
    }
    
    /**
     * Adds a widget to an overlay layer with caching support.
     * 
     * @param layer  the layer index (use LAYER_* constants)
     * @param key    the cache key (e.g., PopupState) for widget reuse
     * @param widget the widget to add
     * @deprecated Use {@link #addToLayer(int, Object, RWidget)} instead
     */
    @Deprecated
    public void addToOverlay(int layer, Object key, RWidget widget) {
        addToLayer(convertLegacyLayer(layer), key, widget);
    }
    
    /**
     * Adds a widget to a layer with caching support.
     * 
     * @param priority layer priority (use LayerPriority constants)
     * @param key      the cache key for widget reuse
     * @param widget   the widget to add
     */
    public void addToLayer(int priority, Object key, RWidget widget) {
        activeOverlayKeys.add(key);
        overlayWidgetCache.put(key, widget);
        
        RenderLayer layer = layerManager.getOrCreateLayer(priority, "layer-" + priority);
        layer.add(widget, key);
    }
    
    /**
     * Converts legacy LAYER_* constants to LayerPriority values.
     */
    private int convertLegacyLayer(int legacyLayer) {
        return switch (legacyLayer) {
            case LAYER_DROPDOWN -> LayerPriority.DROPDOWN;
            case LAYER_POPUP -> LayerPriority.POPUP;
            case LAYER_TOOLTIP -> LayerPriority.TOOLTIP;
            case LAYER_MODAL -> LayerPriority.MODAL;
            default -> LayerPriority.BASE;
        };
    }
    
    /**
     * Gets a cached overlay widget by key.
     */
    public @Nullable RWidget getCachedOverlay(Object key) {
        return overlayWidgetCache.get(key);
    }

    /**
     * Builds a widget from a RenderNode.
     */
    private @Nullable RWidget buildWidget(RenderNode node) {
        if (node == null) return null;

        return switch (node) {
            case RenderNode.Leaf leaf -> buildLeafWidget(leaf);
            case RenderNode.Group group -> buildGroupWidget(group);
            case RenderNode.Empty empty -> null;
            case RenderNode.ScrollArea scrollArea -> buildScrollAreaWidget(scrollArea);
            case RenderNode.PopupWithConfigurator popup -> buildPopupWidget(popup);
            case RenderNode.Popup popup -> buildPopupWidget(popup);
            case RenderNode.Floating floating -> buildFloatingWidget(floating);
            case RenderNode.Dynamic dynamic -> buildDynamicWidget(dynamic);
            case RenderNode.Conditional cond -> buildConditionalWidget(cond);
            case RenderNode.ForEach<?> forEach -> buildForEachWidget(forEach);
            case RenderNode.ComponentRef ref -> buildComponentRefWidget(ref);
            case RenderNode.Slot slot -> buildSlotWidget(slot);
        };
    }

    private @Nullable RWidget buildLeafWidget(RenderNode.Leaf leaf) {
        String type = leaf.type();
        Object content = leaf.content();
        Style style = leaf.style();

        return switch (type) {
            case "text" -> {
                String text = String.valueOf(content);
                RText widget = new RText(text);
                if (style != null) widget.setStyle(style);
                yield widget;
            }
            case "button" -> {
                if (content instanceof Render.ButtonProps props) {
                    RButton widget = new RButton(props.label(), props.onClick());
                    if (style != null) widget.setStyle(style);
                    yield widget;
                }
                yield null;
            }
            case "spacer" -> {
                int size = content instanceof Integer i ? i : 8;
                yield new RSpacer(size, size);
            }
            case "progress_bar" -> {
                if (content instanceof DSLShowcaseScreen.ProgressBarProps props) {
                    RProgressBar widget = new RProgressBar(props.width(), props.height());
                    widget.setProgress(props.progress());
                    widget.setLabel(props.label());
                    yield widget;
                }
                yield null;
            }
            case "slider" -> {
                if (content instanceof DSLShowcaseScreen.SliderProps props) {
                    RSlider widget = new RSlider(props.width(), props.height(), props.state());
                    widget.setLabel(props.label());
                    widget.setRange(0, 1);
                    yield widget;
                }
                yield null;
            }
            case "tree_view" -> {
                if (content instanceof DSLShowcaseScreen.TreeViewProps props) {
                    RTreeView widget = new RTreeView(props.width(), props.height());
                    widget.addRootNode(props.root());
                    yield widget;
                }
                yield null;
            }
            default -> {
                // Unknown type - render as text
                RText widget = new RText("[" + type + "]");
                yield widget;
            }
        };
    }

    private RGroup buildGroupWidget(RenderNode.Group group) {
        RGroup.Direction direction = switch (group.layout()) {
            case COLUMN -> RGroup.Direction.VERTICAL;
            case ROW -> RGroup.Direction.HORIZONTAL;
            case STACK, WRAP, ABSOLUTE, GRID -> RGroup.Direction.STACK;
        };

        RGroup widget = new RGroup(direction);
        // Default gap of 4 (but 0 for STACK since children overlap)
        widget.setGap(direction == RGroup.Direction.STACK ? 0 : 4);

        if (group.style() != null) {
            widget.setStyle(group.style());
        }

        for (RenderNode child : group.children()) {
            RWidget childWidget = buildWidget(child);
            if (childWidget != null) {
                widget.addChild(childWidget);
            }
        }

        return widget;
    }

    private @Nullable RWidget buildConditionalWidget(RenderNode.Conditional cond) {
        if (cond.condition().get()) {
            return buildWidget(cond.whenTrue());
        } else if (cond.whenFalse() != null) {
            return buildWidget(cond.whenFalse());
        }
        return null;
    }

    private <T> RWidget buildForEachWidget(RenderNode.ForEach<T> forEach) {
        RGroup group = new RGroup(RGroup.Direction.VERTICAL);
        group.setGap(4);

        int index = 0;
        for (T item : forEach.items().get()) {
            RenderNode itemNode = forEach.renderer().render(item, index++);
            RWidget itemWidget = buildWidget(itemNode);
            if (itemWidget != null) {
                group.addChild(itemWidget);
            }
        }

        return group;
    }

    private @Nullable RWidget buildSlotWidget(RenderNode.Slot slot) {
        RenderNode fallback = slot.fallback();
        if (fallback != null) {
            return buildWidget(fallback);
        }
        return null;
    }

    private RScrollArea buildScrollAreaWidget(RenderNode.ScrollArea scrollArea) {
        RScrollArea widget = new RScrollArea(scrollArea.width(), scrollArea.height(), scrollArea.state());

        RWidget content = buildWidget(scrollArea.content());
        if (content != null) {
            widget.setContent(content);
        }

        return widget;
    }
    
    private @Nullable RWidget buildPopupWidget(RenderNode.Popup popupNode) {
        // Use state as cache key
        RPopup.PopupState state = popupNode.state();
        
        // Check cache first
        RWidget cached = getCachedOverlay(state);
        if (cached instanceof RPopup cachedPopup) {
            // Reuse cached popup, just add to popup layer
            addToLayer(LayerPriority.POPUP, state, cachedPopup);
            return null;
        }
        
        // Create new popup widget
        RPopup widget = new RPopup(
            popupNode.title(), 
            popupNode.width(), 
            popupNode.height(), 
            state
        );
        
        // Build and set content
        RWidget content = buildWidget(popupNode.content());
        if (content != null) {
            widget.setContent(content);
        }
        
        // Add popup to popup layer with caching
        addToLayer(LayerPriority.POPUP, state, widget);
        
        // Return null - popup is rendered in overlay, not in normal tree position
        return null;
    }
    
    private @Nullable RWidget buildPopupWidget(RenderNode.PopupWithConfigurator popupNode) {
        // Use state as cache key
        RPopup.PopupState state = popupNode.state();
        
        // Check cache first
        RWidget cached = getCachedOverlay(state);
        if (cached instanceof RPopup cachedPopup) {
            // Reuse cached popup, just add to popup layer
            addToLayer(LayerPriority.POPUP, state, cachedPopup);
            return null;
        }
        
        // Create new popup widget
        RPopup widget = new RPopup(
            popupNode.title(), 
            popupNode.width(), 
            popupNode.height(), 
            state
        );
        
        // Build and set content
        RWidget content = buildWidget(popupNode.content());
        if (content != null) {
            widget.setContent(content);
        }
        
        // Apply configurator for event registration (only on first creation)
        if (popupNode.configurator() != null) {
            popupNode.configurator().configure(widget);
        }
        
        // Add popup to popup layer with caching
        addToLayer(LayerPriority.POPUP, state, widget);
        
        // Return null - popup is rendered in overlay, not in normal tree position
        return null;
    }
    
    /**
     * Builds an RFloating widget from a Floating node.
     * 
     * <p>Floating widgets are special - they don't affect parent layout because
     * {@link RFloating#measure} returns {0, 0}. They render at their absolute
     * position within the parent's coordinate space.</p>
     * 
     * <p>Unlike popups which are added to a separate layer, floating widgets
     * remain in the normal widget tree but just don't contribute to layout.</p>
     *
     * @param floatingNode the floating node
     * @return the RFloating widget (returned to parent for rendering, but won't affect layout)
     */
    private @Nullable RWidget buildFloatingWidget(RenderNode.Floating floatingNode) {
        // Build content
        RWidget content = buildWidget(floatingNode.content());
        if (content == null) {
            return null;
        }
        
        // Create floating wrapper
        RFloating widget = new RFloating(content);
        widget.setFloatPosition(floatingNode.x(), floatingNode.y());
        widget.setDraggable(floatingNode.draggable());
        
        // Return the widget - it will be added to parent but won't affect layout
        return widget;
    }

    private @Nullable RWidget buildDynamicWidget(RenderNode.Dynamic dynamic) {
        // Evaluate the dynamic node and build its content
        RenderNode actualNode = dynamic.supplier().get();
        return buildWidget(actualNode);
    }

    // ===== Interaction =====

    /**
     * Handles mouse click events.
     * Uses LayerManager for proper layer-based dispatch.
     *
     * @return true if handled
     */
    public boolean mouseClicked(int mouseX, int mouseY, int button) {
        return layerManager.mouseClicked(mouseX, mouseY, button);
    }

    /**
     * Handles mouse scroll events.
     * Uses LayerManager for proper layer-based dispatch.
     *
     * @return true if handled
     */
    public boolean mouseScrolled(int mouseX, int mouseY, double scrollX, double scrollY) {
        return layerManager.mouseScrolled(mouseX, mouseY, scrollX, scrollY);
    }

    /**
     * Handles mouse drag events.
     * Uses LayerManager for proper layer-based dispatch.
     *
     * @return true if handled
     */
    public boolean mouseDragged(int mouseX, int mouseY, int button, double deltaX, double deltaY) {
        return layerManager.mouseDragged(mouseX, mouseY, button, deltaX, deltaY);
    }

    private boolean propagateDrag(RWidget widget, int mouseX, int mouseY, int button, double deltaX, double deltaY) {
        if (widget == null) return false;

        if (widget instanceof RScrollArea scrollArea) {
            if (scrollArea.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }

        if (widget instanceof RSlider slider) {
            if (slider.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }

        if (widget instanceof RPopup popup) {
            if (popup.mouseDragged(mouseX, mouseY, button, deltaX, deltaY)) {
                return true;
            }
        }

        if (widget instanceof RGroup group) {
            for (RWidget child : group.getChildren()) {
                if (propagateDrag(child, mouseX, mouseY, button, deltaX, deltaY)) {
                    return true;
                }
            }
        }

        if (widget instanceof RPanel panel) {
            return propagateDrag(panel.getContent(), mouseX, mouseY, button, deltaX, deltaY);
        }

        return false;
    }

    /**
     * Handles mouse release events.
     * Uses LayerManager for proper layer-based dispatch.
     */
    public void mouseReleased(int mouseX, int mouseY, int button) {
        layerManager.mouseReleased(mouseX, mouseY, button);
    }
    
    /**
     * Handles tick events.
     * Call this from Screen.tick() to propagate tick events to all widgets.
     */
    public void tick() {
        // Fire tick on all component contexts (for ctx.onTick callbacks)
        for (SimpleComponentContext ctx : componentContexts.values()) {
            ctx.fireTick();
        }
        
        // Broadcast tick to widget tree
        if (rootWidget != null) {
            broadcastTick(rootWidget);
        }
    }
    
    private void broadcastTick(RWidget widget) {
        // Fire tick event on this widget
        widget.events().tick().invoker().onTick(RUIContext.forEvent(widget, 0, 0));
        
        // Recurse to children
        if (widget instanceof RGroup group) {
            for (RWidget child : group.getChildren()) {
                broadcastTick(child);
            }
        } else if (widget instanceof RPopup popup) {
            RWidget content = popup.getContent();
            if (content != null) {
                broadcastTick(content);
            }
        } else if (widget instanceof RScrollArea scrollArea) {
            RWidget content = scrollArea.getContent();
            if (content != null) {
                broadcastTick(content);
            }
        }
    }

    // ===== Utility =====

    /**
     * Updates all dynamic text widgets.
     * Call this when you know signals have changed but before rendering.
     */
    public void update() {
        if (rootWidget != null) {
            rootWidget.update();
        }
    }

    /**
     * Gets the current root widget (may be null before first render).
     */
    public @Nullable RWidget getRootWidget() {
        return rootWidget;
    }

    /**
     * Gets the ElementTree if attached.
     */
    public @Nullable ElementTree getElementTree() {
        return elementTree;
    }
    
    /**
     * Counts the total number of widgets in the current tree.
     */
    public int countWidgets() {
        return countWidgetsRecursive(rootWidget);
    }
    
    private int countWidgetsRecursive(RWidget widget) {
        if (widget == null) return 0;
        
        int count = 1;
        
        if (widget instanceof RGroup group) {
            for (RWidget child : group.getChildren()) {
                count += countWidgetsRecursive(child);
            }
        } else if (widget instanceof RScrollArea scrollArea) {
            count += countWidgetsRecursive(scrollArea.getContent());
        } else if (widget instanceof RPanel panel) {
            count += countWidgetsRecursive(panel.getContent());
        } else if (widget instanceof RPopup popup) {
            // Popup content is handled internally
        }
        
        return count;
    }
    
    // ===== Component Support =====
    
    /**
     * Cache of component contexts keyed by component key or identity.
     * This preserves state across rebuilds.
     */
    private final Map<Object, SimpleComponentContext> componentContexts = new HashMap<>();
    
    /**
     * Builds a widget from a ComponentRef node.
     */
    private @Nullable RWidget buildComponentRefWidget(RenderNode.ComponentRef ref) {
        Component component = ref.component();
        Object key = ref.key();
        
        // Get or create context for this component
        Object contextKey = key != null ? key : System.identityHashCode(component);
        SimpleComponentContext ctx = componentContexts.computeIfAbsent(
            contextKey, k -> new SimpleComponentContext());
        
        // Reset indices for this render pass
        ctx.prepareForRender();
        
        // Render the component
        RenderNode rendered = component.render(ctx);
        
        // Convert the rendered output to widget
        return buildWidget(rendered);
    }
    
    /**
     * Clears component state cache.
     * Call this when the screen is closed.
     */
    public void clearComponentState() {
        componentContexts.clear();
    }
    
    /**
     * Simple ComponentContext implementation for ReactiveRenderer.
     * <p>
     * This provides hook-style state management similar to React hooks.
     * State is preserved across renders by index.
     */
    private static class SimpleComponentContext implements ComponentContext {
        private final List<Signal<?>> signals = new ArrayList<>();
        private final List<Computed<?>> computeds = new ArrayList<>();
        private final List<Object> memos = new ArrayList<>();
        private final List<Object[]> memoDeps = new ArrayList<>();
        private final Map<String, RefImpl<?>> refs = new HashMap<>();
        private final List<Runnable> tickCallbacks = new ArrayList<>();
        
        private int signalIndex = 0;
        private int computedIndex = 0;
        private int memoIndex = 0;
        
        void prepareForRender() {
            signalIndex = 0;
            computedIndex = 0;
            memoIndex = 0;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <T> Signal<T> signal(T initialValue) {
            if (signalIndex < signals.size()) {
                return (Signal<T>) signals.get(signalIndex++);
            }
            Signal<T> signal = Signal.of(initialValue);
            signals.add(signal);
            signalIndex++;
            return signal;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <T> Computed<T> computed(Supplier<T> computation) {
            if (computedIndex < computeds.size()) {
                return (Computed<T>) computeds.get(computedIndex++);
            }
            Computed<T> computed = Computed.of(computation);
            computeds.add(computed);
            computedIndex++;
            return computed;
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <T> T memo(Supplier<T> factory, Object... dependencies) {
            if (memoIndex < memos.size()) {
                Object[] oldDeps = memoDeps.get(memoIndex);
                if (depsEqual(oldDeps, dependencies)) {
                    return (T) memos.get(memoIndex++);
                }
                // Dependencies changed, recompute
                T value = factory.get();
                memos.set(memoIndex, value);
                memoDeps.set(memoIndex, dependencies.clone());
                memoIndex++;
                return value;
            }
            // First time
            T value = factory.get();
            memos.add(value);
            memoDeps.add(dependencies.clone());
            memoIndex++;
            return value;
        }
        
        private boolean depsEqual(Object[] oldDeps, Object[] newDeps) {
            if (oldDeps.length != newDeps.length) return false;
            for (int i = 0; i < oldDeps.length; i++) {
                if (!java.util.Objects.equals(oldDeps[i], newDeps[i])) {
                    return false;
                }
            }
            return true;
        }
        
        @Override
        public void effect(Runnable effect) {
            // Simple: just run effects immediately (not ideal but works for basic cases)
            // A proper implementation would queue effects and run them after render
            effect.run();
        }
        
        @Override
        public void effect(Supplier<Runnable> effect) {
            Runnable cleanup = effect.get();
            // Cleanup would be called on unmount - simplified for now
        }
        
        @Override
        public void effect(Supplier<Runnable> effect, Object... dependencies) {
            // Simplified: just run effect
            effect.get();
        }
        
        @Override
        public void watch(ReactiveState<?> state, Runnable callback) {
            state.subscribe(ignored -> callback.run());
        }
        
        @Override
        public <T> void watch(ReactiveState<T> state, Consumer<T> callback) {
            state.subscribe(callback);
        }
        
        @Override
        public void onMount(Runnable callback) {
            // Run immediately for simplicity
            callback.run();
        }
        
        @Override
        public void onUnmount(Runnable callback) {
            // Would need to track for cleanup
        }
        
        @Override
        public void onTick(Runnable callback) {
            tickCallbacks.add(callback);
        }
        
        /**
         * Called every game tick - invokes all registered tick callbacks.
         */
        void fireTick() {
            for (Runnable callback : tickCallbacks) {
                callback.run();
            }
        }
        
        @Override
        @SuppressWarnings("unchecked")
        public <T> Ref<T> ref(String name) {
            return (Ref<T>) refs.computeIfAbsent(name, k -> new RefImpl<>());
        }
        
        @Override
        public <T> T provide(Providers.Key<T> key) {
            // Try to get from current Providers scope
            Providers.Scope scope = Providers.currentScope();
            return scope != null ? scope.get(key) : null;
        }
        
        @Override
        public void invalidate() {
            // In simple context, invalidation is handled by Signal subscriptions
            // No additional action needed
        }
    }
    
    /**
     * Simple Ref implementation.
     */
    private static class RefImpl<T> implements ComponentContext.Ref<T> {
        private T value;
        
        @Override
        public @Nullable T get() {
            return value;
        }
        
        @Override
        public void set(T value) {
            this.value = value;
        }
    }
}
