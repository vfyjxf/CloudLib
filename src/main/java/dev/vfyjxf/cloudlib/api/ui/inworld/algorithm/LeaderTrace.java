package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import com.google.gson.JsonArray;
import com.google.gson.JsonElement;
import com.google.gson.JsonObject;
import com.google.gson.JsonParseException;
import com.google.gson.JsonParser;
import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import org.jspecify.annotations.Nullable;

import java.util.ArrayList;
import java.util.LinkedHashMap;
import java.util.List;
import java.util.Map;

/**
 * The leader-routing trace format: the bridge between an in-game recording
 * and a deterministic test. One JSON object per line, one line per routing
 * epoch — the line carries everything one {@link LeaderRouter#route} call
 * needs (the {@link Frame}: leaders, clusters, obstacles, viewport) plus,
 * when it is a recording rather than a hand-written input, the epoch's
 * {@link Route}s under {@code routes}. A recording is therefore already a
 * replay input: {@link #decodeFrame} reads the leader side and ignores the
 * routes, so the exact sequence the game routed can be re-routed offline
 * through {@link #replay} and pinned with assertions — the "freeze an
 * in-game report into a test" path.
 * <p>
 * Schema (all coordinates in gui px, arrays are {@code [x, y]} /
 * {@code [x, y, w, h]}):
 * <pre>{@code
 * {"frame": 12,
 *  "leaders": [{"id": "panel/1", "anchor": [x, y],
 *               "port": {"point": [x, y], "face": "left", "normal": [-1, 0]},
 *               "panel": [x, y, w, h] | null}],
 *  "clusters": {"id": "cluster"},
 *  "obstacles": [[x, y, w, h], ...],
 *  "viewport": [x, y, w, h] | null,
 *  "routes": [{"id": "panel/1", "style": "poLeader", "tier": "full",
 *              "alpha": 1.0, "crossings": 0, "shapeEpoch": 3,
 *              "trigger": 1, "decision": "stretched",
 *              "points": [[x, y], ...]}]}
 * }</pre>
 * Pure and static: same line in, same frame out; no wall clock, no GL.
 */
public final class LeaderTrace {

    private LeaderTrace() {}

    /**
     * One routing epoch's input: everything {@link LeaderRouter#route}
     * consumes. {@code index} is the recording's frame counter — replay
     * ignores it, recordings keep their order even through lines dropped on
     * the floor.
     */
    public record Frame(
        long index,
        List<LeaderRouter.Leader> leaders,
        Map<String, String> clusters,
        List<FloatRect> obstacles,
        @Nullable FloatRect viewport
    ) {

        public Frame {
            leaders = List.copyOf(leaders);
            clusters = Map.copyOf(clusters);
            obstacles = List.copyOf(obstacles);
        }
    }

    /**
     * One JSONL line for {@code frame}, optionally carrying the epoch's
     * {@code routes} — a recording. Deterministic field order, one line, no
     * indentation: the format is diffable and greppable.
     */
    public static String encode(Frame frame, @Nullable List<LeaderRouter.Route> routes) {
        JsonObject root = new JsonObject();
        root.addProperty("frame", frame.index());
        JsonArray leaders = new JsonArray();
        for (LeaderRouter.Leader leader : frame.leaders()) {
            JsonObject l = new JsonObject();
            l.addProperty("id", leader.id());
            l.add("anchor", pos(leader.anchorX(), leader.anchorY()));
            JsonObject port = new JsonObject();
            port.add("point", pos(leader.port().point().x(), leader.port().point().y()));
            port.addProperty("face", leader.port().face().name());
            port.add("normal", pos(leader.port().normalX(), leader.port().normalY()));
            l.add("port", port);
            l.add("panel", leader.panel() == null ? null : rect(leader.panel()));
            leaders.add(l);
        }
        root.add("leaders", leaders);
        JsonObject clusters = new JsonObject();
        frame.clusters().forEach(clusters::addProperty);
        root.add("clusters", clusters);
        JsonArray obstacles = new JsonArray();
        for (FloatRect obstacle : frame.obstacles()) {
            obstacles.add(rect(obstacle));
        }
        root.add("obstacles", obstacles);
        root.add("viewport", frame.viewport() == null ? null : rect(frame.viewport()));
        if (routes != null) {
            JsonArray out = new JsonArray();
            for (LeaderRouter.Route route : routes) {
                JsonObject r = new JsonObject();
                r.addProperty("id", route.id());
                r.addProperty("style", route.style().name());
                r.addProperty("tier", route.tier().name());
                r.addProperty("alpha", route.alpha());
                r.addProperty("crossings", route.crossings());
                r.addProperty("shapeEpoch", route.shapeEpoch());
                r.addProperty("trigger", route.telemetry().trigger());
                r.addProperty("decision", route.telemetry().decision().name());
                JsonArray points = new JsonArray();
                for (FloatPos point : route.points()) {
                    points.add(pos(point.x(), point.y()));
                }
                r.add("points", points);
                out.add(r);
            }
            root.add("routes", out);
        }
        return root.toString();
    }

