package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;
import java.util.function.Consumer;
import java.util.function.Supplier;

/**
 * Factory methods for creating render nodes with a Java-friendly API.
 * <p>
 * Supports two styles of building UI trees:
 * <p>
 * <b>1. Implicit scope (like Jetpack Compose):</b>
 * <pre>{@code
 * import static dev.vfyjxf.cloudlib.api.ui.reactive.Render.*;
 * 
 * Column(() -> {
 *     Text("Hello");
 *     Text("World");
 *     Row(() -> {
 *         Button("A", () -> {});
 *         Button("B", () -> {});
 *     });
 * });
 * }</pre>
 * <p>
 * <b>2. Explicit builder (for more control):</b>
 * <pre>{@code
 * Render.column($ -> {
 *     $.text("Hello");
 *     $.spacing(8);
 *     $.forEach(items, (item, i) -> Render.text(item));
 * });
 * }</pre>
 * <p>
 * <b>3. Varargs style:</b>
 * <pre>{@code
 * Render.column(
 *     Render.text("A"),
 *     Render.text("B")
 * );
 * }</pre>
 * <p>
 * For custom widgets not in the DSL, use {@code Child(widget(...))} in implicit scope.
 */
public final class Render {

    /** ThreadLocal for implicit scope building */
    private static final ThreadLocal<LayoutBuilder> CURRENT_BUILDER = new ThreadLocal<>();

    private Render() {}

    // ===== Scope Utilities =====

    /**
     * Gets the current builder in scope, or null if not in a build context.
     */
    static @Nullable LayoutBuilder currentBuilder() {
        return CURRENT_BUILDER.get();
    }

    /**
     * Adds a node to the current scope if in build context.
     * Returns true if added, false if no current scope.
     */
    private static boolean addToScope(RenderNode node) {
        LayoutBuilder builder = CURRENT_BUILDER.get();
        if (builder != null) {
            builder.add(node);
            return true;
        }
        return false;
    }

    /**
     * Builds content in isolation without affecting the parent scope.
     * Used by ScrollArea, Popup, Floating etc. to build their content
     * without accidentally adding it to the parent scope.
     *
     * @param contentBlock the block that builds the content
     * @return the built content as a Column node
     */
    private static RenderNode buildContentInIsolation(Runnable contentBlock) {
        LayoutBuilder parentBuilder = CURRENT_BUILDER.get();
        LayoutBuilder contentBuilder = new LayoutBuilder(LayoutType.COLUMN, null);
        
        CURRENT_BUILDER.set(contentBuilder);
        try {
            contentBlock.run();
        } finally {
            CURRENT_BUILDER.set(parentBuilder);
        }
        
        return contentBuilder.build();
    }

    // ===== Implicit Scope DSL (Compose-like) =====
    // Use with static import for cleanest syntax

    /**
     * Creates a column layout with implicit scope.
     * Children are added automatically when created inside the block.
     *
     * @param block the build block
     * @return a column group node
     */
    public static RenderNode Column(Runnable block) {
        return Column(null, block);
    }

    /**
     * Creates a column layout with style and implicit scope.
     *
     * @param style the style
     * @param block the build block
     * @return a column group node
     */
    public static RenderNode Column(@Nullable Style style, Runnable block) {
        return buildWithScope(LayoutType.COLUMN, style, block);
    }

    /**
     * Creates a row layout with implicit scope.
     *
     * @param block the build block
     * @return a row group node
     */
    public static RenderNode Row(Runnable block) {
        return Row(null, block);
    }

    /**
     * Creates a row layout with style and implicit scope.
     *
     * @param style the style
     * @param block the build block
     * @return a row group node
     */
    public static RenderNode Row(@Nullable Style style, Runnable block) {
        return buildWithScope(LayoutType.ROW, style, block);
    }

    /**
     * Creates a stack layout with implicit scope.
     *
     * @param block the build block
     * @return a stack group node
     */
    public static RenderNode Stack(Runnable block) {
        return Stack(null, block);
    }

    /**
     * Creates a stack layout with style and implicit scope.
     *
     * @param style the style
     * @param block the build block
     * @return a stack group node
     */
    public static RenderNode Stack(@Nullable Style style, Runnable block) {
        return buildWithScope(LayoutType.STACK, style, block);
    }

