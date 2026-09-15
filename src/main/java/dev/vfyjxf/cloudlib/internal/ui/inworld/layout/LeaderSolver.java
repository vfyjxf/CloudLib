package dev.vfyjxf.cloudlib.internal.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Attachment;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Depth;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.GuiVec;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Hull;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LayoutFrame;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LeaderLine;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.LeaderMode;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Obstacle;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelPlacement;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.PanelRequest;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Projection;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Projector;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.SourceSnapshot;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.Space;
import dev.vfyjxf.cloudlib.api.ui.inworld.layout.WorldObstacles;
import net.minecraft.world.phys.AABB;
import net.minecraft.world.phys.Vec3;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.Arrays;
import java.util.Collections;
import java.util.Comparator;
import java.util.HashMap;
import java.util.HashSet;
import java.util.IdentityHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Set;

/**
 * The leader-line router: picks a source attachment and a panel port,
 * then routes a short polyline from the source gap to the port gate that
 * clears panels, exclusions and source hulls — in screen space, lifted
 * to world space for occlusion checks on world panels.
 */
public final class LeaderSolver {

    private final Map<String, String> previous = new HashMap<>();
    private TemporalDebounce debounce;
    private LayoutFrame currentFrame;

    /**
     * Per-solve caches. {@code pools} memoizes each candidate's viable
     * routes (before the assigned-leader filter) for the current
     * {@code poolPanels}/{@code poolExclusions} context — during beam
     * search the same candidate is re-queried with many different
     * assigned sets, so this collapses the expensive generation to once
     * per beam level. {@code silhouettes}/{@code sourceHulls} are
     * frame-scoped projections shared by every candidate.
     */
    private final Map<String, RoutePool> pools = new HashMap<>();

    private final Map<SourceSnapshot, List<List<GuiVec>>> sourceHullCache = new IdentityHashMap<>();
    private final Map<Obstacle, List<GuiVec>> silhouetteCache = new IdentityHashMap<>();
    /**
     * Visuals whose last solve produced no viable route — skipped until
     * the stamp says retry, so a persistently unroutable leader stops
     * paying the full generation every frame.
     */
    private final Map<String, Long> routeBackoff = new HashMap<>();

    private final Set<String> retainedTried = new HashSet<>();
    private List<PanelPlacement> poolPanels;
    private List<List<GuiVec>> poolExclusions;
    private List<List<GuiVec>> contextPads = List.of();
    private List<List<GuiVec>> panelPads = List.of();

    void bind(LayoutFrame frame, TemporalDebounce debounce) {
        this.currentFrame = frame;
        this.debounce = debounce;
        previous.clear();
        pools.clear();
        silhouetteCache.clear();
        sourceHullCache.clear();
        retainedTried.clear();
        poolPanels = null;
        poolExclusions = null;
        debounce.lines.forEach((id, line) -> {
            if (line.visible && line.route != null) previous.put(id, line.route);
        });
    }

    private static final double autoLengthFraction = .42;

    public void reset() {
        this.previous.clear();
        this.pools.clear();
        this.routeBackoff.clear();
        this.retainedTried.clear();
    }

    /** One candidate route: the rendered leader, a stability key and its cost. */
    record Option(LeaderLine leader, String key, double cost) {}

    /**
     * A candidate's unfiltered viable routes (cost-ordered) plus the
     * hidden line to report when every route is ruled out — everything
     * in a {@link #route} call that does not depend on already-assigned
     * leaders.
     */
    private record RoutePool(List<Option> options, LeaderLine fallback, boolean retained) {}

    void commit(Option option) {
        if (option.key() != null) {
            previous.put(option.leader().visualId(), option.key());
            pools.remove(option.leader().visualId());
        }
    }

    /**
     * All distinct viable routes for {@code candidate}, cost-ordered, up
     * to {@code limit} — ports first (diversity), then alternate bends on
     * already-selected ports, plus the currently-visible route when it
     * still exists.
     */
    private List<Option> rawOptions(
            LayoutFrame frame,
            PanelPlacement candidate,
            List<PanelPlacement> panels,
            List<List<GuiVec>> exclusions,
            List<LeaderLine> assigned,
            int limit) {
        List<Option> found = new ArrayList<>();
        LeaderLine fallback = route(frame, candidate, panels, exclusions, assigned, true, found);
        if (found.isEmpty()) {
            return !fallback.screen().isEmpty() || fallback.status().equals("ATTACHED")
                    ? List.of(new Option(fallback, null, length(fallback.screen())))
                    : List.of();
        }
        found.sort(Comparator.comparingDouble(Option::cost).thenComparing(Option::key));
        List<Option> selected = new ArrayList<>();
        // Keep distinct ports before filling with alternative bends on the same port.
        Set<String> ports = new HashSet<>();
        for (Option option : found) {
            if (ports.add(option.key())) selected.add(option);
            if (selected.size() == limit) break;
        }
        for (Option option : found) {
            if (selected.size() >= limit) break;
            if (selected.stream()
                    .noneMatch(
                            old -> old.leader().screen().equals(option.leader().screen()))) {
                selected.add(option);
            }
            if (selected.size() == limit) break;
        }
        TemporalDebounce.Line old = debounce == null ? null : debounce.read(candidate);
        if (old != null && old.visible) {
            found.stream()
                    .filter(o -> Objects.equals(o.key(), old.route))
                    .findFirst()
                    .ifPresent(o -> {
                        if (!selected.contains(o)) selected.add(o);
                    });
        }
        selected.sort(Comparator.comparingDouble(Option::cost).thenComparing(o -> Objects.toString(o.key(), "")));
        return List.copyOf(selected);
    }

