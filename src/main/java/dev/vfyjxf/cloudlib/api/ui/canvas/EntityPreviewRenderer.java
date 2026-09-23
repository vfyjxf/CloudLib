package dev.vfyjxf.cloudlib.api.ui.canvas;

import com.mojang.blaze3d.platform.Lighting;
import com.mojang.blaze3d.systems.RenderSystem;
import com.mojang.blaze3d.vertex.PoseStack;
import dev.vfyjxf.cloudlib.api.text.EntityNode;
import net.minecraft.client.Minecraft;
import net.minecraft.client.gui.GuiGraphics;
import net.minecraft.client.gui.screens.inventory.InventoryScreen;
import net.minecraft.world.entity.Entity;
import net.minecraft.world.entity.LivingEntity;
import org.joml.Quaternionf;
import org.joml.Vector3f;
import org.jspecify.annotations.Nullable;

/**
 * Draws a live entity into a canvas box — the inventory-entity pipeline vanilla uses
 * for the player doll, shared by the rich-text {@link EntityNode} renderer and
 * {@code EntityPreviewWidget} so the technique lives in one place.
 * <p>
 * The box is what the caller reserved, and entities easily exceed it (a tall mob, a
 * wide one), so the draw is scissored <em>hard</em> to the box and then forwarded
 * through {@link SceneCanvas#renderLayered} — depth cleared, z-offset lifted — the
 * only path whose models survive the canvas's batched floor.
 * <p>
 * The entity is placed by its <b>projected</b> bounding box, never by its raw one:
 * the stance looks down at the model from {@link #presentCameraPitch} degrees, so
 * the box's height reaches the screen compressed by the cosine of that tilt and the
 * box's own footprint reaches {@code sin(tilt)·depth} px <em>below</em> the model's
 * feet — the two terms {@link #projectedBox} computes. Placement is
 * {@link #originY}: the projection stands on {@link #previewMargin} px of floor, and
 * a projection too tall for the box is centred instead of sheared off at the ankles.
 * The auto-fit ({@code scale <= 0}) fits the same projection, so a box that fits an
 * entity at all fits it entirely, feet included.
 * <p>
 * {@code followMouse} mirrors the vanilla inventory doll: the body and head yaw and
 * the camera pitch track the pointer's offset from the box center. Without it the
 * preview stands at a fixed presentation stance built from the numbers a GUI block
 * icon is drawn with, so an entity and the icon beside it read as one system: the
 * camera high on the entity's three-quarter corner, looking down at it, the model's
 * facing running toward the lower-right of the box exactly as a block icon's front
 * does. Never at whatever direction the live entity happens to face. In both cases
 * the render rotations are set for the draw and restored after it, so a preview
 * never re-poses the entity in the world. Vanilla's
 * {@code renderEntityInInventoryFollowsMouse} installs a screen-space scissor that
 * would clip the entity away inside the canvas's forwarded transform, so the angle
 * math is reproduced and the draw goes through the plain
 * {@link InventoryScreen#renderEntityInInventory} overload instead. Angles are
 * translation-invariant, which is why computing them from the box center and the
 * pointer in the same local space is equivalent to vanilla.
 * <p>
 * Both branches own their lighting as well: the inventory-entity lights around the
 * draw, and the gui 3D-item ambient handed back afterwards — the ambient a canvas
 * paints under and the one a neighbouring block icon is lit by.
 */
public final class EntityPreviewRenderer {

    /** The packed light value that keeps a preview fully lit regardless of the world. */
    private static final int fullBright = 15728880;

    /**
     * The body yaw of the fixed presentation stance, in degrees. The inventory pipeline turns the model by
     * {@code rotateY(180 - yaw)} and the stance's pose folds a half-turn of its own into that chain, so 135
     * leaves a 45° model turn — the block icon's {@code rotateY(225)} carrying the same half-turn:
     * {@code 180 - 135 = 45 = 225 - 180}. With {@link #presentCameraPitch} below, the preview's whole
     * model→screen rotation is then identical to the block icon's, front included (an entity model is
     * authored facing -Z, the block model's own north). Package-private, like its two siblings, so that
     * equivalence stays checkable without a live client.
     */
    static final float presentBodyYaw = 135f;