    private static RenderNode buildWithScope(LayoutType type, @Nullable Style style, Runnable block) {
        LayoutBuilder parentBuilder = CURRENT_BUILDER.get();
        LayoutBuilder newBuilder = new LayoutBuilder(type, style);
        
        CURRENT_BUILDER.set(newBuilder);
        
        try {
            block.run();
        } finally {
            CURRENT_BUILDER.set(parentBuilder);
        }
        
        RenderNode result = newBuilder.build();
        
        // Auto-add to parent scope if exists
        if (parentBuilder != null) {
            parentBuilder.add(result);
        }
        
        return result;
    }

    /**
     * Creates a text node. Auto-adds to current scope if in build context.
     *
     * @param text the text
     */
    public static void Text(String text) {
        addToScope(RenderNode.leaf("text", text));
    }

    /**
     * Creates a styled text node. Auto-adds to current scope.
     *
     * @param text  the text
     * @param style the style
     */
    public static void Text(String text, Style style) {
        addToScope(RenderNode.leaf("text", text, style));
    }

    /**
     * Creates a reactive text node. Auto-adds to current scope.
     *
     * @param text the text supplier
     */
    public static void Text(Supplier<String> text) {
        addToScope(RenderNode.dynamic(() -> RenderNode.leaf("text", text.get())));
    }

    /**
     * Creates a reactive styled text node. Auto-adds to current scope.
     *
     * @param text  the text supplier
     * @param style the style
     */
    public static void Text(Supplier<String> text, Style style) {
        addToScope(RenderNode.dynamic(() -> RenderNode.leaf("text", text.get(), style)));
    }

    /**
     * Creates a button node. Auto-adds to current scope.
     *
     * @param label   the label
     * @param onClick the click handler
     */
    public static void Button(String label, Runnable onClick) {
        addToScope(RenderNode.leaf("button", new ButtonProps(label, onClick)));
    }

    /**
     * Creates a styled button node. Auto-adds to current scope.
     *
     * @param label   the label
     * @param onClick the click handler
     * @param style   the style
     */
    public static void Button(String label, Runnable onClick, Style style) {
        addToScope(RenderNode.leaf("button", new ButtonProps(label, onClick), style));
    }

    /**
     * Creates an image node. Auto-adds to current scope.
     *
     * @param texture the texture path
     */
    public static void Image(String texture) {
        addToScope(RenderNode.leaf("image", texture));
    }

    /**
     * Creates a styled image node. Auto-adds to current scope.
     *
     * @param texture the texture path
     * @param style   the style
     */
    public static void Image(String texture, Style style) {
        addToScope(RenderNode.leaf("image", texture, style));
    }

    /**
     * Creates a spacer. Auto-adds to current scope.
     *
     * @param size the size in pixels
     */
    public static void Spacer(int size) {
        addToScope(RenderNode.leaf("spacer", size));
    }

    /**
     * Creates a flex spacer. Auto-adds to current scope.
     */
    public static void FlexSpacer() {
        addToScope(RenderNode.leaf("spacer", -1));
    }

    // ===== ScrollArea =====

    /**
     * Creates a scrollable area with implicit scope.
     * Auto-adds to current scope if in build context.
     *
     * @param height the visible height
     * @param block  the content block
     * @return a scroll area node
     */
    public static RenderNode ScrollArea(int height, Runnable block) {
        return ScrollArea(-1, height, new RenderNode.ScrollState(), block);
    }

    /**
     * Creates a scrollable area with shared state.
     * Auto-adds to current scope if in build context.
     *
     * @param height the visible height
     * @param state  the scroll state (for external control)
     * @param block  the content block
     * @return a scroll area node
     */
    public static RenderNode ScrollArea(int height, RenderNode.ScrollState state, Runnable block) {
        return ScrollArea(-1, height, state, block);
    }

