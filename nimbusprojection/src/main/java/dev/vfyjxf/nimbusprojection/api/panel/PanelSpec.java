package dev.vfyjxf.nimbusprojection.api.panel;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.LayoutHint;
import dev.vfyjxf.cloudlib.api.ui.inworld.PanelKey;
import dev.vfyjxf.cloudlib.api.ui.inworld.Presentation;
import dev.vfyjxf.nimbusprojection.api.policy.FocusPolicy;
import dev.vfyjxf.nimbusprojection.api.policy.SuspendPolicy;
import dev.vfyjxf.nimbusprojection.api.sync.PanelChannelHandler;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Immutable-ish description of one in-world panel, offered either
 * imperatively through {@code NimbusClient.open(spec)}, declaratively by a
 * {@link dev.vfyjxf.nimbusprojection.api.provider.PanelProvider}, or as a
 * member of a {@link PanelGroup}.
 * <p>
 * The {@code key} is the panel's identity: providers re-offer the same key
 * each pass to keep a panel alive; when a key stops being offered the panel
 * closes. Two offers with the same key but different anchors/presentations
 * update the existing panel in place.
 */
public final class PanelSpec {

    final PanelKey key;
    InworldAnchor anchor;
    Presentation presentation;
    Function<InworldPanelContext, ? extends Widget> content;

    @Nullable
    Component title;

    List<String> hints = List.of();
    boolean interactive = true;
    boolean leaderLine = true;
    boolean openAnimation = false;
    double maxDistance = 32;
    /**
     * Engagement gating: an interactive panel with {@code onDemand} stays
     * <em>dormant</em> — it tracks its anchor and shows the targeting
     * affordance while focused, but presents no chrome — until the interact
     * key engages it. Defaults true so the world isn't wallpapered with
     * panels; set false for ambient displays that should always show.
     */
    boolean onDemand = true;
    /**
     * Idle placement: when set, the panel's resting form is a flat screen
     * card floating next to the anchor's projected position — the declared
     * world presentation only appears once the player <em>pins</em> it in
     * with the interact key (engage). Several can float or be world-pinned
     * at once. Default false — the spec's presentation is the resting form.
     */
    boolean floatingOnIdle = false;
    /**
     * Layout participation: zoning, folding, off-screen collapse and
     * occlusion policy — read by every presentation driver.
     */
    LayoutHint layoutHint = LayoutHint.defaults();
    /**
     * Transient lifecycle: when set the panel dies after its TTL with a
     * fade-out — toasts, pick-up hints, operation feedback. {@code null} =
     * lives until its key stops being offered (or closed imperatively).
     */
    @Nullable
    Decay decay;
    /**
     * Cross-anchor group membership: joins the {@link PanelGroup} offered
     * under this key regardless of where this panel's anchor sits — the
     * multi-block-structure case (every part of a door shares the group's
     * affordance).
     */
    @Nullable
    PanelKey groupKey;
    /**
     * The role this panel claims inside a container. In an explicit
     * {@link PanelGroup} the member's declared role wins; in an implicit
     * same-anchor merge the first {@link GroupRole#primary} claimant is
     * primary and the rest demote to secondary.
     */
    GroupRole groupRole = GroupRole.primary;
    /**
     * Refuses implicit same-anchor merging: this panel keeps its own
     * affordance even when another panel resolves to the same anchor.
     */
    boolean standalone = false;
    /** Focus-selection override — {@code null} = the runtime's soft-cone default. */
    @Nullable
    FocusPolicy focusPolicy;
    /** Suspend/close policy override — {@code null} = {@link SuspendPolicy#standard()}. */
    @Nullable
    SuspendPolicy suspendPolicy;
    /**
     * The panel's primary action — invoked by the interact hotkey while
     * focused. Panels declaring an action are eligible for cone-based soft
     * focus even without an exact crosshair hit.
     */
    @Nullable
    Consumer<InworldPanelContext> action;
    /** Server → client channel handler; sends go through {@link InworldPanelContext#channel()}. */
    @Nullable
    PanelChannelHandler channel;

    private PanelSpec(
            PanelKey key,
            InworldAnchor anchor,
            Presentation presentation,
            Function<InworldPanelContext, ? extends Widget> content) {
        this.key = key;
        this.anchor = anchor;
        this.presentation = presentation;
        this.content = content;
    }

    public static PanelSpec of(
            PanelKey key,
            InworldAnchor anchor,
            Presentation presentation,
            Function<InworldPanelContext, ? extends Widget> content) {
        return new PanelSpec(key, anchor, presentation, content);
    }

    public PanelKey key() {
        return key;
    }

    public InworldAnchor anchor() {
        return anchor;
    }

    public Presentation presentation() {
        return presentation;
    }

    public Function<InworldPanelContext, ? extends Widget> content() {
        return content;
    }

    public @Nullable Component title() {
        return title;
    }

    /** Small interaction chips rendered on the panel frame, e.g. {@code "[E] open"}. */
    public List<String> hints() {
        return hints;
    }

    /** Whether the panel reacts to clicks/hover at all. */
    public boolean interactive() {
        return interactive;
    }

    /** Whether a connector line is drawn between the panel and its anchor in screen modes. */
    public boolean leaderLine() {
        return leaderLine;
    }

