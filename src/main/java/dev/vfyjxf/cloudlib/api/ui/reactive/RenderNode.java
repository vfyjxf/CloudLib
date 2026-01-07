package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Represents a renderable node in the UI tree.
 * <p>
 * RenderNode is the output of component rendering. Unlike widgets,
 * render nodes are lightweight descriptors that describe what to paint.
 * <p>
 * Types of render nodes:
 * <ul>
 *   <li>{@link Empty} - Nothing to render</li>
 *   <li>{@link Leaf} - A single renderable element</li>
 *   <li>{@link Group} - Multiple children</li>
 *   <li>{@link Slot} - A slot for injected content</li>
 *   <li>{@link ComponentRef} - Reference to another component</li>
 *   <li>{@link Dynamic} - Reactive content that auto-updates</li>
 * </ul>
 */
public sealed interface RenderNode 
        permits RenderNode.Empty, RenderNode.Leaf, RenderNode.Group, 
                RenderNode.Slot, RenderNode.ComponentRef, RenderNode.Dynamic,
                RenderNode.Conditional, RenderNode.ForEach, RenderNode.ScrollArea,
                RenderNode.Popup, RenderNode.PopupWithConfigurator, RenderNode.Floating {

    /**
     * Empty render node singleton.
     */
    Empty EMPTY = new Empty();

    /**
     * Creates an empty render node.
     *
     * @return the empty node
     */
    static RenderNode empty() {
        return EMPTY;
    }

    /**
     * Creates a leaf render node.
     *
     * @param type    the render type
     * @param content the content/configuration
     * @return a leaf node
     */
    static Leaf leaf(String type, Object content) {
        return new Leaf(type, content, null);
    }

    /**
     * Creates a leaf render node with style.
     *
     * @param type    the render type
     * @param content the content/configuration
     * @param style   the style
     * @return a leaf node
     */
    static Leaf leaf(String type, Object content, Style style) {
        return new Leaf(type, content, style);
    }

    /**
     * Creates a group of children.
     *
     * @param layout   the layout type
     * @param children the children
     * @return a group node
     */
    static Group group(LayoutType layout, List<RenderNode> children) {
        return new Group(layout, children, null);
    }

    /**
     * Creates a group of children with style.
     *
     * @param layout   the layout type
     * @param children the children
     * @param style    the style
     * @return a group node
     */
    static Group group(LayoutType layout, List<RenderNode> children, Style style) {
        return new Group(layout, children, style);
    }

    /**
     * Creates a component reference.
     *
     * @param component the component
     * @return a component ref node
     */
    static ComponentRef component(Component component) {
        return new ComponentRef(null, component);
    }

    /**
     * Creates a component reference with key.
     *
     * @param key       the key for diffing
     * @param component the component
     * @return a component ref node
     */
    static ComponentRef component(@Nullable Object key, Component component) {
        return new ComponentRef(key, component);
    }

    /**
     * Creates a dynamic node that re-renders when dependencies change.
     *
     * @param supplier the render function
     * @return a dynamic node
     */
    static Dynamic dynamic(Supplier<RenderNode> supplier) {
        return new Dynamic(supplier);
    }

    /**
     * Creates a conditional node.
     *
     * @param condition the condition
     * @param whenTrue  the node when true
     * @param whenFalse the node when false
     * @return a conditional node
     */
    static Conditional when(Supplier<Boolean> condition, RenderNode whenTrue, @Nullable RenderNode whenFalse) {
        return new Conditional(condition, whenTrue, whenFalse);
    }

    /**
     * Creates a conditional node (show only when true).
     *
     * @param condition the condition
     * @param child     the node when true
     * @return a conditional node
     */
    static Conditional showWhen(Supplier<Boolean> condition, RenderNode child) {
        return new Conditional(condition, child, null);
    }

    /**
     * Creates a for-each node for lists.
     *
     * @param items   the items supplier
     * @param builder the item renderer
     * @param <T>     the item type
     * @return a for-each node
     */
    static <T> ForEach<T> forEach(Supplier<? extends Iterable<T>> items, ItemRenderer<T> builder) {
        return new ForEach<>(items, builder, null);
    }

    /**
     * Creates a for-each node with key extraction.
     *
     * @param items      the items supplier
     * @param keyExtract the key extractor
     * @param builder    the item renderer
     * @param <T>        the item type
     * @return a for-each node
     */
    static <T> ForEach<T> forEach(
            Supplier<? extends Iterable<T>> items,
            KeyExtractor<T> keyExtract,
            ItemRenderer<T> builder
    ) {
        return new ForEach<>(items, builder, keyExtract);
    }

    // ===== Node Types =====

    /**
     * Empty node - renders nothing.
     */
    record Empty() implements RenderNode {}

    /**
     * Leaf node - a single renderable element.
     *
     * @param type    the render type (e.g., "text", "button", "image")
     * @param content the content/props
     * @param style   the style (layout, visual, events, etc.)
     */
    record Leaf(String type, Object content, @Nullable Style style) implements RenderNode {}

    /**
     * Group node - multiple children with a layout.
     *
     * @param layout   the layout type
     * @param children the child nodes
     * @param style    the style (layout, visual, events, etc.)
     */
    record Group(LayoutType layout, List<RenderNode> children, @Nullable Style style) implements RenderNode {}

    /**
     * Slot node - injection point for external content.
     *
     * @param name     the slot name
     * @param fallback the fallback content
     */
    record Slot(String name, @Nullable RenderNode fallback) implements RenderNode {}

    /**
     * Component reference - embeds another component.
     *
     * @param key       optional key for diffing
     * @param component the component
     */
    record ComponentRef(@Nullable Object key, Component component) implements RenderNode {}

    /**
     * Dynamic node - content that re-evaluates.
     *
     * @param supplier the render supplier
     */
    record Dynamic(Supplier<RenderNode> supplier) implements RenderNode {}

    /**
     * Conditional node.
     *
     * @param condition the condition
     * @param whenTrue  rendered when true
     * @param whenFalse rendered when false (nullable)
     */
    record Conditional(
            Supplier<Boolean> condition,
            RenderNode whenTrue,
            @Nullable RenderNode whenFalse
    ) implements RenderNode {}

    /**
     * For-each node for rendering lists.
     *
     * @param items      the items to render
     * @param renderer   the item renderer
     * @param keyExtract optional key extractor for diffing
     * @param <T>        the item type
     */
    record ForEach<T>(
            Supplier<? extends Iterable<T>> items,
            ItemRenderer<T> renderer,
            @Nullable KeyExtractor<T> keyExtract
    ) implements RenderNode {}

    // ===== Helper Interfaces =====

    /**
     * Renders an item to a node.
     *
     * @param <T> the item type
     */
    @FunctionalInterface
    interface ItemRenderer<T> {
        RenderNode render(T item, int index);
    }

    /**
     * Extracts a key from an item.
     *
     * @param <T> the item type
     */
    @FunctionalInterface
    interface KeyExtractor<T> {
        Object getKey(T item);
    }

    // ===== ScrollArea =====

    /**
     * Creates a scrollable area.
     *
     * @param width   the visible width (-1 for auto)
     * @param height  the visible height
     * @param content the scrollable content
     * @return a scroll area node
     */
    static ScrollArea scrollArea(int width, int height, RenderNode content) {
        return new ScrollArea(width, height, content, new ScrollState());
    }

    /**
     * Creates a scrollable area with shared state.
     *
     * @param width   the visible width (-1 for auto)
     * @param height  the visible height
     * @param content the scrollable content
     * @param state   the scroll state (for external control)
     * @return a scroll area node
     */
    static ScrollArea scrollArea(int width, int height, RenderNode content, ScrollState state) {
        return new ScrollArea(width, height, content, state);
    }

    /**
     * Scrollable area node.
     *
     * @param width   visible width (-1 for auto/parent width)
     * @param height  visible height
     * @param content the content to scroll
     * @param state   the scroll state
     */
    record ScrollArea(int width, int height, RenderNode content, ScrollState state) implements RenderNode {}

    /**
     * Mutable scroll state that can be shared across renders.
     * Stores scroll position and drag state to persist across widget rebuilds.
     */
    class ScrollState {
        private int scrollX = 0;
        private int scrollY = 0;
        private int contentWidth = 0;
        private int contentHeight = 0;
        
        // Drag state - stored here to persist across widget rebuilds
        private boolean dragging = false;
        private int dragStartY = 0;
        private int dragStartScroll = 0;
        
        // Configuration
        private boolean draggable = false;

        public int getScrollX() { return scrollX; }
        public int getScrollY() { return scrollY; }
        public int getContentWidth() { return contentWidth; }
        public int getContentHeight() { return contentHeight; }

        public void setScrollX(int x) { this.scrollX = x; }
        public void setScrollY(int y) { this.scrollY = y; }
        public void setContentSize(int width, int height) {
            this.contentWidth = width;
            this.contentHeight = height;
        }

        public void scrollBy(int dx, int dy) {
            this.scrollX += dx;
            this.scrollY += dy;
        }

        public void clamp(int viewWidth, int viewHeight) {
            scrollX = Math.max(0, Math.min(scrollX, Math.max(0, contentWidth - viewWidth)));
            scrollY = Math.max(0, Math.min(scrollY, Math.max(0, contentHeight - viewHeight)));
        }
        
        // Drag state accessors
        public boolean isDragging() { return dragging; }
        public void setDragging(boolean dragging) { this.dragging = dragging; }
        public int getDragStartY() { return dragStartY; }
        public void setDragStartY(int y) { this.dragStartY = y; }
        public int getDragStartScroll() { return dragStartScroll; }
        public void setDragStartScroll(int scroll) { this.dragStartScroll = scroll; }
        
        // Configuration accessors
        public boolean isDraggable() { return draggable; }
        public ScrollState setDraggable(boolean draggable) { 
            this.draggable = draggable; 
            return this;
        }
    }
    
    // ===== Popup =====
    
    /**
     * Creates a popup node.
     *
     * @param title   popup title
     * @param width   popup width
     * @param height  popup height
     * @param state   popup state for position persistence
     * @param content popup content
     * @return a popup node
     */
    static RenderNode popup(String title, int width, int height, 
            dev.vfyjxf.cloudlib.api.ui.reactive.widget.RPopup.PopupState state, 
            RenderNode content) {
        return new Popup(title, width, height, state, content);
    }
    
    /**
     * A popup node that creates an RPopup widget.
     */
    record Popup(String title, int width, int height, 
            dev.vfyjxf.cloudlib.api.ui.reactive.widget.RPopup.PopupState state, 
            RenderNode content) implements RenderNode {}
    
    /**
     * Extended popup node that carries a configurator for event registration.
     */
    record PopupWithConfigurator(
            String title, int width, int height,
            dev.vfyjxf.cloudlib.api.ui.reactive.widget.RPopup.PopupState state,
            RenderNode content,
            Render.PopupConfigurator configurator
    ) implements RenderNode {}
    
    // ===== Floating =====
    
    /**
     * Creates a floating node that doesn't affect parent layout.
     *
     * @param x       initial x position
     * @param y       initial y position
     * @param content the floating content
     * @return a floating node
     */
    static RenderNode floating(int x, int y, RenderNode content) {
        return new Floating(x, y, false, content);
    }
    
    /**
     * Creates a floating node with draggable option.
     *
     * @param x         initial x position
     * @param y         initial y position
     * @param draggable whether the content can be dragged
     * @param content   the floating content
     * @return a floating node
     */
    static RenderNode floating(int x, int y, boolean draggable, RenderNode content) {
        return new Floating(x, y, draggable, content);
    }
    
    /**
     * A floating node that creates an RFloating widget.
     * 
     * <p>Floating widgets render at their position but don't affect parent layout
     * measurements - the parent measures as if this widget doesn't exist.</p>
     */
    record Floating(int x, int y, boolean draggable, RenderNode content) implements RenderNode {}
}
