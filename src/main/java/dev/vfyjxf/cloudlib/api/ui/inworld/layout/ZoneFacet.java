package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.zone.AttentionField;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.LodTier;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.VisibilityPolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.ZoneCandidates;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.ZoneModel;
import dev.vfyjxf.cloudlib.api.ui.inworld.zone.ZoneWeights;
import org.jspecify.annotations.Nullable;

import java.util.Objects;

/**
 * The zone facet (Z2): the declarative opt-in to the visual-zone placement
 * path. A spec carrying a zone facet binds the named zone strategies —
 * {@code candidates.zoneGrid} for the alignment lattice and
 * {@code rank.zoneCost} for the unified cost ranking — in place of the
 * profile's default candidate family and the weighted-linear ranker. A spec
 * without one (the default) runs the exact pre-zone pipeline: no zone
 * context is built, no zone code path is taken.
 * <p>
 * The facet is declaration only — every knob may be left null and resolves
 * to its Z1 default at consumption time (see the {@code *OrDefault}
 * accessors). The attention field is the one input that depends on the
 * frame's screen size, so a null field resolves per frame to a screen-center
 * Gaussian with {@link #defaultAttentionSigmaPx}; an adapter that knows the
 * real gaze origin supplies an explicit field instead. The visibility policy
 * and the initial LOD tier are advisory vocabulary for the presentation
 * side (Z3/Z4 consumers); the coordinator does not read them in Z2.
 * <p>
 * Validation (see {@code FacetRules} rules 8–10): a zone declaration needs
 * screen-arbitration participation (not ghost, not world-only), a declared
 * anchor with a non-dock placement family, and a content-bearing initial
 * tier ({@code full} or {@code compact}).
 *
 * @param attention the center attention field supply; null resolves to a
 *        screen-center Gaussian per frame
 * @param weights the unified cost weights; null means
 *        {@link ZoneWeights#defaults()}
 * @param candidatesConfig the lattice tier clearances; null means
 *        {@link ZoneCandidates.Config#defaults()}
 * @param modelConfig the zone partition knobs; null means
 *        {@link ZoneModel.Config#defaults()}
 * @param visibility how the element responds to losing line of sight or
 *        leaving the screen; the advisory default is {@code fade} (the
 *        current panel semantics)
 * @param initialTier the LOD tier the element enters the scene at; the
 *        default is {@code full}
 */
public record ZoneFacet(
    @Nullable AttentionField attention,
    @Nullable ZoneWeights weights,
    ZoneCandidates.@Nullable Config candidatesConfig,
    ZoneModel.@Nullable Config modelConfig,
    VisibilityPolicy visibility,
    LodTier initialTier
) {

    /**
     * The default spread σ of the screen-center Gaussian used when no
     * attention field is declared, in gui pixels.
     */
    public static final double defaultAttentionSigmaPx = 96.0;

    public ZoneFacet {
        Objects.requireNonNull(visibility, "visibility");
        Objects.requireNonNull(initialTier, "initialTier");
    }

    /** A zone facet with every knob at its default. */
    public static ZoneFacet of() {
        return new ZoneFacet(null, null, null, null, VisibilityPolicy.fade, LodTier.full);
    }

    /** A zone facet declaring an explicit attention field, everything else default. */
    public static ZoneFacet of(AttentionField attention) {
        return new ZoneFacet(attention, null, null, null, VisibilityPolicy.fade, LodTier.full);
    }

    /** The effective weights — {@link ZoneWeights#defaults()} when undeclared. */
    public ZoneWeights weightsOrDefault() {
        return weights != null ? weights : ZoneWeights.defaults();
    }

    /** The effective lattice clearances — the defaults when undeclared. */
    public ZoneCandidates.Config candidatesConfigOrDefault() {
        return candidatesConfig != null ? candidatesConfig : ZoneCandidates.Config.defaults();
    }

    /** The effective partition knobs — the defaults when undeclared. */
    public ZoneModel.Config modelConfigOrDefault() {
        return modelConfig != null ? modelConfig : ZoneModel.Config.defaults();
    }

    /** The same facet with {@code newAttention} as the field supply. */
    public ZoneFacet withAttention(@Nullable AttentionField newAttention) {
        return new ZoneFacet(newAttention, weights, candidatesConfig, modelConfig, visibility, initialTier);
    }

    /** The same facet with {@code newWeights} (null restores the defaults). */
    public ZoneFacet withWeights(@Nullable ZoneWeights newWeights) {
        return new ZoneFacet(attention, newWeights, candidatesConfig, modelConfig, visibility, initialTier);
    }
}
