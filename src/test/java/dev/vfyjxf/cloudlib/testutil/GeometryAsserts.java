package dev.vfyjxf.cloudlib.testutil;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3dc;
import org.joml.Vector3fc;

import java.util.Arrays;

/**
 * Epsilon-based assertions for the geometry value types the inworld layout
 * system works with: {@link Pos}/{@link FloatPos} positions,
 * {@link Rect}/{@link FloatRect} rectangles and 2D/3D vectors
 * ({@link Vec3}, JOML {@link Vector3fc}/{@link Vector3dc} or raw component
 * arrays). Failures report expected and actual components together with the
 * largest per-component delta, so float-precision mismatches stay readable.
 */
public final class GeometryAsserts {

    private GeometryAsserts() {}

    public static void assertPosEquals(Pos expected, Pos actual) {
        if (expected.x() != actual.x() || expected.y() != actual.y()) {
            throw new AssertionError("expected " + expected + " but was " + actual);
        }
    }

    public static void assertPosEquals(FloatPos expected, FloatPos actual, double epsilon) {
        double dx = Math.abs(expected.x() - actual.x());
        double dy = Math.abs(expected.y() - actual.y());
        if (dx > epsilon || dy > epsilon) {
            throw mismatch("FloatPos", expected.toString(), actual.toString(), epsilon, Math.max(dx, dy));
        }
    }

    public static void assertVecEquals(Vec3 expected, Vec3 actual, double epsilon) {
        double dx = Math.abs(expected.x - actual.x);
        double dy = Math.abs(expected.y - actual.y);
        double dz = Math.abs(expected.z - actual.z);
        if (dx > epsilon || dy > epsilon || dz > epsilon) {
            throw mismatch("Vec3", expected.toString(), actual.toString(), epsilon, max(dx, dy, dz));
        }
    }

    public static void assertVecEquals(Vector3fc expected, Vector3fc actual, float epsilon) {
        double dx = Math.abs(expected.x() - actual.x());
        double dy = Math.abs(expected.y() - actual.y());
        double dz = Math.abs(expected.z() - actual.z());
        if (dx > epsilon || dy > epsilon || dz > epsilon) {
            throw mismatch("Vector3f", expected.toString(), actual.toString(), epsilon, max(dx, dy, dz));
        }
    }

    public static void assertVecEquals(Vector3dc expected, Vector3dc actual, double epsilon) {
        double dx = Math.abs(expected.x() - actual.x());
        double dy = Math.abs(expected.y() - actual.y());
        double dz = Math.abs(expected.z() - actual.z());
        if (dx > epsilon || dy > epsilon || dz > epsilon) {
            throw mismatch("Vector3d", expected.toString(), actual.toString(), epsilon, max(dx, dy, dz));
        }
    }

    public static void assertVecEquals(double[] expected, double[] actual, double epsilon) {
        if (expected.length != actual.length) {
            throw new AssertionError(
                    "vector length mismatch: expected " + expected.length + " components but was " + actual.length);
        }
        double maxDelta = 0;
        for (int i = 0; i < expected.length; i++) {
            maxDelta = Math.max(maxDelta, Math.abs(expected[i] - actual[i]));
        }
        if (maxDelta > epsilon) {
            throw mismatch("vector", Arrays.toString(expected), Arrays.toString(actual), epsilon, maxDelta);
        }
    }

    public static void assertRectEquals(Rect expected, Rect actual) {
        if (expected.x() != actual.x()
                || expected.y() != actual.y()
                || expected.width() != actual.width()
                || expected.height() != actual.height()) {
            throw new AssertionError("expected " + expected + " but was " + actual);
        }
    }

    public static void assertRectEquals(FloatRect expected, FloatRect actual, double epsilon) {
        double dx = Math.abs(expected.x() - actual.x());
        double dy = Math.abs(expected.y() - actual.y());
        double dw = Math.abs(expected.width() - actual.width());
        double dh = Math.abs(expected.height() - actual.height());
        if (dx > epsilon || dy > epsilon || dw > epsilon || dh > epsilon) {
            throw mismatch("FloatRect", expected.toString(), actual.toString(), epsilon, max(dx, dy, dw, dh));
        }
    }

    private static AssertionError mismatch(
            String kind, String expected, String actual, double epsilon, double maxDelta) {
        return new AssertionError(kind + " mismatch: expected " + expected + " but was " + actual + " (epsilon="
                + epsilon + ", maxDelta=" + maxDelta + ")");
    }

    private static double max(double... values) {
        double max = 0;
        for (double value : values) {
            max = Math.max(max, value);
        }
        return max;
    }
}
