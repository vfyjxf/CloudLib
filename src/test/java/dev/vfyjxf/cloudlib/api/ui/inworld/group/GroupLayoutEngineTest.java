package dev.vfyjxf.cloudlib.api.ui.inworld.group;

import dev.vfyjxf.cloudlib.api.math.Rect;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The grouping engine's acceptance cases: cluster split/merge hysteresis,
 * orbit assignment churn, and the capacity degradation ladder
 * (ring expansion → representative aggregation → hide+linger).
 */
class GroupLayoutEngineTest {

    private static final InworldGroup group = new InworldGroup("nimbus", "test", "g");

    private static GroupLayoutEngine.GroupMember member(String id, double x, double y) {
        return new GroupLayoutEngine.GroupMember(id, x, y, 20, 20);
    }

    private static GroupLayoutEngine.GroupFrame frame(
        double ax,
        double ay,
        List<GroupLayoutEngine.GroupMember> members,
        Rect... occluders
    ) {
        return new GroupLayoutEngine.GroupFrame(ax, ay, members, List.of(occluders));
    }

    // region cluster hysteresis

    @Test
    void clustersSplitAndMergeWithHysteresis() {
        double radius = 120.0;
        GroupLayoutEngine engine = GroupLayoutEngine.of(group, new ClusterToRepresentative(0.6, radius, 8, 16, true));
        List<GroupLayoutEngine.GroupMember> members = new ArrayList<>();
        members.add(member("m0", 200, 150));
        members.add(member("m1", 200, 150));

        // d = 0.2·R: fresh pair merges.
        members.set(1, member("m1", 200 + 0.2 * radius, 150));
        GroupLayoutEngine.GroupResult close = engine.arrange(frame(200, 150, members));
        assertEquals(1, close.clusters().size(), "a fresh close pair merges");
        assertEquals(GroupLayoutEngine.Outcome.placed, close.of("m0").outcome());
        assertEquals(GroupLayoutEngine.Outcome.aggregated, close.of("m1").outcome());
        assertEquals("m0", close.of("m1").aggregatedInto());

        // d = 1.2·R: history keeps the cluster together (the jitter band).
        members.set(1, member("m1", 200 + 1.2 * radius, 150));
        GroupLayoutEngine.GroupResult drift = engine.arrange(frame(200, 150, members));
        assertEquals(1, drift.clusters().size(), "a merged pair survives the jitter band");

        // d = 3·R: the snapshot debt exceeds the history dividend — split.
        members.set(1, member("m1", 200 + 3 * radius, 150));
        GroupLayoutEngine.GroupResult far = engine.arrange(frame(200, 150, members));
        assertEquals(2, far.clusters().size(), "a distant pair splits");
        assertEquals(GroupLayoutEngine.Outcome.placed, far.of("m0").outcome());
        assertEquals(GroupLayoutEngine.Outcome.placed, far.of("m1").outcome());

        // back to d = 1.2·R: the split history keeps them apart.
        members.set(1, member("m1", 200 + 1.2 * radius, 150));
        GroupLayoutEngine.GroupResult returnDrift = engine.arrange(frame(200, 150, members));
        assertEquals(2, returnDrift.clusters().size(), "a split pair does not re-merge in the jitter band");

        // back to d = 0.2·R: close enough to pay the consistency debt — merge.
        members.set(1, member("m1", 200 + 0.2 * radius, 150));
        GroupLayoutEngine.GroupResult remerge = engine.arrange(frame(200, 150, members));
        assertEquals(1, remerge.clusters().size(), "a truly close pair re-merges");
        assertEquals(2, remerge.clusters().getFirst().memberIds().size());
        assertEquals(1, remerge.clusters().getFirst().aggregatedCount());
    }

    @Test
    void clusterAggregationCarriesUnitSizeAndViews() {
        GroupLayoutEngine engine = GroupLayoutEngine.of(group, new ClusterToRepresentative(0.6, 120.0, 8, 16, true));
        List<GroupLayoutEngine.GroupMember> members = new ArrayList<>();
        members.add(member("rep", 200, 150));
        members.add(member("a", 210, 150));
        members.add(member("b", 190, 155));

        GroupLayoutEngine.GroupResult result = engine.arrange(frame(200, 150, members));
        assertEquals(1, result.clusters().size());
        assertEquals("rep", result.clusters().getFirst().representativeId());
        assertEquals(3, result.of("rep").clusterSize());
        assertEquals(3, result.of("a").clusterSize());
        assertEquals(2, result.aggregatedCount());
        assertEquals(0, result.hiddenCount());
    }

