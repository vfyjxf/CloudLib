package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

/**
 * What the last {@code solve} spent: expansions and route evaluations
 * performed, repairs applied, whether the budget was hit, and wall-clock
 * nanoseconds.
 */
public record SearchStats(int expansions, int routeEvaluations, int repairs, boolean budgetReached, long nanos) {}