    /**
     * Creates a scrollable area with explicit dimensions.
     * Auto-adds to current scope if in build context.
     *
     * @param width  the visible width (-1 for auto)
     * @param height the visible height
     * @param state  the scroll state
     * @param block  the content block
     * @return a scroll area node
     */
    public static RenderNode ScrollArea(int width, int height, RenderNode.ScrollState state, Runnable block) {
        // Build content in isolation - don't add to parent scope
        RenderNode content = buildContentInIsolation(block);
        RenderNode scrollArea = RenderNode.scrollArea(width, height, content, state);
        addToScope(scrollArea);
        return scrollArea;
    }
    
    // ===== Popup =====
    
    /**
     * Creates a popup with title, size and state.
     * Content is built using implicit scope, events are configured via callback.
     *
     * @param title        popup title
     * @param width        popup width
     * @param height       popup height
     * @param state        popup state for position persistence
     * @param configurator consumer to configure the popup events (called when widget is built)
     * @param contentBlock block to build popup content
     * @return a popup node
     */
    public static RenderNode Popup(String title, int width, int height, 
            dev.vfyjxf.cloudlib.api.ui.reactive.widget.RPopup.PopupState state, 
            PopupConfigurator configurator,
            Runnable contentBlock) {
        // Build content in isolation - don't add to parent scope
        RenderNode content = buildContentInIsolation(contentBlock);
        
        // Create popup node with configurator
        RenderNode popup = new RenderNode.PopupWithConfigurator(
            title, width, height, state, content, configurator
        );
        
        addToScope(popup);
        return popup;
    }
    
    /**
     * Creates a popup without event configuration.
     */
    public static RenderNode Popup(String title, int width, int height,
            dev.vfyjxf.cloudlib.api.ui.reactive.widget.RPopup.PopupState state,
            Runnable contentBlock) {
        // Build content in isolation - don't add to parent scope
        RenderNode content = buildContentInIsolation(contentBlock);
        
        RenderNode popup = RenderNode.popup(title, width, height, state, content);
        addToScope(popup);
        return popup;
    }
    
    /**
     * Configurator interface for popup setup.
     * Allows registering events and adding content to the popup.
     */
    @FunctionalInterface
    public interface PopupConfigurator {
        /**
         * Configure the popup.
         * Inside this method, use Child() to add content to the popup.
         * 
         * @param popup the popup widget to configure
         */
        void configure(dev.vfyjxf.cloudlib.api.ui.reactive.widget.RPopup popup);
    }
    
    // ===== Floating / Overlay =====
    
    /**
     * Creates a floating wrapper that doesn't affect parent layout.
     * 
     * <p>Floating widgets render at their absolute position but don't take up
     * space in the parent's layout calculation. Useful for:</p>
     * <ul>
     *   <li>Draggable panels within a container</li>
     *   <li>Tooltips positioned near elements</li>
     *   <li>Floating toolbars</li>
     * </ul>
     * 
     * <p>This uses {@link dev.vfyjxf.cloudlib.api.ui.reactive.widget.layer.LayoutMode#FLOATING}</p>
     * 
     * @param x initial x position
     * @param y initial y position
     * @param contentBlock block to build the floating content
     * @return a floating node
     */
    public static RenderNode Floating(int x, int y, Runnable contentBlock) {
        RenderNode content = buildContentInIsolation(contentBlock);
        RenderNode floating = RenderNode.floating(x, y, content);
        addToScope(floating);
        return floating;
    }
    
    /**
     * Creates a draggable floating wrapper.
     * 
     * @param x initial x position
     * @param y initial y position
     * @param draggable whether the content can be dragged
     * @param contentBlock block to build the floating content
     * @return a floating node
     */
    public static RenderNode Floating(int x, int y, boolean draggable, Runnable contentBlock) {
        RenderNode content = buildContentInIsolation(contentBlock);
        RenderNode floating = RenderNode.floating(x, y, draggable, content);
        addToScope(floating);
        return floating;
    }
    
    // ===== Group (alias for Stack) =====
    
    /**
     * Creates a group layout (stack) with implicit scope.
     * Children are stacked on top of each other, useful for overlays.
     *
     * @param block the build block
     * @return a stack group node
     */
    public static RenderNode Group(Runnable block) {
        return Stack(block);
    }

