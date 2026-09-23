package dev.vfyjxf.cloudlib.api.ui.canvas;

import dev.vfyjxf.cloudlib.api.ui.canvas.EntityPreviewRenderer.ProjectedBox;
import org.junit.jupiter.api.Test;

import java.util.List;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The vertical placement of a preview — the headless arithmetic behind "the entity sits too low and its legs
 * are cut off". The pipeline bottom-aligns the model, and the placement used to do that on the raw bounding
 * box: a box of height {@code h} was assumed to cover {@code h} px of screen. It does not. The stance camera
 * looks down at the model from {@code presentCameraPitch} degrees, so the box reaches only {@code cos(tilt)·h}
 * px up while the footprint the model stands on reaches {@code sin(tilt)·depth} px <em>below</em> its own
 * origin: half the footprint's depth of feet was scissored away, the model read as sitting low, and the same
 * error left an equal gap of unused box under its top.
 * <p>
 * These tests assert on {@link EntityPreviewRenderer#projectedBox}, the fit and the placement as one system:
 * the placed origin the fit was computed for has to keep the whole projection inside the box, for every
 * bounding box and every reserved box, standing on the margin with its head clear of the top.
 */
class EntityPreviewProjectionTest {

    /** The margin the fit and the placement share, spelled out so the assertions read as geometry. */
    private static final int margin = EntityPreviewRenderer.previewMargin;

    /** Bounding boxes across the shapes a preview sees: a chicken, a player, a cow, a spider, a golem. */
    private static final List<float[]> boundingBoxes = List.of(
        new float[]{0.3f, 0.4f},
        new float[]{0.6f, 1.8f},
        new float[]{0.9f, 1.4f},
        new float[]{1.4f, 0.9f},
        new float[]{3.0f, 3.0f}
    );

    /** The reserved boxes: the panel portrait, a rich-text inline box, a square one, a small one. */
    private static final List<int[]> boxes = List
            .of(new int[]{22, 28}, new int[]{40, 30}, new int[]{64, 64}, new int[]{12, 12});

    /** Camera tilts, in degrees: the fixed stance, the extremes the follow-mouse branch reaches, and level. */
    private static final List<Float> tilts = List
            .of(EntityPreviewRenderer.presentCameraPitch, -80f, -45f, 0f, 45f, 80f);

    /** The top of the box the placement tests stand in — non-zero, so an off-by-a-box error cannot hide. */
    private static final int boxTop = 7;

    private static ProjectedBox projected(float bbWidth, float bbHeight, float tilt, float pixelsPerBlock) {
        return EntityPreviewRenderer.projectedBox(bbWidth, bbHeight, tilt).scaled(pixelsPerBlock);
    }

    // region the projection

    @Test
    void aLevelCameraProjectsTheBoxToItsOwnHeight() {
        ProjectedBox flat = EntityPreviewRenderer.projectedBox(0.6f, 1.8f, 0f);
        assertEquals(1.8f, flat.up(), 1e-4f, "level: the box's height is its own screen height");
        assertEquals(0f, flat.down(), 1e-4f, "…and nothing of the box reaches below its origin");
        assertEquals(1.8f, flat.height(), 1e-4f);
    }

    @Test
    void theStanceTiltCompressesTheHeightAndFoldsTheFootprintIn() {
        float bbWidth = 0.6f;
        float bbHeight = 1.8f;
        float halfDepth = bbWidth * (float) Math.sqrt(2.0) / 2f;
        float compression = (float) Math.cos(Math.toRadians(30.0));
        ProjectedBox projected = EntityPreviewRenderer
                .projectedBox(bbWidth, bbHeight, EntityPreviewRenderer.presentCameraPitch);

        assertEquals((float) Math.sqrt(2.0) * bbWidth, projected.width(), 1e-4f, "the yawed footprint's diagonal");
        assertEquals(0.5f * halfDepth, projected.down(), 1e-4f, "the footprint reaches half its depth below");
        assertEquals(
            compression * bbHeight + 0.5f * halfDepth,
            projected.up(),
            1e-4f,
            "the height is compressed to cos(30°) with the footprint folded into the same screen axis"
        );
        assertTrue(projected.up() < bbHeight, "a tilted camera never shows the full box height");
        assertTrue(projected.height() > bbHeight, "…but the footprint adds back what the compression took");
    }

    @Test
    void theTiltsSignOnlyDecidesWhichSideFacesTheCamera() {
        assertEquals(
            EntityPreviewRenderer.projectedBox(0.9f, 1.4f, -30f),
            EntityPreviewRenderer.projectedBox(0.9f, 1.4f, 30f),
            "looking up and looking down cover the same screen"
        );
        assertEquals(
            EntityPreviewRenderer.projectedBox(0.9f, 1.4f, -90f).up(),
            EntityPreviewRenderer.projectedBox(0.9f, 1.4f, 90f).down(),
            1e-4f,
            "straight down, the footprint is all there is"
        );
    }

    @Test
    void aDegenerateBoxProjectsToNothingInsteadOfNaN() {
        ProjectedBox empty = EntityPreviewRenderer.projectedBox(0f, 0f, EntityPreviewRenderer.presentCameraPitch);
        assertEquals(0f, empty.width());
        assertEquals(0f, empty.down());
        assertEquals(0f, empty.height());
    }

    // endregion

    // region the fit

    @Test
    void theCodeFallbackFitIsTheStanceFitAtAnUnscaledEntity() {
        for (float[] bb : boundingBoxes) {
            assertEquals(
                EntityPreviewRenderer.fitScale(
                    EntityPreviewRenderer.projectedBox(bb[0], bb[1], EntityPreviewRenderer.presentCameraPitch),
                    22,
                    28
                ),
                EntityPreviewRenderer.fitScale(bb[0], bb[1], 22, 28),
                "fitScale(bb…) is the projected fit at the stance tilt"
            );
        }
    }

    @Test
    void theFitKeepsTheWholeProjectionInsideTheBox() {
        for (float[] bb : boundingBoxes) {
            for (int[] box : boxes) {
                for (float tilt : tilts) {
                    ProjectedBox perBlock = EntityPreviewRenderer.projectedBox(bb[0], bb[1], tilt);
                    int scale = EntityPreviewRenderer.fitScale(perBlock, box[0], box[1]);
                    ProjectedBox drawn = perBlock.scaled(scale);
                    String where = bb[0] + "x" + bb[1] + " from " + tilt + "° in " + box[0] + "x" + box[1];
                    assertTrue(
                        drawn.width() <= box[0] - 2f * margin + 1e-3f,
                        where + ": scale " + scale + " draws " + drawn.width() + " px wide"
                    );
                    assertTrue(
                        drawn.height() <= box[1] - 2f * margin + 1e-3f,
                        where + ": scale " + scale + " draws " + drawn.height() + " px tall"
                    );
                }
            }
        }
    }

    // endregion

    // region the placement

    @Test
    void theAnchorIsHalfABoundingBoxAboveTheOrigin() {
        // the pair the placement rides on: the pipeline's own translate(0, bbHeight/2, 0) has to come back out
        // of the y it is handed, or the entity is drawn half a bounding box below where it was placed
        for (float[] bb : boundingBoxes) {
            for (int scale : new int[]{1, 4, 13, 40}) {
                float origin = 60f;
                float anchor = EntityPreviewRenderer.anchorY(origin, bb[1], scale);
                assertEquals(origin - bb[1] * scale / 2f, anchor, 1e-4f);
                assertEquals(origin, anchor + bb[1] * scale / 2f, 1e-4f, "the round trip back to the origin");
            }
        }
    }

    @Test
    void aScaledEntityStandsWhereItWasPlacedAndStaysInsideTheBox() {
        // an entity with a scale attribute is drawn larger by the pipeline itself, so it is fitted and
        // projected with that factor folded in — but the pipeline's own translate runs in the outer frame,
        // so the anchor is converted with the bare render scale
        float attribute = 2f;
        int[] box = {22, 28};
        for (float[] bb : boundingBoxes) {
            ProjectedBox perBlock = EntityPreviewRenderer
                    .projectedBox(bb[0], bb[1], EntityPreviewRenderer.presentCameraPitch).scaled(attribute);
            int scale = EntityPreviewRenderer.fitScale(perBlock, box[0], box[1]);
            ProjectedBox drawn = perBlock.scaled(scale);
            float origin = EntityPreviewRenderer.originY(boxTop, box[1], drawn, margin);
            float anchor = EntityPreviewRenderer.anchorY(origin, bb[1], scale);
            String where = bb[0] + "x" + bb[1] + " at a scale attribute of " + attribute;

            assertEquals(origin, anchor + bb[1] * scale / 2f, 1e-3f, where + ": the drawn origin");
            assertTrue(origin - drawn.up() >= boxTop, where + ": the top leaves the box");
            assertTrue(origin + drawn.down() <= boxTop + box[1], where + ": the feet leave the box");
        }
    }

    @Test
    void everyPlacementStandsOnTheMarginAndKeepsTheHeadClear() {
        for (float[] bb : boundingBoxes) {
            for (int[] box : boxes) {
                for (float tilt : tilts) {
                    ProjectedBox perBlock = EntityPreviewRenderer.projectedBox(bb[0], bb[1], tilt);
                    int scale = EntityPreviewRenderer.fitScale(perBlock, box[0], box[1]);
                    ProjectedBox drawn = perBlock.scaled(scale);
                    float origin = EntityPreviewRenderer.originY(boxTop, box[1], drawn, margin);
                    // where the pipeline really draws the origin, through the anchor it is handed
                    float placed = EntityPreviewRenderer.anchorY(origin, bb[1], scale) + bb[1] * scale / 2f;
                    String where = bb[0] + "x" + bb[1] + " from " + tilt + "° in " + box[0] + "x" + box[1];
                    assertEquals(origin, placed, 1e-3f, "the anchor and the placement agree");

                    assertTrue(
                        placed - drawn.up() >= boxTop,
                        where + ": the top is at " + (placed - drawn.up()) + ", above the box top " + boxTop
                    );
                    assertTrue(
                        placed + drawn.down() <= boxTop + box[1],
                        where + ": the feet are at " + (placed + drawn.down()) + ", below the floor "
                                + (boxTop + box[1])
                    );
                    assertEquals(
                        boxTop + box[1] - margin,
                        placed + drawn.down(),
                        1e-3f,
                        where + ": a projection that fits stands on the margin, not on the floor"
                    );
                }
            }
        }
    }

    @Test
    void aProjectionTooBigForItsBoxIsCentredInsteadOfShearedAtTheFeet() {
        // an explicit scale the caller asked for, well past what the box can hold
        ProjectedBox drawn = projected(0.6f, 1.8f, EntityPreviewRenderer.presentCameraPitch, 40f);
        float origin = EntityPreviewRenderer.originY(0, 28, drawn, margin);

        assertEquals(14f, origin + (drawn.down() - drawn.up()) / 2f, 1e-3f, "the projection is centred in the box");
        assertEquals(
            origin - drawn.up() - 0f,
            28f - (origin + drawn.down()),
            1e-3f,
            "what it overflows the top by is what it overflows the bottom by"
        );
    }

    @Test
    void theUnprojectedPlacementIsWhatCutTheFeetOff() {
        // the arithmetic the renderer shipped with: the raw bounding box bottom-aligned on the floor, which is
        // exactly what cy = y + height - bbHeight·scale/2 amounts to, at the 14 px/block the raw fit asked for
        float floor = 28f;
        ProjectedBox drawn = projected(0.6f, 1.8f, EntityPreviewRenderer.presentCameraPitch, 14f);

        assertEquals(2.97f, drawn.down(), 0.05f, "the feet fell this far below the floor — the strip that was lost");
        assertEquals(24.79f, drawn.up(), 0.05f, "…and the model only reached this high");
        assertTrue(floor - drawn.up() > 2f, "leaving a gap of unused box under the top: the entity read as too low");

        // the same projection, placed by the fit and the projection together, fits the box it was fitted to
        int scale = EntityPreviewRenderer.fitScale(0.6f, 1.8f, 22, (int) floor);
        ProjectedBox fitted = projected(0.6f, 1.8f, EntityPreviewRenderer.presentCameraPitch, scale);
        float origin = EntityPreviewRenderer.originY(0, (int) floor, fitted, margin);
        assertTrue(origin - fitted.up() >= 0f, "the head stays inside");
        assertTrue(origin + fitted.down() <= floor, "the feet stay inside");
    }

    // endregion
}
