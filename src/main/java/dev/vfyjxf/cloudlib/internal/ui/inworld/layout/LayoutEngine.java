package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Config;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Depth;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiRect;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.HudRegion;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.IntentContext;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutFrame;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutResult;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutState;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LeaderLine;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LeaderMode;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Metrics;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.OverflowDock;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.OverflowDrawer;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.OverflowGroup;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelMemory;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelRequest;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelTransition;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Pose;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.RequestAddress;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SearchBudget;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SearchStats;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SourceProvider;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SourceSnapshot;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Space;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Tier;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Map.Entry;
import java.util.Objects;
import java.util.Set;
import java.util.TreeMap;
import java.util.TreeSet;

/**
 * The frame solver: samples sources, merges requests into visual units,
 * runs the joint panel/route search under the temporal gates (debounce,
 * recovery, stable-frame reuse), then assigns overflow addresses and
 * emits the {@link LayoutResult}. {@link #solve} is the only entry point;
 * {@link #lastStats} reports the search effort of the latest call.
 */
public final class LayoutEngine {

    private final LeaderSolver leaders = new LeaderSolver();
    private final SearchBudget budget;
    private SearchStats lastStats = new SearchStats(0, 0, 0, false, 0);

    public LayoutEngine() {
        this(SearchBudget.defaults());
    }

    public LayoutEngine(SearchBudget budget) {
        this.budget = Objects.requireNonNull(budget);
    }

    public SearchStats lastStats() {
        return lastStats;
    }

    /** A merge group — one rendered visual keyed by {@code id}, displaying {@code active}. */
    public record Unit(
            String id, List<PanelRequest> members, PanelRequest active, SourceSnapshot source, boolean merged) {}

