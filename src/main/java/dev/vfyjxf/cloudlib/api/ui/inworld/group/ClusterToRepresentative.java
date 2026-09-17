package dev.vfyjxf.cloudlib.api.ui.inworld.group;

/**
 * {@link GroupStrategy} for density aggregation (nameplates crowding at
 * range): members are clustered by the evolutionary {@code Clusterer} —
 * {@code score = α·snapshotQuality + (1−α)·historyConsistency} — so clusters
 * split and merge with hysteresis instead of flapping. Each cluster keeps
 * exactly one visible member, the <em>representative</em> (closest to the
 * cluster centroid, ties keep canonical order), placed at its own position;
 * the other members aggregate into it and surface as the representative's
 * "+N" count. When the cluster count exceeds {@link #maxVisibleClusters},
 * the closest representatives merge (aggregating their members) until the
 * cap holds; surplus members beyond {@link #maxAggregatedPerCluster} hide.
 * <p>
 * {@link #expandOnHover} is the declarative contract that the representative
 * is hover-expandable: the layout result carries the full member list per
 * cluster, so a hover-aware surface can expand the cluster without
 * re-running the engine.
 *
 * @param alpha the snapshot/history trade-off in {@code [0, 1]}
 * @param mergeRadius the distance scale in gui pixels
 * @param maxVisibleClusters the visible-representative cap
 * @param maxAggregatedPerCluster how many members a representative absorbs
 *        before the rest hide
 * @param expandOnHover whether the representative expands on hover
 */
public record ClusterToRepresentative(
        double alpha, double mergeRadius, int maxVisibleClusters, int maxAggregatedPerCluster, boolean expandOnHover)
        implements GroupStrategy {

    public ClusterToRepresentative {
        if (!Double.isFinite(alpha) || alpha < 0 || alpha > 1) {
            throw new IllegalArgumentException("alpha must be in [0, 1]: " + alpha);
        }
        if (!Double.isFinite(mergeRadius) || mergeRadius <= 0) {
            throw new IllegalArgumentException("mergeRadius must be finite and positive: " + mergeRadius);
        }
        if (maxVisibleClusters < 1) {
            throw new IllegalArgumentException("maxVisibleClusters must be at least 1: " + maxVisibleClusters);
        }
        if (maxAggregatedPerCluster < 0) {
            throw new IllegalArgumentException(
                    "maxAggregatedPerCluster must not be negative: " + maxAggregatedPerCluster);
        }
    }

    /** The nameplate-baseline preset: α 0.6, 120 px merge radius, 8 visible. */
    public static ClusterToRepresentative of() {
        return new ClusterToRepresentative(0.6, 120.0, 8, 16, true);
    }
}