    // endregion

    // region orbit churn

    @Test
    void orbitAssignmentKeepsIncumbentSlotsWhenAMemberJoins() {
        GroupLayoutEngine engine = GroupLayoutEngine
                .of(group, new OrbitAroundAnchor(28.0, 14.0, 44.0, 4, 2, 20.0, 0.15, 8));
        List<GroupLayoutEngine.GroupMember> three = new ArrayList<>();
        three.add(member("a", 200, 150));
        three.add(member("b", 200, 150));
        three.add(member("c", 200, 150));

        GroupLayoutEngine.GroupResult before = engine.arrange(frame(200, 150, three));
        assertEquals(3, before.outcomes().size());
        assertEquals(0, before.aggregatedCount());
        assertEquals(1, before.ringsUsed(), "three members fit the inner ring");

        List<GroupLayoutEngine.GroupMember> four = new ArrayList<>(three);
        four.add(member("d", 200, 150));
        GroupLayoutEngine.GroupResult after = engine.arrange(frame(200, 150, four));
        assertEquals(0, after.aggregatedCount());
        for (String incumbent : List.of("a", "b", "c")) {
            assertEquals(
                before.of(incumbent).offsetX(),
                after.of(incumbent).offsetX(),
                1.0e-9,
                incumbent + " keeps its slot x"
            );
            assertEquals(
                before.of(incumbent).offsetY(),
                after.of(incumbent).offsetY(),
                1.0e-9,
                incumbent + " keeps its slot y"
            );
        }
        assertEquals(GroupLayoutEngine.Outcome.placed, after.of("d").outcome());
    }

    @Test
    void orbitSpillsToTheNextRingWhenTheInnerRingFills() {
        GroupLayoutEngine engine = GroupLayoutEngine
                .of(group, new OrbitAroundAnchor(28.0, 14.0, 44.0, 4, 2, 20.0, 0.15, 8));
        List<GroupLayoutEngine.GroupMember> members = new ArrayList<>();
        for (int i = 0; i < 12; i++) {
            members.add(member("m" + i, 200, 150));
        }
        GroupLayoutEngine.GroupResult result = engine.arrange(frame(200, 150, members));
        int inner = engineInnerCapacity(28.0, 44.0);
        assertTrue(
            result.ringsUsed() >= 2,
            "twelve members expand past the inner ring (rings=" + result.ringsUsed() + ")"
        );
        assertTrue(result.ringsUsed() <= 4, "the expansion respects maxRings");
        long placed = result.outcomes().stream()
                .filter(outcome -> outcome.outcome() == GroupLayoutEngine.Outcome.placed).count();
        assertTrue(placed >= inner, "at least the inner ring's members are placed");
    }

    // endregion

    // region capacity ladder

