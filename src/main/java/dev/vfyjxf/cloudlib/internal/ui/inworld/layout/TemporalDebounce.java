package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutFrame;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutState;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LeaderLine;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelRequest;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Space;

import java.util.ArrayList;
import java.util.Collections;
import java.util.HashMap;
import java.util.HashSet;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * Temporal debounce windows: a change (panel move, leader reroute) only
 * becomes legal once the desired state has been continuously available
 * for {@code recoverySeconds} — time gates hold only currently legal
 * choices, never stale geometry.
 */
public final class TemporalDebounce {

    /** A debounce timer for one observed target value. */
    static final class Window {
        Object target;
        double since, seen;
        long frame = Long.MIN_VALUE;

        void clear() {
            target = null;
            frame = Long.MIN_VALUE;
        }

        /** Observe {@code next} at this frame's clock; true once it has been stable for recoverySeconds. */
        boolean observe(LayoutFrame f, Object next) {
            double now = f.clock().seconds();
            boolean same = Objects.equals(target, next);
            boolean repeated = frame == f.clock().frameIndex() && seen == now;
            boolean continuous = frame + 1 == f.clock().frameIndex() && now > seen && now - seen <= .1;
            if (!same || !(repeated || continuous)) since = now;
            target = next;
            seen = now;
            frame = f.clock().frameIndex();
            return now - since + 1e-9 >= f.config().recoverySeconds();
        }

        /** Would {@code next} be legal right now (already observed long enough)? */
        boolean ready(LayoutFrame f, Object next) {
            return Objects.equals(target, next)
                    && f.clock().seconds() - since + 1e-9 >= f.config().recoverySeconds();
        }
    }

    /** Per-visual leader-line debounce state. */
    static final class Line {
        PanelRequest request;
        long generation;
        boolean visible;
        String route;
        double changedAt;
        final Window appearance = new Window();
        final Window change = new Window();
    }

    final Map<String, Line> lines = new HashMap<>();
    final Window movement = new Window();
    private long frame = Long.MIN_VALUE;
    private double time = Double.NEGATIVE_INFINITY;

    public void reset() {
        lines.clear();
        movement.clear();
        frame = Long.MIN_VALUE;
        time = Double.NEGATIVE_INFINITY;
    }

    /**
     * Advances the debounce clocks. Duplicate frames (same index and time)
     * and smooth continuation keep windows alive; skipped frames or a
     * stale gap clear them all — timers never advance on repeated or
     * skipped frames.
     */
    void begin(LayoutFrame f) {
        long index = f.clock().frameIndex();
        double now = f.clock().seconds();
        if (index < frame || now < time) reset();
        if (frame != Long.MIN_VALUE
                && !(index == frame && now == time)
                && !(index == frame + 1 && now > time && now - time <= .1)) {
            movement.clear();
            for (Line line : lines.values()) {
                line.appearance.clear();
                line.change.clear();
            }
        }
        frame = index;
        time = now;
    }

    boolean pending() {
        return movement.target != null
                || lines.values().stream().anyMatch(l -> l.appearance.target != null || l.change.target != null);
    }

    Line read(PanelPlacement candidate) {
        Line line = lines.get(candidate.visualId());
        return line != null
                        && line.generation == candidate.source().generation()
                        && line.request.equals(candidate.active())
                ? line
                : null;
    }

    private Line writable(PanelPlacement candidate) {
        Line line = read(candidate);
        if (line == null) {
            line = new Line();
            line.request = candidate.active();
            line.generation = candidate.source().generation();
            lines.put(candidate.visualId(), line);
        }
        return line;
    }

    boolean maySwitch(LayoutFrame f, PanelPlacement candidate, String desired) {
        Line line = read(candidate);
        return line == null
                || !line.visible
                || Objects.equals(line.route, desired)
                || candidate.active().id().equals(f.interaction().focusedId())
                || line.change.ready(f, desired)
                        && f.clock().seconds() - line.changedAt + 1e-9
                                >= f.config().recoverySeconds();
    }

    void commitRoute(
            LayoutFrame f, PanelPlacement candidate, LeaderSolver.Option selected, LeaderSolver.Option desired) {
        Line line = writable(candidate);
        if (!line.visible || !Objects.equals(line.route, selected.key())) {
            line.changedAt = f.clock().seconds();
        }
        line.visible = true;
        line.route = selected.key();
        line.appearance.clear();
        if (desired != null && !Objects.equals(desired.key(), selected.key())) {
            line.change.observe(f, desired.key());
        } else {
            line.change.clear();
        }
    }

