package dev.vfyjxf.cloudlib.api.ui.inworld.layout;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import dev.vfyjxf.cloudlib.api.math.Rect;
import dev.vfyjxf.cloudlib.api.math.Size;
import dev.vfyjxf.cloudlib.api.ui.inworld.algorithm.AlgorithmProfile;
import dev.vfyjxf.cloudlib.api.ui.inworld.algorithm.OrbitRing;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.PlacementCandidate;
import dev.vfyjxf.cloudlib.api.ui.inworld.coordinator.WorldAabb;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.RayFan;
import dev.vfyjxf.cloudlib.api.ui.inworld.space.SpaceMask;
import org.jetbrains.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;
import java.util.Objects;
import java.util.Optional;
import java.util.Set;

/**
 * The named-strategy catalogs for the pipeline's element-side stages (§7
 * plan B): third parties register <em>named</em> strategies — they never
 * touch the pipeline itself. Names are dotted two-part ids: a stage prefix
 * and a strategy name, e.g. {@code "candidates.rayFan"} or
 * {@code "rank.weightedLinear"}. The built-in names below are the closed
 * set the profiles bind; registration of a new name extends the catalog
 * (for custom layouters and data-driven configuration to select), while
 * {@code replace} swaps a built-in implementation for a better one without
 * touching the pipeline.
 * <p>
 * Registration is global and must happen during mod init, before specs are
 * assembled. All registries are keyed lookups — iteration order never
 * participates in layout.
 */
public final class StageCatalogs {

    /** The anchored single candidate: the anchor-centered rect (face panels, waypoints). */
    public static final String candidatesSingle = "candidates.single";

    /** The ray-fan spread: free-arc candidates around the anchor (§3.0). */
    public static final String candidatesRayFan = "candidates.rayFan";

    /** The orbit-ring spread: ring slots around the anchor, inner rings first. */
    public static final String candidatesOrbitRing = "candidates.orbitRing";

    /** The dock-cursor scan: first-fit slots along the screen edge nearest the anchor. */
    public static final String candidatesDockCursor = "candidates.dockCursor";

    /** The excentric column: a vertical label column beside the focus (Y-consistent variant). */
    public static final String candidatesExcentricColumn = "candidates.excentricColumn";

    /** The passthrough filter: dodge nothing. */
    public static final String avoidNone = "avoid.none";

    /** Drop candidates intersecting registered exclusion areas. */
    public static final String avoidExclusions = "avoid.exclusions";

    /** Drop candidates intersecting exclusions and the avoided layers' occupancy. */
    public static final String avoidExclusionsAndMasks = "avoid.exclusionsAndMasks";

    /** The weighted-linear ranker: anchor distance plus overlap cost, incumbent-aware. */
    public static final String rankWeightedLinear = "rank.weightedLinear";

    /** The sticky ranker: the incumbent-matching candidate first, order otherwise kept. */
    public static final String rankIncumbentFirst = "rank.incumbentFirst";

    /**
     * Generates candidate placements for one propose round. Implementations
     * must be deterministic, side-effect free, and return at least one
     * candidate (an empty list reads as "nothing to show").
     */
    public interface CandidateStrategy {

        /**
         * @param context the anchor, variant footprint, environment and
         *        profile parameters
         * @return candidates in this strategy's canonical order (the rank
         *         stage reorders)
         */
        List<PlacementCandidate> candidates(CandidateContext context);
    }

    /**
     * Filters the candidate list down to the placements this element may
     * actually take this frame.
     */
    public interface AvoidStrategy {

        /** @return the surviving candidates, order preserved */
        List<PlacementCandidate> filter(List<PlacementCandidate> candidates, AvoidContext context);
    }

    /**
     * Orders the surviving candidates into the element's preference order —
     * strongest first; the coordinator grants the first that fits.
     */
    public interface RankStrategy {

        /** @return the ranked candidates */
        List<PlacementCandidate> rank(List<PlacementCandidate> candidates, RankContext context);
    }

