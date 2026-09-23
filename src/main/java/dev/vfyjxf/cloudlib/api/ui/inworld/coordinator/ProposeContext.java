package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

import org.jetbrains.annotations.Nullable;

import java.util.Objects;

/**
 * The context an element's {@link InworldElement#propose} call runs against.
 * Round 0 carries the element's current ladder rung (upgraded one step on
 * epoch boundaries when possible); round 1 — the renegotiation round, reached
 * only by rejected elements — carries the rung the coordinator has already
 * degraded to plus the {@link ElementRejection} being answered, so all the
 * element does is lay out candidates for {@link #variant}.
 *
 * @param epoch the current decision epoch (incremented on every resolve)
 * @param round 0 for the initial round, 1 for the renegotiation round
 * @param budget the space feedback: work area and how much of it is free
 * @param lastPlacement the element's last granted placement, if any
 * @param lastRejection the rejection this proposal must answer; non-null only
 *        on round 1
 * @param variant the ladder rung to propose — round 0: the current rung;
 *        round 1: the degraded rung
 */
public record ProposeContext(
    long epoch,
    int round,
    SpaceBudget budget,
    @Nullable InworldPlacement lastPlacement,
    @Nullable ElementRejection lastRejection,
    InworldVariant variant
) {

    public ProposeContext {
        Objects.requireNonNull(budget, "budget");
        Objects.requireNonNull(variant, "variant");
        if (round < 0 || round > 1) {
            throw new IllegalArgumentException("round must be 0 or 1: " + round);
        }
    }
}
