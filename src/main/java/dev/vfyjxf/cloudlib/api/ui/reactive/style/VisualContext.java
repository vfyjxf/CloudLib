package dev.vfyjxf.cloudlib.api.ui.reactive.style;

import org.jetbrains.annotations.ApiStatus;
import org.jetbrains.annotations.Nullable;

import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Context provided to {@link VisualProperty} implementations for applying visual styles.
 * <p>
 * This class contains visual styling information that does not affect layout,
 * such as colors, borders, text styling, and cursors.
 * <p>
 * VisualContext is separate from layout to maintain a clear distinction between
 * properties that affect the Yoga layout engine and those that only affect rendering.
 *
 * @see VisualProperty
 * @see StyleContext
 */
@ApiStatus.Experimental
public class VisualContext {

    // Background
    @Nullable
    private Integer backgroundColor;
    @Nullable
    private String backgroundImage;

    // Border appearance (not layout border)
    private double borderWidth = 0;
    private int borderColor = 0;
    private double borderRadiusTopLeft = 0;
    private double borderRadiusTopRight = 0;
    private double borderRadiusBottomRight = 0;
    private double borderRadiusBottomLeft = 0;

    // Text styling
    private int textColor = 0xFFFFFFFF;
    private double fontSize = 9;  // Minecraft default
    private boolean bold = false;
    private boolean italic = false;
    private boolean underline = false;
    private boolean strikethrough = false;

    // Cursor
    @Nullable
    private Cursor cursor;

    // Opacity
    private double opacity = 1.0;

    // Visibility
    private boolean visible = true;

    // Custom properties storage
    private final Map<String, Object> customProperties = new LinkedHashMap<>();

    // ==================== Background ====================

    public void setBackgroundColor(int color) {
        this.backgroundColor = color;
    }

    @Nullable
    public Integer getBackgroundColor() {
        return backgroundColor;
    }

    public void setBackgroundImage(String image) {
        this.backgroundImage = image;
    }

    @Nullable
    public String getBackgroundImage() {
        return backgroundImage;
    }

    // ==================== Border Appearance ====================

    public void setBorder(double width, int color) {
        this.borderWidth = width;
        this.borderColor = color;
    }

    public double getBorderWidth() {
        return borderWidth;
    }

    public int getBorderColor() {
        return borderColor;
    }

    public void setBorderRadius(double radius) {
        setBorderRadius(radius, radius, radius, radius);
    }

    public void setBorderRadius(double topLeft, double topRight, double bottomRight, double bottomLeft) {
        this.borderRadiusTopLeft = topLeft;
        this.borderRadiusTopRight = topRight;
        this.borderRadiusBottomRight = bottomRight;
        this.borderRadiusBottomLeft = bottomLeft;
    }

    public double getBorderRadiusTopLeft() {
        return borderRadiusTopLeft;
    }

    public double getBorderRadiusTopRight() {
        return borderRadiusTopRight;
    }

    public double getBorderRadiusBottomRight() {
        return borderRadiusBottomRight;
    }

    public double getBorderRadiusBottomLeft() {
        return borderRadiusBottomLeft;
    }

    // ==================== Text ====================

    public void setTextColor(int color) {
        this.textColor = color;
    }

    public int getTextColor() {
        return textColor;
    }

    public void setFontSize(double size) {
        this.fontSize = size;
    }

    public double getFontSize() {
        return fontSize;
    }

    public void setBold(boolean bold) {
        this.bold = bold;
    }

    public boolean isBold() {
        return bold;
    }

    public void setItalic(boolean italic) {
        this.italic = italic;
    }

    public boolean isItalic() {
        return italic;
    }

    public void setUnderline(boolean underline) {
        this.underline = underline;
    }

    public boolean isUnderline() {
        return underline;
    }

    public void setStrikethrough(boolean strikethrough) {
        this.strikethrough = strikethrough;
    }

    public boolean isStrikethrough() {
        return strikethrough;
    }

    // ==================== Cursor ====================

    public void setCursor(Cursor cursor) {
        this.cursor = cursor;
    }

    @Nullable
    public Cursor getCursor() {
        return cursor;
    }

    // ==================== Opacity ====================

    public void setOpacity(double opacity) {
        this.opacity = Math.max(0, Math.min(1, opacity));
    }

    public double getOpacity() {
        return opacity;
    }

    // ==================== Visibility ====================

    public void setVisible(boolean visible) {
        this.visible = visible;
    }

    public boolean isVisible() {
        return visible;
    }

    // ==================== Custom Properties ====================

