#version 150

// Bezier curve renderer – supports quadratic and cubic curves.
//
// For UE-Blueprint-style connections: use cubic mode with endpoint colours
// (ColorStart / ColorEnd) to get a smooth gradient along the curve, plus
// an optional outer glow (GlowWidth / GlowColor).

in vec2 texCoord;

uniform vec2  Size;       // quad size in pixels
uniform vec2  P0;         // start point      (quad-local pixels)
uniform vec2  P1;         // control point 1
uniform vec2  P2;         // control point 2  (end point for quadratic)
uniform vec2  P3;         // end point        (cubic only)
uniform int   CurveType;  // 0 = quadratic, 1 = cubic
uniform float LineWidth;  // half-width in pixels
uniform float Smoothing;  // AA edge softness

// Gradient colours along the curve (start → end)
uniform vec4  ColorStart;
uniform vec4  ColorEnd;
// Outer glow
uniform float GlowWidth;  // extra glow radius (0 = off)
uniform vec4  GlowColor;  // glow colour

out vec4 fragColor;

// ── helpers ──────────────────────────────────────────────────────────

float dot2(vec2 v) { return dot(v, v); }

// Exact SDF for a quadratic Bezier (Inigo Quilez).
float sdBezierQuadratic(vec2 pos, vec2 A, vec2 B, vec2 C) {
    vec2 a = B - A, b = A - 2.0*B + C, c = a*2.0, d = A - pos;
    float kk = 1.0 / dot(b, b);
    float kx = kk * dot(a, b);
    float ky = kk * (2.0*dot(a, a) + dot(d, b)) / 3.0;
    float kz = kk * dot(d, a);
    float p  = ky - kx*kx;
    float q  = kx*(2.0*kx*kx - 3.0*ky) + kz;
    float h  = q*q + 4.0*p*p*p;
    float res;
    if (h >= 0.0) {
        h = sqrt(h);
        vec2  x  = (vec2(h, -h) - q) / 2.0;
        vec2  uv = sign(x) * pow(abs(x), vec2(1.0/3.0));
        float t  = clamp(uv.x + uv.y - kx, 0.0, 1.0);
        res = dot2(d + (c + b*t)*t);
    } else {
        float z = sqrt(-p);
        float v = acos(q / (p*z*2.0)) / 3.0;
        float m = cos(v), n = sin(v)*1.732050808;
        vec3  t = clamp(vec3(m+m, -n-m, n-m)*z - kx, 0.0, 1.0);
        res = min(dot2(d + (c + b*t.x)*t.x),
                  dot2(d + (c + b*t.y)*t.y));
    }
    return sqrt(res);
}

// Approximate SDF for a cubic Bezier via segment sampling.
// Also returns the closest t parameter for gradient evaluation.
void sdBezierCubic(vec2 pos, vec2 A, vec2 B, vec2 C, vec2 D,
                   out float dist, out float closestT) {
    const int N = 32;
    float minD = 1e10;
    float bestT = 0.0;
    vec2 prev = A;
    for (int i = 1; i <= N; i++) {
        float t   = float(i) / float(N);
        float mt  = 1.0 - t;
        vec2 pt   = mt*mt*mt*A + 3.0*mt*mt*t*B + 3.0*mt*t*t*C + t*t*t*D;
        vec2 pa   = pos - prev;
        vec2 ba   = pt - prev;
        float h   = clamp(dot(pa, ba) / dot(ba, ba), 0.0, 1.0);
        float seg = length(pa - ba*h);
        if (seg < minD) {
            minD  = seg;
            bestT = mix(float(i-1)/float(N), t, h);
        }
        prev = pt;
    }
    dist     = minD;
    closestT = bestT;
}

// Returns closest-t for a quadratic Bezier (simplified).
float closestTQuadratic(vec2 pos, vec2 A, vec2 B, vec2 C) {
    const int N = 32;
    float minD = 1e10, bestT = 0.0;
    for (int i = 0; i <= N; i++) {
        float t  = float(i) / float(N);
        float mt = 1.0 - t;
        vec2 pt  = mt*mt*A + 2.0*mt*t*B + t*t*C;
        float d  = length(pos - pt);
        if (d < minD) { minD = d; bestT = t; }
    }
    return bestT;
}

void main() {
    vec2 pixel = texCoord * Size;

    float d;
    float t;

    if (CurveType == 0) {
        d = sdBezierQuadratic(pixel, P0, P1, P2);
        t = closestTQuadratic(pixel, P0, P1, P2);
    } else {
        sdBezierCubic(pixel, P0, P1, P2, P3, d, t);
    }

    // Core line mask
    float lineMask = 1.0 - smoothstep(LineWidth - Smoothing, LineWidth + Smoothing, d);
    // Glow mask (additive halo outside the line)
    float glowMask = (GlowWidth > 0.0)
        ? (1.0 - smoothstep(LineWidth, LineWidth + GlowWidth, d)) - lineMask
        : 0.0;

    // Gradient colour along curve
    vec4 lineColor = mix(ColorStart, ColorEnd, t);
    // Combine
    fragColor = lineColor * lineMask + GlowColor * glowMask;

    if (fragColor.a < 0.004) discard;
}
