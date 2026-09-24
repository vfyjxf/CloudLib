package dev.vfyjxf.cloudlib.ui.widget;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.canvas.EntityPreviewRenderer;
import dev.vfyjxf.cloudlib.api.ui.canvas.SceneCanvas;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionInfoCollector;
import dev.vfyjxf.cloudlib.api.ui.debug.InspectionProperty;
import dev.vfyjxf.taffy.geometry.FloatSize;
import net.minecraft.world.entity.Entity;
import org.jspecify.annotations.Nullable;

import java.util.Objects;
import java.util.function.Supplier;

/**
 * A small box holding a live 3D entity — the info panel's "here is the thing"
 * portrait, rendered through the same inventory-entity pipeline vanilla uses for
 * the player doll ({@link EntityPreviewRenderer}, shared with the rich-text
 * {@code EntityNode}).
 * <p>
 * The entity arrives through a {@link Supplier} and is queried on <em>every</em>
 * frame, not captured at construction: a preview bound to {@code level::getEntity}
 * follows the entity it names, goes blank when the level unloads it and comes back
 * without the widget being rebuilt. A supplier that returns {@code null} (or a box
 * that has collapsed to zero) simply draws nothing.
 * <p>
 * The widget is a leaf: it declares its size through a measure function on mount and
 * paints only content, so a theme may style it like any other element and the panel
 * around it owns the frame. The size is fixed by the constructor — the entity
 * renderer needs a stable box to center, stand and clip against. {@code followMouse}
 * mirrors {@code EntityNode}'s semantics: the pointer is read in the widget's own
 * local space, which is exactly what the canvas hands {@link #renderInternal}.
 * <p>
 * Rendering needs a live client level and entity render dispatcher, so only the
 * geometry and supplier contract are exercisable headlessly; the draw itself is
 * verified on screen.
 */
public final class EntityPreviewWidget extends Widget {

    private final Supplier<@Nullable ? extends Entity> entity;
    private final int previewWidth;
    private final int previewHeight;
    private final int scale;
    private final boolean followMouse;

    /**
     * A {@code width}×{@code height} preview at the auto-fitting scale — the entity's own bounding box
     * decides the pixels-per-block, so anything from a chicken to a golem fills the box.
     */
    public EntityPreviewWidget(Supplier<@Nullable ? extends Entity> entity, int width, int height) {
        this(entity, width, height, 0, false);
    }

    /**
     * @param entity      queried per frame; may return {@code null} to draw nothing
     * @param width       box width, at least one pixel
     * @param height      box height, at least one pixel
     * @param scale       render scale in pixels per block; {@code 0} (or any negative) fits the
     *                    entity's live bounding box — as the stance camera projects it — into the box
     *                    instead: the dynamic answer that keeps every entity readable, while an
     *                    explicit scale is clamped to at least one
     * @param followMouse whether the body and head track the pointer
     */
    public EntityPreviewWidget(
        Supplier<@Nullable ? extends Entity> entity,
        int width,
        int height,
        int scale,
        boolean followMouse
    ) {
        this.entity = Objects.requireNonNull(entity, "entity");
        this.previewWidth = Math.max(1, width);
        this.previewHeight = Math.max(1, height);
        this.scale = scale > 0 ? scale : 0;
        this.followMouse = followMouse;
        onMount(
            (scene, context, handle) -> scene.layoutTree()
                    .setMeasureFunc(nodeId(), (style, space) -> new FloatSize(previewWidth, previewHeight))
        );
    }

    // region configuration

    /** The live supplier — re-read every frame, answering {@code null} once the entity is gone. */
    public Supplier<@Nullable ? extends Entity> entity() {
        return entity;
    }

    /** The declared box width. */
    public int previewWidth() {
        return previewWidth;
    }

    /** The declared box height. */
    public int previewHeight() {
        return previewHeight;
    }

    /** Render scale in pixels per block; {@code 0} = the auto-fitting scale. */
    public int scale() {
        return scale;
    }

    /** Whether the rendered entity tracks the pointer. */
    public boolean followMouse() {
        return followMouse;
    }

    // endregion

    // region rendering

    @Override
    protected void renderInternal(SceneCanvas canvas, int mouseX, int mouseY, float partialTicks) {
        Entity current = entity.get();
        if (current == null) {
            return;
        }
        // the pointer arrives local to this widget — the same space the box lives in
        EntityPreviewRenderer
                .render(canvas, current, 0, 0, width(), height(), scale, followMouse, mouseX, mouseY, partialTicks);
    }

    // endregion

    // region inspection

    @Override
    public void collectInspectionInfo(InspectionInfoCollector collector) {
        super.collectInspectionInfo(collector);
        collector.add("box", previewWidth + "x" + previewHeight, InspectionProperty.categoryLayout);
        collector.add("scale", scale, InspectionProperty.categoryLayout);
        collector.addWithDefault("followMouse", followMouse, false, InspectionProperty.categoryBasic);
    }

    // endregion
}
