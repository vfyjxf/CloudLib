package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.ui.inworld.OffscreenProjector;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.RayFan;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashMap;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * Composite scenarios through the closed profiles: the algorithms a profile
 * binds are driven together, epoch by epoch, and the full pipeline's output
 * is snapshotted as text. Determinism is the property under test — two fresh
 * runs of the same scenario must produce the identical snapshot sequence —
 * alongside the cross-algorithm acceptance rules (recourse budget never
 * exceeded, cluster structure stable under jitter, indicators continuous
 * around the perimeter).
 */
class AlgorithmComboTest {

    private static final double anchorX = 960;
    private static final double anchorY = 540;

    /** The nameplate crowd: four pairs of nameplates, each pair tight, pairs far apart. */
    private static List<Clusterer.Member> crowd(double jitterSeed) {
        double[][] groups = {
            {700, 380}, {1150, 350}, {620, 700}, {1300, 720},
        };
        List<Clusterer.Member> members = new ArrayList<>();
        int id = 0;
        for (double[] group : groups) {
            double jitter = (id % 2 == 0 ? 1 : -1) * jitterSeed;
            members.add(new Clusterer.Member("e" + id++, group[0] + jitter, group[1]));
            members.add(new Clusterer.Member("e" + id++, group[0], group[1] + 30));
        }
        return members;
    }

    private static List<Rect> occluders() {
        return List.of(new Rect(1000, 520, 20, 40), new Rect(860, 560, 20, 40));
    }

    /**
     * Runs the nameplate pipeline (orbit ring candidates → sticky assignment
     * with recourse → clustering → leader routing) over two epochs and
     * returns one text snapshot per epoch.
     */
    private static List<String> nameplateScenario() {
        AlgorithmProfile profile = AlgorithmProfile.nameplate;
        OrbitRing ring = new OrbitRing(60, 40, 30, 4);
        SlotAssigner assigner = new SlotAssigner(
                profile.params().recourseBudget(), profile.params().slotAssignerCosts());
        Clusterer clusterer = new Clusterer(profile.params().clustererConfig());
        LeaderRouter router = new LeaderRouter(profile.params().leaderRouterConfig());

        List<Clusterer.Member> epochOne = crowd(0);
        List<Clusterer.Member> epochTwo = crowd(7);
        Map<String, String> incumbents = new HashMap<>();
        List<String> snapshots = new ArrayList<>();

        for (List<Clusterer.Member> epoch : List.of(epochOne, epochTwo)) {
            RayFan fan = new RayFan(anchorX, anchorY, 200);
            fan.blockAll(occluders());

            List<OrbitRing.Slot> candidates = ring.freeSlots(fan, epoch.size());
            List<SlotAssigner.Slot> slots = new ArrayList<>(candidates.size());
            for (OrbitRing.Slot slot : candidates) {
                slots.add(new SlotAssigner.Slot(
                        "r" + slot.ring() + "i" + slot.index(), anchorX + slot.offsetX(), anchorY + slot.offsetY()));
            }

            List<SlotAssigner.Element> elements = new ArrayList<>(epoch.size());
            for (Clusterer.Member member : epoch) {
                elements.add(new SlotAssigner.Element(member.id(), member.x(), member.y()));
            }

            SlotAssigner.Result assignment = assigner.assign(elements, slots, incumbents);
            assertTrue(assignment.movesUsed() <= profile.params().recourseBudget());

            for (SlotAssigner.Assignment single : assignment.assignments()) {
                assertTrue(single.assigned(), "unassigned element in a roomy scene: " + single.elementId());
                incumbents.put(single.elementId(), single.slotId());
            }

            List<Clusterer.Cluster> clusters = clusterer.cluster(epoch);
            Map<String, String> clusterMap = new HashMap<>();
            for (int c = 0; c < clusters.size(); c++) {
                for (String memberId : clusters.get(c).memberIds()) {
                    clusterMap.put(memberId, "c" + c);
                }
            }

            Map<String, SlotAssigner.Slot> slotById = new LinkedHashMap<>();
            for (SlotAssigner.Slot slot : slots) {
                slotById.put(slot.id(), slot);
            }
            List<LeaderRouter.Leader> leaders = new ArrayList<>(epoch.size());
            for (Clusterer.Member member : epoch) {
                SlotAssigner.Slot slot = slotById.get(assignment.of(member.id()).slotId());
                leaders.add(new LeaderRouter.Leader(member.id(), member.x(), member.y(), slot.x(), slot.y() - 24));
            }
            List<LeaderRouter.Route> routes = router.route(leaders, clusterMap);

            snapshots.add(snapshot(assignment, clusters, routes));
        }
        return snapshots;
    }

