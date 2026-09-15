package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Config;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiRect;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.HudRegion;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutFrame;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutState;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LeaderLine;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LeaderMode;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Material;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Metrics;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.OrientationPolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelMemory;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelRequest;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PositionPolicy;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SearchBudget;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Space;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Tier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Set;

/**
 * Bounded joint panel/route search: an exact enumeration for small
 * domains, a cost-ordered beam search otherwise, plus deterministic
 * multi-panel repair when the beam leaves units unplaced. Leader routes
 * are solved jointly with panel placement because routes cross and
 * consume the same exclusions.
 */
public final class LayoutSearch {

    record Plan(List<PanelPlacement> panels, List<LeaderSolver.Option> routes, double cost) {}

    record Allocation(Plan plan, GuiRect dock, GuiRect drawer, List<List<GuiVec>> exclusions) {}

    record Beam(List<PanelPlacement> panels, List<LeaderSolver.Option> routes, double cost, double area) {}

    /** Per-solve work counters enforcing the {@link SearchBudget}. */
    public static final class Work {
        public final SearchBudget budget;
        public int expansions, routeCalls, repairs;
        public boolean limited;

        public Work(SearchBudget budget) {
            this.budget = budget;
        }

        public boolean step() {
            if (expansions >= budget.expansions()) {
                limited = true;
                return false;
            }
            expansions++;
            return true;
        }
    }

    private final LayoutFrame frame;
    private final LayoutState state;
    private final List<LayoutEngine.Unit> units;
    private final List<List<GuiVec>> exclusions;
    private final LeaderSolver leaders;
    private final Work work;
    private final Set<String> held;
    private final Plan fixed;
    private final boolean layered;
    private final Map<String, List<PanelPlacement>> domains = new LinkedHashMap<>();
    private final Map<String, LayoutEngine.Unit> byId = new HashMap<>();
    private final Map<List<PanelPlacement>, List<LeaderSolver.Option>> routeCache = new HashMap<>();

    /**
     * Units that share every candidate-affecting input may share the
     * generated domain — the template is re-stamped per unit afterwards.
     */
    private record CandidateKey(
            String source,
            Space space,
            PositionPolicy position,
            OrientationPolicy orientation,
            Material material,
            Metrics metrics,
            GuiRect fixed,
            boolean compact,
            boolean focused,
            PanelMemory previous) {}

    private LayoutSearch(
            LayoutFrame frame,
            LayoutState state,
            List<LayoutEngine.Unit> units,
            List<List<GuiVec>> exclusions,
            LeaderSolver leaders,
            Work work,
            Set<String> held) {
        this(frame, state, units, exclusions, leaders, work, held, new Plan(List.of(), List.of(), 0), false);
    }

    private LayoutSearch(
            LayoutFrame frame,
            LayoutState state,
            List<LayoutEngine.Unit> units,
            List<List<GuiVec>> exclusions,
            LeaderSolver leaders,
            Work work,
            Set<String> held,
            Plan fixed,
            boolean layered) {
        this.held = held;
        this.fixed = fixed;
        this.layered = layered;
        this.frame = frame;
        this.state = state;
        this.units = units;
        this.exclusions = exclusions;
        this.leaders = leaders;
        this.work = work;
        Map<CandidateKey, List<PanelPlacement>> shared = new HashMap<>();
        for (LayoutEngine.Unit unit : units) {
            PanelMemory memory = state.panels.get(unit.id());
            PanelRequest request = unit.active();
            CandidateKey key = new CandidateKey(
                    unit.source().id(),
                    request.space(),
                    request.position(),
                    request.orientation(),
                    request.material(),
                    request.metrics(),
                    request.fixedScreen(),
                    request.compactAllowed(),
                    request.id().equals(frame.interaction().focusedId()),
                    memory);
            List<PanelPlacement> template = shared.get(key);
            List<PanelPlacement> candidates;
            if (template == null) {
                candidates = LayoutEngine.candidates(frame, unit, memory, exclusions);
                shared.put(key, candidates);
            } else {
                candidates = new ArrayList<>(template.size());
                for (PanelPlacement c : template) {
                    candidates.add(new PanelPlacement(
                            unit.id(),
                            unit.members(),
                            request,
                            unit.source(),
                            c.slot(),
                            c.tier(),
                            c.space(),
                            c.pose(),
                            c.worldWidth(),
                            c.worldHeight(),
                            c.screenRect(),
                            c.polygon(),
                            c.score(),
                            unit.merged()));
                }
            }
            domains.put(
                    unit.id(),
                    candidates.stream()
                            .filter(c -> !held.contains(unit.id()) || !RecoveryGate.upgrade(memory, c))
                            .toList());
            byId.put(unit.id(), unit);
        }
    }

