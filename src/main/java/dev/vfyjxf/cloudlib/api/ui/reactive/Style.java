package dev.vfyjxf.cloudlib.api.ui.reactive;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.List;

/**
 * Immutable style configuration for render nodes.
 * <p>
 * Style is a separate type from render nodes, allowing:
 * <ul>
 *   <li>Style reuse across multiple nodes</li>
 *   <li>Style composition and inheritance</li>
 *   <li>Clean separation between content and presentation</li>
 * </ul>
 * <p>
 * Example:
 * <pre>{@code
 * import static dev.vfyjxf.cloudlib.api.ui.reactive.Style.*;
 * 
 * // Define reusable styles
 * var cardStyle = Style.of(
 *     padding(12),
 *     background(0xFFFFFFFF),
 *     border(1, 0xFF666666),
 *     rounded(4)
 * );
 * 
 * var buttonStyle = Style.of(
 *     padding(8, 16),
 *     background(0xFF0066CC),
 *     rounded(4),
 *     cursor(Cursor.HAND)
 * );
 * 
 * // Use styles
 * Render.text("Hello", cardStyle);
 * Render.button("Click", onClick, buttonStyle);
 * 
 * // Compose styles
 * var hoverStyle = buttonStyle.with(background(0xFF0077DD));
 * 
 * // Conditional styling
 * Render.text("Item", isSelected ? selectedStyle : normalStyle);
 * }</pre>
 */
public final class Style {

    /** Empty style singleton */
    public static final Style NONE = new Style(List.of());

    private final List<Property> properties;

    private Style(List<Property> properties) {
        this.properties = properties;
    }

    // ===== Factory Methods =====

    /**
     * Creates a style from properties.
     *
     * @param properties the style properties
     * @return a new style
     */
    public static Style of(Property... properties) {
        if (properties.length == 0) return NONE;
        return new Style(List.of(properties));
    }

    /**
     * Combines this style with additional properties.
     *
     * @param properties additional properties (override existing)
     * @return a new combined style
     */
    public Style with(Property... properties) {
        if (properties.length == 0) return this;
        var combined = new ArrayList<>(this.properties);
        Collections.addAll(combined, properties);
        return new Style(List.copyOf(combined));
    }

    /**
     * Combines this style with another style.
     *
     * @param other the other style (its properties override)
     * @return a new combined style
     */
    public Style with(Style other) {
        if (other == NONE) return this;
        if (this == NONE) return other;
        var combined = new ArrayList<>(this.properties);
        combined.addAll(other.properties);
        return new Style(List.copyOf(combined));
    }

    /**
     * Gets all properties.
     *
     * @return unmodifiable list of properties
     */
    public List<Property> properties() {
        return properties;
    }

    /**
     * Gets a property by type.
     *
     * @param type the property class
     * @param <T>  the property type
     * @return the property or null
     */
    @SuppressWarnings("unchecked")
    public <T extends Property> @Nullable T get(Class<T> type) {
        // Return last matching (allows override)
        for (int i = properties.size() - 1; i >= 0; i--) {
            Property p = properties.get(i);
            if (type.isInstance(p)) {
                return (T) p;
            }
        }
        return null;
    }

    /**
     * Checks if this style has a property type.
     *
     * @param type the property class
     * @return true if present
     */
    public boolean has(Class<? extends Property> type) {
        return get(type) != null;
    }

    // ===== Property Definitions =====

    /** Base interface for all style properties */
    public sealed interface Property permits
            Padding, Size, MinSize, MaxSize,
            Background, Border, Rounded,
            Margin, Offset, Opacity, ZIndex,
            Clickable, Hoverable, Tooltip, Cursor,
            Id, Visible, Clip,
            // New properties
            Color, FontSize, Bold, Strikethrough, TextAlign,
            Flex, Gap, Align, Shadow, BorderBottom,
            FillParent, Wrap, MarginTop, MarginBottom, MinWidth {
    }

    // --- Layout Properties ---

    public record Padding(int left, int top, int right, int bottom) implements Property {
        public Padding(int all) { this(all, all, all, all); }
        public Padding(int horizontal, int vertical) { this(horizontal, vertical, horizontal, vertical); }
    }

