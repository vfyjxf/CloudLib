#version 150

// SDF rounded rectangle – per-corner radii, optional border, optional texture.
//
// UV [0,1] maps normalised position within the quad.  When HasTexture == 1
// Sampler0 provides the base colour (modulated by vertex colour); otherwise
// the vertex colour alone is used as fill.

in vec2 texCoord;

uniform sampler2D Sampler0;

uniform vec4  FillColor;   // fill colour (from Java ARGB → rgba)

uniform vec2  Size;        // quad size in pixels
uniform vec4  Radii;       // corner radii: (topRight, bottomRight, topLeft, bottomLeft)
uniform float BorderWidth; // 0 = filled
uniform vec4  BorderColor; // RGBA
uniform float Smoothing;   // AA width (default 1.0)
uniform int   HasTexture;  // 0 = colour only, 1 = textured

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
    float d      = sdRoundedBox(pixel - center, center, Radii);

    float outer = 1.0 - smoothstep(-Smoothing, Smoothing, d);
    float inner = 1.0 - smoothstep(-Smoothing, Smoothing, d + BorderWidth);

    vec4 base = (HasTexture != 0)
              ? texture(Sampler0, texCoord) * FillColor
              : FillColor;

    fragColor = base * inner + BorderColor * (outer - inner);

    if (fragColor.a < 0.004) discard;
}
