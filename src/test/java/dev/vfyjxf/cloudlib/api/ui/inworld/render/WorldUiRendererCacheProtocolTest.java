package dev.vfyjxf.cloudlib.api.ui.inworld.render;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@code WorldUiRenderer}'s surface-cache wiring, pinned structurally.
 * <p>
 * The renderer is GL- and event-bus-tied (Minecraft instance, NeoForge level
 * stage), so it cannot run headless; this pins the three structural points
 * the {@code RepaintGate} protocol depends on, in the same style as
 * {@code UiSurfaceFrameProtocolTest}:
 * <ol>
 *   <li>{@code renderSurfaces} routes the surface repaint through the
 *       panel's {@code RepaintGate} — a cached panel never reaches
 *       {@code renderSurface(...)};</li>
 *   <li>{@code removePanel} drops the panel's gate (no state leaks across a
 *       removed panel's identity);</li>
 *   <li>{@code clearPanels} clears every gate with the panel list.</li>
 * </ol>
 * The behavioral half (skip/repaint/resize decisions) is covered by
 * {@code RepaintGateTest} against the real gate.
 */
class WorldUiRendererCacheProtocolTest {

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
    void renderSurpassesThroughTheRepaintGate() throws IOException {
        String body = methodBody(source(), "private void renderSurfaces(");
        int gate = body.indexOf("gate.render(");
        int surfaceRender = body.indexOf("renderSurface(");
        assertTrue(gate >= 0, "renderSurfaces must route repaints through the panel's RepaintGate");
        assertTrue(
            surfaceRender > gate,
            "the renderSurface call must sit inside the gate's render action"
                    + " (the runnable), not run unconditionally"
        );
    }

    @Test
    void removePanelDropsTheGate() throws IOException {
        String body = methodBody(source(), "public void removePanel(");
        assertTrue(body.contains("repaintGates.remove(panel)"), "removePanel must drop the panel's repaint gate");
    }

    @Test
    void clearPanelsClearsTheGates() throws IOException {
        String body = methodBody(source(), "public void clearPanels(");
        assertTrue(body.contains("repaintGates.clear()"), "clearPanels must clear every repaint gate");
    }
}