    /**
     * The top-level joint allocation: panels first, then dock/drawer
     * placement against what survived, re-solving when a needed overflow
     * entry displaces panels.
     */
    static Allocation allocate(
            LayoutFrame frame,
            LayoutState state,
            List<LayoutEngine.Unit> units,
            LeaderSolver leaders,
            Work work,
            Set<String> held) {
        List<List<GuiVec>> hud = new ArrayList<>();
        for (HudRegion region : frame.hud()) hud.add(region.polygon());
        boolean directed = units.stream()
                .anyMatch(u -> u.members().stream().anyMatch(r -> PanelRelations.participates(r, frame.requests())));
        if (directed) PanelRelations.layers(units); // validate acyclicity up front
        long nonOmittable = units.stream()
                .filter(u -> u.members().stream()
                        .anyMatch(r -> !r.omissionAllowed()
                                || r.priority() >= 25
                                || r.id().equals(frame.interaction().focusedId())))
                .count();
        boolean certainOverflow = !directed
                && (nonOmittable > frame.config().maxPanels()
                        || units.stream()
                                .anyMatch(u -> !u.source().present()
                                        || !u.source()
                                                .dimension()
                                                .equals(frame.clock().dimension())));
        GuiRect drawer = null;
        GuiRect reservedDock = null;
        if (certainOverflow) {
            if (frame.interaction().overflowOpen()) {
                drawer = LayoutEngine.findBox(
                        frame, state.drawer, hud, new double[][] {{320, 292}, {280, 180}, {220, 112}});
                if (drawer != null) hud.add(drawer.polygon());
            }
            reservedDock =
                    LayoutEngine.findBox(frame, state.dock, hud, new double[][] {{38, 30}, {108, 36}, {236, 42}});
            if (reservedDock != null) hud.add(reservedDock.polygon());
        }
        LayoutSearch search = new LayoutSearch(frame, state, units, hud, leaders, work, held);
        Plan plan = search.run();
        if (!overflow(frame, units, plan)) return new Allocation(plan, null, null, List.copyOf(hud));
        if (directed) {
            List<List<GuiVec>> occupied = new ArrayList<>(hud);
            for (PanelPlacement panel : plan.panels()) occupied.add(panel.polygon());
            if (frame.interaction().overflowOpen()) {
                drawer = LayoutEngine.findBox(
                        frame, state.drawer, occupied, new double[][] {{320, 292}, {280, 180}, {220, 112}});
                if (drawer != null && !routesClearOf(plan.routes(), drawer.polygon())) drawer = null;
                if (drawer != null) {
                    hud.add(drawer.polygon());
                    occupied.add(drawer.polygon());
                }
            }
            GuiRect dock =
                    LayoutEngine.findBox(frame, state.dock, occupied, new double[][] {{236, 42}, {108, 36}, {38, 30}});
            if (dock != null && !routesClearOf(plan.routes(), dock.polygon())) dock = null;
            if (dock != null) hud.add(dock.polygon());
            return new Allocation(plan, dock, drawer, List.copyOf(hud));
        }
        if (certainOverflow) return new Allocation(plan, reservedDock, drawer, List.copyOf(hud));

        if (frame.interaction().overflowOpen()) {
            drawer =
                    LayoutEngine.findBox(frame, state.drawer, hud, new double[][] {{320, 292}, {280, 180}, {220, 112}});
            if (drawer != null) {
                hud.add(drawer.polygon());
                search = new LayoutSearch(frame, state, units, hud, leaders, work, held);
                plan = search.run(plan);
            }
        }
        List<List<GuiVec>> occupied = new ArrayList<>(hud);
        for (PanelPlacement panel : plan.panels()) occupied.add(panel.polygon());
        GuiRect dock =
                LayoutEngine.findBox(frame, state.dock, occupied, new double[][] {{236, 42}, {108, 36}, {38, 30}});
        if (dock != null && routesClearOf(plan.routes(), dock.polygon())) {
            hud.add(dock.polygon());
            return new Allocation(plan, dock, drawer, List.copyOf(hud));
        }
        // Only a genuinely needed entry may displace panels. A feasible entry is
        // reserved even after search exhaustion; the empty plan is always legal.
        dock = LayoutEngine.findBox(frame, state.dock, hud, new double[][] {{38, 30}, {108, 36}, {236, 42}});
        if (dock != null) {
            hud.add(dock.polygon());
            search = new LayoutSearch(frame, state, units, hud, leaders, work, held);
            plan = search.run(plan);
        }
        if (!overflow(frame, units, plan)) {
            // All content fitting never pays for an unused dock or drawer.
            return new Allocation(
                    plan,
                    null,
                    null,
                    frame.hud().stream().map(HudRegion::polygon).toList());
        }
        return new Allocation(plan, dock, drawer, List.copyOf(hud));
    }