    private static String snapshot(
            SlotAssigner.Result assignment, List<Clusterer.Cluster> clusters, List<LeaderRouter.Route> routes) {
        StringBuilder text = new StringBuilder();
        for (SlotAssigner.Assignment single : assignment.assignments()) {
            text.append(single.elementId())
                    .append("->")
                    .append(single.slotId())
                    .append("(")
                    .append(String.format("%.3f", single.cost()))
                    .append(") ");
        }
        text.append("| ");
        for (Clusterer.Cluster cluster : clusters) {
            text.append(cluster.memberIds())
                    .append("*")
                    .append(cluster.representative())
                    .append(" ");
        }
        text.append("| ");
        for (LeaderRouter.Route route : routes) {
            text.append(route.id()).append("@").append(route.style());
            for (FloatPos point : route.points()) {
                text.append(String.format(" (%.2f,%.2f)", point.x(), point.y()));
            }
            text.append(" ");
        }
        return text.toString();
    }

    @Test
    void nameplatePipelineIsDeterministicAcrossRuns() {
        assertEquals(nameplateScenario(), nameplateScenario());
    }

    @Test
    void nameplateClusterStructureSurvivesAnchorJitter() {
        // epoch one and epoch two differ by a 7 px jitter on half the members;
        // the snapshot's cluster section must show the same four pairs both epochs
        List<String> snapshots = nameplateScenario();

        String clustersOne = clustersSection(snapshots.get(0));
        String clustersTwo = clustersSection(snapshots.get(1));
        assertEquals(clustersOne, clustersTwo);
        assertTrue(clustersOne.contains("[e0, e1]"), "expected pair clustering, got: " + clustersOne);
    }

    private static String clustersSection(String snapshot) {
        return snapshot.substring(snapshot.indexOf('|') + 1, snapshot.lastIndexOf('|'));
    }

    @Test
    void dockPipelineCompactsStably() {
        AlgorithmProfile profile = AlgorithmProfile.dock;
        List<String> runs = new ArrayList<>();
        for (int run = 0; run < 2; run++) {
            DockCursor cursor = new DockCursor(
                    DockCursor.Edge.bottom,
                    1920,
                    profile.params().dockMargin(),
                    profile.params().dockSpacing());
            StringBuilder text = new StringBuilder();
            for (int i = 0; i < 6; i++) {
                DockCursor.Slot slot = cursor.allocate("w" + i, 120 + 10 * i);
                assertTrue(slot != null);
            }
            cursor.release("w1");
            cursor.release("w3");
            cursor.compact();
            for (DockCursor.Slot slot : cursor.slots()) {
                text.append(slot.id())
                        .append("@")
                        .append(String.format("%.1f", slot.start()))
                        .append(" ");
            }
            runs.add(text.toString());
        }
        assertEquals(runs.get(0), runs.get(1));
        // order preserved: the survivors keep their relative order after compaction
        assertTrue(runs.get(0).startsWith("w0@")
                && runs.get(0).contains("w2@")
                && runs.get(0).contains("w4@")
                && runs.get(0).contains("w5@")
                && !runs.get(0).contains("w1@"));
        int w0 = runs.get(0).indexOf("w0@");
        int w2 = runs.get(0).indexOf("w2@");
        int w4 = runs.get(0).indexOf("w4@");
        int w5 = runs.get(0).indexOf("w5@");
        assertTrue(w0 < w2 && w2 < w4 && w4 < w5);
    }

    @Test
    void waypointEncoderSweepsThePerimeterContinuously() {
        AlgorithmProfile profile = AlgorithmProfile.waypoint;
        AngleEncoder encoder = new AngleEncoder(1920, 1080, 24, profile.params().angleEncoderConfig());
        encoder.snap(0);

        List<OffscreenProjector.Edge> sequence = new ArrayList<>();
        sequence.add(OffscreenProjector.Edge.right);
        FloatPos previous = null;
        for (int k = 1; k <= 720; k++) {
            double angle = 2 * Math.PI * k / 720;
            double dx = Math.cos(angle);
            double dy = Math.sin(angle);
            OffscreenProjector.Result result =
                    new OffscreenProjector.Result(false, false, dx, dy, angle, null, null, null);
            AngleEncoder.Output output = encoder.update(result, 1.0 / 60.0);

            if (previous != null) {
                double stepX = output.position().x() - previous.x();
                double stepY = output.position().y() - previous.y();
                assertTrue(Math.sqrt(stepX * stepX + stepY * stepY) < 30.0, "teleport in the waypoint sweep");
            }
            previous = output.position();
            if (sequence.get(sequence.size() - 1) != output.edge()) {
                sequence.add(output.edge());
            }
        }
        assertEquals(
                List.of(
                        OffscreenProjector.Edge.right,
                        OffscreenProjector.Edge.bottom,
                        OffscreenProjector.Edge.left,
                        OffscreenProjector.Edge.top,
                        OffscreenProjector.Edge.right),
                sequence);
    }
}