    /** Whether the panel plays a scale-in open animation when created. */
    public boolean openAnimation() {
        return openAnimation;
    }

    /** Maximum camera distance in blocks before the panel is hidden. */
    public double maxDistance() {
        return maxDistance;
    }

    /** Layout participation — see {@link LayoutHint}. */
    public LayoutHint layoutHint() {
        return layoutHint;
    }

    /** Transient lifecycle, or null for a persistent panel. */
    public @Nullable Decay decay() {
        return decay;
    }

    /** The named cross-anchor group this panel joins, or null. */
    public @Nullable PanelKey groupKey() {
        return groupKey;
    }

    /** The role this panel claims inside a container. */
    public GroupRole groupRole() {
        return groupRole;
    }

    /** Whether implicit same-anchor merging is refused. */
    public boolean standalone() {
        return standalone;
    }

    /** Focus-selection override, or null for the runtime default. */
    public @Nullable FocusPolicy focusPolicy() {
        return focusPolicy;
    }

    /** Suspend/close policy override, or null for {@link SuspendPolicy#standard()}. */
    public @Nullable SuspendPolicy suspendPolicy() {
        return suspendPolicy;
    }

    /** The panel's primary action triggered by the interact hotkey, or null. */
    public @Nullable Consumer<InworldPanelContext> action() {
        return action;
    }

    /** The server → client channel handler, or null when unused. */
    public @Nullable PanelChannelHandler channelHandler() {
        return channel;
    }

    /**
     * Whether this panel must be engaged (interact key on its targeted
     * anchor) before it presents. Always false for non-interactive panels.
     */
    public boolean requiresEngage() {
        return interactive && onDemand;
    }

    /**
     * Whether the unengaged form docks to the screen edge instead of taking
     * the declared presentation — the world form is the engaged one.
     */
    public boolean floatingOnIdle() {
        return floatingOnIdle;
    }

    // region mutation

    public PanelSpec title(@Nullable Component title) {
        this.title = title;
        return this;
    }

    public PanelSpec hints(String... hints) {
        this.hints = List.of(hints);
        return this;
    }

    public PanelSpec interactive(boolean interactive) {
        this.interactive = interactive;
        return this;
    }

    public PanelSpec leaderLine(boolean leaderLine) {
        this.leaderLine = leaderLine;
        return this;
    }

    public PanelSpec openAnimation(boolean openAnimation) {
        this.openAnimation = openAnimation;
        return this;
    }

    public PanelSpec maxDistance(double maxDistance) {
        this.maxDistance = maxDistance;
        return this;
    }

    public PanelSpec floatingOnIdle(boolean floatingOnIdle) {
        this.floatingOnIdle = floatingOnIdle;
        return this;
    }

    /**
     * Whether the panel waits for the interact key before presenting
     * (default) or shows itself on sight. See {@link #onDemand}.
     */
    public PanelSpec onDemand(boolean onDemand) {
        this.onDemand = onDemand;
        return this;
    }

    /** Replaces the layout hint — zoning/folding/occlusion declarations. */
    public PanelSpec layoutHint(LayoutHint hint) {
        this.layoutHint = hint;
        return this;
    }

    /** Mutates the current hint in place (chain-friendly convenience). */
    public LayoutHint hint() {
        return layoutHint;
    }

    /** Gives the panel a transient lifecycle — see {@link Decay}. */
    public PanelSpec decay(Decay decay) {
        this.decay = decay;
        return this;
    }

    /** Joins the {@link PanelGroup} offered under {@code groupKey}, across anchors. */
    public PanelSpec groupKey(PanelKey groupKey) {
        this.groupKey = groupKey;
        return this;
    }

    /** The role this panel claims inside a container — see {@link GroupRole}. */
    public PanelSpec groupRole(GroupRole role) {
        this.groupRole = role;
        return this;
    }

    /** Refuses implicit same-anchor merging — this panel keeps its own affordance. */
    public PanelSpec standalone(boolean standalone) {
        this.standalone = standalone;
        return this;
    }

    /** Overrides focus selection for this panel — see {@link FocusPolicy}. */
    public PanelSpec focusPolicy(FocusPolicy policy) {
        this.focusPolicy = policy;
        return this;
    }

    /** Overrides suspend/close behaviour — see {@link SuspendPolicy}. */
    public PanelSpec suspendPolicy(SuspendPolicy policy) {
        this.suspendPolicy = policy;
        return this;
    }

    /**
     * Sets the panel's primary action — run by the interact hotkey while
     * focused. Also makes the panel eligible for soft focus.
     */
    public PanelSpec action(Consumer<InworldPanelContext> action) {
        this.action = action;
        return this;
    }

    /**
     * Registers the handler for server → client payloads on this panel's
     * channel. Client → server sends go through
     * {@link InworldPanelContext#channel()}.
     */
    public PanelSpec channel(PanelChannelHandler handler) {
        this.channel = handler;
        return this;
    }

    public PanelSpec anchor(InworldAnchor anchor) {
        this.anchor = anchor;
        return this;
    }

    public PanelSpec presentation(Presentation presentation) {
        this.presentation = presentation;
        return this;
    }

    // endregion
}