    List<Option> options(
            LayoutFrame frame,
            PanelPlacement candidate,
            List<PanelPlacement> panels,
            List<List<GuiVec>> exclusions,
            List<LeaderLine> assigned,
            int limit) {
        List<Option> raw = rawOptions(frame, candidate, panels, exclusions, assigned, limit);
        if (debounce == null
                || raw.isEmpty()
                || debounce.maySwitch(frame, candidate, raw.get(0).key())) {
            return raw;
        }
        TemporalDebounce.Line old = debounce.read(candidate);
        List<Option> ordered = new ArrayList<>(raw);
        ordered.sort(Comparator.comparingInt(o -> Objects.equals(o.key(), old.route) ? 0 : 1));
        return ordered;
    }

    void commitRequired(
            PanelPlacement candidate,
            Option chosen,
            List<PanelPlacement> panels,
            List<List<GuiVec>> exclusions,
            List<LeaderLine> otherLines) {
        List<Option> raw = rawOptions(currentFrame, candidate, panels, exclusions, otherLines, 32);
        if (debounce != null && !chosen.leader().screen().isEmpty()) {
            debounce.commitRoute(currentFrame, candidate, chosen, raw.isEmpty() ? chosen : raw.get(0));
        }
        commit(chosen);
        // The debounce write above can flip this candidate's retained
        // route, which changes future pool generation — drop its cache.
        pools.remove(candidate.visualId());
    }

    /**
     * The AUTO-mode entry point: routed through the temporal debounce so
     * appearance and reroutes are time-gated. {@code allowed} is the
     * global leader budget check.
     */
    public LeaderLine solve(
            LayoutFrame frame,
            PanelPlacement candidate,
            List<PanelPlacement> panels,
            List<List<GuiVec>> exclusions,
            List<LeaderLine> assigned,
            boolean allowed) {
        if (debounce == null) {
            return route(frame, candidate, panels, exclusions, assigned, allowed, null);
        }
        List<Option> options = allowed ? rawOptions(frame, candidate, panels, exclusions, assigned, 32) : List.of();
        LeaderLine fallback = options.isEmpty() && allowed
                ? route(frame, candidate, panels, exclusions, assigned, true, new ArrayList<>())
                : hidden(
                        candidate,
                        candidate.members().stream().map(PanelRequest::id).toList(),
                        "BUDGET");
        if (!options.isEmpty() && options.get(0).leader().status().equals("ATTACHED")) {
            return options.get(0).leader();
        }
        LeaderLine result = debounce.finishAuto(frame, candidate, options, fallback);
        pools.remove(candidate.visualId());
        return result;
    }

    /**
     * The route search proper. When {@code options} is non-null every
     * viable route is collected instead of only the best; when
     * {@code allowed} is false the search is skipped entirely.
     * <p>
     * Shape generation ({@link #pool}) is independent of the
     * already-assigned leaders and memoized per (candidate, panels)
     * context; this method only applies the assigned-leader filter and
     * the best/hidden bookkeeping on top of the cached pool.
     */
    private LeaderLine route(
            LayoutFrame frame,
            PanelPlacement candidate,
            List<PanelPlacement> panels,
            List<List<GuiVec>> exclusions,
            List<LeaderLine> assigned,
            boolean allowed,
            @Nullable List<Option> options) {
        List<String> memberIds =
                candidate.members().stream().map(PanelRequest::id).toList();
        if (candidate.active().position().mounted()) {
            return hidden(candidate, memberIds, "ATTACHED");
        }
        if (!allowed) {
            return hidden(candidate, memberIds, "BUDGET");
        }
        RoutePool pool = pool(frame, candidate, panels, exclusions, memberIds);
        List<Option> viable = new ArrayList<>(pool.options().size());
        for (Option option : pool.options()) {
            if (!blockedByAssigned(option.leader().screen(), assigned)) viable.add(option);
        }
        if (viable.isEmpty() && pool.retained()) {
            // The retained route is now blocked (by exclusions or an
            // assigned leader) — regenerate the full pool once.
            pools.remove(candidate.visualId());
            pool = regenerate(frame, candidate, panels, memberIds);
            for (Option option : pool.options()) {
                if (!blockedByAssigned(option.leader().screen(), assigned)) viable.add(option);
            }
        }
        if (options != null) options.addAll(viable);
        if (viable.isEmpty()) {
            if (options == null && previous.remove(candidate.visualId()) != null) {
                pools.remove(candidate.visualId());
            }
            return pool.fallback();
        }
        if (options == null) {
            previous.put(candidate.visualId(), viable.get(0).key());
            pools.remove(candidate.visualId());
        }
        return viable.get(0).leader();
    }