    /**
     * Sets a custom property value.
     * <p>
     * This allows storing arbitrary data for custom styling systems.
     *
     * @param name  the property name
     * @param value the property value
     * @param <T>   the value type
     */
    public <T> void setCustomProperty(String name, T value) {
        customProperties.put(name, value);
    }

    /**
     * Sets a custom property value (alias for setCustomProperty).
     *
     * @param name  the property name
     * @param value the property value
     * @param <T>   the value type
     */
    public <T> void setCustom(String name, T value) {
        setCustomProperty(name, value);
    }

    /**
     * Gets a custom property value.
     *
     * @param name the property name
     * @param type the expected value type
     * @param <T>  the value type
     * @return the property value, or null if not set
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCustomProperty(String name, Class<T> type) {
        Object value = customProperties.get(name);
        if (value != null && type.isInstance(value)) {
            return (T) value;
        }
        return null;
    }

    /**
     * Gets a custom property value with unchecked type cast.
     *
     * @param name the property name
     * @param <T>  the value type
     * @return the property value, or null if not set
     */
    @SuppressWarnings("unchecked")
    @Nullable
    public <T> T getCustom(String name) {
        return (T) customProperties.get(name);
    }

    /**
     * Gets a custom property value with a default.
     *
     * @param name         the property name
     * @param type         the expected value type
     * @param defaultValue the default value
     * @param <T>          the value type
     * @return the property value, or the default if not set
     */
    public <T> T getCustomProperty(String name, Class<T> type, T defaultValue) {
        T value = getCustomProperty(name, type);
        return value != null ? value : defaultValue;
    }

    /**
     * Checks if a custom property is set.
     *
     * @param name the property name
     * @return true if the property is set
     */
    public boolean hasCustomProperty(String name) {
        return customProperties.containsKey(name);
    }

    /**
     * Returns an unmodifiable view of all custom properties.
     *
     * @return the custom properties map
     */
    public Map<String, Object> getCustomProperties() {
        return java.util.Collections.unmodifiableMap(customProperties);
    }

    // ==================== Utility Methods ====================

    /**
     * Resets all properties to their default values.
     */
    public void reset() {
        backgroundColor = null;
        backgroundImage = null;
        borderWidth = 0;
        borderColor = 0;
        borderRadiusTopLeft = 0;
        borderRadiusTopRight = 0;
        borderRadiusBottomRight = 0;
        borderRadiusBottomLeft = 0;
        textColor = 0xFFFFFFFF;
        fontSize = 9;
        bold = false;
        italic = false;
        underline = false;
        strikethrough = false;
        cursor = null;
        opacity = 1.0;
        visible = true;
        customProperties.clear();
    }

    /**
     * Copies all values from another context.
     *
     * @param other the context to copy from
     */
    public void copyFrom(VisualContext other) {
        this.backgroundColor = other.backgroundColor;
        this.backgroundImage = other.backgroundImage;
        this.borderWidth = other.borderWidth;
        this.borderColor = other.borderColor;
        this.borderRadiusTopLeft = other.borderRadiusTopLeft;
        this.borderRadiusTopRight = other.borderRadiusTopRight;
        this.borderRadiusBottomRight = other.borderRadiusBottomRight;
        this.borderRadiusBottomLeft = other.borderRadiusBottomLeft;
        this.textColor = other.textColor;
        this.fontSize = other.fontSize;
        this.bold = other.bold;
        this.italic = other.italic;
        this.underline = other.underline;
        this.strikethrough = other.strikethrough;
        this.cursor = other.cursor;
        this.opacity = other.opacity;
        this.visible = other.visible;
        this.customProperties.clear();
        this.customProperties.putAll(other.customProperties);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("VisualContext{");
        if (backgroundColor != null) {
            sb.append("backgroundColor=").append(String.format("0x%08X", backgroundColor)).append(", ");
        }
        if (backgroundImage != null) {
            sb.append("backgroundImage='").append(backgroundImage).append("', ");
        }
        if (borderWidth > 0) {
            sb.append("border=").append(borderWidth).append("/").append(String.format("0x%08X", borderColor)).append(", ");
        }
        if (cursor != null) {
            sb.append("cursor=").append(cursor).append(", ");
        }
        if (opacity < 1.0) {
            sb.append("opacity=").append(opacity).append(", ");
        }
        // Remove trailing ", "
        if (sb.length() > "VisualContext{".length()) {
            sb.setLength(sb.length() - 2);
        }
        sb.append("}");
        return sb.toString();
    }
}
