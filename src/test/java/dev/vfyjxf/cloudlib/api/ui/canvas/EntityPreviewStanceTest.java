package dev.vfyjxf.cloudlib.api.ui.canvas;

import org.joml.Matrix4f;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.junit.jupiter.api.Test;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The fixed presentation stance of {@link EntityPreviewRenderer} — the headless half of it. The stance
 * exists so that a preview reads as one system with the GUI block icon beside it, and this pins that claim
 * as arithmetic instead of taste.
 * <p>
 * Both chains below are the ones the two renderers really run, in the same gui space (x right, y down, z
 * toward the viewer — the space the canvas and the vanilla GUI pass share):
 * <ul>
 * <li>a block icon is {@code scale(s, -s, s) · rotateX(30°) · rotateY(225°)}, vanilla's
 * {@code ItemTransform} gui rotation, the chain the rich-text block renderer reproduces;</li>
 * <li>a fixed-stance entity is {@code scale(s, s, -s) · pose · rotateY(180 - yaw)} — vanilla's inventory
 * pipeline, where {@code pose} is {@code rotateZ(π)} times the presentation tilt.</li>
 * </ul>
 * Equal matrices mean the entity is turned, tilted and looked at exactly like the icon; the direction
 * assertions pin the two properties a viewer reads off the picture: the camera sits above the entity, and
 * the entity's facing runs toward the lower-right of the box.
 */
class EntityPreviewStanceTest {

    /** Vanilla's {@code ItemTransform} gui rotation for a block model. */
    private static final float blockIconTilt = 30f;

    private static final float blockIconYaw = 225f;

    private static float rad(float degrees) {
        return (float) Math.toRadians(degrees);
    }

    /** The block icon's model→screen rotation: {@code scale(s, -s, s) · rotateX(30) · rotateY(225)}. */
    private static Matrix4f blockIcon() {
        return new Matrix4f().scale(1f, -1f, 1f).rotateX(rad(blockIconTilt)).rotateY(rad(blockIconYaw));
    }

    /**
     * The entity preview's model→screen rotation for a stance: the pose the renderer hands vanilla
     * ({@code rotateZ(π) · rotateX(tilt)}) followed by the model turn vanilla applies from the entity's yaw.
     */
    private static Matrix4f entityStance(float bodyYaw, float cameraPitch) {
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        pose.mul(new Quaternionf().rotateX(rad(cameraPitch)));
        return new Matrix4f().scale(1f, 1f, -1f).rotate(pose).rotateY(rad(180f - bodyYaw));
    }

    /** The stance under test, built from the renderer's own constants. */
    private static Matrix4f presentStance() {
        return entityStance(EntityPreviewRenderer.presentBodyYaw, EntityPreviewRenderer.presentCameraPitch);
    }

    /** Where a model direction ends up on screen, normalised. */
    private static Vector3f screen(Matrix4f modelToScreen, float x, float y, float z) {
        return modelToScreen.transformDirection(new Vector3f(x, y, z)).normalize();
    }

    /** The model-frame direction that points at the viewer: the pre-image of the gui's +z. */
    private static Vector3f cameraDirection(Matrix4f modelToScreen) {
        return modelToScreen.invert(new Matrix4f()).transformDirection(new Vector3f(0f, 0f, 1f)).normalize();
    }

    @Test
    void theStanceTurnsAndTiltsTheEntityExactlyLikeABlockIcon() {
        // not "similar to": the same matrix, so the body's 135 is the block's 225 carrying the pose's own
        // half-turn (180 - 135 = 45 = 225 - 180) and the tilt's -30 is the block icon's 30 seen through the
        // pose's rotateZ(pi) and the pipeline's z mirror, which reverse a tilt's sense
        Matrix4f block = blockIcon();
        Matrix4f entity = presentStance();
        assertTrue(
            entity.equals(block, 1e-5f),
            "the fixed stance's model→screen rotation must equal the block icon's,\n block=" + block + "\n entity="
                    + entity
        );
    }

    @Test
    void theCameraSitsThirtyDegreesAboveTheEntityAtTheIconsCorner() {
        Vector3f camera = cameraDirection(presentStance());
        // 30 degrees up (sin 30 = 0.5) on the entity's +X/front (-Z) corner: the same vantage the block icon
        // is drawn from — above and to the side, never below
        assertEquals(0.5f, camera.y(), 1e-4f, "the camera is 30° above the entity's horizon");
        assertEquals(0.6124f, camera.x(), 1e-4f, "…45° off the facing, on its +X side");
        assertEquals(-0.6124f, camera.z(), 1e-4f, "…and in front of it, so the front is what is seen");
    }

    @Test
    void aPositiveTiltWouldPutTheCameraUnderneath() {
        // the sign convention the constant exists for: the same stance with the shipped-sign tilt looks up at
        // the entity from below, which is what "the direction is still wrong" was reading
        Vector3f camera = cameraDirection(entityStance(EntityPreviewRenderer.presentBodyYaw, 30f));
        assertTrue(camera.y() < 0f, "a +30 tilt leaves the camera under the entity, not above it");
    }

    @Test
    void theFacingRunsTowardTheLowerRightAndTheEntityStaysUpright() {
        Matrix4f stance = presentStance();
        Vector3f up = screen(stance, 0f, 1f, 0f);
        assertTrue(up.y() < 0f, "the model's up still reads as screen up");
        assertTrue(up.z() > 0f, "…and leans toward the viewer, which is what shows the top");

        Vector3f facing = screen(stance, 0f, 0f, -1f); // entity models are authored facing -Z
        assertTrue(facing.x() > 0f, "the facing runs to the screen's right");
        assertTrue(facing.y() > 0f, "…and down, as a horizontal direction toward the camera does from above");
        assertTrue(facing.z() > 0f, "…while still pointing at the viewer, so the front is visible, not the back");
    }
}
