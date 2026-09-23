package dev.vfyjxf.cloudlib.api.ui.canvas;

/**
 * The guide-line stroke's resolved look — everything {@code
 * cloudlib:guide_line_hud} and {@code cloudlib:guide_line_world} read past the
 * geometry. The caller resolves it from its theme and hands the same bundle to
 * both passes, so a screen stroke and its world twin stay one style.
 * <p>
 * Colours carry their own alpha (the core's is the stroke's base alpha, the
 * outline's the weight of its band), the dash is expressed in px of arc and
 * the arc window in fractions measured from the panel end (0) toward the world
 * end (1). {@link #ofDefaults} is the survey look: a 2 px core with a 1 px
 * dark outline each side, solid, fully drawn.
 *
 * @param lineWidth the core stroke's full width in px
 * @param edgeWidth the dark outline added on each side of the core, in px
 * @param lineColor the core hue (ARGB; alpha is the stroke's base alpha)
 * @param edgeColor the outline hue (ARGB; alpha is the outline's weight)
 * @param fadeFraction the arc fraction at the world end that fades out
 * @param fadeAlpha the alpha multiplier at the very end of the arc
 * @param dashPeriodPx the dash period in px; 0 draws a solid stroke
 * @param dashDuty the solid fraction of the period, in (0, 1]
 * @param dashPhasePx the dash pattern's advance along the arc, in px
 * @param marker the target-end marker
 * @param markerSize the marker's radius (dot) or length (arrow), in px
 * @param portTick the panel-end tick's half-length in px; 0 draws no tick
 * @param arcStart the drawn arc's start fraction (the entry reveal's window)
 * @param arcEnd the drawn arc's end fraction
 */
public record GuideLineStyle(
    float lineWidth,
    float edgeWidth,
    int lineColor,
    int edgeColor,
    float fadeFraction,
    float fadeAlpha,
    float dashPeriodPx,
    float dashDuty,
    float dashPhasePx,
    Marker marker,
    float markerSize,
    float portTick,
    float arcStart,
    float arcEnd
) {

    /** The target-end marker: shape reads as the target's class, colour as its state. */
    public enum Marker {
        /** No marker — the stroke ends bare. */
        none,
        /** A filled disc — a face/area target. */
        dot,
        /** A filled arrowhead along the arrival direction — an edge/enemy target. */
        arrow,
        /**
         * A two-arm corner bracket along the arrival direction — a
         * directional target mark. Kept as shader capability; no tier
         * currently draws it (the attach tier pairs with an
         * orientation-free dot instead, close distances having no
         * meaningful direction).
         */
        bracket
    }

    /** How many polyline samples one stroke's uniform block holds. */
    public static final int maxPoints = 64;

    public GuideLineStyle {
        if (!(lineWidth > 0)) {
            throw new IllegalArgumentException("lineWidth must be positive: " + lineWidth);
        }
        if (!(edgeWidth >= 0)) {
            throw new IllegalArgumentException("edgeWidth must be non-negative: " + edgeWidth);
        }
        if (marker == null) {
            marker = Marker.none;
        }
    }

    /** The survey defaults: 2 px core, 1 px outline, solid, no marker, no fade. */
    public static GuideLineStyle ofDefaults(int lineColor, int edgeColor) {
        return new GuideLineStyle(2f, 1f, lineColor, edgeColor, 0f, 1f, 0f, 0.5f, 0f, Marker.none, 0f, 0f, 0f, 1f);
    }

    /**
     * The theme-resolved base stroke: everything but the per-frame values
     * ({@link #at}) and the reveal's arc window ({@link #withArc}).
     */
    public static GuideLineStyle of(
        float lineWidth,
        float edgeWidth,
        int lineColor,
        int edgeColor,
        float fadeFraction,
        float fadeAlpha,
        float dashPeriodPx,
        float dashDuty,
        Marker marker,
        float markerSize,
        float portTick
    ) {
        return new GuideLineStyle(
            lineWidth,
            edgeWidth,
            lineColor,
            edgeColor,
            fadeFraction,
            fadeAlpha,
            dashPeriodPx,
            dashDuty,
            0f,
            marker,
            markerSize,
            portTick,
            0f,
            1f
        );
    }

    /**
     * The same stroke at this frame's width, hues, dash phase and marker —
     * the per-leader values the theme cannot know (hover swell, occluded dim,
     * target class, the ants' phase).
     */
    public GuideLineStyle at(
        float lineWidth,
        int lineColor,
        int edgeColor,
        float dashPeriodPx,
        float dashPhasePx,
        Marker marker,
        float markerSize
    ) {
        return new GuideLineStyle(
            lineWidth,
            edgeWidth,
            lineColor,
            edgeColor,
            fadeFraction,
            fadeAlpha,
            dashPeriodPx,
            dashDuty,
            dashPhasePx,
            marker,
            markerSize,
            portTick,
            arcStart,
            arcEnd
        );
    }

    /** The same stroke with the entry/exit reveal's arc window applied. */
    public GuideLineStyle withArc(float arcStart, float arcEnd) {
        return new GuideLineStyle(
            lineWidth,
            edgeWidth,
            lineColor,
            edgeColor,
            fadeFraction,
            fadeAlpha,
            dashPeriodPx,
            dashDuty,
            dashPhasePx,
            marker,
            markerSize,
            portTick,
            arcStart,
            arcEnd
        );
    }
}