    /** Route a fixed set of panels as-is (used by reuse paths and the movement debounce). */
    static @Nullable Plan routeExisting(
            LayoutFrame frame,
            LayoutState state,
            List<PanelPlacement> panels,
            List<List<GuiVec>> exclusions,
            LeaderSolver leaders,
            Work work) {
        LayoutSearch search = new LayoutSearch(frame, state, List.of(), exclusions, leaders, work, Set.of());
        List<LeaderSolver.Option> routes = search.route(panels, List.of());
        return routes == null ? null : new Plan(List.copyOf(panels), routes, 0);
    }

    /** Does any unplaced unit still demand overflow (absent source, or not omittable)? */
    private static boolean overflow(LayoutFrame frame, List<LayoutEngine.Unit> units, Plan plan) {
        Set<String> visible = new HashSet<>();
        for (PanelPlacement candidate : plan.panels()) visible.add(candidate.visualId());
        for (LayoutEngine.Unit unit : units) {
            if (!visible.contains(unit.id())) {
                boolean absent = !unit.source().present()
                        || !unit.source().dimension().equals(frame.clock().dimension());
                for (PanelRequest request : unit.members()) {
                    if (absent
                            || !request.omissionAllowed()
                            || request.priority() >= 25
                            || request.id().equals(frame.interaction().focusedId())) {
                        return true;
                    }
                }
            }
        }
        return false;
    }

    private Plan run() {
        return run(null);
    }