    /**
     * The AUTO-mode leader gate: appearance debounces a newly visible
     * line; once visible, route switches debounce through {@code change}
     * and a focused panel may switch immediately.
     */
    LeaderLine finishAuto(
            LayoutFrame f, PanelPlacement candidate, List<LeaderSolver.Option> options, LeaderLine fallback) {
        Line line = writable(candidate);
        if (options.isEmpty()) {
            hide(line);
            return fallback;
        }
        LeaderSolver.Option desired = options.get(0);
        LeaderSolver.Option selected = desired;
        if (!line.visible
                && !candidate.active().id().equals(f.interaction().focusedId())
                && !line.appearance.observe(f, "visible")) {
            return LeaderSolver.hidden(
                    candidate,
                    candidate.members().stream().map(PanelRequest::id).toList(),
                    "LEADER_RECOVERY_PENDING");
        }
        if (line.visible && !Objects.equals(line.route, desired.key())) {
            line.change.observe(f, desired.key());
            if (!maySwitch(f, candidate, desired.key())) {
                selected = options.stream()
                        .filter(o -> Objects.equals(o.key(), line.route))
                        .findFirst()
                        .orElse(desired);
            }
        }
        commitRoute(f, candidate, selected, desired);
        return selected.leader();
    }

    private static void hide(Line line) {
        line.visible = false;
        line.route = null;
        line.appearance.clear();
        line.change.clear();
    }

    /** 0 for a visual whose leader is already on screen, 1 otherwise — stable ordering aid. */
    int ownerRank(PanelPlacement candidate) {
        Line line = read(candidate);
        return line != null && line.visible ? 0 : 1;
    }

    /**
     * End-of-frame housekeeping: dead visuals' lines are dropped, and any
     * line that ended the frame neither visible nor pending is hidden.
     */
    void end(List<LayoutEngine.Unit> units, List<LeaderLine> rendered) {
        Set<String> live = new HashSet<>();
        Set<String> visible = new HashSet<>();
        Set<String> pending = new HashSet<>();
        for (LayoutEngine.Unit unit : units) live.add(unit.id());
        for (LeaderLine line : rendered) {
            if (!line.screen().isEmpty()) visible.add(line.visualId());
            if (line.status().equals("LEADER_RECOVERY_PENDING")) pending.add(line.visualId());
        }
        lines.keySet().retainAll(live);
        lines.forEach((id, line) -> {
            if (!visible.contains(id) && !pending.contains(id)) hide(line);
        });
    }

    /**
     * Movement debounce for whole-panel relocation: when the only
     * difference between the desired plan and the last result is panels
     * that would move, they are held in place until the move has been
     * continuously proposed for {@code recoverySeconds} — the moved panel
     * must be collision-free and the re-routed plan must stay legal.
     */
    LayoutSearch.Allocation panels(
            LayoutFrame f,
            LayoutState state,
            List<LayoutEngine.Unit> units,
            LayoutSearch.Allocation desired,
            LeaderSolver solver,
            LayoutSearch.Work work) {
        if (state.cachedResult == null
                || f.interaction().focusedId() != null
                || !Objects.equals(
                        state.cachedResult.drawer() != null, f.interaction().overflowOpen())) {
            movement.clear();
            return desired;
        }
        Map<String, PanelPlacement> old = new HashMap<>();
        for (PanelPlacement c : state.cachedResult.panels()) old.put(c.visualId(), c);
        Map<String, LayoutEngine.Unit> byId = new HashMap<>();
        for (LayoutEngine.Unit unit : units) byId.put(unit.id(), unit);
        List<PanelPlacement> held = new ArrayList<>();
        List<String> target = new ArrayList<>();
        for (PanelPlacement c : desired.plan().panels()) {
            PanelPlacement before = old.get(c.visualId());
            PanelPlacement replacement = c;
            if (before != null
                    && before.active().equals(c.active())
                    && before.source().generation() == c.source().generation()
                    && f.requests().stream().noneMatch(r -> PanelRelations.yields(r, c.active()))
                    && before.tier() == c.tier()
                    && (!before.slot().equals(c.slot())
                            || c.space() == Space.screen && !before.screenRect().equals(c.screenRect()))) {
                replacement =
                        LayoutEngine.candidates(
                                        f, byId.get(c.visualId()), state.panels.get(c.visualId()), desired.exclusions())
                                .stream()
                                .filter(p -> p.tier() == c.tier()
                                        && p.slot().equals(before.slot())
                                        && (p.space() != Space.screen
                                                || p.screenRect().equals(before.screenRect())))
                                .findFirst()
                                .orElse(c);
                if (replacement != c) {
                    target.add(c.visualId()
                            + ":"
                            + Math.round(c.center().x() / 8)
                            + ":"
                            + Math.round(c.center().y() / 8));
                }
            }
            held.add(replacement);
        }
        if (target.isEmpty()) {
            movement.clear();
            return desired;
        }
        Collections.sort(target);
        double area = 0;
        for (int i = 0; i < held.size(); i++) {
            area += Math.abs(ScreenMath.area(held.get(i).polygon()));
            for (int j = 0; j < i; j++) {
                if (LayoutSearch.overlap(held.get(i), held.get(j), f.config().gap())) {
                    movement.clear();
                    return desired;
                }
            }
        }
        if (area > f.camera().width() * f.camera().height() * f.config().maxCoverage()) {
            movement.clear();
            return desired;
        }
        LayoutSearch.Plan legal = LayoutSearch.routeExisting(f, state, held, desired.exclusions(), solver, work);
        if (legal == null || movement.observe(f, List.copyOf(target))) {
            movement.clear();
            return desired;
        }
        return new LayoutSearch.Allocation(legal, desired.dock(), desired.drawer(), desired.exclusions());
    }
}