    /**
     * What a candidate strategy sees.
     *
     * @param anchor the anchor's screen position (the motion reference)
     * @param variantSize the proposing variant's footprint
     * @param orientation the declared orientation mode (footprint shaping)
     * @param insetPixels the block-face inset, when the orientation is
     *        {@code blockFace}
     * @param environment the frame's screen, exclusions, occupancy and clock
     * @param params the bound profile's algorithm parameters
     * @param avoids the layers whose occupancy blocks this element's arcs
     */
    public record CandidateContext(
            FloatPos anchor,
            Size variantSize,
            OrientationFacet.Mode orientation,
            double insetPixels,
            LayoutEnvironment environment,
            AlgorithmProfile.Params params,
            Set<SpaceMask> avoids) {

        public CandidateContext {
            Objects.requireNonNull(anchor, "anchor");
            Objects.requireNonNull(variantSize, "variantSize");
            Objects.requireNonNull(orientation, "orientation");
            Objects.requireNonNull(environment, "environment");
            Objects.requireNonNull(params, "params");
            avoids = Set.copyOf(avoids);
            if (!Double.isFinite(insetPixels) || insetPixels < 0) {
                throw new IllegalArgumentException("insetPixels must be finite and non-negative: " + insetPixels);
            }
        }
    }

    /**
     * What an avoid strategy sees.
     *
     * @param avoids the layers whose occupants block this element
     * @param respectsExclusions whether exclusion areas block this element
     * @param environment the frame's exclusions and occupancy
     */
    public record AvoidContext(Set<SpaceMask> avoids, boolean respectsExclusions, LayoutEnvironment environment) {

        public AvoidContext {
            Objects.requireNonNull(avoids, "avoids");
            Objects.requireNonNull(environment, "environment");
            avoids = Set.copyOf(avoids);
        }
    }

    /**
     * What a rank strategy sees.
     *
     * @param anchor the anchor's screen position
     * @param incumbentCenter the incumbent placement's screen center, when
     *        the element holds one
     * @param sticky whether the element arbitrates sticky
     * @param params the bound profile's algorithm parameters
     */
    public record RankContext(
            FloatPos anchor, @Nullable FloatPos incumbentCenter, boolean sticky, AlgorithmProfile.Params params) {

        public RankContext {
            Objects.requireNonNull(anchor, "anchor");
            Objects.requireNonNull(params, "params");
        }
    }

    private static final Map<String, CandidateStrategy> candidateCatalog = new LinkedHashMap<>();
    private static final Map<String, AvoidStrategy> avoidCatalog = new LinkedHashMap<>();
    private static final Map<String, RankStrategy> rankCatalog = new LinkedHashMap<>();

    static {
        registerBuiltins();
    }

    private StageCatalogs() {}

    private static void registerBuiltins() {
        putCandidate(candidatesSingle, StageCatalogs::anchoredCandidates);
        putCandidate(candidatesRayFan, StageCatalogs::rayFanCandidates);
        putCandidate(candidatesOrbitRing, StageCatalogs::orbitRingCandidates);
        putCandidate(candidatesDockCursor, StageCatalogs::dockCursorCandidates);
        putCandidate(candidatesExcentricColumn, StageCatalogs::excentricColumnCandidates);
        putAvoid(avoidNone, (candidates, context) -> candidates);
        putAvoid(avoidExclusions, StageCatalogs::filterExclusions);
        putAvoid(avoidExclusionsAndMasks, StageCatalogs::filterExclusionsAndMasks);
        putRank(rankWeightedLinear, StageCatalogs::rankWeighted);
        putRank(rankIncumbentFirst, StageCatalogs::rankIncumbent);
    }

    // region registration

    /**
     * Registers a candidate strategy under {@code name}.
     *
     * @throws IllegalArgumentException if the name is malformed or taken
     */
    public static synchronized void registerCandidates(String name, CandidateStrategy strategy) {
        register(candidateCatalog, name, strategy, "candidates.");
    }

    /**
     * Registers an avoid strategy under {@code name}.
     *
     * @throws IllegalArgumentException if the name is malformed or taken
     */
    public static synchronized void registerAvoid(String name, AvoidStrategy strategy) {
        register(avoidCatalog, name, strategy, "avoid.");
    }

    /**
     * Registers a rank strategy under {@code name}.
     *
     * @throws IllegalArgumentException if the name is malformed or taken
     */
    public static synchronized void registerRank(String name, RankStrategy strategy) {
        register(rankCatalog, name, strategy, "rank.");
    }

    /**
     * Replaces the strategy registered under {@code name} — the sanctioned
     * way to swap a built-in implementation without touching the pipeline.
     *
     * @throws IllegalArgumentException if the name is unknown
     */
    public static synchronized void replaceCandidates(String name, CandidateStrategy strategy) {
        replace(candidateCatalog, name, strategy);
    }

    /** @see #replaceCandidates(String, CandidateStrategy) */
    public static synchronized void replaceAvoid(String name, AvoidStrategy strategy) {
        replace(avoidCatalog, name, strategy);
    }