    private Plan run(@Nullable Plan seed) {
        if (!layered
                && units.stream().anyMatch(u -> u.members().stream()
                        .anyMatch(r -> PanelRelations.participates(r, frame.requests())))) {
            // Yield DAG: solve layer by layer so yielding panels see their
            // targets already committed.
            Plan committed = new Plan(List.of(), List.of(), 0);
            for (List<LayoutEngine.Unit> layer : PanelRelations.layers(units)) {
                List<PanelRequest> requests = new ArrayList<>();
                for (PanelPlacement c : committed.panels()) requests.addAll(c.members());
                for (LayoutEngine.Unit unit : layer) requests.addAll(unit.members());
                LayoutFrame current = new LayoutFrame(
                        frame.clock(),
                        frame.camera(),
                        frame.viewBasis(),
                        frame.hud(),
                        frame.world(),
                        frame.sources(),
                        requests,
                        frame.interaction(),
                        frame.config());
                LayoutSearch next =
                        new LayoutSearch(current, state, layer, exclusions, leaders, work, held, committed, true);
                committed = next.run(committed);
            }
            return new Plan(committed.panels(), committed.routes(), totalCost(committed.panels()));
        }
        long combinations = 1;
        for (LayoutEngine.Unit unit : units) {
            combinations *= domains.get(unit.id()).size() + 1L;
            if (combinations > 4096) break;
        }
        if (combinations <= 4096
                && fixed.routes().isEmpty()
                && units.stream().noneMatch(u -> u.active().leaderMode() == LeaderMode.required)
                && combinations * Math.max(1, units.size()) < work.budget.expansions() - work.expansions) {
            return exact(
                    0,
                    new ArrayList<>(fixed.panels()),
                    new Plan(fixed.panels(), fixed.routes(), totalCost(fixed.panels())));
        }
        Plan best = beam(units);
        if (seed != null) {
            List<PanelPlacement> kept = new ArrayList<>();
            List<LeaderSolver.Option> routes = new ArrayList<>();
            for (PanelPlacement candidate : seed.panels()) {
                if (LayoutEngine.blocked(
                        candidate.polygon(), exclusions, frame.config().gap())) {
                    continue;
                }
                LeaderSolver.Option route = seed.routes().stream()
                        .filter(o -> o.leader().visualId().equals(candidate.visualId()))
                        .findFirst()
                        .orElse(null);
                if (route != null && exclusions.stream().anyMatch(p -> !routesClearOf(List.of(route), p))) {
                    continue;
                }
                kept.add(candidate);
                if (route != null) routes.add(route);
            }
            if (totalCost(kept) < best.cost()) {
                best = new Plan(List.copyOf(kept), List.copyOf(routes), totalCost(kept));
            }
        }
        if (best.panels().size() < units.size() && !work.limited) {
            List<LayoutEngine.Unit> constrained = new ArrayList<>(units);
            constrained.sort(Comparator.<LayoutEngine.Unit>comparingInt(
                            u -> domains.get(u.id()).size())
                    .thenComparingInt(u -> u.active().leaderMode() == LeaderMode.required ? 0 : 1)
                    .thenComparingDouble(u -> -LayoutEngine.weight(frame, u.active()))
                    .thenComparing(LayoutEngine.Unit::id));
            Plan alternative = beam(constrained);
            if (alternative.cost() < best.cost()) best = alternative;
        }
        for (int pass = 0; pass < 2 && !work.limited; pass++) {
            Plan before = best;
            for (LayoutEngine.Unit unit : units) {
                if (contains(best.panels(), unit.id())) continue;
                Plan improved = repair(best, new ArrayList<>(List.of(unit.id())), new HashSet<>(), 0, best);
                if (improved.cost() + 1e-7 < best.cost()) {
                    best = improved;
                    work.repairs++;
                }
            }
            if (before == best) break;
        }
        return best;
    }

    private Plan exact(int index, List<PanelPlacement> placed, Plan best) {
        if (index == units.size()) {
            double cost = totalCost(placed);
            return cost < best.cost() ? new Plan(List.copyOf(placed), List.of(), cost) : best;
        }
        String id = units.get(index).id();
        for (PanelPlacement candidate : domains.get(id)) {
            if (!work.step()) return best;
            if (placed.size() >= frame.config().maxPanels()
                    || conflicts(candidate, placed)
                    || area(placed) + Math.abs(ScreenMath.area(candidate.polygon())) > coverage()) {
                continue;
            }
            placed.add(candidate);
            best = exact(index + 1, placed, best);
            placed.remove(placed.size() - 1);
        }
        return exact(index + 1, placed, best);
    }

    private Plan beam(List<LayoutEngine.Unit> order) {
        List<Beam> beam = List.of(new Beam(fixed.panels(), fixed.routes(), 0, area(fixed.panels())));
        for (LayoutEngine.Unit unit : order) {
            List<Beam> next = new ArrayList<>();
            double foldedCost = LayoutEngine.weight(frame, unit.active()) * 1700;
            for (Beam parent : beam) {
                next.add(new Beam(parent.panels(), parent.routes(), parent.cost() + foldedCost, parent.area()));
                if (parent.panels().size() >= frame.config().maxPanels()) continue;
                for (PanelPlacement candidate : domains.get(unit.id())) {
                    if (!work.step()) break;
                    double area = Math.abs(ScreenMath.area(candidate.polygon()));
                    if (parent.panels().size() >= frame.config().maxPanels()
                            || parent.area() + area > coverage()
                            || conflicts(candidate, parent.panels())) {
                        continue;
                    }
                    List<PanelPlacement> panels = new ArrayList<>(parent.panels());
                    panels.add(candidate);
                    List<LeaderSolver.Option> routes = route(panels, parent.routes());
                    if (routes == null) continue;
                    next.add(new Beam(
                            List.copyOf(panels),
                            routes,
                            parent.cost()
                                    + cost(candidate)
                                    + parent.panels().stream()
                                            .mapToDouble(p -> alignment(candidate, p, frame.config()))
                                            .sum(),
                            parent.area() + area));
                }
            }
            beam = prune(next, frame.config().beamWidth());
        }
        Beam chosen = beam.stream().min(Comparator.comparingDouble(Beam::cost)).orElseThrow();
        return new Plan(chosen.panels(), chosen.routes(), totalCost(chosen.panels()));
    }