    /** The assigned-leader filter: no crossing and no long coincident runs. */
    private static boolean blockedByAssigned(List<GuiVec> path, List<LeaderLine> assigned) {
        for (LeaderLine line : assigned) {
            if (LayoutMath.pathsCross(path, line.screen())) return true;
        }
        return crowded(path, assigned);
    }

    /**
     * The candidate's cached route pool for the current
     * (panels, exclusions) context — recomputed once per beam level.
     */
    private RoutePool pool(
            LayoutFrame frame,
            PanelPlacement candidate,
            List<PanelPlacement> panels,
            List<List<GuiVec>> exclusions,
            List<String> memberIds) {
        if (panels != poolPanels || exclusions != poolExclusions) {
            pools.clear();
            retainedTried.clear();
            poolPanels = panels;
            poolExclusions = exclusions;
            List<List<GuiVec>> pads = new ArrayList<>(exclusions.size());
            for (List<GuiVec> exclusion : exclusions) {
                pads.add(ScreenMath.offset(exclusion, 3.0));
            }
            contextPads = List.copyOf(pads);
            List<List<GuiVec>> aligned = new ArrayList<>(panels.size());
            for (PanelPlacement panel : panels) {
                aligned.add(ScreenMath.offset(panel.polygon(), 3.0));
            }
            panelPads = List.copyOf(aligned);
        }
        RoutePool pool = pools.get(candidate.visualId());
        if (pool == null) {
            Long retryAt = routeBackoff.get(candidate.visualId());
            if (retryAt != null && frame.clock().frameIndex() < retryAt) {
                pool = new RoutePool(List.of(), hidden(candidate, memberIds, "NO_VISIBLE_SHORT_ROUTE"), false);
                pools.put(candidate.visualId(), pool);
            } else {
                pool = retainedTried.add(candidate.visualId())
                        ? retainedPool(frame, candidate, panels, memberIds)
                        : null;
                if (pool == null) pool = regenerate(frame, candidate, panels, memberIds);
                else pools.put(candidate.visualId(), pool);
            }
        }
        return pool;
    }

    /**
     * Full route generation for {@code candidate} with the failure
     * backoff bookkeeping — used for cold pools and for the final tier
     * after a retained route collapses.
     */
    private RoutePool regenerate(
            LayoutFrame frame, PanelPlacement candidate, List<PanelPlacement> panels, List<String> memberIds) {
        RoutePool pool = generate(frame, candidate, panels, memberIds);
        if (pool.options().isEmpty()) {
            routeBackoff.put(candidate.visualId(), frame.clock().frameIndex() + 15);
        } else {
            routeBackoff.remove(candidate.visualId());
        }
        pools.put(candidate.visualId(), pool);
        return pool;
    }

    /**
     * Every viable route shape for {@code candidate}, cost-ordered —
     * independent of already-assigned leaders. The returned fallback is
     * the hidden line describing why nothing was routable.
     */
    /**
     * The route-generation context shared by every (anchor, port) pair
     * of a candidate: padded exclusions, the source's screen hulls, the
     * gate-level exclusion base, the sorted attach ports and the style.
     */
    private record RouteContext(
            List<List<GuiVec>> paddedExclusions,
            List<List<GuiVec>> sourceHulls,
            List<List<GuiVec>> gateBase,
            List<PanelPort> ports,
            LeaderStyle style) {}

