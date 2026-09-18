package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

import org.jetbrains.annotations.Nullable;

import java.util.EnumMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;

/**
 * An element's ordered degradation ladder (§3.4): an immutable list of
 * {@link InworldVariant rungs}, index 0 the strongest form, each rung weaker
 * than the one before. The coordinator owns the walk down (rejection-driven)
 * and the single-step climb back up (epoch boundaries only); elements just
 * render whatever rung they are granted.
 * <p>
 * Two monotonicity invariants hold for every ladder and are validated at
 * construction: requested area never grows down the ladder, and the content
 * tier never regresses down the ladder (a smaller form never shows more
 * information than a bigger one). Every variant's {@code level} must equal
 * its index.
 */
public final class VariantLadder {

    private static final int defaultDegradeSteps = 1;

    private final List<InworldVariant> rungs;
    private final EnumMap<RejectionReason, Integer> degradeSteps;

    private VariantLadder(List<InworldVariant> rungs, EnumMap<RejectionReason, Integer> degradeSteps) {
        this.rungs = List.copyOf(rungs);
        this.degradeSteps = degradeSteps.clone();
    }

    /**
     * A ladder where every rejection reason degrades exactly one rung.
     *
     * @throws IllegalArgumentException if the list is empty, a variant's level
     *         does not match its index, or the monotonicity invariants fail
     */
    public static VariantLadder of(List<InworldVariant> variants) {
        return of(variants, Map.of());
    }

    /**
     * A ladder with per-reason degrade skips: {@code degradeSteps} maps a
     * rejection reason to how many rungs that reason costs (unmapped reasons
     * cost one). A spatial squeeze — {@code insufficientArea},
     * {@code outOfBounds} — is the typical multi-rung skip.
     *
     * @throws IllegalArgumentException if the list is empty, a variant's level
     *         does not match its index, a skip is not at least 1, or the
     *         monotonicity invariants fail
     */
    public static VariantLadder of(List<InworldVariant> variants, Map<RejectionReason, Integer> degradeSteps) {
        Objects.requireNonNull(variants, "variants");
        Objects.requireNonNull(degradeSteps, "degradeSteps");
        if (variants.isEmpty()) {
            throw new IllegalArgumentException("a ladder needs at least one rung");
        }
        EnumMap<RejectionReason, Integer> steps = new EnumMap<>(RejectionReason.class);
        for (Map.Entry<RejectionReason, Integer> entry : degradeSteps.entrySet()) {
            Objects.requireNonNull(entry.getKey(), "reason");
            Objects.requireNonNull(entry.getValue(), "steps");
            if (entry.getValue() < 1) {
                throw new IllegalArgumentException(
                        "degrade steps must be at least 1 for " + entry.getKey() + ": " + entry.getValue());
            }
            steps.put(entry.getKey(), entry.getValue());
        }
        validate(variants);
        return new VariantLadder(variants, steps);
    }

    private static void validate(List<InworldVariant> variants) {
        for (int i = 0; i < variants.size(); i++) {
            InworldVariant variant = variants.get(i);
            if (variant.level() != i) {
                throw new IllegalArgumentException(
                        "rung " + i + " must carry level " + i + " but carries " + variant.level());
            }
            if (i > 0) {
                InworldVariant previous = variants.get(i - 1);
                if (variant.requestedArea() > previous.requestedArea()) {
                    throw new IllegalArgumentException("requested area must not grow down the ladder: rung " + i
                            + " has " + variant.requestedArea() + " after " + previous.requestedArea());
                }
                if (variant.contentTier().ordinal() < previous.contentTier().ordinal()) {
                    throw new IllegalArgumentException("content tier must not regress down the ladder: rung " + i
                            + " has " + variant.contentTier() + " after " + previous.contentTier());
                }
            }
        }
    }

    /** The number of rungs. */
    public int size() {
        return rungs.size();
    }

    /** The strongest rung (index 0). */
    public InworldVariant strongest() {
        return rungs.getFirst();
    }

    /** The weakest rung (the last index). */
    public InworldVariant weakest() {
        return rungs.getLast();
    }

    /** The rung at {@code level}. */
    public InworldVariant variant(int level) {
        if (level < 0 || level >= rungs.size()) {
            throw new IllegalArgumentException("level out of range [0, " + rungs.size() + "): " + level);
        }
        return rungs.get(level);
    }

    /** Whether {@code variant} is this ladder's weakest rung. */
    public boolean isWeakest(InworldVariant variant) {
        return variant.level() == rungs.size() - 1;
    }

    /** Whether {@code variant} is this ladder's strongest rung. */
    public boolean isStrongest(InworldVariant variant) {
        return variant.level() == 0;
    }

    /**
     * How many rungs the given rejection costs — the reason-based skip.
     */
    public int degradeSteps(RejectionReason reason) {
        return degradeSteps.getOrDefault(reason, defaultDegradeSteps);
    }

    /**
     * The rung to fall back to after {@code current} was rejected for
     * {@code reason}: {@code current}'s level plus the reason's skip, clamped
     * to the weakest rung.
     *
     * @return the degraded rung, or {@code null} when {@code current} already
     *         is the weakest (there is nothing left to give)
     */
    public @Nullable InworldVariant degrade(InworldVariant current, RejectionReason reason) {
        Objects.requireNonNull(current, "current");
        Objects.requireNonNull(reason, "reason");
        int target = Math.min(rungs.size() - 1, current.level() + degradeSteps(reason));
        return target == current.level() ? null : rungs.get(target);
    }

    /**
     * One rung back toward the strongest — the recovery step, allowed only on
     * epoch boundaries (the coordinator enforces that; this method is just
     * the arithmetic).
     *
     * @return the stronger rung, or {@code null} when {@code current} already
     *         is the strongest
     */
    public @Nullable InworldVariant upgrade(InworldVariant current) {
        Objects.requireNonNull(current, "current");
        return current.level() == 0 ? null : rungs.get(current.level() - 1);
    }
}
