package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpacePolicy;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ordered release of coinciding targets: two fixed (non-pushable)
 * elements whose preference orders agree — co-anchored panels proposing the
 * same candidate lattice — used to be granted the same rect (fixed is exempt
 * from the bitmap veto) and then pushed apart in lockstep by their own
 * overlap scoring, alternating between the same two slots on every resolve:
 * the targets coincided forever, the pair flickered and never separated. The
 * arbitration order now breaks that tie the way it breaks every other: the
 * element committed earlier in the round keeps the contested slot, the later
 * one walks its own preference list to its first non-coinciding candidate.
 */
class CoincidenceReleaseTest {

    private static final double dt = 0.05;

    /**
     * A sticky self-managed element with a fixed single-rung ladder proposing
     * an anchor-centered candidate spread — the Nimbus zone-panel shape.
     */
    private static final class LatticeElement implements InworldElement {
        private final String id;
        private final int priority;
        private FloatPos anchor;
        private List<FloatRect> preference;

        LatticeElement(String id, int priority, FloatPos anchor, List<FloatRect> preference) {
            this.id = id;
            this.priority = priority;
            this.anchor = anchor;
            this.preference = List.copyOf(preference);
        }

        void moveTo(FloatPos position, List<FloatRect> newPreference) {
            anchor = position;
            preference = List.copyOf(newPreference);
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public SpaceKind spaceKind() {
            return SpaceKind.panel;
        }

        @Override
        public VariantLadder ladder() {
            return TestElement.ladder(SpacePolicy.fixed, false, true, new Size(80, 30));
        }

        @Override
        public int priority() {
            return priority;
        }

        @Override
        public boolean sticky() {
            return true;
        }

        @Override
        public ElementMode mode() {
            return ElementMode.selfManaged;
        }

        @Override
        public ElementProposal propose(ProposeContext context) {
            List<PlacementCandidate> candidates = new ArrayList<>(preference.size());
            for (FloatRect rect : preference) {
                candidates.add(PlacementCandidate.screen(rect));
            }
            return ElementProposal.of(context.variant(), anchor, candidates);
        }
    }

    /** The lattice of one anchor: in-place plus the four near docks. */
    private static List<FloatRect> lattice(double cx, double cy) {
        double w = 80;
        double h = 30;
        double gap = 8;
        return List.of(
            new FloatRect(cx - w / 2, cy - h / 2, w, h),
            new FloatRect(cx + gap, cy - h / 2, w, h),
            new FloatRect(cx - w - gap, cy - h / 2, w, h),
            new FloatRect(cx - w / 2, cy + gap, w, h),
            new FloatRect(cx - w / 2, cy - h - gap, w, h)
        );
    }

    private static double coincidence(InworldPlacement a, InworldPlacement b) {
        FloatRect x = a.screenRect();
        FloatRect y = b.screenRect();
        double ix = Math.min(x.right(), y.right()) - Math.max(x.x(), y.x());
        double iy = Math.min(x.bottom(), y.bottom()) - Math.max(x.y(), y.y());
        if (ix <= 0 || iy <= 0) {
            return 0;
        }
        return ix * iy / Math.min(x.width() * x.height(), y.width() * y.height());
    }

    private static CoordinationResult frame(InworldCoordinator coordinator, double now) {
        return coordinator.frame(InworldCoordinator.FrameInput.of(400, 300, now, dt));
    }

    @Test
    void coAnchoredFixedPairSeparatesInsteadOfStacking() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        List<FloatRect> preference = lattice(200, 150);
        coordinator.register(new LatticeElement("a", 10, new FloatPos(200, 150), preference));
        coordinator.register(new LatticeElement("b", 10, new FloatPos(200, 150), preference));

        CoordinationResult result = frame(coordinator, dt);

        InworldPlacement a = result.placementOf("a");
        InworldPlacement b = result.placementOf("b");
        assertNotNull(a);
        assertNotNull(b);
        assertTrue(coincidence(a, b) < 0.5, "the pair separated, intersection fraction=" + coincidence(a, b));
        assertEquals(preference.get(0), a.screenRect(), "the earlier element keeps the contested slot");
        assertTrue(
            preference.indexOf(b.screenRect()) > 0,
            "the later element holds one of its own non-coinciding candidates: " + b.screenRect()
        );
    }

