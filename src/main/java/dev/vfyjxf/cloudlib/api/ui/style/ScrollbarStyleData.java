package dev.vfyjxf.cloudlib.api.ui.style;

import dev.vfyjxf.cloudlib.api.ui.scroll.ScrollState;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import org.jspecify.annotations.Nullable;

/**
 * Immutable scrollbar styling data — the value carried by
 * {@code Styles.scrollbarStyle}.
 *
 * @param track        the track (background) texture, or null to keep existing
 * @param thumb        the thumb (handle) texture, or null to keep existing
 * @param width        the scrollbar width (-1 means keep existing/default)
 * @param minThumbSize the minimum thumb size (-1 means keep existing/default)
 */
public record ScrollbarStyleData(
    @Nullable VisualTexture track,
    @Nullable VisualTexture thumb,
    int width,
    int minThumbSize
) {

    /** The visual-context custom-property key this data is stored under. */
    public static final String propertyKey = "scrollbar-style";

    public static ScrollbarStyleData empty() {
        return new ScrollbarStyleData(null, null, -1, -1);
    }

    /**
     * Applies the style data to a {@link ScrollState}, overwriting only the
     * fields that are explicitly set (non-null, non-negative).
     */
    public void applyTo(ScrollState state) {
        if (track != null) state.trackTexture(track);
        if (thumb != null) state.thumbTexture(thumb);
        if (width > 0) state.scrollbarWidth(width);
        if (minThumbSize > 0) state.minThumbSize(minThumbSize);
    }
}