    /**
     * Bounded repair: place the pending units by bumping whatever they
     * overlap (which joins the pending queue), up to
     * {@code repairDepth} total pending placements.
     */
    private Plan repair(Plan partial, List<String> pending, Set<String> locked, int depth, Plan incumbent) {
        if (pending.isEmpty()) {
            if (partial.panels().size() > frame.config().maxPanels()
                    || area(partial.panels()) > coverage()
                    || totalCost(partial.panels()) >= incumbent.cost() - 1e-7) {
                return incumbent;
            }
            List<LeaderSolver.Option> routes = route(partial.panels(), List.of());
            return routes == null
                    ? incumbent
                    : new Plan(List.copyOf(partial.panels()), routes, totalCost(partial.panels()));
        }
        if (depth > work.budget.repairDepth() || work.limited) return incumbent;
        String id = pending.get(0);
        List<PanelPlacement> options = new ArrayList<>(domains.get(id));
        if (depth == 0 && partial.panels().size() < frame.config().maxPanels()) {
            options.addAll(contactCandidates(id, partial.panels()));
        }
        for (PanelPlacement candidate : options) {
            if (!work.step()) break;
            List<PanelPlacement> kept = new ArrayList<>();
            List<String> queue = new ArrayList<>(pending.subList(1, pending.size()));
            boolean blocked = false;
            for (PanelPlacement placed : partial.panels()) {
                if (overlap(candidate, placed, frame.config().gap())) {
                    if (locked.contains(placed.visualId()) || contains(fixed.panels(), placed.visualId())) {
                        blocked = true;
                        break;
                    }
                    if (!queue.contains(placed.visualId())) queue.add(placed.visualId());
                } else {
                    kept.add(placed);
                }
            }
            if (blocked || queue.size() + depth > work.budget.repairDepth()) continue;
            kept.add(candidate);
            if (area(kept) > coverage() || kept.size() > frame.config().maxPanels()) continue;
            Set<String> nextLocked = new HashSet<>(locked);
            nextLocked.add(id);
            incumbent = repair(new Plan(kept, List.of(), 0), queue, nextLocked, depth + 1, incumbent);
        }
        return incumbent;
    }

    /**
     * Extra screen-space slots used only by repair — contact-coordinate
     * positions around the still-placed panels this unit must separate
     * from.
     */
    private List<PanelPlacement> contactCandidates(String id, List<PanelPlacement> placed) {
        LayoutEngine.Unit unit = byId.get(id);
        if (unit.active().space() != Space.screen || unit.active().fixedScreen() != null) {
            return List.of();
        }
        List<List<GuiVec>> occupied = new ArrayList<>(exclusions);
        for (PanelPlacement panel : placed) {
            if (PanelRelations.mustSeparate(unit.active(), panel.active())) {
                occupied.add(panel.polygon());
            }
        }
        List<PanelPlacement> result = new ArrayList<>();
        Set<Tier> seen = new HashSet<>();
        for (PanelPlacement template : domains.get(id)) {
            if (!seen.add(template.tier())) continue;
            GuiRect size = template.screenRect();
            GuiVec preferred = ScreenSlots.preferredPoint(frame, template.pose());
            for (var entry : FreeSpace.slots(frame, size.width(), size.height(), preferred, occupied, 12)) {
                GuiRect rect = entry.getValue();
                double score = rect.center().distance(preferred) * .06
                        + LayoutEngine.memoryCost(
                                frame, state.panels.get(id), entry.getKey(), template.tier(), rect, Space.screen);
                result.add(new PanelPlacement(
                        id,
                        template.members(),
                        template.active(),
                        template.source(),
                        entry.getKey(),
                        template.tier(),
                        Space.screen,
                        template.pose(),
                        template.worldWidth(),
                        template.worldHeight(),
                        rect,
                        rect.polygon(),
                        score,
                        template.merged()));
            }
        }
        return result;
    }

