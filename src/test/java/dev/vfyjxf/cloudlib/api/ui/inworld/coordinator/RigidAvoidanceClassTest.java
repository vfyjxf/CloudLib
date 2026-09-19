package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.AssembledElement;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.ElementSpec;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.InworldProfile;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutEnvironment;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PipelineAssembler;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpacePolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.PreviousFrameLayout;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The rigid yield class (see {@link AvoidanceClass}): an element that
 * declares rigid places itself — its first screen candidate is granted
 * directly every frame at {@code anchor + declared offset}, edge-clamped —
 * and its rect is shared state for nobody else to consume. These tests pin
 * the two halves of that contract: the element's own direct-placement
 * lifecycle (anchor follow, edge clamp without offset hysteresis, retraction
 * into linger) and the invisibility guarantee (no bitmap mark, no
 * coincidence-release walk, no separation push, no zone snapshot entry — the
 * standard elements' outcome is exactly what it would be without the rigid
 * element registered).
 */
class RigidAvoidanceClassTest {

    private static final int width = 400;
    private static final int height = 300;
    private static final double dt = 1.0 / 60.0;

    private static CoordinationResult frame(InworldCoordinator coordinator, double now) {
        return coordinator.frame(InworldCoordinator.FrameInput.of(width, height, now, dt));
    }

    // region direct placement

    @Test
    void rigidElementPlacesDirectlyAtAnchorPlusDeclaredOffsetAndFollowsTheAnchor() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        // the declared placement: the panel docks 70 px right, 30 px below
        // its anchor (the first candidate; the alternates exist to prove the
        // first one always wins, competition or not)
        DockedElement rigid = new DockedElement("rigid", 200, 150, 70, 30);
        coordinator.register(rigid);

        CoordinationResult first = frame(coordinator, dt);
        InworldPlacement grant = first.placementOf("rigid");
        assertNotNull(grant, "the rigid element presents directly");
        assertEquals(70 - 30, grant.offsetRect().x(), 1.0e-9, "the declared offset is the first candidate's dock");
        assertEquals(30 - 10, grant.offsetRect().y(), 1.0e-9);

