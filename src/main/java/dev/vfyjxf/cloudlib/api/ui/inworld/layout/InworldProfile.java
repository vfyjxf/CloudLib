package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.algorithm.AlgorithmProfile;
import dev.vfyjxf.cloudlib.api.ui.inworld.group.ClusterToRepresentative;
import dev.vfyjxf.cloudlib.api.ui.inworld.group.InworldGroup;
import dev.vfyjxf.cloudlib.api.ui.inworld.group.OrbitAroundAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpaceMask;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpacePolicy;

import java.util.Set;
import java.util.function.Supplier;

/**
 * The closed registry of built-in layout profiles (§3.0 iron rule 2): each
 * constant is a preset combination of the seven facets bound to one
 * {@link AlgorithmProfile}'s algorithm combination. New combinations are
 * new constants — a compile-time act, never a runtime combinator. Callers
 * pick a profile and override facets (the {@code with}-methods revalidate);
 * the facets' default anchors are placeholder bindings meant to be replaced
 * with the real entity/block/position anchor.
 * <p>
 * The ten constants cover the inworld type catalog's layout families (the
 * full 22-class mapping lives in {@code docs/inworld-type-catalog-mapping.md}):
 * <ul>
 *   <li>{@link #nameplate} — entity health bars and name tags: orbit-ring
 *       candidates, sticky arbitration, occlusion-aware avoidance, density
 *       clustering with a "+N" representative, degradation down to a
 *       directional pip</li>
 *   <li>{@link #facePanel} — block-face attached panels: anchored to the
 *       face quad, occlusion-exempt, dwell-heavy stability</li>
 *   <li>{@link #ground} — ground-parallel decals (area markers, ground
 *       projections): fixed posture on a world position</li>
 *   <li>{@link #hologram} — floating holograms: yaw billboard above a world
 *       position, spring-lifted, occlusion-exempt</li>
 *   <li>{@link #follow} — entity-following interactive panels: nameplate
 *       dynamics claiming both the world and screen-panel layers (the
 *       occluded→sidebar degradation path)</li>
 *   <li>{@link #orbit} — grouped rings (status rows, riders): orbit-ring
 *       candidates with passive arbitration and
 *       {@code OrbitAroundAnchor} grouping</li>
 *   <li>{@link #dock} — screen-edge docked UI (minimap, compass): the
 *       one-dimensional dock cursor, HUD-avoiding</li>
 *   <li>{@link #waypoint} — world markers: single anchored candidates,
 *       indicator-layer exemption, degradation to a directional cue</li>
 *   <li>{@link #transientUi} — damage numbers, pings, bubbles: ghost policy
 *       (mutual non-interference), quick fades</li>
 *   <li>{@link #excentric} — near-crosshair hint columns: the excentric
 *       column with straight leaders</li>
 * </ul>
 */
