package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import dev.vfyjxf.cloudlib.api.ui.inworld.space.IntervalSet;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.RayFan;

import java.util.ArrayList;
import java.util.List;

/**
 * The ring-slot structure around a world anchor (§3.0): concentric rings of
 * evenly spaced angular slots whose occupied arcs are subtracted with
 * {@link RayFan}/{@link IntervalSet} — an occluder near the anchor removes
 * every slot whose angular interval its shadow covers. When a ring's free
 * capacity is exhausted, {@link #freeSlots(RayFan, int)} simply reaches into
 * the next ring outward (radius grows by {@code radiusStep} per ring), which
 * is the whole "expand to the next ring" degradation path.
 * <p>
 * Rings are geometric, not stateful: the caller owns the anchor and the fan
 * (blocked from this epoch's occluders), this class answers which slots are
 * usable. Slot counts adapt to the ring's circumference — a ring's slots are
 * spaced roughly {@code slotArcLength} pixels of arc apart, so outer rings
 * hold more slots; odd rings are staggered half a step so slots don't line up
 * radially. Slots are pure offsets: {@link Slot#offsetX()}/{@link Slot#offsetY()}
 * are relative to the anchor, in the anchor's gui-pixel frame (angles follow
 * the standard atan2 convention, clockwise on screen because gui y grows
 * downward). Ordering is deterministic: ring ascending, then slot index
 * ascending.
 */
public final class OrbitRing {

    private static final double twoPi = 2 * Math.PI;

    /**
     * One candidate position: ring {@code ring}, slot {@code index} on it,
     * at {@code angle} radians (in {@code [0, 2π)}) and {@code radius} pixels
     * from the anchor. The slot's angular half-interval is
     * {@link #slotHalfArc(int) half the ring's step}.
     */
    public record Slot(int ring, int index, double angle, double radius) {

        /** The slot's x offset from the anchor. */
        public double offsetX() {
            return Math.cos(angle) * radius;
        }

        /** The slot's y offset from the anchor. */
        public double offsetY() {
            return Math.sin(angle) * radius;
        }
    }

    private final double baseRadius;
    private final double radiusStep;
    private final double slotArcLength;
    private final int maxRings;

    /**
     * @throws IllegalArgumentException if any parameter is not finite, if
     *         radii/arc length are not positive, or if maxRings is not
     *         positive
     */
    public OrbitRing(double baseRadius, double radiusStep, double slotArcLength, int maxRings) {
        requirePositive("baseRadius", baseRadius);
        requirePositive("radiusStep", radiusStep);
        requirePositive("slotArcLength", slotArcLength);
        if (maxRings <= 0) {
            throw new IllegalArgumentException("maxRings must be positive: " + maxRings);
        }
        this.baseRadius = baseRadius;
        this.radiusStep = radiusStep;
        this.slotArcLength = slotArcLength;
        this.maxRings = maxRings;
    }

    public double baseRadius() {
        return baseRadius;
    }

    public double radiusStep() {
        return radiusStep;
    }

    /** The configured desired arc distance between neighboring slots. */
    public double slotArcLength() {
        return slotArcLength;
    }

    public int maxRings() {
        return maxRings;
    }

    /** The radius of {@code ring} (ring 0 is at {@code baseRadius}). */
    public double ringRadius(int ring) {
        requireRing(ring);
        return baseRadius + ring * radiusStep;
    }

    /**
     * How many slots ring {@code ring} holds: its circumference divided by
     * the desired slot arc length, at least two.
     */
    public int ringSlotCount(int ring) {
        requireRing(ring);
        return Math.max(2, (int) Math.floor(twoPi * ringRadius(ring) / slotArcLength));
    }

    /** The angular half-interval a slot on {@code ring} occupies, radians. */
    public double slotHalfArc(int ring) {
        return Math.PI / ringSlotCount(ring);
    }

    /**
     * The slot at {@code ring}/{@code index}; indices wrap around the ring
     * (so {@code -1} is the last slot).
     */
    public Slot slot(int ring, int index) {
        requireRing(ring);
        int count = ringSlotCount(ring);
        int wrapped = Math.floorMod(index, count);
        double phase = (ring % 2) * Math.PI / count;
        double angle = normalize(phase + twoPi * wrapped / count);
        return new Slot(ring, wrapped, angle, ringRadius(ring));
    }

    /** All slots of {@code ring}, index ascending. */
    public List<Slot> ringSlots(int ring) {
        int count = ringSlotCount(ring);
        List<Slot> slots = new ArrayList<>(count);
        for (int i = 0; i < count; i++) {
            slots.add(slot(ring, i));
        }
        return slots;
    }

    /**
     * Whether the slot's whole angular interval lies in the fan's free arcs
     * — a slot straddling a blocked boundary counts as blocked (the
     * conservative direction). A free arc covering the entire turn contains
     * everything, wrap-around slots included.
     */
    public boolean isFree(Slot slot, RayFan fan) {
        List<IntervalSet.Interval> freeArcs = fan.freeArcs();
        if (freeArcs.isEmpty()) {
            return false;
        }
        double halfArc = slotHalfArc(slot.ring());
        double lo = normalize(slot.angle() - halfArc);
        double hi = lo + 2 * halfArc;
        for (IntervalSet.Interval arc : freeArcs) {
            if (arc.end() - arc.start() >= twoPi - 1.0e-9) {
                return true;
            }
            for (double shift : new double[]{-twoPi, 0.0, twoPi}) {
                if (arc.start() + shift <= lo && hi <= arc.end() + shift) {
                    return true;
                }
            }
        }
        return false;
    }

    /**
     * Up to {@code count} free slots, ring ascending (inner rings first),
     * index ascending within a ring — capacity exhaustion on one ring spills
     * into the next outward ring. The result may be shorter than
     * {@code count} when every ring is blocked or {@code maxRings} is
     * exhausted.
     */
    public List<Slot> freeSlots(RayFan fan, int count) {
        if (count <= 0) {
            return List.of();
        }
        List<Slot> free = new ArrayList<>(Math.min(count, 16));
        for (int ring = 0; ring < maxRings && free.size() < count; ring++) {
            for (Slot slot : ringSlots(ring)) {
                if (free.size() >= count) {
                    break;
                }
                if (isFree(slot, fan)) {
                    free.add(slot);
                }
            }
        }
        return free;
    }

    /** Every free slot on every ring, in the canonical order. */
    public List<Slot> freeSlots(RayFan fan) {
        return freeSlots(fan, Integer.MAX_VALUE);
    }

    private void requireRing(int ring) {
        if (ring < 0 || ring >= maxRings) {
            throw new IllegalArgumentException("ring out of range [0, " + maxRings + "): " + ring);
        }
    }

    private static double normalize(double angle) {
        double normalized = angle % twoPi;
        if (normalized < 0) {
            normalized += twoPi;
        }
        return normalized;
    }

    private static void requirePositive(String name, double value) {
        if (!Double.isFinite(value) || value <= 0) {
            throw new IllegalArgumentException(name + " must be finite and positive: " + value);
        }
    }
}
