package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.VisibilityTracker;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.LodTier;
import org.junit.jupiter.api.Test;

import java.util.Objects;

import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The ContentTier↔LodTier bridge: the full mapping table pinned, and the
 * coordinator's degradation result surfaced through
 * {@link CoordinationResult.ElementState#lodTier()}.
 */
class LodTierBridgeTest {

    // region the pinned mapping table

    @Test
    void everyContentTierMapsToItsPinnedLodTier() {
        assertSame(LodTier.full, CoordinationResult.ElementState.lodTierOf(ContentTier.full));
        assertSame(LodTier.compact, CoordinationResult.ElementState.lodTierOf(ContentTier.compact));
        assertSame(LodTier.compact, CoordinationResult.ElementState.lodTierOf(ContentTier.labelOnly));
        assertSame(LodTier.icon, CoordinationResult.ElementState.lodTierOf(ContentTier.iconOnly));
        assertSame(LodTier.icon, CoordinationResult.ElementState.lodTierOf(ContentTier.pip));
        assertSame(LodTier.icon, CoordinationResult.ElementState.lodTierOf(ContentTier.directionalOnly));
    }

    @Test
    void theBridgeNeverInvertsTheContentDegradeOrder() {
        ContentTier[] order = ContentTier.values();
        for (int i = 1; i < order.length; i++) {
            LodTier weaker = CoordinationResult.ElementState.lodTierOf(order[i]);
            LodTier stronger = CoordinationResult.ElementState.lodTierOf(order[i - 1]);
            assertTrue(
                stronger.ordinal() <= weaker.ordinal(),
                "the bridge must not invert the degrade order: " + stronger + " → " + weaker
            );
        }
    }

    // endregion

    // region ElementState exposure

    @Test
    void aStateWithoutPlacementIsHidden() {
        CoordinationResult.ElementState state = new CoordinationResult.ElementState(
            "e",
            VisibilityTracker.Phase.hidden,
            0.0,
            null,
            null,
            null
        );
        assertSame(LodTier.hidden, state.lodTier());
    }

    @Test
    void theStrongestRungExposesFullAndDegradationExposesTheGrantedRungsTier() {
        // a single-candidate element whose anchor sits below an interior
        // exclusion covering most of the screen: only the smallest rung (the
        // 48×24 pip) fits in the remaining bottom strip, so the ladder walks
        // from the 120×40 full rung down to it, and the state's LOD tier
        // follows the granted rung
        TestElement element = TestElement.arbitrated("e", 24, 284, new Size(120, 40)).withLadder(
            TestElement
                    .ladder(new Size(120, 40), new Size(100, 36), new Size(80, 32), new Size(64, 28), new Size(48, 24))
        ).withSingleCandidate();
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        coordinator.register(element);

        // a free screen: the strongest rung is granted as-is
        CoordinationResult open = coordinator.frame(InworldCoordinator.FrameInput.of(400, 300, 0.0, 1.0 / 60.0));
        assertSame(ContentTier.full, Objects.requireNonNull(open.placementOf("e")).variant().contentTier());
        assertSame(LodTier.full, Objects.requireNonNull(open.elementState("e")).lodTier());

        // the interior exclusion leaves only a 24px bottom strip the pip rung
        // fits into (interior, so it never becomes a strut): every bigger
        // rung overlaps it and degrades away
        Rect interior = new Rect(8, 8, 384, 264);
        CoordinationResult squeezed = coordinator
                .frame(InworldCoordinator.FrameInput.of(400, 300, 1.0, 1.0 / 60.0, interior));
        assertSame(ContentTier.pip, Objects.requireNonNull(squeezed.placementOf("e")).variant().contentTier());
        assertSame(LodTier.icon, Objects.requireNonNull(squeezed.elementState("e")).lodTier());
    }

    @Test
    void aLingeringElementReportsTheTierItFadesOutFrom() {
        TestElement element = TestElement.arbitrated("e", 24, 284, new Size(120, 40))
                .withLadder(TestElement.ladder(new Size(120, 40), new Size(100, 36))).withSingleCandidate();
        InworldCoordinator coordinator = InworldCoordinator.withDefaults();
        coordinator.register(element);
        coordinator.frame(InworldCoordinator.FrameInput.of(400, 300, 0.0, 1.0 / 60.0));

        // the anchor goes away: retraction enters linger, the placement is
        // retained and so is its tier
        element.anchorValid = false;
        CoordinationResult lingering = coordinator.frame(InworldCoordinator.FrameInput.of(400, 300, 1.0, 1.0 / 60.0));
        assertSame(VisibilityTracker.Phase.lingering, Objects.requireNonNull(lingering.elementState("e")).phase());
        assertSame(LodTier.full, lingering.elementState("e").lodTier());
    }

    // endregion
}