    public LayoutResult solve(LayoutFrame frame, LayoutState state) {
        long started = System.nanoTime();
        validateInput(frame);
        if (!Objects.equals(state.dimension, frame.clock().dimension())
                || frame.clock().seconds() < state.lastTime) {
            state.reset();
            leaders.reset();
        }
        state.dimension = frame.clock().dimension();
        state.lastTime = frame.clock().seconds();
        state.debounce.begin(frame);
        leaders.bind(frame, state.debounce);

        // Sample each referenced source once.
        TreeMap<String, SourceSnapshot> snapshots = new TreeMap<>();
        List<String> diagnostics = new ArrayList<>();
        for (PanelRequest request : frame.requests()) {
            if (!snapshots.containsKey(request.sourceId())) {
                SourceProvider provider = frame.sources().get(request.sourceId());
                SourceSnapshot snapshot = provider == null
                        ? SourceSnapshot.absent(
                                request.sourceId(), frame.clock().dimension(), 0L)
                        : Objects.requireNonNull(provider.sample(frame.clock()), "null source snapshot");
                if (!snapshot.id().equals(request.sourceId())) {
                    throw new IllegalArgumentException("source key/id mismatch: " + request.sourceId());
                }
                snapshots.put(snapshot.id(), snapshot);
            }
        }

        state.panels
                .entrySet()
                .removeIf(e -> frame.clock().seconds() - e.getValue().seenAt() > 2.0);

        // Whole-result reuse: identical stable frame and no pending gates.
        StableFrame cacheKey = StableFrame.of(frame, snapshots);
        if (cacheKey != null
                && cacheKey.equals(state.cachedInput)
                && state.cachedResult != null
                && state.recovery.isEmpty()
                && !state.debounce.pending()) {
            LayoutResult previous = state.cachedResult;
            state.panels.replaceAll((id, memory) -> new PanelMemory(
                    memory.slot(),
                    memory.tier(),
                    memory.pose(),
                    memory.rect(),
                    memory.space(),
                    memory.changedAt(),
                    frame.clock().seconds(),
                    memory.generation()));
            lastStats = new SearchStats(0, 0, 0, false, System.nanoTime() - started);
            return new LayoutResult(
                    frame.clock().frameIndex(),
                    previous.panels(),
                    previous.leaders(),
                    previous.dock(),
                    previous.drawer(),
                    previous.addresses(),
                    List.of(),
                    previous.diagnostics(),
                    snapshots);
        }

        List<Unit> units = units(frame, snapshots);
        for (Unit unit : units) {
            PanelMemory memory = state.panels.get(unit.id());
            if (memory != null && memory.generation() != unit.source().generation()) {
                state.panels.remove(unit.id());
                state.recovery.remove(unit.id());
            }
        }

        LayoutSearch.Work work = new LayoutSearch.Work(budget);
        LayoutSearch.Allocation allocation =
                ScreenReuse.tryReuse(frame, state, units, snapshots, cacheKey, leaders, work);
        if (allocation == null) {
            allocation = LayoutSearch.allocate(frame, state, units, leaders, work, Set.of());
        }

        // Recovery gate: panels held below their recovered tier re-solve
        // with the held set as a constraint.
        Set<String> held = RecoveryGate.observe(frame, state, units, allocation.plan());
        LayoutSearch.Work recoveryWork = null;
        if (allocation.plan().panels().stream()
                .anyMatch(
                        c -> held.contains(c.visualId()) && RecoveryGate.upgrade(state.panels.get(c.visualId()), c))) {
            recoveryWork = new LayoutSearch.Work(budget);
            allocation = LayoutSearch.allocate(frame, state, units, leaders, recoveryWork, held);
        }

        allocation = state.debounce.panels(frame, state, units, allocation, leaders, work);

        GuiRect dockRect = allocation.dock();
        GuiRect drawerRect = allocation.drawer();
        state.dock = dockRect;
        state.drawer = drawerRect;
        List<List<GuiVec>> exclusions = allocation.exclusions();

        Map<String, String> reasons = new HashMap<>();
        for (Unit unit : units) {
            if (!unit.source().present()
                    || !unit.source().dimension().equals(frame.clock().dimension())) {
                reasons.put(unit.id(), "SOURCE_UNAVAILABLE");
            } else if (held.contains(unit.id())) {
                reasons.put(unit.id(), "RECOVERY_PENDING");
            } else if (work.limited || recoveryWork != null && recoveryWork.limited) {
                reasons.put(unit.id(), "SEARCH_BUDGET");
            }
        }

        List<PanelPlacement> panels = new ArrayList<>(allocation.plan().panels());
        List<LeaderLine> leaderLines = new ArrayList<>();
        for (LeaderSolver.Option option : allocation.plan().routes()) {
            PanelPlacement owner = panels.stream()
                    .filter(c -> c.visualId().equals(option.leader().visualId()))
                    .findFirst()
                    .orElseThrow();
            leaders.commitRequired(
                    owner,
                    option,
                    panels,
                    exclusions,
                    allocation.plan().routes().stream()
                            .filter(o -> !o.leader().visualId().equals(owner.visualId()))
                            .map(LeaderSolver.Option::leader)
                            .toList());
            leaderLines.add(option.leader());
        }

        // AUTO leaders resolve after REQUIRED routes committed — yield layer
        // first, then focused, then panels that already show a leader, then weight.
        List<PanelPlacement> autoOrder = new ArrayList<>(panels);
        Map<String, Integer> yieldOrder = new HashMap<>();
        int layerIndex = 0;
        for (List<Unit> layer : PanelRelations.layers(units)) {
            for (Unit unit : layer) yieldOrder.put(unit.id(), layerIndex);
            layerIndex++;
        }
        autoOrder.sort(Comparator.<PanelPlacement>comparingInt(c -> yieldOrder.getOrDefault(c.visualId(), 0))
                .thenComparingInt(
                        c -> c.active().id().equals(frame.interaction().focusedId()) ? -1 : state.debounce.ownerRank(c))
                .thenComparingDouble(c -> -weight(frame, c.active()))
                .thenComparing(PanelPlacement::visualId));
        Set<String> autoSources = new HashSet<>();
        for (LeaderLine line : leaderLines) {
            if (!line.screen().isEmpty()) autoSources.add(line.sourceId());
        }
        for (PanelPlacement candidate : autoOrder) {
            if (candidate.active().leaderMode() == LeaderMode.auto) {
                if (autoSources.contains(candidate.source().id())) {
                    leaderLines.add(LeaderSolver.hidden(
                            candidate,
                            candidate.members().stream().map(PanelRequest::id).toList(),
                            "SOURCE_ALREADY_LINKED"));
                } else {
                    LeaderLine line = leaders.solve(
                            frame,
                            candidate,
                            panels,
                            exclusions,
                            leaderLines,
                            leaderLines.stream()
                                            .filter(l -> !l.screen().isEmpty())
                                            .count()
                                    < frame.config().maxLeaders());
                    leaderLines.add(line);
                    if (!line.screen().isEmpty())
                        autoSources.add(candidate.source().id());
                }
            }
        }
        state.debounce.end(units, leaderLines);

        lastStats = new SearchStats(
                work.expansions + (recoveryWork == null ? 0 : recoveryWork.expansions),
                work.routeCalls + (recoveryWork == null ? 0 : recoveryWork.routeCalls),
                work.repairs + (recoveryWork == null ? 0 : recoveryWork.repairs),
                work.limited || recoveryWork != null && recoveryWork.limited,
                System.nanoTime() - started);
        if (lastStats.budgetReached()) {
            diagnostics.add("SEARCH_BUDGET: feasible incumbent retained; optimality not established");
        }

        // Per-request addresses, panel memory and transitions.
        TreeMap<String, RequestAddress> addresses = new TreeMap<>();
        TreeMap<String, List<String>> foldedGroups = new TreeMap<>();
        List<PanelTransition> transitions = new ArrayList<>();
        Set<String> placed = new HashSet<>();
        Map<String, PanelMemory> previousMemory = Map.copyOf(state.panels);

        for (PanelPlacement candidate : panels) {
            placed.add(candidate.visualId());
            PanelMemory memory = state.panels.get(candidate.visualId());
            GuiRect bounds = LayoutMath.bounds(candidate.polygon());
            if (memory != null && !Objects.equals(memory.rect(), bounds)) {
                String kind;
                if (candidate.space() == Space.screen
                        && memory.space() == Space.screen
                        && memory.rect() != null
                        && leaderLines.stream().allMatch(line -> line.screen().isEmpty())
                        && safeSweep(frame, candidate, memory.rect(), bounds, panels, exclusions, previousMemory)) {
                    kind = "SLIDE_SAFE";
                } else if (candidate.space() == Space.world && Objects.equals(memory.slot(), candidate.slot())) {
                    kind = "TRACK_POSE";
                } else {
                    kind = "CUT_FADE_IN";
                }
                transitions.add(new PanelTransition(candidate.visualId(), kind, memory.rect(), bounds));
            }

            double changedAt = memory != null
                            && memory.tier() == candidate.tier()
                            && memory.slot().equals(candidate.slot())
                    ? memory.changedAt()
                    : frame.clock().seconds();
            state.panels.put(
                    candidate.visualId(),
                    new PanelMemory(
                            candidate.slot(),
                            candidate.tier(),
                            candidate.pose(),
                            bounds,
                            candidate.space(),
                            changedAt,
                            frame.clock().seconds(),
                            candidate.source().generation()));

            for (PanelRequest member : candidate.members()) {
                addresses.put(
                        member.id(),
                        new RequestAddress(
                                member.id(),
                                candidate.merged() ? Tier.merged : candidate.tier(),
                                candidate.visualId(),
                                candidate.merged()
                                        ? "TAB_MEMBER; active="
                                                + candidate.active().id()
                                        : "VISIBLE"));
            }
        }

        for (Unit unit : units) {
            if (!placed.contains(unit.id())) {
                String reason = reasons.getOrDefault(unit.id(), "CAPACITY_OR_STABILITY");
                boolean absent = !unit.source().present()
                        || !unit.source().dimension().equals(frame.clock().dimension());
                PanelMemory memory = state.panels.get(unit.id());
                double changedAt = memory != null && memory.tier() == Tier.folded
                        ? memory.changedAt()
                        : frame.clock().seconds();
                state.panels.put(
                        unit.id(),
                        new PanelMemory(
                                "folded",
                                Tier.folded,
                                memory == null ? null : memory.pose(),
                                null,
                                unit.active().space(),
                                changedAt,
                                frame.clock().seconds(),
                                unit.source().generation()));

                for (PanelRequest member : unit.members()) {
                    boolean omittable = member.omissionAllowed()
                            && member.priority() < 25
                            && !member.id().equals(frame.interaction().focusedId())
                            && !absent;
                    Tier tier = omittable ? Tier.omitted : (absent ? Tier.unavailable : Tier.folded);
                    String group = group(member);
                    addresses.put(
                            member.id(), new RequestAddress(member.id(), tier, omittable ? "omitted" : group, reason));
                    if (!omittable) {
                        foldedGroups
                                .computeIfAbsent(group, k -> new ArrayList<>())
                                .add(member.id());
                    }
                }
            }
        }

        List<OverflowGroup> groups = new ArrayList<>();
        List<String> foldedIds = new ArrayList<>();
        for (Entry<String, List<String>> entry : foldedGroups.entrySet()) {
            Collections.sort(entry.getValue());
            groups.add(new OverflowGroup(entry.getKey(), entry.getValue()));
            foldedIds.addAll(entry.getValue());
        }

        List<String> pageable = new ArrayList<>();
        for (OverflowGroup group : groups) {
            if (frame.interaction().overflowGroup() == null
                    || group.id().equals(frame.interaction().overflowGroup())) {
                pageable.addAll(group.requestIds());
            }
        }
        int pageSize = drawerRect == null ? 6 : Math.max(1, (int) ((drawerRect.height() - 44.0) / 32.0));
        int pageCount = Math.max(1, (pageable.size() + pageSize - 1) / pageSize);
        int page = Math.max(0, Math.min(pageCount - 1, frame.interaction().page()));
        List<String> pageItems = List.copyOf(pageable.subList(
                Math.min(pageable.size(), page * pageSize), Math.min(pageable.size(), (page + 1) * pageSize)));
        OverflowDock dock = new OverflowDock(
                dockRect,
                dockRect == null && !foldedIds.isEmpty(),
                groups,
                foldedIds.size(),
                page,
                pageCount,
                pageItems);
        OverflowDrawer drawer = drawerRect == null ? null : new OverflowDrawer(drawerRect, pageItems, page, pageCount);

        if (dock.externalOnly() && dock.total() > 0) {
            diagnostics.add(
                    "NO_SCREEN_CAPACITY: overflow registry is available through host action; no HUD-safe on-screen entry exists");
        }
        if (frame.interaction().overflowOpen() && drawerRect == null) {
            diagnostics.add("DRAWER_NO_SPACE: host must open a separate screen or release an exclusion region");
        }
        for (LeaderLine line : leaderLines) {
            if (line.screen().isEmpty() && !line.status().equals("ATTACHED")) {
                diagnostics.add(line.visualId() + ": leader " + line.status());
            }
        }

        LayoutResult result = new LayoutResult(
                frame.clock().frameIndex(),
                panels,
                leaderLines,
                dock,
                drawer,
                addresses,
                transitions,
                diagnostics,
                snapshots);
        List<String> violations = validate(frame, result);
        if (!violations.isEmpty()) {
            throw new IllegalStateException("result constraint violation: " + violations);
        }

        // Only a fully settled plan (everything FULL or steady-state) becomes the reuse key.
        boolean settled = true;
        for (Unit unit : units) {
            PanelMemory before = previousMemory.get(unit.id());
            PanelMemory after = state.panels.get(unit.id());
            if (after.tier() != Tier.full && (before == null || before.tier() != after.tier())) {
                settled = false;
            }
        }
        state.cachedInput = settled ? cacheKey : null;
        state.cachedResult = result;
        lastStats = new SearchStats(
                lastStats.expansions(),
                lastStats.routeEvaluations(),
                lastStats.repairs(),
                lastStats.budgetReached(),
                System.nanoTime() - started);
        return result;
    }

