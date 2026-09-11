package dev.vfyjxf.cloudlib.ui.inworld;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanel;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelSpec;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPlacement;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Per-panel runtime state owned by {@link InworldManager}: the spec, the chrome
 * widget and all per-frame geometry resolved from the anchor and placement.
 */
final class PanelRuntime implements InworldPanel {

    final InworldManager manager;
    final Object key;
    InworldPanelSpec spec;
    InworldPanelWidget widget;

    //region per-frame state

    /** resolved world-space anchor position this frame, null when not alive */
    @Nullable Vec3 anchorWorld;
    /** projected anchor point in gui px, null when behind the camera */
    @Nullable FloatPos anchorScreen;
    /** distance camera → anchor in blocks */
    double distance;
    /** whether the panel should be drawn this frame */
    boolean presented;
    /** whether the panel is currently presented flat in screen space */
    boolean flat;
    /** resolved dock corner this frame (AUTO resolved to a concrete corner) */
    InworldPlacement.DockCorner dockCorner = InworldPlacement.DockCorner.AUTO;
    /** last resolved AUTO dock corner — hysteresis keeps it until the anchor crosses far past center */
    @Nullable InworldPlacement.DockCorner lastAutoCorner;

    /** whether the resolved flat position should glide to its target (dock/floating/expand) */
    boolean smoothMove;
    /** target flat-screen position resolved this frame */
    int targetX, targetY;
    /** smoothed flat-screen position — lerped toward the target so slot changes glide */
    float posX, posY;
    /** whether posX/posY hold a valid previous position (false → snap, no glide) */
    boolean posInit;
    /** sticky conflict-slide direction (index into the 4-side candidate list);
     *  keeps a displaced tag from flip-flopping between near-tied sides */
    int lastSlideDir = -1;

    /** this frame the panel is collapsed to an off-screen edge indicator */
    boolean indicator;
    /** normalized screen-space direction from the center toward the off-screen anchor */
    @Nullable FloatPos indicatorDir;

    /** this frame the dock column ran out of room and the panel shows chrome only */
    boolean folded;

    /** gameTime+partialTick when the panel was created; -1 = no open animation */
    float bornAt = -1;
    /** current open-animation scale driven by the manager (1 = fully open) */
    float openScale = 1f;

    //face placement geometry (world space)
    Vec3 faceOrigin;
    Vec3 faceU;
    Vec3 faceV;
    Vec3 faceNormal;
    /** Sticky world position for expand panels — re-scored each frame, only
     *  replaced when a clearly better spot appears (no per-frame jumps). */
    Vec3 expandPos;
    /** true while every expand spot overlaps foreground screen area too much —
     *  the panel hides instead of covering the chrome (with hysteresis). */
    boolean expandHidden;
    double facePpb;
    /** offscreen target the face panel's widget tree is rendered into each frame */
    @Nullable com.mojang.blaze3d.pipeline.RenderTarget faceTarget;

    /** panel-local pointer position when crosshair-pointed in world mode */
    @Nullable FloatPos pointedUv;

    //endregion

    private boolean closed;

    PanelRuntime(InworldManager manager, InworldPanelSpec spec) {
        this.manager = manager;
        this.key = spec.key();
        this.spec = spec;
    }

    //region InworldPanel

    @Override
    public Object key() {
        return key;
    }

    @Override
    public InworldAnchor anchor() {
        return spec.anchor();
    }

    @Override
    public InworldPlacement placement() {
        return spec.placement();
    }

    @Override
    public Widget widget() {
        return widget;
    }

    @Override
    public Widget content() {
        return widget.content();
    }

    @Override
    public boolean focused() {
        return manager.focused() == this;
    }

    @Override
    public boolean hovered() {
        return widget.hovered();
    }

    @Override
    public boolean visible() {
        return widget.visible();
    }

    @Override
    public void setVisible(boolean visible) {
        widget.setVisible(visible);
    }

    @Override
    public void close() {
        closed = true;
        manager.close(key);
    }

    @Override
    public @Nullable Pos screenPos() {
        if (!presented || !flat) return null;
        return new Pos(widget.screenX, widget.screenY);
    }

    @Override
    public @Nullable Size screenSize() {
        if (!presented || !flat) return null;
        return widget.size();
    }

    //endregion

    boolean closed() {
        return closed;
    }

    /** The scene-space x used for synthesized pointer input (parked face panels use their slot). */
    int inputSceneX() {
        return widget.screenX;
    }

    int inputSceneY() {
        return widget.screenY;
    }
}
