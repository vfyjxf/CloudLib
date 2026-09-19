package dev.vfyjxf.cloudlib.api.ui.canvas;

import org.junit.jupiter.api.Test;

import java.io.IOException;
import java.io.InputStream;
import java.nio.charset.StandardCharsets;
import java.util.LinkedHashSet;
import java.util.Set;
import java.util.regex.Matcher;
import java.util.regex.Pattern;

import static org.junit.jupiter.api.Assertions.assertEquals;
import static org.junit.jupiter.api.Assertions.assertNotNull;
import static org.junit.jupiter.api.Assertions.assertTrue;

/**
 * The guide-line shaders' resource contract: every uniform the Java side sets
 * by name — and the vertex attributes its {@code VertexFormat} supplies — has
 * to be declared by the shader's json, or the driver silently hands back a
 * dead uniform and the stroke draws wrong. GLSL itself cannot be compiled
 * headlessly, so this pins the half that can be checked without a context.
 */
class GuideLineShaderResourceTest {

    private static final String dir = "/assets/cloudlib/shaders/core/";

    /** The uniforms {@link SceneCanvas#guideLine} sets, by name. */
    private static final Set<String> hudUniforms = Set.of(
            "ModelViewMat",
            "ProjMat",
            "Size",
            "Points",
            "PointCount",
            "Marker",
            "MarkerSize",
            "PortTick",
            "LineColor",
            "EdgeColor",
            "LineWidth",
            "EdgeWidth",
            "Smoothing",
            "FadeFraction",
            "FadeAlpha",
            "DashPeriod",
            "DashDuty",
            "DashPhase",
            "ArcStart",
            "ArcEnd");

    /** The uniforms one world-pass stroke sets — the vertex stage and the shared fragment math. */
    private static final Set<String> worldUniforms = Set.of(
            "ModelViewMat",
            "ProjMat",
            "ScreenSize",
            "VertexCount",
            "ArcLengthPx",
            "LineColor",
            "EdgeColor",
            "LineWidth",
            "EdgeWidth",
            "Smoothing",
            "FadeFraction",
            "FadeAlpha",
            "DashPeriod",
            "DashDuty",
            "DashPhase",
            "ArcStart",
            "ArcEnd");

    @Test
    void theHudShaderDeclaresEveryUniformTheCanvasSets() throws IOException {
        String json = read(dir + "guide_line_hud.json");

        assertEquals(hudUniforms, uniformNames(json), "the HUD json's uniform set");
        assertEquals(128, uniformCount(json, "Points"), "64 vec2 samples fit one uniform block");
        assertEquals(2.0, Double.parseDouble(uniformValue(json, "LineWidth")), 1.0e-9, "the default 2 px core");
        assertTrue(json.contains("\"cloudlib:guide_line_hud\""), "self-registered program name");
        assertTrue(json.contains("\"Position\"") && json.contains("\"UV0\""), "the POSITION_TEX attributes");
    }

    @Test
    void theWorldShaderDeclaresEveryUniformThePassSets() throws IOException {
        String json = read(dir + "guide_line_world.json");

        assertEquals(worldUniforms, uniformNames(json), "the world json's uniform set");
        assertTrue(
                json.contains("\"Position\"") && json.contains("\"Color\"") && json.contains("\"Normal\""),
                "the POSITION_COLOR_NORMAL attributes the NDC expansion reads");
        assertTrue(json.contains("\"cloudlib:guide_line_world\""));
    }

    @Test
    void bothShadersShareOneFragmentMathInclude() throws IOException {
        String include = read("/assets/cloudlib/shaders/include/guide_line_common.glsl");
        for (String name : Set.of(
                "LineColor",
                "EdgeColor",
                "LineWidth",
                "EdgeWidth",
                "Smoothing",
                "FadeFraction",
                "FadeAlpha",
                "DashPeriod",
                "DashDuty",
                "DashPhase",
                "ArcStart",
                "ArcEnd")) {
            assertTrue(include.contains("uniform") && include.contains(name), "the include declares " + name);
        }
        assertTrue(include.contains("vec4 guideLineStroke("), "the shared entry point");
        assertTrue(include.contains("guideLineDash(") && include.contains("guideLineEndFade("));

        // and both fragment stages import it rather than re-deriving the maths
        assertTrue(read(dir + "guide_line_hud.fsh").contains("#moj_import <cloudlib:guide_line_common.glsl>"));
        assertTrue(read(dir + "guide_line_world.fsh").contains("#moj_import <cloudlib:guide_line_common.glsl>"));
        assertTrue(
                read(dir + "guide_line_world.vsh").contains("#moj_import <cloudlib:guide_line_common.glsl>"),
                "the vertex stage widens LineWidth by the outline band, so it reads the style too");
    }

    @Test
    void theWorldVertexStageCopiesTheVanillaLineExpansion() throws IOException {
        String vsh = read(dir + "guide_line_world.vsh");

        assertTrue(vsh.contains("VIEW_SHRINK"), "vanilla's pixel-centre shrink");
        assertTrue(
                vsh.contains("vec4(Position + Normal, 1.0)"), "Normal is the neighbour sample, not a surface normal");
        assertTrue(vsh.contains("gl_VertexID % 2"), "the pair is displaced to opposite sides");
        assertTrue(vsh.contains("vertexArc") && vsh.contains("vertexSide"), "the fragment stage's varyings");
        // one connector is a strip of four vertices: two per end, so the arc
        // index counts PAIRS — the two vertices of a pair are one sample
        assertTrue(
                vsh.contains("gl_VertexID / 2") && vsh.contains("VertexCount / 2 - 1"),
                "the arc derives from the vertex pair index, not the raw vertex id");
    }

    // region tiny json readers

    private static String read(String resource) throws IOException {
        try (InputStream in = GuideLineShaderResourceTest.class.getResourceAsStream(resource)) {
            assertNotNull(in, "missing resource " + resource);
            return new String(in.readAllBytes(), StandardCharsets.UTF_8);
        }
    }

    /** Every {@code "name": "X"} under the json's uniform list. */
    private static Set<String> uniformNames(String json) {
        Set<String> names = new LinkedHashSet<>();
        Matcher matcher = Pattern.compile("\"name\"\\s*:\\s*\"(\\w+)\"").matcher(json);
        while (matcher.find()) {
            names.add(matcher.group(1));
        }
        return names;
    }

    private static int uniformCount(String json, String name) {
        return Integer.parseInt(uniformField(json, name, "count"));
    }

    private static String uniformValue(String json, String name) {
        return uniformField(json, name, "values");
    }

    /** The {@code field}'s value inside the uniform object named {@code name}. */
    private static String uniformField(String json, String name, String field) {
        Matcher entry = Pattern.compile("\\{[^{}]*\"name\"\\s*:\\s*\"" + name + "\"[^{}]*}")
                .matcher(json);
        assertTrue(entry.find(), "no uniform named " + name);
        Matcher value =
                Pattern.compile("\"" + field + "\"\\s*:\\s*(\\[[^]]*]|\\d+)").matcher(entry.group());
        assertTrue(value.find(), "uniform " + name + " declares no " + field);
        String raw = value.group(1);
        if (!raw.startsWith("[")) return raw;
        String first = raw.substring(1, raw.indexOf(']')).split(",")[0].trim();
        return first;
    }

    // endregion
}
