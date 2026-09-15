package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutFrame;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutState;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelMemory;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Space;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Tier;
import org.jetbrains.annotations.Nullable;

import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * The recovery debounce: a panel that was folded or compacted may not
 * re-expand until the better placement has been continuously available
 * for {@code recoverySeconds} — this stops oscillation between tiers.
 */
public final class RecoveryGate {

    private RecoveryGate() {}

    public record Observation(Tier tier, Space space, long generation, double since, double seen, long frame) {}

    /** Is {@code candidate} an upgrade over what {@code memory} remembers? */
    public static boolean upgrade(@Nullable PanelMemory memory, PanelPlacement candidate) {
        if (memory == null
                || memory.generation() != candidate.source().generation()
                || memory.space() != candidate.space()) {
            return false;
        }
        return memory.tier() == Tier.folded || memory.tier() == Tier.compact && candidate.tier() == Tier.full;
    }

    /**
     * Records upgrade observations for this frame's plan and returns the
     * set of unit ids still held (inside their recovery window).
     */
    static Set<String> observe(
            LayoutFrame frame, LayoutState state, List<LayoutEngine.Unit> units, LayoutSearch.Plan feasible) {
        Map<String, PanelPlacement> selected = new HashMap<>();
        for (PanelPlacement candidate : feasible.panels()) {
            selected.put(candidate.visualId(), candidate);
        }
        Set<String> held = new HashSet<>();
        Set<String> current = new HashSet<>();
        for (LayoutEngine.Unit unit : units) {
            String id = unit.id();
            current.add(id);
            PanelMemory memory = state.panels.get(id);
            PanelPlacement candidate = selected.get(id);
            boolean focused = unit.active().id().equals(frame.interaction().focusedId());
            if (focused
                    || memory == null
                    || memory.generation() != unit.source().generation()
                    || memory.space() != unit.active().space()) {
                state.recovery.remove(id);
                continue;
            }
            if (candidate == null || !upgrade(memory, candidate)) {
                state.recovery.remove(id);
                if (memory.tier() == Tier.folded || memory.tier() == Tier.compact) held.add(id);
                continue;
            }
            double now = frame.clock().seconds();
            Observation old = state.recovery.get(id);
            boolean repeated = old != null
                    && frame.clock().frameIndex() == old.frame()
                    && now == old.seen()
                    && old.tier() == candidate.tier()
                    && old.space() == candidate.space()
                    && old.generation() == candidate.source().generation();
            boolean continuous = old != null
                    && old.tier() == candidate.tier()
                    && old.space() == candidate.space()
                    && old.generation() == candidate.source().generation()
                    && frame.clock().frameIndex() == old.frame() + 1
                    && now > old.seen()
                    && now - old.seen() <= .1;
            double since = continuous || repeated ? old.since() : now;
            state.recovery.put(
                    id,
                    new Observation(
                            candidate.tier(),
                            candidate.space(),
                            candidate.source().generation(),
                            since,
                            now,
                            frame.clock().frameIndex()));
            if (now - since + 1e-9 < frame.config().recoverySeconds()) held.add(id);
        }
        state.recovery.keySet().retainAll(current);
        return held;
    }
}
