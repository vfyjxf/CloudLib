package dev.vfyjxf.nimbusprojection.api.panel;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldAnchor;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPanelContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.InworldPlacement;
import dev.vfyjxf.nimbusprojection.api.sync.PanelChannelHandler;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Consumer;
import java.util.function.Function;

/**
 * Immutable-ish description of one in-world panel, offered either
 * imperatively through {@code NimbusClient.open(spec)} or declaratively by a
 * {@link dev.vfyjxf.nimbusprojection.api.provider.PanelProvider}.
 * <p>
 * The {@code key} is the panel's identity: providers re-offer the same key
 * each scan to keep a panel alive; when a key stops being offered the panel
 * closes. Two offers with the same key but different anchors/placements
 * update the existing panel in place.
 */
public final class PanelSpec {

    final Object key;
    InworldAnchor anchor;
    InworldPlacement placement;
    Function<InworldPanelContext, ? extends Widget> content;

    @Nullable Component title;
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
     * Non-interactive panels ignore this — they are pure displays and are
     * always presented.
     */
    boolean onDemand = true;
    /**
     * Zoning group: displaced non-interactive panels that cannot keep their
     * anchor position are gathered into a side rail; panels sharing a group
     * key stay adjacent inside it. {@code null} derives the group from the
     * panel key's parent path ("tracker/ent/12" → "tracker/ent").
     */
    @Nullable String group;
    /**
     * Merge cap for the zoning group: at most this many non-interactive
     * members are shown at once; the rest collapse into a "+N" badge on the
     * last visible member. Unlimited by default. When members declare
     * different limits the smallest wins.
     */
    int groupLimit = Integer.MAX_VALUE;
    /**
     * Off-screen collapse: when set and the supplier allows, a panel whose
     * anchor leaves the camera view shrinks to a small edge indicator
     * (diamond + bearing tick + distance) instead of rendering the full
     * panel where the target can't be seen anyway.
     */
    @Nullable BooleanSupplier offscreenIndicator;
    /**
     * The panel's primary action — invoked by the interact hotkey when this
     * panel is focused/soft-focused (look roughly at the anchor, press the
     * key). Panels declaring an action are also eligible for cone-based soft
     * focus even without an exact crosshair hit.
     */
    @Nullable Consumer<InworldPanelContext> action;
    /**
     * Handler for payloads arriving <em>from the server</em> on this panel's
     * channel. The matching send handle is
     * {@link InworldPanelContext#channel()}.
     */
    @Nullable PanelChannelHandler channel;

    private PanelSpec(
            Object key,
            InworldAnchor anchor,
            InworldPlacement placement,
            Function<InworldPanelContext, ? extends Widget> content
    ) {
        this.key = key;
        this.anchor = anchor;
        this.placement = placement;
        this.content = content;
    }

    public static PanelSpec of(
            Object key,
            InworldAnchor anchor,
            InworldPlacement placement,
            Function<InworldPanelContext, ? extends Widget> content
    ) {
        return new PanelSpec(key, anchor, placement, content);
    }

    public Object key() {
        return key;
    }

    public InworldAnchor anchor() {
        return anchor;
    }

    public InworldPlacement placement() {
        return placement;
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

    /**
     * The zoning group this panel belongs to. Defaults to the panel key's
     * parent path so sibling keys ("x/tag/1", "x/tag/2") zone together.
     */
    public String group() {
        if (group != null) return group;
        String k = String.valueOf(key);
        int slash = k.lastIndexOf('/');
        return slash > 0 ? k.substring(0, slash) : k;
    }

    /** Max members of this panel's group shown at once — see {@link #groupLimit}. */
    public int groupLimit() {
        return groupLimit;
    }

    /** Whether this panel may currently collapse to an off-screen edge indicator. */
    public boolean collapsesOffscreen() {
        return offscreenIndicator != null && offscreenIndicator.getAsBoolean();
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

    //region mutation

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

    /**
     * Whether the panel waits for the interact key before presenting
     * (default) or shows itself on sight. See {@link #onDemand}.
     */
    public PanelSpec onDemand(boolean onDemand) {
        this.onDemand = onDemand;
        return this;
    }

    /** Overrides the zoning group (see {@link #group()}). */
    public PanelSpec group(@Nullable String group) {
        this.group = group;
        return this;
    }

    /** Caps how many members of this panel's group may be visible at once; extras merge into a "+N" badge. */
    public PanelSpec groupLimit(int groupLimit) {
        this.groupLimit = Math.max(1, groupLimit);
        return this;
    }

    /** Allow this panel to collapse to an edge indicator whenever its anchor is off-screen. */
    public PanelSpec offscreenIndicator() {
        return offscreenIndicator(() -> true);
    }

    /**
     * Caller-controlled off-screen collapse — the panel shrinks to an edge
     * indicator only while {@code allowed} returns true.
     */
    public PanelSpec offscreenIndicator(BooleanSupplier allowed) {
        this.offscreenIndicator = allowed;
        return this;
    }

    /**
     * Sets the panel's primary action — run by the interact hotkey while the
     * panel is focused. Also makes the panel eligible for soft focus.
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

    public PanelSpec placement(InworldPlacement placement) {
        this.placement = placement;
        return this;
    }

    //endregion
}