    private RouteContext routeContext(LayoutFrame frame, PanelPlacement candidate, List<PanelPlacement> panels) {
        SourceSnapshot source = candidate.source();
        // Exclusions padded for the lead segment; panels padded separately below.
        List<List<GuiVec>> paddedExclusions = new ArrayList<>(contextPads);
        for (int i = 0; i < panels.size(); i++) {
            if (panels.get(i) != candidate) {
                paddedExclusions.add(panelPads.get(i));
            }
        }

        // The source's own silhouette — the lead must exit it, never cut through.
        List<List<GuiVec>> sourceHulls = sourceHulls(frame, source);

        // Port-invariant gate exclusions: padded panels + the candidate's
        // own rect + the source hulls — silhouette dodges append per port.
        List<List<GuiVec>> gateBase = new ArrayList<>(paddedExclusions);
        gateBase.add(ScreenMath.offset(candidate.polygon(), 2.0));
        for (List<GuiVec> hull : sourceHulls) {
            gateBase.add(ScreenMath.offset(hull, 2.0));
        }

        LeaderTarget target = candidate.space() == Space.world
                ? new WorldRect(
                        candidate.pose().origin(),
                        candidate.pose().basis().right(),
                        candidate.pose().basis().up(),
                        candidate.worldWidth(),
                        candidate.worldHeight())
                : new HudRect(
                        candidate.screenRect().x(),
                        candidate.screenRect().y(),
                        candidate.screenRect().width(),
                        candidate.screenRect().height());
        LeaderStyle style = LeaderStyle.routing();
        List<PanelPort> ports = new ArrayList<>(target.ports(frame.camera(), style));
        GuiVec preferred = ScreenSlots.preferredPoint(frame, source.frame());
        ports.sort(Comparator.comparingDouble(port -> port.point().distance(preferred) + (port.slot() == 1 ? 0 : 12)));
        return new RouteContext(paddedExclusions, sourceHulls, gateBase, ports, style);
    }

    /** Does the anchor project on-screen, face the camera and stay visible? */
    private static boolean anchorViable(LayoutFrame frame, Attachment anchor, Projection anchorProjection) {
        return anchorProjection.valid()
                && ScreenMath.inViewport(anchorProjection.point(), frame.camera(), 2.0)
                && (anchor.normal() == null
                        || !(anchor.normal().dot(frame.camera().eye().subtract(anchor.point())) <= 0.0))
                && frame.world().visible(frame.camera().eye(), anchor.point(), Set.of());
    }

    /**
     * Fast path for dynamic frames: revalidate only the (anchor, port)
     * pair of the route committed last solve — the routeKey is
     * {@code generation:anchorKey:portKey:topology}, so the pair is
     * recovered by prefix match. Returns null when the retained pair no
     * longer exists or produces no viable route, so the caller falls
     * back to a full {@link #generate}.
     */
    private @Nullable RoutePool retainedPool(
            LayoutFrame frame, PanelPlacement candidate, List<PanelPlacement> panels, List<String> memberIds) {
        String wanted = previous.get(candidate.visualId());
        if (wanted == null) return null;
        SourceSnapshot source = candidate.source();
        Attachment anchor = null;
        String rest = null;
        for (Attachment attachment : source.attachments()) {
            String prefix = source.generation() + ":" + attachment.key() + ":";
            if (wanted.startsWith(prefix)
                    && (anchor == null
                            || attachment.key().length() > anchor.key().length())) {
                anchor = attachment;
                rest = wanted.substring(prefix.length());
            }
        }
        if (anchor == null) return null;
        RouteContext context = routeContext(frame, candidate, panels);
        PanelPort port = null;
        for (PanelPort p : context.ports()) {
            if (rest.startsWith(p.key() + ":")
                    && (port == null || p.key().length() > port.key().length())) {
                port = p;
            }
        }
        if (port == null) return null;
        Projection anchorProjection = frame.camera().project(anchor.point());
        if (!anchorViable(frame, anchor, anchorProjection)) return null;
        List<Option> found = new ArrayList<>();
        optionsForPair(frame, candidate, anchor, anchorProjection, port, context, memberIds, found);
        if (found.isEmpty()) return null;
        found.sort(Comparator.comparingDouble(Option::cost).thenComparing(Option::key));
        return new RoutePool(List.copyOf(found), hidden(candidate, memberIds, "NO_VISIBLE_SHORT_ROUTE"), true);
    }