    static String group(PanelRequest request) {
        return request.overflowGroup() != null && !request.overflowGroup().isBlank()
                ? request.overflowGroup()
                : "other";
    }

    static double weight(LayoutFrame frame, PanelRequest request) {
        return request.id().equals(frame.interaction().focusedId()) ? 100.0 : 1.0 + request.priority() / 20.0;
    }

    /**
     * Groups requests into visual units: merge-eligible requests sharing
     * the same merge key become one visual (active = the selected member
     * or the highest-weight one); everything else stays a singleton.
     */
    static List<Unit> units(LayoutFrame frame, Map<String, SourceSnapshot> snapshots) {
        List<PanelRequest> sorted = new ArrayList<>(frame.requests());
        sorted.sort(
                Comparator.<PanelRequest>comparingDouble(r -> -weight(frame, r)).thenComparing(PanelRequest::id));
        LinkedHashMap<String, List<PanelRequest>> groups = new LinkedHashMap<>();
        for (PanelRequest request : sorted) {
            SourceSnapshot source = snapshots.get(request.sourceId());
            boolean mergeable = request.mergeAllowed()
                    && !PanelRelations.participates(request, frame.requests())
                    && request.mergeKey() != null
                    && !request.mergeKey().isBlank()
                    && request.position().canRelocate()
                    && request.fixedScreen() == null
                    && !request.id().equals(frame.interaction().focusedId())
                    && source.present()
                    && source.dimension().equals(frame.clock().dimension());
            String key = mergeable
                    ? "merge:" + request.family()
                            + ":" + request.mergeKey()
                            + ":" + request.sourceId()
                            + ":" + request.space()
                            + ":" + request.material()
                            + ":" + request.metrics()
                            + ":" + request.position()
                            + ":" + request.orientation()
                            + ":" + request.leaderMode()
                            + ":" + request.compactAllowed()
                            + ":" + request.crossSpaceAvoidance()
                            + ":" + new TreeSet<>(request.yieldTo())
                            + ":" + request.yieldScope()
                    : "single:" + request.id();
            groups.computeIfAbsent(key, k -> new ArrayList<>()).add(request);
        }

        List<Unit> units = new ArrayList<>();
        for (Entry<String, List<PanelRequest>> entry : groups.entrySet()) {
            List<PanelRequest> members = entry.getValue();
            PanelRequest active = members.get(0);
            for (PanelRequest member : members) {
                if (member.id().equals(frame.interaction().selectedMemberId())) {
                    active = member;
                }
            }
            boolean merged = members.size() > 1;
            String id = merged
                    ? "group:"
                            + members.stream()
                                    .map(PanelRequest::id)
                                    .sorted()
                                    .reduce((a, b) -> a + "|" + b)
                                    .orElseThrow()
                    : active.id();
            units.add(new Unit(id, members, active, snapshots.get(active.sourceId()), merged));
        }
        units.sort(Comparator.<Unit>comparingDouble(u -> -u.members().stream()
                        .mapToDouble(r -> weight(frame, r))
                        .max()
                        .orElse(0.0))
                .thenComparing(Unit::id));
        return units;
    }

