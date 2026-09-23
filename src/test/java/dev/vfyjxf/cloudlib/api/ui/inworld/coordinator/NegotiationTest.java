package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.SwitchGate;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.VisibilityTracker;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The negotiation protocol: rejection → degradation → acceptance, ladder
 * exhaustion → hidden with linger, the deadline guarantee, budget-driven
 * proactive degradation, and the self-managed register/reject contract.
 */
class NegotiationTest {

    private static final int width = 400;
    private static final int height = 300;
    private static final double dt = 1.0 / 60.0;

    private static CoordinationResult frame(InworldCoordinator coordinator, double now) {
        return coordinator.frame(InworldCoordinator.FrameInput.of(width, height, now, dt));
    }

    private static CoordinationResult frame(InworldCoordinator coordinator, double now, Rect... exclusions) {
        return coordinator.frame(InworldCoordinator.FrameInput.of(width, height, now, dt, exclusions));
    }

    @Test
    void rejectedElementDegradesAndIsAcceptedInRoundOne() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement blocker = TestElement.arbitrated("blocker", 200, 140, new Size(100, 40)).withPriority(10);
        TestElement squeezed = TestElement.arbitrated("squeezed", 200, 175, new Size(100, 40), new Size(60, 24))
                .withSingleCandidate();
        coordinator.register(blocker);
        coordinator.register(squeezed);

        CoordinationResult result = frame(coordinator, 0);

