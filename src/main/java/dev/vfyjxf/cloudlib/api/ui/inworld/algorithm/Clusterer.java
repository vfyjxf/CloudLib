package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

/**
 * Evolutionary clustering (§3.0): each epoch's clustering is scored as
 * {@code score = α·snapshotQuality + (1−α)·historyConsistency} — the α knob
 * directly trades sensitivity for stability. α = 1 clusters each snapshot on
 * its own merits alone; lower α makes the previous epoch's structure
 * increasingly hard to change.
 * <p>
 * Both terms are built from pairwise contributions so they share a scale:
 * for a pair of members at distance {@code d}, the snapshot term contributes
 * {@code max(−1, 1 − d/mergeRadius)} when clustered together (close pairs
 * earn, distant pairs <em>pay</em>), and the history term contributes
 * {@code +1} when togetherness matches the previous epoch, {@code −1} when it
 * flips. The clustering is then greedy agglomerative: starting from
 * singletons, the pair of clusters with the largest strictly positive merge
 * gain is merged, until no merge gains.
 * <p>
 * The hysteresis falls out of the formula. With no history, a pair merges
 * only inside {@code d < mergeRadius·(2 − 1/α)} (the consistency debt must be
 * paid); once merged, the pair earns its history dividend every following
 * epoch and only splits beyond {@code d > mergeRadius/α} (the snapshot debt
 * must exceed the dividend). Between those radii — the jitter band — the
 * structure simply does not change. Iteration order is the caller's member
 * list; ties keep the earlier pair; maps are membership-only. Same inputs,
 * same clusters.
 */
public final class Clusterer {

    /**
     * @param alpha the snapshot/history trade-off, in {@code [0, 1]}
     * @param mergeRadius the distance scale in gui pixels at which a pair is
     *        "close"; positive
     */
    public record Config(double alpha, double mergeRadius) {

        public Config {
            if (!Double.isFinite(alpha) || alpha < 0 || alpha > 1) {
                throw new IllegalArgumentException("alpha must be in [0, 1]: " + alpha);
            }
            if (!Double.isFinite(mergeRadius) || mergeRadius <= 0) {
                throw new IllegalArgumentException("mergeRadius must be finite and positive: " + mergeRadius);
            }
        }

        public static Config of(double alpha, double mergeRadius) {
            return new Config(alpha, mergeRadius);
        }
    }

    /** One element to cluster, at its screen position. */
    public record Member(String id, double x, double y) {}

    /**
     * One output cluster: member ids in snapshot order, the centroid, and
     * the representative — the member closest to the centroid (ties keep the
     * earlier member).
     */
    public record Cluster(List<String> memberIds, double centerX, double centerY, String representative) {

        public Cluster {
            memberIds = List.copyOf(memberIds);
        }

        public int size() {
            return memberIds.size();
        }
    }

    private final Config config;
    private List<Cluster> history = List.of();

    public Clusterer(Config config) {
        this.config = config;
    }

    public Config config() {
        return config;
    }

    /** The previous epoch's clusters — the history the next call scores against. */
    public List<Cluster> history() {
        return history;
    }

    /** Drops the history (a scene change, a teleport — anything that voids consistency). */
    public void reset() {
        history = List.of();
    }

    /**
     * Clusters one epoch's snapshot; the result becomes the next call's
     * history.
     *
     * @throws IllegalArgumentException if member ids are duplicated or
     *         positions are not finite
     */
    public List<Cluster> cluster(List<Member> snapshot) {
        Map<String, Integer> previousIndex = indexHistory();
        for (Member member : snapshot) {
            if (!Double.isFinite(member.x()) || !Double.isFinite(member.y())) {
                throw new IllegalArgumentException("member position must be finite: " + member);
            }
        }

        double[][] distance = distances(snapshot);
        List<List<Integer>> clusters = new ArrayList<>();
        for (int i = 0; i < snapshot.size(); i++) {
            List<Integer> singleton = new ArrayList<>();
            singleton.add(i);
            clusters.add(singleton);
        }

        while (true) {
            int bestA = -1;
            int bestB = -1;
            double bestGain = 0;
            for (int a = 0; a < clusters.size(); a++) {
                for (int b = a + 1; b < clusters.size(); b++) {
                    double gain = mergeGain(snapshot, clusters.get(a), clusters.get(b), distance, previousIndex);
                    if (gain > bestGain) {
                        bestGain = gain;
                        bestA = a;
                        bestB = b;
                    }
                }
            }
            if (bestA < 0) {
                break;
            }
            clusters.get(bestA).addAll(clusters.get(bestB));
            clusters.remove(bestB);
        }

        List<Cluster> result = new ArrayList<>(clusters.size());
        for (List<Integer> members : clusters) {
            List<Integer> ordered = new ArrayList<>(members);
            ordered.sort(Integer::compareTo);
            result.add(toCluster(snapshot, ordered));
        }
        history = List.copyOf(result);
        return result;
    }

    private Map<String, Integer> indexHistory() {
        Map<String, Integer> index = new HashMap<>();
        for (int i = 0; i < history.size(); i++) {
            for (String id : history.get(i).memberIds()) {
                index.put(id, i);
            }
        }
        return index;
    }

    private double mergeGain(
            List<Member> snapshot,
            List<Integer> a,
            List<Integer> b,
            double[][] distance,
            Map<String, Integer> previousIndex) {
        double gain = 0;
        for (int i : a) {
            for (int j : b) {
                double snapshotTerm = Math.max(-1.0, 1.0 - distance[i][j] / config.mergeRadius());
                Integer prevI = previousIndex.get(snapshot.get(i).id());
                Integer prevJ = previousIndex.get(snapshot.get(j).id());
                boolean wasTogether = prevI != null && prevI.equals(prevJ);
                double historyTerm = wasTogether ? 1.0 : -1.0;
                gain += config.alpha() * snapshotTerm + (1.0 - config.alpha()) * historyTerm;
            }
        }
        return gain;
    }

    private static Cluster toCluster(List<Member> snapshot, List<Integer> memberIndices) {
        List<String> ids = new ArrayList<>(memberIndices.size());
        double x = 0;
        double y = 0;
        for (int index : memberIndices) {
            Member member = snapshot.get(index);
            ids.add(member.id());
            x += member.x();
            y += member.y();
        }
        x /= memberIndices.size();
        y /= memberIndices.size();

        String representative = null;
        double best = Double.POSITIVE_INFINITY;
        for (int index : memberIndices) {
            Member member = snapshot.get(index);
            double dx = member.x() - x;
            double dy = member.y() - y;
            double distance = Math.sqrt(dx * dx + dy * dy);
            if (distance < best) {
                best = distance;
                representative = member.id();
            }
        }
        return new Cluster(ids, x, y, representative);
    }

    private static double[][] distances(List<Member> snapshot) {
        Map<String, Integer> seen = new HashMap<>();
        double[][] distance = new double[snapshot.size()][snapshot.size()];
        for (int i = 0; i < snapshot.size(); i++) {
            if (seen.put(snapshot.get(i).id(), i) != null) {
                throw new IllegalArgumentException(
                        "duplicate member id: " + snapshot.get(i).id());
            }
            for (int j = 0; j < i; j++) {
                double dx = snapshot.get(i).x() - snapshot.get(j).x();
                double dy = snapshot.get(i).y() - snapshot.get(j).y();
                distance[i][j] = distance[j][i] = Math.sqrt(dx * dx + dy * dy);
            }
        }
        return distance;
    }
}
