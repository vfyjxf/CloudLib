package dev.vfyjxf.cloudlib.api.ui.style;

import dev.vfyjxf.cloudlib.api.ui.style.property.VisualProperty;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import org.jetbrains.annotations.Nullable;

import java.util.Collections;
import java.util.LinkedHashMap;
import java.util.Map;

/**
 * Context provided to {@link VisualProperty} implementations for applying visual styles.
 * <p>
 * This class contains visual styling information that does not affect layout,
 * such as colors, borders, text styling, and cursors.
 * <p>
 * VisualContext is separate from layout to maintain a clear distinction between
 * properties that affect the Taffy layout engine and those that only affect rendering.
 * <p>
 * <b>Visual Properties Categories:</b>
 * <ul>
 *   <li><b>Background &amp; Icon</b>: background texture, icon/foreground texture</li>
 *   <li><b>Border</b>: border width, color, and corner radii</li>
 *   <li><b>Shadow</b>: drop shadow with offset, blur, and color</li>
 *   <li><b>Opacity</b>: transparency level (0.0 - 1.0)</li>
 *   <li><b>Text Styling</b>: color, bold, italic, underline, strikethrough</li>
 * </ul>
 *
 * @see VisualProperty
 * @see StyleContext
 */
public class VisualContext {

    //region background & icon
    private VisualTexture background = VisualTexture.empty;
    private VisualTexture icon = VisualTexture.empty;
    //endregion

    //region zIndex
    /**
     * The z-index for sibling sorting within the same parent.
     * Lower values render first (behind), higher values render last (on top).
     */
    private int zIndex = 0;
    //endregion

    //region border
    private float borderWidth;
    private int borderColor;
    //endregion

    //region shadow
    private float shadowOffsetX;
    private float shadowOffsetY;
    private float shadowBlurRadius;
    private int shadowColor;
    //endregion

    //region text styling
    private @Nullable Integer textColor;
    private boolean textBold;
    private boolean textItalic;
    private boolean textUnderline;
    private boolean textStrikethrough;
    //endregion

    //region custom properties
    private final Map<String, Object> properties = new LinkedHashMap<>();
    //endregion

