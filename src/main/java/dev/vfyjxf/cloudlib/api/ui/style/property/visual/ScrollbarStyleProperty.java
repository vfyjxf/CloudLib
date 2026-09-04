package dev.vfyjxf.cloudlib.api.ui.style.property.visual;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.scroll.ScrollEffect;
import dev.vfyjxf.cloudlib.api.ui.scroll.ScrollState;
import dev.vfyjxf.cloudlib.api.ui.style.StyleType;
import dev.vfyjxf.cloudlib.api.ui.style.VisualContext;
import dev.vfyjxf.cloudlib.api.ui.style.property.VisualProperty;
import dev.vfyjxf.cloudlib.api.ui.texture.VisualTexture;
import org.jetbrains.annotations.Nullable;

/**
 * Visual property for configuring scrollbar appearance through the style system.
 * <p>
 * This property stores scrollbar styling data (track texture, thumb texture, width, min thumb size)
 * in the widget's {@link VisualContext} as a custom property, which can then be read by
 * {@link ScrollEffect ScrollEffect} or any custom scroll logic.
 *
 * <h3>Usage with UIStyles</h3>
 * <pre>{@code
 * widget.useStyle(UIStyle.of(
 *     scrollbarStyle(
 *         new ColorTexture(0x40000000),  // track
 *         new ColorTexture(0xFFAAAAAA),  // thumb
 *         6                              // width
 *     )
 * ));
 * }</pre>
 *
 * <h3>Applying to ScrollState</h3>
 * The style can be applied to a {@link ScrollState} via {@link ScrollbarStyleData#applyTo(ScrollState)}:
 * <pre>{@code
 * ScrollbarStyleData styleData = ScrollbarStyleProperty.getFrom(widget);
 * if (styleData != null) {
 *     styleData.applyTo(scrollState);
 * }
 * }</pre>
 *
 * @see ScrollState
 * @see ScrollEffect
 */
public record ScrollbarStyleProperty(ScrollbarStyleData data) implements VisualProperty {

    //region types

    /**
     * The style type key for scrollbar styling.
     */
    public static final StyleType<ScrollbarStyleData> type = StyleType.visual(
            "scrollbar-style",
            ScrollbarStyleData::empty
    );

    /**
     * The custom property key used in {@link VisualContext}.
     */
    public static final String PROPERTY_KEY = "scrollbar-style";

    //endregion

    //region constructors

    /**
     * Creates a scrollbar style property with track and thumb textures.
     *
     * @param track the track (background) texture
     * @param thumb the thumb (handle) texture
     */
    public ScrollbarStyleProperty(VisualTexture track, VisualTexture thumb) {
        this(new ScrollbarStyleData(track, thumb, -1, -1));
    }

    /**
     * Creates a scrollbar style property with track, thumb, and width.
     *
     * @param track the track (background) texture
     * @param thumb the thumb (handle) texture
     * @param width the scrollbar width in pixels
     */
    public ScrollbarStyleProperty(VisualTexture track, VisualTexture thumb, int width) {
        this(new ScrollbarStyleData(track, thumb, width, -1));
    }

    /**
     * Creates a scrollbar style property with all parameters.
     *
     * @param track        the track (background) texture
     * @param thumb        the thumb (handle) texture
     * @param width        the scrollbar width in pixels (-1 to keep default)
     * @param minThumbSize the minimum thumb size in pixels (-1 to keep default)
     */
    public ScrollbarStyleProperty(VisualTexture track, VisualTexture thumb, int width, int minThumbSize) {
        this(new ScrollbarStyleData(track, thumb, width, minThumbSize));
    }

    //endregion

    //region VisualProperty implementation

    @Override
    public StyleType<?> type() {
        return type;
    }

    @Override
    public void applyToWidget(VisualContext context) {
        context.setProperty(PROPERTY_KEY, data);
    }

    //endregion

    //region utility

    /**
     * Retrieves the scrollbar style data from a widget's visual context.
     *
     * @param widget the widget to read from
     * @return the scrollbar style data, or null if not set
     */
    public static @Nullable ScrollbarStyleData getFrom(Widget widget) {
        return widget.style().visualContext().getProperty(PROPERTY_KEY, ScrollbarStyleData.class);
    }

    //endregion

    //region data record

    /**
     * Immutable data holder for scrollbar styling.
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

        public static ScrollbarStyleData empty() {
            return new ScrollbarStyleData(null, null, -1, -1);
        }

        /**
         * Applies the style data to a {@link ScrollState}, overwriting only the fields
         * that are explicitly set (non-null, non-negative).
         *
         * @param state the scroll state to apply to
         */
        public void applyTo(ScrollState state) {
            if (track != null) state.trackTexture(track);
            if (thumb != null) state.thumbTexture(thumb);
            if (width > 0) state.scrollbarWidth(width);
            if (minThumbSize > 0) state.minThumbSize(minThumbSize);
        }
    }

    //endregion

    @Override
    public String toString() {
        return "ScrollbarStyleProperty{" + data + '}';
    }
}