    /** @see #replaceCandidates(String, CandidateStrategy) */
    public static synchronized void replaceRank(String name, RankStrategy strategy) {
        replace(rankCatalog, name, strategy);
    }

    /**
     * Removes a registered (non-built-in) strategy.
     *
     * @throws IllegalArgumentException when the name is unknown or built in
     */
    public static synchronized void unregisterCandidates(String name) {
        unregister(candidateCatalog, name);
    }

    /** @see #unregisterCandidates(String) */
    public static synchronized void unregisterAvoid(String name) {
        unregister(avoidCatalog, name);
    }

    /** @see #unregisterCandidates(String) */
    public static synchronized void unregisterRank(String name) {
        unregister(rankCatalog, name);
    }

    /** The registered candidate strategy, if any. */
    public static synchronized Optional<CandidateStrategy> candidateStrategy(String name) {
        return Optional.ofNullable(candidateCatalog.get(Objects.requireNonNull(name, "name")));
    }

    /** The registered avoid strategy, if any. */
    public static synchronized Optional<AvoidStrategy> avoidStrategy(String name) {
        return Optional.ofNullable(avoidCatalog.get(Objects.requireNonNull(name, "name")));
    }

    /** The registered rank strategy, if any. */
    public static synchronized Optional<RankStrategy> rankStrategy(String name) {
        return Optional.ofNullable(rankCatalog.get(Objects.requireNonNull(name, "name")));
    }

    /** The candidate strategy named {@code name}. */
    public static CandidateStrategy requireCandidateStrategy(String name) {
        return candidateStrategy(name).orElseThrow(() -> unknown(name, "candidate"));
    }

    /** The avoid strategy named {@code name}. */
    public static AvoidStrategy requireAvoidStrategy(String name) {
        return avoidStrategy(name).orElseThrow(() -> unknown(name, "avoid"));
    }

    /** The rank strategy named {@code name}. */
    public static RankStrategy requireRankStrategy(String name) {
        return rankStrategy(name).orElseThrow(() -> unknown(name, "rank"));
    }

    /** An immutable snapshot of the candidate catalog. */
    public static synchronized Map<String, CandidateStrategy> candidateCatalog() {
        return Map.copyOf(candidateCatalog);
    }

    /** An immutable snapshot of the avoid catalog. */
    public static synchronized Map<String, AvoidStrategy> avoidCatalog() {
        return Map.copyOf(avoidCatalog);
    }

    /** An immutable snapshot of the rank catalog. */
    public static synchronized Map<String, RankStrategy> rankCatalog() {
        return Map.copyOf(rankCatalog);
    }

    private static <S> void register(Map<String, S> catalog, String name, S strategy, String prefix) {
        Objects.requireNonNull(strategy, "strategy");
        requireWellFormed(name, prefix);
        if (catalog.containsKey(name)) {
            throw new IllegalArgumentException("strategy name already registered: " + name);
        }
        catalog.put(name, strategy);
    }

    private static <S> void replace(Map<String, S> catalog, String name, S strategy) {
        Objects.requireNonNull(strategy, "strategy");
        if (!catalog.containsKey(name)) {
            throw new IllegalArgumentException("cannot replace unknown strategy: " + name);
        }
        catalog.put(name, strategy);
    }

    private static <S> void unregister(Map<String, S> catalog, String name) {
        if (!catalog.containsKey(name)) {
            throw new IllegalArgumentException("cannot unregister unknown strategy: " + name);
        }
        if (isBuiltin(name)) {
            throw new IllegalArgumentException("built-in strategies cannot be unregistered: " + name);
        }
        catalog.remove(name);
    }

    private static boolean isBuiltin(String name) {
        return switch (name) {
            case candidatesSingle,
                    candidatesRayFan,
                    candidatesOrbitRing,
                    candidatesDockCursor,
                    candidatesExcentricColumn,
                    avoidNone,
                    avoidExclusions,
                    avoidExclusionsAndMasks,
                    rankWeightedLinear,
                    rankIncumbentFirst -> true;
            default -> false;
        };
    }

    private static void requireWellFormed(String name, String prefix) {
        Objects.requireNonNull(name, "name");
        if (!name.startsWith(prefix)) {
            throw new IllegalArgumentException("strategy name must start with '" + prefix + "': " + name);
        }
        String rest = name.substring(prefix.length());
        if (rest.isEmpty() || !Character.isLowerCase(rest.charAt(0)) || rest.indexOf('.') >= 0) {
            throw new IllegalArgumentException("strategy name must be '<stage>.<lowerCamelName>': " + name);
        }
    }

