// The guide-line stroke's shared style uniforms and fragment arithmetic.
//
// Imported by both guide-line shaders so a dash, a fade or an outline changes
// in exactly one place: guide_line_hud resolves the polyline distance and arc
// position per fragment, guide_line_world receives them interpolated from the
// vertex stage, and both end in guideLineStroke().
//
// Arc positions are fractions of the stroke's length measured from the PANEL
// end (0) toward the WORLD end (1) — the direction the entry animation grows
// in, and the end the arc-length fade dims.

uniform vec4  LineColor;    // core hue; alpha is the stroke's base alpha
uniform vec4  EdgeColor;    // dark outline hue; alpha is the outline's weight
uniform float LineWidth;    // the core stroke's full width, in px
uniform float EdgeWidth;    // px of outline added on each side of the core
uniform float Smoothing;    // AA width in px — one target texel (1 / guiScale)
uniform float FadeFraction; // arc fraction at the world end that fades out
uniform float FadeAlpha;    // the alpha multiplier at the world end of the arc
uniform float DashPeriod;   // dash period in px; 0 draws a solid stroke
uniform float DashDuty;     // solid fraction of the period, in (0, 1]
uniform float DashPhase;    // the dash pattern's offset along the arc, in px
uniform float ArcStart;     // the drawn arc's start fraction (panel end = 0)
uniform float ArcEnd;       // the drawn arc's end fraction (world end = 1)

// The arc-length fade: full over the panel side of the stroke, easing down to
// FadeAlpha over the last FadeFraction of the arc at the world end.
float guideLineEndFade(float arcFraction) {
    float fade = clamp(FadeFraction, 0.0, 1.0);
    if (fade <= 0.0) {
        return 1.0;
    }
    float remaining = 1.0 - clamp(arcFraction, 0.0, 1.0);
    if (remaining >= fade) {
        return 1.0;
    }
    return FadeAlpha + (1.0 - FadeAlpha) * (remaining / fade);
}

// The dash mask at an arc position in px: 1 inside a solid run, 0 in a gap,
// eased on both edges by one smoothing width.
float guideLineDash(float arcPx) {
    if (DashPeriod <= 0.0) {
        return 1.0;
    }
    float period = max(DashPeriod, 0.001);
    float along = mod(arcPx + DashPhase, period);
    float duty = clamp(DashDuty, 0.0, 1.0) * period;
    float aa = max(Smoothing, 0.01);
    return (1.0 - smoothstep(duty - aa, duty + aa, along)) * smoothstep(0.0, aa, along);
}

// The reveal's arc window: 1 inside [ArcStart, ArcEnd], eased over one
// smoothing width at both ends — the growing tip never steps a whole pixel.
float guideLineWindow(float arcFraction, float totalPx) {
    float aa = max(Smoothing, 0.01) / max(totalPx, 1.0);
    return smoothstep(ArcStart - aa, ArcStart + aa, arcFraction)
        * (1.0 - smoothstep(ArcEnd - aa, ArcEnd + aa, arcFraction));
}

// One stroke sample: `centerPx` is the distance from the centreline in px,
// `arcPx` the position along the arc in px, `totalPx` the stroke's length.
// The core carries LineColor, the band around it EdgeColor, and both are
// dimmed by the dash, the end fade and the reveal window.
vec4 guideLineStroke(float centerPx, float arcPx, float totalPx) {
    float total = max(totalPx, 0.001);
    float arcFraction = clamp(arcPx / total, 0.0, 1.0);
    float window = guideLineWindow(arcFraction, total);
    if (window <= 0.0) {
        return vec4(0.0);
    }

    float halfW = LineWidth * 0.5;
    float core = 1.0 - smoothstep(halfW - Smoothing, halfW + Smoothing, centerPx);
    float outer = 1.0 - smoothstep(halfW + EdgeWidth - Smoothing, halfW + EdgeWidth + Smoothing, centerPx);
    float edge = max(outer - core, 0.0);
    if (core <= 0.0 && edge <= 0.0) {
        return vec4(0.0);
    }

    float common = guideLineDash(arcPx) * guideLineEndFade(arcFraction) * window;
    vec3 rgb = mix(EdgeColor.rgb, LineColor.rgb, clamp(core, 0.0, 1.0));
    return vec4(rgb, (core * LineColor.a + edge * EdgeColor.a) * common);
}