    /** The head yaw of the fixed presentation stance: the head faces where the body does. */
    static final float presentHeadYaw = 135f;

    /**
     * The camera tilt of the fixed presentation stance, in degrees — negative, and the sign is the whole
     * point. The stance's pose composes {@code rotateZ(π)} before this tilt and the caller then mirrors Z,
     * a pair that reverses a tilt's sense: a positive angle here leans the model's top <em>away</em> from
     * the viewer, putting the camera underneath the entity with its facing running to the upper-left — the
     * block icon's view mirrored through a worm's-eye camera. -30 is the block icon's own 30° camera
     * elevation, and it is also vanilla's side of the mouse convention that looks down at the doll:
     * {@code renderEntityInInventoryFollowsMouse} passes a negative tilt whenever the pointer sits below
     * the box center.
     */
    static final float presentCameraPitch = -30f;

    /**
     * The px of air kept between the reserved box and the entity drawn inside it. The fit and the placement
     * both leave it, so a preview never touches the edge it is scissored at.
     */
    static final int previewMargin = 1;

    /**
     * √2 — the diagonal of a square, and with it the footprint a bounding box of width {@code w} sweeps once a
     * yaw turns it. The stance's 45° model turn is the extreme case; every other yaw stays inside the same
     * square, which is what makes the projection below independent of the direction the entity happens to
     * face.
     */
    private static final float footprintDiagonal = (float) Math.sqrt(2.0);

    private EntityPreviewRenderer() {}

    /**
     * Renders {@code entity} into the {@code width}×{@code height} box at ({@code x},
     * {@code y}), clipped to it.
     *
     * @param canvas       the canvas to forward the draw through
     * @param entity       the live entity, or {@code null} (draws nothing)
     * @param x            box left, in the canvas's current local space
     * @param y            box top
     * @param width        box width; a non-positive box draws nothing
     * @param height       box height
     * @param scale        render scale in pixels per block; {@code 0} (or any negative) fits the entity's
     *                     own bounding box — as the stance camera projects it — into the box instead — the
     *                     dynamic answer that keeps a golem and a chicken equally readable
     * @param followMouse  whether the body/head track the pointer (living entities only)
     * @param mouseX       pointer x in the same local space as the box
     * @param mouseY       pointer y
     * @param partialTicks frame partial ticks, for non-living entities' interpolated pose
     */
    public static void render(
        SceneCanvas canvas,
        @Nullable Entity entity,
        int x,
        int y,
        int width,
        int height,
        int scale,
        boolean followMouse,
        float mouseX,
        float mouseY,
        float partialTicks
    ) {
        if (entity == null || width <= 0 || height <= 0) {
            return;
        }
        int effectiveScale = scale > 0 ? scale : fitScaleFor(entity, width, height);
        canvas.withClip(x, y, width, height, () -> canvas.renderLayered(graphics -> {
            if (entity instanceof LivingEntity living) {
                renderLiving(graphics, living, x, y, width, height, effectiveScale, followMouse, mouseX, mouseY);
            } else {
                renderSimple(graphics, entity, x, y, width, height, effectiveScale, partialTicks);
            }
        }));
    }

    /**
     * The pixels-per-block an entity fits its box at — {@code scale <= 0}'s answer, at the fixed presentation
     * stance ({@link #presentCameraPitch}) and an unscaled entity. The box the fit works against is the
     * bounding box <em>projected</em> under that stance: a tilted camera compresses the model's height and
     * folds the footprint it stands on into the same screen axis, so the raw box measures the wrong thing —
     * fitting {@code bbHeight} px of height is how a preview ends up with its feet outside the box.
     */
    public static int fitScale(float bbWidth, float bbHeight, int width, int height) {
        return fitScale(projectedBox(bbWidth, bbHeight, presentCameraPitch), width, height);
    }

