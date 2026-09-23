package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Map;
import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class SlotAssignerTest {

    private static final SlotAssigner.Costs plainCosts = SlotAssigner.Costs.of(1.0, 0.0, 0.0);

    private static SlotAssigner.Element element(String id, double x, double y) {
        return new SlotAssigner.Element(id, x, y);
    }

    private static SlotAssigner.Slot slot(String id, double x, double y) {
        return new SlotAssigner.Slot(id, x, y);
    }

    @Test
    void greedyFirstPlacementTakesTheNearestFreeSlot() {
        SlotAssigner assigner = new SlotAssigner(0, plainCosts);

        SlotAssigner.Result result = assigner.assign(
            List.of(element("a", 0, 0), element("b", 100, 0)),
            List.of(slot("s1", 0, 0), slot("s2", 100, 0), slot("s3", 500, 0)),
            Map.of()
        );

        assertEquals("s1", Objects.requireNonNull(result.of("a")).slotId());
        assertEquals("s2", Objects.requireNonNull(result.of("b")).slotId());
        assertEquals(0, result.movesUsed());
    }

    @Test
    void zeroBudgetIsPureStickiness() {
        SlotAssigner assigner = new SlotAssigner(0, plainCosts);

        SlotAssigner.Result result = assigner.assign(
            List.of(element("a", 0, 0), element("b", 100, 0)),
            List.of(slot("s1", 0, 0), slot("s2", 100, 0)),
            Map.of("a", "s2", "b", "s1")
        );

        assertEquals("s2", Objects.requireNonNull(result.of("a")).slotId());
        assertEquals("s1", Objects.requireNonNull(result.of("b")).slotId());
        assertEquals(0, result.movesUsed());
    }

    @Test
    void budgetCapsImprovingMovesPerEpoch() {
        SlotAssigner assigner = new SlotAssigner(1, plainCosts);

        SlotAssigner.Result result = assigner.assign(
            List.of(element("a", 0, 0), element("b", 50, 0)),
            List.of(slot("s1", 0, 0), slot("s2", 30, 0), slot("s3", 200, 0)),
            Map.of("a", "s2", "b", "s3")
        );

        // both want s1; b's gain (150) beats a's (30), and the budget allows one move
        assertEquals("s2", Objects.requireNonNull(result.of("a")).slotId());
        assertEquals("s1", Objects.requireNonNull(result.of("b")).slotId());
        assertEquals(1, result.movesUsed());
    }

    @Test
    void movesNeverExceedTheBudget() {
        SlotAssigner.Costs costs = SlotAssigner.Costs.of(1.0, 0.0, 0.0);
        List<SlotAssigner.Slot> slots = List
                .of(slot("s1", 0, 0), slot("s2", 10, 0), slot("s3", 20, 0), slot("s4", 400, 0), slot("s5", 410, 0));
        List<SlotAssigner.Element> elements = List.of(element("a", 0, 0), element("b", 10, 0), element("c", 20, 0));

        for (int budget = 0; budget <= 3; budget++) {
            SlotAssigner assigner = new SlotAssigner(budget, costs);
            SlotAssigner.Result result = assigner.assign(elements, slots, Map.of("a", "s4", "b", "s5", "c", "s3"));
            assertTrue(result.movesUsed() <= budget, "movesUsed " + result.movesUsed() + " exceeded budget " + budget);
        }
    }

    @Test
    void switchPenaltyBlocksMarginalMoves() {
        SlotAssigner.Costs penalizing = SlotAssigner.Costs.of(1.0, 20.0, 0.0);
        SlotAssigner assigner = new SlotAssigner(4, penalizing);

        SlotAssigner.Result result = assigner
                .assign(List.of(element("a", 0, 0)), List.of(slot("s1", 0, 0), slot("s2", 12, 0)), Map.of("a", "s2"));

        assertEquals("s2", Objects.requireNonNull(result.of("a")).slotId());
        assertEquals(0, result.movesUsed());
    }

    @Test
    void incumbentDiscountHoldsTheHeldSlot() {
        SlotAssigner.Costs discounted = SlotAssigner.Costs.of(1.0, 5.0, 0.5);
        SlotAssigner withoutDiscount = new SlotAssigner(4, SlotAssigner.Costs.of(1.0, 5.0, 0.0));

        // anchor at 0; incumbent s2 at 8: undiscounted the move saves 8 against the
        // 5 penalty and happens; discounted the incumbent only costs 4, less than
        // the penalty — held
        SlotAssigner.Result moving = withoutDiscount
                .assign(List.of(element("a", 0, 0)), List.of(slot("s1", 0, 0), slot("s2", 8, 0)), Map.of("a", "s2"));
        assertEquals("s1", Objects.requireNonNull(moving.of("a")).slotId());

        SlotAssigner assigner = new SlotAssigner(4, discounted);
        SlotAssigner.Result held = assigner
                .assign(List.of(element("a", 0, 0)), List.of(slot("s1", 0, 0), slot("s2", 8, 0)), Map.of("a", "s2"));
        assertEquals("s2", Objects.requireNonNull(held.of("a")).slotId());
        assertEquals(0, held.movesUsed());
    }

    @Test
    void vanishedIncumbentRehomesWithoutSpendingBudget() {
        SlotAssigner assigner = new SlotAssigner(0, plainCosts);

        SlotAssigner.Result result = assigner
                .assign(List.of(element("a", 0, 0)), List.of(slot("s1", 0, 0), slot("s2", 90, 0)), Map.of("a", "gone"));

        assertEquals("s1", Objects.requireNonNull(result.of("a")).slotId());
        assertEquals(0, result.movesUsed());
    }

    @Test
    void overflowLeavesElementsUnassigned() {
        SlotAssigner assigner = new SlotAssigner(2, plainCosts);

        SlotAssigner.Result result = assigner.assign(
            List.of(element("a", 0, 0), element("b", 5, 0), element("c", 10, 0)),
            List.of(slot("s1", 0, 0), slot("s2", 6, 0)),
            Map.of()
        );

        assertTrue(Objects.requireNonNull(result.of("a")).assigned());
        assertTrue(Objects.requireNonNull(result.of("b")).assigned());
        assertFalse(Objects.requireNonNull(result.of("c")).assigned());
        assertTrue(Double.isNaN(Objects.requireNonNull(result.of("c")).cost()));
        assertEquals(3, result.assignments().size());
    }

    @Test
    void sameInputYieldsTheSameOutput() {
        List<SlotAssigner.Element> elements = List
                .of(element("a", 0, 0), element("b", 100, 40), element("c", 300, 10), element("d", 60, 90));
        List<SlotAssigner.Slot> slots = List.of(
            slot("s1", 10, 10),
            slot("s2", 90, 30),
            slot("s3", 280, 0),
            slot("s4", 50, 100),
            slot("s5", 400, 400)
        );
        Map<String, String> incumbents = Map.of("a", "s5", "c", "s3");

        SlotAssigner first = new SlotAssigner(2, SlotAssigner.Costs.of(1.0, 15.0, 0.2));
        SlotAssigner second = new SlotAssigner(2, SlotAssigner.Costs.of(1.0, 15.0, 0.2));

        assertEquals(first.assign(elements, slots, incumbents), second.assign(elements, slots, incumbents));
        assertEquals(first.assign(elements, slots, incumbents), second.assign(elements, slots, incumbents));
    }

    @Test
    void rejectsInvalidUse() {
        SlotAssigner assigner = new SlotAssigner(1, plainCosts);

        assertThrows(IllegalArgumentException.class, () -> new SlotAssigner(-1, plainCosts));
        assertThrows(IllegalArgumentException.class, () -> SlotAssigner.Costs.of(0, 0, 0));
        assertThrows(IllegalArgumentException.class, () -> SlotAssigner.Costs.of(1, -1, 0));
        assertThrows(IllegalArgumentException.class, () -> SlotAssigner.Costs.of(1, 0, 1));
        assertThrows(
            IllegalArgumentException.class,
            () -> assigner.assign(List.of(element("a", 0, 0), element("a", 1, 1)), List.of(slot("s1", 0, 0)), Map.of())
        );
        assertThrows(
            IllegalArgumentException.class,
            () -> assigner.assign(List.of(element("a", 0, 0)), List.of(slot("s1", 0, 0), slot("s1", 1, 1)), Map.of())
        );
    }
}