    private RoutePool generate(
            LayoutFrame frame, PanelPlacement candidate, List<PanelPlacement> panels, List<String> memberIds) {
        SourceSnapshot source = candidate.source();
        TemporalDebounce.Line history = debounce == null ? null : debounce.read(candidate);
        boolean retainingRoute = history != null && history.visible;
        Projection owner = frame.camera().project(source.frame().origin());
        boolean anchorInView = false;
        for (Attachment attachment : source.attachments()) {
            Projection projection = frame.camera().project(attachment.point());
            if (projection.valid() && ScreenMath.inViewport(projection.point(), frame.camera(), 2)) {
                anchorInView = true;
                break;
            }
        }
        if (!anchorInView) {
            String status = owner.valid()
                    ? (ScreenMath.inViewport(owner.point(), frame.camera(), 2)
                            ? "NO_VISIBLE_ANCHOR"
                            : "EDGE_PROXY_OFFSCREEN")
                    : (source.frame()
                                            .origin()
                                            .subtract(frame.camera().eye())
                                            .dot(frame.viewBasis().normal())
                                    >= 0
                            ? "EDGE_PROXY_BEHIND"
                            : "EDGE_PROXY_CLIPPED");
            return new RoutePool(List.of(), hidden(candidate, memberIds, status), false);
        }

        RouteContext context = routeContext(frame, candidate, panels);
        List<PanelPort> ports = context.ports();
        if (ports.size() > 8 && !retainingRoute) {
            ports = ports.subList(0, 8);
        }

        List<Attachment> attachments = new ArrayList<>(source.attachments());
        attachments.sort(Comparator.comparingDouble(attachment -> {
            Projection projection = frame.camera().project(attachment.point());
            return projection.valid()
                    ? projection.point().distance(candidate.center()) + attachment.preference()
                    : 1.0E10;
        }));

        List<Option> found = new ArrayList<>();
        int anchorsTried = 0;

        for (Attachment anchor : attachments) {
            Projection anchorProjection = frame.camera().project(anchor.point());
            if (!anchorViable(frame, anchor, anchorProjection)) {
                continue;
            }
            if (++anchorsTried > 10 && !retainingRoute) {
                break;
            }
            for (PanelPort port : ports) {
                optionsForPair(frame, candidate, anchor, anchorProjection, port, context, memberIds, found);
            }
        }

        found.sort(Comparator.comparingDouble(Option::cost).thenComparing(Option::key));
        return new RoutePool(List.copyOf(found), hidden(candidate, memberIds, "NO_VISIBLE_SHORT_ROUTE"), false);
    }

