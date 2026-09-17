package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import org.junit.jupiter.api.Test;

import java.util.List;
import java.util.Optional;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertFalse;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class AlgorithmProfileTest {

    @Test
    void theProfileSetIsClosedAndPlanned() {
        assertEquals(
                List.of(
                        AlgorithmProfile.nameplate,
                        AlgorithmProfile.dock,
                        AlgorithmProfile.facePanel,
                        AlgorithmProfile.waypoint,
                        AlgorithmProfile.transientUi,
                        AlgorithmProfile.excentric,
                        AlgorithmProfile.orbit),
                List.of(AlgorithmProfile.values()));
    }

    @Test
    void nameplateCarriesTheWowNameplateBaseline() {
        AlgorithmProfile.Params params = AlgorithmProfile.nameplate.params();

        assertEquals(0.025, params.motionSpeed(), 1.0e-12);
        assertEquals(0.8, params.overlapH(), 1.0e-12);
        assertEquals(1.1, params.overlapV(), 1.0e-12);
        assertEquals(0.4, params.occludedAlphaMult(), 1.0e-12);
    }

    @Test
    void eachProfileBindsItsAlgorithmCombination() {
        assertEquals(AlgorithmProfile.Placement.orbitRing, AlgorithmProfile.nameplate.placement());
        assertEquals(AlgorithmProfile.LeaderStrategy.adaptive, AlgorithmProfile.nameplate.leaderStrategy());
        assertEquals(AlgorithmProfile.OffscreenStrategy.angleEncoder, AlgorithmProfile.nameplate.offscreenStrategy());
        assertTrue(AlgorithmProfile.nameplate.clustersElements());

        assertEquals(AlgorithmProfile.Placement.dockCursor, AlgorithmProfile.dock.placement());
        assertEquals(AlgorithmProfile.LeaderStrategy.none, AlgorithmProfile.dock.leaderStrategy());
        assertFalse(AlgorithmProfile.dock.clustersElements());

        assertEquals(AlgorithmProfile.Placement.anchoredQuad, AlgorithmProfile.facePanel.placement());

        assertEquals(AlgorithmProfile.OffscreenStrategy.angleEncoder, AlgorithmProfile.waypoint.offscreenStrategy());
        assertEquals(AlgorithmProfile.Placement.none, AlgorithmProfile.waypoint.placement());

        assertEquals(AlgorithmProfile.Placement.none, AlgorithmProfile.transientUi.placement());
        assertEquals(AlgorithmProfile.LeaderStrategy.none, AlgorithmProfile.transientUi.leaderStrategy());
        assertFalse(AlgorithmProfile.transientUi.clustersElements());

        assertEquals(AlgorithmProfile.Placement.excentricColumn, AlgorithmProfile.excentric.placement());
        assertEquals(AlgorithmProfile.LeaderStrategy.straightOnly, AlgorithmProfile.excentric.leaderStrategy());

        assertEquals(AlgorithmProfile.Placement.orbitRing, AlgorithmProfile.orbit.placement());
        assertTrue(AlgorithmProfile.orbit.clustersElements());
    }

    @Test
    void profilesExposeValidConfigsForTheirBoundAlgorithms() {
        for (AlgorithmProfile profile : AlgorithmProfile.values()) {
            AlgorithmProfile.Params params = profile.params();

            assertEquals(params.switchPenalty(), params.slotAssignerCosts().switchPenalty(), 0.0);
            assertEquals(params.incumbentDiscount(), params.slotAssignerCosts().incumbentDiscount(), 0.0);
            assertEquals(params.clusterAlpha(), params.clustererConfig().alpha(), 0.0);
            assertEquals(params.clusterMergeRadius(), params.clustererConfig().mergeRadius(), 0.0);
            assertEquals(params.leaderBand(), params.leaderRouterConfig().band(), 0.0);
            assertEquals(params.leaderDwellEpochs(), params.leaderRouterConfig().dwellEpochs());
            assertEquals(params.encoderLambda(), params.angleEncoderConfig().lambda(), 0.0);
            assertEquals(params.encoderEdgeBand(), params.angleEncoderConfig().edgeBand(), 0.0);
            assertTrue(params.recourseBudget() >= 0);
            assertTrue(params.motionSpeed() > 0 && params.motionSpeed() <= 1);
        }
    }

    @Test
    void byIdRoundTripsAndRejectsUnknownNames() {
        for (AlgorithmProfile profile : AlgorithmProfile.values()) {
            assertEquals(Optional.of(profile), AlgorithmProfile.byId(profile.toString()));
        }
        assertEquals(Optional.empty(), AlgorithmProfile.byId("doesNotExist"));
        assertEquals(Optional.empty(), AlgorithmProfile.byId("NAMEPLATE"));
    }

    @Test
    void paramsRejectInvalidValues() {
        assertThrows(
                IllegalArgumentException.class,
                () -> new AlgorithmProfile.Params(
                        2.0, 0.8, 1.1, 0.4, 2, 20.0, 0.15, 0.6, 120.0, 1.0, 2, 8.0, 0.2, 12.0, 4.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AlgorithmProfile.Params(
                        0.025, 0.8, 1.1, 0.4, -1, 20.0, 0.15, 0.6, 120.0, 1.0, 2, 8.0, 0.2, 12.0, 4.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AlgorithmProfile.Params(
                        0.025, 0.8, 1.1, 0.4, 2, 20.0, 1.0, 0.6, 120.0, 1.0, 2, 8.0, 0.2, 12.0, 4.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AlgorithmProfile.Params(
                        0.025, 0.8, 1.1, 0.4, 2, 20.0, 0.15, 1.5, 120.0, 1.0, 2, 8.0, 0.2, 12.0, 4.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AlgorithmProfile.Params(
                        0.025, 0.8, 1.1, 0.4, 2, 20.0, 0.15, 0.6, 120.0, 0.0, 2, 8.0, 0.2, 12.0, 4.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AlgorithmProfile.Params(
                        0.025, 0.8, 1.1, 0.4, 2, 20.0, 0.15, 0.6, 120.0, 1.0, 0, 8.0, 0.2, 12.0, 4.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AlgorithmProfile.Params(
                        0.025, 0.8, 1.1, 0.4, 2, 20.0, 0.15, 0.6, 120.0, 1.0, 2, 0, 0.2, 12.0, 4.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AlgorithmProfile.Params(
                        0.025, 0.8, 1.1, 0.4, 2, 20.0, 0.15, 0.6, 120.0, 1.0, 2, 8.0, 0.0, 12.0, 4.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AlgorithmProfile.Params(
                        0.025, 0.8, 1.1, 0.4, 2, 20.0, 0.15, 0.6, 120.0, 1.0, 2, 8.0, 0.2, -1, 4.0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AlgorithmProfile.Params(
                        0.025, 0.8, 1.1, 0.4, 2, 20.0, 0.15, 0.6, 120.0, 1.0, 2, 8.0, 0.2, 12.0, -1));
    }
}
