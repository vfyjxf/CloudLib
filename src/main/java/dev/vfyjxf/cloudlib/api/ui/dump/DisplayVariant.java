package dev.vfyjxf.cloudlib.api.ui.dump;

import java.util.ArrayList;
import java.util.List;

/**
 * Describes a physical display configuration: resolution + GUI scale.
 * <p>
 * Used by both JUnit tests ({@code TestScene.forEachSize}) and dump tests
 * ({@code DumpContext.forEachVariant}) to test layouts under different display conditions.
 *
 * <h3>Example</h3>
 * <pre>{@code
 * // Standard set: common resolutions × scales 1-4
 * for (DisplayVariant v : DisplayVariant.standardSet()) {
 *     scene.resize(v.logicalWidth(), v.logicalHeight());
 *     // assert layout...
 * }
 *
 * // Custom variant
 * var ultrawide = DisplayVariant.of("3440x1440_s2", 3440, 1440, 2);
 * }</pre>
 *
 * @param name           human-readable label, e.g. "1920x1080_s2"
 * @param physicalWidth  physical pixel width
 * @param physicalHeight physical pixel height
 * @param guiScale       Minecraft GUI scale factor (1-4)
 */
public record DisplayVariant(String name, int physicalWidth, int physicalHeight, int guiScale) {

    public DisplayVariant {
        if (guiScale < 1) throw new IllegalArgumentException("guiScale must be >= 1, got " + guiScale);
        if (physicalWidth < 1 || physicalHeight < 1)
            throw new IllegalArgumentException("Resolution must be positive");
    }

    /**
     * Logical width seen by the UI layout engine.
     */
    public int logicalWidth() {
        return physicalWidth / guiScale;
    }

    /**
     * Logical height seen by the UI layout engine.
     */
    public int logicalHeight() {
        return physicalHeight / guiScale;
    }

    // ==================== Factory methods ====================

    public static DisplayVariant of(String name, int physicalWidth, int physicalHeight, int guiScale) {
        return new DisplayVariant(name, physicalWidth, physicalHeight, guiScale);
    }

    public static DisplayVariant small(int scale) {
        return new DisplayVariant("854x480_s" + scale, 854, 480, scale);
    }

    public static DisplayVariant hd(int scale) {
        return new DisplayVariant("1366x768_s" + scale, 1366, 768, scale);
    }

    public static DisplayVariant fhd(int scale) {
        return new DisplayVariant("1920x1080_s" + scale, 1920, 1080, scale);
    }

    public static DisplayVariant qhd(int scale) {
        return new DisplayVariant("2560x1440_s" + scale, 2560, 1440, scale);
    }

    public static DisplayVariant uhd4k(int scale) {
        return new DisplayVariant("3840x2160_s" + scale, 3840, 2160, scale);
    }

    // ==================== Preset sets ====================

    /**
     * Standard test set: common resolutions × reasonable GUI scales.
     * Filters out combinations where the logical size would be too small (< 200×120).
     */
    public static List<DisplayVariant> standardSet() {
        int[][] sizes = {
                {854, 480},
                {1366, 768},
                {1920, 1080},
                {2560, 1440},
        };
        String[] names = {"854x480", "1366x768", "1920x1080", "2560x1440"};

        List<DisplayVariant> list = new ArrayList<>();
        for (int i = 0; i < sizes.length; i++) {
            for (int scale = 1; scale <= 4; scale++) {
                int lw = sizes[i][0] / scale;
                int lh = sizes[i][1] / scale;
                if (lw >= 200 && lh >= 120) {
                    list.add(new DisplayVariant(names[i] + "_s" + scale, sizes[i][0], sizes[i][1], scale));
                }
            }
        }
        return List.copyOf(list);
    }

    /**
     * Compact test set: FHD at scales 1-3 only.
     * Use when you want a quick multi-scale sanity check.
     */
    public static List<DisplayVariant> compactSet() {
        return List.of(fhd(1), fhd(2), fhd(3));
    }
}
