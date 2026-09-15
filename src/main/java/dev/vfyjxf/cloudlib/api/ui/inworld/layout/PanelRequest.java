package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import java.util.Objects;
import java.util.Set;

/**
 * One UI surface the solver is asked to place this frame.
 * <p>
 * {@code mergeKey} groups requests that may share one visual;
 * {@code overflowGroup} controls how folded requests cluster in the dock.
 * Yielding is explicit and directional — this panel may be pushed aside
 * or repositioned so the panels it yields to keep their place; cycles
 * are rejected. {@code yieldTo} names specific request ids while
 * {@code yieldScope} widens the rule to every request in the frame or in
 * a chosen {@link Space}.
 */
public record PanelRequest(
        String id,
        String sourceId,
        String title,
        String family,
        String mergeKey,
        String overflowGroup,
        int priority,
        Space space,
        PositionPolicy position,
        OrientationPolicy orientation,
        Material material,
        Metrics metrics,
        boolean compactAllowed,
        boolean mergeAllowed,
        boolean omissionAllowed,
        LeaderMode leaderMode,
        GuiRect fixedScreen,
        boolean crossSpaceAvoidance,
        Set<String> yieldTo,
        YieldScope yieldScope) {

    public PanelRequest(
            String id,
            String sourceId,
            String title,
            String family,
            String mergeKey,
            String overflowGroup,
            int priority,
            Space space,
            PositionPolicy position,
            OrientationPolicy orientation,
            Material material,
            Metrics metrics,
            boolean compactAllowed,
            boolean mergeAllowed,
            boolean omissionAllowed,
            LeaderMode leaderMode,
            GuiRect fixedScreen) {
        this(
                id,
                sourceId,
                title,
                family,
                mergeKey,
                overflowGroup,
                priority,
                space,
                position,
                orientation,
                material,
                metrics,
                compactAllowed,
                mergeAllowed,
                omissionAllowed,
                leaderMode,
                fixedScreen,
                false,
                Set.of(),
                YieldScope.none);
    }

    /**
     * Which panels this request defers to, beyond the ids in
     * {@link #yieldTo}. Space-scoped rules yield to every request in the
     * named space; {@link #all} yields to every other request in the
     * frame; {@link #otherSpace} is the first-class form of the legacy
     * {@code crossSpaceAvoidance} flag.
     */
    public enum YieldScope {
        none,
        all,
        screen,
        world,
        otherSpace
    }

    public PanelRequest {
        Objects.requireNonNull(id);
        Objects.requireNonNull(sourceId);
        Objects.requireNonNull(title);
        Objects.requireNonNull(position);
        Objects.requireNonNull(orientation);
        Objects.requireNonNull(material);
        Objects.requireNonNull(metrics);
        Objects.requireNonNull(space);
        Objects.requireNonNull(leaderMode);
        Objects.requireNonNull(family);
        yieldTo = Set.copyOf(yieldTo);
        Objects.requireNonNull(yieldScope);
        if (yieldTo.contains(id)) {
            throw new IllegalArgumentException("UI cannot yield to itself: " + id);
        }
        if (priority < 0 || priority > 100) {
            throw new IllegalArgumentException("priority 0..100");
        }
        if (position.mounted()
                && !(orientation instanceof SourceOrientation)
                && !(orientation instanceof ReadableBothSides both && both.base() instanceof SourceOrientation)) {
            throw new IllegalArgumentException("mounted UI must use the source surface orientation");
        }
    }

    /**
     * Legacy broad cross-space rule — yields to every panel in the other
     * space. Prefer {@link #withYieldTo} for named targets or
     * {@link #withYieldToOtherSpace} for the equivalent scope rule.
     */
    @Deprecated
    public PanelRequest withCrossSpaceAvoidance(boolean enabled) {
        return new PanelRequest(
                id,
                sourceId,
                title,
                family,
                mergeKey,
                overflowGroup,
                priority,
                space,
                position,
                orientation,
                material,
                metrics,
                compactAllowed,
                mergeAllowed,
                omissionAllowed,
                leaderMode,
                fixedScreen,
                enabled,
                yieldTo,
                yieldScope);
    }

    /** This request yields to the named panels only. */
    public PanelRequest withYieldTo(String... panelIds) {
        return new PanelRequest(
                id,
                sourceId,
                title,
                family,
                mergeKey,
                overflowGroup,
                priority,
                space,
                position,
                orientation,
                material,
                metrics,
                compactAllowed,
                mergeAllowed,
                omissionAllowed,
                leaderMode,
                fixedScreen,
                false,
                Set.of(panelIds),
                YieldScope.none);
    }

    /** This request yields to every other panel in the frame. */
    public PanelRequest withYieldToAll() {
        return new PanelRequest(
                id,
                sourceId,
                title,
                family,
                mergeKey,
                overflowGroup,
                priority,
                space,
                position,
                orientation,
                material,
                metrics,
                compactAllowed,
                mergeAllowed,
                omissionAllowed,
                leaderMode,
                fixedScreen,
                false,
                Set.of(),
                YieldScope.all);
    }

    /** This request yields to every panel placed in {@code space}. */
    public PanelRequest withYieldToAll(Space space) {
        return new PanelRequest(
                id,
                sourceId,
                title,
                family,
                mergeKey,
                overflowGroup,
                priority,
                this.space,
                position,
                orientation,
                material,
                metrics,
                compactAllowed,
                mergeAllowed,
                omissionAllowed,
                leaderMode,
                fixedScreen,
                false,
                Set.of(),
                space == Space.screen ? YieldScope.screen : YieldScope.world);
    }

    /** This request yields to every panel in the other space. */
    public PanelRequest withYieldToOtherSpace() {
        return new PanelRequest(
                id,
                sourceId,
                title,
                family,
                mergeKey,
                overflowGroup,
                priority,
                space,
                position,
                orientation,
                material,
                metrics,
                compactAllowed,
                mergeAllowed,
                omissionAllowed,
                leaderMode,
                fixedScreen,
                false,
                Set.of(),
                YieldScope.otherSpace);
    }
}