    /**
     * Adds a custom/pre-built node to current scope.
     * Use this for custom widgets not in the DSL.
     *
     * @param node the node to add
     */
    public static void Child(RenderNode node) {
        if (node != null) {
            addToScope(node);
        }
    }

    /**
     * Embeds a component in current scope.
     *
     * @param component the component
     */
    public static void Embed(Component component) {
        addToScope(RenderNode.component(component));
    }

    /**
     * Embeds a component with key in current scope.
     *
     * @param key       the key (can be String, Key, or any Object)
     * @param component the component
     */
    public static void Embed(Object key, Component component) {
        addToScope(RenderNode.component(key, component));
    }
    
    /**
     * Embeds a component with a typed Key in current scope.
     * <p>
     * Prefer using {@link Key} over raw strings for better type safety and features.
     * 
     * @param key       the typed key
     * @param component the component
     * @see Key#of(Object)
     * @see Key#unique()
     */
    public static void Embed(Key key, Component component) {
        addToScope(RenderNode.component(key, component));
    }

    // ===== Effects (use ComponentContext for these) =====
    // Effect APIs are removed - use ComponentContext.effect() instead
    // These were previously delegated to CompositionScope

    /**
     * Creates a keyed composition block.
     * <p>
     * Like Compose's {@code key(value) {}}.
     *
     * @param keyValue the key
     * @param block    the composition block
     */
    public static void Key(Object keyValue, Runnable block) {
        // TODO: Implement keyed rendering
        block.run();
    }

    // ===== Conditional & Iteration =====

    /**
     * Conditional rendering in current scope.
     *
     * @param condition the condition
     * @param block     the block to render if true
     */
    public static void If(boolean condition, Runnable block) {
        if (condition) {
            block.run();
        }
    }

    /**
     * Reactive conditional rendering in current scope.
     *
     * @param condition the condition supplier
     * @param whenTrue  the node when true
     */
    public static void When(Supplier<Boolean> condition, RenderNode whenTrue) {
        addToScope(RenderNode.showWhen(condition, whenTrue));
    }

    /**
     * Reactive conditional rendering with else branch.
     *
     * @param condition the condition supplier
     * @param whenTrue  the node when true
     * @param whenFalse the node when false
     */
    public static void When(Supplier<Boolean> condition, RenderNode whenTrue, RenderNode whenFalse) {
        addToScope(RenderNode.when(condition, whenTrue, whenFalse));
    }

    /**
     * For-each loop in current scope.
     *
     * @param items    the items
     * @param renderer the item renderer
     * @param <T>      the item type
     */
    public static <T> void ForEach(Iterable<T> items, RenderNode.ItemRenderer<T> renderer) {
        int index = 0;
        for (T item : items) {
            RenderNode node = renderer.render(item, index++);
            if (node != null) {
                addToScope(node);
            }
        }
    }

    /**
     * Reactive for-each in current scope.
     *
     * @param items    the items supplier
     * @param renderer the item renderer
     * @param <T>      the item type
     */
    public static <T> void ForEach(Supplier<? extends Iterable<T>> items, RenderNode.ItemRenderer<T> renderer) {
        addToScope(RenderNode.forEach(items, renderer));
    }

    // ===== Traditional API (lowercase) =====

    // ===== Basic Nodes =====

    /**
     * Creates a text node.
     *
     * @param text the static text
     * @return a text render node
     */
    public static RenderNode text(String text) {
        return RenderNode.leaf("text", text);
    }

    /**
     * Creates a text node with style.
     *
     * @param text  the static text
     * @param style the style
     * @return a text render node
     */
    public static RenderNode text(String text, Style style) {
        return RenderNode.leaf("text", text, style);
    }

    /**
     * Creates a reactive text node.
     *
     * @param text the text supplier
     * @return a dynamic text render node
     */
    public static RenderNode text(Supplier<String> text) {
        return RenderNode.dynamic(() -> RenderNode.leaf("text", text.get()));
    }

    /**
     * Creates a reactive text node with style.
     *
     * @param text  the text supplier
     * @param style the style
     * @return a dynamic text render node
     */
    public static RenderNode text(Supplier<String> text, Style style) {
        return RenderNode.dynamic(() -> RenderNode.leaf("text", text.get(), style));
    }