    /**
     * The placement domain of one unit: every pose the position policy
     * offers at each allowed tier, projected (world panels) or slotted
     * (screen panels), filtered by viewport, readability, collision and
     * occlusion, costed and diversity-pruned.
     */
    static List<PanelPlacement> candidates(
            LayoutFrame frame, Unit unit, @Nullable PanelMemory memory, List<List<GuiVec>> exclusions) {
        if (!unit.source().present()
                || !unit.source().dimension().equals(frame.clock().dimension())) {
            return List.of();
        }
        PanelRequest request = unit.active();
        new IntentContext(frame, unit.source(), memory == null ? null : memory.pose());
        List<PanelPlacement> domain = new ArrayList<>();
        List<Tier> tiers = request.compactAllowed()
                        && !request.id().equals(frame.interaction().focusedId())
                ? List.of(Tier.full, Tier.compact)
                : List.of(Tier.full);
        for (Tier tier : tiers) {
            Metrics metrics = request.metrics();
            double worldWidth = metrics.worldWidth();
            double worldHeight = tier == Tier.compact ? metrics.compactWorldHeight() : metrics.worldHeight();
            List<WorldPlacement.Placement> placements =
                    WorldPlacement.propose(frame, request, unit.source(), memory == null ? null : memory.pose(), tier);
            List<PanelPlacement> options = new ArrayList<>();
            for (WorldPlacement.Placement placement : placements) {
                Pose pose = placement.pose();
                if (request.space() != Space.world) {
                    double screenWidth = tier == Tier.compact ? metrics.compactScreenWidth() : metrics.screenWidth();
                    double screenHeight = tier == Tier.compact ? metrics.compactScreenHeight() : metrics.screenHeight();
                    for (Entry<String, GuiRect> entry :
                            ScreenSlots.slots(frame, request, pose, screenWidth, screenHeight, memory)) {
                        GuiRect rect = entry.getValue();
                        List<GuiVec> polygon = rect.polygon();
                        if (LayoutMath.inView(
                                        polygon, frame.camera(), frame.config().margin())
                                && LayoutMath.readable(polygon, metrics, tier)
                                && !blocked(polygon, exclusions, frame.config().gap())) {
                            double score = rect.center().distance(ScreenSlots.preferredPoint(frame, pose)) * .06
                                    + memoryCost(frame, memory, entry.getKey(), tier, rect, request.space());
                            options.add(new PanelPlacement(
                                    unit.id(),
                                    unit.members(),
                                    request,
                                    unit.source(),
                                    entry.getKey(),
                                    tier,
                                    request.space(),
                                    pose,
                                    worldWidth,
                                    worldHeight,
                                    rect,
                                    polygon,
                                    score,
                                    unit.merged()));
                        }
                    }
                    break;
                }
                List<GuiVec> polygon = LayoutMath.projected(frame.camera(), pose, worldWidth, worldHeight);
                if (LayoutMath.inView(polygon, frame.camera(), frame.config().margin())
                        && LayoutMath.readable(polygon, metrics, tier)
                        && !blocked(polygon, exclusions, frame.config().gap())
                        && (request.material().depth() != Depth.test
                                || WorldPlacement.visiblePanel(frame, pose, worldWidth, worldHeight))) {
                    double score = placement.preference()
                            + memoryCost(
                                    frame, memory, placement.key(), tier, LayoutMath.bounds(polygon), request.space());
                    options.add(new PanelPlacement(
                            unit.id(),
                            unit.members(),
                            request,
                            unit.source(),
                            placement.key(),
                            tier,
                            request.space(),
                            pose,
                            worldWidth,
                            worldHeight,
                            null,
                            polygon,
                            score,
                            unit.merged()));
                }
            }
            options.sort(Comparator.comparingDouble(PanelPlacement::score).thenComparing(PanelPlacement::slot));
            domain.addAll(LayoutSearch.diverseCandidates(
                    options, Math.max(8, frame.config().candidatesPerTier() * 3)));
        }
        return domain;
    }

