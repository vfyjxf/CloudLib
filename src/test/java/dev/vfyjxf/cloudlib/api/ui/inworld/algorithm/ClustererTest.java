package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class ClustererTest {

    private static Clusterer.Member member(String id, double x, double y) {
        return new Clusterer.Member(id, x, y);
    }

    private static List<List<String>> structure(List<Clusterer.Cluster> clusters) {
        return clusters.stream().map(Clusterer.Cluster::memberIds).toList();
    }

    @Test
    void twoTightGroupsClusterSeparatelyWithRepresentatives() {
        Clusterer clusterer = new Clusterer(Clusterer.Config.of(0.7, 100));
        List<Clusterer.Member> snapshot = List.of(
                member("a", 0, 0),
                member("b", 25, 0),
                member("c", 0, 25),
                member("d", 500, 10),
                member("e", 525, 10),
                member("f", 500, 35));

        List<Clusterer.Cluster> clusters = clusterer.cluster(snapshot);

        assertEquals(List.of(List.of("a", "b", "c"), List.of("d", "e", "f")), structure(clusters));
        for (Clusterer.Cluster cluster : clusters) {
            assertEquals(3, cluster.size());
        }
        Clusterer.Cluster first = clusters.get(0);
        assertEquals(25.0 / 3.0, first.centerX(), 1.0e-9);
        assertEquals(25.0 / 3.0, first.centerY(), 1.0e-9);
        // (25/3, 25/3): a is at distance ~11.8, b and c at ~15.2 — a represents
        assertEquals("a", first.representative());
        assertEquals(clusters, clusterer.history());
    }

    @Test
    void emptyAndSingletonSnapshotsAreHandled() {
        Clusterer clusterer = new Clusterer(Clusterer.Config.of(0.7, 100));

        assertEquals(List.of(), clusterer.cluster(List.of()));

        List<Clusterer.Cluster> single = clusterer.cluster(List.of(member("only", 10, 10)));
        assertEquals(1, single.size());
        assertEquals(List.of("only"), single.get(0).memberIds());
        assertEquals("only", single.get(0).representative());
    }

    @Test
    void jitterInsideTheHysteresisBandKeepsTheStructure() {
        Clusterer clusterer = new Clusterer(Clusterer.Config.of(0.7, 100));

        clusterer.cluster(List.of(member("a", 0, 0), member("b", 40, 0)));

        // stretch the pair to 120 (merge zone was d < 2R − R/α ≈ 143): still merged
        assertEquals(
                List.of(List.of("a", "b")),
                structure(clusterer.cluster(List.of(member("a", 0, 0), member("b", 120, 0)))));

        // and wobble it back and forth — the structure must not flip
        for (int i = 0; i < 12; i++) {
            double x = (i % 2 == 0) ? 60 : 110;
            assertEquals(
                    List.of(List.of("a", "b")),
                    structure(clusterer.cluster(List.of(member("a", 0, 0), member("b", x, 0)))));
        }
    }

    @Test
    void driftBeyondTheSplitThresholdSplitsTheCluster() {
        Clusterer clusterer = new Clusterer(Clusterer.Config.of(0.7, 100));

        clusterer.cluster(List.of(member("a", 0, 0), member("b", 40, 0)));

        // 2R/α... the split boundary is d > R/α ≈ 143; at 200 the dividend cannot pay
        List<List<String>> split = structure(clusterer.cluster(List.of(member("a", 0, 0), member("b", 200, 0))));
        assertEquals(List.of(List.of("a"), List.of("b")), split);
    }

    @Test
    void alphaOneFollowsTheSnapshotAlphaLowerResistsIt() {
        List<Clusterer.Member> firstEpoch = List.of(member("a", 0, 0), member("b", 40, 0));
        List<Clusterer.Member> drifted = List.of(member("a", 0, 0), member("b", 130, 0));

        Clusterer snapshotDriven = new Clusterer(Clusterer.Config.of(1.0, 100));
        snapshotDriven.cluster(firstEpoch);
        assertEquals(List.of(List.of("a"), List.of("b")), structure(snapshotDriven.cluster(drifted)));

        Clusterer historyDriven = new Clusterer(Clusterer.Config.of(0.7, 100));
        historyDriven.cluster(firstEpoch);
        assertEquals(List.of(List.of("a", "b")), structure(historyDriven.cluster(drifted)));
    }

    @Test
    void resetDropsTheHistoryDividend() {
        Clusterer clusterer = new Clusterer(Clusterer.Config.of(0.7, 100));

        clusterer.cluster(List.of(member("a", 0, 0), member("b", 40, 0)));
        clusterer.reset();

        // without history the pair at 130 no longer earns its merge
        assertEquals(
                List.of(List.of("a"), List.of("b")),
                structure(clusterer.cluster(List.of(member("a", 0, 0), member("b", 130, 0)))));
    }

    @Test
    void theSameSequenceYieldsTheSameClusters() {
        List<List<Clusterer.Member>> epochs = List.of(
                List.of(member("a", 0, 0), member("b", 30, 5), member("c", 400, 0), member("d", 420, 8)),
                List.of(member("a", 5, 0), member("b", 35, 3), member("c", 405, 2), member("d", 415, 12)),
                List.of(member("a", 8, 2), member("b", 60, 4), member("c", 402, 0), member("d", 430, 6)));

        Clusterer first = new Clusterer(Clusterer.Config.of(0.6, 100));
        Clusterer second = new Clusterer(Clusterer.Config.of(0.6, 100));

        for (List<Clusterer.Member> epoch : epochs) {
            assertEquals(first.cluster(epoch), second.cluster(epoch));
        }
    }

    @Test
    void rejectsInvalidUse() {
        Clusterer clusterer = new Clusterer(Clusterer.Config.of(0.7, 100));

        assertThrows(IllegalArgumentException.class, () -> Clusterer.Config.of(-0.1, 100));
        assertThrows(IllegalArgumentException.class, () -> Clusterer.Config.of(1.1, 100));
        assertThrows(IllegalArgumentException.class, () -> Clusterer.Config.of(0.7, 0));
        assertThrows(
                IllegalArgumentException.class, () -> clusterer.cluster(List.of(member("a", 0, 0), member("a", 1, 1))));
        assertThrows(IllegalArgumentException.class, () -> clusterer.cluster(List.of(member("a", Double.NaN, 0))));
    }

    @Test
    void farApartPointsNeverMergeEvenWithHistory() {
        Clusterer clusterer = new Clusterer(Clusterer.Config.of(0.5, 100));

        for (int epoch = 0; epoch < 5; epoch++) {
            List<Clusterer.Cluster> clusters = clusterer.cluster(List.of(member("a", 0, 0), member("b", 1000, 0)));
            assertEquals(2, clusters.size());
            assertTrue(clusters.get(0).memberIds().size() == 1
                    && clusters.get(1).memberIds().size() == 1);
        }
    }
}