    public record Margin(int left, int top, int right, int bottom) implements Property {
        public Margin(int all) { this(all, all, all, all); }
        public Margin(int horizontal, int vertical) { this(horizontal, vertical, horizontal, vertical); }
    }

    public record Size(int width, int height) implements Property {
        public static Size width(int w) { return new Size(w, -1); }
        public static Size height(int h) { return new Size(-1, h); }
    }

    public record MinSize(int width, int height) implements Property {}
    public record MaxSize(int width, int height) implements Property {}
    public record Offset(int x, int y) implements Property {}

    // --- Visual Properties ---

    public record Background(int color) implements Property {}
    public record Border(int width, int color) implements Property {}
    public record Rounded(int radius) implements Property {
        public Rounded(int topLeft, int topRight, int bottomRight, int bottomLeft) {
            this(topLeft); // Simplified for now
        }
    }
    public record Opacity(float value) implements Property {}
    public record ZIndex(int value) implements Property {}
    public record Visible(boolean value) implements Property {}
    public record Clip(boolean value) implements Property {}

    // --- Interaction Properties ---

    public record Clickable(Runnable onClick) implements Property {}
    public record Hoverable(Runnable onEnter, @Nullable Runnable onExit) implements Property {}
    public record Tooltip(String text) implements Property {}
    public record Cursor(CursorType type) implements Property {}

    public enum CursorType { DEFAULT, HAND, TEXT, MOVE, RESIZE_H, RESIZE_V, POINTER }

    // --- Metadata ---

    public record Id(String value) implements Property {}

    // ===== Static Factory Methods for Properties =====
    // Use with static import: import static ...Style.*;

    public static Padding padding(int all) {
        return new Padding(all);
    }

    public static Padding padding(int horizontal, int vertical) {
        return new Padding(horizontal, vertical);
    }

    public static Padding padding(int left, int top, int right, int bottom) {
        return new Padding(left, top, right, bottom);
    }

    public static Margin margin(int all) {
        return new Margin(all);
    }

    public static Margin margin(int horizontal, int vertical) {
        return new Margin(horizontal, vertical);
    }

    public static Margin margin(int left, int top, int right, int bottom) {
        return new Margin(left, top, right, bottom);
    }

    public static Size size(int width, int height) {
        return new Size(width, height);
    }

    public static Size width(int width) {
        return Size.width(width);
    }

    public static Size height(int height) {
        return Size.height(height);
    }

    public static MinSize minSize(int width, int height) {
        return new MinSize(width, height);
    }

    public static MaxSize maxSize(int width, int height) {
        return new MaxSize(width, height);
    }

    public static Offset offset(int x, int y) {
        return new Offset(x, y);
    }

    public static Background background(int color) {
        return new Background(color);
    }

    public static Border border(int width, int color) {
        return new Border(width, color);
    }

    public static Rounded rounded(int radius) {
        return new Rounded(radius);
    }

    public static Opacity opacity(float value) {
        return new Opacity(value);
    }

    public static ZIndex zIndex(int value) {
        return new ZIndex(value);
    }

    public static Visible visible(boolean value) {
        return new Visible(value);
    }

    public static Clip clip(boolean value) {
        return new Clip(value);
    }

    public static Clickable clickable(Runnable onClick) {
        return new Clickable(onClick);
    }

    public static Hoverable hoverable(Runnable onEnter) {
        return new Hoverable(onEnter, null);
    }

    public static Hoverable hoverable(Runnable onEnter, Runnable onExit) {
        return new Hoverable(onEnter, onExit);
    }

    public static Tooltip tooltip(String text) {
        return new Tooltip(text);
    }

    public static Cursor cursor(CursorType type) {
        return new Cursor(type);
    }

    public static Id id(String value) {
        return new Id(value);
    }

    // ==================== New Properties ====================

    /** Text color */
    public record Color(int value) implements Property {}

    /** Font size */
    public record FontSize(int size) implements Property {}