    /**
     * Sets a custom property value.
     * <p>
     * This allows storing arbitrary data for custom styling systems.
     *
     * @param name  the property name
     * @param value the property value
     * @param <T>   the value type
     */
    public <T> void setProperty(String name, T value) {
        properties.put(name, value);
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
    public <T extends @Nullable Object> T getProperty(String name, Class<T> type) {
        Object value = properties.get(name);
        if (type.isInstance(value)) {
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
    public <T> T getProperty(String name) {
        return (T) properties.get(name);
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
    public <T> T getProperty(String name, Class<T> type, T defaultValue) {
        T value = getProperty(name, type);
        return value != null ? value : defaultValue;
    }

    /**
     * Checks if a custom property is set.
     *
     * @param name the property name
     * @return true if the property is set
     */
    public boolean hasProperty(String name) {
        return properties.containsKey(name);
    }

    /**
     * Returns an unmodifiable view of all custom properties.
     *
     * @return the custom properties map
     */
    public Map<String, Object> getProperties() {
        return Collections.unmodifiableMap(properties);
    }

    public VisualTexture background() {
        return background;
    }

    public void setBackground(VisualTexture texture) {
        this.background = texture;
    }

    public VisualTexture icon() {
        return icon;
    }

    public void setIcon(VisualTexture texture) {
        this.icon = texture;
    }

    //region zIndex accessors

    /**
     * Gets the z-index.
     *
     * @return the z-index
     */
    public int zIndex() {
        return zIndex;
    }

    /**
     * Sets the z-index.
     *
     * @param zIndex the z-index
     */
    public void setZIndex(int zIndex) {
        this.zIndex = zIndex;
    }

    //endregion

    public float borderWidth() {
        return borderWidth;
    }

    public int borderColor() {
        return borderColor;
    }

    public void border(float width, int argb) {
        this.borderWidth = Math.max(0.0f, width);
        this.borderColor = argb;
    }

    public @Nullable Integer textColor() {
        return textColor;
    }

    public void textColor(@Nullable Integer argb) {
        this.textColor = argb;
    }

    public boolean textBold() {
        return textBold;
    }

    public void textBold(boolean bold) {
        this.textBold = bold;
    }

    public boolean textItalic() {
        return textItalic;
    }

    public void textItalic(boolean italic) {
        this.textItalic = italic;
    }

    public boolean textUnderline() {
        return textUnderline;
    }

    public void textUnderline(boolean underline) {
        this.textUnderline = underline;
    }

    public boolean textStrikethrough() {
        return textStrikethrough;
    }

    public void textStrikethrough(boolean strikethrough) {
        this.textStrikethrough = strikethrough;
    }

    // === Shadow Methods ===

    /**
     * Gets the shadow X offset.
     */
    public float shadowOffsetX() {
        return shadowOffsetX;
    }

    /**
     * Gets the shadow Y offset.
     */
    public float shadowOffsetY() {
        return shadowOffsetY;
    }

    /**
     * Gets the shadow blur radius.
     */
    public float shadowBlurRadius() {
        return shadowBlurRadius;
    }

    /**
     * Gets the shadow color.
     */
    public int shadowColor() {
        return shadowColor;
    }

    /**
     * Sets the shadow properties.
     *
     * @param offsetX    horizontal offset
     * @param offsetY    vertical offset
     * @param blurRadius blur radius (0 = sharp edge)
     * @param color      shadow color (ARGB)
     */
    public void setShadow(float offsetX, float offsetY, float blurRadius, int color) {
        this.shadowOffsetX = offsetX;
        this.shadowOffsetY = offsetY;
        this.shadowBlurRadius = Math.max(0.0f, blurRadius);
        this.shadowColor = color;
    }

    /**
     * Returns whether a shadow is defined.
     */
    public boolean hasShadow() {
        return (shadowColor & 0xFF000000) != 0 && (shadowBlurRadius > 0 || shadowOffsetX != 0 || shadowOffsetY != 0);
    }


    public void reset() {
        // Background & Icon
        background = VisualTexture.empty;
        icon = VisualTexture.empty;

        // Border
        borderWidth = 0.0f;
        borderColor = 0;

        // Shadow
        shadowOffsetX = 0.0f;
        shadowOffsetY = 0.0f;
        shadowBlurRadius = 0.0f;
        shadowColor = 0;

        // Text Styling
        textColor = null;
        textBold = false;
        textItalic = false;
        textUnderline = false;
        textStrikethrough = false;

        // Custom properties
        properties.clear();
    }

    /**
     * Copies all values from another context.
     *
     * @param other the context to copy from
     */
    public void copyFrom(VisualContext other) {
        // Background & Icon
        this.background = other.background;
        this.icon = other.icon;

        // Border
        this.borderWidth = other.borderWidth;
        this.borderColor = other.borderColor;

        // Shadow
        this.shadowOffsetX = other.shadowOffsetX;
        this.shadowOffsetY = other.shadowOffsetY;
        this.shadowBlurRadius = other.shadowBlurRadius;
        this.shadowColor = other.shadowColor;

        // Text Styling
        this.textColor = other.textColor;
        this.textBold = other.textBold;
        this.textItalic = other.textItalic;
        this.textUnderline = other.textUnderline;
        this.textStrikethrough = other.textStrikethrough;

        // Custom properties
        this.properties.clear();
        this.properties.putAll(other.properties);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("VisualContext{");
        boolean any = false;

        if (background != null && background != VisualTexture.empty) {
            sb.append("background=").append(background).append(", ");
            any = true;
        }
        if (icon != null && icon != VisualTexture.empty) {
            sb.append("icon=").append(icon).append(", ");
            any = true;
        }
        if (borderWidth != 0.0f) {
            sb.append("border=").append(borderWidth).append(" ")
                    .append(String.format("0x%08X", borderColor)).append(", ");
            any = true;
        }
        if (hasShadow()) {
            sb.append("shadow=(").append(shadowOffsetX).append(",")
                    .append(shadowOffsetY).append(",")
                    .append(shadowBlurRadius).append(",")
                    .append(String.format("0x%08X", shadowColor)).append("), ");
            any = true;
        }
        if (textColor != null) {
            sb.append("textColor=").append(String.format("0x%08X", textColor)).append(", ");
            any = true;
        }
        if (textBold || textItalic || textUnderline || textStrikethrough) {
            sb.append("textStyle=");
            if (textBold) sb.append("bold ");
            if (textItalic) sb.append("italic ");
            if (textUnderline) sb.append("underline ");
            if (textStrikethrough) sb.append("strikethrough ");
            sb.append(", ");
            any = true;
        }

        if (!properties.isEmpty()) {
            sb.append("extra=").append(properties);
            any = true;
        }

        if (!any) {
            return "VisualContext{}";
        }

        String s = sb.toString();
        return s.endsWith(", ") ? s.substring(0, s.length() - 2) + "}" : s + "}";
    }
}
