package dev.vfyjxf.cloudlib.api.ui.inworld.group;

import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.inworld.algorithm.Clusterer;
import dev.vfyjxf.cloudlib.api.ui.inworld.algorithm.OrbitRing;
import dev.vfyjxf.cloudlib.api.ui.inworld.algorithm.SlotAssigner;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.RayFan;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The per-group arrangement engine (§3.7): one instance per
 * {@link InworldGroup}, carrying the state that makes arrangement stable
 * across epochs — the orbit incumbents (slot stickiness) and the cluster
 * history (evolutionary consistency). Each {@link #arrange} call is one
 * epoch: members in the caller's canonical order come in, per-member
 * outcomes go out.
 * <p>
 * Outcomes carry offsets <em>relative to the member's own anchor</em> (§3.0
 * iron rule 1), so an element can adopt its slot directly as the offset of
 * its coordinator placement. The capacity degradation ladder is uniform
 * across strategies: spill outward (orbit rings) → aggregate into the
 * representative's "+N" → hide, and hidden members are flagged
 * {@link MemberOutcome#linger} so the driver retracts them (the
 * coordinator's retract path fades them out instead of blinking).
 * <p>
 * Deterministic by construction: iteration is over the caller's member list
 * only, ties break by canonical index, and no randomness participates. Same
 * inputs plus the same engine history, same result.
 */
public final class GroupLayoutEngine {

    /** What happened to one member this epoch. */
    public enum Outcome {
        /** The member holds a slot/position this epoch. */
        placed,
        /** The member is folded into a representative's "+N" count. */
        aggregated,
        /** The member is not shown; retract it so it lingers out. */
        hidden
    }

    /** One member to arrange, at its own anchor's projected screen position. */
    public record GroupMember(String id, double anchorX, double anchorY, double width, double height) {

        public GroupMember {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("member id must not be empty");
            }
            if (!Double.isFinite(anchorX) || !Double.isFinite(anchorY)) {
                throw new IllegalArgumentException("member position must be finite: " + anchorX + "," + anchorY);
            }
            if (!Double.isFinite(width) || width <= 0 || !Double.isFinite(height) || height <= 0) {
                throw new IllegalArgumentException("member size must be finite and positive: " + width + "x" + height);
            }
        }
    }

    /**
     * One epoch's inputs.
     *
     * @param anchorX the group anchor's projected screen x
     * @param anchorY the group anchor's projected screen y
     * @param members the members in canonical order (the deterministic
     *        tie-break)
     * @param occluders screen rectangles that block ring slots near the
     *        anchor (exclusion areas, committed occupants)
     */
    public record GroupFrame(double anchorX, double anchorY, List<GroupMember> members, List<Rect> occluders) {

        public GroupFrame {
            members = List.copyOf(members);
            occluders = List.copyOf(occluders);
            if (!Double.isFinite(anchorX) || !Double.isFinite(anchorY)) {
                throw new IllegalArgumentException("anchor must be finite: " + anchorX + "," + anchorY);
            }
        }
    }

    /**
     * One member's outcome.
     *
     * @param id the member id
     * @param outcome what happened
     * @param offsetX the granted position's x, relative to the member's own
     *        anchor (0 for aggregated members)
     * @param offsetY the granted position's y, relative to the member's own
     *        anchor
     * @param aggregatedInto the representative this member folded into, when
     *        {@link Outcome#aggregated}
     * @param clusterSize the size of this member's visible unit (itself plus
     *        its aggregatees)
     * @param linger whether a hidden member should retract (fade out) rather
     *        than hard-hide
     */
    public record MemberOutcome(
            String id,
            Outcome outcome,
            double offsetX,
            double offsetY,
            @Nullable String aggregatedInto,
            int clusterSize,
            boolean linger) {

        public MemberOutcome {
            if (id == null || id.isEmpty()) {
                throw new IllegalArgumentException("member id must not be empty");
            }
            if (aggregatedInto == null && outcome == Outcome.aggregated) {
                throw new IllegalArgumentException("an aggregated member names its representative");
            }
            if (clusterSize < 1) {
                throw new IllegalArgumentException("clusterSize must be at least 1: " + clusterSize);
            }
        }
    }

    /**
     * One visible unit: a placed representative plus the members folded into
     * its "+N".
     *
     * @param representativeId the placed member others folded into
     * @param memberIds the representative first, then its aggregatees in
     *        canonical order
     * @param centerX the representative's position x
     * @param centerY the representative's position y
     */
    public record ClusterView(String representativeId, List<String> memberIds, double centerX, double centerY) {

        public ClusterView {
            memberIds = List.copyOf(memberIds);
        }

        /** How many members folded into the representative. */
        public int aggregatedCount() {
            return memberIds.size() - 1;
        }
    }

    /**
     * One epoch's arrangement.
     *
     * @param outcomes one per member, in the frame's canonical order
     * @param clusters the visible units, in representative canonical order
     * @param ringsUsed how many orbit rings were handed out (1 = inner ring
     *        only); the ring-expansion rung of the ladder
     * @param aggregatedCount how many members folded into representatives
     * @param hiddenCount how many members hid
     */
    public record GroupResult(
            List<MemberOutcome> outcomes,
            List<ClusterView> clusters,
            int ringsUsed,
            int aggregatedCount,
            int hiddenCount) {

        public GroupResult {
            outcomes = List.copyOf(outcomes);
            clusters = List.copyOf(clusters);
        }

        /** The outcome of {@code memberId}, or null when absent. */
        public @Nullable MemberOutcome of(String memberId) {
            for (MemberOutcome outcome : outcomes) {
                if (outcome.id().equals(memberId)) {
                    return outcome;
                }
            }
            return null;
        }
    }

    private final InworldGroup group;
    private final GroupStrategy strategy;
    private final @Nullable Clusterer clusterer;
    private final Map<String, String> slotIncumbents = new HashMap<>();

    private GroupLayoutEngine(InworldGroup group, GroupStrategy strategy, @Nullable Clusterer clusterer) {
        this.group = group;
        this.strategy = strategy;
        this.clusterer = clusterer;
    }

    /** An engine running {@code strategy} for {@code group}. */
    public static GroupLayoutEngine of(InworldGroup group, GroupStrategy strategy) {
        Objects.requireNonNull(group, "group");
        Objects.requireNonNull(strategy, "strategy");
        if (strategy instanceof ClusterToRepresentative cluster) {
            return new GroupLayoutEngine(
                    group, strategy, new Clusterer(new Clusterer.Config(cluster.alpha(), cluster.mergeRadius())));
        }
        return new GroupLayoutEngine(group, strategy, null);
    }

    /** The group this engine arranges. */
    public InworldGroup group() {
        return group;
    }

    /** The strategy this engine runs. */
    public GroupStrategy strategy() {
        return strategy;
    }

    /** Drops all cross-epoch state (a scene change, a teleport). */
    public void reset() {
        if (clusterer != null) {
            clusterer.reset();
        }
        slotIncumbents.clear();
    }

    /**
     * Arranges one epoch.
     *
     * @throws IllegalArgumentException if member ids are duplicated
     */
    public GroupResult arrange(GroupFrame frame) {
        Objects.requireNonNull(frame, "frame");
        requireUniqueIds(frame.members());
        if (strategy instanceof OrbitAroundAnchor orbit) {
            return arrangeOrbit(frame, orbit);
        }
        if (strategy instanceof ClusterToRepresentative cluster) {
            return arrangeCluster(frame, cluster);
        }
        if (strategy instanceof StackInColumn stack) {
            return arrangeStack(frame, stack);
        }
        return arrangeUngrouped(frame);
    }

    // region orbit

    private GroupResult arrangeOrbit(GroupFrame frame, OrbitAroundAnchor orbit) {
        double reach = orbit.baseRadius() + (orbit.maxRings() - 1) * orbit.radiusStep() + orbit.slotArcLength();
        RayFan fan = new RayFan(frame.anchorX(), frame.anchorY(), reach);
        fan.blockAll(frame.occluders());
        OrbitRing ring = new OrbitRing(orbit.baseRadius(), orbit.radiusStep(), orbit.slotArcLength(), orbit.maxRings());
        List<OrbitRing.Slot> free = ring.freeSlots(fan, frame.members().size());

        Map<String, String> assignment = new LinkedHashMap<>();
        int ringsUsed = 0;
        for (OrbitRing.Slot slot : free) {
            ringsUsed = Math.max(ringsUsed, slot.ring() + 1);
        }
        if (!free.isEmpty()) {
            List<SlotAssigner.Element> elements =
                    new ArrayList<>(frame.members().size());
            for (GroupMember member : frame.members()) {
                elements.add(new SlotAssigner.Element(member.id(), member.anchorX(), member.anchorY()));
            }
            List<SlotAssigner.Slot> slots = new ArrayList<>(free.size());
            for (OrbitRing.Slot slot : free) {
                slots.add(new SlotAssigner.Slot(
                        slotId(slot.ring(), slot.index()),
                        frame.anchorX() + slot.offsetX(),
                        frame.anchorY() + slot.offsetY()));
            }
            SlotAssigner assigner = new SlotAssigner(
                    orbit.recourseBudget(),
                    new SlotAssigner.Costs(1.0, orbit.switchPenalty(), orbit.incumbentDiscount()));
            SlotAssigner.Result assigned = assigner.assign(elements, slots, slotIncumbents);
            for (SlotAssigner.Assignment one : assigned.assignments()) {
                if (one.assigned()) {
                    assignment.put(one.elementId(), one.slotId());
                }
            }
        }
        slotIncumbents.keySet().retainAll(assignment.keySet());
        slotIncumbents.putAll(assignment);

        // The aggregation anchor: the placed member nearest the group
        // anchor; when every slot is blocked, that member holds the anchor
        // itself so the group keeps one visible representative.
        boolean forceRepresentative = free.isEmpty();
        String representative = nearestTo(
                frame,
                forceRepresentative
                        ? frame.members().stream().map(GroupMember::id).toList()
                        : assignedIds(frame, assignment),
                frame.anchorX(),
                frame.anchorY());

        Map<String, List<String>> aggregatees = new LinkedHashMap<>();
        List<MemberOutcome> outcomes = new ArrayList<>(frame.members().size());
        int aggregated = 0;
        int hidden = 0;
        int overflow = 0;
        for (GroupMember member : frame.members()) {
            String slotId = assignment.get(member.id());
            if (slotId != null) {
                OrbitRing.Slot slot = ring.slot(ringIndex(slotId), slotOrdinal(slotId));
                outcomes.add(new MemberOutcome(
                        member.id(),
                        Outcome.placed,
                        slot.offsetX() + frame.anchorX() - member.anchorX(),
                        slot.offsetY() + frame.anchorY() - member.anchorY(),
                        null,
                        1,
                        false));
                continue;
            }
            if (forceRepresentative && member.id().equals(representative)) {
                outcomes.add(new MemberOutcome(
                        member.id(),
                        Outcome.placed,
                        frame.anchorX() - member.anchorX(),
                        frame.anchorY() - member.anchorY(),
                        null,
                        1,
                        false));
                continue;
            }
            if (representative != null && !representative.equals(member.id()) && overflow < orbit.maxAggregated()) {
                overflow++;
                aggregated++;
                aggregatees
                        .computeIfAbsent(representative, key -> new ArrayList<>())
                        .add(member.id());
                outcomes.add(new MemberOutcome(member.id(), Outcome.aggregated, 0, 0, representative, 1, false));
                continue;
            }
            hidden++;
            outcomes.add(new MemberOutcome(member.id(), Outcome.hidden, 0, 0, null, 1, true));
        }
        List<MemberOutcome> sized = withUnitSizes(outcomes);
        return new GroupResult(sized, viewsFor(frame, sized, aggregatees), ringsUsed, aggregated, hidden);
    }

    private static List<String> assignedIds(GroupFrame frame, Map<String, String> assignment) {
        List<String> assigned = new ArrayList<>();
        for (GroupMember member : frame.members()) {
            if (assignment.containsKey(member.id())) {
                assigned.add(member.id());
            }
        }
        return assigned;
    }

    // endregion

    // region cluster

    private GroupResult arrangeCluster(GroupFrame frame, ClusterToRepresentative cluster) {
        List<Clusterer.Member> snapshot = new ArrayList<>(frame.members().size());
        for (GroupMember member : frame.members()) {
            snapshot.add(new Clusterer.Member(member.id(), member.anchorX(), member.anchorY()));
        }
        List<Clusterer.Cluster> clusters = new ArrayList<>(clusterer.cluster(snapshot));

        // The visible-cap rung: merge the closest clusters until the cap
        // holds — merging absorbs, it never hides.
        while (clusters.size() > cluster.maxVisibleClusters()) {
            int bestA = -1;
            int bestB = -1;
            double bestDistance = Double.POSITIVE_INFINITY;
            for (int a = 0; a < clusters.size(); a++) {
                for (int b = a + 1; b < clusters.size(); b++) {
                    double distance = Math.hypot(
                            clusters.get(a).centerX() - clusters.get(b).centerX(),
                            clusters.get(a).centerY() - clusters.get(b).centerY());
                    if (distance < bestDistance) {
                        bestDistance = distance;
                        bestA = a;
                        bestB = b;
                    }
                }
            }
            List<String> merged = new ArrayList<>(clusters.get(bestA).memberIds());
            merged.addAll(clusters.get(bestB).memberIds());
            clusters.set(bestA, mergedCluster(frame, merged));
            clusters.remove(bestB);
        }

        Map<String, List<String>> aggregatees = new LinkedHashMap<>();
        int aggregated = 0;
        int hidden = 0;
        Map<String, Integer> clusterSizeByMember = new HashMap<>();
        Map<String, String> foldTarget = new HashMap<>();
        Map<String, Boolean> hideMember = new HashMap<>();
        for (Clusterer.Cluster one : clusters) {
            String representative = one.representative();
            List<String> foldable = new ArrayList<>(one.memberIds());
            foldable.remove(representative);
            foldable.sort((x, y) -> {
                double dx = distance(frame, x, representative);
                double dy = distance(frame, y, representative);
                int byDistance = Double.compare(dx, dy);
                return byDistance != 0 ? byDistance : Integer.compare(indexOf(frame, x), indexOf(frame, y));
            });
            int folded = 0;
            for (String id : one.memberIds()) {
                if (id.equals(representative)) {
                    continue;
                }
                if (folded < cluster.maxAggregatedPerCluster()) {
                    folded++;
                    aggregated++;
                    foldTarget.put(id, representative);
                    aggregatees
                            .computeIfAbsent(representative, key -> new ArrayList<>())
                            .add(id);
                } else {
                    hidden++;
                    hideMember.put(id, Boolean.TRUE);
                }
                clusterSizeByMember.put(id, one.size());
            }
            clusterSizeByMember.put(representative, one.size());
        }

        List<MemberOutcome> outcomes = new ArrayList<>(frame.members().size());
        for (GroupMember member : frame.members()) {
            String foldedInto = foldTarget.get(member.id());
            if (foldedInto != null) {
                outcomes.add(new MemberOutcome(
                        member.id(),
                        Outcome.aggregated,
                        0,
                        0,
                        foldedInto,
                        clusterSizeByMember.getOrDefault(member.id(), 1),
                        false));
            } else if (hideMember.containsKey(member.id())) {
                outcomes.add(new MemberOutcome(
                        member.id(),
                        Outcome.hidden,
                        0,
                        0,
                        null,
                        clusterSizeByMember.getOrDefault(member.id(), 1),
                        true));
            } else {
                outcomes.add(new MemberOutcome(
                        member.id(),
                        Outcome.placed,
                        0,
                        0,
                        null,
                        clusterSizeByMember.getOrDefault(member.id(), 1),
                        false));
            }
        }
        return new GroupResult(outcomes, viewsFor(frame, outcomes, aggregatees), 0, aggregated, hidden);
    }

    // endregion

    // region stack

    private GroupResult arrangeStack(GroupFrame frame, StackInColumn stack) {
        int sign = stack.direction() == StackInColumn.Direction.up ? -1 : 1;
        Map<String, List<String>> aggregatees = new LinkedHashMap<>();
        List<MemberOutcome> outcomes = new ArrayList<>(frame.members().size());
        int aggregated = 0;
        int hidden = 0;
        String lastVisible = null;
        double cursor = 0;
        int placedCount = 0;
        for (GroupMember member : frame.members()) {
            if (placedCount < stack.maxVisible()) {
                double y = sign * cursor;
                cursor += member.height() + stack.spacing();
                placedCount++;
                lastVisible = member.id();
                outcomes.add(new MemberOutcome(member.id(), Outcome.placed, 0, y, null, 1, false));
                continue;
            }
            if (aggregated < stack.maxAggregated() && lastVisible != null) {
                aggregated++;
                aggregatees
                        .computeIfAbsent(lastVisible, key -> new ArrayList<>())
                        .add(member.id());
                outcomes.add(new MemberOutcome(member.id(), Outcome.aggregated, 0, 0, lastVisible, 1, false));
                continue;
            }
            hidden++;
            outcomes.add(new MemberOutcome(member.id(), Outcome.hidden, 0, 0, null, 1, true));
        }
        List<MemberOutcome> sized = withUnitSizes(outcomes);
        return new GroupResult(sized, viewsFor(frame, sized, aggregatees), 0, aggregated, hidden);
    }

    // endregion

    // region ungrouped

    private GroupResult arrangeUngrouped(GroupFrame frame) {
        List<MemberOutcome> outcomes = new ArrayList<>(frame.members().size());
        for (GroupMember member : frame.members()) {
            outcomes.add(new MemberOutcome(member.id(), Outcome.placed, 0, 0, null, 1, false));
        }
        return new GroupResult(outcomes, viewsFor(frame, outcomes, Map.of()), 0, 0, 0);
    }

    // endregion

    // region helpers

    private static String slotId(int ring, int index) {
        return "r" + ring + "i" + index;
    }

    private static int ringIndex(String slotId) {
        return Integer.parseInt(slotId.substring(1, slotId.indexOf('i')));
    }

    private static int slotOrdinal(String slotId) {
        return Integer.parseInt(slotId.substring(slotId.indexOf('i') + 1));
    }

    /** The id nearest ({@code x}, {@code y}); ties keep the earlier canonical index. */
    private static String nearestTo(GroupFrame frame, List<String> candidates, double x, double y) {
        String best = null;
        double bestDistance = Double.POSITIVE_INFINITY;
        int bestIndex = Integer.MAX_VALUE;
        for (String id : candidates) {
            GroupMember member = memberOf(frame, id);
            double distance = Math.hypot(member.anchorX() - x, member.anchorY() - y);
            int index = indexOf(frame, id);
            if (distance < bestDistance || (distance == bestDistance && index < bestIndex)) {
                best = id;
                bestDistance = distance;
                bestIndex = index;
            }
        }
        return best;
    }

    private static Clusterer.Cluster mergedCluster(GroupFrame frame, List<String> memberIds) {
        double x = 0;
        double y = 0;
        for (String id : memberIds) {
            GroupMember member = memberOf(frame, id);
            x += member.anchorX();
            y += member.anchorY();
        }
        x /= memberIds.size();
        y /= memberIds.size();
        return new Clusterer.Cluster(memberIds, x, y, nearestTo(frame, memberIds, x, y));
    }

    private static double distance(GroupFrame frame, String id, String other) {
        GroupMember a = memberOf(frame, id);
        GroupMember b = memberOf(frame, other);
        return Math.hypot(a.anchorX() - b.anchorX(), a.anchorY() - b.anchorY());
    }

    private static GroupMember memberOf(GroupFrame frame, String id) {
        for (GroupMember member : frame.members()) {
            if (member.id().equals(id)) {
                return member;
            }
        }
        throw new IllegalArgumentException("unknown member id: " + id);
    }

    private static int indexOf(GroupFrame frame, String id) {
        for (int i = 0; i < frame.members().size(); i++) {
            if (frame.members().get(i).id().equals(id)) {
                return i;
            }
        }
        throw new IllegalArgumentException("unknown member id: " + id);
    }

    /** Rebuilds the outcomes with each representative's unit size spread onto its unit. */
    private static List<MemberOutcome> withUnitSizes(List<MemberOutcome> outcomes) {
        Map<String, Integer> unitExtras = new LinkedHashMap<>();
        for (MemberOutcome outcome : outcomes) {
            if (outcome.outcome() == Outcome.aggregated && outcome.aggregatedInto() != null) {
                unitExtras.merge(outcome.aggregatedInto(), 1, Integer::sum);
            }
        }
        List<MemberOutcome> sized = new ArrayList<>(outcomes.size());
        for (MemberOutcome outcome : outcomes) {
            Integer extra = unitExtras.get(outcome.id());
            if (extra == null) {
                sized.add(outcome);
            } else {
                sized.add(new MemberOutcome(
                        outcome.id(),
                        outcome.outcome(),
                        outcome.offsetX(),
                        outcome.offsetY(),
                        outcome.aggregatedInto(),
                        1 + extra,
                        outcome.linger()));
            }
        }
        return sized;
    }

    private static List<ClusterView> viewsFor(
            GroupFrame frame, List<MemberOutcome> outcomes, Map<String, List<String>> aggregatees) {
        List<ClusterView> views = new ArrayList<>();
        for (MemberOutcome outcome : outcomes) {
            if (outcome.outcome() != Outcome.placed) {
                continue;
            }
            List<String> members = new ArrayList<>();
            members.add(outcome.id());
            List<String> folded = aggregatees.get(outcome.id());
            if (folded != null) {
                members.addAll(folded);
            }
            GroupMember representative = memberOf(frame, outcome.id());
            views.add(new ClusterView(outcome.id(), members, representative.anchorX(), representative.anchorY()));
        }
        return views;
    }

    private static void requireUniqueIds(List<GroupMember> members) {
        Set<String> seen = new HashSet<>();
        for (GroupMember member : members) {
            if (!seen.add(member.id())) {
                throw new IllegalArgumentException("duplicate member id: " + member.id());
            }
        }
    }

    // endregion
}