    /**
     * The pixels-per-block at which a projected box fits a {@code width}×{@code height} box, with
     * {@link #previewMargin} px kept free on every side; the tighter of the two axes wins, so a golem and a
     * chicken fill the same box and neither is clipped. The projection is the input because the fit has to
     * know the camera and the entity's own scale attribute: a caller that has both scales the projection it
     * passes ({@code projectedBox(…).scaled(scale)}), exactly the way a draw's pixels are
     * {@code scale × entity scale}.
     */
    public static int fitScale(ProjectedBox projected, int width, int height) {
        float byWidth = (width - 2f * previewMargin) / Math.max(projected.width(), 0.05f);
        float byHeight = (height - 2f * previewMargin) / Math.max(projected.height(), 0.05f);
        return Math.max(1, (int) Math.min(byWidth, byHeight));
    }

    /**
     * {@code scale <= 0}'s fit for a live entity: a living entity's box is projected under the stance's tilt
     * and its own scale attribute (the inventory pipeline scales the model by it, so it multiplies the
     * pixels), while a non-living one is drawn straight on — no tilt, no model turn — and keeps the untilted
     * fit it has always had.
     */
    private static int fitScaleFor(Entity entity, int width, int height) {
        if (entity instanceof LivingEntity living) {
            ProjectedBox projected = projectedBox(living.getBbWidth(), living.getBbHeight(), presentCameraPitch)
                    .scaled(living.getScale());
            return fitScale(projected, width, height);
        }
        return untiltedFitScale(entity.getBbWidth(), entity.getBbHeight(), width, height);
    }

    /**
     * The pixels-per-block an untilted draw fits its box at. The straight-on branch reserves the bounding box
     * itself (a camera that looks along the horizon projects height to height) and keeps the headroom this
     * fit has always carried: a model's arms and rods run wider than its box, so the width is given more air
     * than the height.
     */
    private static int untiltedFitScale(float bbWidth, float bbHeight, int width, int height) {
        float byWidth = width / Math.max(bbWidth * 1.25f, 0.05f);
        float byHeight = height / Math.max(bbHeight * 1.05f, 0.05f);
        return Math.max(1, (int) Math.min(byWidth, byHeight));
    }

    // region projection (pure — the headless tests drive these)

    /**
     * The box an entity's bounding box projects to under a camera tilt of {@code cameraPitchDegrees}, in
     * pixels at one pixel-per-block of render scale; multiply it by the render scale (and by the entity's own
     * scale attribute, which the inventory pipeline applies inside the draw) for the pixels a real draw
     * covers.
     * <p>
     * The derivation, in the pipeline's own chain — {@code translate(0, bbHeight/2, 0)} then
     * {@code pose = rotateZ(π)·rotateX(tilt)} then the model, all under {@code scale(s, s, -s)}. The pose's
     * y row is {@code (0, -cos(tilt), sin(tilt))}, and the model turn the dispatcher applies from the body
     * yaw moves a point's {@code x}/{@code z} around without changing its {@code y}, so a model point
     * ({@code y} up, {@code z} toward the viewer) lands at screen
     * {@code y = origin - (cos(tilt)·y + sin(tilt)·z)} for the anchor that puts the box origin at
     * {@code origin}. Two consequences, both of them what the untilted arithmetic missed:
     * <ul>
     * <li>a bounding box of height {@code h} covers {@code cos(tilt)·h} px <em>above</em> its own origin, not
     * {@code h} — at 30° a model is 13% shorter on screen than its box says;</li>
     * <li>the footprint the model stands on reaches {@code sin(tilt)·depth} px <em>below</em> that origin —
     * at 30°, half the footprint's depth, which is the strip of feet the bottom-aligned draw used to scissor
     * off.</li>
     * </ul>
     * The depth is the box's own width taken through the footprint diagonal (√2) — the square a yaw sweeps —
     * so no direction the entity is turned to clips. A tilt of 0 projects the box to exactly its own height,
     * which is the one case the raw bounding box was ever right about.
     *
     * @param bbWidth            the entity's bounding-box width, in blocks
     * @param bbHeight           the entity's bounding-box height, in blocks
     * @param cameraPitchDegrees the stance camera's tilt; its sign only decides which side of the box faces
     *                           the viewer, never how much of the screen the box covers
     * @return the projection: {@code width} px across, {@code up}/{@code down} px of reach above/below the
     *         box's origin
     */
    public static ProjectedBox projectedBox(float bbWidth, float bbHeight, float cameraPitchDegrees) {
        double tilt = Math.toRadians(cameraPitchDegrees);
        float compression = (float) Math.abs(Math.cos(tilt));
        float depthShare = (float) Math.abs(Math.sin(tilt));
        float width = footprintDiagonal * Math.max(bbWidth, 0f);
        float below = depthShare * width / 2f;
        return new ProjectedBox(width, compression * Math.max(bbHeight, 0f) + below, below);
    }

