package dev.vfyjxf.nimbusprojection.internal;

import net.minecraft.core.BlockPos;

import java.util.ArrayList;
import java.util.List;

/**
 * The trail of world-container positions a drag has crossed — the "world is
 * UI" counterpart of a stroke path. Small, ordered and deduplicated so the
 * commit list is exactly the destinations the player swept over, earliest
 * first.
 */
public final class DragTrail {

    /** A drag only ever spans a handful of visible containers. */
    public static final int capacity = 8;

    private final ArrayList<BlockPos> trail = new ArrayList<>(capacity);
    private BlockPos last;

    /**
     * Offers the position currently under the player's ray. Adds it to the
     * trail when it is a new container, ignoring repeats of the immediate last
     * position and positions already in the trail. The immediate-last position
     * is tracked separately so the trail does not grow every frame while the
     * player hovers the same block.
     *
     * @return true when {@code pos} was newly appended
     */
    public boolean offer(BlockPos pos) {
        if (pos.equals(last)) return false;
        last = pos;
        if (trail.contains(pos) || trail.size() >= capacity) return false;
        trail.add(pos);
        return true;
    }

    /** Resets the consecutive-position latch without clearing the trail — used
     *  when the ray leaves containers entirely (air/null offer). */
    public void leave() {
        last = null;
    }

    public List<BlockPos> targets() {
        return List.copyOf(trail);
    }

    public int size() {
        return trail.size();
    }

    public boolean isEmpty() {
        return trail.isEmpty();
    }

    public void clear() {
        trail.clear();
        last = null;
    }
}
