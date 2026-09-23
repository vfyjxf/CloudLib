package dev.vfyjxf.cloudlib.testutil;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.ui.inworld.Projection;
import net.minecraft.world.phys.Vec3;
import org.joml.Matrix4f;
import org.joml.Vector3f;

/**
 * A programmable {@link Projection} factory for headless tests: configure a
 * camera — eye position, look-at target or Minecraft yaw/pitch, vertical fov,
 * depth range, gui-scaled screen size — and {@link #build()} produces a real
 * {@code Projection} whose matrices are computed with JOML. No Minecraft
 * client or render state is touched, so it works in plain unit tests.
 * <p>
 * The class also carries the round-trip contract the inworld geometry relies
 * on: projecting a world point to the screen and unprojecting the resulting
 * ray must reconstruct the original point within epsilon
 * ({@link #assertRoundTrip}), and a world point's screen position must match
 * the expected pixels ({@link #assertScreenOf}).
 * <p>
 * Yaw/pitch follow the Minecraft convention: yaw 0 looks toward +Z with yaw
 * growing counterclockwise, pitch +90 looks straight down; at ±90 pitch the
 * camera up vector is derived from yaw the way the vanilla camera does,
 * avoiding the look-at degeneracy of a world-space up axis.
 */
public final class ProjectionSimulator {

    private double eyeX;
    private double eyeY;
    private double eyeZ;
    private boolean explicitTarget = true;
    private double targetX;
    private double targetY = 0;
    private double targetZ = -1;
    private double yawDegrees;
    private double pitchDegrees;
    private double fovDegrees = 70;
    private double near = 0.05;
    private double far = 1000;
    private int screenWidth = 480;
    private int screenHeight = 270;

    private ProjectionSimulator() {}

    public static ProjectionSimulator create() {
        return new ProjectionSimulator();
    }

    /** {@code create().eye(x, y, z)}. */
    public static ProjectionSimulator at(double x, double y, double z) {
        return create().eye(x, y, z);
    }

    /** Places the camera eye; overrides the previous eye. */
    public ProjectionSimulator eye(double x, double y, double z) {
        this.eyeX = x;
        this.eyeY = y;
        this.eyeZ = z;
        return this;
    }

    /** Aims the camera at a world point; overrides any yaw/pitch setting. */
    public ProjectionSimulator lookAt(double x, double y, double z) {
        this.explicitTarget = true;
        this.targetX = x;
        this.targetY = y;
        this.targetZ = z;
        return this;
    }

    /** Aims the camera by Minecraft yaw/pitch; overrides any look-at target. */
    public ProjectionSimulator yawPitch(double yawDegrees, double pitchDegrees) {
        this.explicitTarget = false;
        this.yawDegrees = yawDegrees;
        this.pitchDegrees = pitchDegrees;
        return this;
    }

    /** Vertical field of view in degrees (the vanilla default is 70). */
    public ProjectionSimulator fov(double degrees) {
        this.fovDegrees = degrees;
        return this;
    }

    public ProjectionSimulator depthRange(double near, double far) {
        this.near = near;
        this.far = far;
        return this;
    }

    /** The gui-scaled screen size the projection maps into. */
    public ProjectionSimulator screen(int width, int height) {
        this.screenWidth = width;
        this.screenHeight = height;
        return this;
    }

    /** The unit forward direction the current settings look along. */
    public Vec3 forward() {
        if (explicitTarget) {
            return new Vec3(targetX - eyeX, targetY - eyeY, targetZ - eyeZ).normalize();
        }
        double yaw = Math.toRadians(yawDegrees);
        double pitch = Math.toRadians(pitchDegrees);
        return new Vec3(-Math.sin(yaw) * Math.cos(pitch), -Math.sin(pitch), Math.cos(yaw) * Math.cos(pitch));
    }

    /**
     * Builds the {@link Projection} from the current settings.
     *
     * @throws IllegalArgumentException when the screen size, fov or depth range is invalid
     */
    public Projection build() {
        if (screenWidth <= 0 || screenHeight <= 0) {
            throw new IllegalArgumentException("screen size must be positive: " + screenWidth + "x" + screenHeight);
        }
        if (fovDegrees <= 0 || fovDegrees >= 180) {
            throw new IllegalArgumentException("fov must be in (0, 180) degrees: " + fovDegrees);
        }
        if (near <= 0 || far <= near) {
            throw new IllegalArgumentException("depth range must satisfy 0 < near < far: " + near + ".." + far);
        }
        Vec3 forward = forward();
        Vec3 up = explicitTarget
                ? new Vec3(0, 1, 0)
                : rightFromYaw(Math.toRadians(yawDegrees)).cross(forward).normalize();
        Matrix4f worldToView = new Matrix4f().lookAt(
            new Vector3f((float) eyeX, (float) eyeY, (float) eyeZ),
            new Vector3f((float) (eyeX + forward.x), (float) (eyeY + forward.y), (float) (eyeZ + forward.z)),
            new Vector3f((float) up.x, (float) up.y, (float) up.z)
        );
        Matrix4f viewToClip = new Matrix4f().perspective(
            (float) Math.toRadians(fovDegrees),
            (float) screenWidth / screenHeight,
            (float) near,
            (float) far
        );
        return Projection.capture(worldToView, viewToClip, new Vec3(eyeX, eyeY, eyeZ), screenWidth, screenHeight);
    }

    /**
     * Asserts the world→screen→ray round trip: unprojecting the screen
     * position of {@code world} and walking the resulting ray for the point's
     * camera distance must land back on {@code world}.
     */
    public static void assertRoundTrip(Projection projection, Vec3 world, double epsilon) {
        FloatPos screen = projection.worldToScreen(world);
        if (screen == null) {
            throw new AssertionError("expected " + world + " to project on screen but it is behind the camera");
        }
        Vec3 reconstructed = projection.cameraPos()
                .add(projection.rayDirection(screen.x(), screen.y()).scale(projection.distance(world)));
        GeometryAsserts.assertVecEquals(world, reconstructed, epsilon);
    }

    /**
     * Asserts that {@code world} projects onto the expected gui-scaled screen
     * position.
     */
    public static void assertScreenOf(
        Projection projection,
        Vec3 world,
        double screenX,
        double screenY,
        double epsilon
    ) {
        FloatPos screen = projection.worldToScreen(world);
        if (screen == null) {
            throw new AssertionError(
                "expected " + world + " at (" + screenX + ", " + screenY + ") but it is behind the camera"
            );
        }
        GeometryAsserts.assertPosEquals(new FloatPos(screenX, screenY), screen, epsilon);
    }

    /** The camera right direction for a yaw, before any pitch rotation. */
    private static Vec3 rightFromYaw(double yaw) {
        return new Vec3(-Math.cos(yaw), 0, -Math.sin(yaw));
    }
}