    /**
     * Every viable route for one (anchor, port) pair — lead direction,
     * elbow shapes, silhouette dodges, detours and the final filters.
     * Appends viable options to {@code found}.
     */
    private void optionsForPair(
            LayoutFrame frame,
            PanelPlacement candidate,
            Attachment anchor,
            Projection anchorProjection,
            PanelPort port,
            RouteContext context,
            List<String> memberIds,
            List<Option> found) {
        SourceSnapshot source = candidate.source();
        LeaderStyle style = context.style();
        GuiVec anchorPoint = anchorProjection.point();
        GuiVec leadDir = port.gate().sub(anchorPoint).unit();
        if (leadDir.lenSqr() < 0.25) return;

        if (anchor.normal() != null) {
            Projection normalProjection =
                    frame.camera().project(anchor.point().add(anchor.normal().scale(0.1)));
            if (normalProjection.valid()) {
                GuiVec normalDir = normalProjection.point().sub(anchorPoint);
                if (normalDir.lenSqr() > 1.0 && normalDir.unit().dot(leadDir) > 0.35) {
                    leadDir = normalDir.unit();
                }
            }
        }
        if (anchor.normal() == null) {
            leadDir = Math.abs(leadDir.x()) >= Math.abs(leadDir.y())
                    ? new GuiVec(Math.signum(leadDir.x()), 0.0)
                    : new GuiVec(0.0, Math.signum(leadDir.y()));
        }

        double sourceGap = style.sourceGap();
        for (List<GuiVec> hull : context.sourceHulls()) {
            if (ScreenMath.inside(anchorPoint, hull)) {
                sourceGap = Math.max(sourceGap, rayExit(anchorPoint, leadDir, hull) + 8.0);
            }
        }
        if (sourceGap > 140.0) return;

        GuiVec leadEnd = anchorPoint.add(leadDir.mul(sourceGap));
        if (!ScreenMath.inViewport(leadEnd, frame.camera(), 2.0)) return;

        // Candidate elbow shapes from the lead end to the port gate.
        List<List<GuiVec>> shapes = new ArrayList<>();
        shapes.add(List.of(leadEnd, port.gate()));
        shapes.add(List.of(leadEnd, new GuiVec(port.gate().x(), leadEnd.y()), port.gate()));
        shapes.add(List.of(leadEnd, new GuiVec(leadEnd.x(), port.gate().y()), port.gate()));
        GuiVec delta = port.gate().sub(leadEnd);
        double diag = Math.min(Math.abs(delta.x()), Math.abs(delta.y()));
        shapes.add(List.of(
                leadEnd,
                leadEnd.add(new GuiVec(Math.signum(delta.x()) * diag, Math.signum(delta.y()) * diag)),
                port.gate()));

        List<List<GuiVec>> gateExclusions = context.gateBase();

        // Physical world panels also dodge the screen silhouette of
        // solid world obstacles the lifted route would pierce.
        if (candidate.space() == Space.world && candidate.active().material().requiresFreeWorldSpace()) {
            List<Vec3> lifted = lift(
                    frame.camera(),
                    List.of(anchorPoint, leadEnd, port.gate(), port.point()),
                    anchorProjection.depth(),
                    port,
                    anchor.point());
            AABB liftBox = null;
            for (Vec3 p : lifted) {
                liftBox = liftBox == null ? new AABB(p, p) : liftBox.minmax(new AABB(p, p));
            }
            List<List<GuiVec>> dodged = null;
            WorldObstacles.Index index = frame.world().index();
            for (int k = 0, end = index.end(index.solid, liftBox.maxX); k < end; k++) {
                int j = index.solid[k];
                Obstacle obstacle = index.obstacles[j];
                if (source.obstacleIds().contains(obstacle.id()) || !liftBox.intersects(index.bounds[j])) {
                    continue;
                }
                List<GuiVec> silhouette = silhouette(frame, obstacle);
                if (silhouette.isEmpty()) {
                    continue;
                }
                boolean pierces = false;
                for (int i = 1; i < lifted.size(); i++) {
                    if (obstacle.shape().segment(lifted.get(i - 1), lifted.get(i), 0.012)) {
                        pierces = true;
                    }
                }
                if (pierces && !ScreenMath.inside(leadEnd, silhouette) && !ScreenMath.inside(port.gate(), silhouette)) {
                    if (dodged == null) dodged = new ArrayList<>(context.gateBase());
                    dodged.add(ScreenMath.offset(silhouette, 4.0));
                }
            }
            if (dodged != null) gateExclusions = dodged;
        }

        // A REQUIRED leader may detour around blockers through the
        // visibility graph when every direct shape is obstructed.
        if (candidate.active().leaderMode() == LeaderMode.required
                && !clear(List.of(leadEnd, port.gate()), gateExclusions)) {
            List<GuiVec> detour = visibilityPath(leadEnd, port.gate(), gateExclusions, frame.camera());
            if (!detour.isEmpty()) {
                shapes.add(detour);
            }
        }

        for (List<GuiVec> shape : shapes) {
            if (!clear(shape, gateExclusions)) continue;
            List<GuiVec> path = new ArrayList<>();
            path.add(anchorPoint);
            path.addAll(shape);
            path.add(port.point());
            List<GuiVec> compacted = compactPath(path);
            if (!(compacted.size() >= 3
                    && compacted.size() <= 7
                    && clear(compacted, context.paddedExclusions())
                    && !LayoutMath.pathEnters(compacted, candidate.polygon()))) {
                continue;
            }

            double routeLength = length(compacted);
            boolean auto = candidate.active().leaderMode() == LeaderMode.auto;
            double limit = auto
                    ? Math.min(
                            420, Math.min(frame.camera().width(), frame.camera().height()) * autoLengthFraction)
                    : Math.min(700, anchorPoint.distance(port.point()) * 1.6 + 64);
            if (!(routeLength <= limit && bends(compacted) <= (auto ? 2 : 4))) continue;

            boolean allInView = true;
            for (GuiVec point : compacted) {
                if (!ScreenMath.inViewport(point, frame.camera(), 2.0)) {
                    allInView = false;
                }
            }
            if (!allInView) continue;

            List<Vec3> world = candidate.space() == Space.world
                    ? lift(frame.camera(), compacted, anchorProjection.depth(), port, anchor.point())
                    : screenPath(frame.camera(), compacted, anchorProjection.depth(), anchor.point());
            if (!LeaderVisibility.clear(frame, candidate, compacted, world)) continue;

            String routeKey = source.generation() + ":" + anchor.key() + ":" + port.key() + ":" + topology(compacted);
            double cost = routeLength
                    + bends(compacted) * 42
                    + (port.slot() == 1 ? 0 : 12)
                    + anchor.preference()
                    + (routeKey.equals(this.previous.get(candidate.visualId())) ? 0 : 24);
            found.add(new Option(
                    new LeaderLine(
                            candidate.visualId(), source.id(), memberIds, compacted, world, "VISIBLE", Depth.test),
                    routeKey,
                    cost));
        }
    }

    /** The source's screen silhouettes, cached per frame. */
    private List<List<GuiVec>> sourceHulls(LayoutFrame frame, SourceSnapshot source) {
        List<List<GuiVec>> cached = sourceHullCache.get(source);
        if (cached != null) return cached;
        List<List<GuiVec>> hulls = new ArrayList<>();
        for (Hull hull : source.hulls()) {
            List<GuiVec> points = ScreenMath.projectAll(frame.camera(), hull.vertices());
            if (points.size() >= 3) {
                points = ScreenMath.hull(points);
                if (points.size() >= 3 && Math.abs(ScreenMath.area(points)) > 2.0) {
                    hulls.add(points);
                }
            }
        }
        List<List<GuiVec>> result = List.copyOf(hulls);
        sourceHullCache.put(source, result);
        return result;
    }