    /**
     * The screen y the entity's origin — the centre of its bounding box's bottom face, the point the
     * pipeline's {@code (0, bbHeight/2, 0)} translate is measured from — has to be drawn at so the projected
     * box stands inside a box whose top is {@code boxTop} and whose height is {@code boxHeight}:
     * bottom-aligned {@code margin} px up off the floor, or, when the projection is taller than the box can
     * hold, centred — the symmetric cut a fixed {@code scale} that overflows deserves, since the overflow
     * itself cannot be helped but the feet do not have to be what pays for it.
     */
    public static float originY(int boxTop, int boxHeight, ProjectedBox projected, int margin) {
        if (projected.height() <= boxHeight - 2f * margin) {
            return boxTop + boxHeight - margin - projected.down();
        }
        return boxTop + boxHeight / 2f + (projected.up() - projected.down()) / 2f;
    }

    /**
     * The {@code y} the inventory pipeline has to be handed for an origin at {@link #originY}. The pipeline
     * translates by {@code bbHeight/2} inside its own scaled frame before the pose, so the anchor sits half a
     * bounding box — {@code bbHeight × pixelsPerBlock / 2} px — above the origin the model is really drawn
     * at: {@code anchorY + bbHeight·pixelsPerBlock/2 = originY}, exactly, which is the pair the placement
     * relies on.
     *
     * @param originY         the screen y the box's origin must be drawn at
     * @param bbHeight        the entity's bounding-box height, in blocks
     * @param pixelsPerBlock  the render scale the pipeline itself is handed — never that scale times the
     *                        entity's own attribute: the translate this undoes runs in the outer frame,
     *                        where the attribute has not been applied yet
     */
    public static float anchorY(float originY, float bbHeight, float pixelsPerBlock) {
        return originY - bbHeight * pixelsPerBlock / 2f;
    }

    /**
     * A bounding box as a stance camera projects it: {@code width} px across, {@code up} px of reach above
     * the box's origin and {@code down} px below it. {@link #height()} is the px it covers vertically.
     */
    public record ProjectedBox(float width, float up, float down) {

        /** The same projection at another render scale — the entity's own scale attribute, say. */
        public ProjectedBox scaled(float factor) {
            return new ProjectedBox(width * factor, up * factor, down * factor);
        }

        /** The px the projection covers vertically: {@link #up} and {@link #down} together. */
        public float height() {
            return up + down;
        }
    }

    // endregion