    @Test
    void capacityExhaustionAggregatesThenHides() {
        // One ring of 3 slots, at most 2 aggregated into the representative.
        GroupLayoutEngine engine = GroupLayoutEngine
                .of(group, new OrbitAroundAnchor(28.0, 14.0, 44.0, 1, 0, 20.0, 0.15, 2));

        List<GroupLayoutEngine.GroupMember> four = new ArrayList<>();
        for (int i = 0; i < 4; i++) {
            four.add(member("m" + i, 200, 150));
        }
        GroupLayoutEngine.GroupResult aggregated = engine.arrange(frame(200, 150, four));
        assertEquals(1, aggregated.aggregatedCount(), "the 4th member folds into the representative");
        assertEquals(0, aggregated.hiddenCount());
        assertEquals(2, aggregated.of("m0").clusterSize(), "the representative's unit counts its aggregatees");

        List<GroupLayoutEngine.GroupMember> six = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            six.add(member("m" + i, 200, 150));
        }
        GroupLayoutEngine.GroupResult hidden = engine.arrange(frame(200, 150, six));
        assertEquals(2, hidden.aggregatedCount());
        assertEquals(1, hidden.hiddenCount(), "beyond the aggregation cap members hide");
        assertEquals(3, hidden.of("m0").clusterSize(), "both aggregatees hang on the representative's unit");
        assertTrue(
            hidden.outcomes().stream().anyMatch(GroupLayoutEngine.MemberOutcome::linger),
            "hidden members linger"
        );
    }

    @Test
    void fullyBlockedAnchorStillShowsOneRepresentative() {
        GroupLayoutEngine engine = GroupLayoutEngine
                .of(group, new OrbitAroundAnchor(28.0, 14.0, 44.0, 4, 2, 20.0, 0.15, 8));
        List<GroupLayoutEngine.GroupMember> members = new ArrayList<>();
        for (int i = 0; i < 3; i++) {
            members.add(member("m" + i, 200, 150));
        }
        Rect blocking = new Rect(150, 100, 100, 100);
        GroupLayoutEngine.GroupResult result = engine.arrange(frame(200, 150, members, blocking));
        long placed = result.outcomes().stream()
                .filter(outcome -> outcome.outcome() == GroupLayoutEngine.Outcome.placed).count();
        assertEquals(1, placed, "the representative holds the anchor itself");
        assertEquals(2, result.aggregatedCount());
        assertEquals(0, result.hiddenCount());
        assertEquals(0.0, result.outcomes().getFirst().offsetX(), 1.0e-9);
        assertEquals(0.0, result.outcomes().getFirst().offsetY(), 1.0e-9);
    }

    // endregion

    // region stack and no-grouping

    @Test
    void columnStacksInOrderWithOverflowAggregation() {
        GroupLayoutEngine engine = GroupLayoutEngine
                .of(group, new StackInColumn(StackInColumn.Direction.up, 4.0, 4, 1));
        List<GroupLayoutEngine.GroupMember> members = new ArrayList<>();
        for (int i = 0; i < 6; i++) {
            members.add(member("m" + i, 200, 150));
        }
        GroupLayoutEngine.GroupResult result = engine.arrange(frame(200, 150, members));
        assertEquals(0.0, result.of("m0").offsetY(), 1.0e-9);
        assertEquals(-24.0, result.of("m1").offsetY(), 1.0e-9);
        assertEquals(-48.0, result.of("m2").offsetY(), 1.0e-9);
        assertEquals(-72.0, result.of("m3").offsetY(), 1.0e-9);
        assertEquals(GroupLayoutEngine.Outcome.aggregated, result.of("m4").outcome());
        assertEquals("m3", result.of("m4").aggregatedInto(), "the last visible member absorbs the overflow");
        assertEquals(GroupLayoutEngine.Outcome.hidden, result.of("m5").outcome());
        assertTrue(result.of("m5").linger());
    }

    @Test
    void noGroupingPlacesEveryoneIndependently() {
        GroupLayoutEngine engine = GroupLayoutEngine.of(group, NoGrouping.instance);
        List<GroupLayoutEngine.GroupMember> members = new ArrayList<>();
        members.add(member("a", 10, 10));
        members.add(member("b", 400, 300));
        members.add(member("c", 250, 30));

        GroupLayoutEngine.GroupResult result = engine.arrange(frame(0, 0, members));
        assertEquals(3, result.clusters().size());
        assertEquals(0, result.aggregatedCount());
        assertEquals(0, result.hiddenCount());
        for (GroupLayoutEngine.GroupMember member : members) {
            GroupLayoutEngine.MemberOutcome outcome = result.of(member.id());
            assertNotNull(outcome);
            assertEquals(GroupLayoutEngine.Outcome.placed, outcome.outcome());
            assertEquals(0.0, outcome.offsetX(), 1.0e-9);
            assertEquals(0.0, outcome.offsetY(), 1.0e-9);
            assertNull(outcome.aggregatedInto());
        }
    }

    // endregion

    /** The inner ring's slot count for the given geometry, mirroring OrbitRing. */
    private static int engineInnerCapacity(double baseRadius, double slotArcLength) {
        int slots = (int) Math.floor(2 * Math.PI * baseRadius / slotArcLength);
        return Math.max(2, slots);
    }
}
