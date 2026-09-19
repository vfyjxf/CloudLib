package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.AvoidanceClass;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.ElementProposal;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.SpaceKind;
import dev.vfyjxf.cloudlib.api.ui.inworld.group.ClusterToRepresentative;
import dev.vfyjxf.cloudlib.api.ui.inworld.group.InworldGroup;
import dev.vfyjxf.cloudlib.api.ui.inworld.group.OrbitAroundAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpaceMask;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpacePolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.SwitchGate;
import dev.vfyjxf.cloudlib.api.ui.inworld.stability.VisibilityTracker;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The illegal-facet-combination table (see {@link FacetRules}): every rule
 * rejects at construction — the behavior space stays enumerable.
 */
class ElementSpecValidationTest {

    private static final InworldGroup group = new InworldGroup("nimbus", "test", "g");

    // region rule 1: anchor ↔ orientation

    @Test
    void blockFaceOrientationNeedsABlockFaceAnchor() {
        assertThrows(
                IllegalArgumentException.class,
                () -> rawSpec(InworldProfile.facePanel, AnchorFacet.entity("cow"), OrientationFacet.blockFace()));
    }

    @Test
    void groundParallelNeedsAPositionOrBlockFaceAnchor() {
        assertThrows(
                IllegalArgumentException.class,
                () -> rawSpec(
                        InworldProfile.ground, AnchorFacet.cameraTracked(0.5, 0.5), OrientationFacet.groundParallel()));
        assertThrows(
                IllegalArgumentException.class,
                () -> rawSpec(InworldProfile.ground, AnchorFacet.entity("cow"), OrientationFacet.groundParallel()));
    }

    @Test
    void billboardsNeedWorldAnchors() {
        assertThrows(
                IllegalArgumentException.class,
                () -> rawSpec(InworldProfile.nameplate, AnchorFacet.none(), OrientationFacet.cameraBillboard()));
        assertThrows(
                IllegalArgumentException.class,
                () -> rawSpec(InworldProfile.hologram, AnchorFacet.none(), OrientationFacet.yawBillboard()));
    }

    @Test
    void screenOrientationNeedsAScreenAnchorAndViceVersa() {
        assertThrows(
                IllegalArgumentException.class,
                () -> rawSpec(InworldProfile.dock, AnchorFacet.entity("cow"), OrientationFacet.screen()));
        assertThrows(IllegalArgumentException.class, () -> ElementSpec.from(InworldProfile.nameplate, "x")
                .withOrientation(OrientationFacet.screen()));
    }

    @Test
    void anchorWitherCoheresTheOrientationFamily() {
        ElementSpec cohered =
                ElementSpec.from(InworldProfile.waypoint, "x").withAnchor(AnchorFacet.cameraTracked(0.5, 0.5));
        assertEquals(OrientationFacet.Mode.screen, cohered.orientation().mode());
        ElementSpec back = cohered.withAnchor(AnchorFacet.position(1, 2, 3));
        assertEquals(OrientationFacet.Mode.cameraBillboard, back.orientation().mode());
        ElementSpec face = ElementSpec.from(InworldProfile.dock, "y")
                .withAnchor(AnchorFacet.blockFace(1, 2, 3, AnchorFacet.Normal.up));
        assertEquals(OrientationFacet.Mode.blockFace, face.orientation().mode());
    }

    // endregion

    // region rules 2–4: layers and ghosts

    @Test
    void worldLayerNeedsAWorldAnchor() {
        assertThrows(IllegalArgumentException.class, () -> ElementSpec.from(InworldProfile.dock, "x")
                .withSpaces(new SpaceFacet(
                        Set.of(SpaceMask.worldAnchored, SpaceMask.screenPanel), SpacePolicy.passive, 6)));
    }

    @Test
    void ghostsNeitherDodgeNorAreDodged() {
        assertThrows(IllegalArgumentException.class, () -> ElementSpec.from(InworldProfile.transientUi, "x")
                .withAvoidance(AvoidanceFacet.of(Set.of(SpaceMask.screenPanel))));
        assertThrows(IllegalArgumentException.class, () -> ElementSpec.from(InworldProfile.transientUi, "x")
                .withAvoidance(new AvoidanceFacet(Set.of(), Set.of(), true)));
    }

    @Test
    void ghostsOnlyUseAnchoredCandidates() {
        assertThrows(IllegalArgumentException.class, () -> ElementSpec.from(InworldProfile.dock, "x")
                .withSpaces(new SpaceFacet(Set.of(SpaceMask.screenPanel), SpacePolicy.ghost, 6))
                .withAvoidance(AvoidanceFacet.none()));
    }

    // endregion

    // region rules 5–6: grouping

    @Test
    void orbitAndClusterStrategiesNeedWorldAnchors() {
        assertThrows(IllegalArgumentException.class, () -> ElementSpec.from(InworldProfile.dock, "x")
                .withGroup(GroupFacet.of(group, OrbitAroundAnchor.of())));
        assertThrows(IllegalArgumentException.class, () -> ElementSpec.from(InworldProfile.dock, "x")
                .withGroup(GroupFacet.of(group, ClusterToRepresentative.of())));
    }

