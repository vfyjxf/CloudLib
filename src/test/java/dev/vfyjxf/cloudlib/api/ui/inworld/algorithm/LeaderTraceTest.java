package dev.vfyjxf.cloudlib.api.ui.inworld.algorithm;

import dev.vfyjxf.cloudlib.api.math.FloatPos;
import dev.vfyjxf.cloudlib.api.math.FloatRect;
import org.junit.jupiter.api.Test;

import java.io.BufferedReader;
import java.io.IOException;
import java.io.InputStream;
import java.io.InputStreamReader;
import java.nio.charset.StandardCharsets;
import java.util.ArrayList;
import java.util.HashMap;
import java.util.List;
import java.util.Map;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertThrows;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The trace format against itself: a line round-trips its frame and its
 * routes, the deterministic replay reproduces itself, and the frozen
 * oscillation fixture — the in-game report this whole pipeline exists for —
 * replays with its topology switches bounded. The fixture is four mutually
 * crossing/near leaders with one anchor panning 0.5 px per frame: before the
 * anti-oscillation guards this exact sequence churned 114 topology changes
 * after the settle (an every-epoch reroute that dodged one leader by
 * piercing another, phantom re-commits off the stale crossing matrix, and
 * the switch-cost rule undoing bought detours at every 12 px quantization
 * flip).
 */
class LeaderTraceTest {

    private static final LeaderRouter.Config config = LeaderRouter.Config.of(1.0, 2);

    @Test
    void aFrameRoundTripsThroughTheLine() {
        LeaderTrace.Frame frame = new LeaderTrace.Frame(
                7,
                List.of(
                        new LeaderRouter.Leader(
                                "panel/1",
                                12.5,
                                -3.25,
                                new AttachPointResolver.Port(
                                        AttachPointResolver.Face.left, new FloatPos(400, 0), -1.0, 0.0),
                                new FloatRect(394, -40, 180, 96)),
                        LeaderRouter.Leader.toPoint("b", 0, 0, 100, 100)),
                Map.of("b", "cluster"),
                List.of(new FloatRect(600, 210, 40, 80)),
                new FloatRect(0, 0, 1280, 720));

        String line = LeaderTrace.encode(frame, null);
        assertEquals(frame, LeaderTrace.decodeFrame(line), "the frame survives the line");
        assertEquals(line, LeaderTrace.encode(LeaderTrace.decodeFrame(line), null), "the line is stable");

        LeaderTrace.Frame unbounded = new LeaderTrace.Frame(0, frame.leaders(), Map.of(), List.of(), null);
        assertEquals(unbounded, LeaderTrace.decodeFrame(LeaderTrace.encode(unbounded, null)), "null viewport rides");
    }

    @Test
    void aRecordingLineCarriesItsRoutes() {
        LeaderTrace.Frame frame = new LeaderTrace.Frame(
                1, List.of(LeaderRouter.Leader.toPoint("a", 0, 0, 300, 100)), Map.of(), List.of(), null);
        LeaderRouter router = new LeaderRouter(config);
        List<LeaderRouter.Route> routes = router.route(frame.leaders(), Map.of(), List.of(), null);

        String line = LeaderTrace.encode(frame, routes);
        assertEquals(routes, LeaderTrace.decodeRoutes(line), "the routes survive the line");
        // and the line is still a replay input: the routes member is inert
        assertEquals(frame, LeaderTrace.decodeFrame(line));
    }

    @Test
    void malformedLinesAreRejectedWithTheLineInTheMessage() {
        assertThrows(IllegalArgumentException.class, () -> LeaderTrace.decodeFrame("not json"));
        assertThrows(IllegalArgumentException.class, () -> LeaderTrace.decodeFrame("{\"leaders\": 3}"));
        String noLeaders = "{\"frame\":0}";
        assertThrows(IllegalArgumentException.class, () -> LeaderTrace.decodeFrame(noLeaders));
        String badPos =
                "{\"leaders\":[{\"id\":\"a\",\"anchor\":[1],\"port\":{\"point\":[0,0],\"face\":\"left\",\"normal\":[1,0]}}]}";
        assertThrows(IllegalArgumentException.class, () -> LeaderTrace.decodeFrame(badPos));
    }

    @Test
    void theOscillationFixtureReplaysWithBoundedTopologySwitches() throws IOException {
        List<LeaderTrace.Frame> frames = readFixture("/trace/oscillation.jsonl");
        assertEquals(96, frames.size(), "the fixture is the full 96-frame pan");

        int switches = topologySwitches(LeaderTrace.replay(frames, config));
        assertTrue(switches <= 1, "the settled field flipped topology " + switches + " times under a pure pan");
    }

    @Test
    void theReplayIsDeterministic() throws IOException {
        List<LeaderTrace.Frame> frames = readFixture("/trace/oscillation.jsonl");
        assertEquals(LeaderTrace.replay(frames, config), LeaderTrace.replay(frames, config));
    }

    /** shapeEpoch changes per leader after the settle window (frame 12), summed. */
    private static int topologySwitches(List<List<LeaderRouter.Route>> epochs) {
        Map<String, Long> last = new HashMap<>();
        int switches = 0;
        for (int frame = 0; frame < epochs.size(); frame++) {
            for (LeaderRouter.Route route : epochs.get(frame)) {
                Long previous = last.put(route.id(), route.shapeEpoch());
                if (previous != null && frame >= 12 && previous != route.shapeEpoch()) {
                    switches++;
                }
            }
        }
        return switches;
    }

    private static List<LeaderTrace.Frame> readFixture(String resource) throws IOException {
        try (InputStream in = LeaderTraceTest.class.getResourceAsStream(resource)) {
            assertTrue(in != null, resource + " missing from the test classpath");
            List<LeaderTrace.Frame> frames = new ArrayList<>();
            try (BufferedReader reader = new BufferedReader(new InputStreamReader(in, StandardCharsets.UTF_8))) {
                String line;
                while ((line = reader.readLine()) != null) {
                    if (!line.isBlank()) {
                        frames.add(LeaderTrace.decodeFrame(line));
                    }
                }
            }
            return frames;
        }
    }
}
