package dev.vfyjxf.cloudlib.api.ui.style;

/**
 * A drop shadow value: offset, blur radius and ARGB color — the value carried
 * by {@code Styles.boxShadow}.
 */
public record Shadow(float offsetX, float offsetY, float blurRadius, int color) {

    public static final Shadow none = new Shadow(0, 0, 0, 0);

    public static Shadow of(float offsetX, float offsetY, float blurRadius, int color) {
        return new Shadow(offsetX, offsetY, blurRadius, color);
    }
}
