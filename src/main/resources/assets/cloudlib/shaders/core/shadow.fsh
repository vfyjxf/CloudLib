#version 150

// Rounded-rectangle soft shadow (Gaussian-like falloff via SDF).

in vec2 texCoord;

uniform vec4  FillColor;    // shadow colour

uniform vec2  Size;           // total quad size (including expansion)
uniform vec4  Radii;          // corner radii of inner shape
uniform float ShadowSpread;   // how far the quad was expanded
uniform float ShadowSoftness; // blur distance

out vec4 fragColor;

float sdRoundedBox(vec2 p, vec2 b, vec4 r) {
    r.xy = (p.x > 0.0) ? r.xy : r.zw;
    r.x  = (p.y > 0.0) ? r.x  : r.y;
    vec2 q = abs(p) - b + r.x;
    return min(max(q.x, q.y), 0.0) + length(max(q, 0.0)) - r.x;
}

void main() {
    vec2  pixel  = texCoord * Size;
    vec2  center = Size * 0.5;
    vec2  inner  = max(center - vec2(ShadowSpread), vec2(0.0));
    float d      = sdRoundedBox(pixel - center, inner, Radii);

    float shadow = 1.0 - smoothstep(0.0, max(ShadowSoftness, 0.001), d);

    fragColor = FillColor * vec4(1.0, 1.0, 1.0, shadow);

    if (fragColor.a < 0.004) discard;
}
