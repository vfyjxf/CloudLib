package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.AvoidanceClass;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * A declarative element (§7 plan B): one UI surface's layout behavior as
 * seven immutable facets plus the closed {@link InworldProfile} that binds
 * the algorithm combination. The {@code PipelineAssembler} turns a spec
 * into an element the {@code InworldCoordinator} can drive; the pipeline
 * stages' behavior is decided by the facets and the profile.
 * <p>
 * Construction runs the {@link FacetRules} validation table — illegal facet
 * combinations are rejected here, not discovered mid-frame. Specs are built
 * from a profile preset ({@link #from} / {@link #of}) and adjusted with the
 * {@code with}-methods, each of which revalidates. The escape hatch is
 * {@link #custom(InworldLayouter)}: an embedded layouter whose propose
 * replaces the element-side stages for scenarios the facets cannot
 * express.
 * <p>
 * The {@code worldOnly} bit opts the element out of projection: the
 * coordinator bypasses screen arbitration entirely (rule 7 requires the
 * world representation such an element needs — a custom layouter or a world
 * anchor).
 * <p>
 * The optional {@link ZoneFacet} (Z2) declares the visual-zone placement
 * path: a spec with one binds the zone strategies instead of the profile's
 * defaults, and its ranker scores against the previous frame's committed
 * layout. The default is null — no zone declaration, the exact pre-zone
 * pipeline everywhere.
 *
 * @param id the element's unique id within its coordinator, non-empty
 * @param profile the closed profile binding the algorithm combination
 * @param anchor the anchor facet
 * @param orientation the orientation facet
 * @param spaces the space facet
 * @param avoidance the avoidance facet
 * @param stability the stability facet
 * @param degrade the degrade facet
 * @param group the group facet
 * @param zone the zone facet, or null when the element does not declare the
 *        zone path
 * @param custom the escape-hatch layouter, or null
 * @param worldOnly whether the element lives purely in world space: no
 *        screen projection, no screen-space coordination (default false)
 * @param avoidanceClass the element's yield declaration — rigid places
 *        itself directly and blocks nobody; standard is the full
 *        arbitration participation (default standard)
 */
public record ElementSpec(
    String id,
    InworldProfile profile,
    AnchorFacet anchor,
    OrientationFacet orientation,
    SpaceFacet spaces,
    AvoidanceFacet avoidance,
    StabilityFacet stability,
    DegradeFacet degrade,
    GroupFacet group,
    @Nullable ZoneFacet zone,
    @Nullable InworldLayouter custom,
    boolean worldOnly,
    AvoidanceClass avoidanceClass
) {

    /**
     * The pre-zone constructor: a spec without a zone declaration, identical
     * to passing a null zone facet.
     */
    public ElementSpec(
        String id,
        InworldProfile profile,
        AnchorFacet anchor,
        OrientationFacet orientation,
        SpaceFacet spaces,
        AvoidanceFacet avoidance,
        StabilityFacet stability,
        DegradeFacet degrade,
        GroupFacet group,
        @Nullable InworldLayouter custom,
        boolean worldOnly
    ) {
        this(id, profile, anchor, orientation, spaces, avoidance, stability, degrade, group, null, custom, worldOnly);
    }

    /**
     * The pre-yield constructor: a spec declaring the standard avoidance
     * class, identical to passing {@link AvoidanceClass#standard}.
     */
    public ElementSpec(
        String id,
        InworldProfile profile,
        AnchorFacet anchor,
        OrientationFacet orientation,
        SpaceFacet spaces,
        AvoidanceFacet avoidance,
        StabilityFacet stability,
        DegradeFacet degrade,
        GroupFacet group,
        @Nullable ZoneFacet zone,
        @Nullable InworldLayouter custom,
        boolean worldOnly
    ) {
        this(
            id,
            profile,
            anchor,
            orientation,
            spaces,
            avoidance,
            stability,
            degrade,
            group,
            zone,
            custom,
            worldOnly,
            AvoidanceClass.standard
        );
    }

    public ElementSpec {
        if (id == null || id.isEmpty()) {
            throw new IllegalArgumentException("id must not be empty");
        }
        Objects.requireNonNull(profile, "profile");
        Objects.requireNonNull(anchor, "anchor");
        Objects.requireNonNull(orientation, "orientation");
        Objects.requireNonNull(spaces, "spaces");
        Objects.requireNonNull(avoidance, "avoidance");
        Objects.requireNonNull(stability, "stability");
        Objects.requireNonNull(degrade, "degrade");
        Objects.requireNonNull(group, "group");
        FacetRules.validate(anchor, orientation, spaces, avoidance, group, profile.algorithm());
        FacetRules.validateWorldOnly(worldOnly, anchor, custom != null);
        FacetRules.validateZone(zone, anchor, spaces, profile.algorithm(), worldOnly);
        FacetRules.validateAvoidanceClass(avoidanceClass, worldOnly, zone);
    }

    /** A spec from the profile's facet preset (its default anchor) under {@code id}. */
    public static ElementSpec from(InworldProfile profile, String id) {
        return of(profile, id, profile.facets().anchor());
    }

    /** A spec from the profile's preset with {@code anchor} substituted, under {@code id}. */
    public static ElementSpec of(InworldProfile profile, String id, AnchorFacet anchor) {
        ElementFacets facets = profile.facets();
        return new ElementSpec(
            id,
            profile,
            anchor,
            facets.orientation(),
            facets.spaces(),
            facets.avoidance(),
            facets.stability(),
            facets.degrade(),
            facets.group(),
            null,
            null,
            false,
            AvoidanceClass.standard
        );
    }

    /** The escape hatch: this spec with {@code layouter} embedded. */
    public ElementSpec custom(InworldLayouter layouter) {
        Objects.requireNonNull(layouter, "layouter");
        return new ElementSpec(
            id,
            profile,
            anchor,
            orientation,
            spaces,
            avoidance,
            stability,
            degrade,
            group,
            zone,
            layouter,
            worldOnly,
            avoidanceClass
        );
    }

    /**
     * Swaps the anchor facet. When the new anchor's family cannot carry the
     * current orientation (a camera-tracked anchor cannot billboard), the
     * orientation coheres to the new anchor family's default — the anchor
     * choice implies the orientation family. Cross-family re-anchoring
     * swaps the space layers first (an intermediate spec still claiming the
     * {@code worldAnchored} layer under a screen anchor violates rule 2);
     * directly pairing an incompatible anchor and orientation through the
     * constructor still throws (rule 1).
     */
    public ElementSpec withAnchor(AnchorFacet newAnchor) {
        OrientationFacet next = orientation;
        if (!FacetRules.orientationCompatible(newAnchor, next)) {
            next = FacetRules.coheredOrientation(newAnchor);
        }
        return new ElementSpec(
            id,
            profile,
            newAnchor,
            next,
            spaces,
            avoidance,
            stability,
            degrade,
            group,
            zone,
            custom,
            worldOnly,
            avoidanceClass
        );
    }

    /** Swaps the orientation facet; an incompatible anchor/orientation pair throws (rule 1). */
    public ElementSpec withOrientation(OrientationFacet newOrientation) {
        return new ElementSpec(
            id,
            profile,
            anchor,
            newOrientation,
            spaces,
            avoidance,
            stability,
            degrade,
            group,
            zone,
            custom,
            worldOnly,
            avoidanceClass
        );
    }

    /** Swaps the space facet. */
    public ElementSpec withSpaces(SpaceFacet newSpaces) {
        return new ElementSpec(
            id,
            profile,
            anchor,
            orientation,
            newSpaces,
            avoidance,
            stability,
            degrade,
            group,
            zone,
            custom,
            worldOnly,
            avoidanceClass
        );
    }

    /** Swaps the avoidance facet. */
    public ElementSpec withAvoidance(AvoidanceFacet newAvoidance) {
        return new ElementSpec(
            id,
            profile,
            anchor,
            orientation,
            spaces,
            newAvoidance,
            stability,
            degrade,
            group,
            zone,
            custom,
            worldOnly,
            avoidanceClass
        );
    }

    /** Swaps the stability facet. */
    public ElementSpec withStability(StabilityFacet newStability) {
        return new ElementSpec(
            id,
            profile,
            anchor,
            orientation,
            spaces,
            avoidance,
            newStability,
            degrade,
            group,
            zone,
            custom,
            worldOnly,
            avoidanceClass
        );
    }

    /** Swaps the degrade facet. */
    public ElementSpec withDegrade(DegradeFacet newDegrade) {
        return new ElementSpec(
            id,
            profile,
            anchor,
            orientation,
            spaces,
            avoidance,
            stability,
            newDegrade,
            group,
            zone,
            custom,
            worldOnly,
            avoidanceClass
        );
    }

    /** Swaps the group facet. */
    public ElementSpec withGroup(GroupFacet newGroup) {
        return new ElementSpec(
            id,
            profile,
            anchor,
            orientation,
            spaces,
            avoidance,
            stability,
            degrade,
            newGroup,
            zone,
            custom,
            worldOnly,
            avoidanceClass
        );
    }

    /**
     * Declares the zone facet (rules 8–10 validate the combination); a null
     * facet removes the declaration and restores the default pipeline.
     */
    public ElementSpec withZone(@Nullable ZoneFacet newZone) {
        return new ElementSpec(
            id,
            profile,
            anchor,
            orientation,
            spaces,
            avoidance,
            stability,
            degrade,
            group,
            newZone,
            custom,
            worldOnly,
            avoidanceClass
        );
    }

    /**
     * Declares this element world-only (rule 7: the capability needs a custom
     * layouter or a world anchor).
     */
    public ElementSpec withWorldOnly() {
        return withWorldOnly(true);
    }

    /** Sets the world-only capability bit explicitly. */
    public ElementSpec withWorldOnly(boolean newWorldOnly) {
        return new ElementSpec(
            id,
            profile,
            anchor,
            orientation,
            spaces,
            avoidance,
            stability,
            degrade,
            group,
            zone,
            custom,
            newWorldOnly,
            avoidanceClass
        );
    }

    /**
     * Declares the element's yield class (rule 11 validates the combination):
     * {@link AvoidanceClass#rigid} places the element directly and makes its
     * rect block nobody; {@link AvoidanceClass#standard} is the default.
     */
    public ElementSpec withAvoidanceClass(AvoidanceClass newAvoidanceClass) {
        return new ElementSpec(
            id,
            profile,
            anchor,
            orientation,
            spaces,
            avoidance,
            stability,
            degrade,
            group,
            zone,
            custom,
            worldOnly,
            newAvoidanceClass
        );
    }
}
