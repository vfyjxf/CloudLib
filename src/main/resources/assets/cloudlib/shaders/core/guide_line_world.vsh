#version 150

#moj_import <cloudlib:guide_line_common.glsl>

// The guide-line world pass: the vanilla rendertype_lines NDC expansion, with
// LineWidth widened to cover the stroke's outline band as well as its core.
//
// `Normal` is not a surface normal here — it is the direction from this vertex
// to its neighbour along the line (vanilla's own line convention), so
// Position + Normal is a second sample of the stroke and the two projected
// samples give the screen-space direction to offset across. The pair of
// vertices that makes one segment is displaced to opposite sides, so the line
// rasterizes as a quad of exactly LineWidth + 2 * EdgeWidth pixels.
//
// vertexArc is the sample's position along the line, 0 at the panel end and 1
// at the world end; it feeds the same dash/fade/window arithmetic the HUD
// shader resolves per fragment.

in vec3 Position;
in vec4 Color;
in vec3 Normal;

uniform mat4  ModelViewMat;
uniform mat4  ProjMat;
uniform vec2  ScreenSize;
uniform int   VertexCount;

out vec4  vertexColor;
out float vertexArc;
out float vertexSide;

const float VIEW_SHRINK = 1.0 - (1.0 / 256.0);
const mat4 VIEW_SCALE = mat4(
    VIEW_SHRINK, 0.0, 0.0, 0.0,
    0.0, VIEW_SHRINK, 0.0, 0.0,
    0.0, 0.0, VIEW_SHRINK, 0.0,
    0.0, 0.0, 0.0, 1.0
);

void main() {
    float geomWidth = LineWidth + 2.0 * EdgeWidth;

    vec4 linePosStart = ProjMat * VIEW_SCALE * ModelViewMat * vec4(Position, 1.0);
    vec4 linePosEnd = ProjMat * VIEW_SCALE * ModelViewMat * vec4(Position + Normal, 1.0);

    vec3 ndc1 = linePosStart.xyz / linePosStart.w;
    vec3 ndc2 = linePosEnd.xyz / linePosEnd.w;

    vec2 lineScreenDirection = normalize((ndc2.xy - ndc1.xy) * ScreenSize);
    vec2 lineOffset = vec2(-lineScreenDirection.y, lineScreenDirection.x) * geomWidth / ScreenSize;

    if (lineOffset.x < 0.0) {
        lineOffset *= -1.0;
    }

    if (gl_VertexID % 2 == 0) {
        gl_Position = vec4((ndc1 + vec3(lineOffset, 0.0)) * linePosStart.w, linePosStart.w);
        vertexSide = 1.0;
    } else {
        gl_Position = vec4((ndc1 - vec3(lineOffset, 0.0)) * linePosStart.w, linePosStart.w);
        vertexSide = -1.0;
    }

    vertexColor = Color;
    vertexArc = float(gl_VertexID) / max(float(VertexCount - 1), 1.0);
}