    @Test
    void separatedAssignmentIsStableAcrossResolves() {
        // the anti-flicker property: with static inputs every resolve produces
        // the same grants — no alternation between two slots
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        List<FloatRect> preference = lattice(200, 150);
        coordinator.register(new LatticeElement("a", 10, new FloatPos(200, 150), preference));
        coordinator.register(new LatticeElement("b", 10, new FloatPos(200, 150), preference));

        FloatRect firstA = null;
        FloatRect firstB = null;
        for (int i = 0; i < 10; i++) {
            CoordinationResult result = frame(coordinator, dt * (i + 1));
            FloatRect a = result.placementOf("a").screenRect();
            FloatRect b = result.placementOf("b").screenRect();
            if (i == 0) {
                firstA = a;
                firstB = b;
            } else {
                assertEquals(firstA, a, "resolve " + i + ": the contested slot's holder never flips");
                assertEquals(firstB, b, "resolve " + i + ": the released element stays on its released slot");
            }
            assertTrue(coincidence(result.placementOf("a"), result.placementOf("b")) < 0.5);
        }
    }

    @Test
    void releasedElementArrivesOnceTheEnvironmentAllows() {
        // delayed arrival, not a freeze: when the anchors separate, the
        // released element's grant follows its own anchor (the carried slot
        // re-anchored, or a fresh slot of its own lattice) and clears the
        // winner — it is never pinned away from its anchor
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        List<FloatRect> preferenceA = lattice(200, 150);
        LatticeElement a = new LatticeElement("a", 10, new FloatPos(200, 150), preferenceA);
        LatticeElement b = new LatticeElement("b", 10, new FloatPos(200, 150), lattice(200, 150));
        coordinator.register(a);
        coordinator.register(b);
        frame(coordinator, dt);
        frame(coordinator, 2 * dt);

        b.moveTo(new FloatPos(320, 150), lattice(320, 150));
        CoordinationResult moved = frame(coordinator, 3 * dt);

        FloatRect grant = moved.placementOf("b").screenRect();
        assertEquals(0, coincidence(moved.placementOf("a"), moved.placementOf("b")), 1.0e-9);
        assertTrue(
            Math.hypot(grant.centerX() - 320, grant.centerY() - 150) <= 60 + 40,
            "the released element rides its own anchor: grant center " + grant.centerX() + "," + grant.centerY()
                    + " anchor 320,150"
        );
        assertEquals(preferenceA.get(0), moved.placementOf("a").screenRect());
    }

    @Test
    void threeElementsCyclingOnOneTargetAllSeparate() {
        // A→B→C→A around one shared preferred slot: the ordered release
        // resolves the cycle in one round — three slots, no pair stacked
        // (grazes below the coincidence fraction stay tolerated by design)
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        List<FloatRect> preference = lattice(200, 150);
        coordinator.register(new LatticeElement("a", 10, new FloatPos(200, 150), preference));
        coordinator.register(new LatticeElement("b", 10, new FloatPos(200, 150), preference));
        coordinator.register(new LatticeElement("c", 10, new FloatPos(200, 150), preference));

        CoordinationResult result = frame(coordinator, dt);

        List<FloatRect> granted = new ArrayList<>();
        for (String id : new String[]{"a", "b", "c"}) {
            assertNotNull(result.placementOf(id), id + " presented");
            granted.add(result.placementOf(id).screenRect());
        }
        for (int i = 0; i < granted.size(); i++) {
            for (int j = i + 1; j < granted.size(); j++) {
                FloatRect x = granted.get(i);
                FloatRect y = granted.get(j);
                double ix = Math.min(x.right(), y.right()) - Math.max(x.x(), y.x());
                double iy = Math.min(x.bottom(), y.bottom()) - Math.max(x.y(), y.y());
                double fraction = ix <= 0 || iy <= 0
                        ? 0
                        : ix * iy / Math.min(x.width() * x.height(), y.width() * y.height());
                assertTrue(
                    fraction < 0.5,
                    "elements " + i + " and " + j + " do not stack: " + x + " vs " + y + " fraction=" + fraction
                );
            }
        }
    }

    @Test
    void loneFixedElementKeepsItsPreferredCandidate() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        List<FloatRect> preference = lattice(200, 150);
        coordinator.register(new LatticeElement("solo", 10, new FloatPos(200, 150), preference));

        CoordinationResult result = frame(coordinator, dt);

        assertEquals(preference.get(0), result.placementOf("solo").screenRect());
    }
}