    /** Bold text */
    public record Bold(boolean enabled) implements Property {}

    /** Strikethrough text */
    public record Strikethrough(boolean enabled) implements Property {}

    /** Text alignment */
    public record TextAlign(Alignment alignment) implements Property {}

    /** Flex grow factor */
    public record Flex(int factor) implements Property {}

    /** Gap between children */
    public record Gap(int value) implements Property {}

    /** Content alignment */
    public record Align(Alignment alignment) implements Property {}

    /** Box shadow */
    public record Shadow(int blur) implements Property {}

    /** Bottom border only */
    public record BorderBottom(int width, int color) implements Property {}

    /** Fill parent container */
    public record FillParent(boolean enabled) implements Property {}

    /** Text wrap */
    public record Wrap(boolean enabled) implements Property {}

    /** Top margin only */
    public record MarginTop(int value) implements Property {}

    /** Bottom margin only */
    public record MarginBottom(int value) implements Property {}

    /** Minimum width */
    public record MinWidth(int value) implements Property {}

    public enum Alignment {
        START, CENTER, END, SPACE_BETWEEN, SPACE_AROUND, SPACE_EVENLY
    }

    // ==================== New Factory Methods ====================

    public static Color color(int value) {
        return new Color(value);
    }

    public static FontSize fontSize(int size) {
        return new FontSize(size);
    }

    public static Bold bold() {
        return new Bold(true);
    }

    public static Bold bold(boolean enabled) {
        return new Bold(enabled);
    }

    public static Strikethrough strikethrough() {
        return new Strikethrough(true);
    }

    public static Strikethrough strikethrough(boolean enabled) {
        return new Strikethrough(enabled);
    }

    public static TextAlign textAlign(Alignment alignment) {
        return new TextAlign(alignment);
    }

    public static Flex flex(int factor) {
        return new Flex(factor);
    }

    public static Gap gap(int value) {
        return new Gap(value);
    }

    public static Align align(Alignment alignment) {
        return new Align(alignment);
    }

    public static Shadow shadow(int blur) {
        return new Shadow(blur);
    }

    public static BorderBottom borderBottom(int width, int color) {
        return new BorderBottom(width, color);
    }

    public static FillParent fillParent(boolean enabled) {
        return new FillParent(enabled);
    }

    public static Wrap wrap(boolean enabled) {
        return new Wrap(enabled);
    }

    public static MarginTop marginTop(int value) {
        return new MarginTop(value);
    }

    public static MarginBottom marginBottom(int value) {
        return new MarginBottom(value);
    }

    public static MinWidth minWidth(int value) {
        return new MinWidth(value);
    }

    // ==================== Builder Pattern ====================

    /**
     * Creates a new StyleBuilder for chained style construction.
     */
    public static StyleBuilder builder() {
        return new StyleBuilder();
    }

    /**
     * Fluent builder for constructing styles with method chaining.
     */
    public static class StyleBuilder {
        private final List<Property> properties = new ArrayList<>();

        public StyleBuilder padding(int all) {
            properties.add(new Padding(all, all, all, all));
            return this;
        }

        public StyleBuilder padding(int horizontal, int vertical) {
            properties.add(new Padding(vertical, horizontal, vertical, horizontal));
            return this;
        }

        public StyleBuilder padding(int top, int right, int bottom, int left) {
            properties.add(new Padding(top, right, bottom, left));
            return this;
        }

        public StyleBuilder margin(int all) {
            properties.add(new Margin(all, all, all, all));
            return this;
        }

        public StyleBuilder margin(int horizontal, int vertical) {
            properties.add(new Margin(vertical, horizontal, vertical, horizontal));
            return this;
        }

        public StyleBuilder marginTop(int value) {
            properties.add(new MarginTop(value));
            return this;
        }

        public StyleBuilder marginBottom(int value) {
            properties.add(new MarginBottom(value));
            return this;
        }

        public StyleBuilder size(int width, int height) {
            properties.add(new Size(width, height));
            return this;
        }

        public StyleBuilder width(int width) {
            properties.add(new Size(width, -1));
            return this;
        }

