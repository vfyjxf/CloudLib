package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.algorithm.AlgorithmProfile;

import java.util.Objects;

/**
 * Turns an {@link ElementSpec} into a drivable element (§7 plan B): resolves
 * the spec's profile-bound named strategies from the {@link StageCatalogs}
 * (fail-fast — an unknown name throws here, not mid-frame) and produces an
 * {@link AssembledElement} ready to register with the
 * {@code InworldCoordinator}. Per frame, the driving adapter hands the
 * element its {@link LayoutEnvironment} ({@code beginFrame}), runs the
 * coordinator's frame, then dispatches the result back
 * ({@link AssembledElement#observe}) so an embedded custom layouter hears
 * its {@code arbitrated} callback.
 * <p>
 * The stage-to-strategy binding is closed: the profile's
 * {@link AlgorithmProfile.Placement} picks the candidate strategy, the
 * avoidance facet picks the filter, and stickiness picks the ranker. Third
 * parties change behavior by registering or replacing named strategies —
 * never by touching this pipeline.
 */
public final class PipelineAssembler {

    /** A fresh assembler (stateless — the catalogs are the shared state). */
    public static PipelineAssembler create() {
        return new PipelineAssembler();
    }

    private PipelineAssembler() {}

    /**
     * Assembles {@code spec} into a coordinator-drivable element.
     *
     * @throws IllegalArgumentException if a bound strategy name is unknown
     */
    public AssembledElement assemble(ElementSpec spec) {
        Objects.requireNonNull(spec, "spec");
        return new AssembledElement(spec, resolveCandidates(spec), resolveAvoid(spec), resolveRank(spec));
    }

    private static StageCatalogs.CandidateStrategy resolveCandidates(ElementSpec spec) {
        String name =
                switch (spec.profile().algorithm().placement()) {
                    case none -> StageCatalogs.candidatesSingle;
                    case dockCursor -> StageCatalogs.candidatesDockCursor;
                    case orbitRing -> StageCatalogs.candidatesOrbitRing;
                    case anchoredQuad -> StageCatalogs.candidatesSingle;
                    case excentricColumn -> StageCatalogs.candidatesExcentricColumn;
                };
        return StageCatalogs.requireCandidateStrategy(name);
    }

    private static StageCatalogs.AvoidStrategy resolveAvoid(ElementSpec spec) {
        if (!spec.avoidance().respectsExclusions() && spec.avoidance().avoids().isEmpty()) {
            return StageCatalogs.requireAvoidStrategy(StageCatalogs.avoidNone);
        }
        if (spec.avoidance().avoids().isEmpty()) {
            return StageCatalogs.requireAvoidStrategy(StageCatalogs.avoidExclusions);
        }
        return StageCatalogs.requireAvoidStrategy(StageCatalogs.avoidExclusionsAndMasks);
    }

    private static StageCatalogs.RankStrategy resolveRank(ElementSpec spec) {
        return StageCatalogs.requireRankStrategy(StageCatalogs.rankWeightedLinear);
    }
}