    /**
     * Creates a button node.
     *
     * @param label   the button label
     * @param onClick the click handler
     * @return a button render node
     */
    public static RenderNode button(String label, Runnable onClick) {
        return RenderNode.leaf("button", new ButtonProps(label, onClick));
    }

    /**
     * Creates a button node with style.
     *
     * @param label   the button label
     * @param onClick the click handler
     * @param style   the style
     * @return a button render node
     */
    public static RenderNode button(String label, Runnable onClick, Style style) {
        return RenderNode.leaf("button", new ButtonProps(label, onClick), style);
    }

    /**
     * Creates a reactive button node.
     *
     * @param label   the label supplier
     * @param onClick the click handler
     * @return a dynamic button render node
     */
    public static RenderNode button(Supplier<String> label, Runnable onClick) {
        return RenderNode.dynamic(() -> RenderNode.leaf("button", new ButtonProps(label.get(), onClick)));
    }

    /**
     * Creates a reactive button node with style.
     *
     * @param label   the label supplier
     * @param onClick the click handler
     * @param style   the style
     * @return a dynamic button render node
     */
    public static RenderNode button(Supplier<String> label, Runnable onClick, Style style) {
        return RenderNode.dynamic(() -> RenderNode.leaf("button", new ButtonProps(label.get(), onClick), style));
    }

    /**
     * Creates an image node.
     *
     * @param texture the texture path
     * @return an image render node
     */
    public static RenderNode image(String texture) {
        return RenderNode.leaf("image", texture);
    }

    /**
     * Creates an image node with style.
     *
     * @param texture the texture path
     * @param style   the style
     * @return an image render node
     */
    public static RenderNode image(String texture, Style style) {
        return RenderNode.leaf("image", texture, style);
    }

    /**
     * Creates a spacer node.
     *
     * @param size the size in pixels
     * @return a spacer render node
     */
    public static RenderNode spacer(int size) {
        return RenderNode.leaf("spacer", size);
    }

    /**
     * Creates a flexible spacer that fills available space.
     *
     * @return a flex spacer
     */
    public static RenderNode flexSpacer() {
        return RenderNode.leaf("spacer", -1);
    }

    // ===== Layout Nodes =====

    /**
     * Creates a column layout with varargs children.
     *
     * @param children the children
     * @return a column group node
     */
    public static RenderNode column(RenderNode... children) {
        return RenderNode.group(LayoutType.COLUMN, List.of(children));
    }

    /**
     * Creates a column layout with style and varargs children.
     *
     * @param style    the style
     * @param children the children
     * @return a column group node
     */
    public static RenderNode column(Style style, RenderNode... children) {
        return RenderNode.group(LayoutType.COLUMN, List.of(children), style);
    }

    /**
     * Creates a column layout with builder.
     *
     * @param builder the builder consumer
     * @return a column group node
     */
    public static RenderNode column(Consumer<LayoutBuilder> builder) {
        LayoutBuilder b = new LayoutBuilder(LayoutType.COLUMN);
        builder.accept(b);
        return b.build();
    }

    /**
     * Creates a column layout with style and builder.
     *
     * @param style   the style
     * @param builder the builder consumer
     * @return a column group node
     */
    public static RenderNode column(Style style, Consumer<LayoutBuilder> builder) {
        LayoutBuilder b = new LayoutBuilder(LayoutType.COLUMN, style);
        builder.accept(b);
        return b.build();
    }

    /**
     * Creates a row layout with varargs children.
     *
     * @param children the children
     * @return a row group node
     */
    public static RenderNode row(RenderNode... children) {
        return RenderNode.group(LayoutType.ROW, List.of(children));
    }

    /**
     * Creates a row layout with style and varargs children.
     *
     * @param style    the style
     * @param children the children
     * @return a row group node
     */
    public static RenderNode row(Style style, RenderNode... children) {
        return RenderNode.group(LayoutType.ROW, List.of(children), style);
    }

    /**
     * Creates a row layout with builder.
     *
     * @param builder the builder consumer
     * @return a row group node
     */
    public static RenderNode row(Consumer<LayoutBuilder> builder) {
        LayoutBuilder b = new LayoutBuilder(LayoutType.ROW);
        builder.accept(b);
        return b.build();
    }

