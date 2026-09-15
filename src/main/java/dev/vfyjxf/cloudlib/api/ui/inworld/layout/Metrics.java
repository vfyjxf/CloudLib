package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

/**
 * The size contract of one panel request: the world-space quad extent, the
 * gui-px surface painted onto it, the compact-fallback extent and the
 * minimum projected footprint that still counts as readable.
 */
public record Metrics(
        double worldWidth,
        double worldHeight,
        double screenWidth,
        double screenHeight,
        double compactWorldHeight,
        double compactScreenWidth,
        double compactScreenHeight,
        double minProjectedWidth,
        double minProjectedHeight) {

    public Metrics {
        if (!Double.isFinite(worldWidth
                        + worldHeight
                        + screenWidth
                        + screenHeight
                        + compactWorldHeight
                        + compactScreenWidth
                        + compactScreenHeight
                        + minProjectedWidth
                        + minProjectedHeight)
                || worldWidth <= 0.0
                || worldHeight <= 0.0
                || screenWidth <= 0.0
                || screenHeight <= 0.0
                || compactWorldHeight <= 0.0
                || compactScreenWidth <= 0.0
                || compactScreenHeight <= 0.0
                || minProjectedWidth <= 0.0
                || minProjectedHeight <= 0.0) {
            throw new IllegalArgumentException();
        }
    }

    public static Metrics standard() {
        return new Metrics(1.55, 1.1, 270.0, 180.0, 0.27, 190.0, 46.0, 100.0, 62.0);
    }
}
