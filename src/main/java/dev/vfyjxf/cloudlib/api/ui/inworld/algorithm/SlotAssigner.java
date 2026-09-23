package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * Online slot assignment with a <em>recourse budget</em> — the formal answer
 * to dynamic assignment (§3.0): each epoch at most {@code K} deliberate
 * relocations are allowed; {@code K = 0} degenerates to pure stickiness.
 * <p>
 * The cost model is a linear combination with adjustable weights: the anchor
 * term {@code anchorWeight · distance(element anchor, slot position)}, plus a
 * flat {@code switchPenalty} charged to any move, with the incumbent slot
 * getting a multiplicative {@code incumbentDiscount} — the hysteresis that
 * makes leaving a held slot harder than arriving at a slightly better one.
 * <p>
 * Each {@link #assign} call is one epoch:
 * <ol>
 *   <li><strong>Sticky claims</strong> — every element keeps its incumbent
 *       slot if that slot still exists. Free; consumes no budget.</li>
 *   <li><strong>Greedy placement</strong> — homeless elements (new ones, or
 *       those whose incumbent slot vanished) take their cheapest free slot in
 *       element order, ties broken by slot order. Also free.</li>
 *   <li><strong>Recourse</strong> — while budget remains, the single most
 *       improving move (switch-penalty and incumbent discount already priced
 *       in) is applied, then re-scanned. Each applied move consumes one unit
 *       of budget.</li>
 * </ol>
 * Deterministic by construction: iteration is over the caller's element and
 * slot lists only; maps are used for membership, never for ordering; there is
 * no randomness. The same inputs always produce the same {@link Result}.
 */
public final class SlotAssigner {

    /**
     * The cost weights.
     *
     * @param anchorWeight weight of the anchor distance; must be positive
     * @param switchPenalty flat cost charged to every executed move;
     *        non-negative
     * @param incumbentDiscount multiplicative discount applied to the cost of
     *        an element's incumbent slot, in {@code [0, 1)} — the incumbent
     *        hysteresis
     */
    public record Costs(double anchorWeight, double switchPenalty, double incumbentDiscount) {

        public Costs {
            if (!Double.isFinite(anchorWeight) || anchorWeight <= 0) {
                throw new IllegalArgumentException("anchorWeight must be finite and positive: " + anchorWeight);
            }
            if (!Double.isFinite(switchPenalty) || switchPenalty < 0) {
                throw new IllegalArgumentException("switchPenalty must be finite and non-negative: " + switchPenalty);
            }
            if (!Double.isFinite(incumbentDiscount) || incumbentDiscount < 0 || incumbentDiscount >= 1) {
                throw new IllegalArgumentException("incumbentDiscount must be in [0, 1): " + incumbentDiscount);
            }
        }

        public static Costs of(double anchorWeight, double switchPenalty, double incumbentDiscount) {
            return new Costs(anchorWeight, switchPenalty, incumbentDiscount);
        }
    }

    /** An element to place, anchored at a screen position it wants to be near. */
    public record Element(String id, double anchorX, double anchorY) {}

    /** A placeable slot at a screen position. */
    public record Slot(String id, double x, double y) {}

    /**
     * One element's outcome. {@code cost} is the anchor term of the final
     * slot ({@link Double#NaN} when unassigned — there were more elements
     * than slots); the caller degrades unassigned elements.
     */
    public record Assignment(String elementId, @Nullable String slotId, double cost) {

        public boolean assigned() {
            return slotId != null;
        }
    }

    /** One epoch's outcome: assignments in element order, plus the budget spent. */
    public record Result(List<Assignment> assignments, int movesUsed) {

        public Result {
            assignments = List.copyOf(assignments);
        }

        public @Nullable Assignment of(String elementId) {
            for (Assignment assignment : assignments) {
                if (assignment.elementId().equals(elementId)) {
                    return assignment;
                }
            }
            return null;
        }
    }

    private final int maxMovesPerEpoch;
    private final Costs costs;

    /**
     * @throws IllegalArgumentException if maxMovesPerEpoch is negative
     */
    public SlotAssigner(int maxMovesPerEpoch, Costs costs) {
        if (maxMovesPerEpoch < 0) {
            throw new IllegalArgumentException("maxMovesPerEpoch must not be negative: " + maxMovesPerEpoch);
        }
        this.maxMovesPerEpoch = maxMovesPerEpoch;
        this.costs = costs;
    }

    /**
     * Runs one assignment epoch.
     *
     * @param elements the elements to place, in the caller's canonical order
     *        (this order is the deterministic tie-break)
     * @param slots the available slots, in the caller's canonical order
     * @param incumbents the previous epoch's element id → slot id map;
     *        entries pointing at slots or elements that no longer exist are
     *        ignored (a vanished slot leaves its element homeless but does
     *        not consume budget)
     *
     * @throws IllegalArgumentException if element or slot ids are duplicated
     */
    public Result assign(List<Element> elements, List<Slot> slots, Map<String, String> incumbents) {
        Map<String, Slot> slotById = indexSlots(slots);
        indexElements(elements);

        Map<String, Slot> assignment = new LinkedHashMap<>();
        int movesUsed = 0;

        for (Element element : elements) {
            String incumbentId = incumbents.get(element.id());
            Slot incumbent = incumbentId == null ? null : slotById.get(incumbentId);
            if (incumbent != null && !isTaken(assignment, incumbent.id())) {
                assignment.put(element.id(), incumbent);
            }
        }

        for (Element element : elements) {
            if (assignment.containsKey(element.id())) {
                continue;
            }
            Slot best = null;
            double bestCost = Double.POSITIVE_INFINITY;
            for (Slot slot : slots) {
                if (isTaken(assignment, slot.id())) {
                    continue;
                }
                double cost = anchorCost(element, slot);
                if (cost < bestCost) {
                    best = slot;
                    bestCost = cost;
                }
            }
            if (best != null) {
                assignment.put(element.id(), best);
            }
        }

        while (movesUsed < maxMovesPerEpoch) {
            Move best = bestImprovement(elements, slots, assignment, incumbents);
            if (best == null) {
                break;
            }
            assignment.put(best.elementId(), best.target);
            movesUsed++;
        }

        List<Assignment> assignments = new ArrayList<>(elements.size());
        for (Element element : elements) {
            Slot slot = assignment.get(element.id());
            double cost = slot == null ? Double.NaN : anchorCost(element, slot);
            assignments.add(new Assignment(element.id(), slot == null ? null : slot.id(), cost));
        }
        return new Result(assignments, movesUsed);
    }

    private record Move(String elementId, Slot target, double gain) {}

    private @Nullable Move bestImprovement(
        List<Element> elements,
        List<Slot> slots,
        Map<String, Slot> assignment,
        Map<String, String> incumbents
    ) {
        Move best = null;
        for (Element element : elements) {
            Slot current = assignment.get(element.id());
            if (current == null) {
                continue;
            }
            String incumbentId = incumbents.get(element.id());
            double currentCost = incumbentCost(element, current, incumbentId);
            for (Slot slot : slots) {
                if (slot.id().equals(current.id()) || isTaken(assignment, slot.id())) {
                    continue;
                }
                double targetCost = incumbentCost(element, slot, incumbentId);
                double gain = currentCost - targetCost - costs.switchPenalty();
                if (gain > 0 && (best == null || gain > best.gain())) {
                    best = new Move(element.id(), slot, gain);
                }
            }
        }
        return best;
    }

    private double anchorCost(Element element, Slot slot) {
        double dx = element.anchorX() - slot.x();
        double dy = element.anchorY() - slot.y();
        return costs.anchorWeight() * Math.sqrt(dx * dx + dy * dy);
    }

    private double incumbentCost(Element element, Slot slot, @Nullable String incumbentId) {
        double cost = anchorCost(element, slot);
        return slot.id().equals(incumbentId) ? cost * (1.0 - costs.incumbentDiscount()) : cost;
    }

    private static boolean isTaken(Map<String, Slot> assignment, String slotId) {
        for (Slot taken : assignment.values()) {
            if (taken.id().equals(slotId)) {
                return true;
            }
        }
        return false;
    }

    private static Map<String, Slot> indexSlots(List<Slot> slots) {
        Map<String, Slot> byId = new HashMap<>();
        for (Slot slot : slots) {
            if (byId.put(slot.id(), slot) != null) {
                throw new IllegalArgumentException("duplicate slot id: " + slot.id());
            }
        }
        return byId;
    }

    private static Map<String, Element> indexElements(List<Element> elements) {
        Map<String, Element> byId = new HashMap<>();
        for (Element element : elements) {
            if (byId.put(element.id(), element) != null) {
                throw new IllegalArgumentException("duplicate element id: " + element.id());
            }
        }
        return byId;
    }
}
