package dev.vfyjxf.cloudlib.internal.ui.inworld;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.nio.charset.StandardCharsets;
import java.nio.file.Files;
import java.nio.file.Path;
import java.util.List;

import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * {@link UiSurface#render}'s GL state protocol, pinned structurally.
 * <p>
 * The contract under repair (the resize-frame flicker):
 * {@code RenderTarget.resize} (1.21.1) ends with an unconditional
 * {@code glBindFramebuffer(GL_FRAMEBUFFER, 0)}, so the caller's framebuffer
 * binding must be captured <b>before</b> the size pass inside
 * {@code render()} — a capture taken after {@code ensure()} reads 0 on
 * resize frames, and the {@code finally} restore then binds the default
 * framebuffer over the caller's target. That frame's whole world pass draws
 * into the wrong buffer and the end-of-frame blit erases every panel for
 * one frame.
 * <p>
 * GL is unavailable headless, so this cannot execute the real method; it
 * asserts the source order of the two steps inside {@code render()} — the
 * capture query ({@code GL_FRAMEBUFFER_BINDING}) must precede the
 * {@code ensure()} call — plus that the {@code finally} restore reuses the
 * captured binding ({@code prevFbo}), which is what makes "captured before
 * the stomp" the correct value. The behavioral half (resize stomps the
 * binding to 0) is Minecraft's, verified against the 21.1 bytecode.
 */
class UiSurfaceFrameProtocolTest {

    private static final String sourceRel = "src/main/java/dev/vfyjxf/cloudlib/internal/ui/inworld/UiSurface.java";

    /** The render() method body (line comments stripped), extracted by brace matching. */
    private static String renderBody() throws IOException {
        List<String> lines = Files.readAllLines(sourceFile(), StandardCharsets.UTF_8);
        StringBuilder body = new StringBuilder();
        int depth = 0;
        boolean started = false;
        for (String line : lines) {
            if (!started && line.contains("public void render(")) started = true;
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
        throw new AssertionError("render() body not found in " + sourceRel);
    }

    private static Path sourceFile() throws IOException {
        Path path = Path.of(sourceRel);
        if (Files.exists(path)) return path;
        // runners with a different working directory: walk up for the repo root
        Path dir = Path.of("").toAbsolutePath();
        for (int i = 0; i < 6 && dir != null; i++) {
            Path candidate = dir.resolve(sourceRel);
            if (Files.exists(candidate)) return candidate;
            dir = dir.getParent();
        }
        throw new AssertionError(sourceRel + " not found from " + Path.of("").toAbsolutePath());
    }

    @Test
    void framebufferCapturePrecedesTheSizePass() throws IOException {
        String body = renderBody();
        int capture = body.indexOf("GL_FRAMEBUFFER_BINDING");
        int ensure = body.indexOf("ensure(");
        assertTrue(capture >= 0, "render() must query the framebuffer binding (GL_FRAMEBUFFER_BINDING)");
        assertTrue(ensure >= 0, "render() must run the size pass (ensure)");
        assertTrue(
            capture < ensure,
            "the caller's framebuffer binding must be captured BEFORE ensure() —"
                    + " RenderTarget.resize rebinds framebuffer 0, so a later capture would restore"
                    + " the default framebuffer over the caller's target on resize frames"
        );
    }

    @Test
    void theFinallyBlockRestoresTheCapturedBinding() throws IOException {
        String body = renderBody();
        int finallyBlock = body.indexOf("finally");
        int restore = body.indexOf("_glBindFramebuffer", finallyBlock);
        assertTrue(finallyBlock >= 0, "render() must restore state in a finally block");
        assertTrue(restore > finallyBlock, "the finally block must rebind the framebuffer");
        assertTrue(
            body.indexOf("prevFbo", restore) > restore,
            "the finally restore reuses the captured prevFbo local, not a fresh query"
        );
    }
}
