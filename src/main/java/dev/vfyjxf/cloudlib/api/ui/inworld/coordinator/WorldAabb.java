package dev.vfyjxf.cloudlib.api.ui.inworld.coordinator;

import java.util.Objects;

/**
 * An axis-aligned bounding box in world space — the world half of a
 * {@link PlacementCandidate}'s dual representation. The coordinator package is
 * pure logic, so it carries its own double-precision box instead of borrowing
 * the Minecraft-coupled vector types; adapters convert at the boundary.
 * Occlusion sampling and leader routing consume this representation (never the
 * screen rect alone), while screen-space overlap arbitration consumes the
 * projected rect.
 *
 * @param minX the low x corner, world units (blocks)
 * @param minY the low y corner
 * @param minZ the low z corner
 * @param maxX the high x corner; must not be below {@code minX}
 * @param maxY the high y corner
 * @param maxZ the high z corner
 */
public record WorldAabb(double minX, double minY, double minZ, double maxX, double maxY, double maxZ) {

    public WorldAabb {
        requireCorner("minX/maxX", minX, maxX);
        requireCorner("minY/maxY", minY, maxY);
        requireCorner("minZ/maxZ", minZ, maxZ);
    }

    /**
     * A box of {@code width × height × depth} centered at the given point.
     */
    public static WorldAabb around(
            double centerX, double centerY, double centerZ, double width, double height, double depth) {
        if (!isFinite(width) || width < 0 || !isFinite(height) || height < 0 || !isFinite(depth) || depth < 0) {
            throw new IllegalArgumentException(
                    "box size must be finite and non-negative: " + width + "x" + height + "x" + depth);
        }
        return new WorldAabb(
                centerX - width * 0.5,
                centerY - height * 0.5,
                centerZ - depth * 0.5,
                centerX + width * 0.5,
                centerY + height * 0.5,
                centerZ + depth * 0.5);
    }

    /** The {@code x} component of the box center. */
    public double centerX() {
        return (minX + maxX) * 0.5;
    }

    /** The {@code y} component of the box center. */
    public double centerY() {
        return (minY + maxY) * 0.5;
    }

    /** The {@code z} component of the box center. */
    public double centerZ() {
        return (minZ + maxZ) * 0.5;
    }

    private static void requireCorner(String name, double min, double max) {
        Objects.requireNonNull(name);
        if (!isFinite(min) || !isFinite(max) || max < min) {
            throw new IllegalArgumentException(name + " must be finite with max >= min: " + min + ".." + max);
        }
    }

    private static boolean isFinite(double value) {
        return Double.isFinite(value);
    }
}