    /** A solid obstacle's screen silhouette, or empty when it has none — cached per frame. */
    private List<GuiVec> silhouette(LayoutFrame frame, Obstacle obstacle) {
        List<GuiVec> cached = silhouetteCache.get(obstacle);
        if (cached != null) return cached;
        List<GuiVec> result = List.of();
        List<GuiVec> points =
                ScreenMath.projectAll(frame.camera(), obstacle.shape().previewVertices());
        if (points.size() >= 3) {
            List<GuiVec> silhouette = ScreenMath.hull(points);
            if (silhouette.size() >= 3 && !(Math.abs(ScreenMath.area(silhouette)) < 3.0)) {
                result = silhouette;
            }
        }
        silhouetteCache.put(obstacle, result);
        return result;
    }

    /** A hidden leader — status records why it isn't drawn. */
    static LeaderLine hidden(PanelPlacement candidate, List<String> memberIds, String status) {
        return new LeaderLine(
                candidate.visualId(), candidate.source().id(), memberIds, List.of(), List.of(), status, Depth.test);
    }

    /** Interior direction changes along a path (including reversals). */
    static int bends(List<GuiVec> path) {
        int count = 0;
        for (int i = 1; i + 1 < path.size(); i++) {
            GuiVec a = path.get(i).sub(path.get(i - 1));
            GuiVec b = path.get(i + 1).sub(path.get(i));
            if (Math.abs(a.cross(b)) > 1e-5 * Math.max(1, a.len() * b.len()) || a.dot(b) < 0) {
                count++;
            }
        }
        return count;
    }

    /** The octant sequence of a path — part of the route stability key. */
    static String topology(List<GuiVec> path) {
        StringBuilder result = new StringBuilder();
        for (int i = 1; i < path.size(); i++) {
            GuiVec d = path.get(i).sub(path.get(i - 1));
            result.append(Math.floorMod((int) Math.rint(Math.atan2(d.y(), d.x()) / (Math.PI / 4)), 8))
                    .append(',');
        }
        return result.toString();
    }

    /** Unprojects a screen path at a constant depth, keeping the real anchor as the first point. */
    private static List<Vec3> screenPath(Projector camera, List<GuiVec> path, double depth, Vec3 anchor) {
        List<Vec3> world = new ArrayList<>(path.size());
        for (GuiVec point : path) world.add(camera.unproject(point, depth));
        world.set(0, anchor);
        return List.copyOf(world);
    }

    /**
     * Is {@code path} too close to an already-assigned leader? Sharing a
     * source's first segment is fine; a long coincident run or a crossing
     * is not.
     */
    static boolean crowded(List<GuiVec> path, List<LeaderLine> assigned) {
        for (LeaderLine leader : assigned) {
            if (crowdedBy(path, leader)) return true;
        }
        return false;
    }

    /** {@code path} vs one assigned leader — see {@link #crowded}. */
    static boolean crowdedBy(List<GuiVec> path, LeaderLine leader) {
        for (int i = 1; i < path.size(); i++) {
            for (int j = 1; j < leader.screen().size(); j++) {
                GuiVec a = path.get(i - 1);
                GuiVec b = path.get(i);
                GuiVec c = leader.screen().get(j - 1);
                GuiVec d = leader.screen().get(j);
                // One shared source may branch, but not hide a long coincident segment.
                if (i == 1 && j == 1 && a.distance(c) < 1) continue;
                if (LayoutMath.segmentCross(a, b, c, d)
                        || Math.min(
                                        Math.min(pointSegment(a, c, d), pointSegment(b, c, d)),
                                        Math.min(pointSegment(c, a, b), pointSegment(d, a, b)))
                                < 4) {
                    return true;
                }
            }
        }
        return false;
    }

    private static double pointSegment(GuiVec p, GuiVec a, GuiVec b) {
        double dx = b.x() - a.x();
        double dy = b.y() - a.y();
        double length = dx * dx + dy * dy;
        double t =
                length < 1e-12 ? 0 : Math.max(0, Math.min(1, ((p.x() - a.x()) * dx + (p.y() - a.y()) * dy) / length));
        return Math.hypot(p.x() - a.x() - t * dx, p.y() - a.y() - t * dy);
    }

    static double length(List<GuiVec> path) {
        double total = 0.0;
        for (int i = 1; i < path.size(); i++) {
            total += path.get(i).distance(path.get(i - 1));
        }
        return total;
    }

