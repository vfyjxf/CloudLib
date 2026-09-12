package dev.vfyjxf.nimbusprojection.internal;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.Pos;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanel;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldTraceable;
import dev.vfyjxf.cloudlib.api.ui.inworld.PanelKey;
import dev.vfyjxf.cloudlib.api.ui.inworld.Presentation;
import dev.vfyjxf.nimbusprojection.api.panel.GroupRole;
import dev.vfyjxf.nimbusprojection.api.panel.PanelSpec;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

/**
 * Per-panel runtime state owned by {@link InworldManager}: the spec, the chrome
 * widget and all per-frame geometry resolved from the anchor and presentation.
 */
final class PanelRuntime implements InworldPanel {

    final InworldManager manager;
    final PanelKey key;
    PanelSpec spec;
    InworldPanelWidget widget;

    /** the group this panel belongs to, null for standalone panels */
    @Nullable
    PanelKey groupKey;

    GroupRole groupRole = GroupRole.primary;

    // region per-frame state

    /** resolved world-space anchor position this frame, null when not alive */
    @Nullable
    Vec3 anchorWorld;
    /** projected anchor point in gui px, null when behind the camera */
    @Nullable
    FloatPos anchorScreen;
    /** distance camera → anchor in blocks */
    double distance;
    /** whether the panel should be drawn this frame */
    boolean presented;
    /** whether the panel is currently presented flat in screen space */
    boolean flat;
    /** the presentation actually resolved this frame — may differ from
     *  {@code spec.presentation()} after engage-expansion upgrades it
     *  (face/follow → expand); deferred solver passes must read this, not
     *  the spec */
    Presentation effective;
    /** resolved dock corner this frame (auto resolved to a concrete corner) */
    Presentation.DockCorner dockCorner = Presentation.DockCorner.auto;
    /** dock layout already placed this panel this frame — the conflict pass must leave it alone */
    boolean docked;
    /** last resolved auto dock corner — hysteresis keeps it until the anchor crosses far past center */
    @Nullable
    Presentation.DockCorner lastAutoCorner;

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
    /** smoothed scene luminance behind the leader line's midpoint — picks dark/bright ink */
    float lineLum = 0.2f;
    /** normalized screen-space direction from the center toward the off-screen anchor */
    @Nullable
    FloatPos indicatorDir;

    /** this frame the dock column ran out of room and the panel shows chrome only */
    boolean folded;
    /** last measured height while unfolded — folding collapses the layout to a
     *  chrome strip, so solvers must budget against the remembered full height
     *  or the fold decision would flap frame to frame */
    int unfoldedHeight;
    /** same as {@link #unfoldedHeight} for width — a folded panel can also
     *  shrink horizontally when its content was wider than the chrome */
    int unfoldedWidth;

    /** a live trace session froze this panel's position — resolvers keep it presented but never retarget */
    boolean pinned;

    /** the pin key docked this panel — survives focus loss, range and a dead
     *  anchor ("signal lost"), closes only on unpin, suspend-close from a
     *  dimension change, or the provider's own decay semantics */
    boolean userPinned;

    /** the interact key expanded this on-demand panel — presents until it
     *  loses all targeting for the grace window */
    boolean engaged;

    /** Emitted by a {@code sharedDomain} provider — presence relays for this key. */
    boolean shared;
    /** tick the engaged panel last had no target; -1 while still held */
    long engageIdleSince = -1;

    /** gameTime+partialTick when the panel was created; -1 = no open animation */
    float bornAt = -1;
    /** current open-animation scale driven by the manager (1 = fully open) */
    float openScale = 1f;

    // face presentation geometry (world space)
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
    @Nullable
    com.mojang.blaze3d.pipeline.RenderTarget faceTarget;

    /** panel-local pointer position when crosshair-pointed in world mode */
    @Nullable
    FloatPos pointedUv;

    /**
     * 0..1 focus "heat": ramps up while this panel owns the player's attention
     * (focused or crosshair-pointed), decays after. Drives the scan frame /
     * engage-chip fade so focus changes read as smooth transitions instead of
     * pops. Render-side only — updated once per level-stage frame.
     */
    float focusHeat;

    /** ticks since creation — decay countdown baseline */
    long bornTick;

    // endregion

    private boolean closed;

    PanelRuntime(InworldManager manager, PanelSpec spec) {
        this.manager = manager;
        this.key = spec.key();
        this.spec = spec;
        this.groupKey = spec.groupKey();
        if (spec.groupRole() != null) this.groupRole = spec.groupRole();
    }

    // region InworldPanel

    @Override
    public PanelKey key() {
        return key;
    }

    @Override
    public InworldAnchor anchor() {
        return spec.anchor();
    }

    @Override
    public Presentation presentation() {
        return spec.presentation();
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
    public boolean engaged() {
        return engaged;
    }

    @Override
    public boolean pinned() {
        return userPinned;
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

    // endregion

    boolean closed() {
        return closed;
    }

    /** The content's trace-mode handler, when it implements {@link InworldTraceable}. */
    @Nullable
    InworldTraceable traceable() {
        return widget.content() instanceof InworldTraceable t ? t : null;
    }

    /** The scene-space x used for synthesized pointer input (parked face panels use their slot). */
    int inputSceneX() {
        return widget.screenX;
    }

    int inputSceneY() {
        return widget.screenY;
    }
}
