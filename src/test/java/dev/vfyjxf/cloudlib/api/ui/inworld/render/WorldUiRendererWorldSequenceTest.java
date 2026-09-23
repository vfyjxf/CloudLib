package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code WorldUiRenderer}'s world-pass wiring, pinned structurally.
 * <p>
 * The renderer is GL- and event-bus-tied (Minecraft instance, NeoForge level
 * stage), so the pass itself cannot run headless; the ordering <em>rule</em> is
 * covered behaviourally by {@code DepthOrderTest}, and this pins the wiring the
 * rule depends on:
 * <ol>
 *   <li>the pass orders its items through {@link DepthOrder} — a local
 *       re-implementation is how the far → near contract silently inverted
 *       before (the pass drew the nearest item first, so the farthest item
 *       composited on top of a nearer translucent quad);</li>
 *   <li>a panel's companion lines draw inside its own slot of the sequence,
 *       not in one trailing batch after every quad;</li>
 *   <li>{@code overlays} is cleared by the level-stage hook itself, so an
 *       early return (hidden GUI, no panels) cannot leave a host's geometry
 *       for the next frame;</li>
 *   <li>an overlay-only frame still draws — both early-return guards count
 *       {@code overlays}.</li>
 * </ol>
 */
class WorldUiRendererWorldSequenceTest {

    private static final String sourceRel = "src/main/java/dev/vfyjxf/cloudlib/api/ui/inworld/render/WorldUiRenderer.java";

    private static String source() throws IOException {
        Path path = sourceFile();
        return Files.readString(path, StandardCharsets.UTF_8);
    }

    private static Path sourceFile() throws IOException {
        Path path = Path.of(sourceRel);
        if (Files.exists(path)) return path;
        Path dir = Path.of("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve(sourceRel);
            if (Files.exists(candidate)) return candidate;
            dir = dir.getParent();
        }
        throw new AssertionError(sourceRel + " not found from " + Path.of("").toAbsolutePath());
    }

    /** The body of the named method (line comments stripped), by brace matching. */
    private static String methodBody(String source, String signature) {
        List<String> lines = source.lines().toList();
        StringBuilder body = new StringBuilder();
        int depth = 0;
        boolean started = false;
        for (String line : lines) {
            if (!started && line.contains(signature)) started = true;
            if (!started) continue;
            int comment = line.indexOf("//");
            String code = comment >= 0 ? line.substring(0, comment) : line;
            body.append(code).append('\n');
            for (int i = 0; i < code.length(); i++) {
                char c = code.charAt(i);
                if (c == '{') depth++;
                if (c == '}') depth--;
            }
            if (depth == 0 && code.contains("}")) return body.toString();
        }
        throw new AssertionError("method '" + signature + "' not found in " + sourceRel);
    }

    @Test
    void theWorldPassOrdersThroughDepthOrder() throws IOException {
        String sequence = methodBody(source(), "private List<DrawItem> sequence(");
        assertTrue(
            sequence.contains("DepthOrder.farToNear("),
            "the pass must order its items through DepthOrder — the one place the far → near direction is fixed"
        );
        String pass = methodBody(source(), "private void worldPass(");
        assertTrue(pass.contains("sequence("), "the world pass must draw the sequence it ordered");
    }

    @Test
    void aPanelsLinesDrawInItsOwnSlot() throws IOException {
        String pass = methodBody(source(), "private void worldPass(");
        int quad = pass.indexOf("drawPanelQuad(");
        int lines = pass.indexOf("drawPanelLines(");
        int emitters = pass.indexOf("drawEmitterLines(");
        assertTrue(quad >= 0 && lines > quad, "a panel's companion lines must draw with the panel's quad");
        assertTrue(emitters > lines, "only the untethered emitters may draw after the whole sequence");
    }

    @Test
    void theOverlayListIsClearedByTheFrameHook() throws IOException {
        String hook = methodBody(source(), "private void onLevelStage(");
        assertTrue(hook.contains("overlays.clear()"), "the level-stage hook must clear the frame's overlays");
        assertTrue(
            hook.indexOf("finally") < hook.indexOf("overlays.clear()"),
            "the clear must be in a finally — an early return or a throw must not leave stale overlays"
        );
    }

    @Test
    void anOverlayOnlyFrameStillDraws() throws IOException {
        String pass = methodBody(source(), "private void worldPass(");
        int guards = 0;
        int from = 0;
        while (true) {
            int at = pass.indexOf("overlays.isEmpty()", from);
            if (at < 0) break;
            guards++;
            from = at + 1;
        }
        assertTrue(
            guards >= 2,
            "both early-return guards must count overlays, or a host's chrome would vanish"
                    + " on a frame with no panels, got " + guards
        );
    }
}