    private static IllegalArgumentException unknown(String name, String stage) {
        return new IllegalArgumentException("unknown " + stage + " strategy: " + name);
    }

    private static void putCandidate(String name, CandidateStrategy strategy) {
        candidateCatalog.put(name, strategy);
    }

    private static void putAvoid(String name, AvoidStrategy strategy) {
        avoidCatalog.put(name, strategy);
    }

    private static void putRank(String name, RankStrategy strategy) {
        rankCatalog.put(name, strategy);
    }

    // endregion

    // region built-in candidate strategies

    private static List<PlacementCandidate> anchoredCandidates(CandidateContext context) {
        FloatRect rect = footprint(context);
        return List.of(candidate(context, rect));
    }

    private static List<PlacementCandidate> rayFanCandidates(CandidateContext context) {
        double radius =
                Math.max(context.variantSize().width(), context.variantSize().height()) + 8.0;
        RayFan fan = new RayFan(context.anchor().x(), context.anchor().y(), radius);
        blockFan(context, fan, radius);
        List<Double> directions = fan.freeRays();
        int stride = Math.max(1, (directions.size() + 7) / 8);
        List<PlacementCandidate> candidates = new ArrayList<>(Math.min(8, directions.size()));
        for (int i = 0; i < directions.size() && candidates.size() < 8; i += stride) {
            double angle = directions.get(i);
            double cx = context.anchor().x() + Math.cos(angle) * radius;
            double cy = context.anchor().y() + Math.sin(angle) * radius;
            candidates.add(candidate(
                    context,
                    FloatRect.around(
                            new FloatPos(cx, cy),
                            context.variantSize().width(),
                            context.variantSize().height())));
        }
        if (candidates.isEmpty()) {
            candidates.add(candidate(context, footprint(context)));
        }
        return candidates;
    }

    private static List<PlacementCandidate> orbitRingCandidates(CandidateContext context) {
        double base =
                Math.max(context.variantSize().width(), context.variantSize().height()) * 0.5 + 10.0;
        OrbitRing ring = new OrbitRing(base, 14.0, 44.0, 4);
        double reach = base + 3 * 14.0 + 44.0;
        RayFan fan = new RayFan(context.anchor().x(), context.anchor().y(), reach);
        blockFan(context, fan, reach);
        List<OrbitRing.Slot> slots = ring.freeSlots(fan, 8);
        List<PlacementCandidate> candidates = new ArrayList<>(slots.size());
        for (OrbitRing.Slot slot : slots) {
            double cx = context.anchor().x() + slot.offsetX();
            double cy = context.anchor().y() + slot.offsetY();
            candidates.add(candidate(
                    context,
                    FloatRect.around(
                            new FloatPos(cx, cy),
                            context.variantSize().width(),
                            context.variantSize().height())));
        }
        if (candidates.isEmpty()) {
            candidates.add(candidate(context, footprint(context)));
        }
        return candidates;
    }

    private static List<PlacementCandidate> dockCursorCandidates(CandidateContext context) {
        LayoutEnvironment env = context.environment();
        double w = context.variantSize().width();
        double h = context.variantSize().height();
        double margin = context.params().dockMargin();
        double spacing = context.params().dockSpacing();
        double fx = context.anchor().x() / env.screenWidth();
        double fy = context.anchor().y() / env.screenHeight();
        double dTop = fy;
        double dBottom = 1 - fy;
        double dLeft = fx;
        double dRight = 1 - fx;
        String edge;
        double min = Math.min(Math.min(dTop, dBottom), Math.min(dLeft, dRight));
        if (dTop == min) {
            edge = "top";
        } else if (dBottom == min) {
            edge = "bottom";
        } else if (dLeft == min) {
            edge = "left";
        } else {
            edge = "right";
        }

        // The blocking intervals along the edge's scanline: every exclusion
        // and avoided occupant intersecting the edge band.
        List<double[]> intervals = new ArrayList<>();
        double scanStart;
        double scanEnd;
        switch (edge) {
            case "top" -> {
                scanStart = 0;
                scanEnd = margin + h;
            }
            case "bottom" -> {
                scanStart = env.screenHeight() - margin - h;
                scanEnd = env.screenHeight();
            }
            case "left" -> {
                scanStart = 0;
                scanEnd = margin + w;
            }
            default -> {
                scanStart = env.screenWidth() - margin - w;
                scanEnd = env.screenWidth();
            }
        }
        for (Rect exclusion : env.exclusionRects()) {
            addEdgeInterval(intervals, edge, exclusion, scanStart, scanEnd, env);
        }
        for (LayoutEnvironment.MaskedRect occupant : env.occupancy()) {
            if (context.avoids().contains(occupant.layer())) {
                addEdgeInterval(intervals, edge, occupant.rect(), scanStart, scanEnd, env);
            }
        }
        intervals.sort((a, b) -> Double.compare(a[0], b[0]));

        double extent = edge.equals("top") || edge.equals("bottom") ? env.screenWidth() : env.screenHeight();
        double cursor = margin;
        for (double[] interval : intervals) {
            if (cursor + w + spacing <= interval[0]) {
                break;
            }
            cursor = Math.max(cursor, interval[1] + spacing);
        }
        List<PlacementCandidate> candidates = new ArrayList<>(2);
        if (cursor + w <= extent - margin) {
            candidates.add(candidate(context, edgeRect(edge, cursor, margin, w, h, env)));
        }
        double tail = extent - margin - w;
        if (tail > cursor + 1.0) {
            candidates.add(candidate(context, edgeRect(edge, tail, margin, w, h, env)));
        }
        if (candidates.isEmpty()) {
            candidates.add(candidate(context, edgeRect(edge, margin, margin, w, h, env)));
        }
        return candidates;
    }

