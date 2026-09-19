#version 150

#moj_import <cloudlib:guide_line_common.glsl>

// The guide-line HUD pass: one quad per stroke, carrying the whole polyline in
// the Points uniform (64 vec2 samples, in quad-local px, panel end first). The
// fragment resolves the distance to the polyline and the arc position itself,
// so a dash, a gradient and the AA edges cost no geometry at all.

in vec2 texCoord;

uniform vec2 Size;         // the quad's size in px — texCoord × Size is local px
uniform vec2 Points[64];   // up to 64 samples, quad-local px, panel end first
uniform int  PointCount;   // how many of them are live
uniform int  Marker;       // 0 = none, 1 = dot (face/area), 2 = arrow (edge/enemy)
uniform float MarkerSize;  // the dot's radius / the arrow's length, in px
uniform float PortTick;    // the panel-end tick's half-length in px; 0 = off

out vec4 fragColor;

// The 3-edge triangle SDF (Inigo Quilez) — the arrow's filled body.
float sdTriangle(vec2 p, vec2 p0, vec2 p1, vec2 p2) {
    vec2 e0 = p1 - p0, e1 = p2 - p1, e2 = p0 - p2;
    vec2 v0 = p - p0, v1 = p - p1, v2 = p - p2;
    vec2 q0 = v0 - e0 * clamp(dot(v0, e0) / dot(e0, e0), 0.0, 1.0);
    vec2 q1 = v1 - e1 * clamp(dot(v1, e1) / dot(e1, e1), 0.0, 1.0);
    vec2 q2 = v2 - e2 * clamp(dot(v2, e2) / dot(e2, e2), 0.0, 1.0);
    float s = sign(e0.x * e2.y - e0.y * e2.x);
    vec2 d = min(min(vec2(dot(q0, q0), s * (v0.x * e0.y - v0.y * e0.x)),
                     vec2(dot(q1, q1), s * (v1.x * e1.y - v1.y * e1.x))),
                     vec2(dot(q2, q2), s * (v2.x * e2.y - v2.y * e2.x)));
    return -sqrt(d.x) * sign(d.y);
}

float sdSegment(vec2 p, vec2 a, vec2 b) {
    vec2 ab = b - a;
    float len2 = dot(ab, ab);
    float h = len2 > 1.0e-8 ? clamp(dot(p - a, ab) / len2, 0.0, 1.0) : 0.0;
    return length(p - (a + ab * h));
}

vec2 unit(vec2 v) {
    float len = length(v);
    return len > 1.0e-4 ? v / len : vec2(1.0, 0.0);
}

void main() {
    if (PointCount < 2) {
        discard;
    }
    vec2 p = texCoord * Size;

    // closest sample on the polyline, with the arc position there
    float best = 1.0e20;
    float bestArc = 0.0;
    float walked = 0.0;
    for (int i = 0; i < 63; i++) {
        if (i + 1 >= PointCount) {
            break;
        }
        vec2 a = Points[i];
        vec2 b = Points[i + 1];
        vec2 ab = b - a;
        float len2 = dot(ab, ab);
        float h = len2 > 1.0e-8 ? clamp(dot(p - a, ab) / len2, 0.0, 1.0) : 0.0;
        float d = length(p - (a + ab * h));
        if (d < best) {
            best = d;
            bestArc = walked + sqrt(len2) * h;
        }
        walked += sqrt(len2);
    }
    float total = max(walked, 0.001);

    vec4 stroke = guideLineStroke(best, bestArc, total);
    float aa = max(Smoothing, 0.01);
    float halfW = LineWidth * 0.5;

    // the panel-end port tick: a bar square across the stroke, at full alpha
    float tick = 0.0;
    if (PortTick > 0.0) {
        vec2 dir = unit(Points[1] - Points[0]);
        vec2 perp = vec2(-dir.y, dir.x);
        float d = sdSegment(p, Points[0] - perp * PortTick, Points[0] + perp * PortTick);
        tick = (1.0 - smoothstep(halfW - aa, halfW + aa, d)) * guideLineWindow(0.0, total);
    }

    // the target-end marker: a dot for a face/area target, an arrow for an
    // edge/enemy one, appearing once the reveal has reached that end
    float mark = 0.0;
    if (Marker > 0 && MarkerSize > 0.0) {
        vec2 end = Points[PointCount - 1];
        vec2 dir = unit(end - Points[PointCount - 2]);
        vec2 perp = vec2(-dir.y, dir.x);
        float d;
        if (Marker == 2) {
            vec2 tip = end + dir * (MarkerSize * 0.55);
            vec2 base = end - dir * (MarkerSize * 0.45);
            d = sdTriangle(p, tip, base + perp * (MarkerSize * 0.42), base - perp * (MarkerSize * 0.42));
        } else {
            d = length(p - end) - MarkerSize;
        }
        mark = (1.0 - smoothstep(-aa, aa, d)) * guideLineWindow(1.0, total);
    }

    float solid = max(mark, tick) * LineColor.a;
    float alpha = max(stroke.a, solid);
    if (alpha < 0.004) {
        discard;
    }
    fragColor = vec4(mix(stroke.rgb, LineColor.rgb, clamp(solid, 0.0, 1.0)), alpha);
}
