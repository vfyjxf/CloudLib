package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Insets;
import dev.vfyjxf.cloudlib.api.math.Rect;

import java.util.Objects;

/**
 * The screen partition for one anchor (Z1): which visual zone a screen point
 * or candidate rect belongs to. Four zones, classified against the safe
 * rectangle and the anchor:
 * <ul>
 *   <li>{@link Region#anchor anchor} — the anchor neighborhood: within
 *       {@code anchorRadiusPx} of the anchor; the element's natural place</li>
 *   <li>{@link Region#edge edge} — the screen-edge band: the
 *       {@code edgeBandPx}-wide band hugging the safe rectangle's boundary
 *       (points outside the safe rectangle classify here too); where edge
 *       proxies and docked indicators live</li>
 *   <li>{@link Region#displacement displacement} — the drift band: within
 *       {@code maxDisplacementPx} (D_max) of the anchor; how far an
 *       anchor-attached panel may drift before it is no longer "at" the
 *       anchor</li>
 *   <li>{@link Region#center center} — everything else: the central
 *       attention field's domain, the most expensive screen real estate</li>
 * </ul>
 * Classification precedence is {@code anchor > edge > displacement > center}:
 * a point both near the anchor and in the edge band is an anchor point (the
 * more specific zone wins), and the drift band is clipped by the edge band's
 * precedence. All geometry is gui-scaled pixels in the standard GUI frame
 * (origin top-left, y down); the safe rectangle comes from
 * {@link #screenSafeRect(int, int, Insets)}.
 */
public final class ZoneModel {

    /** The four visual zones; see the class javadoc for the precedence. */
    public enum Region {
        anchor, edge, displacement, center
    }

    /**
     * The partition knobs, all in gui-scaled pixels.
     *
     * @param anchorRadiusPx the anchor neighborhood radius
     * @param maxDisplacementPx the drift-band radius D_max — how far a panel
     *        may drift from its anchor
     * @param edgeBandPx the width of the band hugging the safe rectangle's
     *        boundary
     */
    public record Config(double anchorRadiusPx, double maxDisplacementPx, double edgeBandPx) {

        public Config {
            requirePositive("anchorRadiusPx", anchorRadiusPx);
            requirePositive("maxDisplacementPx", maxDisplacementPx);
            requirePositive("edgeBandPx", edgeBandPx);
        }

        public static Config defaults() {
            return new Config(24.0, 160.0, 40.0);
        }

        public static Config of(double anchorRadiusPx, double maxDisplacementPx, double edgeBandPx) {
            return new Config(anchorRadiusPx, maxDisplacementPx, edgeBandPx);
        }

        private static void requirePositive(String name, double value) {
            if (!Double.isFinite(value) || value <= 0.0) {
                throw new IllegalArgumentException(name + " must be finite and positive: " + value);
            }
        }
    }

    private final FloatPos anchor;
    private final Rect safeRect;
    private final AttentionField attention;
    private final Config config;

    /**
     * @throws IllegalArgumentException if the anchor is not finite
     */
    public ZoneModel(FloatPos anchor, Rect safeRect, AttentionField attention, Config config) {
        this.anchor = requireFinite(anchor, "anchor");
        this.safeRect = Objects.requireNonNull(safeRect, "safeRect");
        this.attention = Objects.requireNonNull(attention, "attention");
        this.config = Objects.requireNonNull(config, "config");
    }

    /** A model with {@link Config#defaults()}. */
    public static ZoneModel of(FloatPos anchor, Rect safeRect, AttentionField attention) {
        return new ZoneModel(anchor, safeRect, attention, Config.defaults());
    }

    /**
     * The safe rectangle: the screen inset by {@code insets} (HUD margins),
     * clamped to non-negative size — the area panels must stay inside.
     */
    public static Rect screenSafeRect(int width, int height, Insets insets) {
        if (width <= 0 || height <= 0) {
            throw new IllegalArgumentException("screen size must be positive: " + width + "x" + height);
        }
        Objects.requireNonNull(insets, "insets");
        int x = Math.max(0, insets.left());
        int y = Math.max(0, insets.top());
        int safeWidth = Math.max(0, width - insets.left() - insets.right());
        int safeHeight = Math.max(0, height - insets.top() - insets.bottom());
        return new Rect(x, y, safeWidth, safeHeight);
    }

    public FloatPos anchor() {
        return anchor;
    }

    public Rect safeRect() {
        return safeRect;
    }

    /** The center attention field this partition grades the center zone with. */
    public AttentionField attention() {
        return attention;
    }

    public Config config() {
        return config;
    }

    /** The drift-band radius D_max, in pixels. */
    public double maxDisplacementPx() {
        return config.maxDisplacementPx();
    }

    /** Which zone the point {@code (x, y)} belongs to. */
    public Region regionOf(double x, double y) {
        double dx = x - anchor.x();
        double dy = y - anchor.y();
        if (Math.sqrt(dx * dx + dy * dy) <= config.anchorRadiusPx()) {
            return Region.anchor;
        }
        if (x < safeRect.x() + config.edgeBandPx()
                || x > safeRect.right() - config.edgeBandPx()
                || y < safeRect.y() + config.edgeBandPx()
                || y > safeRect.bottom() - config.edgeBandPx()) {
            return Region.edge;
        }
        if (Math.sqrt(dx * dx + dy * dy) <= config.maxDisplacementPx()) {
            return Region.displacement;
        }
        return Region.center;
    }

    /** Which zone the point belongs to. */
    public Region regionOf(FloatPos point) {
        requireFinite(point, "point");
        return regionOf(point.x(), point.y());
    }

    /** Which zone the rect's center belongs to. */
    public Region regionOf(Rect rect) {
        return regionOf(rect.centerX(), rect.centerY());
    }

    /**
     * How far the rect's center lies beyond the drift band D_max — 0 when it
     * is within the band, {@code distance(center, anchor) − D_max} otherwise.
     */
    public double driftExcess(Rect rect) {
        double dx = rect.centerX() - anchor.x();
        double dy = rect.centerY() - anchor.y();
        return Math.max(0.0, Math.sqrt(dx * dx + dy * dy) - config.maxDisplacementPx());
    }

    /**
     * Whether the rect touches the edge band: any part of it lies within
     * {@code edgeBandPx} of the safe rectangle's boundary, or outside the
     * safe rectangle entirely.
     */
    public boolean touchesEdgeBand(Rect rect) {
        return rect.x() < safeRect.x() + config.edgeBandPx()
                || rect.right() > safeRect.right() - config.edgeBandPx()
                || rect.y() < safeRect.y() + config.edgeBandPx()
                || rect.bottom() > safeRect.bottom() - config.edgeBandPx();
    }

    private static FloatPos requireFinite(FloatPos pos, String name) {
        Objects.requireNonNull(pos, name);
        if (!Double.isFinite(pos.x()) || !Double.isFinite(pos.y())) {
            throw new IllegalArgumentException(name + " must be finite: " + pos.x() + ", " + pos.y());
        }
        return pos;
    }
}