    /**
     * Routes all REQUIRED leaders of {@code panels}, reusing prior routes
     * that stay clear of the new set and falling back to a bounded
     * per-panel assignment search. Null = infeasible.
     */
    private @Nullable List<LeaderSolver.Option> route(List<PanelPlacement> panels, List<LeaderSolver.Option> prior) {
        List<PanelPlacement> required = panels.stream()
                .filter(c -> c.active().leaderMode() == LeaderMode.required)
                .toList();
        if (required.isEmpty()) return List.of();
        if (required.stream().filter(c -> !c.active().position().mounted()).count()
                > frame.config().maxLeaders()) {
            return null;
        }
        List<PanelPlacement> key = List.copyOf(panels);
        if (routeCache.containsKey(key)) return routeCache.get(key);
        List<LeaderSolver.Option> reusable = prior.stream()
                .filter(o -> panels.stream()
                        .allMatch(c -> c.visualId().equals(o.leader().visualId())
                                || !LayoutMath.pathEnters(o.leader().screen(), ScreenMath.offset(c.polygon(), 3))))
                .toList();
        for (LeaderSolver.Option line : fixed.routes()) {
            for (PanelPlacement panel : panels) {
                if (!panel.visualId().equals(line.leader().visualId())
                        && LayoutMath.pathEnters(line.leader().screen(), ScreenMath.offset(panel.polygon(), 3))) {
                    return null;
                }
            }
        }
        List<PanelPlacement> unassigned = required.stream()
                .filter(c -> fixed.routes().stream()
                        .noneMatch(o -> o.leader().visualId().equals(c.visualId())))
                .toList();
        List<LeaderSolver.Option> result = assignRoutes(unassigned, panels, new ArrayList<>(fixed.routes()), reusable);
        if (routeCache.size() < 1024) routeCache.put(key, result);
        return result;
    }

    /**
     * Joint required-leader assignment. Routing order is significant —
     * an assigned leader constrains the remaining panels' options — so
     * each level picks the next panel dynamically: the first explored
     * path follows {@code required} order exactly, and alternative
     * orders are explored only when a branch proves infeasible.
     */
    private @Nullable List<LeaderSolver.Option> assignRoutes(
            List<PanelPlacement> required,
            List<PanelPlacement> panels,
            List<LeaderSolver.Option> assigned,
            List<LeaderSolver.Option> reusable) {
        if (required.isEmpty()) return List.copyOf(assigned);
        for (int pick = 0; pick < required.size(); pick++) {
            PanelPlacement candidate = required.get(pick);
            List<PanelPlacement> rest = new ArrayList<>(required);
            rest.remove(pick);
            List<LeaderSolver.Option> old = reusable.stream()
                    .filter(o -> o.leader().visualId().equals(candidate.visualId()))
                    .toList();
            for (LeaderSolver.Option option : old) {
                if (crosses(option, assigned)) continue;
                assigned.add(option);
                List<LeaderSolver.Option> result = assignRoutes(rest, panels, assigned, reusable);
                assigned.remove(assigned.size() - 1);
                if (result != null) return result;
            }
            if (work.routeCalls >= work.budget.routeEvaluations()) {
                work.limited = true;
                return null;
            }
            work.routeCalls++;
            List<LeaderLine> assignedLeaders =
                    assigned.stream().map(LeaderSolver.Option::leader).toList();
            List<LeaderSolver.Option> options =
                    leaders.options(frame, candidate, panels, exclusions, assignedLeaders, work.budget.routeVariants());
            for (LeaderSolver.Option option : options) {
                if (!work.step()) return null;
                assigned.add(option);
                List<LeaderSolver.Option> result = assignRoutes(rest, panels, assigned, reusable);
                assigned.remove(assigned.size() - 1);
                if (result != null) return result;
            }
        }
        return null;
    }

