package dev.vfyjxf.cloudlib.api.ui.base;

import dev.vfyjxf.cloudlib.api.math.Pos;
import org.joml.Matrix3x2f;

/**
 * A single step in a {@link Viewport}'s transform pipeline.
 * <p>
 * Each {@code ViewportTransform} represents one affine operation (translate, scale, rotate,
 * layout offset, or arbitrary matrix) that participates in the viewport's composed transform.
 * The operations are applied in list order via post-multiplication when building the forward matrix.
 * <p>
 * Modelled after the CSS {@code transform} property: a viewport's transform is an
 * <em>ordered list</em> of {@code ViewportTransform} steps, and the order determines
 * the final result.  Every widget's pipeline starts with a {@link Layout} transform that
 * encodes the widget's position within its parent — the layout system updates this
 * automatically, making position "just another transform step".
 * <pre>{@code
 * // Pipeline: [Layout(10,20), Scale(2,2), Translate(-scrollX,-scrollY)]
 * // Forward matrix: T(10,20) · S(2,2) · T(-scrollX,-scrollY)
 * viewport.setUserTransforms(
 *     ViewportTransform.scale(2, 2),
 *     ViewportTransform.translate(-scrollX, -scrollY)
 * );
 * }</pre>
 * <p>
 * All implementations are immutable records. To "update" a transform, create a new
 * instance and replace it in the pipeline via {@link Viewport#setTransform(int, ViewportTransform)}.
 *
 * @see Viewport
 */
public sealed interface ViewportTransform {

    /**
     * Applies this transform to the given matrix by post-multiplication.
     * <p>
     * JOML uses post-multiply: {@code m.op()} ≡ {@code m = m · Op}.
     *
     * @param matrix the matrix to post-multiply into
     */
    void apply(Matrix3x2f matrix);

    //region factory methods

    /**
     * Creates a translation (scroll / pan) transform.
     */
    static Translate translate(double dx, double dy) {
        return new Translate(dx, dy);
    }

    /**
     * Creates a non-uniform scale transform.
     */
    static Scale scale(double sx, double sy) {
        return new Scale(sx, sy);
    }

    /**
     * Creates a uniform scale transform.
     */
    static Scale uniformScale(double s) {
        return new Scale(s, s);
    }

    /**
     * Creates a rotation transform.
     *
     * @param radians clockwise angle in radians
     */
    static Rotate rotate(double radians) {
        return new Rotate(radians);
    }

    /**
     * Creates a rotation transform from degrees.
     *
     * @param degrees clockwise angle in degrees
     */
    static Rotate rotateDegrees(double degrees) {
        return new Rotate(Math.toRadians(degrees));
    }

    /**
     * Creates a layout-position transform.
     * Semantically equivalent to a {@link Translate} but tagged as the layout offset
     * so the layout system can find and update it without affecting user-added transforms.
     *
     * @param x layout x offset in parent space
     * @param y layout y offset in parent space
     */
    static Layout layout(int x, int y) {
        return new Layout(x, y);
    }

    /**
     * Creates an arbitrary affine transform from a matrix. The matrix is defensively copied.
     */
    static Affine of(Matrix3x2f matrix) {
        return new Affine(new Matrix3x2f(matrix));
    }

    //endregion

    //region record types

    /**
     * The layout-position transform. Semantically equivalent to a {@link Translate}
     * but tagged so that the layout engine can identify and update it independently
     * of user-added transforms (scroll, zoom, etc.).
     * <p>
     * Every widget's viewport pipeline starts with a {@code Layout(0, 0)} by default.
     * The layout engine updates this transform when computing positions via
     * {@link Viewport#setLayout(int, int)}.
     * <p>
     * Origin wrapping is transparent to this transform because
     * {@code T(o) · T(layout) · T(-o) = T(layout)}.
     *
     * @param x layout x offset in parent space
     * @param y layout y offset in parent space
     */
    record Layout(int x, int y) implements ViewportTransform {
        /**
         * Creates a layout transform from a {@link Pos}.
         */
        public static Layout of(Pos pos) {
            return new Layout(pos.x(), pos.y());
        }

        @Override
        public void apply(Matrix3x2f matrix) {
            matrix.translate(x, y);
        }
    }

    /**
     * A 2D translation. Commonly used for scrolling / panning.
     *
     * @param dx horizontal translation
     * @param dy vertical translation
     */
    record Translate(double dx, double dy) implements ViewportTransform {
        @Override
        public void apply(Matrix3x2f matrix) {
            matrix.translate((float) dx, (float) dy);
        }
    }

    /**
     * A 2D scale transform.
     *
     * @param sx horizontal scale factor
     * @param sy vertical scale factor
     */
    record Scale(double sx, double sy) implements ViewportTransform {

        /**
         * Returns the uniform scale when {@code sx == sy}. Otherwise returns the geometric mean.
         */
        public double uniform() {
            return (sx == sy) ? sx : Math.sqrt(sx * sy);
        }

        @Override
        public void apply(Matrix3x2f matrix) {
            matrix.scale((float) sx, (float) sy);
        }
    }

    /**
     * A 2D rotation around the coordinate origin.
     * Typically combined with origin wrapping in {@link Viewport} so that the
     * rotation actually happens around the viewport's origin point.
     *
     * @param radians clockwise angle in radians
     */
    record Rotate(double radians) implements ViewportTransform {

        /**
         * Returns the angle in degrees.
         */
        public double degrees() {
            return Math.toDegrees(radians);
        }

        @Override
        public void apply(Matrix3x2f matrix) {
            matrix.rotate((float) radians);
        }
    }

    /**
     * An arbitrary 2D affine transform specified by a raw {@link Matrix3x2f}.
     * <p>
     * The stored matrix is a defensive copy and will not be modified by the
     * viewport. Use this for transforms that cannot be expressed as a simple
     * translate, scale, or rotate (e.g. shear/skew).
     *
     * @param matrix the 3×2 affine matrix
     */
    record Affine(Matrix3x2f matrix) implements ViewportTransform {

        /**
         * Defensive copy on construction.
         */
        public Affine {
            matrix = new Matrix3x2f(matrix);
        }

        @Override
        public void apply(Matrix3x2f mat) {
            mat.mul(matrix);
        }
    }

    //endregion
}
