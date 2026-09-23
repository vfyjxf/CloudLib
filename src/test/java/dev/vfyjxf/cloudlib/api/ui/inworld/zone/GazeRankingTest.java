package dev.vfyjxf.cloudlib.api.ui.inworld.zone;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Comparator;
import java.util.List;
import java.util.function.Predicate;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GazeRankingTest {

    private static final Comparator<GazeRanking.GazeEntry> order = GazeRanking.gazeOrder();

    private static GazeRanking.GazeEntry entry(
        String id,
        double screenDistance,
        double worldDistance,
        int priority,
        long registrationIndex
    ) {
        return new GazeRanking.GazeEntry(id, screenDistance, worldDistance, priority, registrationIndex);
    }

    @Test
    void screenDistanceOutranksEverything() {
        GazeRanking.GazeEntry near = entry("near", 50.0, 999.0, 0, 9);
        GazeRanking.GazeEntry far = entry("far", 60.0, 0.0, 100, 0);

        assertTrue(order.compare(near, far) < 0);
        assertTrue(order.compare(far, near) > 0);
    }

    @Test
    void worldDistanceBreaksScreenDistanceTies() {
        GazeRanking.GazeEntry closer = entry("closer", 50.0, 100.0, 0, 9);
        GazeRanking.GazeEntry farther = entry("farther", 50.0, 200.0, 100, 0);

        assertTrue(order.compare(closer, farther) < 0);
        assertTrue(order.compare(farther, closer) > 0);
    }

    @Test
    void higherPriorityWinsWhenDistancesTie() {
        GazeRanking.GazeEntry urgent = entry("urgent", 50.0, 100.0, 5, 9);
        GazeRanking.GazeEntry casual = entry("casual", 50.0, 100.0, 3, 0);

        assertTrue(order.compare(urgent, casual) < 0);
        assertTrue(order.compare(casual, urgent) > 0);
    }

    @Test
    void registrationIndexIsTheFinalStableTieBreak() {
        GazeRanking.GazeEntry first = entry("first", 50.0, 100.0, 3, 1);
        GazeRanking.GazeEntry second = entry("second", 50.0, 100.0, 3, 2);

        assertTrue(order.compare(first, second) < 0);
        assertTrue(order.compare(second, first) > 0);
    }

    @Test
    void equalKeysUpToRegistrationNeverSwapUnderSort() {
        // three priority groups, each with equal screen/world keys: the sort
        // must keep registration order inside every group
        List<GazeRanking.GazeEntry> entries = new ArrayList<>(
            List.of(
                entry("a3", 50.0, 100.0, 3, 3),
                entry("b1", 50.0, 100.0, 3, 1),
                entry("c2", 50.0, 100.0, 3, 2),
                entry("d1", 10.0, 100.0, 0, 7),
                entry("e2", 70.0, 5.0, 9, 0),
                entry("f2", 10.0, 100.0, 0, 6)
            )
        );

        entries.sort(order);

        assertEquals(
            List.of("f2", "d1", "b1", "c2", "a3", "e2"),
            entries.stream().map(GazeRanking.GazeEntry::id).toList()
        );
    }

    @Test
    void entryFactoryComputesTheScreenDistance() {
        GazeRanking.GazeEntry entry = GazeRanking
                .entry("a", new FloatPos(240, 135), new FloatPos(300, 187), 64.0, 2, 3);

        assertEquals(Math.hypot(60, 52), entry.screenDistance(), 1.0e-12);
        assertEquals(64.0, entry.worldDistance(), 0.0);
        assertEquals("a", entry.id());
        assertEquals(2, entry.priority());
        assertEquals(3, entry.registrationIndex());
    }

    @Test
    void gazeRadiusFiltersInclusivelyAtTheBoundary() {
        Predicate<GazeRanking.GazeEntry> within = GazeRanking.withinGazeRadius(100.0);

        assertTrue(within.test(entry("in", 0.0, 0.0, 0, 0)));
        assertTrue(within.test(entry("edge", 100.0, 0.0, 0, 0)));
        assertFalse(within.test(entry("out", 100.5, 0.0, 0, 0)));
        assertFalse(within.test(entry("far", 400.0, 0.0, 0, 0)));

        List<GazeRanking.GazeEntry> entries = List
                .of(entry("a", 30.0, 0.0, 0, 0), entry("b", 130.0, 0.0, 0, 1), entry("c", 99.0, 0.0, 0, 2));
        assertEquals(List.of("a", "c"), entries.stream().filter(within).map(GazeRanking.GazeEntry::id).toList());
        // a zero radius keeps only what is exactly on the crosshair
        assertTrue(GazeRanking.withinGazeRadius(0.0).test(entry("on", 0.0, 0.0, 0, 0)));
        assertFalse(GazeRanking.withinGazeRadius(0.0).test(entry("off", 0.1, 0.0, 0, 0)));
    }

    @Test
    void rejectsInvalidUse() {
        assertThrows(IllegalArgumentException.class, () -> entry("a", -1.0, 0.0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> entry("a", 0.0, Double.NaN, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> entry("a", 0.0, 0.0, 0, -1));
        assertThrows(NullPointerException.class, () -> entry(null, 0.0, 0.0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> GazeRanking.withinGazeRadius(-1.0));
        assertThrows(IllegalArgumentException.class, () -> GazeRanking.withinGazeRadius(Double.NaN));
        assertThrows(NullPointerException.class, () -> GazeRanking.entry("a", new FloatPos(0, 0), null, 0.0, 0, 0));
    }
}
