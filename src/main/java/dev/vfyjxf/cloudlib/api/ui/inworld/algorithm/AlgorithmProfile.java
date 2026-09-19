package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import java.util.Optional;

/**
 * The closed set of named layout profiles — the only public way the multi
 * algorithm layer's components are combined (§3.0 iron rule 2: free assembly
 * of algorithms is an untestable behavior space, so it does not exist here).
 * Each constant binds a fixed algorithm combination plus its default
 * parameters; callers pick a profile, never a pile of algorithms. The
 * extension point is this enumeration itself: a new combination is a new
 * constant (a compile-time act), not a runtime combinator.
 * <p>
 * The seven constants cover the inworld UI type catalog's layout families:
 * <ul>
 *   <li>{@link #nameplate} — entity health bars / nameplates: orbit-ring
 *       candidates around the anchor, sticky slot assignment with a small
 *       recourse budget, adaptive leaders, density clustering, occlusion
 *       fade, and off-screen degradation to an ANGLE indicator. Numeric
 *       defaults follow the WoW nameplate baseline (§3.0): motionSpeed 0.025
 *       per-frame equivalent, overlapH 0.8, overlapV 1.1,
 *       occludedAlphaMult 0.4</li>
 *   <li>{@link #dock} — screen-edge docked UI (minimap and friends): the
 *       one-dimensional {@link DockCursor}, nothing else</li>
 *   <li>{@link #facePanel} — block-face attached panels: anchored to the
 *       quad, no slot machinery, no leaders</li>
 *   <li>{@link #waypoint} — waypoints: {@link AngleEncoder} off-screen
 *       indication, no slot machinery</li>
 *   <li>{@link #transientUi} — damage numbers and pings: weakly coordinated,
 *       no slots, no leaders, no clustering (mutual non-interference)</li>
 *   <li>{@link #excentric} — near-crosshair multiple hints: the
 *       {@link ExcentricColumn} with straight leaders back to the focus</li>
 *   <li>{@link #orbit} — grouped rings (status icon rows, riders): orbit
 *       rings with clustering and shared-trunk hyperleaders</li>
 * </ul>
 */
public enum AlgorithmProfile {
    nameplate(
            Placement.orbitRing,
            LeaderStrategy.adaptive,
            OffscreenStrategy.angleEncoder,
            true,
            Params.of(0.025, 0.8, 1.1, 0.4, 2, 20.0, 0.15, 0.6, 120.0, 1.0, 2, 8.0, 0.2, 12.0, 4.0)),
    dock(
            Placement.dockCursor,
            LeaderStrategy.none,
            OffscreenStrategy.none,
            false,
            Params.of(0.1, 0.8, 1.1, 1.0, 1, 30.0, 0.2, 0.5, 80.0, 1.0, 2, 8.0, 0.2, 8.0, 6.0)),
    facePanel(
            Placement.anchoredQuad,
            LeaderStrategy.none,
            OffscreenStrategy.none,
            false,
            Params.of(0.05, 0.8, 1.1, 0.6, 0, 0.0, 0.0, 0.5, 80.0, 1.0, 2, 8.0, 0.2, 6.0, 4.0)),
    waypoint(
            Placement.none,
            LeaderStrategy.none,
            OffscreenStrategy.angleEncoder,
            false,
            Params.of(0.05, 0.8, 1.1, 1.0, 0, 0.0, 0.0, 0.5, 80.0, 1.0, 2, 6.0, 0.15, 6.0, 4.0)),
    transientUi(
            Placement.none,
            LeaderStrategy.none,
            OffscreenStrategy.none,
            false,
            Params.of(0.15, 1.0, 1.0, 1.0, 0, 0.0, 0.0, 0.5, 80.0, 1.0, 2, 8.0, 0.2, 6.0, 4.0)),
    excentric(
            Placement.excentricColumn,
            LeaderStrategy.straightOnly,
            OffscreenStrategy.none,
            false,
            Params.of(0.1, 0.8, 1.1, 1.0, 0, 0.0, 0.0, 0.5, 80.0, 1.0, 2, 8.0, 0.2, 6.0, 4.0)),
    orbit(
            Placement.orbitRing,
            LeaderStrategy.adaptive,
            OffscreenStrategy.none,
            true,
            Params.of(0.025, 0.8, 1.1, 0.4, 2, 20.0, 0.15, 0.6, 120.0, 1.0, 2, 8.0, 0.2, 12.0, 4.0));