public enum InworldProfile {
    nameplate(
        AlgorithmProfile.nameplate,
        () -> new ElementFacets(
            AnchorFacet.position(0, 1, 0),
            OrientationFacet.cameraBillboard(),
            new SpaceFacet(Set.of(SpaceMask.worldAnchored), SpacePolicy.active, 2),
            AvoidanceFacet.of(Set.of(SpaceMask.screenPanel, SpaceMask.worldAnchored)),
            StabilityFacet.nameplateBaseline(),
            DegradeFacet.active(new Size(100, 26), new Size(80, 20), new Size(60, 14), new Size(24, 24)),
            GroupFacet.of(new InworldGroup("cloudlib", "nameplates", "default"), ClusterToRepresentative.of())
        )
    ), facePanel(
        AlgorithmProfile.facePanel,
        () -> new ElementFacets(
            AnchorFacet.blockFace(0, 0, 0, AnchorFacet.Normal.north),
            OrientationFacet.blockFace(),
            new SpaceFacet(Set.of(SpaceMask.worldAnchored), SpacePolicy.fixed, 5),
            AvoidanceFacet.none(),
            StabilityFacet.fixedBaseline(),
            DegradeFacet.fixed(new Size(120, 90), new Size(90, 60), new Size(60, 24), new Size(20, 20)),
            GroupFacet.none()
        )
    ), ground(
        AlgorithmProfile.facePanel,
        () -> new ElementFacets(
            AnchorFacet.position(0, 0, 0),
            OrientationFacet.groundParallel(),
            new SpaceFacet(Set.of(SpaceMask.worldAnchored), SpacePolicy.fixed, 4),
            AvoidanceFacet.none(),
            StabilityFacet.fixedBaseline(),
            DegradeFacet.fixed(new Size(64, 64), new Size(32, 32), new Size(12, 12)),
            GroupFacet.none()
        )
    ), hologram(
        AlgorithmProfile.facePanel,
        () -> new ElementFacets(
            AnchorFacet.position(0, 1, 0),
            OrientationFacet.yawBillboard(),
            new SpaceFacet(Set.of(SpaceMask.worldAnchored), SpacePolicy.fixed, 4),
            AvoidanceFacet.none(),
            StabilityFacet.nameplateBaseline(),
            DegradeFacet.fixed(new Size(80, 60), new Size(48, 36), new Size(24, 24)),
            GroupFacet.none()
        )
    ), follow(
        AlgorithmProfile.nameplate,
        () -> new ElementFacets(
            AnchorFacet.entity("entity"),
            OrientationFacet.cameraBillboard(),
            new SpaceFacet(Set.of(SpaceMask.worldAnchored, SpaceMask.screenPanel), SpacePolicy.active, 4),
            AvoidanceFacet.of(Set.of(SpaceMask.screenPanel)),
            StabilityFacet.nameplateBaseline(),
            DegradeFacet.active(new Size(140, 100), new Size(100, 60), new Size(60, 28), new Size(28, 28)),
            GroupFacet.none()
        )
    ), orbit(
        AlgorithmProfile.orbit,
        () -> new ElementFacets(
            AnchorFacet.position(0, 1, 0),
            OrientationFacet.cameraBillboard(),
            new SpaceFacet(Set.of(SpaceMask.worldAnchored), SpacePolicy.passive, 1),
            AvoidanceFacet.of(Set.of(SpaceMask.screenPanel)),
            StabilityFacet.nameplateBaseline(),
            DegradeFacet.of(SpacePolicy.passive, true, true, new Size(96, 28), new Size(48, 24), new Size(24, 24)),
            GroupFacet.of(new InworldGroup("cloudlib", "rings", "default"), OrbitAroundAnchor.of())
        )
    ), dock(
        AlgorithmProfile.dock,
        () -> new ElementFacets(
            AnchorFacet.cameraTracked(0.97, 0.05),
            OrientationFacet.screen(),
            new SpaceFacet(Set.of(SpaceMask.screenPanel), SpacePolicy.passive, 6),
            AvoidanceFacet.of(Set.of(SpaceMask.hudBase, SpaceMask.hudOverlay)),
            StabilityFacet.fixedBaseline(),
            DegradeFacet.of(SpacePolicy.passive, false, true, new Size(100, 100), new Size(64, 64), new Size(32, 32)),
            GroupFacet.none()
        )
    ), waypoint(
        AlgorithmProfile.waypoint,
        () -> new ElementFacets(
            AnchorFacet.position(0, 0, 0),
            OrientationFacet.cameraBillboard(),
            new SpaceFacet(Set.of(SpaceMask.indicator), SpacePolicy.fixed, 3),
            AvoidanceFacet.none(),
            StabilityFacet.fixedBaseline(),
            DegradeFacet.fixed(new Size(90, 28), new Size(48, 20), new Size(28, 28), new Size(24, 12)),
            GroupFacet.none()
        )
    ), transientUi(
        AlgorithmProfile.transientUi,
        () -> new ElementFacets(
            AnchorFacet.position(0, 1, 0),
            OrientationFacet.cameraBillboard(),
            new SpaceFacet(Set.of(SpaceMask.worldAnchored), SpacePolicy.ghost, 0),
            AvoidanceFacet.none(),
            StabilityFacet.transientBaseline(),
            DegradeFacet.ghost(new Size(40, 20), new Size(24, 16), new Size(12, 12)),
            GroupFacet.none()
        )
    ), excentric(
        AlgorithmProfile.excentric,
        () -> new ElementFacets(
            AnchorFacet.position(0, 0, 0),
            OrientationFacet.cameraBillboard(),
            new SpaceFacet(Set.of(SpaceMask.worldAnchored), SpacePolicy.passive, 2),
            AvoidanceFacet.of(Set.of(SpaceMask.screenPanel)),
            StabilityFacet.nameplateBaseline(),
            DegradeFacet.of(SpacePolicy.passive, true, true, new Size(120, 22), new Size(80, 20), new Size(40, 18)),
            GroupFacet.none()
        )
    );

    private final AlgorithmProfile algorithm;
    private final Supplier<ElementFacets> preset;
    private ElementFacets facets;

    InworldProfile(AlgorithmProfile algorithm, Supplier<ElementFacets> preset) {
        this.algorithm = algorithm;
        this.preset = preset;
    }

    /** The bound algorithm combination (the M3 closed profile). */
    public AlgorithmProfile algorithm() {
        return algorithm;
    }

    /**
     * The profile's facet preset — seven immutable facets, validated against
     * this profile's algorithm binding (memoized after the first call).
     */
    public ElementFacets facets() {
        if (facets == null) {
            ElementFacets built = preset.get();
            FacetRules.validate(
                built.anchor(),
                built.orientation(),
                built.spaces(),
                built.avoidance(),
                built.group(),
                algorithm
            );
            facets = built;
        }
        return facets;
    }
}
