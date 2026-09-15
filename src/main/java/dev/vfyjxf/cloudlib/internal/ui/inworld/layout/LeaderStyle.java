package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

/**
 * The leader routing style constants: gap kept off the source, gate
 * length off the panel edge, clearance, bend cost and the hard limits.
 */
public record LeaderStyle(
        double sourceGap,
        double terminalLength,
        double clearance,
        double bendCost,
        double titlePreference,
        double switchThreshold,
        double minPanelArea,
        double viewportMargin) {

    public LeaderStyle {
        if (sourceGap <= 0.0
                || terminalLength <= 0.0
                || clearance < 0.0
                || bendCost < 0.0
                || titlePreference < 0.0
                || switchThreshold < 0.0
                || minPanelArea < 0.0
                || viewportMargin < 0.0) {
            throw new IllegalArgumentException("style values");
        }
    }

    /** The routing style the solver uses for all panels. */
    public static LeaderStyle routing() {
        return new LeaderStyle(8.0, 12.0, 3.0, 32.0, 24.0, 20.0, 100.0, 2.0);
    }
}
