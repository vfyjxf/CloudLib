package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpacePolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.VisibilityTracker;
import org.junit.jupiter.api.Test;

import java.util.ArrayList;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * End-to-end pipeline behavior: stability of the sticky slot across linger
 * and recovery, the dirty/epoch re-resolve rules, space-policy effects, the
 * epoch upgrade path, and whole-run determinism.
 */
class CoordinatorScenarioTest {

    private static final int width = 400;
    private static final int height = 300;
    private static final double dt = 1.0 / 60.0;

    private static CoordinationResult frame(InworldCoordinator coordinator, double now) {
        return coordinator.frame(InworldCoordinator.FrameInput.of(width, height, now, dt));
    }

    private static CoordinationResult frame(InworldCoordinator coordinator, double now, Rect... exclusions) {
        return coordinator.frame(InworldCoordinator.FrameInput.of(width, height, now, dt, exclusions));
    }

    // region determinism

    @Test
    void sameInputProducesTheSameOutputAcrossRuns() {
        List<String> first = runScriptedScene();
        List<String> second = runScriptedScene();
        assertEquals(first, second);
        assertEquals(18, first.size());
    }

    /** A mixed scene exercising every renegotiation cause plus linger and recovery. */
    private static List<String> runScriptedScene() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement nameplate = TestElement.arbitrated("nameplate", 120, 80, new Size(100, 40), new Size(60, 24))
                .withSticky().withPriority(3);
        TestElement hud = TestElement.arbitrated("hud", 300, 60, new Size(80, 30), new Size(40, 20))
                .withKind(SpaceKind.panel).withPriority(10);
        TestElement tracker = TestElement.arbitrated("tracker", 200, 220, new Size(90, 30), new Size(50, 18))
                .withSticky().withKind(SpaceKind.tracked);
        TestElement fixedPanel = TestElement.arbitrated("fixedPanel", 250, 150, new Size(40, 40))
                .withLadder(TestElement.ladder(SpacePolicy.fixed, false, true, new Size(40, 40)));
        TestElement ghostMark = TestElement.arbitrated("ghostMark", 200, 150, new Size(30, 30))
                .withLadder(TestElement.ladder(SpacePolicy.ghost, false, true, new Size(30, 30))).withPriority(1);

        coordinator.register(nameplate);
        coordinator.register(hud);
        coordinator.register(tracker);
        coordinator.register(ghostMark);

        List<Rect> bottomStrip = List.of(new Rect(0, 280, 400, 20));
        List<String> snapshots = new ArrayList<>();
        double now = -dt;