        public StyleBuilder height(int height) {
            properties.add(new Size(-1, height));
            return this;
        }

        public StyleBuilder minWidth(int value) {
            properties.add(new MinWidth(value));
            return this;
        }

        public StyleBuilder minSize(int width, int height) {
            properties.add(new MinSize(width, height));
            return this;
        }

        public StyleBuilder maxSize(int width, int height) {
            properties.add(new MaxSize(width, height));
            return this;
        }

        public StyleBuilder background(int color) {
            properties.add(new Background(color));
            return this;
        }

        public StyleBuilder color(int textColor) {
            properties.add(new Color(textColor));
            return this;
        }

        public StyleBuilder fontSize(int size) {
            properties.add(new FontSize(size));
            return this;
        }

        public StyleBuilder bold() {
            properties.add(new Bold(true));
            return this;
        }

        public StyleBuilder bold(boolean enabled) {
            properties.add(new Bold(enabled));
            return this;
        }

        public StyleBuilder strikethrough() {
            properties.add(new Strikethrough(true));
            return this;
        }

        public StyleBuilder strikethrough(boolean enabled) {
            properties.add(new Strikethrough(enabled));
            return this;
        }

        public StyleBuilder textAlign(Alignment alignment) {
            properties.add(new TextAlign(alignment));
            return this;
        }

        public StyleBuilder border(int width, int color) {
            properties.add(new Border(width, color));
            return this;
        }

        public StyleBuilder borderBottom(int width, int color) {
            properties.add(new BorderBottom(width, color));
            return this;
        }

        public StyleBuilder borderRadius(int radius) {
            properties.add(new Rounded(radius));
            return this;
        }

        public StyleBuilder rounded(int radius) {
            properties.add(new Rounded(radius));
            return this;
        }

        public StyleBuilder shadow(int blur) {
            properties.add(new Shadow(blur));
            return this;
        }

        public StyleBuilder flex(int factor) {
            properties.add(new Flex(factor));
            return this;
        }

        public StyleBuilder gap(int value) {
            properties.add(new Gap(value));
            return this;
        }

        public StyleBuilder align(Alignment alignment) {
            properties.add(new Align(alignment));
            return this;
        }

        public StyleBuilder fillParent() {
            properties.add(new FillParent(true));
            return this;
        }

        public StyleBuilder wrap() {
            properties.add(new Wrap(true));
            return this;
        }

        public StyleBuilder wrap(boolean enabled) {
            properties.add(new Wrap(enabled));
            return this;
        }

        public StyleBuilder opacity(float value) {
            properties.add(new Opacity(value));
            return this;
        }

        public StyleBuilder visible(boolean value) {
            properties.add(new Visible(value));
            return this;
        }

        public StyleBuilder clip() {
            properties.add(new Clip(true));
            return this;
        }

        public StyleBuilder zIndex(int value) {
            properties.add(new ZIndex(value));
            return this;
        }

        public StyleBuilder tooltip(String text) {
            properties.add(new Tooltip(text));
            return this;
        }

        public StyleBuilder cursor(CursorType type) {
            properties.add(new Cursor(type));
            return this;
        }

        public StyleBuilder clickable(Runnable onClick) {
            properties.add(new Clickable(onClick));
            return this;
        }

        public StyleBuilder hoverable(Runnable onEnter) {
            properties.add(new Hoverable(onEnter, null));
            return this;
        }

        public StyleBuilder hoverable(Runnable onEnter, Runnable onExit) {
            properties.add(new Hoverable(onEnter, onExit));
            return this;
        }

        public StyleBuilder id(String value) {
            properties.add(new Id(value));
            return this;
        }

        public StyleBuilder offset(int x, int y) {
            properties.add(new Offset(x, y));
            return this;
        }

        /**
         * Add a custom property.
         */
        public StyleBuilder add(Property property) {
            properties.add(property);
            return this;
        }

        /**
         * Build the final Style object.
         */
        public Style build() {
            return new Style(List.copyOf(properties));
        }
    }

    @Override
    public String toString() {
        return "Style" + properties;
    }
}
