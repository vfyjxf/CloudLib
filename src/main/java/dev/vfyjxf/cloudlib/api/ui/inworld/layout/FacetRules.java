package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.algorithm.AlgorithmProfile;
import dev.vfyjxf.cloudlib.api.ui.inworld.group.ClusterToRepresentative;
import dev.vfyjxf.cloudlib.api.ui.inworld.group.OrbitAroundAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpacePolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.LodTier;
import org.jetbrains.annotations.Nullable;

/**
 * The illegal-facet-combination table (§7 plan B: the behavior space must be
 * enumerable, so contradictions are rejected at construction — both in
 * {@link ElementFacets} and {@link ElementSpec}):
 * <ol>
 *   <li><strong>anchor ↔ orientation</strong> — {@code blockFace} orientation
 *       needs a block-face anchor; {@code groundParallel} needs a position
 *       or block-face anchor; {@code cameraBillboard}/{@code yawBillboard}
 *       need a world anchor; {@code screen} orientation needs a
 *       camera-tracked or no anchor — and conversely, camera-tracked and
 *       no-anchor elements must be {@code screen}-oriented</li>
 *   <li><strong>layers ↔ anchor</strong> — claiming the {@code worldAnchored}
 *       layer without a world anchor is a contradiction</li>
 *   <li><strong>ghost avoidance</strong> — a ghost dodges nothing, is dodged
 *       by nothing, and ignores exclusions (it participates in nothing at
 *       all)</li>
 *   <li><strong>ghost candidates</strong> — a ghost may only use anchored
 *       single-candidate placement (dock cursors, rings and columns are
 *       occupancy machinery a ghost never registers)</li>
 *   <li><strong>group anchor</strong> — {@code OrbitAroundAnchor} and
 *       {@code ClusterToRepresentative} arrange world things and need a
 *       world anchor</li>
 *   <li><strong>group clustering</strong> — {@code ClusterToRepresentative}
 *       needs a profile whose algorithm binds clustering</li>
 *   <li><strong>world-only capability</strong> — a world-only element must
 *       bring its own world representation: a custom layouter proposing world
 *       candidates, or a world anchor whose frame carries the world box. The
 *       built-in candidate stages project screen rects, so a screen anchor
 *       with no custom layouter has nothing world-only to place</li>
 *   <li><strong>zone participation</strong> — a zone declaration needs
 *       screen-arbitration participation: not a ghost (a ghost participates
 *       in nothing, which the zone cost's overlap/adjacency machinery
 *       contradicts) and not world-only (the zone lattice proposes screen
 *       rects; a world-only element leaves screen arbitration entirely)</li>
 *   <li><strong>zone anchor</strong> — a zone declaration needs a declared
 *       anchor with an anchor-positioned candidate family: the anchor must
 *       not be {@code none} (the lattice docks to a declared anchor, a
 *       no-anchor panel's placement comes from its candidates) and the
 *       profile's placement must not be the dock cursor (its candidates scan
 *       the screen edge — anchor position only picks the edge — so a zone
 *       declaration would silently discard the profile's declared
 *       behavior)</li>
 *   <li><strong>zone entry tier</strong> — the zone facet's initial LOD tier
 *       must be {@code full} or {@code compact}: the content-bearing tiers a
 *       fresh element may enter at. {@code icon} and below are degradation
 *       outcomes the coordinator's ladder owns, and {@code clustered} is the
 *       grouping layer's verdict — neither is a legal declaration</li>
 * </ol>
 * Per-facet field validation (non-empty ids, finite coordinates, positive
 * radii, …) lives in the facet records themselves.
 */
public final class FacetRules {

    private FacetRules() {}

    /**
     * Validates the seven facets plus the profile binding.
     *
     * @throws IllegalArgumentException with the rule number on violation
     */
    public static void validate(
            AnchorFacet anchor,
            OrientationFacet orientation,
            SpaceFacet spaces,
            AvoidanceFacet avoidance,
            GroupFacet group,
            AlgorithmProfile algorithm) {
        rule1(anchor, orientation);
        rule2(anchor, spaces);
        rule3(avoidance, spaces);
        rule4(spaces, algorithm);
        rule5(anchor, group);
        rule6(group, algorithm);
    }

    private static void rule1(AnchorFacet anchor, OrientationFacet orientation) {
        if (!orientationCompatible(anchor, orientation)) {
            throw new IllegalArgumentException(
                    orientation.mode() + " orientation is illegal with a " + anchor.kind() + " anchor (rule 1)");
        }
    }

    /** Whether the anchor/orientation pair is legal (rule 1's predicate). */
    public static boolean orientationCompatible(AnchorFacet anchor, OrientationFacet orientation) {
        return switch (orientation.mode()) {
            case blockFace -> anchor instanceof AnchorFacet.BlockFace;
            case groundParallel -> anchor instanceof AnchorFacet.Position || anchor instanceof AnchorFacet.BlockFace;
            case cameraBillboard, yawBillboard -> anchor.worldAnchored();
            case screen -> !anchor.worldAnchored();
        };
    }

