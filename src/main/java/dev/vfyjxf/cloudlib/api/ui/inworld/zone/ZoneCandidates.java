package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.math.Size;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashSet;
import java.util.List;
import java.util.Objects;
import java.util.Set;

/**
 * The candidate lattice around an anchor (Z1): 25 alignment-semantic
 * candidate rectangles — the in-place candidate (the panel centered on the
 * anchor) plus eight directions × three distance tiers. Each candidate is
 * defined by <em>alignment</em>, not free coordinates: the {@link Direction#top}
 * near candidate is the panel whose bottom-center sits {@code nearPx} above
 * the anchor, {@link Direction#left} the panel whose right edge sits
 * {@code nearPx} left of the anchor, the diagonals offset per axis, and so
 * on — so every candidate is "the panel docked to the anchor on that side at
 * that clearance", which is what keeps the lattice stable as the anchor
 * moves.
 * <p>
 * Every candidate is clamped into the safe rectangle by the nearest-feasible
 * shift (both axes clamped independently) rather than dropped; a candidate is
 * dropped only when the panel cannot fit inside the safe rectangle at all.
 * Duplicate rects are removed — different direction/tier combinations may
 * land on the same rect, especially after clamping near a screen edge —
 * keeping the first occurrence in the canonical order. That order is: the
 * in-place candidate first, then each direction in enum order with tiers
 * near, medium, far.
 */
public final class ZoneCandidates {

    /**
     * The eight dock directions around an anchor. Diagonals offset the panel
     * by the tier's clearance on <em>both</em> axes.
     */
    public enum Direction {
        top,
        topLeft,
        topRight,
        left,
        right,
        bottom,
        bottomLeft,
        bottomRight
    }

    /** The distance tier of a candidate; {@link #anchor} is the in-place tier. */
    public enum Tier {
        anchor,
        near,
        medium,
        far
    }

    /**
     * The tier clearances, in pixels — the gap between the anchor and the
     * panel along each docked axis.
     *
     * @param nearPx the near-tier clearance
     * @param mediumPx the medium-tier clearance
     * @param farPx the far-tier clearance
     */
    public record Config(double nearPx, double mediumPx, double farPx) {

        public Config {
            requirePositive("nearPx", nearPx);
            requirePositive("mediumPx", mediumPx);
            requirePositive("farPx", farPx);
            if (!(nearPx < mediumPx && mediumPx < farPx)) {
                throw new IllegalArgumentException("clearances must satisfy nearPx < mediumPx < farPx: " + nearPx + ", "
                        + mediumPx + ", " + farPx);
            }
        }

        public static Config defaults() {
            return new Config(8.0, 28.0, 60.0);
        }

        public static Config of(double nearPx, double mediumPx, double farPx) {
            return new Config(nearPx, mediumPx, farPx);
        }

        private static void requirePositive(String name, double value) {
            if (!Double.isFinite(value) || value <= 0.0) {
                throw new IllegalArgumentException(name + " must be finite and positive: " + value);
            }
        }
    }

    /**
     * One lattice candidate. The direction is null exactly on the in-place
     * tier ({@link Tier#anchor}).
     *
     * @param direction the dock direction, null for the in-place candidate
     * @param tier the distance tier
     * @param rect the candidate rect, already clamped into the safe rectangle
     */
    public record Candidate(@Nullable Direction direction, Tier tier, Rect rect) {

        public Candidate {
            Objects.requireNonNull(tier, "tier");
            Objects.requireNonNull(rect, "rect");
            if ((direction == null) != (tier == Tier.anchor)) {
                throw new IllegalArgumentException(
                        "direction must be null exactly on the anchor tier: " + direction + ", " + tier);
            }
        }
    }

    private ZoneCandidates() {}