    /**
     * Creates a row layout with style and builder.
     *
     * @param style   the style
     * @param builder the builder consumer
     * @return a row group node
     */
    public static RenderNode row(Style style, Consumer<LayoutBuilder> builder) {
        LayoutBuilder b = new LayoutBuilder(LayoutType.ROW, style);
        builder.accept(b);
        return b.build();
    }

    /**
     * Creates a stack layout with varargs children.
     *
     * @param children the children
     * @return a stack group node
     */
    public static RenderNode stack(RenderNode... children) {
        return RenderNode.group(LayoutType.STACK, List.of(children));
    }

    /**
     * Creates a stack layout with style and varargs children.
     *
     * @param style    the style
     * @param children the children
     * @return a stack group node
     */
    public static RenderNode stack(Style style, RenderNode... children) {
        return RenderNode.group(LayoutType.STACK, List.of(children), style);
    }

    /**
     * Creates a stack layout with builder.
     *
     * @param builder the builder consumer
     * @return a stack group node
     */
    public static RenderNode stack(Consumer<LayoutBuilder> builder) {
        LayoutBuilder b = new LayoutBuilder(LayoutType.STACK);
        builder.accept(b);
        return b.build();
    }

    /**
     * Creates a stack layout with style and builder.
     *
     * @param style   the style
     * @param builder the builder consumer
     * @return a stack group node
     */
    public static RenderNode stack(Style style, Consumer<LayoutBuilder> builder) {
        LayoutBuilder b = new LayoutBuilder(LayoutType.STACK, style);
        builder.accept(b);
        return b.build();
    }

    // ===== Conditional/Dynamic =====

    /**
     * Creates a conditional node.
     *
     * @param condition the condition supplier
     * @param whenTrue  the node when true
     * @return a conditional render node
     */
    public static RenderNode when(Supplier<Boolean> condition, RenderNode whenTrue) {
        return RenderNode.showWhen(condition, whenTrue);
    }

    /**
     * Creates a conditional node with else branch.
     *
     * @param condition the condition supplier
     * @param whenTrue  the node when true
     * @param whenFalse the node when false
     * @return a conditional render node
     */
    public static RenderNode when(Supplier<Boolean> condition, RenderNode whenTrue, RenderNode whenFalse) {
        return RenderNode.when(condition, whenTrue, whenFalse);
    }

    /**
     * Creates a for-each node.
     *
     * @param items    the items supplier
     * @param renderer the item renderer
     * @param <T>      the item type
     * @return a for-each render node
     */
    public static <T> RenderNode forEach(Supplier<? extends Iterable<T>> items, RenderNode.ItemRenderer<T> renderer) {
        return RenderNode.forEach(items, renderer);
    }

    /**
     * Creates a for-each node with key extraction.
     *
     * @param items     the items supplier
     * @param keyExtract the key extractor
     * @param renderer  the item renderer
     * @param <T>       the item type
     * @return a for-each render node
     */
    public static <T> RenderNode forEach(
            Supplier<? extends Iterable<T>> items,
            RenderNode.KeyExtractor<T> keyExtract,
            RenderNode.ItemRenderer<T> renderer
    ) {
        return RenderNode.forEach(items, keyExtract, renderer);
    }

    /**
     * Creates a dynamic node.
     *
     * @param supplier the render supplier
     * @return a dynamic render node
     */
    public static RenderNode dynamic(Supplier<RenderNode> supplier) {
        return RenderNode.dynamic(supplier);
    }

    // ===== Component Reference =====

    /**
     * Embeds a component.
     *
     * @param component the component
     * @return a component ref node
     */
    public static RenderNode embed(Component component) {
        return RenderNode.component(component);
    }

    /**
     * Embeds a component with key.
     *
     * @param key       the key
     * @param component the component
     * @return a component ref node
     */
    public static RenderNode embed(Object key, Component component) {
        return RenderNode.component(key, component);
    }

    // ===== Slots =====