    private static void addEdgeInterval(
            List<double[]> intervals, String edge, Rect rect, double scanStart, double scanEnd, LayoutEnvironment env) {
        boolean horizontal = edge.equals("top") || edge.equals("bottom");
        double alongStart = horizontal ? rect.x() : rect.y();
        double alongEnd = horizontal ? rect.right() : rect.bottom();
        double crossStart = horizontal ? rect.y() : rect.x();
        double crossEnd = horizontal ? rect.bottom() : rect.right();
        if (crossEnd <= scanStart || crossStart >= scanEnd || alongEnd <= alongStart) {
            return;
        }
        intervals.add(new double[] {alongStart, alongEnd});
    }

    private static FloatRect edgeRect(
            String edge, double along, double margin, double w, double h, LayoutEnvironment env) {
        return switch (edge) {
            case "top" -> new FloatRect(along, margin, w, h);
            case "bottom" -> new FloatRect(along, env.screenHeight() - margin - h, w, h);
            case "left" -> new FloatRect(margin, along, w, h);
            default -> new FloatRect(env.screenWidth() - margin - w, along, w, h);
        };
    }

    private static List<PlacementCandidate> excentricColumnCandidates(CandidateContext context) {
        double w = context.variantSize().width();
        double h = context.variantSize().height();
        LayoutEnvironment env = context.environment();
        boolean rightColumn = context.anchor().x() < env.screenWidth() * 0.5;
        double gap = 8.0;
        double dx = rightColumn ? gap + w * 0.5 : -(gap + w * 0.5);
        List<PlacementCandidate> candidates = new ArrayList<>(3);
        for (int row = 0; row < 3; row++) {
            double cy = context.anchor().y() + row * (h + 4.0);
            double cx = context.anchor().x() + dx;
            candidates.add(candidate(context, FloatRect.around(new FloatPos(cx, cy), w, h)));
        }
        return candidates;
    }

    /**
     * The anchor-centered footprint: the variant rect shrunk by the declared
     * inset when the orientation is a block face.
     */
    private static FloatRect footprint(CandidateContext context) {
        double w = context.variantSize().width();
        double h = context.variantSize().height();
        if (context.orientation() == OrientationFacet.Mode.blockFace && context.insetPixels() > 0) {
            w = Math.max(1, w - 2 * context.insetPixels());
            h = Math.max(1, h - 2 * context.insetPixels());
        }
        return FloatRect.around(context.anchor(), w, h);
    }

    /**
     * The dual representation's world half: the anchor's world box when the
     * frame resolved one (the candidate's world identity — the screen rect
     * is what arbitrates); otherwise a reference-depth box around the
     * candidate's screen center, the headless/test convention.
     */
    private static PlacementCandidate candidate(CandidateContext context, FloatRect rect) {
        LayoutEnvironment env = context.environment();
        if (env.anchor() != null && env.anchor().world() != null) {
            return PlacementCandidate.dual(env.anchor().world(), rect);
        }
        return PlacementCandidate.dual(
                WorldAabb.around(rect.centerX(), 64.0, rect.centerY(), rect.width(), 8.0, rect.height()), rect);
    }

