package dev.vfyjxf.cloudlib.testutil;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Rect;
import net.minecraft.world.phys.Vec3;
import org.joml.Vector3d;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

class GeometryAssertsTest {

    @Test
    void exactPosAssertionPassesOnEqualAndFailsOnDifferent() {
        GeometryAsserts.assertPosEquals(new Pos(3, 4), new Pos(3, 4));

        AssertionError error =
                assertThrows(AssertionError.class, () -> GeometryAsserts.assertPosEquals(new Pos(3, 4), new Pos(3, 5)));
        assertTrue(error.getMessage().contains("expected Pos[x=3, y=4]"));
    }

    @Test
    void floatPosAssertionHonorsEpsilonAndReportsDelta() {
        GeometryAsserts.assertPosEquals(new FloatPos(1, 2), new FloatPos(1 + 1.0e-7, 2 - 1.0e-7), 1.0e-6);

        AssertionError error = assertThrows(
                AssertionError.class,
                () -> GeometryAsserts.assertPosEquals(new FloatPos(1, 2), new FloatPos(1.25, 2), 0.1));
        assertTrue(error.getMessage().contains("epsilon=0.1"));
        assertTrue(error.getMessage().contains("maxDelta=0.25"));
    }

    @Test
    void vec3AssertionComparesAllComponents() {
        GeometryAsserts.assertVecEquals(new Vec3(1, 2, 3), new Vec3(1, 2 + 1.0e-9, 3), 1.0e-6);

        assertThrows(
                AssertionError.class,
                () -> GeometryAsserts.assertVecEquals(new Vec3(1, 2, 3), new Vec3(1, 2, 3 + 0.5), 0.1));
        assertThrows(
                AssertionError.class,
                () -> GeometryAsserts.assertVecEquals(new Vec3(1, 2, 3), new Vec3(1, 2 + 0.5, 3), 0.1));
    }

    @Test
    void jomlFloatVecAssertionHonorsEpsilon() {
        GeometryAsserts.assertVecEquals(new Vector3f(0.5f, 0, 0), new Vector3f(0.5f, 1.0e-7f, 0), 1.0e-6f);

        assertThrows(
                AssertionError.class,
                () -> GeometryAsserts.assertVecEquals(new Vector3f(1, 0, 0), new Vector3f(1.5f, 0, 0), 0.1f));
    }

    @Test
    void jomlDoubleVecAssertionHonorsEpsilon() {
        GeometryAsserts.assertVecEquals(new Vector3d(1, 2, 3), new Vector3d(1, 2, 3 + 1.0e-9), 1.0e-6);

        assertThrows(
                AssertionError.class,
                () -> GeometryAsserts.assertVecEquals(new Vector3d(1, 2, 3), new Vector3d(1, 2.5, 3), 0.1));
    }

    @Test
    void componentArrayAssertionChecksLengthAndComponents() {
        GeometryAsserts.assertVecEquals(new double[] {1, 2}, new double[] {1 + 1.0e-7, 2}, 1.0e-6);
        GeometryAsserts.assertVecEquals(new double[0], new double[0], 1.0e-6);

        AssertionError mismatch = assertThrows(
                AssertionError.class,
                () -> GeometryAsserts.assertVecEquals(new double[] {1, 2}, new double[] {1, 2.5}, 0.1));
        assertTrue(mismatch.getMessage().contains("maxDelta=0.5"));

        AssertionError length = assertThrows(
                AssertionError.class,
                () -> GeometryAsserts.assertVecEquals(new double[] {1, 2}, new double[] {1, 2, 3}, 0.1));
        assertTrue(length.getMessage().contains("length mismatch"));
    }

    @Test
    void exactRectAssertionPassesOnEqualAndFailsOnAnyComponent() {
        GeometryAsserts.assertRectEquals(new Rect(1, 2, 3, 4), new Rect(1, 2, 3, 4));

        assertThrows(
                AssertionError.class,
                () -> GeometryAsserts.assertRectEquals(new Rect(1, 2, 3, 4), new Rect(1, 2, 3, 5)));
        assertThrows(
                AssertionError.class,
                () -> GeometryAsserts.assertRectEquals(new Rect(1, 2, 3, 4), new Rect(0, 2, 3, 4)));
    }

    @Test
    void floatRectAssertionHonorsEpsilonOnAllEdges() {
        FloatRect expected = new FloatRect(10, 20, 30, 40);
        GeometryAsserts.assertRectEquals(expected, new FloatRect(10 + 1.0e-7, 20, 30, 40 - 1.0e-7), 1.0e-6);
        // a delta of exactly epsilon is still within tolerance
        GeometryAsserts.assertRectEquals(expected, new FloatRect(10, 20, 30, 40.5), 0.5);

        assertThrows(
                AssertionError.class,
                () -> GeometryAsserts.assertRectEquals(expected, new FloatRect(10, 20, 30, 40 + 0.2), 0.1));
        assertThrows(
                AssertionError.class,
                () -> GeometryAsserts.assertRectEquals(expected, new FloatRect(10 + 0.2, 20, 30, 40), 0.1));
    }
}