    static double memoryCost(
            LayoutFrame frame, @Nullable PanelMemory memory, String slot, Tier tier, GuiRect rect, Space space) {
        if (memory == null || memory.rect() == null || memory.space() != space) return 0.0;
        double cost = memory.slot().equals(slot) ? 0.0 : frame.config().switchPenalty();
        if (memory.tier() != tier) cost += frame.config().switchPenalty() * .4;
        return cost + Math.min(600.0, memory.rect().center().distance(rect.center())) * .18;
    }

    static boolean blocked(List<GuiVec> polygon, List<List<GuiVec>> exclusions, double gap) {
        for (List<GuiVec> exclusion : exclusions) {
            if (LayoutMath.overlap(polygon, exclusion, gap)) return true;
        }
        return false;
    }

    /**
     * Finds a free rect for the dock/drawer: the remembered rect when it
     * still fits, then each candidate size at the nine anchor spots,
     * free-space search, and finally a 24px grid scan.
     */
    static @Nullable GuiRect findBox(
            LayoutFrame frame, @Nullable GuiRect cached, List<List<GuiVec>> exclusions, double[][] sizes) {
        if (cached != null
                && LayoutMath.inView(
                        cached.polygon(), frame.camera(), frame.config().margin())
                && !blocked(cached.polygon(), exclusions, frame.config().gap())) {
            return cached;
        }
        double margin = frame.config().margin();
        double viewWidth = frame.camera().width();
        double viewHeight = frame.camera().height();
        for (double[] size : sizes) {
            double width = size[0];
            double height = size[1];
            if (width > viewWidth - 2.0 * margin || height > viewHeight - 2.0 * margin) continue;
            for (double y : new double[] {margin, viewHeight - margin - height, (viewHeight - height) * .5}) {
                for (double x : new double[] {viewWidth - margin - width, margin, (viewWidth - width) * .5}) {
                    GuiRect rect = new GuiRect(x, y, width, height);
                    if (!blocked(rect.polygon(), exclusions, frame.config().gap())) return rect;
                }
            }
            for (var entry : FreeSpace.slots(
                    frame,
                    width,
                    height,
                    new GuiVec(viewWidth - margin - width / 2, margin + height / 2),
                    exclusions,
                    8)) {
                return entry.getValue();
            }
            for (double y = margin; y + height <= viewHeight - margin; y += 24.0) {
                for (double x = viewWidth - margin - width; x >= margin; x -= 24.0) {
                    GuiRect rect = new GuiRect(x, y, width, height);
                    if (!blocked(rect.polygon(), exclusions, frame.config().gap())) return rect;
                }
            }
        }
        return null;
    }