    /**
     * Creates a named slot.
     *
     * @param name the slot name
     * @return a slot node
     */
    public static RenderNode slot(String name) {
        return new RenderNode.Slot(name, null);
    }

    /**
     * Creates a slot with fallback content.
     *
     * @param name     the slot name
     * @param fallback the fallback content
     * @return a slot node
     */
    public static RenderNode slot(String name, RenderNode fallback) {
        return new RenderNode.Slot(name, fallback);
    }

    // ===== Empty =====

    /**
     * Returns an empty render node.
     *
     * @return empty node
     */
    public static RenderNode empty() {
        return RenderNode.empty();
    }

    /**
     * Returns an empty render node with a style.
     * Useful for creating styled spacers or dividers.
     *
     * @param style the style to apply
     * @return empty node with style
     */
    public static RenderNode Empty(Style style) {
        // Empty with style is typically used for spacers/dividers
        // We create a leaf node that renders nothing but has size from style
        return new RenderNode.Leaf("spacer", style, null);
    }

    // ===== Helper Classes =====

    /**
     * Button properties.
     */
    public record ButtonProps(String label, Runnable onClick) {}

    /**
     * Builder for layout nodes with inline element creation.
     * <p>
     * Methods return void to enable a clean imperative style:
     * <pre>{@code
     * Render.column($ -> {
     *     $.text("Hello");
     *     $.button("Click", () -> doSomething());
     *     $.spacing(8);
     * });
     * }</pre>
     */
    public static final class LayoutBuilder {
        private final LayoutType type;
        private final Style style;
        private final List<RenderNode> children = new ArrayList<>();
        private int spacingValue = 0;

        LayoutBuilder(LayoutType type) {
            this(type, null);
        }

        LayoutBuilder(LayoutType type, Style style) {
            this.type = type;
            this.style = style;
        }

        // ===== Direct Node Creation =====

        /** Adds a text node. */
        public void text(String content) {
            children.add(Render.text(content));
        }

        /** Adds a text node with style. */
        public void text(String content, Style style) {
            children.add(Render.text(content, style));
        }

        /** Adds a reactive text node. */
        public void text(Supplier<String> content) {
            children.add(Render.text(content));
        }

        /** Adds a reactive text node with style. */
        public void text(Supplier<String> content, Style style) {
            children.add(Render.text(content, style));
        }

        /** Adds a button. */
        public void button(String label, Runnable onClick) {
            children.add(Render.button(label, onClick));
        }

        /** Adds a button with style. */
        public void button(String label, Runnable onClick, Style style) {
            children.add(Render.button(label, onClick, style));
        }

        /** Adds a reactive button. */
        public void button(Supplier<String> label, Runnable onClick) {
            children.add(Render.button(label, onClick));
        }

        /** Adds a reactive button with style. */
        public void button(Supplier<String> label, Runnable onClick, Style style) {
            children.add(Render.button(label, onClick, style));
        }

        /** Adds an image. */
        public void image(String texture) {
            children.add(Render.image(texture));
        }

        /** Adds an image with style. */
        public void image(String texture, Style style) {
            children.add(Render.image(texture, style));
        }

        /** Adds a spacer. */
        public void spacer(int size) {
            children.add(Render.spacer(size));
        }

        /** Adds a flex spacer. */
        public void flexSpacer() {
            children.add(Render.flexSpacer());
        }

        // ===== Nested Layouts =====

        /** Adds a column. */
        public void column(RenderNode... children) {
            this.children.add(Render.column(children));
        }

        /** Adds a column with style. */
        public void column(Style style, RenderNode... children) {
            this.children.add(Render.column(style, children));
        }

        /** Adds a column with builder. */
        public void column(Consumer<LayoutBuilder> builder) {
            this.children.add(Render.column(builder));
        }

        /** Adds a column with style and builder. */
        public void column(Style style, Consumer<LayoutBuilder> builder) {
            this.children.add(Render.column(style, builder));
        }

        /** Adds a row. */
        public void row(RenderNode... children) {
            this.children.add(Render.row(children));
        }

        /** Adds a row with style. */
        public void row(Style style, RenderNode... children) {
            this.children.add(Render.row(style, children));
        }

