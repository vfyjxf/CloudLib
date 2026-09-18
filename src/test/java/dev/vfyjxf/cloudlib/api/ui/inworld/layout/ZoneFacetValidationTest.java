package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpaceMask;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpacePolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.LodTier;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.VisibilityPolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.ZoneCandidates;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.ZoneModel;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.ZoneWeights;
import org.junit.jupiter.api.Test;

import java.util.Set;

import static org.junit.jupiter.api.Assertions.assertDoesNotThrow;
import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNull;
import static org.junit.jupiter.api.Assertions.assertSame;
import static org.junit.jupiter.api.Assertions.assertThrows;

/**
 * The zone facet's slice of the illegal-facet-combination table (rules 8–10)
 * plus the facet's own field semantics.
 */
class ZoneFacetValidationTest {

    /** A legal zone declaration on the face-panel family: a world anchor and anchored-quad placement. */
    private static ElementSpec zoneSpec(InworldProfile profile) {
        return ElementSpec.from(profile, "z").withZone(ZoneFacet.of());
    }

    // region rule 8: zone participation

    @Test
    void aGhostCannotDeclareZone() {
        // transientUi presets the ghost policy; its placement family (none)
        // and anchor (position) are otherwise zone-legal
        assertThrows(IllegalArgumentException.class, () -> zoneSpec(InworldProfile.transientUi));
        // flipping the policy to passive legalizes the same declaration
        assertDoesNotThrow(() -> ElementSpec.from(InworldProfile.transientUi, "z")
                .withSpaces(new SpaceFacet(Set.of(SpaceMask.worldAnchored), SpacePolicy.passive, 0))
                .withZone(ZoneFacet.of()));
    }

    @Test
    void aWorldOnlyElementCannotDeclareZone() {
        assertThrows(IllegalArgumentException.class, () -> ElementSpec.from(InworldProfile.nameplate, "z")
                .withWorldOnly()
                .withZone(ZoneFacet.of()));
        assertThrows(IllegalArgumentException.class, () -> zoneSpec(InworldProfile.nameplate)
                .withWorldOnly());
    }

    // endregion

    // region rule 9: zone anchor

    @Test
    void aNoAnchorPanelCannotDeclareZone() {
        // spaces swap first (the cross-family convention), then the anchor:
        // the none anchor is legal with the cohered screen orientation
        ElementSpec screenSpec = ElementSpec.from(InworldProfile.facePanel, "z")
                .withSpaces(new SpaceFacet(Set.of(SpaceMask.screenPanel), SpacePolicy.passive, 5))
                .withAnchor(AnchorFacet.none());
        assertEquals(OrientationFacet.Mode.screen, screenSpec.orientation().mode());
        assertThrows(IllegalArgumentException.class, () -> screenSpec.withZone(ZoneFacet.of()));
    }

    @Test
    void theDockCursorFamilyCannotDeclareZone() {
        // the dock profile's candidates scan the screen edge, not the anchor
        assertThrows(IllegalArgumentException.class, () -> zoneSpec(InworldProfile.dock));
        // the same camera-tracked element on an anchor-positioned family
        // (facePanel's anchoredQuad) declares zone freely — the family is the
        // profile's binding, so the fix is picking the anchored profile
        assertDoesNotThrow(() -> ElementSpec.from(InworldProfile.facePanel, "z")
                .withSpaces(new SpaceFacet(Set.of(SpaceMask.screenPanel), SpacePolicy.passive, 1))
                .withDegrade(DegradeFacet.of(SpacePolicy.passive, false, true, new Size(60, 20), new Size(40, 16)))
                .withAnchor(AnchorFacet.cameraTracked(0.1, 0.9))
                .withZone(ZoneFacet.of()));
    }

    @Test
    void worldAnchoredFamiliesDeclareZoneFreely() {
        assertDoesNotThrow(() -> zoneSpec(InworldProfile.facePanel));
        assertDoesNotThrow(() -> zoneSpec(InworldProfile.nameplate));
        assertDoesNotThrow(() -> zoneSpec(InworldProfile.excentric));
        assertDoesNotThrow(() -> zoneSpec(InworldProfile.waypoint));
    }

    // endregion

    // region rule 10: zone entry tier

    @Test
    void theInitialTierMustBeFullOrCompact() {
        for (LodTier tier : LodTier.values()) {
            ZoneFacet facet = new ZoneFacet(null, null, null, null, VisibilityPolicy.fade, tier);
            if (tier == LodTier.full || tier == LodTier.compact) {
                assertSame(
                        tier,
                        ElementSpec.from(InworldProfile.facePanel, "z")
                                .withZone(facet)
                                .zone()
                                .initialTier());
            } else {
                assertThrows(
                        IllegalArgumentException.class,
                        () -> ElementSpec.from(InworldProfile.facePanel, "z").withZone(facet),
                        tier + " is a degrade outcome, not a declaration");
            }
        }
    }

    // endregion

    // region facet fields

    @Test
    void nullKnobsResolveToTheirDefaults() {
        ZoneFacet facet = ZoneFacet.of();
        assertNull(facet.attention());
        assertEquals(ZoneWeights.defaults(), facet.weightsOrDefault());
        assertEquals(ZoneCandidates.Config.defaults(), facet.candidatesConfigOrDefault());
        assertEquals(ZoneModel.Config.defaults(), facet.modelConfigOrDefault());
        assertSame(VisibilityPolicy.fade, facet.visibility());
        assertSame(LodTier.full, facet.initialTier());

        assertThrows(NullPointerException.class, () -> new ZoneFacet(null, null, null, null, null, LodTier.full));
        assertThrows(
                NullPointerException.class, () -> new ZoneFacet(null, null, null, null, VisibilityPolicy.fade, null));
    }

    @Test
    void specsDefaultToNoZoneDeclarationAndWithMethodsCarryIt() {
        for (InworldProfile profile : InworldProfile.values()) {
            assertNull(ElementSpec.from(profile, "x").zone());
        }
        ElementSpec zoned = zoneSpec(InworldProfile.facePanel).withStability(StabilityFacet.fixedBaseline());
        assertEquals(ZoneFacet.of(), zoned.zone(), "the with-methods carry the zone facet");
        assertNull(zoned.withZone(null).zone(), "a null facet removes the declaration");
    }

    // endregion
}
