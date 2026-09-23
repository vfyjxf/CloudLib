package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.ElementRejection;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.InworldVariant;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.SpaceBudget;
import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * What an {@link InworldLayouter} (and the pipeline stages) see when
 * proposing: the coordinator's round bookkeeping merged with the frame's
 * {@link LayoutEnvironment} and the element's own declared spec.
 *
 * @param epoch the current decision epoch
 * @param round 0 for the initial round, 1 for the renegotiation round
 * @param budget the space feedback (work area, free fraction)
 * @param lastPlacement the element's last granted placement, if any
 * @param lastRejection the rejection this proposal answers; round 1 only
 * @param variant the ladder rung to propose
 * @param environment this frame's external inputs (screen, exclusions,
 *        occupancy, anchor, clock)
 * @param spec the element's declared spec — a custom layouter reads the
 *        facets it honors and ignores the rest
 */
public record InworldLayoutContext(
    long epoch,
    int round,
    SpaceBudget budget,
    @Nullable InworldPlacement lastPlacement,
    @Nullable ElementRejection lastRejection,
    InworldVariant variant,
    LayoutEnvironment environment,
    ElementSpec spec
) {

    public InworldLayoutContext {
        Objects.requireNonNull(budget, "budget");
        Objects.requireNonNull(variant, "variant");
        Objects.requireNonNull(environment, "environment");
        Objects.requireNonNull(spec, "spec");
        if (round < 0 || round > 1) {
            throw new IllegalArgumentException("round must be 0 or 1: " + round);
        }
    }
}
