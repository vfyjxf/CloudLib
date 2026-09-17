package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.SwitchGate;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The Y-consistent excentric labeling variant (Fekete &amp; Plaisant, §3.0)
 * for near-crosshair multiple hints: labels stack in one vertical column on a
 * single side of the focus point — the "Y-consistent" alignment that keeps
 * them mutually non-overlapping and visually stable — with each label's
 * connector on its focus-facing edge, ready for a leader back to the focus.
 * <p>
 * The side is chosen by available room (the caller passes the screen bounds)
 * and gated by a {@link SwitchGate}: the side flips only when the room
 * imbalance has moved a full {@code sideBand} from its value at the last
 * commit and held for the dwell ticks, so a focus oscillating around the
 * screen center cannot swap the column left-right-left. Re-layout is
 * stateless beyond that gate: the column follows the focus every call,
 * keeping its alignment — the "labels re-pack but stay a column" property.
 * Labels keep the caller's order; nothing here ever sorts them.
 */
public final class ExcentricColumn {

    /** Which side of the focus the column occupies. */
    public enum Side {
        left,
        right
    }

    /**
     * @param focusGap horizontal gap between the focus and the column's
     *        focus-facing edge; non-negative
     * @param labelGap vertical gap between neighboring labels; non-negative
     * @param sideBand the side-switch hysteresis band in pixels of room
     *        imbalance; positive
     * @param sideDwellTicks how many consecutive layouts a candidate side
     *        must survive before committing; at least 1
     */
    public record Config(double focusGap, double labelGap, double sideBand, int sideDwellTicks) {

        public Config {
            if (!Double.isFinite(focusGap) || focusGap < 0) {
                throw new IllegalArgumentException("focusGap must be finite and non-negative: " + focusGap);
            }
            if (!Double.isFinite(labelGap) || labelGap < 0) {
                throw new IllegalArgumentException("labelGap must be finite and non-negative: " + labelGap);
            }
            if (!Double.isFinite(sideBand) || sideBand <= 0) {
                throw new IllegalArgumentException("sideBand must be finite and positive: " + sideBand);
            }
            if (sideDwellTicks < 1) {
                throw new IllegalArgumentException("sideDwellTicks must be at least 1: " + sideDwellTicks);
            }
        }

        public static Config of(double focusGap, double labelGap, double sideBand, int sideDwellTicks) {
            return new Config(focusGap, labelGap, sideBand, sideDwellTicks);
        }
    }

    /** One label to place, by size. */
    public record Label(String id, double width, double height) {}

    /**
     * One placed label: its top-left corner and its connector — the midpoint
     * of the focus-facing vertical edge, where a leader to the focus attaches.
     */
    public record Placed(String id, FloatPos topLeft, FloatPos connector) {}

    /**
     * One layout: the gated side, the column's focus-facing x line, and the
     * placed labels in input order.
     */
    public record Layout(Side side, double columnX, List<Placed> labels) {

        public Layout {
            labels = List.copyOf(labels);
        }
    }

    private final Config config;
    private @Nullable SwitchGate<Side> sideGate;

    public ExcentricColumn(Config config) {
        this.config = config;
    }

    public Config config() {
        return config;
    }

    /**
     * Lays out the labels around the focus, clamped into {@code bounds}.
     *
     * @param focus the focus point (the crosshair's current target)
     * @param labels the labels in display order
     * @param bounds the screen area the column must fit in
     *
     * @throws IllegalArgumentException if label sizes are not finite and
     *         positive
     */
    public Layout layout(FloatPos focus, List<Label> labels, FloatRect bounds) {
        double roomRight = bounds.right() - focus.x();
        double roomLeft = focus.x() - bounds.x();
        Side desired = roomRight >= roomLeft ? Side.right : Side.left;
        sideGate = sideGate(desired, roomRight - roomLeft);
        Side side = sideGate.current();

        double columnX = side == Side.right ? focus.x() + config.focusGap() : focus.x() - config.focusGap();

        double totalHeight = 0;
        for (Label label : labels) {
            if (label.width() <= 0
                    || label.height() <= 0
                    || !Double.isFinite(label.width())
                    || !Double.isFinite(label.height())) {
                throw new IllegalArgumentException("label size must be finite and positive: " + label);
            }
            totalHeight += label.height();
        }
        if (!labels.isEmpty()) {
            totalHeight += config.labelGap() * (labels.size() - 1);
        }

        double y = focus.y() - totalHeight * 0.5;
        if (totalHeight <= bounds.height()) {
            y = Math.max(bounds.y(), Math.min(y, bounds.bottom() - totalHeight));
        }

        List<Placed> placed = new ArrayList<>(labels.size());
        for (Label label : labels) {
            double x = side == Side.right ? columnX : columnX - label.width();
            placed.add(new Placed(
                    label.id(),
                    new FloatPos(x, y),
                    new FloatPos(side == Side.right ? x : x + label.width(), y + label.height() * 0.5)));
            y += label.height() + config.labelGap();
        }
        return new Layout(side, columnX, placed);
    }

    private SwitchGate<Side> sideGate(Side desired, double imbalance) {
        if (sideGate == null) {
            return new SwitchGate<>(
                    new SwitchGate.Config(config.sideBand(), config.sideDwellTicks(), 0.0, 0), desired, imbalance);
        }
        sideGate.propose(desired, imbalance);
        return sideGate;
    }
}
