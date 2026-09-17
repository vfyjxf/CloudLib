package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import java.util.Objects;

/**
 * The seven facets as one immutable bundle — the shape a profile presets.
 * Construction runs the {@link FacetRules} validation table, so a bundled
 * preset is legal by construction (profiles validate once, at class init).
 *
 * @param anchor the anchor facet
 * @param orientation the orientation facet
 * @param spaces the space facet
 * @param avoidance the avoidance facet
 * @param stability the stability facet
 * @param degrade the degrade facet
 * @param group the group facet
 */
public record ElementFacets(
        AnchorFacet anchor,
        OrientationFacet orientation,
        SpaceFacet spaces,
        AvoidanceFacet avoidance,
        StabilityFacet stability,
        DegradeFacet degrade,
        GroupFacet group) {

    public ElementFacets {
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(orientation, "orientation");
        Objects.requireNonNull(spaces, "spaces");
        Objects.requireNonNull(avoidance, "avoidance");
        Objects.requireNonNull(stability, "stability");
        Objects.requireNonNull(degrade, "degrade");
        Objects.requireNonNull(group, "group");
    }

    /**
     * A validated bundle. The profile binding participates in validation
     * (rules 4 and 6).
     *
     * @throws IllegalArgumentException on an illegal facet combination
     */
    public static ElementFacets of(InworldProfile profile, ElementFacets facets) {
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(facets, "facets");
        FacetRules.validate(
                facets.anchor, facets.orientation, facets.spaces, facets.avoidance, facets.group, profile.algorithm());
        return facets;
    }
}