    /** The line's {@link Frame} — the {@code routes} member, if present, is ignored. */
    public static Frame decodeFrame(String line) {
        JsonObject root = parse(line);
        JsonArray leadersJson = requireArray(root, "leaders", line);
        List<LeaderRouter.Leader> leaders = new ArrayList<>(leadersJson.size());
        for (JsonElement element : leadersJson) {
            JsonObject l = element.getAsJsonObject();
            JsonObject port = requireObject(l, "port", line);
            FloatPos anchor = readPos(requireArray(l, "anchor", line), line);
            FloatPos point = readPos(requireArray(port, "point", line), line);
            AttachPointResolver.Face face = AttachPointResolver.Face.valueOf(requireString(port, "face", line));
            FloatPos normal = readPos(requireArray(port, "normal", line), line);
            JsonElement panelJson = l.get("panel");
            FloatRect panel = panelJson == null || panelJson.isJsonNull()
                    ? null
                    : readRect(panelJson.getAsJsonArray(), line);
            leaders.add(
                new LeaderRouter.Leader(
                    requireString(l, "id", line),
                    anchor.x(),
                    anchor.y(),
                    new AttachPointResolver.Port(face, point, normal.x(), normal.y()),
                    panel
                )
            );
        }
        Map<String, String> clusters = new LinkedHashMap<>();
        JsonElement clustersJson = root.get("clusters");
        if (clustersJson != null && clustersJson.isJsonObject()) {
            for (Map.Entry<String, JsonElement> entry : clustersJson.getAsJsonObject().entrySet()) {
                clusters.put(entry.getKey(), entry.getValue().getAsString());
            }
        }
        List<FloatRect> obstacles = new ArrayList<>();
        for (JsonElement element : optArray(root, "obstacles")) {
            obstacles.add(readRect(element.getAsJsonArray(), line));
        }
        JsonElement viewportJson = root.get("viewport");
        FloatRect viewport = viewportJson == null || viewportJson.isJsonNull()
                ? null
                : readRect(viewportJson.getAsJsonArray(), line);
        long index = root.has("frame") ? root.get("frame").getAsLong() : 0;
        return new Frame(index, leaders, clusters, obstacles, viewport);
    }