    /**
     * Is sliding {@code candidate} from {@code from} to {@code to} safe
     * while every other panel moves simultaneously (or cuts straight to
     * its destination)? Used to classify a transition as
     * {@code "SLIDE_SAFE"} rather than a fade cut.
     */
    static boolean safeSweep(
            LayoutFrame frame,
            PanelPlacement candidate,
            GuiRect from,
            GuiRect to,
            List<PanelPlacement> panels,
            List<List<GuiVec>> exclusions,
            Map<String, PanelMemory> previous) {
        if (candidate.space() != Space.screen || candidate.active().leaderMode() != LeaderMode.none) return false;
        if (!LayoutMath.inView(from.polygon(), frame.camera(), frame.config().margin())
                || !LayoutMath.inView(
                        to.polygon(), frame.camera(), frame.config().margin())) {
            return false;
        }
        for (List<GuiVec> polygon : exclusions) {
            if (MotionSafety.collide(from, to, polygon, frame.config().gap())) return false;
        }
        for (PanelPlacement other : panels) {
            if (other.visualId().equals(candidate.visualId())) continue;
            if (!PanelRelations.mustSeparate(candidate.active(), other.active())) continue;
            GuiRect target = LayoutMath.bounds(other.polygon());
            PanelMemory memory = previous.get(other.visualId());
            GuiRect start = memory != null && memory.rect() != null ? memory.rect() : target;
            if (other.space() != Space.screen && !start.equals(target)) return false;
            // Test synchronized slides and a neighbor cut directly to its destination.
            if (MotionSafety.collide(from, to, start, target, frame.config().gap())
                    || MotionSafety.collide(
                            from, to, target, target, frame.config().gap())) {
                return false;
            }
        }
        return true;
    }