    /** Which discrete placement family the profile binds. */
    public enum Placement {
        none,
        dockCursor,
        orbitRing,
        anchoredQuad,
        excentricColumn
    }

    /** Which leader routing family the profile binds. */
    public enum LeaderStrategy {
        none,
        straightOnly,
        adaptive
    }

    /** Which off-screen indication family the profile binds. */
    public enum OffscreenStrategy {
        none,
        angleEncoder
    }

    /**
     * The profile's default parameters — the knobs of every algorithm the
     * profile binds, in one place. Fields only meaningful to a profile's
     * bound algorithms are read by those algorithms; the rest carry sensible
     * defaults so the record stays flat and total.
     *
     * @param motionSpeed per-frame follow rate baseline (the WoW nameplate
     *        motionSpeed, 0.025, means "close 2.5% of the gap per frame")
     * @param overlapH the horizontal overlap threshold in overlap-threshold
     *        units (WoW baseline 0.8)
     * @param overlapV the vertical overlap threshold (WoW baseline 1.1)
     * @param occludedAlphaMult the alpha multiplier while occluded (WoW
     *        baseline 0.4)
     * @param recourseBudget the SlotAssigner per-epoch move budget K
     * @param switchPenalty the SlotAssigner flat switch cost
     * @param incumbentDiscount the SlotAssigner incumbent hysteresis
     * @param clusterAlpha the Clusterer α — the stability/sensitivity knob
     * @param clusterMergeRadius the Clusterer distance scale in gui pixels
     * @param leaderBand the LeaderRouter upgrade band (crossing-count units)
     * @param leaderDwellEpochs the LeaderRouter dwell, in epochs
     * @param encoderLambda the AngleEncoder angle smoothing rate in 1/s
     * @param encoderEdgeBand the AngleEncoder edge-switch band in radians
     * @param dockMargin the DockCursor end margin in gui pixels
     * @param dockSpacing the DockCursor inter-slot gap in gui pixels
     */
    public record Params(
            double motionSpeed,
            double overlapH,
            double overlapV,
            double occludedAlphaMult,
            int recourseBudget,
            double switchPenalty,
            double incumbentDiscount,
            double clusterAlpha,
            double clusterMergeRadius,
            double leaderBand,
            int leaderDwellEpochs,
            double encoderLambda,
            double encoderEdgeBand,
            double dockMargin,
            double dockSpacing) {

        public Params {
            if (!Double.isFinite(motionSpeed) || motionSpeed <= 0 || motionSpeed > 1) {
                throw new IllegalArgumentException("motionSpeed must be in (0, 1]: " + motionSpeed);
            }
            if (!Double.isFinite(overlapH) || overlapH <= 0) {
                throw new IllegalArgumentException("overlapH must be finite and positive: " + overlapH);
            }
            if (!Double.isFinite(overlapV) || overlapV <= 0) {
                throw new IllegalArgumentException("overlapV must be finite and positive: " + overlapV);
            }
            if (!Double.isFinite(occludedAlphaMult) || occludedAlphaMult <= 0 || occludedAlphaMult > 1) {
                throw new IllegalArgumentException("occludedAlphaMult must be in (0, 1]: " + occludedAlphaMult);
            }
            if (recourseBudget < 0) {
                throw new IllegalArgumentException("recourseBudget must not be negative: " + recourseBudget);
            }
            if (!Double.isFinite(switchPenalty) || switchPenalty < 0) {
                throw new IllegalArgumentException("switchPenalty must be finite and non-negative: " + switchPenalty);
            }
            if (!Double.isFinite(incumbentDiscount) || incumbentDiscount < 0 || incumbentDiscount >= 1) {
                throw new IllegalArgumentException("incumbentDiscount must be in [0, 1): " + incumbentDiscount);
            }
            if (!Double.isFinite(clusterAlpha) || clusterAlpha < 0 || clusterAlpha > 1) {
                throw new IllegalArgumentException("clusterAlpha must be in [0, 1]: " + clusterAlpha);
            }
            if (!Double.isFinite(clusterMergeRadius) || clusterMergeRadius <= 0) {
                throw new IllegalArgumentException(
                        "clusterMergeRadius must be finite and positive: " + clusterMergeRadius);
            }
            if (!Double.isFinite(leaderBand) || leaderBand <= 0) {
                throw new IllegalArgumentException("leaderBand must be finite and positive: " + leaderBand);
            }
            if (leaderDwellEpochs < 1) {
                throw new IllegalArgumentException("leaderDwellEpochs must be at least 1: " + leaderDwellEpochs);
            }
            if (!Double.isFinite(encoderLambda) || encoderLambda <= 0) {
                throw new IllegalArgumentException("encoderLambda must be finite and positive: " + encoderLambda);
            }
            if (!Double.isFinite(encoderEdgeBand) || encoderEdgeBand <= 0) {
                throw new IllegalArgumentException("encoderEdgeBand must be finite and positive: " + encoderEdgeBand);
            }
            if (!Double.isFinite(dockMargin) || dockMargin < 0) {
                throw new IllegalArgumentException("dockMargin must be finite and non-negative: " + dockMargin);
            }
            if (!Double.isFinite(dockSpacing) || dockSpacing < 0) {
                throw new IllegalArgumentException("dockSpacing must be finite and non-negative: " + dockSpacing);
            }
        }

        public static Params of(
                double motionSpeed,
                double overlapH,
                double overlapV,
                double occludedAlphaMult,
                int recourseBudget,
                double switchPenalty,
                double incumbentDiscount,
                double clusterAlpha,
                double clusterMergeRadius,
                double leaderBand,
                int leaderDwellEpochs,
                double encoderLambda,
                double encoderEdgeBand,
                double dockMargin,
                double dockSpacing) {
            return new Params(
                    motionSpeed,
                    overlapH,
                    overlapV,
                    occludedAlphaMult,
                    recourseBudget,
                    switchPenalty,
                    incumbentDiscount,
                    clusterAlpha,
                    clusterMergeRadius,
                    leaderBand,
                    leaderDwellEpochs,
                    encoderLambda,
                    encoderEdgeBand,
                    dockMargin,
                    dockSpacing);
        }

        /** {@link SlotAssigner} costs for this profile. */
        public SlotAssigner.Costs slotAssignerCosts() {
            return new SlotAssigner.Costs(1.0, switchPenalty, incumbentDiscount);
        }

        /** {@link Clusterer} config for this profile. */
        public Clusterer.Config clustererConfig() {
            return new Clusterer.Config(clusterAlpha, clusterMergeRadius);
        }

        /** {@link LeaderRouter} config for this profile. */
        public LeaderRouter.Config leaderRouterConfig() {
            return LeaderRouter.Config.of(leaderBand, leaderDwellEpochs);
        }

        /** {@link AngleEncoder} config for this profile. */
        public AngleEncoder.Config angleEncoderConfig() {
            return new AngleEncoder.Config(encoderLambda, encoderEdgeBand, 1);
        }
    }

    private final Placement placement;
    private final LeaderStrategy leaders;
    private final OffscreenStrategy offscreen;
    private final boolean clusters;
    private final Params params;

    AlgorithmProfile(
            Placement placement, LeaderStrategy leaders, OffscreenStrategy offscreen, boolean clusters, Params params) {
        this.placement = placement;
        this.leaders = leaders;
        this.offscreen = offscreen;
        this.clusters = clusters;
        this.params = params;
    }

    public Placement placement() {
        return placement;
    }

    public LeaderStrategy leaderStrategy() {
        return leaders;
    }

    public OffscreenStrategy offscreenStrategy() {
        return offscreen;
    }

    /** Whether the profile clusters its elements at density. */
    public boolean clustersElements() {
        return clusters;
    }

    public Params params() {
        return params;
    }

    /**
     * Tolerant lookup for data-driven configuration: matches the constant's
     * {@link #toString()} exactly.
     */
    public static Optional<AlgorithmProfile> byId(String id) {
        for (AlgorithmProfile profile : values()) {
            if (profile.toString().equals(id)) {
                return Optional.of(profile);
            }
        }
        return Optional.empty();
    }
}
