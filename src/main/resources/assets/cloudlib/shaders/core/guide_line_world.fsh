#version 150

#moj_import <cloudlib:guide_line_common.glsl>

// The guide-line world pass's fragment stage: the arc position and the
// distance across the strip arrive interpolated from the vertex stage, so the
// stroke's dash, end fade, reveal window and outline bands are the same
// arithmetic guide_line_hud resolves per fragment.

in vec4  vertexColor;
in float vertexArc;
in float vertexSide;

uniform float ArcLengthPx; // the connector's screen length, in px

out vec4 fragColor;

void main() {
    float geomWidth = LineWidth + 2.0 * EdgeWidth;
    float centerPx = abs(vertexSide) * geomWidth * 0.5;
    vec4 stroke = guideLineStroke(centerPx, clamp(vertexArc, 0.0, 1.0) * ArcLengthPx, ArcLengthPx);
    vec4 color = stroke * vertexColor;
    if (color.a < 0.004) {
        discard;
    }
    fragColor = color;
}