    /**
     * The orientation a freshly re-anchored spec coheres to when its current
     * orientation would be illegal for the new anchor: world anchors fall
     * back to the camera billboard, block faces to the face quad, screen
     * anchors to screen orientation.
     */
    public static OrientationFacet coheredOrientation(AnchorFacet anchor) {
        if (anchor instanceof AnchorFacet.BlockFace) {
            return OrientationFacet.blockFace();
        }
        if (anchor.worldAnchored()) {
            return OrientationFacet.cameraBillboard();
        }
        return OrientationFacet.screen();
    }

    private static void rule2(AnchorFacet anchor, SpaceFacet spaces) {
        if (spaces.claimsWorldLayer() && !anchor.worldAnchored()) {
            throw new IllegalArgumentException(
                    "the worldAnchored layer needs a world anchor: " + anchor.kind() + " (rule 2)");
        }
    }

    private static void rule3(AvoidanceFacet avoidance, SpaceFacet spaces) {
        if (spaces.policy() != SpacePolicy.ghost) {
            return;
        }
        if (!avoidance.avoids().isEmpty() || !avoidance.avoidedBy().isEmpty()) {
            throw new IllegalArgumentException("a ghost dodges nothing and is dodged by nothing (rule 3)");
        }
        if (avoidance.respectsExclusions()) {
            throw new IllegalArgumentException("a ghost ignores exclusion areas (rule 3)");
        }
    }

    private static void rule4(SpaceFacet spaces, AlgorithmProfile algorithm) {
        if (spaces.policy() != SpacePolicy.ghost) {
            return;
        }
        AlgorithmProfile.Placement placement = algorithm.placement();
        if (placement != AlgorithmProfile.Placement.none && placement != AlgorithmProfile.Placement.anchoredQuad) {
            throw new IllegalArgumentException(
                    "a ghost only supports anchored single-candidate placement, not " + placement + " (rule 4)");
        }
    }

    private static void rule5(AnchorFacet anchor, GroupFacet group) {
        if ((group.strategy() instanceof OrbitAroundAnchor || group.strategy() instanceof ClusterToRepresentative)
                && !anchor.worldAnchored()) {
            throw new IllegalArgumentException(
                    group.strategy().getClass().getSimpleName() + " needs a world anchor (rule 5)");
        }
    }

    private static void rule6(GroupFacet group, AlgorithmProfile algorithm) {
        if (group.strategy() instanceof ClusterToRepresentative && !algorithm.clustersElements()) {
            throw new IllegalArgumentException(
                    "ClusterToRepresentative needs a cluster-binding profile, not " + algorithm + " (rule 6)");
        }
    }

    /**
     * Rule 7: a world-only element must bring its own world representation —
     * a custom layouter that proposes world candidates, or a world anchor.
     *
     * @throws IllegalArgumentException with the rule number on violation
     */
    public static void validateWorldOnly(boolean worldOnly, AnchorFacet anchor, boolean hasCustomLayouter) {
        if (worldOnly && !hasCustomLayouter && !anchor.worldAnchored()) {
            throw new IllegalArgumentException(
                    "worldOnly needs a custom layouter or a world anchor: " + anchor.kind() + " (rule 7)");
        }
    }

    /**
     * Rules 8–10: the zone facet's combinations. A null facet is the default
     * and always legal — nothing below runs for it.
     *
     * @throws IllegalArgumentException with the rule number on violation
     */
    public static void validateZone(
            @Nullable ZoneFacet zone,
            AnchorFacet anchor,
            SpaceFacet spaces,
            AlgorithmProfile algorithm,
            boolean worldOnly) {
        if (zone == null) {
            return;
        }
        rule8(zone, spaces, worldOnly);
        rule9(anchor, algorithm);
        rule10(zone);
    }

    private static void rule8(ZoneFacet zone, SpaceFacet spaces, boolean worldOnly) {
        if (spaces.policy() == SpacePolicy.ghost) {
            throw new IllegalArgumentException("a zone declaration needs screen participation, not ghost (rule 8)");
        }
        if (worldOnly) {
            throw new IllegalArgumentException(
                    "a zone declaration needs screen participation, not the world-only capability (rule 8)");
        }
    }

    private static void rule9(AnchorFacet anchor, AlgorithmProfile algorithm) {
        if (anchor.kind() == AnchorFacet.Kind.none) {
            throw new IllegalArgumentException("a zone declaration needs a declared anchor, not none (rule 9)");
        }
        if (algorithm.placement() == AlgorithmProfile.Placement.dockCursor) {
            throw new IllegalArgumentException(
                    "a zone declaration needs an anchor-positioned candidate family, not the dock cursor (rule 9)");
        }
    }

    private static void rule10(ZoneFacet zone) {
        LodTier tier = zone.initialTier();
        if (tier != LodTier.full && tier != LodTier.compact) {
            throw new IllegalArgumentException(
                    "a zone declaration enters at full or compact, not " + tier + " (rule 10)");
        }
    }
}