    /** The line's recorded routes, in the recorded order — the replay's expected side. */
    public static List<LeaderRouter.Route> decodeRoutes(String line) {
        JsonElement routesJson = parse(line).get("routes");
        if (routesJson == null || !routesJson.isJsonArray()) {
            return List.of();
        }
        List<LeaderRouter.Route> routes = new ArrayList<>(routesJson.getAsJsonArray().size());
        for (JsonElement element : routesJson.getAsJsonArray()) {
            JsonObject r = element.getAsJsonObject();
            List<FloatPos> points = new ArrayList<>();
            for (JsonElement point : optArray(r, "points")) {
                points.add(readPos(point.getAsJsonArray(), line));
            }
            routes.add(
                new LeaderRouter.Route(
                    requireString(r, "id", line),
                    LeaderRouter.Style.valueOf(requireString(r, "style", line)),
                    points,
                    null,
                    r.has("crossings") ? r.get("crossings").getAsInt() : 0,
                    r.has("shapeEpoch") ? r.get("shapeEpoch").getAsLong() : 0L,
                    LeaderRouter.Tier
                            .valueOf(r.has("tier") ? r.get("tier").getAsString() : LeaderRouter.Tier.full.name()),
                    r.has("alpha") ? r.get("alpha").getAsDouble() : 1.0,
                    new LeaderRouter.Telemetry(
                        r.has("trigger") ? r.get("trigger").getAsInt() : 0,
                        LeaderRouter.Decision.valueOf(
                            r.has("decision") ? r.get("decision").getAsString() : LeaderRouter.Decision.unchanged.name()
                        )
                    )
                )
            );
        }
        return routes;
    }

    /**
     * The deterministic replay: one {@link LeaderRouter}, one {@code route}
     * epoch per frame, in order — the same call sequence the game made, so
     * the same hysteresis state evolves and the same decisions come out.
     */
    public static List<List<LeaderRouter.Route>> replay(List<Frame> frames, LeaderRouter.Config config) {
        LeaderRouter router = new LeaderRouter(config);
        List<List<LeaderRouter.Route>> epochs = new ArrayList<>(frames.size());
        for (Frame frame : frames) {
            epochs.add(router.route(frame.leaders(), frame.clusters(), frame.obstacles(), frame.viewport()));
        }
        return epochs;
    }

    private static JsonObject parse(String line) {
        JsonElement root;
        try {
            root = JsonParser.parseString(line);
        } catch (JsonParseException e) {
            throw new IllegalArgumentException("trace line is not JSON: " + line, e);
        }
        if (!root.isJsonObject()) {
            throw new IllegalArgumentException("trace line is not a JSON object: " + line);
        }
        return root.getAsJsonObject();
    }

    private static JsonArray requireArray(JsonObject root, String member, String line) {
        JsonElement element = root.get(member);
        if (element == null || !element.isJsonArray()) {
            throw new IllegalArgumentException("trace line missing array '" + member + "': " + line);
        }
        return element.getAsJsonArray();
    }

    private static JsonObject requireObject(JsonObject root, String member, String line) {
        JsonElement element = root.get(member);
        if (element == null || !element.isJsonObject()) {
            throw new IllegalArgumentException("trace line missing object '" + member + "': " + line);
        }
        return element.getAsJsonObject();
    }

    private static String requireString(JsonObject root, String member, String line) {
        JsonElement element = root.get(member);
        if (element == null || !element.isJsonPrimitive()) {
            throw new IllegalArgumentException("trace line missing string '" + member + "': " + line);
        }
        return element.getAsString();
    }

    private static Iterable<JsonElement> optArray(JsonObject root, String member) {
        JsonElement element = root.get(member);
        return element != null && element.isJsonArray() ? element.getAsJsonArray() : List.of();
    }

    private static FloatPos readPos(JsonArray array, String line) {
        if (array.size() != 2) {
            throw new IllegalArgumentException("trace position is not [x, y]: " + line);
        }
        return new FloatPos(array.get(0).getAsDouble(), array.get(1).getAsDouble());
    }

    private static FloatRect readRect(JsonArray array, String line) {
        if (array.size() != 4) {
            throw new IllegalArgumentException("trace rect is not [x, y, w, h]: " + line);
        }
        return new FloatRect(
            array.get(0).getAsDouble(),
            array.get(1).getAsDouble(),
            array.get(2).getAsDouble(),
            array.get(3).getAsDouble()
        );
    }

    private static JsonArray pos(double x, double y) {
        JsonArray array = new JsonArray(2);
        array.add(x);
        array.add(y);
        return array;
    }

    private static JsonArray rect(FloatRect rect) {
        JsonArray array = new JsonArray(4);
        array.add(rect.x());
        array.add(rect.y());
        array.add(rect.width());
        array.add(rect.height());
        return array;
    }
}