    private static boolean crosses(LeaderSolver.Option option, List<LeaderSolver.Option> assigned) {
        for (LeaderSolver.Option other : assigned) {
            if (LayoutMath.pathsCross(option.leader().screen(), other.leader().screen())) return true;
        }
        for (LeaderSolver.Option other : assigned) {
            if (LeaderSolver.crowdedBy(option.leader().screen(), other.leader())) return true;
        }
        return false;
    }

    private static boolean routesClearOf(List<LeaderSolver.Option> routes, List<GuiVec> polygon) {
        for (LeaderSolver.Option route : routes) {
            if (LayoutMath.pathEnters(route.leader().screen(), ScreenMath.offset(polygon, 3))) {
                return false;
            }
        }
        return true;
    }

    private boolean conflicts(PanelPlacement candidate, List<PanelPlacement> panels) {
        for (PanelPlacement other : panels) {
            if (overlap(candidate, other, frame.config().gap())) return true;
        }
        return false;
    }

    /** Panel-vs-panel overlap honoring the yield rules and the screen fast path. */
    static boolean overlap(PanelPlacement a, PanelPlacement b, double gap) {
        if (!PanelRelations.mustSeparate(a.active(), b.active())) return false;
        if (a.space() == Space.screen && b.space() == Space.screen) {
            GuiRect x = a.screenRect();
            GuiRect y = b.screenRect();
            return !(x.x() + x.width() + gap <= y.x() + 1e-7
                    || y.x() + y.width() + gap <= x.x() + 1e-7
                    || x.y() + x.height() + gap <= y.y() + 1e-7
                    || y.y() + y.height() + gap <= x.y() + 1e-7);
        }
        return LayoutMath.overlap(a.polygon(), b.polygon(), gap);
    }

    private double coverage() {
        return frame.camera().width() * frame.camera().height() * frame.config().maxCoverage();
    }

    private static double area(List<PanelPlacement> panels) {
        return panels.stream()
                .mapToDouble(c -> Math.abs(ScreenMath.area(c.polygon())))
                .sum();
    }

    private static boolean contains(List<PanelPlacement> panels, String id) {
        return panels.stream().anyMatch(c -> c.visualId().equals(id));
    }

    private double cost(PanelPlacement candidate) {
        return candidate.score()
                + (candidate.tier() == Tier.compact ? LayoutEngine.weight(frame, candidate.active()) * 360 : 0);
    }

    private double totalCost(List<PanelPlacement> panels) {
        double result = panels.stream().mapToDouble(this::cost).sum();
        for (int i = 0; i < panels.size(); i++) {
            for (int j = 0; j < i; j++) {
                result += alignment(panels.get(i), panels.get(j), frame.config());
            }
        }
        for (LayoutEngine.Unit unit : units) {
            if (!contains(panels, unit.id())) {
                result += LayoutEngine.weight(frame, unit.active()) * 1700;
            }
        }
        return result;
    }

    /** Small, order-independent preference for common rows/columns, never a constraint. */
    static double alignment(PanelPlacement a, PanelPlacement b, Config config) {
        if (a.space() != Space.screen || b.space() != Space.screen) return 0;
        GuiRect x = a.screenRect();
        GuiRect y = b.screenRect();
        double dx = Math.max(x.x(), y.x()) - Math.min(x.x() + x.width(), y.x() + y.width());
        double dy = Math.max(x.y(), y.y()) - Math.min(x.y() + x.height(), y.y() + y.height());
        boolean row = dx >= config.gap() - 1e-7
                && dx <= config.gap() + 32
                && (Math.abs(x.y() - y.y()) <= .5 || Math.abs(x.y() + x.height() - y.y() - y.height()) <= .5);
        boolean column = dy >= config.gap() - 1e-7
                && dy <= config.gap() + 32
                && (Math.abs(x.x() - y.x()) <= .5 || Math.abs(x.x() + x.width() - y.x() - y.width()) <= .5);
        return row || column ? -4.0 / Math.max(1, config.maxPanels()) : 0;
    }