        InworldPlacement placement = result.placementOf("squeezed");
        assertNotNull(placement, "the degraded form must be granted");
        assertEquals(1, placement.variant().level(), "full overlaps the blocker, compact fits");
        assertEquals(new FloatRect(170, 163, 60, 24), placement.screenRect());
        // the negotiation trail: full rejected, compact proposed in round 1
        assertEquals(List.of(0, 1), squeezed.ctxVariants.stream().map(InworldVariant::level).toList());
        assertEquals(List.of(0, 1), squeezed.usedVariants.stream().map(InworldVariant::level).toList());
        assertEquals(2, squeezed.proposeCount);
        assertNotNull(squeezed.lastRejectionSeen);
        assertEquals(RejectionReason.overlap, squeezed.lastRejectionSeen.reason());
        assertEquals("blocker", squeezed.lastRejectionSeen.blockerId());
        assertNull(result.elementState("squeezed").rejection());
    }

    @Test
    void ladderExhaustionHidesTheElementWithLinger() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement victim = TestElement
                .arbitrated("victim", 200, 150, new Size(100, 40), new Size(60, 24), new Size(20, 12));
        coordinator.register(victim);
        frame(coordinator, 0);
        // let the element finish its fade-in so the linger starts from full alpha
        double now = advance(coordinator, dt, 12);
        assertEquals(0, coordinator.placementOf("victim").orElseThrow().variant().level());

        // a full-screen blocker arrives: membership change forces a re-resolve
        TestElement blocker = TestElement.arbitrated("blocker", 200, 150, new Size(400, 300)).withPriority(10);
        coordinator.register(blocker);
        now += dt;
        CoordinationResult squeezed = frame(coordinator, now);

        // the whole ladder was walked within the renegotiation round: full,
        // compact, icon — all rejected
        List<Integer> ctxLevels = victim.ctxVariants.stream().map(InworldVariant::level).toList();
        assertEquals(List.of(0, 1, 2), ctxLevels.subList(ctxLevels.size() - 3, ctxLevels.size()));
        assertNull(squeezed.placementOf("victim"), "an exhausted ladder grants nothing");

        // hidden means linger, not vanish: full alpha, frozen rect
        CoordinationResult.ElementState state = squeezed.elementState("victim");
        assertEquals(VisibilityTracker.Phase.lingering, state.phase());
        assertEquals(1.0, state.alpha(), 1.0e-9);
        assertNotNull(state.visualRect());
        assertNotNull(state.rejection());
        assertEquals(RejectionReason.overlap, state.rejection().reason());
        assertEquals("blocker", state.rejection().blockerId());

        // past linger + fade the element is truly gone
        for (int i = 0; i < 40; i++) {
            now += dt;
            coordinator.frame(InworldCoordinator.FrameInput.of(width, height, now, dt));
        }
        assertEquals(
            VisibilityTracker.Phase.hidden,
            coordinator.lastResult().orElseThrow().elementState("victim").phase()
        );
    }

    @Test
    void frameEndsCommittedEvenWithOneRound() {
        InworldCoordinator.Config config = new InworldCoordinator.Config(
            0.2,
            1,
            12,
            48,
            24,
            3,
            8,
            0.3,
            900,
            30,
            VisibilityTracker.Config.of(0.15, 0.25, 0.25),
            SwitchGate.Config.of(16.0, 1, 2.0, 0)
        );
        InworldCoordinator coordinator = new InworldCoordinator(config);
        TestElement blocker = TestElement.arbitrated("blocker", 200, 150, new Size(400, 300)).withPriority(10);
        TestElement squeezed = TestElement.arbitrated("squeezed", 200, 150, new Size(100, 40), new Size(60, 24))
                .withSingleCandidate();
        coordinator.register(blocker);
        coordinator.register(squeezed);

        CoordinationResult result = frame(coordinator, 0);

        // deadline honored: one propose round, then a committed result with a
        // terminal state for every element
        assertEquals(1, squeezed.proposeCount);
        assertNull(result.placementOf("squeezed"));
        assertNotNull(result.elementState("squeezed").rejection());
        assertNotNull(result.placementOf("blocker"));
        assertEquals(2, result.elementStates().size());
    }

    @Test
    void budgetFeedbackDrivesProactiveDegradation() {
        // a top exclusion makes the work area 300x50: the full form does not
        // fit area-wise, the compact one does
        Rect topStrip = new Rect(0, 0, 300, 150);
        InworldCoordinator proactive = InworldCoordinator.withDefaults();
        TestElement aware = TestElement.arbitrated("aware", 150, 175, new Size(160, 100), new Size(80, 40));
        aware.budgetAware = true;
        proactive.register(aware);
        CoordinationResult proactively = proactive.frame(InworldCoordinator.FrameInput.of(300, 200, 0, dt, topStrip));

        InworldPlacement placement = proactively.placementOf("aware");
        assertNotNull(placement);
        assertEquals(1, placement.variant().level(), "the budget said full cannot fit");
        // never rejected: the very first proposal was already compact
        assertEquals(List.of(1), levels(aware.usedVariants));
        assertNull(aware.lastRejectionSeen);
        assertNull(proactively.elementState("aware").rejection());

        // the reactive control: without budget awareness the element learns
        // the same tier the hard way — out-of-bounds rejection first
        InworldCoordinator reactive = InworldCoordinator.withDefaults();
        TestElement blind = TestElement.arbitrated("blind", 150, 175, new Size(160, 100), new Size(80, 40));
        reactive.register(blind);
        CoordinationResult reactively = reactive.frame(InworldCoordinator.FrameInput.of(300, 200, 0, dt, topStrip));

        assertEquals(1, reactively.placementOf("blind").variant().level());
        assertEquals(List.of(0, 1), levels(blind.usedVariants));
        assertEquals(RejectionReason.outOfBounds, blind.lastRejectionSeen.reason());
    }

    @Test
    void selfManagedElementsRegisterOccupancyAndRejectConflicts() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement owner = TestElement.selfManaged("owner", 100, 100, new Size(80, 40)).withPriority(10);
        TestElement avoider = TestElement.arbitrated("avoider", 100, 100, new Size(100, 40)).withPriority(5);
        TestElement intruder = TestElement.selfManaged("intruder", 100, 100, new Size(80, 40)).withPriority(5);
        coordinator.register(owner);
        coordinator.register(avoider);
        coordinator.register(intruder);

        CoordinationResult result = frame(coordinator, 0);

        // occupancy registered: the owner sits where it wants
        assertEquals(new FloatRect(60, 80, 80, 40), result.placementOf("owner").screenRect());
        // and the arbitrated element had to route around it
        FloatRect avoiderRect = result.placementOf("avoider").screenRect();
        assertTrue(
            avoiderRect.intersection(result.placementOf("owner").screenRect()).area() == 0,
            "the arbitrated element must avoid the self-managed rect"
        );
        assertTrue(avoiderRect.x() > 60);

        // while a conflicting self-managed element is rejected — once, no
        // renegotiation, with the blocker named
        assertNull(result.placementOf("intruder"));
        assertEquals(1, intruder.proposeCount);
        CoordinationResult.ElementState intruderState = result.elementState("intruder");
        assertEquals(RejectionReason.overlap, intruderState.rejection().reason());
        assertEquals("owner", intruderState.rejection().blockerId());
    }

    @Test
    void rejectionsCarryAFreeRectSuggestionWhenSpaceRemains() {
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        TestElement band = TestElement.arbitrated("band", 200, 150, new Size(400, 60)).withPriority(10);
        TestElement squeezed = TestElement
                .arbitrated("squeezed", 200, 150, new Size(100, 40), new Size(60, 24), new Size(20, 12))
                .withSingleCandidate();
        coordinator.register(band);
        coordinator.register(squeezed);

        CoordinationResult result = frame(coordinator, 0);

        assertNull(result.placementOf("squeezed"));
        Rect suggestion = result.elementState("squeezed").rejection().suggestedRect();
        assertNotNull(suggestion, "the screen has room above and below the band");
        assertEquals(20, suggestion.width());
        assertEquals(12, suggestion.height());
        assertTrue(suggestion.y() + suggestion.height() <= 120 || suggestion.y() >= 180, "outside the band");
    }

    private static List<Integer> levels(List<InworldVariant> variants) {
        return variants.stream().map(InworldVariant::level).toList();
    }

    private static double advance(InworldCoordinator coordinator, double now, int frames) {
        for (int i = 0; i < frames; i++) {
            now += dt;
            coordinator.frame(InworldCoordinator.FrameInput.of(width, height, now, dt));
        }
        return now;
    }
}
