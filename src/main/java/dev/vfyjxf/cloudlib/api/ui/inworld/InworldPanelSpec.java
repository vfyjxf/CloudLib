package dev.vfyjxf.cloudlib.api.ui.inworld;

import dev.vfyjxf.cloudlib.api.ui.base.Widget;
import net.minecraft.network.chat.Component;
import org.jetbrains.annotations.Nullable;

import java.util.List;
import java.util.function.BooleanSupplier;
import java.util.function.Function;

/**
 * Immutable-ish description of one in-world panel, offered either imperatively
 * through {@code InworldUi.show(spec)} or declaratively by an
 * {@link InworldProvider}.
 * <p>
 * The {@code key} is the panel's identity: providers re-offer the same key each
 * scan to keep a panel alive; when a key stops being offered the panel closes.
 * Two offers with the same key but different anchors/placements update the
 * existing panel in place.
 */
public final class InworldPanelSpec {

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
     * Zoning group: displaced non-interactive panels (entity tags) that cannot
     * keep their anchor position are gathered into a side rail; panels sharing
     * a group key stay adjacent inside it. {@code null} derives the group from
     * the panel key's parent path ("tracker/ent/12" → "tracker/ent").
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

    private InworldPanelSpec(
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

    public static InworldPanelSpec of(
            Object key,
            InworldAnchor anchor,
            InworldPlacement placement,
            Function<InworldPanelContext, ? extends Widget> content
    ) {
        return new InworldPanelSpec(key, anchor, placement, content);
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

    //region mutation

    public InworldPanelSpec title(@Nullable Component title) {
        this.title = title;
        return this;
    }

    public InworldPanelSpec hints(String... hints) {
        this.hints = List.of(hints);
        return this;
    }

    public InworldPanelSpec interactive(boolean interactive) {
        this.interactive = interactive;
        return this;
    }

    public InworldPanelSpec leaderLine(boolean leaderLine) {
        this.leaderLine = leaderLine;
        return this;
    }

    public InworldPanelSpec openAnimation(boolean openAnimation) {
        this.openAnimation = openAnimation;
        return this;
    }

    public InworldPanelSpec maxDistance(double maxDistance) {
        this.maxDistance = maxDistance;
        return this;
    }

    /** Overrides the zoning group (see {@link #group()}). */
    public InworldPanelSpec group(@Nullable String group) {
        this.group = group;
        return this;
    }

    /** Caps how many members of this panel's group may be visible at once; extras merge into a "+N" badge. */
    public InworldPanelSpec groupLimit(int groupLimit) {
        this.groupLimit = Math.max(1, groupLimit);
        return this;
    }

    /** Allow this panel to collapse to an edge indicator whenever its anchor is off-screen. */
    public InworldPanelSpec offscreenIndicator() {
        return offscreenIndicator(() -> true);
    }

    /**
     * Caller-controlled off-screen collapse — the panel shrinks to an edge
     * indicator only while {@code allowed} returns true.
     */
    public InworldPanelSpec offscreenIndicator(BooleanSupplier allowed) {
        this.offscreenIndicator = allowed;
        return this;
    }

    public InworldPanelSpec anchor(InworldAnchor anchor) {
        this.anchor = anchor;
        return this;
    }

    public InworldPanelSpec placement(InworldPlacement placement) {
        this.placement = placement;
        return this;
    }

    //endregion
}
