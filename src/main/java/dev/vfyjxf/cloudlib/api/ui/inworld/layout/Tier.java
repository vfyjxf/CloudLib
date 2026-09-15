package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

/**
 * The presentation tier the solver picked for a request. Only {@link #full},
 * {@link #compact} and {@link #merged} produce a visible panel; {@link #folded}
 * lands in the overflow dock, {@link #omitted} is skipped by request and
 * {@link #unavailable} means the source could not be sampled.
 */
public enum Tier {
    full,
    compact,
    merged,
    folded,
    omitted,
    unavailable
}
