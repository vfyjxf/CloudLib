package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.List;

/**
 * The fixed one-dimensional cursor for a screen-edge dock — one instance owns
 * one edge's scanline (§3.0: "a 1D problem gets a 1D algorithm"). Elements
 * occupy {@link Slot intervals} along the scanline; widths come from the
 * caller (an element's content tier maps to a width — allocate with the tier's
 * current width and {@link #resize} when the tier changes, so slot width
 * adapts without reordering anything).
 * <p>
 * The scanline runs along the edge and is addressed by a single coordinate:
 * x for {@link Edge#top}/{@link Edge#bottom}, y for {@link Edge#left}/
 * {@link Edge#right}. {@link #allocate} is first-fit in coordinate order —
 * existing slots never move, a new slot takes the earliest gap that fits.
 * {@link #release} punches a hole; {@link #compact} slides every slot toward
 * the scanline origin <em>in coordinate order</em>, which is why compaction is
 * stable: elements keep their relative order, they are never re-sorted or
 * reshuffled. No two-dimensional packing ever happens here — the cross-edge
 * position (how far from the screen border) is the caller's constant.
 */
public final class DockCursor {

    /** Which screen edge this cursor's scanline runs along. */
    public enum Edge {
        top,
        right,
        bottom,
        left
    }

    /**
     * One occupied interval of the scanline. {@code start} is the coordinate
     * of the interval's low end along the edge (see class docs for the axis
     * per edge); {@code width} its extent.
     */
    public record Slot(String id, Edge edge, double start, double width) {

        public Slot {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("slot id must not be empty");
            }
        }

        public double end() {
            return start + width;
        }

        public double center() {
            return start + width * 0.5;
        }
    }

    private final Edge edge;
    private final double extent;
    private final double margin;
    private final double spacing;
    private final List<Slot> slots = new ArrayList<>();

    /**
     * @param extent the full scanline length in gui pixels (the screen width
     *               for a horizontal edge, the screen height for a vertical
     *               one)
     * @param margin pixels kept free at both ends of the scanline
     * @param spacing the minimum gap maintained between neighboring slots
     *
     * @throws IllegalArgumentException if any parameter is not finite, if
     *         extent is not positive, or if margin/spacing are negative
     */
    public DockCursor(Edge edge, double extent, double margin, double spacing) {
        if (extent <= 0 || !Double.isFinite(extent)) {
            throw new IllegalArgumentException("extent must be finite and positive: " + extent);
        }
        if (margin < 0 || !Double.isFinite(margin)) {
            throw new IllegalArgumentException("margin must be finite and non-negative: " + margin);
        }
        if (spacing < 0 || !Double.isFinite(spacing)) {
            throw new IllegalArgumentException("spacing must be finite and non-negative: " + spacing);
        }
        this.edge = edge;
        this.extent = extent;
        this.margin = margin;
        this.spacing = spacing;
    }

    public Edge edge() {
        return edge;
    }

    /** The full scanline length this cursor was built with. */
    public double extent() {
        return extent;
    }

    /**
     * Allocates a slot of {@code width} for {@code id}, first-fit in
     * coordinate order: the earliest gap (leading margin, holes between
     * slots, trailing space — in that order) that fits the width plus its
     * neighbor spacing.
     *
     * @return the new slot, or {@code null} when no gap fits — the caller
     *         degrades the element's content tier and retries with a smaller
     *         width
     * @throws IllegalArgumentException if the id is already allocated or the
     *         width is not finite and positive
     */
    public @Nullable Slot allocate(String id, double width) {
        requireNewId(id);
        requireWidth(width);
        double cursor = margin;
        for (Slot slot : slots) {
            if (cursor + width + spacing <= slot.start()) {
                return store(id, cursor, width);
            }
            cursor = slot.end() + spacing;
        }
        if (cursor + width <= extent - margin) {
            return store(id, cursor, width);
        }
        return null;
    }

    /**
     * Releases the slot owned by {@code id}, leaving a hole (use
     * {@link #compact} to close holes).
     *
     * @return whether a slot with that id existed
     */
    public boolean release(String id) {
        return slots.removeIf(slot -> slot.id().equals(id));
    }

    /**
     * Adapts the slot's width to a new content tier. The slot keeps its
     * {@code start}; the width only changes when it still fits before the
     * next slot and the trailing margin — adaptation never displaces
     * neighbors and never reorders.
     *
     * @return the resized slot, or {@code null} when the width does not fit
     * @throws IllegalArgumentException if the id is unknown or the width is
     *         not finite and positive
     */
    public @Nullable Slot resize(String id, double newWidth) {
        requireWidth(newWidth);
        int index = indexOf(id);
        if (index < 0) {
            throw new IllegalArgumentException("unknown slot id: " + id);
        }
        Slot slot = slots.get(index);
        double limit = index + 1 < slots.size() ? slots.get(index + 1).start() - spacing : extent - margin;
        if (slot.start() + newWidth > limit) {
            return null;
        }
        Slot resized = new Slot(id, edge, slot.start(), newWidth);
        slots.set(index, resized);
        return resized;
    }

    /**
     * Slides every slot toward the scanline origin, in coordinate order —
     * the stable compaction. Relative order is preserved by construction:
     * slots are visited and shifted strictly left-to-right, never sorted by
     * any other key.
     *
     * @return whether any slot moved
     */
    public boolean compact() {
        boolean moved = false;
        double cursor = margin;
        for (int i = 0; i < slots.size(); i++) {
            Slot slot = slots.get(i);
            if (slot.start() > cursor) {
                slots.set(i, new Slot(slot.id(), edge, cursor, slot.width()));
                moved = true;
            }
            cursor = Math.max(cursor, slots.get(i).end()) + spacing;
        }
        return moved;
    }

    /** The slot owned by {@code id}, if any. */
    public @Nullable Slot slot(String id) {
        int index = indexOf(id);
        return index < 0 ? null : slots.get(index);
    }

    /** An immutable snapshot of the slots, in coordinate order. */
    public List<Slot> slots() {
        return List.copyOf(slots);
    }

    /** The sum of all slot widths. */
    public double usedLength() {
        double total = 0;
        for (Slot slot : slots) {
            total += slot.width();
        }
        return total;
    }

    /**
     * The trailing room available to a further allocation — the length of the
     * largest allocatable interval at the scanline's far end.
     */
    public double freeLength() {
        double cursor = slots.isEmpty() ? margin : slots.getLast().end() + spacing;
        return Math.max(0, extent - margin - cursor);
    }

    private Slot store(String id, double start, double width) {
        Slot slot = new Slot(id, edge, start, width);
        slots.add(slot);
        slots.sort((a, b) -> Double.compare(a.start(), b.start()));
        return slot;
    }

    private int indexOf(String id) {
        for (int i = 0; i < slots.size(); i++) {
            if (slots.get(i).id().equals(id)) {
                return i;
            }
        }
        return -1;
    }

    private void requireNewId(String id) {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("slot id must not be empty");
        }
        if (indexOf(id) >= 0) {
            throw new IllegalArgumentException("slot id already allocated: " + id);
        }
    }

    private static void requireWidth(double width) {
        if (width <= 0 || !Double.isFinite(width)) {
            throw new IllegalArgumentException("width must be finite and positive: " + width);
        }
    }
}