    /**
     * Beam pruning: best cost first, a reserved slice of area-diverse
     * frontier entries, then occupancy-signature diversity, then fill by
     * cost.
     */
    private List<Beam> prune(List<Beam> input, int width) {
        input.sort(Comparator.comparingDouble(Beam::cost));
        if (input.size() <= width) return input;
        List<Beam> chosen = new ArrayList<>(input.subList(0, Math.max(1, width / 2)));
        List<Beam> frontier = new ArrayList<>();
        double smallest = Double.POSITIVE_INFINITY;
        for (Beam beam : input) {
            if (beam.area() < smallest - 1e-7) {
                frontier.add(beam);
                smallest = beam.area();
            }
        }
        int reserved = Math.max(1, width / 4);
        for (int i = 1; i <= reserved; i++) {
            if (chosen.size() >= width) break;
            Beam beam = frontier.get((frontier.size() - 1) * i / reserved);
            if (!chosen.contains(beam)) chosen.add(beam);
        }
        Set<String> signatures = new HashSet<>();
        for (Beam beam : chosen) signatures.add(signature(beam));
        for (Beam beam : input) {
            if (chosen.size() >= width) break;
            if (signatures.add(signature(beam))) chosen.add(beam);
        }
        for (Beam beam : input) {
            if (chosen.size() >= width) break;
            if (!chosen.contains(beam)) chosen.add(beam);
        }
        return chosen;
    }

    /** A coarse 4×3 occupancy signature for beam diversity. */
    private String signature(Beam beam) {
        int[] occupancy = new int[12];
        for (PanelPlacement panel : beam.panels()) {
            int x = Math.min(3, (int) (panel.center().x() / frame.camera().width() * 4));
            int y = Math.min(2, (int) (panel.center().y() / frame.camera().height() * 3));
            occupancy[y * 4 + x] += panel.tier() == Tier.full ? 10 : 1;
        }
        return Arrays.toString(occupancy) + ":" + Math.round(beam.area() / 1000);
    }

    private record GeometryKey(Tier tier, List<GuiVec> polygon) {}

    /**
     * Domain reduction: dedupe identical placements, then pick
     * {@code limit} entries — the first half by score order, the rest by
     * farthest-point diversity, sorted by score at the end.
     */
    static List<PanelPlacement> diverseCandidates(List<PanelPlacement> sorted, int limit) {
        LinkedHashMap<GeometryKey, PanelPlacement> unique = new LinkedHashMap<>();
        for (PanelPlacement candidate : sorted) {
            unique.putIfAbsent(new GeometryKey(candidate.tier(), candidate.polygon()), candidate);
        }
        List<PanelPlacement> candidates = new ArrayList<>(unique.values());
        int count = candidates.size();
        if (count <= limit) return candidates;
        double[] xs = new double[count];
        double[] ys = new double[count];
        double[] distance = new double[count];
        boolean[] used = new boolean[count];
        Arrays.fill(distance, Double.POSITIVE_INFINITY);
        for (int i = 0; i < count; i++) {
            GuiVec center = candidates.get(i).center();
            xs[i] = center.x();
            ys[i] = center.y();
        }
        List<PanelPlacement> chosen = new ArrayList<>(limit);
        for (int n = 0; n < limit; n++) {
            int best = n;
            if (n >= Math.max(1, limit / 2)) {
                best = -1;
                double farthest = -1;
                for (int i = 0; i < count; i++) {
                    if (!used[i] && distance[i] > farthest) {
                        best = i;
                        farthest = distance[i];
                    }
                }
            }
            used[best] = true;
            chosen.add(candidates.get(best));
            for (int i = 0; i < count; i++) {
                double dx = xs[i] - xs[best];
                double dy = ys[i] - ys[best];
                distance[i] = Math.min(distance[i], dx * dx + dy * dy);
            }
        }
        chosen.sort(Comparator.comparingDouble(PanelPlacement::score).thenComparing(PanelPlacement::slot));
        return chosen;
    }
}