    /** Drops consecutive points closer than 0.1px — collinear kinks from shape construction. */
    static List<GuiVec> compactPath(List<GuiVec> path) {
        List<GuiVec> result = new ArrayList<>();
        for (GuiVec point : path) {
            if (result.isEmpty() || point.distance(result.get(result.size() - 1)) > 0.1) {
                result.add(point);
            }
        }
        return result;
    }

    /** Does {@code path} stay outside every convex exclusion polygon? */
    static boolean clear(List<GuiVec> path, List<List<GuiVec>> exclusions) {
        for (List<GuiVec> exclusion : exclusions) {
            if (LayoutMath.pathEnters(path, exclusion)) {
                return false;
            }
        }
        return true;
    }

    /** Distance from {@code point} along {@code direction} to the polygon's exit, 0 when outside. */
    static double rayExit(GuiVec point, GuiVec direction, List<GuiVec> polygon) {
        double exit = 0.0;
        for (int i = 0; i < polygon.size(); i++) {
            GuiVec vertex = polygon.get(i);
            GuiVec edge = polygon.get((i + 1) % polygon.size()).sub(vertex);
            double denominator = direction.cross(edge);
            if (!(Math.abs(denominator) < 1.0E-8)) {
                double along = vertex.sub(point).cross(edge) / denominator;
                double across = vertex.sub(point).cross(direction) / denominator;
                if (along >= 0.0 && across >= 0.0 && across <= 1.0) {
                    exit = Math.max(exit, along);
                }
            }
        }
        return exit;
    }

    /**
     * Lifts a screen path to world space: depths interpolate linearly in
     * 1/z between the anchor depth and the port depth, then the two last
     * points snap to the port's world gate and attach point.
     */
    static List<Vec3> lift(Projector camera, List<GuiVec> path, double depth, PanelPort port, Vec3 anchor) {
        List<Vec3> world = new ArrayList<>();
        double total = length(path);
        double travelled = 0.0;
        for (int i = 0; i < path.size(); i++) {
            if (i > 0) {
                travelled += path.get(i).distance(path.get(i - 1));
            }
            double t = travelled / Math.max(1.0E-6, total);
            double interpolated = 1.0 / ((1.0 - t) / depth + t / port.depth());
            world.add(camera.unproject(path.get(i), interpolated));
        }
        world.set(0, anchor);
        world.set(world.size() - 1, port.worldPoint());
        world.set(world.size() - 2, port.worldGate());
        return world;
    }

    /**
     * Dijkstra detour through the vertices of the (already padded)
     * exclusion polygons — used only for REQUIRED leaders whose direct
     * shapes all collide.
     */
    static List<GuiVec> visibilityPath(GuiVec from, GuiVec to, List<List<GuiVec>> exclusions, Projector camera) {
        List<GuiVec> nodes = new ArrayList<>(List.of(from, to));
        for (List<GuiVec> exclusion : exclusions) {
            for (GuiVec vertex : ScreenMath.offset(exclusion, 3.0)) {
                if (ScreenMath.inViewport(vertex, camera, 3.0)
                        && vertex.distance(from) + vertex.distance(to) < from.distance(to) * 1.8 + 100.0) {
                    nodes.add(vertex);
                }
            }
        }
        if (nodes.size() > 100) {
            return List.of();
        }

        int count = nodes.size();
        double[] distance = new double[count];
        int[] parent = new int[count];
        boolean[] visited = new boolean[count];
        Arrays.fill(distance, Double.POSITIVE_INFINITY);
        Arrays.fill(parent, -1);
        distance[0] = 0.0;

        for (int i = 0; i < count; i++) {
            int next = -1;
            for (int j = 0; j < count; j++) {
                if (!visited[j] && (next < 0 || distance[j] < distance[next])) {
                    next = j;
                }
            }
            if (next < 0 || !Double.isFinite(distance[next]) || next == 1) {
                break;
            }
            visited[next] = true;
            for (int j = 0; j < count; j++) {
                if (!visited[j] && j != next && clear(List.of(nodes.get(next), nodes.get(j)), exclusions)) {
                    double candidate = distance[next] + nodes.get(next).distance(nodes.get(j)) + 20.0;
                    if (candidate < distance[j]) {
                        distance[j] = candidate;
                        parent[j] = next;
                    }
                }
            }
        }

        if (!Double.isFinite(distance[1])) {
            return List.of();
        }
        List<GuiVec> path = new ArrayList<>();
        for (int at = 1; at != -1; at = parent[at]) {
            path.add(nodes.get(at));
        }
        Collections.reverse(path);
        return path;
    }
}
