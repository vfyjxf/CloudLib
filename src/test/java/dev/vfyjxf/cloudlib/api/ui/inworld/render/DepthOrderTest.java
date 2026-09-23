package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Random;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The world pass's ordering contract, against the production sort.
 * <p>
 * The pass writes no depth, so its draw order <em>is</em> its occlusion: the
 * sequence must hand the farthest item to the blend first, or a far surface
 * composites over a near one — the reported bug, a far panel landing on top of
 * a near translucent quad. These tests run the real
 * {@link DepthOrder#farToNear} headless.
 */
class DepthOrderTest {

    /**
     * One item's stable identity with this frame's distance — the shape the
     * renderer hands the sort: a panel or overlay object that keeps its
     * identity frame to frame while its measured distance moves.
     */
    private static final class Item {

        final String id;
        final int sequence;
        double distance;

        Item(String id, double distance, int sequence) {
            this.id = id;
            this.distance = distance;
            this.sequence = sequence;
        }
    }

    private static final double eps = DepthOrder.defaultTieEps;

    /** One frame's sort, folding the result back into the history the next frame reads. */
    private static List<String> frame(List<Item> items, Map<Item, Integer> lastOrder) {
        List<Item> ordered = DepthOrder.farToNear(
            items,
            item -> item.distance,
            item -> item.sequence,
            item -> lastOrder.getOrDefault(item, Integer.MAX_VALUE),
            eps
        );
        lastOrder.clear();
        for (int i = 0; i < ordered.size(); i++) {
            lastOrder.put(ordered.get(i), i);
        }
        List<String> ids = new ArrayList<>(ordered.size());
        for (Item item : ordered) {
            ids.add(item.id);
        }
        return ids;
    }

    /** The regression: the sequence is the blend's back-to-front order, so it starts at the farthest item. */
    @Test
    void theSequenceIsFarthestFirst() {
        List<Item> items = List.of(new Item("near", 2, 0), new Item("far", 30, 1), new Item("mid", 9, 2));
        assertEquals(List.of("far", "mid", "near"), frame(items, new IdentityHashMap<>()));
    }

    /** Items spaced past the tie window follow their distances, whatever last frame ordered them as. */
    @Test
    void aRealDistanceChangeBeatsLastFramesOrder() {
        Map<Item, Integer> lastOrder = new IdentityHashMap<>();
        Item a = new Item("a", 5, 0);
        Item b = new Item("b", 15, 1);
        Item c = new Item("c", 25, 2);
        assertEquals(List.of("c", "b", "a"), frame(new ArrayList<>(List.of(a, b, c)), lastOrder));

        // the camera swept past them: now a is the farthest
        a.distance = 25;
        b.distance = 15;
        c.distance = 5;
        assertEquals(
            List.of("a", "b", "c"),
            frame(new ArrayList<>(List.of(a, b, c)), lastOrder),
            "a gap past the tie window must reorder, never stick"
        );
    }

    /** Inside the tie window last frame's order holds — the hysteresis that keeps the pass from flickering. */
    @Test
    void nearTiedItemsKeepLastFramesOrder() {
        Map<Item, Integer> lastOrder = new IdentityHashMap<>();
        Item a = new Item("a", 10.00, 0);
        Item b = new Item("b", 10.02, 1);
        // a cold frame inside one cluster is registration-ordered, not
        // distance-ordered — the pair is a tie by definition
        assertEquals(List.of("a", "b"), frame(new ArrayList<>(List.of(a, b)), lastOrder));

        // the same pair, a hair closer together in the other order: the drift
        // is under the tie window, so the drawn order must not flip
        a.distance = 10.03;
        b.distance = 10.00;
        assertEquals(List.of("a", "b"), frame(new ArrayList<>(List.of(a, b)), lastOrder));
    }

    /** Exactly-equal distances resolve by registration, so a cold frame is deterministic. */
    @Test
    void equalDistancesFallBackToTheRegistrationSequence() {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            items.add(new Item("i" + i, 12.0, i));
        }
        Collections.shuffle(items, new Random(7));
        assertEquals(List.of("i0", "i1", "i2", "i3", "i4", "i5"), frame(items, new IdentityHashMap<>()));
    }

    /** The result is always a permutation of the frame's items — nothing is dropped or duplicated. */
    @Test
    void theSequenceIsAPermutation() {
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 64; i++) {
            items.add(new Item("i" + i, 1 + i * 0.5, i));
        }
        List<String> ordered = frame(items, new IdentityHashMap<>());
        assertEquals(items.size(), ordered.size());
        assertEquals(items.stream().map(item -> item.id).sorted().toList(), ordered.stream().sorted().toList());
    }

    /** Every item spaced past the tie window comes out strictly ordered by distance, cold frame or not. */
    @Test
    void separatedItemsNeverInvert() {
        Map<Item, Integer> lastOrder = new IdentityHashMap<>();
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 40; i++) {
            items.add(new Item("i" + i, 1 + i, i));
        }
        for (int round = 0; round < 4; round++) {
            // the same identities, their distances rotated by one slot per round —
            // every gap is one block, far past the tie window
            for (int i = 0; i < items.size(); i++) {
                items.get(i).distance = 1 + ((i + round) % items.size());
            }
            List<Item> expected = new ArrayList<>(items);
            expected.sort(Comparator.comparingDouble((Item item) -> -item.distance));
            List<String> ids = new ArrayList<>(expected.size());
            for (Item item : expected) {
                ids.add(item.id);
            }
            assertEquals(ids, frame(new ArrayList<>(items), lastOrder), "round " + round);
        }
    }

    /**
     * A long ladder of near-ties is the case that used to blow up: an
     * intransitive "sticky tie-break" comparator makes {@code List.sort} throw
     * a comparison-contract violation (and before that, silently scramble the
     * order). The chain must hold for hundreds of items across frames.
     */
    @Test
    void aLongLadderOfNearTiesKeepsTheComparisonContract() {
        Map<Item, Integer> lastOrder = new IdentityHashMap<>();
        Random random = new Random(11);
        List<Item> items = new ArrayList<>();
        for (int i = 0; i < 400; i++) {
            items.add(new Item("i" + i, 20 + i * 0.004, i));
        }
        for (int frame = 0; frame < 5; frame++) {
            for (Item item : items) {
                item.distance = 20 + item.sequence * 0.004 + random.nextDouble() * 0.01;
            }
            List<String> ordered = frame(items, lastOrder);
            assertEquals(items.size(), ordered.size());
            assertEquals(items.size(), ordered.stream().distinct().count());
        }
    }

    /** An empty frame is a no-op, not a throw. */
    @Test
    void anEmptyFrameSortsToNothing() {
        assertTrue(frame(List.of(), new IdentityHashMap<>()).isEmpty());
    }
}