    /**
     * Generates the candidate lattice for one anchor and panel size against
     * the safe rectangle.
     *
     * @param anchor the anchor's screen position
     * @param panel the panel's footprint
     * @param safeRect the rectangle candidates must fit inside
     * @param config the tier clearances
     * @return the candidates in the canonical order (in-place first, then
     *         directions in enum order, tiers near → medium → far), clamped,
     *         deduplicated, and with unfixable candidates dropped
     * @throws IllegalArgumentException if the anchor is not finite or the
     *         panel footprint is not positive
     */
    public static List<Candidate> generate(FloatPos anchor, Size panel, Rect safeRect, Config config) {
        Objects.requireNonNull(anchor, "anchor");
        if (!Double.isFinite(anchor.x()) || !Double.isFinite(anchor.y())) {
            throw new IllegalArgumentException("anchor must be finite: " + anchor.x() + ", " + anchor.y());
        }
        Objects.requireNonNull(panel, "panel");
        if (panel.width() <= 0 || panel.height() <= 0) {
            throw new IllegalArgumentException("panel footprint must be positive: " + panel);
        }
        Objects.requireNonNull(safeRect, "safeRect");
        Objects.requireNonNull(config, "config");

        boolean fits = panel.width() <= safeRect.width() && panel.height() <= safeRect.height();
        Set<Rect> seen = new LinkedHashSet<>();
        List<Candidate> candidates = new ArrayList<>(25);
        if (fits) {
            addCandidate(candidates, seen, null, Tier.anchor, centered(anchor, panel), safeRect);
            for (Direction direction : Direction.values()) {
                for (Tier tier : new Tier[] {Tier.near, Tier.medium, Tier.far}) {
                    double clearance = tier == Tier.near
                            ? config.nearPx()
                            : tier == Tier.medium ? config.mediumPx() : config.farPx();
                    addCandidate(
                            candidates, seen, direction, tier, docked(anchor, panel, direction, clearance), safeRect);
                }
            }
        }
        return List.copyOf(candidates);
    }

    /** The lattice with {@link Config#defaults()} clearances. */
    public static List<Candidate> generate(FloatPos anchor, Size panel, Rect safeRect) {
        return generate(anchor, panel, safeRect, Config.defaults());
    }

    private static void addCandidate(
            List<Candidate> candidates,
            Set<Rect> seen,
            @Nullable Direction direction,
            Tier tier,
            Rect rect,
            Rect safeRect) {
        Rect clamped = clampInto(rect, safeRect);
        if (clamped == null) {
            return;
        }
        if (seen.add(clamped)) {
            candidates.add(new Candidate(direction, tier, clamped));
        }
    }

    private static Rect centered(FloatPos anchor, Size panel) {
        return new Rect(
                (int) Math.round(anchor.x() - panel.width() * 0.5),
                (int) Math.round(anchor.y() - panel.height() * 0.5),
                panel.width(),
                panel.height());
    }

    private static Rect docked(FloatPos anchor, Size panel, Direction direction, double clearance) {
        double left = anchor.x() - panel.width() * 0.5;
        double top = anchor.y() - panel.height() * 0.5;
        switch (direction) {
            case top -> top = anchor.y() - clearance - panel.height();
            case bottom -> top = anchor.y() + clearance;
            case left -> left = anchor.x() - clearance - panel.width();
            case right -> left = anchor.x() + clearance;
            case topLeft -> {
                left = anchor.x() - clearance - panel.width();
                top = anchor.y() - clearance - panel.height();
            }
            case topRight -> {
                left = anchor.x() + clearance;
                top = anchor.y() - clearance - panel.height();
            }
            case bottomLeft -> {
                left = anchor.x() - clearance - panel.width();
                top = anchor.y() + clearance;
            }
            case bottomRight -> {
                left = anchor.x() + clearance;
                top = anchor.y() + clearance;
            }
        }
        return new Rect((int) Math.round(left), (int) Math.round(top), panel.width(), panel.height());
    }

    /**
     * The nearest-feasible shift into the safe rectangle: each axis clamped
     * independently. Null when the rect cannot fit inside at all.
     */
    private static @Nullable Rect clampInto(Rect rect, Rect safeRect) {
        if (rect.width() > safeRect.width() || rect.height() > safeRect.height()) {
            return null;
        }
        int x = Math.min(Math.max(rect.x(), safeRect.x()), safeRect.right() - rect.width());
        int y = Math.min(Math.max(rect.y(), safeRect.y()), safeRect.bottom() - rect.height());
        return new Rect(x, y, rect.width(), rect.height());
    }
}