        now = step(coordinator, snapshots, now, bottomStrip);
        now = step(coordinator, snapshots, now, bottomStrip); // idle frame
        nameplate.shift(3, 2); // sub-threshold drift
        now = step(coordinator, snapshots, now, bottomStrip);
        coordinator.register(fixedPanel); // membership change
        now = step(coordinator, snapshots, now, bottomStrip);
        nameplate.anchorValid = false; // anchor lost
        for (int i = 0; i < 4; i++) {
            now = step(coordinator, snapshots, now, bottomStrip); // linger
        }
        nameplate.anchorValid = true; // recovery
        now = step(coordinator, snapshots, now, bottomStrip);
        now = step(coordinator, snapshots, now); // exclusion removed
        tracker.shift(60, 0); // anchor jump
        now = step(coordinator, snapshots, now);
        coordinator.unregister("ghostMark"); // membership change
        now = step(coordinator, snapshots, now);
        for (int i = 0; i < 5; i++) {
            now = step(coordinator, snapshots, now); // epoch ticks
        }
        hud.shift(0, 40);
        now = step(coordinator, snapshots, now);
        return snapshots;
    }

    private static double step(
        InworldCoordinator coordinator,
        List<String> snapshots,
        double now,
        List<Rect> exclusions
    ) {
        double next = now + dt;
        CoordinationResult result = coordinator
                .frame(InworldCoordinator.FrameInput.of(width, height, next, dt, exclusions));
        snapshots.add(describe(result));
        return next;
    }

    private static double step(InworldCoordinator coordinator, List<String> snapshots, double now) {
        return step(coordinator, snapshots, now, List.of());
    }

    private static String describe(CoordinationResult result) {
        StringBuilder text = new StringBuilder("f" + result.frame() + "e" + result.epoch() + "c" + result.cause());
        for (CoordinationResult.ElementState state : result.elementStates()) {
            text.append(' ').append(state.elementId()).append(':');
            text.append(state.placement() == null ? "-" : state.placement().variant().level());
            text.append(
                state.visualRect() == null
                        ? "-"
                        : String.format(
                            "(%.2f,%.2f,%.0fx%.0f)",
                            state.visualRect().x(),
                            state.visualRect().y(),
                            state.visualRect().width(),
                            state.visualRect().height()
                        )
            );
            text.append('/').append(state.phase()).append('/').append(String.format("%.3f", state.alpha()));
            if (state.rejection() != null) {
                text.append("/rej:").append(state.rejection().reason()).append(':')
                        .append(state.rejection().blockerId());
            }
        }
        return text.toString();
    }

    // endregion

    // region linger and recovery

    @Test
    void invalidAnchorLingersInsteadOfVanishing() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement element = TestElement.arbitrated("e", 200, 150, new Size(100, 40), new Size(60, 24)).withSticky();
        coordinator.register(element);

        CoordinationResult placed = frame(coordinator, 0);
        FloatRect placedVisual = placed.elementState("e").visualRect();
        assertNotNull(placedVisual);
        double now = advance(coordinator, dt, 12); // fully faded in and visible

        element.anchorValid = false;
        now += dt;
        CoordinationResult retracted = frame(coordinator, now);

        assertFalse(retracted.resolved(), "a retraction is not a space event");
        assertEquals(CoordinationResult.RenegotiationCause.none, retracted.cause());
        assertNull(retracted.placementOf("e"), "a retracted element is not presented");
        CoordinationResult.ElementState state = retracted.elementState("e");
        assertEquals(VisibilityTracker.Phase.lingering, state.phase());
        assertEquals(1.0, state.alpha(), 1.0e-9);
        assertEquals(placedVisual, state.visualRect(), "the rect freezes in place while lingering");
        assertNotNull(state.placement(), "the sticky memory is retained");

        // still lingering well past the epoch, then fading, then gone
        now = advance(coordinator, now, 10);
        assertEquals(
            VisibilityTracker.Phase.lingering,
            coordinator.lastResult().orElseThrow().elementState("e").phase()
        );
        now = advance(coordinator, now, 10); // ~0.35s total: past linger (0.25) into fade
        assertEquals(VisibilityTracker.Phase.fading, coordinator.lastResult().orElseThrow().elementState("e").phase());
        now = advance(coordinator, now, 16); // ~0.6s: past fade (0.25)
        CoordinationResult.ElementState gone = coordinator.lastResult().orElseThrow().elementState("e");
        assertEquals(VisibilityTracker.Phase.hidden, gone.phase());
        assertEquals(0.0, gone.alpha(), 1.0e-9);
    }

    @Test
    void recoveredAnchorReturnsToItsStickyPosition() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement element = TestElement.arbitrated("e", 200, 150, new Size(100, 40), new Size(60, 24)).withSticky();
        coordinator.register(element);

        CoordinationResult placed = frame(coordinator, 0);
        FloatRect original = placed.placementOf("e").screenRect();
        double now = advance(coordinator, dt, 12); // fully faded in and visible

        element.anchorValid = false;
        now += dt;
        now = advance(coordinator, now, 2); // a few lingering frames
        element.anchorValid = true;
        now += dt;
        CoordinationResult recovered = frame(coordinator, now);

        assertEquals(CoordinationResult.RenegotiationCause.presentationChanged, recovered.cause());
        assertTrue(recovered.resolved());
        InworldPlacement placement = recovered.placementOf("e");
        assertNotNull(placement, "recovery re-presents the element");
        assertEquals(placed.placementOf("e").offsetRect().x(), placement.offsetRect().x(), 0.01);
        assertEquals(placed.placementOf("e").offsetRect().y(), placement.offsetRect().y(), 0.01);
        assertEquals(original.x(), placement.screenRect().x(), 0.01);
        assertEquals(original.y(), placement.screenRect().y(), 0.01);
        CoordinationResult.ElementState state = recovered.elementState("e");
        assertEquals(VisibilityTracker.Phase.visible, state.phase(), "a linger rescue keeps full alpha");
        assertEquals(1.0, state.alpha(), 1.0e-9);
    }

    @Test
    void recoveryFromFullyHiddenAlsoRestoresTheStickyPosition() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement element = TestElement.arbitrated("e", 200, 150, new Size(100, 40), new Size(60, 24)).withSticky();
        coordinator.register(element);
        CoordinationResult placed = frame(coordinator, 0);
        double now = advance(coordinator, dt, 12); // fully faded in and visible

        element.anchorValid = false;
        now += dt;
        now = advance(coordinator, now, 40); // linger + fade out completely
        assertEquals(VisibilityTracker.Phase.hidden, coordinator.lastResult().orElseThrow().elementState("e").phase());

        element.anchorValid = true;
        now += dt;
        CoordinationResult recovered = frame(coordinator, now);
        InworldPlacement placement = recovered.placementOf("e");
        assertNotNull(placement);
        assertEquals(placed.placementOf("e").offsetRect().x(), placement.offsetRect().x(), 0.01);
        assertEquals(placed.placementOf("e").offsetRect().y(), placement.offsetRect().y(), 0.01);
        assertEquals(VisibilityTracker.Phase.appearing, recovered.elementState("e").phase());
    }

    // endregion

    // region dirty and epoch rules

    @Test
    void subThresholdAnchorDriftDoesNotTriggerAResolve() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement element = TestElement.arbitrated("e", 200, 150, new Size(100, 40), new Size(60, 24)).withSticky();
        coordinator.register(element);
        CoordinationResult first = frame(coordinator, 0);
        assertEquals(150, first.placementOf("e").screenRect().x(), 0.01);

        element.shift(5, 0); // below the 12 px threshold — and it stays there
        double now = 0;
        for (int i = 0; i < 10; i++) {
            now += dt;
            CoordinationResult result = frame(coordinator, now);
            assertFalse(result.resolved(), "sub-threshold drift must not re-resolve");
            assertEquals(CoordinationResult.RenegotiationCause.none, result.cause());
            assertEquals(1, result.epoch());
            // the offset is untouched; the continuous layer carries the drift
            InworldPlacement placement = result.placementOf("e");
            assertEquals(first.placementOf("e").offsetRect().x(), placement.offsetRect().x(), 1.0e-9);
            assertEquals(first.placementOf("e").offsetRect().y(), placement.offsetRect().y(), 1.0e-9);
            assertEquals(155, placement.screenRect().x(), 1.0e-9);
        }
        // the spring has converged onto the drifted target
        assertEquals(155, coordinator.lastResult().orElseThrow().elementState("e").visualRect().x(), 0.5);

        // with nothing else happening, the epoch timer is what eventually fires
        CoordinationResult epochFrame = null;
        for (int i = 0; i < 6 && epochFrame == null; i++) {
            now += dt;
            CoordinationResult result = frame(coordinator, now);
            if (result.resolved()) {
                epochFrame = result;
            }
        }
        assertNotNull(epochFrame, "the epoch timer must eventually fire");
        assertEquals(CoordinationResult.RenegotiationCause.epochElapsed, epochFrame.cause());
        assertEquals(2, coordinator.epoch());
    }

    @Test
    void anchorJumpTriggersResolveAndFlipsInsteadOfTeleporting() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement element = TestElement.arbitrated("e", 200, 150, new Size(100, 40), new Size(60, 24)).withSticky();
        coordinator.register(element);
        CoordinationResult first = frame(coordinator, 0);
        FloatRect visualAtRest = first.elementState("e").visualRect();

        element.shift(60, 0); // well past the 12 px threshold
        double now = dt;
        CoordinationResult jumped = frame(coordinator, now);

        assertEquals(CoordinationResult.RenegotiationCause.anchorDisplacement, jumped.cause());
        assertTrue(jumped.resolved());
        // sticky: the same slot, re-anchored
        InworldPlacement placement = jumped.placementOf("e");
        assertEquals(first.placementOf("e").offsetRect().x(), placement.offsetRect().x(), 0.01);
        assertEquals(210, placement.screenRect().x(), 0.01);
        // the discrete move is a FLIP: the visual starts where it rested
        FloatRect visual = jumped.elementState("e").visualRect();
        assertEquals(visualAtRest.x(), visual.x(), 0.01);
        assertEquals(visualAtRest.y(), visual.y(), 0.01);

        now = advance(coordinator, now, 30); // 0.5 s: past the 250 ms morph clamp
        FloatRect settled = coordinator.lastResult().orElseThrow().elementState("e").visualRect();
        assertEquals(210, settled.x(), 0.5);
        assertEquals(130, settled.y(), 0.5);
    }

    @Test
    void exclusionChangeReroutesTheElement() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement element = TestElement.arbitrated("e", 200, 150, new Size(100, 40), new Size(60, 24));
        coordinator.register(element);
        frame(coordinator, 0);
        assertEquals(150, coordinator.placementOf("e").orElseThrow().screenRect().x(), 0.01);

        Rect cover = new Rect(200, 100, 200, 100);
        double now = dt;
        CoordinationResult rerouted = frame(coordinator, now, cover);

        assertEquals(CoordinationResult.RenegotiationCause.exclusionsChanged, rerouted.cause());
        FloatRect rect = rerouted.placementOf("e").screenRect();
        assertTrue(rect.intersection(toFloat(cover)).area() == 0, "must not sit on the exclusion: " + rect);
        // the hugging exclusion became a strut: the work area is now
        // 200 px wide and the primary candidate clamps flush against it
        assertEquals(100, rect.x(), 0.01, "routed to the free left side: " + rect);
        assertEquals(200, rect.right(), 0.01);
    }

    @Test
    void membershipChangesTriggerResolves() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement first = TestElement.arbitrated("a", 200, 150, new Size(100, 40));
        coordinator.register(first);
        frame(coordinator, 0);
        assertEquals(
            CoordinationResult.RenegotiationCause.membershipChanged,
            coordinator.lastResult().orElseThrow().cause()
        );

        double now = dt;
        now = advance(coordinator, now, 3);
        assertFalse(coordinator.lastResult().orElseThrow().resolved());

        TestElement second = TestElement.arbitrated("b", 100, 60, new Size(60, 24));
        coordinator.register(second);
        CoordinationResult joined = frame(coordinator, now);
        assertEquals(CoordinationResult.RenegotiationCause.membershipChanged, joined.cause());
        assertTrue(joined.resolved());
        assertNotNull(joined.placementOf("b"));

        now += dt;
        assertTrue(coordinator.unregister("b"));
        CoordinationResult left = frame(coordinator, now);
        assertEquals(CoordinationResult.RenegotiationCause.membershipChanged, left.cause());
        assertNull(left.elementState("b"), "an unregistered element is gone outright");
    }

    // endregion

    // region space policy and upgrade

    @Test
    void fixedPolicyIsExemptButStillOccupies() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement normal = TestElement.arbitrated("normal", 200, 150, new Size(100, 40)).withPriority(10);
        TestElement fixedElement = TestElement.arbitrated("fixed", 200, 150, new Size(100, 40))
                .withKind(SpaceKind.panel)
                .withLadder(TestElement.ladder(SpacePolicy.fixed, false, true, new Size(100, 40)));
        coordinator.register(normal);
        coordinator.register(fixedElement);

        CoordinationResult result = frame(coordinator, 0);

        // the fixed element is granted its contested rect unconditionally
        assertEquals(new FloatRect(150, 130, 100, 40), result.placementOf("fixed").screenRect());
        assertTrue(
            result.placementOf("normal").screenRect().intersects(result.placementOf("fixed").screenRect()),
            "fixed is exempt from avoidance: the overlap stands at commit"
        );

        // and relax pushes the pushable visual out of the fixed one over frames
        double now = dt;
        now = advance(coordinator, now, 20);
        FloatRect normalVisual = coordinator.lastResult().orElseThrow().elementState("normal").visualRect();
        FloatRect fixedVisual = coordinator.lastResult().orElseThrow().elementState("fixed").visualRect();
        assertTrue(
            normalVisual.intersection(fixedVisual).area() == 0,
            "relax must separate the pushable element from the fixed one: " + normalVisual + " vs " + fixedVisual
        );
        assertEquals(new FloatRect(150, 130, 100, 40), fixedVisual, "the fixed rect itself never moves");
    }

    @Test
    void ghostPolicyIsInvisibleToArbitration() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement ghost = TestElement.arbitrated("ghost", 200, 150, new Size(30, 30))
                .withLadder(TestElement.ladder(SpacePolicy.ghost, false, true, new Size(30, 30))).withPriority(10);
        TestElement solid = TestElement.arbitrated("solid", 200, 150, new Size(100, 40));
        coordinator.register(ghost);
        coordinator.register(solid);

        CoordinationResult result = frame(coordinator, 0);

        assertNotNull(result.placementOf("ghost"), "ghosts still get their placement");
        assertNotNull(result.placementOf("solid"));
        assertEquals(
            150,
            result.placementOf("solid").screenRect().x(),
            0.01,
            "the ghost occupies nothing: the solid element takes the spot"
        );
        assertTrue(result.placementOf("solid").screenRect().intersects(result.placementOf("ghost").screenRect()));
    }

    @Test
    void epochUpgradeRestoresTheDegradedVariant() {
        // 152 px tall so the strip's cell boundary does not eat into the
        // 48 px work area it leaves below (the bitmap is deliberately
        // conservative about touched cells)
        Rect topStrip = new Rect(0, 0, 300, 152);
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement element = TestElement.arbitrated("e", 150, 100, new Size(160, 100), new Size(80, 40));
        coordinator.register(element);

        // squeezed: full is out of bounds, compact is granted in round 1
        CoordinationResult squeezed = coordinator.frame(InworldCoordinator.FrameInput.of(300, 200, 0, dt, topStrip));
        assertEquals(1, squeezed.placementOf("e").variant().level());

        double now = 0;
        // the squeeze clears mid-epoch: a re-resolve happens (exclusions
        // changed) but no upgrade — upgrades are epoch-boundary-only
        now += 6 * dt;
        CoordinationResult cleared = frame(coordinator, now);
        assertEquals(CoordinationResult.RenegotiationCause.exclusionsChanged, cleared.cause());
        assertEquals(1, cleared.placementOf("e").variant().level(), "no upgrade outside an epoch boundary");

        // the next epoch tick climbs one rung back up
        int guard = 0;
        while (coordinator.lastResult().orElseThrow().placementOf("e") == null
                || coordinator.lastResult().orElseThrow().placementOf("e").variant().level() != 0) {
            assertTrue(guard++ < 120, "the upgrade must happen within a couple of epochs");
            now += dt;
            frame(coordinator, now);
        }
        CoordinationResult upgraded = coordinator.lastResult().orElseThrow();
        assertEquals(CoordinationResult.RenegotiationCause.epochElapsed, upgraded.cause());
        assertEquals(0, upgraded.placementOf("e").variant().level());
        assertEquals(160, upgraded.placementOf("e").screenRect().width(), 0.01);
        assertEquals(100, upgraded.placementOf("e").screenRect().height(), 0.01);
    }

    // endregion

    private static double advance(InworldCoordinator coordinator, double now, int frames) {
        for (int i = 0; i < frames; i++) {
            now += dt;
            coordinator.frame(InworldCoordinator.FrameInput.of(width, height, now, dt));
        }
        return now;
    }

    private static FloatRect toFloat(Rect rect) {
        return new FloatRect(rect.x(), rect.y(), rect.width(), rect.height());
    }
}