        // the anchor moves: the rect follows it every frame (no epoch gating,
        // no spring, no displacement clamp) — resolve frames and idle frames
        // alike
        double now = dt;
        for (int i = 0; i < 40; i++) {
            rigid.moveTo(200 + i * 2.5, 150 + i * 1.25);
            now += dt;
            CoordinationResult result = frame(coordinator, now);
            InworldPlacement placement = result.placementOf("rigid");
            assertEquals(
                    rigid.anchor().x() + 40, placement.screenRect().x(), 1.0e-9, "frame " + i + ": rides the anchor");
            assertEquals(
                    rigid.anchor().y() + 20, placement.screenRect().y(), 1.0e-9, "frame " + i + ": rides the anchor");
            assertEquals(
                    placement.screenRect(),
                    result.elementState("rigid").visualRect(),
                    "frame " + i + ": the rigid grant is its own visual — nothing relaxes it");
        }
    }

    @Test
    void edgeClampActsOnTheFinalRectOnlyAndNeverRewritesTheDeclaredOffset() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        // a 120x20 panel declared centered on its anchor: walking the anchor
        // toward the left edge pushes the declared rect off-screen
        DockedElement rigid = new DockedElement("rigid", 200, 150, new Size(120, 20), 0, 0);
        coordinator.register(rigid);

        double now = dt;
        frame(coordinator, now);

        // off-screen: the rect clamps to the work-area edge, every frame,
        // without an epoch in sight and without a rejection
        rigid.moveTo(20, 150);
        for (int i = 0; i < 20; i++) {
            now += dt;
            CoordinationResult clamped = frame(coordinator, now);
            assertNotNull(clamped.placementOf("rigid"), "frame " + i + ": the clamp never rejects");
            assertEquals(
                    0, clamped.placementOf("rigid").screenRect().x(), 1.0e-9, "frame " + i + ": pinned at the edge");
        }

        // back from the edge: the rect returns to exactly anchor + declared
        // offset — the clamp never fed a rewritten offset back into the
        // placement (no hysteresis, no re-pin)
        rigid.moveTo(200, 150);
        now += dt;
        CoordinationResult back = frame(coordinator, now);
        assertEquals(
                200 - 60, back.placementOf("rigid").screenRect().x(), 1.0e-9, "the declared offset returns untouched");
    }

    @Test
    void retractedRigidElementLingersInsteadOfRejecting() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        DockedElement rigid = new DockedElement("rigid", 200, 150, 0, 0);
        coordinator.register(rigid);
        double now = dt;
        frame(coordinator, now);
        for (int i = 0; i < 12; i++) {
            now += dt;
            frame(coordinator, now); // let the fade-in finish
        }
        assertNotNull(coordinator.lastResult().orElseThrow().placementOf("rigid"));

        rigid.anchorValid = false;
        now += dt;
        CoordinationResult retracted = frame(coordinator, now);
        assertNull(retracted.placementOf("rigid"), "a retracted rigid element leaves the presented set");
        assertNotNull(retracted.elementState("rigid").placement(), "the last grant is retained as linger memory");
        assertNull(retracted.elementState("rigid").rejection(), "retraction is never a rejection");

        rigid.anchorValid = true;
        now += dt;
        assertNotNull(frame(coordinator, now).placementOf("rigid"), "the anchor recovers into a direct grant again");
    }

    @Test
    void specDrivenElementForwardsTheRigidDeclarationAndPlacesDirectly() {
        ElementSpec spec = ElementSpec.from(InworldProfile.dock, "hud").withAvoidanceClass(AvoidanceClass.rigid);
        AssembledElement element = PipelineAssembler.create().assemble(spec);
        assertEquals(AvoidanceClass.rigid, element.avoidanceClass(), "the assembled element forwards the declaration");

        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        coordinator.register(element);
        double now = 0;
        for (int i = 0; i < 12; i++) {
            now += dt;
            element.beginFrame(LayoutEnvironment.of(width, height));
            coordinator.frame(InworldCoordinator.FrameInput.of(width, height, now, dt));
            element.observe(coordinator.lastResult().orElseThrow());
        }
        InworldPlacement grant = coordinator.lastResult().orElseThrow().placementOf("hud");
        assertNotNull(grant, "the spec-driven rigid element presents");
        assertTrue(
                grant.screenRect().x() >= 0 && grant.screenRect().right() <= width,
                "the grant is edge-clamped into the screen: " + grant.screenRect());
    }

    // endregion

    // region invisibility to others

    @Test
    void rigidRectNeverMarksTheOccupancyBitmap() {
        // a standard active element proposing the exact rect a rigid element
        // already holds must still be granted — the rigid rect marked no
        // cell of the bitmap
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement rigid = TestElement.arbitrated("rigid", 200, 150, new Size(60, 20))
                .withRigid()
                .withSingleCandidate()
                .withPriority(100);
        TestElement standard = TestElement.arbitrated("standard", 200, 150, new Size(60, 20))
                .withSingleCandidate()
                .withPriority(0);
        coordinator.register(rigid);
        coordinator.register(standard);

        CoordinationResult result = frame(coordinator, dt);
        assertNotNull(result.placementOf("rigid"));
        assertNotNull(result.placementOf("standard"), "the standard element overlaps the rigid rect freely");
        assertTrue(
                result.placementOf("rigid")
                        .screenRect()
                        .intersects(result.placementOf("standard").screenRect()),
                "both grants hold the same rect");
    }

    @Test
    void coincidenceReleaseDoesNotTreatARigidRectAsABlocker() {
        // a fixed standard element whose preferred candidate exactly stacks
        // on a rigid element's rect keeps the slot — a rigid rect is not a
        // non-pushable blocker, so the ordered release never walks. The same
        // stack against a fixed standard peer would force the walk.
        List<FloatRect> lattice = List.of(
                new FloatRect(160, 135, 80, 30), new FloatRect(248, 135, 80, 30), new FloatRect(72, 135, 80, 30));
        InworldCoordinator rigidScene = InworldCoordinator.withDefaults();
        rigidScene.register(new FixedLatticeElement("holder", 10, lattice, true));
        rigidScene.register(new FixedLatticeElement("fixed", 5, lattice, false));
        CoordinationResult againstRigid = frame(rigidScene, dt);
        assertEquals(
                lattice.get(0),
                againstRigid.placementOf("fixed").screenRect(),
                "a rigid rect does not release the coincidence walk");

        InworldCoordinator standardScene = InworldCoordinator.withDefaults();
        standardScene.register(new FixedLatticeElement("holder", 10, lattice, false));
        standardScene.register(new FixedLatticeElement("fixed", 5, lattice, false));
        CoordinationResult againstFixed = frame(standardScene, dt);
        assertTrue(
                lattice.indexOf(againstFixed.placementOf("fixed").screenRect()) > 0,
                "the standard fixed peer does release the walk: "
                        + againstFixed.placementOf("fixed").screenRect());
    }

    @Test
    void separationNeverMovesARigidElementAndARigidRectPushesNobody() {
        // a standard pushable element overlapping a rigid one: neither side
        // moves — the rigid element is not in the separation set (never
        // moved, never an obstacle), so the overlap simply stands. Against a
        // ghost peer holding the very same rect — one that blocks nobody
        // either, but that the separation layer does see — the pushable
        // element is shoved out: the contrast proves the test can see a push.
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement rigid = TestElement.arbitrated("rigid", 200, 150, new Size(60, 20))
                .withRigid()
                .withSingleCandidate()
                .withPriority(100);
        TestElement pushable = TestElement.arbitrated("pushable", 200, 150, new Size(60, 20))
                .withSingleCandidate()
                .withPriority(0);
        coordinator.register(rigid);
        coordinator.register(pushable);
        double now = 0;
        for (int i = 0; i < 30; i++) {
            now += dt;
            CoordinationResult result = frame(coordinator, now);
            assertEquals(
                    new FloatRect(170, 140, 60, 20),
                    result.elementState("rigid").visualRect(),
                    "frame " + i + ": rigid never moves");
            assertEquals(
                    new FloatRect(170, 140, 60, 20),
                    result.elementState("pushable").visualRect(),
                    "frame " + i + ": nothing pushes the pushable either — the overlap stands");
        }

        InworldCoordinator ghostScene = InworldCoordinator.withDefaults();
        TestElement ghost = TestElement.arbitrated("ghost", 200, 150, new Size(60, 20))
                .withLadder(TestElement.ladder(SpacePolicy.ghost, false, true, new Size(60, 20)))
                .withSingleCandidate()
                .withPriority(100);
        TestElement pushablePeer = TestElement.arbitrated("peer", 200, 150, new Size(60, 20))
                .withSingleCandidate()
                .withPriority(0);
        ghostScene.register(ghost);
        ghostScene.register(pushablePeer);
        now = 0;
        for (int i = 0; i < 30; i++) {
            now += dt;
            CoordinationResult result = frame(ghostScene, now);
            assertTrue(
                    result.elementState("peer").visualRect().y() >= 160,
                    "frame " + i + ": against the separation-visible ghost the pushable is shoved out: "
                            + result.elementState("peer").visualRect());
        }
    }

    @Test
    void zoneSnapshotCarriesNoRigidRect() {
        // the zone vocabulary's neighbor source is the previous-frame
        // snapshot: a rigid rect must never appear in it, so a zone-scoring
        // element's overlap term sees standard neighbors only
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement rigid = TestElement.arbitrated("rigid", 200, 150, new Size(60, 20))
                .withRigid()
                .withSingleCandidate();
        TestElement standard = TestElement.arbitrated("standard", 300, 60, new Size(80, 30));
        TestElement consumer = TestElement.arbitrated("consumer", 200, 220, new Size(90, 30));
        coordinator.register(rigid);
        coordinator.register(standard);
        coordinator.register(new ZoneConsumerElement(consumer));

        double now = 0;
        for (int i = 0; i < 10; i++) {
            now += dt;
            frame(coordinator, now);
        }
        PreviousFrameLayout snapshot = coordinator.previousZoneLayout().orElseThrow();
        assertTrue(snapshot.placements().containsKey("standard"), "the standard neighbor is placed");
        assertTrue(snapshot.placements().containsKey("consumer"), "the consumer itself is placed");
        assertFalse(snapshot.placements().containsKey("rigid"), "the rigid rect is invisible to zone scoring");
        assertEquals(Set.of("standard", "consumer"), snapshot.placements().keySet());
    }

    @Test
    void standardElementsArbitrateExactlyAsWithoutTheRigidElement() {
        // the non-interference guarantee: the same frame sequence against
        // two coordinators, one carrying a rigid element registered on top of
        // the contested anchor (and unregistered again later), the other not.
        // Every arbitrating element's outcome must be identical.
        List<CoordinationResult> withRigid = runMixedScene(true);
        List<CoordinationResult> withoutRigid = runMixedScene(false);
        assertEquals(withoutRigid.size(), withRigid.size());
        for (int i = 0; i < withRigid.size(); i++) {
            CoordinationResult a = withRigid.get(i);
            CoordinationResult b = withoutRigid.get(i);
            assertEquals(b.cause(), a.cause(), "cause diverged at frame " + (i + 1));
            assertEquals(b.resolved(), a.resolved(), "resolved diverged at frame " + (i + 1));
            assertEquals(b.epoch(), a.epoch(), "epoch diverged at frame " + (i + 1));
            assertEquals(b.budget(), a.budget(), "budget diverged at frame " + (i + 1));
            for (String id : new String[] {"panel", "tracker"}) {
                assertEquals(
                        b.placementOf(id), a.placementOf(id), "placement of " + id + " diverged at frame " + (i + 1));
                assertEquals(
                        b.elementState(id), a.elementState(id), "state of " + id + " diverged at frame " + (i + 1));
            }
        }
    }

    private static List<CoordinationResult> runMixedScene(boolean withRigidElement) {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement panel = TestElement.arbitrated("panel", 120, 80, new Size(100, 40), new Size(60, 24))
                .withSticky();
        TestElement tracker = TestElement.arbitrated("tracker", 200, 220, new Size(90, 30))
                .withKind(SpaceKind.tracked)
                .withSticky();
        TestElement rigid = TestElement.arbitrated("rigid", 120, 80, new Size(100, 40))
                .withRigid()
                .withPriority(100)
                .withSingleCandidate();
        coordinator.register(panel);
        coordinator.register(tracker);

        List<CoordinationResult> results = new ArrayList<>();
        double now = 0;
        for (int i = 0; i < 30; i++) {
            now += dt;
            if (i == 10 && withRigidElement) {
                // a membership change that must not be one: the rigid element
                // registers right on top of the contested anchor
                coordinator.register(rigid);
            }
            if (i == 15) {
                panel.shift(80, 30); // past the displacement threshold: a resolve
            }
            if (i == 22 && withRigidElement) {
                coordinator.unregister("rigid");
            }
            results.add(frame(coordinator, now));
            if (withRigidElement && i >= 10 && i < 22) {
                assertNotNull(
                        results.get(results.size() - 1).placementOf("rigid"),
                        "the rigid element presents whenever it has a screen candidate");
            }
        }
        return results;
    }

    // endregion

    // region element shapes

    /**
     * A rigid element declaring one dock: the first candidate sits
     * {@code dockX}/{@code dockY} off the anchor center, the alternates sit
     * further out — the coordinator must always take the first, competition
     * or not.
     */
    private static final class DockedElement implements InworldElement {

        private final String id;
        private final Size size;
        private final double dockX;
        private final double dockY;
        private FloatPos anchor;

        boolean anchorValid = true;

        DockedElement(String id, double anchorX, double anchorY, double dockX, double dockY) {
            this(id, anchorX, anchorY, new Size(60, 20), dockX, dockY);
        }

        DockedElement(String id, double anchorX, double anchorY, Size size, double dockX, double dockY) {
            this.id = id;
            this.size = size;
            this.anchor = new FloatPos(anchorX, anchorY);
            this.dockX = dockX;
            this.dockY = dockY;
        }

        void moveTo(double x, double y) {
            anchor = new FloatPos(x, y);
        }

        FloatPos anchor() {
            return anchor;
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
            return TestElement.ladder(SpacePolicy.active, false, true, size);
        }

        @Override
        public AvoidanceClass avoidanceClass() {
            return AvoidanceClass.rigid;
        }

        @Override
        public ElementProposal propose(ProposeContext context) {
            if (!anchorValid) {
                return ElementProposal.retract(context.variant());
            }
            List<PlacementCandidate> candidates = new ArrayList<>(2);
            candidates.add(PlacementCandidate.screen(FloatRect.around(
                    new FloatPos(anchor.x() + dockX, anchor.y() + dockY), size.width(), size.height())));
            candidates.add(PlacementCandidate.screen(FloatRect.around(
                    new FloatPos(anchor.x() - dockX - 60, anchor.y() - dockY - 60), size.width(), size.height())));
            return ElementProposal.of(context.variant(), anchor, candidates);
        }
    }

    /**
     * A fixed-policy sticky lattice element — the coincidence-release shape;
     * {@code rigid} turns its held rect into one nobody may treat as a
     * blocker.
     */
    private static final class FixedLatticeElement implements InworldElement {

        private final String id;
        private final int priority;
        private final FloatPos anchor;
        private final List<FloatRect> lattice;
        private final boolean rigid;

        FixedLatticeElement(String id, int priority, List<FloatRect> lattice, boolean rigid) {
            this.id = id;
            this.priority = priority;
            this.anchor = new FloatPos(200, 150);
            this.lattice = List.copyOf(lattice);
            this.rigid = rigid;
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
        public AvoidanceClass avoidanceClass() {
            return rigid ? AvoidanceClass.rigid : AvoidanceClass.standard;
        }

        @Override
        public ElementProposal propose(ProposeContext context) {
            List<PlacementCandidate> candidates = new ArrayList<>(lattice.size());
            for (FloatRect rect : lattice) {
                candidates.add(PlacementCandidate.screen(rect));
            }
            return ElementProposal.of(context.variant(), anchor, candidates);
        }
    }

    /** Wraps a {@link TestElement} so it consumes zone layout while keeping its proposal. */
    private static final class ZoneConsumerElement implements InworldElement {

        private final TestElement delegate;

        ZoneConsumerElement(TestElement delegate) {
            this.delegate = delegate;
        }

        @Override
        public String id() {
            return delegate.id();
        }

        @Override
        public SpaceKind spaceKind() {
            return delegate.spaceKind();
        }

        @Override
        public VariantLadder ladder() {
            return delegate.ladder();
        }

        @Override
        public ElementProposal propose(ProposeContext context) {
            return delegate.propose(context);
        }

        @Override
        public int priority() {
            return delegate.priority();
        }

        @Override
        public boolean sticky() {
            return delegate.sticky();
        }

        @Override
        public boolean consumesZoneLayout() {
            return true;
        }
    }

    // endregion
}
