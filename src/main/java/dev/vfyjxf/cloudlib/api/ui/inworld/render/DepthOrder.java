package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.function.ToDoubleFunction;
import java.util.function.ToIntFunction;

/**
 * The world pass's far → near ordering, as pure data — the sequence every
 * item that shares the pass's single translucent draw must follow.
 * <p>
 * <b>Why far → near.</b> Nothing in the world pass writes depth, so the
 * depth buffer still holds only the level's opaque geometry: a UI quad passes
 * {@code LEQUAL} wherever the scene behind it is farther, which is everywhere.
 * The blend is premultiplied {@code ONE, ONE_MINUS_SRC_ALPHA} — over
 * compositing — so <em>draw order alone</em> decides which surface ends up in
 * front. Drawing a far item after a near one composites it over the near one,
 * exactly backwards: the far surface must be attenuated by the near one's
 * alpha, not stacked on top of it. Hence the far → near sequence.
 * <p>
 * <b>Stability.</b> Distance alone would flip near-equidistant items every
 * time the camera drifts across the tie boundary. A comparator that breaks
 * near-ties by the previous frame's order is not transitive (a~b and b~c by
 * stickiness, a~c by distance), which makes {@code List.sort} throw a
 * comparison-contract violation once enough items are up. So the sort runs in
 * two passes: near-ties are first chained into <em>clusters</em> off the
 * distance-sorted order (each item within {@code tieEps} of its sorted
 * neighbour joins its cluster), and the final order is the fully transitive
 * chain (cluster, previous frame's order, registration sequence). Items
 * inside a cluster keep their previous relative order across frames, and
 * every level of the chain is a total order.
 * <p>
 * Headless-pure: no GL, no game state — {@link WorldUiRenderer} hands it the
 * frame's items and sorts the result it returns.
 */
public final class DepthOrder {

    /** Within this distance (blocks), two items count as tied. */
    public static final double defaultTieEps = 0.05;

    private DepthOrder() {}

    /**
     * Orders {@code items} far → near, keeping near-tied items in their
     * previous relative order.
     *
     * @param items         the frame's items, in registration order
     * @param distance      the item's view distance in blocks — the primary key
     * @param sequence      the item's registration index — the final,
     *                      always-available tie-break
     * @param previousOrder the item's index in the last frame's returned
     *                      sequence, or {@code Integer.MAX_VALUE} for an item
     *                      that was not in it (a newcomer sorts last inside
     *                      its cluster)
     * @param tieEps        the distance below which two items count as tied
     * @return a new list in the pass's draw order — farthest item first
     */
    public static <T> List<T> farToNear(
        List<T> items,
        ToDoubleFunction<T> distance,
        ToIntFunction<T> sequence,
        ToIntFunction<T> previousOrder,
        double tieEps
    ) {
        List<T> ordered = new ArrayList<>(items);
        Map<T, Double> distances = new IdentityHashMap<>(ordered.size() * 2);
        for (T item : ordered) {
            distances.put(item, distance.applyAsDouble(item));
        }
        // far → near; exactly-equal distances fall back to the registration
        // sequence, so the primary pass is a total order of its own
        Comparator<T> byDistance = Comparator.comparingDouble((T item) -> distances.get(item)).reversed();
        ordered.sort(byDistance.thenComparingInt(sequence));
        // chain sorted neighbours within tieEps into clusters (cluster ids
        // come out in far → near order, and the chain is single-link:
        // a~b and b~c put all three in one cluster)
        Map<T, Integer> cluster = new IdentityHashMap<>(ordered.size() * 2);
        int clusterId = 0;
        double previous = Double.POSITIVE_INFINITY;
        for (T item : ordered) {
            double d = Objects.requireNonNull(distances.get(item));
            if (previous - d >= tieEps) clusterId++;
            cluster.put(item, clusterId);
            previous = d;
        }
        ordered.sort(
            Comparator.comparingInt((T item) -> cluster.get(item)).thenComparingInt(previousOrder)
                    .thenComparingInt(sequence)
        );
        return ordered;
    }
}
