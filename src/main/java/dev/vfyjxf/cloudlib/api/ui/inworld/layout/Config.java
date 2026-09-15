package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

/**
 * The solver's tuning knobs: layout spacing, search effort, temporal
 * stability and overflow limits. {@link #defaults()} is what the source
 * implementation was tuned against.
 */
public record Config(
        double margin,
        double gap,
        double minPanelVisibility,
        int beamWidth,
        int candidatesPerTier,
        double switchPenalty,
        double recoverySeconds,
        int maxLeaders,
        double worldClearance,
        int maxPanels,
        double maxCoverage) {

    public Config {
        if (!Double.isFinite(margin
                        + gap
                        + minPanelVisibility
                        + switchPenalty
                        + recoverySeconds
                        + worldClearance
                        + maxCoverage)
                || margin < 0.0
                || gap < 0.0
                || minPanelVisibility < 0.0
                || minPanelVisibility > 1.0
                || switchPenalty < 0.0
                || recoverySeconds < 0.0
                || worldClearance < 0.0
                || beamWidth < 1
                || candidatesPerTier < 1
                || maxLeaders < 0
                || maxPanels < 0
                || maxCoverage <= 0.0
                || maxCoverage > 1.0) {
            throw new IllegalArgumentException("layout config");
        }
    }

    public static Config defaults() {
        return new Config(14.0, 10.0, 0.93, 14, 8, 65.0, 0.25, 5, 0.015, 12, 0.48);
    }
}