    private static void blockFan(CandidateContext context, RayFan fan, double radius) {
        LayoutEnvironment env = context.environment();
        Rect reach = new Rect(
                (int) Math.round(context.anchor().x() - radius),
                (int) Math.round(context.anchor().y() - radius),
                (int) Math.round(2 * radius),
                (int) Math.round(2 * radius));
        for (Rect exclusion : env.exclusionRects()) {
            if (exclusion.intersects(reach)) {
                fan.block(exclusion);
            }
        }
        for (LayoutEnvironment.MaskedRect occupant : env.occupancy()) {
            if (context.avoids().contains(occupant.layer()) && occupant.rect().intersects(reach)) {
                fan.block(occupant.rect());
            }
        }
    }

    // endregion

    // region built-in avoid strategies

    private static List<PlacementCandidate> filterExclusions(
            List<PlacementCandidate> candidates, AvoidContext context) {
        List<PlacementCandidate> surviving = new ArrayList<>(candidates.size());
        outer:
        for (PlacementCandidate candidate : candidates) {
            for (Rect exclusion : context.environment().exclusionRects()) {
                if (candidate.screenRect().intersects(toFloat(exclusion))) {
                    continue outer;
                }
            }
            surviving.add(candidate);
        }
        return surviving;
    }

    private static List<PlacementCandidate> filterExclusionsAndMasks(
            List<PlacementCandidate> candidates, AvoidContext context) {
        List<PlacementCandidate> surviving = new ArrayList<>(candidates.size());
        for (PlacementCandidate candidate : candidates) {
            if (!blockedByMasks(candidate, context)) {
                surviving.add(candidate);
            }
        }
        return surviving;
    }

    private static boolean blockedByMasks(PlacementCandidate candidate, AvoidContext context) {
        FloatRect rect = candidate.screenRect();
        for (Rect exclusion : context.environment().exclusionRects()) {
            if (rect.intersects(toFloat(exclusion))) {
                return true;
            }
        }
        for (LayoutEnvironment.MaskedRect occupant : context.environment().occupancy()) {
            if (context.avoids().contains(occupant.layer()) && rect.intersects(toFloat(occupant.rect()))) {
                return true;
            }
        }
        return false;
    }

    private static FloatRect toFloat(Rect rect) {
        return new FloatRect(rect.x(), rect.y(), rect.width(), rect.height());
    }

    // endregion

    // region built-in rank strategies

    private static List<PlacementCandidate> rankWeighted(List<PlacementCandidate> candidates, RankContext context) {
        List<PlacementCandidate> ranked = new ArrayList<>(candidates);
        ranked.sort((a, b) -> compareCosts(cost(a, context), cost(b, context)));
        return ranked;
    }

    /**
     * Epsilon-tolerant cost comparison: geometrically equidistant candidates
     * must stay in their canonical order (floating point must not win a
     * tie-break), and {@code List.sort} is stable.
     */
    private static int compareCosts(double a, double b) {
        if (Math.abs(a - b) <= 1.0e-9) {
            return 0;
        }
        return Double.compare(a, b);
    }

    private static double cost(PlacementCandidate candidate, RankContext context) {
        double distance = Math.hypot(
                candidate.screenRect().centerX() - context.anchor().x(),
                candidate.screenRect().centerY() - context.anchor().y());
        double cost = distance;
        if (context.sticky() && context.incumbentCenter() != null) {
            double incumbentDistance = Math.hypot(
                    candidate.screenRect().centerX() - context.incumbentCenter().x(),
                    candidate.screenRect().centerY() - context.incumbentCenter().y());
            cost += context.params().switchPenalty() * Math.min(1.0, incumbentDistance / 64.0);
        }
        return cost;
    }

    private static List<PlacementCandidate> rankIncumbent(List<PlacementCandidate> candidates, RankContext context) {
        if (!context.sticky() || context.incumbentCenter() == null) {
            return candidates;
        }
        List<PlacementCandidate> ranked = new ArrayList<>(candidates.size());
        List<PlacementCandidate> rest = new ArrayList<>(candidates.size());
        for (PlacementCandidate candidate : candidates) {
            double distance = Math.hypot(
                    candidate.screenRect().centerX() - context.incumbentCenter().x(),
                    candidate.screenRect().centerY() - context.incumbentCenter().y());
            if (distance <= 0.5) {
                ranked.add(candidate);
            } else {
                rest.add(candidate);
            }
        }
        ranked.addAll(rest);
        return ranked;
    }

    // endregion
}