    private static void renderLiving(
        GuiGraphics graphics,
        LivingEntity entity,
        int x,
        int y,
        int width,
        int height,
        int scale,
        boolean followMouse,
        float mouseX,
        float mouseY
    ) {
        // Stand the entity on the reserved floor by its *projection*: the box the caller reserved holds the
        // bounding box as the stance camera flattens it, feet included — see projectedBox/originY.
        float pixelsPerBlock = scale * entity.getScale();
        float cx = x + width / 2f;
        Quaternionf pose = new Quaternionf().rotateZ((float) Math.PI);
        Quaternionf camera;
        // the rotations below are render inputs the inventory pipeline reads straight off the entity; they
        // are set for the draw and restored after it, so a preview never leaves its stance on the live body
        float bodyRot = entity.yBodyRot;
        float yRot = entity.getYRot();
        float xRot = entity.getXRot();
        float headRot = entity.yHeadRot;
        float headRotO = entity.yHeadRotO;
        float tilt;
        if (followMouse) {
            float yaw = (float) Math.atan((cx - mouseX) / 40.0);
            // vanilla reads the pointer against the box's centre, and the placement needs the tilt the pose
            // will carry before it can place anything, so the centre is the pivot here as well — anchoring
            // the angles on the drawn origin instead would make the pitch depend on the placement that
            // depends on the pitch
            float pitch = (float) Math.atan((y + height / 2f - mouseY) / 40.0);
            tilt = pitch * 20f;
            camera = new Quaternionf().rotateX(tilt * (float) (Math.PI / 180.0));
            pose.mul(camera);
            entity.yBodyRot = 180.0f + yaw * 20.0f;
            entity.setYRot(180.0f + yaw * 40.0f);
            entity.setXRot(-pitch * 20.0f);
            entity.yHeadRot = entity.getYRot();
            entity.yHeadRotO = entity.getYRot();
        } else {
            tilt = presentCameraPitch;
            camera = new Quaternionf().rotateX(tilt * (float) (Math.PI / 180.0));
            pose.mul(camera);
            entity.yBodyRot = presentBodyYaw;
            entity.setYRot(presentHeadYaw);
            entity.setXRot(0f);
            entity.yHeadRot = presentHeadYaw;
            entity.yHeadRotO = presentHeadYaw;
        }
        ProjectedBox projected = projectedBox(entity.getBbWidth(), entity.getBbHeight(), tilt).scaled(pixelsPerBlock);
        float originY = originY(y, height, projected, previewMargin);
        // the anchor is converted with the pipeline's own scale, never with pixelsPerBlock: the translate it
        // undoes runs in the outer frame, where the entity's scale attribute has not been applied yet
        float cy = anchorY(originY, entity.getBbHeight(), scale);
        try {
            // the inventory lights are the doll's own, and vanilla's overload installs them before the draw
            // and hands back the gui 3D-item ambient after it; repeating the install and doing the hand-back
            // in a finally keeps this branch symmetric with renderSimple and keeps a failed draw from leaving
            // the entity lights on for the rest of the canvas
            Lighting.setupForEntityInInventory();
            InventoryScreen.renderEntityInInventory(
                graphics,
                cx,
                cy,
                scale,
                new Vector3f(0, entity.getBbHeight() / 2f, 0),
                pose,
                camera,
                entity
            );
        } finally {
            entity.yBodyRot = bodyRot;
            entity.setYRot(yRot);
            entity.setXRot(xRot);
            entity.yHeadRot = headRot;
            entity.yHeadRotO = headRotO;
            Lighting.setupFor3DItems();
        }
    }

    private static void renderSimple(
        GuiGraphics graphics,
        Entity entity,
        int x,
        int y,
        int width,
        int height,
        int scale,
        float partialTicks
    ) {
        PoseStack pose = graphics.pose();
        pose.pushPose();
        try {
            pose.translate(x + width / 2f, y + height / 2f + scale * entity.getBbHeight() / 2f, 100f);
            pose.scale(scale, scale, -scale);
            Lighting.setupForEntityInInventory();
            var dispatcher = Minecraft.getInstance().getEntityRenderDispatcher();
            dispatcher.setRenderShadow(false);
            RenderSystem.runAsFancy(
                () -> dispatcher.render(entity, 0, 0, 0, 0f, partialTicks, pose, graphics.bufferSource(), fullBright)
            );
            graphics.flush();
            dispatcher.setRenderShadow(true);
        } finally {
            pose.popPose();
            Lighting.setupFor3DItems();
        }
    }
}
