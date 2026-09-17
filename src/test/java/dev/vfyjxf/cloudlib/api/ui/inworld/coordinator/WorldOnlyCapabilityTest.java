package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.AssembledElement;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.ElementSpec;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.InworldLayoutContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.InworldLayouter;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.InworldProfile;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutEnvironment;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PipelineAssembler;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.VisibilityTracker;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.HashSet;
import java.util.List;
import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The world-only capability (see {@link InworldElement#worldOnly()}): an
 * element that lives purely in world space never enters the screen
 * arbitration flow. These tests pin the three halves of that contract — the
 * element's own lifecycle (states, phase transitions, world-only grants),
 * the non-interference guarantee (the projecting elements' outcome is
 * byte-identical to a run without the world-only element), and the contract
 * edge (exclusions cannot move or hide it; the capability bit and the
 * proposal shape must agree).
 */
class WorldOnlyCapabilityTest {

    private static final int width = 400;
    private static final int height = 300;
    private static final double dt = 1.0 / 60.0;

    private static CoordinationResult frame(InworldCoordinator coordinator, double now) {
        return coordinator.frame(InworldCoordinator.FrameInput.of(width, height, now, dt));
    }

    private static CoordinationResult frame(InworldCoordinator coordinator, double now, Rect... exclusions) {
        return coordinator.frame(InworldCoordinator.FrameInput.of(width, height, now, dt, exclusions));
    }

    // region lifecycle

    @Test
    void worldOnlyElementPresentsWithOnlyAWorldPartAndRetractsIntoLinger() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement bar =
                TestElement.arbitrated("bar", 200, 150, new Size(60, 20)).withWorldOnly();
        TestElement nameplate = TestElement.arbitrated("nameplate", 300, 60, new Size(80, 30));
        coordinator.register(bar);
        coordinator.register(nameplate);

        double now = 0;
        for (int i = 0; i < 12; i++) {
            now += dt;
            CoordinationResult result = frame(coordinator, now);
            // elementStates exactly cover the registration set, every frame
            Set<String> reported = new HashSet<>();
            for (CoordinationResult.ElementState state : result.elementStates()) {
                reported.add(state.elementId());
            }
            assertEquals(Set.of("bar", "nameplate"), reported);

            InworldPlacement grant = result.placementOf("bar");
            assertNotNull(grant, "the world-only element presents with a world candidate");
            assertNotNull(grant.world());
            assertEquals(WorldAabb.around(200, 64.0, 150, 60, 8.0, 20), grant.world());
            // the screen half is degenerate: no projection was coordinated
            assertEquals(FloatRect.empty, grant.offsetRect());
            assertNull(result.elementState("bar").visualRect());
            assertNull(result.elementState("bar").rejection());
        }
        // one propose per frame, round 0 only — the world-only flow has no
        // renegotiation round even while other elements arbitrate
        assertEquals(12, bar.proposeCount);
        assertEquals(0, bar.lastRound);
        // the fade-in completed: presented, at full alpha
        assertEquals(
                VisibilityTracker.Phase.visible, lastState(coordinator, "bar").phase());
        assertEquals(1.0, lastState(coordinator, "bar").alpha(), 1.0e-9);

        // the anchor is gone: retract — state without placement, linger (not
        // rejection), the last world grant retained as sticky memory
        bar.anchorValid = false;
        now += dt;
        CoordinationResult retracted = frame(coordinator, now);
        assertNull(retracted.placementOf("bar"));
        assertNotNull(retracted.elementState("bar").placement());
        assertNull(retracted.elementState("bar").rejection());
        assertEquals(
                VisibilityTracker.Phase.lingering, retracted.elementState("bar").phase());
        assertEquals(1.0, retracted.elementState("bar").alpha(), 1.0e-9);

        // linger expires, the element fades out and hides — still a state
        for (int i = 0; i < 45; i++) {
            now += dt;
            frame(coordinator, now);
        }
        VisibilityTracker.Phase hidden = lastState(coordinator, "bar").phase();
        assertTrue(
                hidden == VisibilityTracker.Phase.fading || hidden == VisibilityTracker.Phase.hidden,
                "expected the retracted element to fade out, got " + hidden);

        // the anchor recovers: presented again, fading back in toward visible
        bar.anchorValid = true;
        for (int i = 0; i < 30; i++) {
            now += dt;
            CoordinationResult recovered = frame(coordinator, now);
            assertNotNull(recovered.placementOf("bar"));
        }
        assertEquals(
                VisibilityTracker.Phase.visible, lastState(coordinator, "bar").phase());
        assertEquals(1.0, lastState(coordinator, "bar").alpha(), 1.0e-9);
    }

    @Test
    void worldOnlyProposalWithoutAWorldCandidateRetractsInsteadOfRejecting() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        // a world-only element whose candidates are screen-only: nothing to
        // grant in world space, so the element retracts (state, no placement,
        // no rejection)
        coordinator.register(new StubElement("stub", true) {

            @Override
            public ElementProposal propose(ProposeContext context) {
                return ElementProposal.worldOnly(
                        context.variant(), List.of(PlacementCandidate.screen(new FloatRect(10, 10, 60, 20))));
            }
        });

        CoordinationResult first = frame(coordinator, dt);
        assertNull(first.placementOf("stub"));
        assertNull(first.elementState("stub").placement());
        assertNull(first.elementState("stub").rejection());
        assertEquals(VisibilityTracker.Phase.hidden, first.elementState("stub").phase());
    }

    @Test
    void worldOnlyGrantSkipsScreenOnlyCandidatesAndTakesTheWorldOne() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        WorldAabb box = WorldAabb.around(12, 64, -3, 60, 8, 20);
        coordinator.register(new StubElement("mixed", true) {

            @Override
            public ElementProposal propose(ProposeContext context) {
                return ElementProposal.worldOnly(
                        context.variant(),
                        List.of(PlacementCandidate.screen(new FloatRect(0, 0, 60, 20)), PlacementCandidate.world(box)));
            }
        });

        CoordinationResult result = frame(coordinator, dt);
        InworldPlacement grant = result.placementOf("mixed");
        assertNotNull(grant);
        assertEquals(box, grant.world());
    }

    // endregion

    // region non-interference

    @Test
    void projectingElementsArbitrateExactlyAsWithoutTheWorldOnlyElement() {
        // the same frame sequence against two coordinators: one carries a
        // high-priority world-only element on top of the projecting scene
        // (registered mid-run, unregistering again later), the other does
        // not. Every projecting element's outcome — placement, state, budget,
        // cause, epoch — must be identical: the world-only element occupies
        // no screen estate and re-resolves nothing.
        List<CoordinationResult> withWorld = runMixedScene(true);
        List<CoordinationResult> withoutWorld = runMixedScene(false);
        assertEquals(withoutWorld.size(), withWorld.size());
        for (int i = 0; i < withWorld.size(); i++) {
            CoordinationResult a = withWorld.get(i);
            CoordinationResult b = withoutWorld.get(i);
            assertEquals(b.cause(), a.cause(), "cause diverged at frame " + (i + 1));
            assertEquals(b.resolved(), a.resolved(), "resolved diverged at frame " + (i + 1));
            assertEquals(b.epoch(), a.epoch(), "epoch diverged at frame " + (i + 1));
            assertEquals(b.budget(), a.budget(), "budget diverged at frame " + (i + 1));
            for (String id : new String[] {"panel", "hud", "tracker"}) {
                assertEquals(
                        b.placementOf(id), a.placementOf(id), "placement of " + id + " diverged at frame " + (i + 1));
                assertEquals(
                        b.elementState(id), a.elementState(id), "state of " + id + " diverged at frame " + (i + 1));
            }
        }
    }

    private static List<CoordinationResult> runMixedScene(boolean withWorldOnly) {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement panel = TestElement.arbitrated("panel", 120, 80, new Size(100, 40), new Size(60, 24))
                .withSticky()
                .withPriority(3);
        TestElement hud = TestElement.arbitrated("hud", 300, 60, new Size(80, 30))
                .withKind(SpaceKind.panel)
                .withPriority(10);
        TestElement tracker = TestElement.arbitrated("tracker", 200, 220, new Size(90, 30))
                .withKind(SpaceKind.tracked)
                .withSticky();
        TestElement billboard = TestElement.arbitrated("billboard", 120, 80, new Size(100, 40))
                .withPriority(100)
                .withWorldOnly();
        coordinator.register(panel);
        coordinator.register(hud);
        coordinator.register(tracker);

        List<CoordinationResult> results = new ArrayList<>();
        double now = 0;
        Rect exclusion = new Rect(0, 260, 400, 40);
        for (int i = 0; i < 30; i++) {
            now += dt;
            boolean billboardLive = false;
            if (i == 10 && withWorldOnly) {
                // a membership change that must not be one: the world-only
                // element registers right on top of the contested anchor
                coordinator.register(billboard);
                billboardLive = true;
            }
            billboardLive = billboardLive || (withWorldOnly && i > 10 && i < 22);
            if (i == 15) {
                panel.shift(80, 30); // past the displacement threshold: a resolve
            }
            if (i == 22 && withWorldOnly) {
                coordinator.unregister("billboard");
            }
            results.add(frame(coordinator, now, exclusion));
            if (billboardLive) {
                assertNotNull(
                        results.get(results.size() - 1).placementOf("billboard"),
                        "the world-only element must present whenever it has a world candidate");
            }
        }
        return results;
    }

    // endregion

    // region screen-space pressure

    @Test
    void exclusionsCoveringTheWouldBeRectNeitherMoveNorHideTheWorldOnlyElement() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement billboard =
                TestElement.arbitrated("billboard", 200, 150, new Size(60, 40)).withWorldOnly();
        TestElement panel =
                TestElement.arbitrated("panel", 200, 150, new Size(60, 40)).withSingleCandidate();
        coordinator.register(billboard);
        coordinator.register(panel);

        double now = 0;
        now += dt;
        CoordinationResult free = frame(coordinator, now);
        assertNotNull(free.placementOf("billboard"));
        assertNotNull(free.placementOf("panel"));
        // let the fade-in finish so the phases below are exact
        for (int i = 0; i < 12; i++) {
            now += dt;
            frame(coordinator, now);
        }
        assertEquals(
                VisibilityTracker.Phase.visible,
                lastState(coordinator, "billboard").phase());

        // a HUD exclusion exactly over the would-be screen rect of both
        // elements: the projecting one must yield (rejection), the world-only
        // one must stay put and presented — same world box, no rejection
        Rect hud = new Rect(120, 90, 160, 120);
        WorldAabb expected = WorldAabb.around(200, 64.0, 150, 60, 8.0, 40);
        for (int i = 0; i < 20; i++) {
            now += dt;
            CoordinationResult squeezed = frame(coordinator, now, hud);
            InworldPlacement grant = squeezed.placementOf("billboard");
            assertNotNull(grant, "an exclusion cannot hide a world-only element");
            assertEquals(expected, grant.world(), "an exclusion cannot move a world-only element");
            assertNull(squeezed.elementState("billboard").rejection());
            assertEquals(
                    VisibilityTracker.Phase.visible,
                    squeezed.elementState("billboard").phase());

            assertNull(squeezed.placementOf("panel"), "the projecting element must yield to the exclusion");
            assertNotNull(squeezed.elementState("panel").rejection());
            assertEquals(
                    RejectionReason.exclusion,
                    squeezed.elementState("panel").rejection().reason());
        }
    }

    // endregion

    // region contract edges

    @Test
    void aProjectingElementMayNotProposeAWorldOnlyProposal() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        coordinator.register(new StubElement("fake", false) {

            @Override
            public ElementProposal propose(ProposeContext context) {
                return ElementProposal.worldOnly(
                        context.variant(), PlacementCandidate.world(WorldAabb.around(0, 64, 0, 10, 8, 10)));
            }
        });
        assertThrows(IllegalArgumentException.class, () -> frame(coordinator, dt));
    }

    @Test
    void specDrivenCustomLayouterDeclaresWorldOnlyThroughTheSpec() {
        WorldAabb box = WorldAabb.around(4, 70, 4, 60, 8, 20);
        InworldLayouter billboard = new InworldLayouter() {

            @Override
            public SpaceReservation reserve(InworldLayoutContext ctx) {
                return SpaceReservation.arbitrated(SpaceKind.world);
            }

            @Override
            public ElementProposal propose(InworldLayoutContext ctx) {
                return ElementProposal.worldOnly(ctx.variant(), PlacementCandidate.world(box));
            }

            @Override
            public void arbitrated(InworldPlacement placement, Feedback feedback) {}
        };
        ElementSpec spec = ElementSpec.from(InworldProfile.dock, "billboard")
                .custom(billboard)
                .withWorldOnly();
        AssembledElement element = PipelineAssembler.create().assemble(spec);
        assertTrue(element.worldOnly(), "the assembled element forwards the spec's capability bit");

        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        coordinator.register(element);
        double now = 0;
        for (int i = 0; i < 12; i++) {
            now += dt;
            element.beginFrame(LayoutEnvironment.of(width, height));
            CoordinationResult result = frame(coordinator, now);
            element.observe(result);
        }
        InworldPlacement grant = lastResult(coordinator).placementOf("billboard");
        assertNotNull(grant);
        assertEquals(box, grant.world());
        assertEquals(FloatRect.empty, grant.offsetRect());
    }

    // endregion

    // region helpers

    private static CoordinationResult.ElementState lastState(InworldCoordinator coordinator, String id) {
        CoordinationResult last = lastResult(coordinator);
        assertNotNull(last);
        CoordinationResult.ElementState state = last.elementState(id);
        assertNotNull(state);
        return state;
    }

    private static CoordinationResult lastResult(InworldCoordinator coordinator) {
        return coordinator.lastResult().orElse(null);
    }

    /** An element whose capability bit is fixed and whose propose is overridden per test. */
    private abstract static class StubElement implements InworldElement {

        private final String id;
        private final boolean worldOnly;

        StubElement(String id, boolean worldOnly) {
            this.id = id;
            this.worldOnly = worldOnly;
        }

        @Override
        public String id() {
            return id;
        }

        @Override
        public SpaceKind spaceKind() {
            return SpaceKind.world;
        }

        @Override
        public VariantLadder ladder() {
            return TestElement.ladder(new Size(60, 20));
        }

        @Override
        public boolean worldOnly() {
            return worldOnly;
        }
    }

    // endregion
}