    @Test
    void clusterStrategyNeedsAClusterBindingProfile() {
        assertThrows(IllegalArgumentException.class, () -> ElementSpec.from(InworldProfile.facePanel, "x")
                .withGroup(GroupFacet.of(group, ClusterToRepresentative.of())));
        assertDoesNotThrow(() -> ElementSpec.from(InworldProfile.nameplate, "x")
                .withGroup(GroupFacet.of(group, ClusterToRepresentative.of())));
    }

    // endregion

    // region rule 7: world-only capability

    @Test
    void worldOnlyNeedsACustomLayouterOrAWorldAnchor() {
        // a screen anchor with no custom layouter has nothing world-only to place
        assertThrows(IllegalArgumentException.class, () -> ElementSpec.from(InworldProfile.dock, "x")
                .withWorldOnly());
        assertThrows(IllegalArgumentException.class, () -> ElementSpec.from(InworldProfile.dock, "x")
                .withAnchor(AnchorFacet.none())
                .withWorldOnly());
        // a world anchor carries the world box
        assertDoesNotThrow(() -> ElementSpec.from(InworldProfile.nameplate, "x").withWorldOnly());
        // a custom layouter proposes world candidates on its own
        assertDoesNotThrow(() ->
                ElementSpec.from(InworldProfile.dock, "x").custom(noopLayouter).withWorldOnly());
        // flipping the bit back off revalidates cleanly
        assertDoesNotThrow(() -> ElementSpec.from(InworldProfile.dock, "x").withWorldOnly(false));
    }

    // endregion

    // region rule 11: rigid yield

    @Test
    void rigidYieldNeedsTheScreenPlaneNotTheWorldOnlyCapability() {
        // rigid is declarable on an ordinary screen-plane spec
        assertDoesNotThrow(() -> ElementSpec.from(InworldProfile.dock, "x").withAvoidanceClass(AvoidanceClass.rigid));
        assertEquals(
                AvoidanceClass.standard,
                ElementSpec.from(InworldProfile.dock, "x").avoidanceClass(),
                "the default is the standard participation");
        // the world-only capability already leaves screen arbitration — rigid
        // on top has no screen rect to place
        assertThrows(IllegalArgumentException.class, () -> ElementSpec.from(InworldProfile.nameplate, "x")
                .withWorldOnly()
                .withAvoidanceClass(AvoidanceClass.rigid));
    }

    @Test
    void rigidYieldCannotDeclareZone() {
        // the zone path binds candidate ranking the rigid flow never runs —
        // the declaration would be silently discarded
        assertThrows(IllegalArgumentException.class, () -> ElementSpec.from(InworldProfile.facePanel, "z")
                .withZone(ZoneFacet.of())
                .withAvoidanceClass(AvoidanceClass.rigid));
        // and back to standard the very same declaration is legal
        assertDoesNotThrow(() -> ElementSpec.from(InworldProfile.facePanel, "z")
                .withZone(ZoneFacet.of())
                .withAvoidanceClass(AvoidanceClass.standard));
    }

    // endregion

    // region facet-level validation

    @Test
    void facetValueTypesRejectMalformedInput() {
        assertThrows(IllegalArgumentException.class, () -> AnchorFacet.cameraTracked(1.5, 0.5));
        assertThrows(IllegalArgumentException.class, () -> AnchorFacet.entity(""));
        assertThrows(
                IllegalArgumentException.class,
                () -> new AnchorFacet.BlockFace(0, 0, 0, AnchorFacet.Normal.north, -1.0));
        assertThrows(IllegalArgumentException.class, () -> new SpaceFacet(Set.of(), SpacePolicy.active, 0));
        assertThrows(
                IllegalArgumentException.class,
                () -> new StabilityFacet(
                        SwitchGate.Config.of(16.0, 1, 2.0, 0),
                        0.0,
                        900.0,
                        VisibilityTracker.Config.of(0.15, 0.25, 0.25),
                        true));
    }

    @Test
    void emptyIdsAreRejected() {
        assertThrows(IllegalArgumentException.class, () -> ElementSpec.from(InworldProfile.nameplate, ""));
        assertThrows(IllegalArgumentException.class, () -> ElementSpec.from(InworldProfile.nameplate, null));
    }

    // endregion

    /** The canonical constructor with a substituted anchor/orientation pair. */
    private static ElementSpec rawSpec(InworldProfile profile, AnchorFacet anchor, OrientationFacet orientation) {
        ElementFacets facets = profile.facets();
        return new ElementSpec(
                "x",
                profile,
                anchor,
                orientation,
                facets.spaces(),
                facets.avoidance(),
                facets.stability(),
                facets.degrade(),
                facets.group(),
                null,
                false);
    }

    /** A never-invoked layouter: rule 7 only checks for its presence. */
    private static final InworldLayouter noopLayouter = new InworldLayouter() {

        @Override
        public SpaceReservation reserve(InworldLayoutContext ctx) {
            return SpaceReservation.arbitrated(SpaceKind.world);
        }

        @Override
        public ElementProposal propose(InworldLayoutContext ctx) {
            return ElementProposal.retract(ctx.variant());
        }

        @Override
        public void arbitrated(InworldPlacement placement, Feedback feedback) {}
    };
}
