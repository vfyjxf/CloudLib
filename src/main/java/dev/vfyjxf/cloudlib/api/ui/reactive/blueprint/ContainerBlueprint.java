package dev.vfyjxf.cloudlib.api.ui.reactive.blueprint;

import dev.vfyjxf.cloudlib.api.ui.reactive.Blueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.CompositeBlueprint;
import dev.vfyjxf.cloudlib.api.ui.reactive.CompositeElement;
import dev.vfyjxf.cloudlib.api.ui.reactive.Key;
import dev.vfyjxf.cloudlib.api.ui.reactive.ScopedReceiver;
import dev.vfyjxf.cloudlib.api.ui.reactive.UIElement;
import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.List;

/**
 * A container blueprint that wraps its child with padding, background, etc.
 * <p>
 * Example:
 * <pre>{@code
 * Container(
 *     padding(10),
 *     background(0xFF333333),
 *     () -> {
 *         Text("Padded content");
 *     }
 * );
 * }</pre>
 */
@ApiStatus.Experimental
public record ContainerBlueprint(
        List<Blueprint> children,
        double paddingTop,
        double paddingRight,
        double paddingBottom,
        double paddingLeft,
        int backgroundColor,
        double width,
        double height,
        @Nullable Key key
) implements CompositeBlueprint {

    /**
     * No background color.
     */
    public static final int NO_BACKGROUND = 0x00000000;

    /**
     * Auto-size value.
     */
    public static final double AUTO = -1;

    /**
     * Creates a container with default settings.
     *
     * @param children the child blueprints
     */
    public ContainerBlueprint(List<Blueprint> children) {
        this(children, 0, 0, 0, 0, NO_BACKGROUND, AUTO, AUTO, null);
    }

    @Override
    public List<Blueprint> getChildren() {
        return children;
    }

    @Override
    @Nullable
    public Key key() {
        return key;
    }

    // ========== Static DSL Methods ==========

    /**
     * Creates a simple container.
     *
     * @param builder the builder lambda
     * @return the created blueprint
     */
    public static ContainerBlueprint Container(Runnable builder) {
        List<Blueprint> children = ScopedReceiver.buildScope(builder);
        ContainerBlueprint blueprint = new ContainerBlueprint(children);
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a container with uniform padding.
     *
     * @param padding the padding on all sides
     * @param builder the builder lambda
     * @return the created blueprint
     */
    public static ContainerBlueprint Container(double padding, Runnable builder) {
        List<Blueprint> children = ScopedReceiver.buildScope(builder);
        ContainerBlueprint blueprint = new ContainerBlueprint(
                children, padding, padding, padding, padding, NO_BACKGROUND, AUTO, AUTO, null
        );
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a container with symmetric padding.
     *
     * @param horizontal the horizontal padding
     * @param vertical   the vertical padding
     * @param builder    the builder lambda
     * @return the created blueprint
     */
    public static ContainerBlueprint Container(double horizontal, double vertical, Runnable builder) {
        List<Blueprint> children = ScopedReceiver.buildScope(builder);
        ContainerBlueprint blueprint = new ContainerBlueprint(
                children, vertical, horizontal, vertical, horizontal, NO_BACKGROUND, AUTO, AUTO, null
        );
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a container with background color.
     *
     * @param backgroundColor the background color (ARGB)
     * @param builder         the builder lambda
     * @return the created blueprint
     */
    public static ContainerBlueprint ContainerWithBackground(int backgroundColor, Runnable builder) {
        List<Blueprint> children = ScopedReceiver.buildScope(builder);
        ContainerBlueprint blueprint = new ContainerBlueprint(
                children, 0, 0, 0, 0, backgroundColor, AUTO, AUTO, null
        );
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a container with fixed size.
     *
     * @param width   the fixed width
     * @param height  the fixed height
     * @param builder the builder lambda
     * @return the created blueprint
     */
    public static ContainerBlueprint SizedContainer(double width, double height, Runnable builder) {
        List<Blueprint> children = ScopedReceiver.buildScope(builder);
        ContainerBlueprint blueprint = new ContainerBlueprint(
                children, 0, 0, 0, 0, NO_BACKGROUND, width, height, null
        );
        ScopedReceiver.addToScope(blueprint);
        return blueprint;
    }

    /**
     * Creates a full-featured container using a builder.
     *
     * @return the container builder
     */
    public static Builder container() {
        return new Builder();
    }

    /**
     * Builder for ContainerBlueprint.
     */
    public static class Builder {
        private double paddingTop = 0;
        private double paddingRight = 0;
        private double paddingBottom = 0;
        private double paddingLeft = 0;
        private int backgroundColor = NO_BACKGROUND;
        private double width = AUTO;
        private double height = AUTO;
        @Nullable
        private Key key = null;

        public Builder padding(double all) {
            this.paddingTop = all;
            this.paddingRight = all;
            this.paddingBottom = all;
            this.paddingLeft = all;
            return this;
        }

        public Builder padding(double horizontal, double vertical) {
            this.paddingTop = vertical;
            this.paddingRight = horizontal;
            this.paddingBottom = vertical;
            this.paddingLeft = horizontal;
            return this;
        }

        public Builder padding(double top, double right, double bottom, double left) {
            this.paddingTop = top;
            this.paddingRight = right;
            this.paddingBottom = bottom;
            this.paddingLeft = left;
            return this;
        }

        public Builder background(int color) {
            this.backgroundColor = color;
            return this;
        }

        public Builder size(double width, double height) {
            this.width = width;
            this.height = height;
            return this;
        }

        public Builder width(double width) {
            this.width = width;
            return this;
        }

        public Builder height(double height) {
            this.height = height;
            return this;
        }

        public Builder key(Key key) {
            this.key = key;
            return this;
        }

        public ContainerBlueprint build(Runnable builder) {
            List<Blueprint> children = ScopedReceiver.buildScope(builder);
            ContainerBlueprint blueprint = new ContainerBlueprint(
                    children, paddingTop, paddingRight, paddingBottom, paddingLeft,
                    backgroundColor, width, height, key
            );
            ScopedReceiver.addToScope(blueprint);
            return blueprint;
        }
    }
}