        /** Adds a row with builder. */
        public void row(Consumer<LayoutBuilder> builder) {
            this.children.add(Render.row(builder));
        }

        /** Adds a row with style and builder. */
        public void row(Style style, Consumer<LayoutBuilder> builder) {
            this.children.add(Render.row(style, builder));
        }

        /** Adds a stack. */
        public void stack(RenderNode... children) {
            this.children.add(Render.stack(children));
        }

        /** Adds a stack with style. */
        public void stack(Style style, RenderNode... children) {
            this.children.add(Render.stack(style, children));
        }

        /** Adds a stack with builder. */
        public void stack(Consumer<LayoutBuilder> builder) {
            this.children.add(Render.stack(builder));
        }

        /** Adds a stack with style and builder. */
        public void stack(Style style, Consumer<LayoutBuilder> builder) {
            this.children.add(Render.stack(style, builder));
        }

        // ===== Adding Pre-built Nodes =====

        /** Adds a pre-built node. */
        public void add(RenderNode node) {
            if (node != null) {
                children.add(node);
            }
        }

        /** Adds multiple pre-built nodes. */
        public void add(RenderNode... nodes) {
            for (RenderNode node : nodes) {
                if (node != null) {
                    children.add(node);
                }
            }
        }

        /** Embeds a component. */
        public void embed(Component component) {
            children.add(Render.embed(component));
        }

        /** Embeds a component with key. */
        public void embed(Object key, Component component) {
            children.add(Render.embed(key, component));
        }

        // ===== Conditional & Loop =====

        /** Conditionally adds content (static). */
        public void when(boolean condition, Runnable block) {
            if (condition) {
                block.run();
            }
        }

        /** Conditionally adds a node (static). */
        public void when(boolean condition, RenderNode node) {
            if (condition && node != null) {
                children.add(node);
            }
        }

        /** Conditionally adds content (reactive). */
        public void when(Supplier<Boolean> condition, Supplier<RenderNode> nodeSupplier) {
            children.add(RenderNode.showWhen(condition, RenderNode.dynamic(nodeSupplier)));
        }

        /** Conditionally adds a node (reactive). */
        public void when(Supplier<Boolean> condition, RenderNode whenTrue) {
            children.add(RenderNode.showWhen(condition, whenTrue));
        }

        /** Conditionally adds a node with else (reactive). */
        public void when(Supplier<Boolean> condition, RenderNode whenTrue, RenderNode whenFalse) {
            children.add(RenderNode.when(condition, whenTrue, whenFalse));
        }

        /** Iterates over items (static). */
        public <T> void forEach(Iterable<T> items, RenderNode.ItemRenderer<T> renderer) {
            int index = 0;
            for (T item : items) {
                RenderNode node = renderer.render(item, index++);
                if (node != null) {
                    children.add(node);
                }
            }
        }

        /** Iterates over items (reactive). */
        public <T> void forEach(Supplier<? extends Iterable<T>> items, RenderNode.ItemRenderer<T> renderer) {
            children.add(RenderNode.forEach(items, renderer));
        }

        /** Iterates over items with key (reactive). */
        public <T> void forEach(
                Supplier<? extends Iterable<T>> items,
                RenderNode.KeyExtractor<T> keyExtractor,
                RenderNode.ItemRenderer<T> renderer
        ) {
            children.add(RenderNode.forEach(items, keyExtractor, renderer));
        }

        // ===== Configuration =====

        /** Sets spacing between children. */
        public void spacing(int spacing) {
            this.spacingValue = spacing;
        }

        // ===== Build =====

        RenderNode build() {
            List<RenderNode> resultChildren;
            if (spacingValue > 0 && children.size() > 1) {
                List<RenderNode> spaced = new ArrayList<>();
                for (int i = 0; i < children.size(); i++) {
                    if (i > 0) {
                        spaced.add(Render.spacer(spacingValue));
                    }
                    spaced.add(children.get(i));
                }
                resultChildren = spaced;
            } else {
                resultChildren = new ArrayList<>(children);
            }
            
            if (style != null) {
                return RenderNode.group(type, resultChildren, style);
            }
            return RenderNode.group(type, resultChildren);
        }
    }
}
