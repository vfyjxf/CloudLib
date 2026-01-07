package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.Nullable;

import java.util.function.Consumer;
import java.util.function.Function;

/**
 * A component is a reusable UI building block.
 * <p>
 * Unlike Flutter where "everything is a widget", Components are:
 * <ul>
 *   <li><b>Only for UI</b> - Data passing uses {@link Providers}</li>
 *   <li><b>Functions</b> - Components are render functions, not class hierarchies</li>
 *   <li><b>Flat</b> - No deep nesting required</li>
 * </ul>
 * <p>
 * Component Types:
 * <ul>
 *   <li>{@link #pure(RenderNode)} - Static content, no state</li>
 *   <li>{@link #stateless(Function)} - Depends on props only</li>
 *   <li>{@link #stateful(Function)} - Has internal state</li>
 * </ul>
 * <p>
 * Example:
 * <pre>{@code
 * // Stateless - pure function of props
 * Component greeting = Component.stateless(ctx -> {
 *     String name = ctx.get(NameProvider.class);
 *     return Render.text("Hello, " + name);
 * });
 * 
 * // Stateful - has internal state
 * Component counter = Component.stateful(ctx -> {
 *     var count = ctx.signal(0);
 *     
 *     return Render.column(col -> col
 *         .child(Render.text(() -> "Count: " + count.get()))
 *         .child(Render.button("++", () -> count.update(n -> n + 1)))
 *     );
 * });
 * }</pre>
 */
public sealed interface Component permits Component.Pure, Component.Stateless, Component.Stateful, Component.Styled {

    /**
     * Creates a pure component with static content.
     *
     * @param content the static render node
     * @return a pure component
     */
    static Component pure(RenderNode content) {
        return new Pure(content);
    }

    /**
     * Creates a stateless component.
     *
     * @param render the render function
     * @return a stateless component
     */
    static Component stateless(Function<ComponentContext, RenderNode> render) {
        return new Stateless(render);
    }

    /**
     * Creates a stateful component.
     *
     * @param render the render function with state hooks
     * @return a stateful component
     */
    static Component stateful(Function<ComponentContext, RenderNode> render) {
        return new Stateful(render);
    }

    /**
     * Creates a component builder for complex configuration.
     *
     * @return a new builder
     */
    static Builder builder() {
        return new Builder();
    }

    /**
     * Renders this component with the given context.
     *
     * @param ctx the component context
     * @return the render node
     */
    RenderNode render(ComponentContext ctx);

    /**
     * Whether this component has internal state.
     *
     * @return true if stateful
     */
    boolean isStateful();

    /**
     * Wraps this component with a style.
     *
     * @param style the style to apply
     * @return a wrapped component
     */
    default Component with(Style style) {
        return new Styled(this, style);
    }

    // ===== Component Types =====

    /**
     * Pure component with static content.
     */
    record Pure(RenderNode content) implements Component {
        @Override
        public RenderNode render(ComponentContext ctx) {
            return content;
        }

        @Override
        public boolean isStateful() {
            return false;
        }
    }

    /**
     * Stateless component (no internal state).
     */
    record Stateless(Function<ComponentContext, RenderNode> renderFn) implements Component {
        @Override
        public RenderNode render(ComponentContext ctx) {
            return renderFn.apply(ctx);
        }

        @Override
        public boolean isStateful() {
            return false;
        }
    }

    /**
     * Stateful component (has internal state via hooks).
     */
    record Stateful(Function<ComponentContext, RenderNode> renderFn) implements Component {
        @Override
        public RenderNode render(ComponentContext ctx) {
            return renderFn.apply(ctx);
        }

        @Override
        public boolean isStateful() {
            return true;
        }
    }

    /**
     * Component with style applied.
     */
    final class Styled implements Component {
        private final Component inner;
        private final Style style;

        Styled(Component inner, Style style) {
            this.inner = inner;
            this.style = style;
        }

        @Override
        public RenderNode render(ComponentContext ctx) {
            RenderNode node = inner.render(ctx);
            // Apply style to the root node of the component
            return applyStyleToNode(node, style);
        }

        private RenderNode applyStyleToNode(RenderNode node, Style style) {
            if (style == null || style == Style.NONE) return node;
            return switch (node) {
                case RenderNode.Leaf leaf -> new RenderNode.Leaf(
                    leaf.type(), 
                    leaf.content(), 
                    leaf.style() != null ? leaf.style().with(style) : style
                );
                case RenderNode.Group group -> new RenderNode.Group(
                    group.layout(), 
                    group.children(), 
                    group.style() != null ? group.style().with(style) : style
                );
                default -> node; // For other node types, return as-is
            };
        }

        @Override
        public boolean isStateful() {
            return inner.isStateful();
        }

        public Component inner() {
            return inner;
        }

        public Style style() {
            return style;
        }
    }

    // ===== Builder =====

    /**
     * Builder for creating components with configuration.
     */
    final class Builder {
        @Nullable
        private Object key;
        @Nullable
        private Style style;
        @Nullable
        private Function<ComponentContext, RenderNode> render;
        private boolean stateful = false;

        Builder() {}

        /**
         * Sets a key for diffing.
         *
         * @param key the key
         * @return this builder
         */
        public Builder key(Object key) {
            this.key = key;
            return this;
        }

        /**
         * Adds a style.
         *
         * @param style the style
         * @return this builder
         */
        public Builder style(Style style) {
            this.style = this.style == null ? style : this.style.with(style);
            return this;
        }

        /**
         * Sets as stateful.
         *
         * @return this builder
         */
        public Builder stateful() {
            this.stateful = true;
            return this;
        }

        /**
         * Sets the render function.
         *
         * @param render the render function
         * @return this builder
         */
        public Builder render(Function<ComponentContext, RenderNode> render) {
            this.render = render;
            return this;
        }

        /**
         * Builds the component.
         *
         * @return the built component
         */
        public Component build() {
            if (render == null) {
                throw new IllegalStateException("Render function must be set");
            }

            Component component = stateful
                    ? new Stateful(render)
                    : new Stateless(render);

            if (style != null) {
                component = component.with(style);
            }

            return component;
        }
    }
}
