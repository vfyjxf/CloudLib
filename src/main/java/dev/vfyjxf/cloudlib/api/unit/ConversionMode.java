package dev.vfyjxf.cloudlib.api.unit;

/**
 * Controls conversion strategy for exact and approximate ratio paths.
 */
public enum ConversionMode {
    /**
     * Prefer exact ratio paths when available, otherwise fall back to approximate ratios.
     */
    exactFirstThenApprox,
    /**
     * Require exact ratio paths; throw when only approximate paths are available.
     */
    exactOnly,
    /**
     * Use approximate ratio paths directly without exact-path probing.
     */
    approximateOnly
}
