package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import net.minecraft.world.phys.Vec3;

import java.util.Arrays;
import java.util.List;

/**
 * A {@link Projector} backed by a captured view-projection matrix —
 * column-major {@code double[16]}, as JOML/Mojang emit. Use this when
 * the host's projection is not a plain pinhole (shaders, custom fov,
 * mc's own projection matrix).
 * <p>
 * {@code zeroToOne} selects the clip-depth convention: {@code true}
 * for [0,1] depth (DirectX-style), {@code false} for OpenGL [-1,1].
 */
public final class MatrixCamera implements Projector {

    final Vec3 eye;
    final double[] matrix;
    final double[] inverse;
    final double width;
    final double height;
    final boolean zeroToOne;

    public MatrixCamera(Vec3 eye, double[] viewProjection, double width, double height, boolean zeroToOne) {
        if (viewProjection.length != 16 || width <= 0.0 || height <= 0.0) {
            throw new IllegalArgumentException();
        }
        this.eye = eye;
        this.matrix = viewProjection.clone();
        this.inverse = invert(this.matrix);
        this.width = width;
        this.height = height;
        this.zeroToOne = zeroToOne;
        if (Math.abs(this.inverse[11]) < 1.0E-12) {
            throw new IllegalArgumentException("perspective matrix required");
        }
    }

    /** A stable identity for frame-reuse caching — the matrix itself is mutable-copied. */
    public List<Object> cacheKey() {
        return List.of(
                eye, width, height, zeroToOne, Arrays.stream(matrix).boxed().toList());
    }

    @Override
    public Vec3 eye() {
        return eye;
    }

    @Override
    public double width() {
        return width;
    }

    @Override
    public double height() {
        return height;
    }

    @Override
    public Projection project(Vec3 point) {
        Vec3 rel = point.subtract(eye);
        double[] clip = transform(matrix, rel.x, rel.y, rel.z, 1.0);
        boolean inFront = Double.isFinite(clip[3])
                && clip[3] > 1.0E-8
                && clip[2] > (zeroToOne ? 0.0 : -clip[3])
                && clip[2] < clip[3];
        if (!inFront) {
            return new Projection(GuiVec.zero, clip[3], false);
        }
        return new Projection(
                new GuiVec((clip[0] / clip[3] + 1.0) * width * 0.5, (1.0 - clip[1] / clip[3]) * height * 0.5),
                clip[3],
                true);
    }

    @Override
    public Vec3 unproject(GuiVec point, double depth) {
        double ndcX = (2.0 * point.x() / width - 1.0) * depth;
        double ndcY = (1.0 - 2.0 * point.y() / height) * depth;
        double ndcZ = (1.0 - inverse[3] * ndcX - inverse[7] * ndcY - inverse[15] * depth) / inverse[11];
        double[] view = transform(inverse, ndcX, ndcY, ndcZ, depth);
        return eye.add(new Vec3(view[0] / view[3], view[1] / view[3], view[2] / view[3]));
    }

    static double[] transform(double[] m, double x, double y, double z, double w) {
        double[] out = new double[4];
        for (int i = 0; i < 4; i++) {
            out[i] = m[i] * x + m[4 + i] * y + m[8 + i] * z + m[12 + i] * w;
        }
        return out;
    }

    static double[] invert(double[] m) {
        double[][] aug = new double[4][8];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                aug[row][col] = m[col * 4 + row];
            }
            aug[row][4 + row] = 1.0;
        }

        for (int col = 0; col < 4; col++) {
            int pivot = col;
            for (int row = col + 1; row < 4; row++) {
                if (Math.abs(aug[row][col]) > Math.abs(aug[pivot][col])) {
                    pivot = row;
                }
            }
            if (Math.abs(aug[pivot][col]) < 1.0E-14) {
                throw new IllegalArgumentException("singular matrix");
            }
            double[] swap = aug[col];
            aug[col] = aug[pivot];
            aug[pivot] = swap;
            double scale = aug[col][col];
            for (int j = 0; j < 8; j++) {
                aug[col][j] /= scale;
            }
            for (int row = 0; row < 4; row++) {
                if (row != col) {
                    double factor = aug[row][col];
                    for (int j = 0; j < 8; j++) {
                        aug[row][j] -= factor * aug[col][j];
                    }
                }
            }
        }

        double[] out = new double[16];
        for (int row = 0; row < 4; row++) {
            for (int col = 0; col < 4; col++) {
                out[col * 4 + row] = aug[row][4 + col];
            }
        }
        return out;
    }
}
