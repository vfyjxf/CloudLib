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
 *
 * @see VisualProperty
 * @see StyleContext
 */
public class VisualContext {

    private VisualTexture background = VisualTexture.empty;
    private VisualTexture icon = VisualTexture.empty;

    private float borderWidth;
    private int borderColor;

    private float radiusTopLeft;
    private float radiusTopRight;
    private float radiusBottomRight;
    private float radiusBottomLeft;

    private float opacity = 1.0f;

    private @Nullable Integer textColor;
    private boolean textBold;
    private boolean textItalic;
    private boolean textUnderline;
    private boolean textStrikethrough;

    private final Map<String, Object> properties = new LinkedHashMap<>();

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

    public void borderRadius(float topLeft, float topRight, float bottomRight, float bottomLeft) {
        this.radiusTopLeft = Math.max(0.0f, topLeft);
        this.radiusTopRight = Math.max(0.0f, topRight);
        this.radiusBottomRight = Math.max(0.0f, bottomRight);
        this.radiusBottomLeft = Math.max(0.0f, bottomLeft);
    }

    public float radiusTopLeft() {
        return radiusTopLeft;
    }

    public float radiusTopRight() {
        return radiusTopRight;
    }

    public float radiusBottomRight() {
        return radiusBottomRight;
    }

    public float radiusBottomLeft() {
        return radiusBottomLeft;
    }

    public float opacity() {
        return opacity;
    }

    public void opacity(float opacity) {
        this.opacity = Math.max(0.0f, Math.min(1.0f, opacity));
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

    public void reset() {
        background = VisualTexture.empty;
        borderWidth = 0.0f;
        borderColor = 0;
        radiusTopLeft = 0.0f;
        radiusTopRight = 0.0f;
        radiusBottomRight = 0.0f;
        radiusBottomLeft = 0.0f;
        opacity = 1.0f;
        textColor = null;
        textBold = false;
        textItalic = false;
        textUnderline = false;
        textStrikethrough = false;
        properties.clear();
    }

    /**
     * Copies all values from another context.
     *
     * @param other the context to copy from
     */
    public void copyFrom(VisualContext other) {
        this.background = other.background;
        this.borderWidth = other.borderWidth;
        this.borderColor = other.borderColor;
        this.radiusTopLeft = other.radiusTopLeft;
        this.radiusTopRight = other.radiusTopRight;
        this.radiusBottomRight = other.radiusBottomRight;
        this.radiusBottomLeft = other.radiusBottomLeft;
        this.opacity = other.opacity;
        this.textColor = other.textColor;
        this.textBold = other.textBold;
        this.textItalic = other.textItalic;
        this.textUnderline = other.textUnderline;
        this.textStrikethrough = other.textStrikethrough;
        this.properties.clear();
        this.properties.putAll(other.properties);
    }

    @Override
    public String toString() {
        StringBuilder sb = new StringBuilder("VisualContext{");
        boolean any = false;

        if (borderWidth != 0.0f) {
            sb.append("border=").append(borderWidth).append(" ")
              .append(String.format("0x%08X", borderColor)).append(", ");
            any = true;
        }
        if (radiusTopLeft != 0.0f || radiusTopRight != 0.0f || radiusBottomRight != 0.0f || radiusBottomLeft != 0.0f) {
            sb.append("radius=").append(radiusTopLeft).append(",")
              .append(radiusTopRight).append(",")
              .append(radiusBottomRight).append(",")
              .append(radiusBottomLeft).append(", ");
            any = true;
        }
        if (opacity != 1.0f) {
            sb.append("opacity=").append(opacity).append(", ");
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