    static void validateInput(LayoutFrame frame) {
        Set<String> ids = new HashSet<>();
        for (PanelRequest request : frame.requests()) {
            if (!ids.add(request.id())) {
                throw new IllegalArgumentException("duplicate UI id: " + request.id());
            }
        }
        Config config = frame.config();
        if (config.beamWidth() < 1
                || config.candidatesPerTier() < 1
                || config.gap() < 0.0
                || config.margin() < 0.0
                || config.maxLeaders() < 0
                || config.maxPanels() < 0
                || config.maxCoverage() <= 0.0
                || config.maxCoverage() > 1.0) {
            throw new IllegalArgumentException("invalid config");
        }
        for (HudRegion hud : frame.hud()) {
            if (Math.abs(ScreenMath.area(hud.polygon())) < 1.0E-8
                    || ScreenMath.hull(hud.polygon()).size() != hud.polygon().size()) {
                throw new IllegalArgumentException("HUD regions must be convex; split concave shapes: " + hud.id());
            }
        }
    }

    /**
     * The full post-solve invariant check — every panel inside the
     * viewport and readable, no overlaps, occlusion or collision respected,
     * every request accounted exactly once, leader constraints enforced.
     * {@code solve} throws on violation; exposed for tests.
     */
    public static List<String> validate(LayoutFrame frame, LayoutResult result) {
        List<String> violations = new ArrayList<>();
        Map<String, Integer> accounting = new HashMap<>();
        if (result.panels().size() > frame.config().maxPanels()) {
            violations.add("panel count limit");
        }
        if (result.leaders().stream().filter(l -> !l.screen().isEmpty()).count()
                > frame.config().maxLeaders()) {
            violations.add("leader count limit");
        }
        if (result.dock().total() == 0
                && (result.dock().rect() != null || result.dock().externalOnly())) {
            violations.add("unused overflow entry");
        }
        if (result.panels().stream()
                        .mapToDouble(c -> Math.abs(ScreenMath.area(c.polygon())))
                        .sum()
                > frame.camera().width()
                                * frame.camera().height()
                                * frame.config().maxCoverage()
                        + 1.0E-7) {
            violations.add("coverage limit");
        }

        List<List<GuiVec>> exclusions = new ArrayList<>();
        for (HudRegion hud : frame.hud()) exclusions.add(hud.polygon());
        if (result.dock().rect() != null) exclusions.add(result.dock().rect().polygon());
        if (result.drawer() != null) exclusions.add(result.drawer().rect().polygon());
        for (int i = 0; i < exclusions.size(); i++) {
            if (i >= frame.hud().size()) {
                if (!LayoutMath.inView(
                        exclusions.get(i), frame.camera(), frame.config().margin())) {
                    violations.add("entry outside viewport");
                }
                for (int j = 0; j < i; j++) {
                    if (LayoutMath.overlap(
                            exclusions.get(i), exclusions.get(j), frame.config().gap())) {
                        violations.add("entry overlaps HUD/entry");
                    }
                }
            }
        }

        for (int i = 0; i < result.panels().size(); i++) {
            PanelPlacement candidate = result.panels().get(i);
            if (candidate.active().leaderMode() == LeaderMode.required
                    && result.leaders().stream()
                            .noneMatch(l -> l.visualId().equals(candidate.visualId())
                                    && (!l.screen().isEmpty() || l.status().equals("ATTACHED")))) {
                violations.add(candidate.visualId() + " missing required leader");
            }
            if (candidate.space() == Space.screen
                    && candidate.active().fixedScreen() != null
                    && !candidate.active().fixedScreen().equals(candidate.screenRect())) {
                violations.add(candidate.visualId() + " moved fixed screen panel");
            }
            if (candidate.space() == Space.world) {
                IntentContext context = new IntentContext(frame, candidate.source(), candidate.pose());
                if (candidate.active().position().mounted()
                        && !WorldPlacement.mountedFits(
                                candidate.active(),
                                context,
                                candidate.pose(),
                                candidate.worldWidth(),
                                candidate.worldHeight())) {
                    violations.add(candidate.visualId() + " outside mounted patch");
                }
                if (!candidate.active().position().canRelocate()
                        && candidate
                                .active()
                                .position()
                                .slots(context, candidate.worldWidth(), candidate.worldHeight())
                                .stream()
                                .noneMatch(slot -> slot.center()
                                                .distanceTo(candidate.pose().origin())
                                        < 1e-7)) {
                    violations.add(candidate.visualId() + " moved fixed world panel");
                }
            }
            for (PanelRequest member : candidate.members()) {
                accounting.merge(member.id(), 1, Integer::sum);
            }
            if (!LayoutMath.inView(
                            candidate.polygon(), frame.camera(), frame.config().margin())
                    || !LayoutMath.readable(
                            candidate.polygon(), candidate.active().metrics(), candidate.tier())) {
                violations.add(candidate.visualId() + " unreadable/outside");
            }
            if (blocked(candidate.polygon(), exclusions, frame.config().gap())) {
                violations.add(candidate.visualId() + " overlaps exclusion");
            }
            for (int j = 0; j < i; j++) {
                if (LayoutSearch.overlap(
                        candidate, result.panels().get(j), frame.config().gap())) {
                    violations.add(candidate.visualId() + " overlaps panel");
                }
            }
            if (candidate.space() == Space.world
                    && candidate
                                    .pose()
                                    .basis()
                                    .normal()
                                    .dot(frame.camera()
                                            .eye()
                                            .subtract(candidate.pose().origin()))
                            <= 1.0E-7) {
                violations.add(candidate.visualId() + " backface");
            }
            if (candidate.space() == Space.world
                    && candidate.active().material().depth() == Depth.test
                    && !WorldPlacement.visiblePanel(
                            frame, candidate.pose(), candidate.worldWidth(), candidate.worldHeight())) {
                violations.add(candidate.visualId() + " occluded beyond limit");
            }
            if (candidate.space() == Space.world
                    && candidate.active().material().requiresFreeWorldSpace()
                    && !frame.world()
                            .free(
                                    candidate.pose(),
                                    candidate.worldWidth(),
                                    candidate.worldHeight(),
                                    frame.config().worldClearance())) {
                violations.add(candidate.visualId() + " world collision");
            }
        }

        Set<String> overflow = new HashSet<>();
        for (OverflowGroup group : result.dock().groups()) {
            for (String id : group.requestIds()) {
                accounting.merge(id, 1, Integer::sum);
                overflow.add(id);
            }
        }
        if (overflow.size() != result.dock().total()) {
            violations.add("overflow count");
        }

        for (PanelRequest request : frame.requests()) {
            RequestAddress address = result.addresses().get(request.id());
            if (address == null) {
                violations.add("no address " + request.id());
                continue;
            }
            if (address.tier() == Tier.omitted) {
                if (!request.omissionAllowed()) {
                    violations.add("unauthorized omission " + request.id());
                }
                accounting.merge(request.id(), 1, Integer::sum);
            }
            if (accounting.getOrDefault(request.id(), 0) != 1) {
                violations.add("accounting " + request.id());
            }
        }
        if (result.addresses().size() != frame.requests().size()) {
            violations.add("address cardinality");
        }

        for (int i = 0; i < result.leaders().size(); i++) {
            LeaderLine line = result.leaders().get(i);
            if (line.screen().isEmpty()) continue;
            PanelPlacement owner = result.panels().stream()
                    .filter(p -> p.visualId().equals(line.visualId()))
                    .findFirst()
                    .orElse(null);
            if (line.depth() != Depth.test
                    || owner == null
                    || line.world().size() != line.screen().size()
                    || !LeaderVisibility.clear(frame, owner, line.screen(), line.world())) {
                violations.add("leader occlusion or depth policy");
            }
            if (line.screen().size() < 2) {
                violations.add("degenerate leader");
            }
            for (GuiVec point : line.screen()) {
                if (!Double.isFinite(point.x() + point.y()) || !ScreenMath.inViewport(point, frame.camera(), 2.0)) {
                    violations.add("leader outside");
                }
            }
            for (List<GuiVec> exclusion : exclusions) {
                if (LayoutMath.pathEnters(line.screen(), exclusion)) {
                    violations.add("leader exclusion");
                }
            }
            for (PanelPlacement panel : result.panels()) {
                if (!panel.visualId().equals(line.visualId())
                        && LayoutMath.pathEnters(line.screen(), panel.polygon())) {
                    violations.add("leader panel");
                }
            }
            for (int j = 0; j < i; j++) {
                if (LayoutMath.pathsCross(line.screen(), result.leaders().get(j).screen())) {
                    violations.add("leader crossing");
                }
            }
        }
        return violations;
    }
}
